package com.micatechnologies.minecraft.csm.hvac;

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

  /**
   * Distance in blocks within which a direct HVAC unit (heater/cooler) has full effect.
   */
  private static final double UNIT_FULL_EFFECT_DISTANCE = 4.0;

  /**
   * Distance in blocks beyond which a direct HVAC unit has no effect.
   */
  private static final double UNIT_MAX_EFFECT_DISTANCE = 12.0;

  /**
   * Distance in blocks within which a vent relay has full effect. Vents distribute
   * conditioned air through a room, so they have a wider full-effect zone than the
   * equipment itself.
   */
  private static final double VENT_FULL_EFFECT_DISTANCE = 6.0;

  /**
   * Distance in blocks beyond which a vent relay has no effect. Vents cover a room-sized
   * area (~24 blocks / ~72 feet diameter) which is realistic for a ceiling vent.
   */
  private static final double VENT_MAX_EFFECT_DISTANCE = 24.0;

  /**
   * Number of chunks in each direction to scan for HVAC units (2 = 5x5 grid around player).
   * Needs to cover VENT_MAX_EFFECT_DISTANCE (24 blocks = 1.5 chunks), so 2 is safe.
   */
  private static final int CHUNK_SCAN_RADIUS = 2;

  /**
   * Maximum distance (in blocks, in the {@code roomDirection}) to step away from a
   * wall-mounted device when sampling ambient temperature. Stops earlier if a solid
   * block is encountered. Three steps strikes the balance between picking up the room
   * temperature past the wall the device is flush against and not reaching into a
   * neighboring room through a doorway.
   */
  private static final int AMBIENT_SAMPLE_MAX_STEPS = 3;

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
   * Multiplier applied to the HVAC contribution for each solid block between the unit and the
   * query position. A value of 0.3 means each wall reduces the effect to 30% of what it was,
   * so one wall = 30%, two walls = 9%, three walls = 2.7% (effectively zero).
   */
  private static final float WALL_ATTENUATION_FACTOR = 0.3f;

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
   * {@code devicePos} with its sensor face pointing in {@code roomDirection}.
   *
   * <p><b>Why this exists.</b> A wall-mounted thermostat's {@code pos} sits flush against
   * the wall the device is mounted on. The wall-attenuation raycast in
   * {@link #calculateHvacOffset} reduces every HVAC source's contribution to that pos by
   * 0.3× per intervening solid block — and worse, the per-direction offset cap is gated on
   * the number of heater/cooler units whose final weighted contribution is &gt; 0. Distant
   * units that the room itself would receive via vents and air mixing get attenuated to
   * effectively zero when sampled at the wall surface, collapsing the cap to its
   * single-unit floor (50°F) and freezing the reading well below the room's actual
   * temperature.</p>
   *
   * <p>Sampling forward through {@code roomDirection} (up to {@link #AMBIENT_SAMPLE_MAX_STEPS}
   * blocks, stopping at the first solid block) lets the device read the temperature of the
   * air it's exposed to rather than the air right at the wall. The strongest offset across
   * those sample points is returned, since a heater pumping into one part of the room and
   * a cooler into another shouldn't average to "comfortable" when neither is — and a wall
   * sensor would feel whichever flow actually reaches it.</p>
   *
   * @param world         the world instance
   * @param devicePos     position of the wall-mounted device
   * @param roomDirection direction from the device's pos into the room (usually the device's
   *                      {@code FACING} property). May be {@code null}, in which case only
   *                      the device's own pos is sampled.
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
    float bestOffset = calculateHvacOffset(world, devicePos);

    if (roomDirection != null) {
      for (int step = 1; step <= AMBIENT_SAMPLE_MAX_STEPS; step++) {
        BlockPos samplePos = devicePos.offset(roomDirection, step);
        if (isSolidBlock(world.getBlockState(samplePos))) {
          break;
        }
        float offset = calculateHvacOffset(world, samplePos);
        if (Math.abs(offset) > Math.abs(bestOffset)) {
          bestOffset = offset;
        }
      }
    }

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
   * Scans nearby chunks (3x3 grid) for active {@link IHvacUnit} tile entities and calculates a
   * distance-weighted temperature offset at the given position.
   *
   * <p>Each active unit's contribution is scaled by distance (full effect within 4-6 blocks
   * depending on type, linear falloff to zero at 12-24 blocks).</p>
   *
   * <p>This provides per-position granularity without per-block temperature tracking. A heater
   * and cooler in the same chunk will each dominate their nearby area: standing next to the
   * heater you feel mostly warmth, near the cooler mostly cold, and in between a blend.</p>
   *
   * <p><b>Performance:</b> Scans 9 chunk TE maps (typically 0–20 TEs each), performs a
   * distance calculation per active HVAC unit found. Total cost: microseconds. World/chunk
   * access must remain on the server thread (not thread-safe).</p>
   *
   * @param world the world instance
   * @param pos   the position to calculate the offset for
   *
   * @return the distance-weighted HVAC temperature offset in degrees Fahrenheit
   */
  private static float calculateHvacOffset(World world, BlockPos pos) {
    int centerCX = pos.getX() >> 4;
    int centerCZ = pos.getZ() >> 4;

    float heatingOffset = 0.0f;
    float coolingOffset = 0.0f;
    // Count heater/cooler equipment (not vents) separately for the cap calculation. The cap
    // is gated on actual energy sources, not on delivery points: 50 vents + 1 heater is one
    // "unit's worth" of capacity, not 51.
    int heatingUnits = 0;
    int coolingUnits = 0;
    // Use the larger of the two max distances for the initial distance-squared cull
    double maxDistForCull = Math.max(UNIT_MAX_EFFECT_DISTANCE, VENT_MAX_EFFECT_DISTANCE);
    double maxCullDistSq = maxDistForCull * maxDistForCull;

    for (int cx = centerCX - CHUNK_SCAN_RADIUS; cx <= centerCX + CHUNK_SCAN_RADIUS; cx++) {
      for (int cz = centerCZ - CHUNK_SCAN_RADIUS; cz <= centerCZ + CHUNK_SCAN_RADIUS; cz++) {
        if (!world.isChunkGeneratedAt(cx, cz)) {
          continue;
        }
        Chunk chunk = world.getChunk(cx, cz);
        for (TileEntity te : chunk.getTileEntityMap().values()) {
          if (!(te instanceof IHvacUnit)) {
            continue;
          }
          IHvacUnit unit = (IHvacUnit) te;
          if (!unit.isHvacActive()) {
            continue;
          }

          double distSq = te.getPos().distanceSq(pos);
          if (distSq > maxCullDistSq) {
            continue;
          }

          double dist = Math.sqrt(distSq);
          boolean isVent = te instanceof TileEntityHvacVentRelay;

          // Use different distance curves for vents vs direct units
          double fullDist = isVent ? VENT_FULL_EFFECT_DISTANCE : UNIT_FULL_EFFECT_DISTANCE;
          double maxDist = isVent ? VENT_MAX_EFFECT_DISTANCE : UNIT_MAX_EFFECT_DISTANCE;
          float distWeight = calculateDistanceWeight(dist, fullDist, maxDist);

          // Both vents and direct units use wall attenuation — walls block airflow
          // between rooms, which is the basis of zone separation.
          float wallWeight = calculateWallAttenuation(world, te.getPos(), pos);
          float weight = distWeight * wallWeight;

          float contribution = unit.getTemperatureContribution();
          float weightedOffset = Math.abs(contribution) * weight;

          if (debugThisPass) {
            String type = isVent ? "VENT" : (contribution > 0 ? "HEATER" : "COOLER");
            LOGGER.info(String.format("[HVAC-DEBUG]   %s at %s dist=%.1f distW=%.2f wallW=%.2f contrib=%.1f weighted=%.1f",
                type, te.getPos(), dist, distWeight, wallWeight, contribution, weightedOffset));
          }

          if (contribution > 0) {
            heatingOffset += weightedOffset;
            // Only count actual heater units toward the cap, not vent relays. Vents
            // distribute heat from a heater; they don't supply additional heat capacity.
            if (weightedOffset > 0.0f && !isVent) {
              heatingUnits++;
            }
          } else if (contribution < 0) {
            coolingOffset += weightedOffset;
            if (weightedOffset > 0.0f && !isVent) {
              coolingUnits++;
            }
          }

        }
      }
    }

    // Clamp each direction. Cap = BASE_CAP + per-extra-unit bonus, hard-clamped at the
    // absolute ceiling. Single-unit systems sit at BASE_CAP (50°F); multi-unit setups can
    // push further to handle extreme climates (3 units → 100°F, 5 units → 150°F). Vents do
    // NOT increase the cap — that prevents the runaway where 50 vents stack to a ~1500°F
    // raw offset and oscillate the thermostat.
    float effectiveHeatingCap = computeOffsetCap(heatingUnits);
    float effectiveCoolingCap = computeOffsetCap(coolingUnits);
    heatingOffset = Math.min(heatingOffset, effectiveHeatingCap);
    coolingOffset = Math.min(coolingOffset, effectiveCoolingCap);

    // Outdoor attenuation: conditioned air dissipates quickly without a roof
    float totalOffset = heatingOffset - coolingOffset;
    boolean isOutdoors = totalOffset != 0.0f && world.canSeeSky(pos);
    if (isOutdoors) {
      totalOffset *= OUTDOOR_ATTENUATION_FACTOR;
    }

    if (debugThisPass) {
      LOGGER.info(String.format("[HVAC-DEBUG]   totals: heating=%.1f (cap=%.0f, %d units) cooling=%.1f (cap=%.0f, %d units) outdoor=%s finalOffset=%.1f",
          heatingOffset, effectiveHeatingCap, heatingUnits, coolingOffset, effectiveCoolingCap, coolingUnits, isOutdoors, totalOffset));
    }

    return totalOffset;
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
   * Calculates the wall attenuation factor for an HVAC unit's contribution between two
   * positions. Tries several candidate paths (direct line, plus a few elevated variants)
   * and returns the BEST attenuation — i.e. the path that finds the fewest walls. This
   * approximates the way air actually flows: it doesn't punch through walls in a straight
   * line, it rises and finds openings around obstacles.
   *
   * <p>Concrete example: a vent under a balcony enclosure with a player to the side.
   * The direct line from vent to player crosses the balcony's vertical wall plus the
   * balcony floor (2 walls → 9% contribution). But a path that starts a few blocks
   * above the vent — simulating air rising out of the enclosure's open top before
   * heading sideways — finds 0 walls and delivers the full contribution. Taking the
   * MAX across paths is the cheap stand-in for an actual flood-fill or pathfinding
   * solution.</p>
   *
   * <p><b>Performance:</b> ~5 paths × ~N block lookups per path, where N is the distance
   * in blocks. For 3-16 block distances and 2-4 contributors per query, this adds
   * 60-320 lookups per temperature calculation (every 2 seconds). Still microseconds.</p>
   *
   * @param world the world instance
   * @param from  the HVAC unit position
   * @param to    the query position
   *
   * @return the best wall attenuation multiplier across all candidate paths, between 0 and 1
   */
  private static float calculateWallAttenuation(World world, BlockPos from, BlockPos to) {
    // Direct line first — usually fine when nothing's in the way, and lets us early-exit
    // if it already hits 100% so we don't run extra raycasts for nothing.
    float best = attenuationForPath(world, from, to);
    if (best >= 0.999f) {
      return best;
    }

    // Elevated start: simulates air rising off the vent before moving sideways. Catches
    // the "vent enclosed under a balcony / overhang" case where the balcony's floor is
    // an obstacle on the direct line but disappears once you go a couple blocks above.
    best = Math.max(best, attenuationForPath(world, from.up(), to));
    if (best >= 0.999f) return best;
    best = Math.max(best, attenuationForPath(world, from.up(2), to));
    if (best >= 0.999f) return best;

    // Elevated end: simulates air arriving at the player from above (e.g. coming over
    // an interior partition rather than punching through it).
    best = Math.max(best, attenuationForPath(world, from, to.up()));
    if (best >= 0.999f) return best;

    // Both elevated — handles symmetric obstructions where neither end alone is enough.
    best = Math.max(best, attenuationForPath(world, from.up(), to.up()));
    return best;
  }

  /**
   * Computes the wall-count attenuation along a single ray. Returns 1.0 when the path is
   * unobstructed and {@link #WALL_ATTENUATION_FACTOR}^N when N walls are crossed, with an
   * early exit when attenuation drops below ~1% (≥ 3 walls).
   */
  private static float attenuationForPath(World world, BlockPos from, BlockPos to) {
    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();
    double dz = to.getZ() - from.getZ();
    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (length < 1.0) {
      return 1.0f;
    }

    double stepSize = 0.9;
    int steps = (int) (length / stepSize);
    double sx = dx / length * stepSize;
    double sy = dy / length * stepSize;
    double sz = dz / length * stepSize;

    float attenuation = 1.0f;
    double cx = from.getX() + 0.5;
    double cy = from.getY() + 0.5;
    double cz = from.getZ() + 0.5;

    BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

    for (int i = 1; i < steps; i++) {
      cx += sx;
      cy += sy;
      cz += sz;
      checkPos.setPos(cx, cy, cz);
      if (checkPos.equals(from) || checkPos.equals(to)) {
        continue;
      }
      IBlockState state = world.getBlockState(checkPos);
      if (isSolidBlock(state)) {
        attenuation *= WALL_ATTENUATION_FACTOR;
        if (attenuation < 0.01f) {
          return 0.0f;
        }
      }
    }
    return attenuation;
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
