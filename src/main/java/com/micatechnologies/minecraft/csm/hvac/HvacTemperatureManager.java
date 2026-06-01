package com.micatechnologies.minecraft.csm.hvac;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Temperature calculation engine for the HVAC system. Combines the per-chunk biome baseline
 * with a per-position HVAC offset derived from nearby {@link IHvacUnit} tile entities,
 * weighted by distance and a coarse wall-attenuation raycast. The biome baseline is cached
 * per chunk for {@link #CACHE_LIFETIME_TICKS} ticks (2 seconds) since it doesn't vary inside
 * a chunk; the HVAC offset is recomputed every call so two positions in the same chunk get
 * the right local reading (a heater and cooler in the same chunk each dominate their own
 * area instead of cancelling out).
 *
 * <p><b>Smoothing belongs to the consumer.</b> This class returns the instantaneous reading
 * only. Callers that want a smoothed display value (the HUD overlay, future indicators)
 * own a {@link TemperatureSmoother} instance. Thermostats run their own thermal-mass
 * blend on top of {@link #getAmbientTemperatureAt}. Keeping smoothing state out of this
 * manager is what lets multiple consumers coexist without corrupting each other's history.</p>
 *
 * <p><b>Thread safety:</b> The per-dimension cache uses {@code ConcurrentHashMap} so the
 * server tick thread (thermostat reads) and the client render thread (HUD reads in
 * single-player) can both query without external synchronization.</p>
 *
 * @author Mica Technologies
 * @see IHvacUnit
 * @see TemperatureSmoother
 * @since 2026.4
 */
public class HvacTemperatureManager {

  /** Set to true to enable detailed temperature debug logging to the game log. */
  private static final boolean DEBUG_LOGGING = false;

  private static final Logger LOGGER = LogManager.getLogger("CSM-HVAC");

  /** Returns whether debug logging is enabled. Used by thermostat TEs to add their own logs. */
  public static boolean isDebugLogging() { return DEBUG_LOGGING; }

  /** Throttle debug logs to at most once per this many milliseconds. */
  private static final long DEBUG_LOG_INTERVAL_MS = 1000L;
  private static long lastDebugLogMs = 0L;
  /** Set to true for the current calculation pass when the throttle allows logging. */
  private static boolean debugThisPass = false;

  /**
   * Number of ticks between cache recalculations. At 20 TPS this equals 2 seconds. Kept short
   * so temperature changes are visible quickly when moving between chunks or toggling HVAC.
   */
  private static final long CACHE_LIFETIME_TICKS = 40L;

  /**
   * Base HVAC offset cap per direction (heating / cooling), in degrees Fahrenheit. This is the
   * ceiling for a system with a single active heater/cooler unit. Caps the total weighted
   * offset, regardless of how many vent relays are in range — vents are delivery points, not
   * independent heat sources, so adding more vents alone should not increase the indoor
   * temperature ceiling.
   *
   * <p>Sized so a single heater can plausibly hold a room at room-temperature against a
   * meaningfully cold biome (e.g. baseline -10°F + 50°F cap → 40°F indoor max).</p>
   */
  private static final float BASE_HVAC_OFFSET_CAP = 50.0f;

  /**
   * Additional offset cap headroom (in °F) per <em>extra</em> active heater/cooler unit
   * (vents do not count). Lets multi-RTU systems handle extreme-climate buildings where one
   * unit is insufficient — a 5-RTU rooftop on a -30°F mountain biome can push the indoor
   * temperature comfortably above 70°F, where a single unit could not.
   *
   * <p><b>Sample caps:</b></p>
   * <ul>
   *   <li>1 unit: 50°F (e.g. -30°F outdoor → +20°F indoor max)</li>
   *   <li>3 units: 100°F (e.g. -30°F outdoor → +70°F indoor max — comfortable)</li>
   *   <li>5 units: 150°F (e.g. -30°F outdoor → +120°F — extreme)</li>
   * </ul>
   */
  private static final float HVAC_OFFSET_PER_EXTRA_UNIT = 25.0f;

  /**
   * Hard absolute ceiling on the per-direction offset, regardless of unit count. Prevents
   * pathological setups (player griefing, world-edit'd HVAC fields) from producing
   * thousand-degree temperatures that crash the smoothing logic or display.
   */
  private static final float HVAC_OFFSET_HARD_CAP = 250.0f;

  // NOTE on units: the distance thresholds below are measured in flood-fill PATH STEPS
  // (6-connected BFS hops through open air), not straight-line blocks. Because air bends
  // around obstacles, a path-step count is always >= the Euclidean distance — e.g. a ceiling
  // vent 6 blocks away horizontally in an 8-tall room is ~8.5 blocks Euclidean but ~12 path
  // steps (over and down). The thresholds are tuned for that inflation: the older straight-line
  // values (vent 6/24, direct 4/12) made everything read far too weak once propagation switched
  // to path distance. See the room simulations in the HVAC system docs.

  /**
   * Path-step distance within which a direct HVAC unit (heater/cooler) has full effect.
   */
  private static final double UNIT_FULL_EFFECT_DISTANCE = 6.0;

  /**
   * Path-step distance beyond which a direct HVAC unit has no effect.
   */
  private static final double UNIT_MAX_EFFECT_DISTANCE = 20.0;

  /**
   * Path-step distance within which a vent relay has full effect. Vents distribute
   * conditioned air through a room, so they have a wider full-effect zone than the
   * equipment itself.
   */
  private static final double VENT_FULL_EFFECT_DISTANCE = 8.0;

  /**
   * Path-step distance beyond which a vent relay has no effect. Wide enough to cover a
   * room-sized area plus the extra path length of bending around a corner into an adjacent
   * connected space (~30 steps), so a thermostat just around the corner from its vent still
   * feels it.
   */
  private static final double VENT_MAX_EFFECT_DISTANCE = 40.0;

  /**
   * Number of chunks in each direction to scan for HVAC units. Must cover the straight-line
   * span of {@link #VENT_MAX_EFFECT_DISTANCE} path steps — air bending around corners means a
   * vent within 40 path steps can still be a fair number of blocks away in a straight line, so
   * a 3-chunk (7x7) radius gives headroom over the old 2.
   */
  private static final int CHUNK_SCAN_RADIUS = 3;

  /**
   * How often (in ticks) to sweep stale entries from the chunk temperature cache. At 20 TPS
   * this equals 30 seconds — frequent enough to prevent unbounded growth during exploration,
   * infrequent enough to be negligible cost.
   */
  private static final long EVICTION_INTERVAL_TICKS = 600L;

  /**
   * Maximum age (in ticks) for a cache entry before it becomes eligible for eviction. Entries
   * older than this are removed during the next sweep. At 20 TPS this equals 60 seconds.
   */
  private static final long EVICTION_MAX_AGE_TICKS = 1200L;

  /**
   * World tick at which the last eviction sweep was performed. Used to throttle sweeps to
   * once per {@link #EVICTION_INTERVAL_TICKS}.
   */
  private static long lastEvictionTick = 0L;

  /**
   * Maximum number of air cells the flood fill will visit from a single query position before
   * giving up. This is the hard tick-cost ceiling: every temperature query costs at most this
   * many block-state lookups regardless of how open the surrounding space is.
   *
   * <p>This is a <em>3D</em> flood, so the cell count grows roughly cubically with reach: the
   * old 512-cell budget was exhausted only ~7 path steps out in an open 8-tall room, which left
   * a ceiling vent ~12 steps away reading as unreachable (weight 0) — the "vents are too weak /
   * don't reach me" bug. 4096 covers an open room out past {@link #VENT_MAX_EFFECT_DISTANCE}
   * including a bend around a corner, while the early-stop in {@link #floodFillAirDistances}
   * (the fill quits as soon as every candidate source has been located) keeps the typical cost
   * far below this ceiling. A source whose air path exceeds the budget reads as unreachable
   * (same as being walled off).</p>
   */
  private static final int MAX_FLOOD_BLOCKS = 4096;

  /**
   * Multiplier applied to the total HVAC offset when the query position can see the sky
   * (i.e. has no roof overhead). Conditioned air dissipates rapidly outdoors, so heaters
   * and coolers are only 30% as effective. Uses the O(1) heightmap check
   * {@code World.canSeeSky()}, so there is no performance cost.
   */
  private static final float OUTDOOR_ATTENUATION_FACTOR = 0.3f;

  /**
   * Per-dimension cache of chunk temperature data, keyed by the dimension ID of the world.
   */
  private static final Map<Integer, Map<Long, ChunkTempData>> dimensionCaches = new ConcurrentHashMap<>();

  /**
   * Retrieves the temperature in degrees Fahrenheit at the given position. This is the
   * canonical, instantaneous reading: biome baseline plus capped HVAC offset, no smoothing.
   * Callers that want a smoothed display value should own a {@link TemperatureSmoother}
   * instance and feed this reading into it — smoothing state is intentionally not held
   * inside this manager because it must be per-consumer (the HUD's smoother needs different
   * dynamics than a thermostat's thermal-mass blend, and sharing one static smoother across
   * callers / threads corrupts it).
   *
   * <p>Biome baseline mapping: {@code tempF = biomeTemp * 90 - 4} — approximately -4°F at
   * biome temp 0.0, ~68°F at 0.8 (plains), and ~176°F at 2.0 (desert).</p>
   *
   * @param world the world instance (must be server-side or single-player integrated server)
   * @param pos   the block position to query temperature at
   *
   * @return the raw temperature in degrees Fahrenheit (baseline + capped HVAC offset)
   */
  public static float getTemperatureAt(World world, BlockPos pos) {
    if (DEBUG_LOGGING) {
      long now = System.currentTimeMillis();
      debugThisPass = (now - lastDebugLogMs >= DEBUG_LOG_INTERVAL_MS);
      if (debugThisPass) {
        lastDebugLogMs = now;
      }
    }

    float baseline = getCachedBaseline(world, pos);
    float currentOffset = calculateHvacOffset(world, pos);

    if (debugThisPass) {
      LOGGER.info(String.format("[HVAC-DEBUG] pos=%s baseline=%.1f rawOffset=%.1f final=%.1f",
          pos, baseline, currentOffset, baseline + currentOffset));
      debugThisPass = false;
    }

    return baseline + currentOffset;
  }

  /**
   * Reads the ambient temperature for a wall- or ceiling-mounted device that occupies
   * {@code devicePos}.
   *
   * <p><b>Why this exists.</b> A wall-mounted thermostat's {@code pos} sits flush against the
   * wall the device is mounted on. The air-path flood fill in {@link #calculateHvacOffset}
   * starts from {@code devicePos} and expands into whatever open air the device is exposed to —
   * around corners and through doorways — so a single query already reads the room the device
   * actually serves; no extra forward sampling is needed.</p>
   *
   * <p>The {@code roomDirection} parameter is retained for API/back-compat with callers but is
   * no longer used now that propagation follows real air paths rather than a straight-line
   * raycast.</p>
   *
   * @param world         the world instance
   * @param devicePos     position of the wall-mounted device
   * @param roomDirection unused; retained for API compatibility (may be {@code null})
   * @return the ambient temperature in degrees Fahrenheit
   */
  public static float getAmbientTemperatureAt(World world, BlockPos devicePos,
      EnumFacing roomDirection) {
    if (DEBUG_LOGGING) {
      long now = System.currentTimeMillis();
      debugThisPass = (now - lastDebugLogMs >= DEBUG_LOG_INTERVAL_MS);
      if (debugThisPass) {
        lastDebugLogMs = now;
      }
    }

    float baseline = getCachedBaseline(world, devicePos);
    // The flood fill in calculateHvacOffset already walks the open air around this device —
    // around corners and through doorways — starting from devicePos, so a single query reads
    // the air the device is actually exposed to. The legacy forward-sampling walk through
    // roomDirection is no longer needed now that propagation follows real air paths.
    float bestOffset = calculateHvacOffset(world, devicePos);

    if (debugThisPass) {
      LOGGER.info(String.format(
          "[HVAC-AMBIENT] pos=%s dir=%s baseline=%.1f bestOffset=%.1f temp=%.1f",
          devicePos, roomDirection, baseline, bestOffset, baseline + bestOffset));
      debugThisPass = false;
    }

    return baseline + bestOffset;
  }

  /**
   * Returns the biome baseline temperature at the given position (no HVAC contribution).
   * Exposed so consumers that maintain their own smoothing state (see
   * {@link TemperatureSmoother}) can track HVAC offset separately from baseline drift —
   * needed because a player crossing a biome boundary shifts baseline by tens of degrees
   * and would otherwise look like a huge HVAC swing to a naive smoother.
   *
   * @param world the world instance
   * @param pos   the block position to query
   * @return the biome baseline temperature in degrees Fahrenheit
   */
  public static float getBaselineAt(World world, BlockPos pos) {
    return getCachedBaseline(world, pos);
  }

  /**
   * Returns the cached biome baseline temperature for the chunk containing the given position.
   * The baseline is recalculated when the cache entry expires.
   */
  private static float getCachedBaseline(World world, BlockPos pos) {
    Map<Long, ChunkTempData> cache = getOrCreateCache(world);
    long chunkKey = chunkKey(pos);
    long currentTick = world.getTotalWorldTime();

    // Periodic eviction of stale entries to prevent unbounded cache growth
    if (currentTick - lastEvictionTick >= EVICTION_INTERVAL_TICKS) {
      lastEvictionTick = currentTick;
      cache.values().removeIf(d -> (currentTick - d.timestamp) >= EVICTION_MAX_AGE_TICKS);
    }

    ChunkTempData data = cache.get(chunkKey);
    if (data != null && (currentTick - data.timestamp) < CACHE_LIFETIME_TICKS) {
      return data.temperature;
    }

    float biomeTemp = world.getBiome(pos).getTemperature(pos);
    float baselineTempF = biomeTemp * 90.0f - 4.0f;
    cache.put(chunkKey, new ChunkTempData(baselineTempF, currentTick));
    return baselineTempF;
  }

  /**
   * Returns whether any active {@link IHvacUnit} tile entity exists within the given radius of the
   * specified position. This is useful for determining if a position is within the influence zone
   * of any HVAC equipment without computing a full temperature value.
   *
   * <p>This method scans the tile entity maps of nearby chunks rather than iterating over
   * individual block positions, making it efficient even for large radii.</p>
   *
   * @param world  the world instance
   * @param pos    the center position to search from
   * @param radius the search radius in blocks
   *
   * @return {@code true} if at least one active HVAC unit exists within the radius,
   *     {@code false} otherwise
   */
  public static boolean isNearActiveHvac(World world, BlockPos pos, int radius) {
    int minCX = (pos.getX() - radius) >> 4;
    int maxCX = (pos.getX() + radius) >> 4;
    int minCZ = (pos.getZ() - radius) >> 4;
    int maxCZ = (pos.getZ() + radius) >> 4;
    double radiusSq = (double) radius * radius;

    for (int cx = minCX; cx <= maxCX; cx++) {
      for (int cz = minCZ; cz <= maxCZ; cz++) {
        if (!world.isChunkGeneratedAt(cx, cz)) {
          continue;
        }
        Chunk chunk = world.getChunk(cx, cz);
        for (TileEntity te : chunk.getTileEntityMap().values()) {
          if (te instanceof IHvacUnit && ((IHvacUnit) te).isHvacActive()) {
            if (te.getPos().distanceSq(pos) <= radiusSq) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns whether any {@link IHvacUnit} tile entity (active or not) exists within the given
   * radius of the specified position. This is used by the HUD to show the temperature display
   * whenever the player is near HVAC equipment, even if it's powered off.
   *
   * @param world  the world instance
   * @param pos    the center position to search from
   * @param radius the search radius in blocks
   *
   * @return {@code true} if at least one HVAC unit exists within the radius
   */
  public static boolean isNearAnyHvac(World world, BlockPos pos, int radius) {
    int minCX = (pos.getX() - radius) >> 4;
    int maxCX = (pos.getX() + radius) >> 4;
    int minCZ = (pos.getZ() - radius) >> 4;
    int maxCZ = (pos.getZ() + radius) >> 4;
    double radiusSq = (double) radius * radius;

    for (int cx = minCX; cx <= maxCX; cx++) {
      for (int cz = minCZ; cz <= maxCZ; cz++) {
        if (!world.isChunkGeneratedAt(cx, cz)) {
          continue;
        }
        Chunk chunk = world.getChunk(cx, cz);
        for (TileEntity te : chunk.getTileEntityMap().values()) {
          if (te instanceof IHvacUnit && te.getPos().distanceSq(pos) <= radiusSq) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Invalidates the cached temperature data for the specified chunk. This should be called when an
   * HVAC block is placed or broken to force an immediate recalculation on the next temperature
   * query.
   *
   * @param world  the world instance
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   */
  public static void invalidateChunk(World world, int chunkX, int chunkZ) {
    Map<Long, ChunkTempData> cache = getOrCreateCache(world);
    long key = chunkKeyFromCoords(chunkX, chunkZ);
    cache.remove(key);
  }

  // Biome baseline mapping: tempF = biomeTemp * 90 - 4
  //   0.0 (Ice Plains) -> -4°F    (freezing)
  //   0.25 (Taiga)     -> 18.5°F  (cold)
  //   0.5 (Forest)     -> 41°F    (cool)
  //   0.8 (Plains)     -> 68°F    (comfortable)
  //   1.0 (Jungle)     -> 86°F    (warm)
  //   1.5 (Mesa)       -> 131°F   (hot - arid)
  //   2.0 (Desert)     -> 176°F   (scorching)
  // The high end is intentionally extreme to make coolers meaningful in hot biomes.

  /**
   * Computes the distance-weighted HVAC temperature offset at the given position by following
   * the actual <em>air path</em> from the query point to each active {@link IHvacUnit}.
   *
   * <p><b>Why a flood fill.</b> The previous implementation weighted each source by a
   * straight-line raycast that counted intervening solid blocks. That punched through interior
   * geometry on the direct line: a vent and a thermostat in the same room but separated by a
   * corner or a short partition wall read as "1–2 walls apart" (30%/9% of the contribution),
   * so a thermostat just around the corner from its own vent never felt it. This method instead
   * flood-fills outward through open air from {@code pos} (see {@link #floodFillAirDistances})
   * and measures each source by its shortest air-path distance. Air bends around corners and
   * through doorways but is stopped cold by a sealed wall — which is also exactly what makes a
   * sealed adjacent room a separate thermal zone.</p>
   *
   * <p><b>Stacking / extreme biomes.</b> Each reachable active source — <em>including vent
   * relays</em>, which are the delivery points for rooftop/remote equipment — counts toward the
   * per-direction offset cap (see {@link #computeOffsetCap}). This is the fix for "cooling never
   * keeps up in the desert": a single rooftop cooler feeding three vents now provides three
   * contributors' worth of capacity instead of being pinned at the single-unit floor, so
   * stacking units to overcome an extreme biome actually works. The absolute hard cap plus the
   * thermostat's own proportional throttling keep this from running away.</p>
   *
   * <p><b>Performance:</b> one bounded flood fill (≤ {@link #MAX_FLOOD_BLOCKS} block-state
   * lookups) plus a cheap chunk-TE scan, per query, at the 1–2 second query cadence. World/chunk
   * access must remain on the server thread (not thread-safe).</p>
   *
   * @param world the world instance
   * @param pos   the position to calculate the offset for
   *
   * @return the air-path-weighted HVAC temperature offset in degrees Fahrenheit
   */
  private static float calculateHvacOffset(World world, BlockPos pos) {
    // Gather active HVAC sources in the surrounding chunks, culled by straight-line distance.
    // (Air-path distance is always >= straight-line distance, so anything outside the largest
    // falloff radius in a straight line can never be in range by air either.)
    int centerCX = pos.getX() >> 4;
    int centerCZ = pos.getZ() >> 4;
    double maxDistForCull = Math.max(UNIT_MAX_EFFECT_DISTANCE, VENT_MAX_EFFECT_DISTANCE);
    double maxCullDistSq = maxDistForCull * maxDistForCull;

    List<TileEntity> sources = new ArrayList<>();
    for (int cx = centerCX - CHUNK_SCAN_RADIUS; cx <= centerCX + CHUNK_SCAN_RADIUS; cx++) {
      for (int cz = centerCZ - CHUNK_SCAN_RADIUS; cz <= centerCZ + CHUNK_SCAN_RADIUS; cz++) {
        if (!world.isChunkGeneratedAt(cx, cz)) {
          continue;
        }
        Chunk chunk = world.getChunk(cx, cz);
        for (TileEntity te : chunk.getTileEntityMap().values()) {
          if (!(te instanceof IHvacUnit) || !((IHvacUnit) te).isHvacActive()) {
            continue;
          }
          if (te.getPos().distanceSq(pos) <= maxCullDistSq) {
            sources.add(te);
          }
        }
      }
    }
    if (sources.isEmpty()) {
      return 0.0f;
    }

    // Map each candidate air cell (the six neighbours of every source block — the source block
    // itself is solid and blows into the open cell beside it) to the index of the source that
    // owns it. The flood uses this to early-stop: a source is "located" the moment BFS first
    // reaches any of its neighbour cells (BFS visits in increasing-distance order, so the first
    // hit is already the shortest path). Once every source that CAN be located has been, the
    // fill returns — so the typical cost is far below the MAX_FLOOD_BLOCKS ceiling. We only pay
    // the full budget when a source is genuinely unreachable within range.
    Map<Long, Integer> targetCellToSource = new HashMap<>();
    for (int i = 0; i < sources.size(); i++) {
      BlockPos sp = sources.get(i).getPos();
      for (EnumFacing dir : EnumFacing.values()) {
        // putIfAbsent: if two sources are adjacent and share a neighbour cell, the first owner
        // wins — the other is still located via its own remaining neighbours.
        targetCellToSource.putIfAbsent(sp.offset(dir).toLong(), i);
      }
    }

    // Flood-fill open air from the query position so we can measure each source by air path.
    Map<Long, Integer> reachable =
        floodFillAirDistances(world, pos, targetCellToSource, sources.size());

    float heatingOffset = 0.0f;
    float coolingOffset = 0.0f;
    // Every reachable delivery point (vent OR direct unit) counts toward the cap — that's what
    // lets stacked vents/RTUs raise the ceiling enough to condition an extreme biome.
    int heatingContributors = 0;
    int coolingContributors = 0;

    for (TileEntity te : sources) {
      IHvacUnit unit = (IHvacUnit) te;
      int pathDist = shortestAirPathTo(reachable, te.getPos());
      if (pathDist < 0) {
        continue; // no open-air path within budget — walled off / unreachable
      }
      boolean isVent = te instanceof TileEntityHvacVentRelay;
      double fullDist = isVent ? VENT_FULL_EFFECT_DISTANCE : UNIT_FULL_EFFECT_DISTANCE;
      double maxDist = isVent ? VENT_MAX_EFFECT_DISTANCE : UNIT_MAX_EFFECT_DISTANCE;
      float distWeight = calculateDistanceWeight(pathDist, fullDist, maxDist);
      if (distWeight <= 0.0f) {
        continue;
      }

      float contribution = unit.getTemperatureContribution();
      float weightedOffset = Math.abs(contribution) * distWeight;

      if (debugThisPass) {
        String type = isVent ? "VENT" : (contribution > 0 ? "HEATER" : "COOLER");
        LOGGER.info(String.format(
            "[HVAC-DEBUG]   %s at %s airPath=%d distW=%.2f contrib=%.1f weighted=%.1f",
            type, te.getPos(), pathDist, distWeight, contribution, weightedOffset));
      }

      if (contribution > 0) {
        heatingOffset += weightedOffset;
        if (weightedOffset > 0.0f) {
          heatingContributors++;
        }
      } else if (contribution < 0) {
        coolingOffset += weightedOffset;
        if (weightedOffset > 0.0f) {
          coolingContributors++;
        }
      }
    }

    // Clamp each direction. Cap = BASE_CAP + per-extra-contributor bonus, hard-clamped at the
    // absolute ceiling. One contributor sits at BASE_CAP (50°F); stacked setups push further
    // to handle extreme climates. Bounded by the hard cap so a wall of vents can't produce a
    // thousand-degree offset, and the thermostat's proportional control throttles long before
    // the cap matters in normal operation.
    float effectiveHeatingCap = computeOffsetCap(heatingContributors);
    float effectiveCoolingCap = computeOffsetCap(coolingContributors);
    heatingOffset = Math.min(heatingOffset, effectiveHeatingCap);
    coolingOffset = Math.min(coolingOffset, effectiveCoolingCap);

    // Outdoor attenuation: conditioned air dissipates quickly without a roof
    float totalOffset = heatingOffset - coolingOffset;
    boolean isOutdoors = totalOffset != 0.0f && world.canSeeSky(pos);
    if (isOutdoors) {
      totalOffset *= OUTDOOR_ATTENUATION_FACTOR;
    }

    if (debugThisPass) {
      LOGGER.info(String.format(
          "[HVAC-DEBUG]   totals: heating=%.1f (cap=%.0f, %d) cooling=%.1f (cap=%.0f, %d) outdoor=%s finalOffset=%.1f",
          heatingOffset, effectiveHeatingCap, heatingContributors, coolingOffset,
          effectiveCoolingCap, coolingContributors, isOutdoors, totalOffset));
    }

    return totalOffset;
  }

  /**
   * Breadth-first flood fill from {@code start} through open (non-solid) air, returning a map
   * from packed block position ({@link BlockPos#toLong()}) to the shortest step-distance from
   * the start. Expansion is bounded three ways: it stops as soon as every cell in
   * {@code targets} has been located (the early-stop — the common case, keeping cost well below
   * the ceiling), it never visits more than {@link #MAX_FLOOD_BLOCKS} cells, and it never
   * expands a cell already at the largest falloff radius (no source past that distance could
   * contribute anyway). The start cell is always seeded at distance 0 even when it is the solid
   * block a wall-mounted thermostat occupies; only its non-solid neighbours are enqueued, so the
   * fill represents the air the device is exposed to.
   *
   * @param targetCellToSource maps each air cell the caller cares about to the index of the
   *                           source that owns it; used for the early-stop.
   * @param sourceCount        total number of sources; the fill returns once every locatable
   *                           source has been seen. A source that is never reached (sealed off,
   *                           or beyond the budget/range) leaves its cells absent from the map
   *                           and simply prevents the early-stop, falling back to the budget cap.
   */
  private static Map<Long, Integer> floodFillAirDistances(World world, BlockPos start,
      Map<Long, Integer> targetCellToSource, int sourceCount) {
    Map<Long, Integer> visited = new HashMap<>();
    ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    visited.put(start.toLong(), 0);
    queue.add(start);

    boolean[] sourceLocated = new boolean[sourceCount];
    int remainingSources = sourceCount;
    // The start cell itself could be a source's neighbour (e.g. a thermostat right beside a vent).
    Integer startOwner = targetCellToSource.get(start.toLong());
    if (startOwner != null) {
      sourceLocated[startOwner] = true;
      remainingSources--;
    }

    int maxStep = (int) Math.ceil(VENT_MAX_EFFECT_DISTANCE);

    while (!queue.isEmpty() && visited.size() < MAX_FLOOD_BLOCKS && remainingSources > 0) {
      BlockPos cur = queue.poll();
      int curDist = visited.get(cur.toLong());
      if (curDist >= maxStep) {
        continue;
      }
      for (EnumFacing dir : EnumFacing.values()) {
        BlockPos next = cur.offset(dir);
        long key = next.toLong();
        if (visited.containsKey(key)) {
          continue;
        }
        if (isSolidBlock(world.getBlockState(next))) {
          continue; // walls stop airflow — this is what separates sealed rooms into zones
        }
        visited.put(key, curDist + 1);
        Integer owner = targetCellToSource.get(key);
        if (owner != null && !sourceLocated[owner]) {
          sourceLocated[owner] = true;
          remainingSources--;
        }
        if (visited.size() >= MAX_FLOOD_BLOCKS || remainingSources <= 0) {
          break;
        }
        queue.add(next);
      }
    }
    return visited;
  }

  /**
   * Returns the shortest air-path step-distance from the flood-fill origin to the air cell the
   * given source blows into (the closest visited neighbour of {@code sourcePos}), or {@code -1}
   * if no air cell adjacent to the source was reached within the flood-fill budget. A source
   * block is itself solid, so we measure to the open cell next to it rather than to the block.
   */
  private static int shortestAirPathTo(Map<Long, Integer> reachable, BlockPos sourcePos) {
    int best = -1;
    Integer self = reachable.get(sourcePos.toLong());
    if (self != null) {
      best = self;
    }
    for (EnumFacing dir : EnumFacing.values()) {
      Integer d = reachable.get(sourcePos.offset(dir).toLong());
      if (d != null && (best < 0 || d + 1 < best)) {
        best = d + 1;
      }
    }
    return best;
  }

  /**
   * Computes the per-direction offset cap for the given number of active heater/cooler
   * units. Returns at least {@link #BASE_HVAC_OFFSET_CAP} (when zero or one unit), grows
   * linearly with extra units, and is hard-clamped at {@link #HVAC_OFFSET_HARD_CAP}.
   */
  private static float computeOffsetCap(int unitCount) {
    int extra = Math.max(0, unitCount - 1);
    float cap = BASE_HVAC_OFFSET_CAP + HVAC_OFFSET_PER_EXTRA_UNIT * extra;
    return Math.min(cap, HVAC_OFFSET_HARD_CAP);
  }

  /**
   * Calculates the distance weight for an HVAC unit's contribution. Returns 1.0 for distances
   * within fullEffectDist, linearly falls off to 0.0 at maxEffectDist, and returns 0.0 beyond.
   *
   * @param distance       the distance in blocks from the unit to the query position
   * @param fullEffectDist distance within which the unit has 100% effect
   * @param maxEffectDist  distance beyond which the unit has 0% effect
   *
   * @return the weight factor between 0.0 and 1.0
   */
  private static float calculateDistanceWeight(double distance, double fullEffectDist,
      double maxEffectDist) {
    if (distance <= fullEffectDist) {
      return 1.0f;
    }
    if (distance >= maxEffectDist) {
      return 0.0f;
    }
    return (float) ((maxEffectDist - distance) / (maxEffectDist - fullEffectDist));
  }

  /**
   * Returns whether the given block state represents a solid (non-air, non-replaceable) block
   * that counts as a wall for the attenuation raycast.
   *
   * @param state the block state to check
   *
   * @return {@code true} if the block is solid enough to attenuate HVAC airflow
   */
  private static boolean isSolidBlock(IBlockState state) {
    Material material = state.getMaterial();
    return material.isSolid() && !material.isReplaceable();
  }

  /**
   * Retrieves or creates the chunk temperature cache for the given world's dimension.
   *
   * @param world the world instance
   *
   * @return the chunk temperature cache map for this dimension
   */
  private static Map<Long, ChunkTempData> getOrCreateCache(World world) {
    int dimensionId = world.provider.getDimension();
    return dimensionCaches.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());
  }

  /**
   * Computes a chunk cache key from a block position by converting to chunk coordinates.
   *
   * @param pos the block position
   *
   * @return the chunk key as a long
   */
  private static long chunkKey(BlockPos pos) {
    return chunkKeyFromCoords(pos.getX() >> 4, pos.getZ() >> 4);
  }

  /**
   * Computes a chunk cache key from chunk coordinates using Minecraft's {@code ChunkPos} encoding.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   *
   * @return the chunk key as a long
   */
  private static long chunkKeyFromCoords(int chunkX, int chunkZ) {
    return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
  }

  /**
   * Internal data class that holds cached temperature information for a single chunk. Stores the
   * calculated temperature and the world tick at which it was computed.
   */
  private static class ChunkTempData {

    /**
     * The cached temperature value in degrees Fahrenheit.
     */
    final float temperature;

    /**
     * The world tick time at which this cache entry was computed.
     */
    final long timestamp;

    /**
     * Constructs a new chunk temperature data entry.
     *
     * @param temperature the temperature in degrees Fahrenheit
     * @param timestamp   the world tick time of computation
     */
    ChunkTempData(float temperature, long timestamp) {
      this.temperature = temperature;
      this.timestamp = timestamp;
    }
  }
}
