package com.micatechnologies.minecraft.csm.hvac;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Server-side temperature calculation engine that tracks per-chunk temperature data. Temperature is
 * computed from the biome baseline, modified by active HVAC tile entities ({@link IHvacUnit}) in the
 * chunk. Results are cached per chunk and recalculated every {@link #CACHE_LIFETIME_TICKS} ticks (2
 * seconds) to avoid expensive per-tick recomputation.
 *
 * <p><b>Granularity:</b> The biome baseline is cached per-chunk, but HVAC influence is calculated
 * per-position using distance weighting. This means two positions in the same chunk can have
 * different temperatures if they are different distances from HVAC equipment. A heater and cooler
 * in the same chunk will each dominate their local area rather than canceling out.</p>
 *
 * <p><b>Thread safety:</b> This class uses {@code ConcurrentHashMap} for its per-dimension cache
 * to allow safe concurrent access from the server tick thread (thermostat updates) and the
 * client render thread (HUD overlay) in single-player (integrated server).</p>
 *
 * @author Mica Technologies
 * @see IHvacUnit
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
   * Maximum total HVAC offset (positive or negative) that can be applied to a position's
   * temperature, in degrees Fahrenheit.
   */
  private static final float MAX_HVAC_OFFSET = 24.0f;

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
   * EMA blending factor when HVAC is actively pushing the temperature away from baseline
   * (ramping up). Fast enough to track the instantaneous offset in ~10-15 seconds, keeping
   * the HUD responsive and in agreement with the thermostat.
   */
  private static final float HUD_RAMP_FACTOR = 0.08f;

  /**
   * EMA blending factor when the player leaves an HVAC zone entirely (raw offset is zero
   * but the smoothed value is still significant). Converges to baseline in ~20-30 seconds,
   * simulating the transition from a conditioned space to the outdoors.
   */
  private static final float HUD_TRANSITION_FACTOR = 0.04f;

  /**
   * EMA blending factor when HVAC is still present but reduced (residual decay in progress).
   * Much slower than the transition factor to simulate the room holding its temperature
   * while the conditioned air lingers.
   * <ul>
   *   <li>30 seconds: ~83% of offset retained</li>
   *   <li>1 minute: ~70% retained</li>
   *   <li>2 minutes: ~49% retained</li>
   *   <li>5 minutes: ~16% retained</li>
   * </ul>
   */
  private static final float HUD_DECAY_FACTOR = 0.003f;

  /**
   * The last smoothed HVAC offset returned by the asymmetric EMA. Ramps quickly toward
   * the instantaneous offset when HVAC is active, decays slowly when HVAC turns off.
   * Transient — resets on restart.
   *
   * <p><b>Client-only:</b> This field is read/written exclusively by {@link #getTemperatureAt},
   * which is called from the client render thread (HUD overlay). Server-side code must use
   * {@link #getRawTemperatureAt} instead, which bypasses smoothing. Calling
   * {@code getTemperatureAt} from the server thread would corrupt this state.</p>
   */
  private static float lastSmoothedOffset = Float.NaN;


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
   * Retrieves the temperature in degrees Fahrenheit at the given position in the world. This is
   * the primary public API for the temperature system. The result is computed from the biome
   * baseline temperature, modified by any active HVAC units in the chunk, and cached for
   * {@link #CACHE_LIFETIME_TICKS} ticks.
   *
   * <p>The biome baseline is mapped to Fahrenheit using the formula:
   * {@code tempF = biomeTemp * 90 - 4}, which yields approximately -4°F at biome temp 0.0,
   * ~68°F at 0.8 (plains), and ~176°F at 2.0 (desert).</p>
   *
   * @param world the world instance (should be server-side)
   * @param pos   the block position to query temperature at
   *
   * @return the temperature in degrees Fahrenheit at the given position
   */
  public static float getTemperatureAt(World world, BlockPos pos) {
    // Check if this call should produce debug output (throttled to once per second)
    if (DEBUG_LOGGING) {
      long now = System.currentTimeMillis();
      debugThisPass = (now - lastDebugLogMs >= DEBUG_LOG_INTERVAL_MS);
      if (debugThisPass) {
        lastDebugLogMs = now;
      }
    }

    // Biome baseline is cached per-chunk (doesn't vary within a chunk)
    float baseline = getCachedBaseline(world, pos);
    // HVAC offset is the instantaneous contribution (capped at ±MAX_HVAC_OFFSET)
    float currentOffset = calculateHvacOffset(world, pos);
    // EMA smoothing for comfortable display — responsive enough to track the
    // instantaneous offset, keeping the HUD in agreement with the thermostat
    float smoothedOffset = applySmoothing(currentOffset);

    if (debugThisPass) {
      LOGGER.info(String.format("[HVAC-DEBUG] pos=%s baseline=%.1f rawOffset=%.1f smoothed=%.1f final=%.1f",
          pos, baseline, currentOffset, smoothedOffset, baseline + smoothedOffset));
      debugThisPass = false;
    }

    return baseline + smoothedOffset;
  }

  /**
   * Returns the temperature at the given position without any transition smoothing.
   * Use this for fixed sensors (thermostats) that should reflect the actual temperature
   * at their location rather than a player-relative smoothed value.
   *
   * @param world the world instance
   * @param pos   the block position to query temperature at
   *
   * @return the raw temperature in degrees Fahrenheit (baseline + HVAC offset, no smoothing)
   */
  public static float getRawTemperatureAt(World world, BlockPos pos) {
    float baseline = getCachedBaseline(world, pos);
    float currentOffset = calculateHvacOffset(world, pos);
    if (debugThisPass) {
      LOGGER.info(String.format("[HVAC-THERMOSTAT] pos=%s baseline=%.1f rawOffset=%.1f rawTemp=%.1f",
          pos, baseline, currentOffset, baseline + currentOffset));
    }
    return baseline + currentOffset;
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

  /**
   * Applies asymmetric EMA smoothing to the HVAC offset for HUD display. Uses a fast
   * blending factor ({@link #HUD_RAMP_FACTOR}) when HVAC is actively pushing the
   * temperature away from baseline, and a slow factor ({@link #HUD_DECAY_FACTOR}) when
   * the temperature is returning toward baseline (HVAC off or reduced power).
   *
   * <p>This produces realistic behavior: walking into an air-conditioned room, you feel
   * the temperature drop within 10-15 seconds. When the AC turns off, the room holds
   * its temperature for minutes — not snapping back instantly.</p>
   *
   * @param currentOffset the freshly calculated HVAC offset
   *
   * @return the smoothed offset value
   */
  private static float applySmoothing(float currentOffset) {
    if (Float.isNaN(lastSmoothedOffset)) {
      lastSmoothedOffset = currentOffset;
      return currentOffset;
    }

    // Three-rate smoothing:
    // 1. RAMP: HVAC actively pushing temp away from baseline — track quickly
    // 2. TRANSITION: player left HVAC zone (raw ≈ 0, smoothed significant) — moderate
    // 3. DECAY: HVAC still present but reduced/residual — hold temperature slowly
    boolean directionChanged =
        (currentOffset > 0.5f && lastSmoothedOffset < -0.5f)
            || (currentOffset < -0.5f && lastSmoothedOffset > 0.5f);

    float factor;
    if (directionChanged) {
      // Crossed from heating to cooling or vice versa — track quickly
      factor = HUD_RAMP_FACTOR;
    } else if (Math.abs(currentOffset) < 0.5f && Math.abs(lastSmoothedOffset) > 1.0f) {
      // Raw offset is zero but smoothed is significant — player left the HVAC zone
      // entirely (walked outside, moved to an unconditioned area). Transition to
      // baseline at a moderate rate (~20-30 seconds to converge).
      factor = HUD_TRANSITION_FACTOR;
    } else if (Math.abs(currentOffset) + 0.5f < Math.abs(lastSmoothedOffset)) {
      // HVAC still contributing but less than before (residual decay in progress,
      // or player moved slightly farther from vent). Hold temperature slowly.
      factor = HUD_DECAY_FACTOR;
    } else {
      // HVAC active and pushing — track quickly
      factor = HUD_RAMP_FACTOR;
    }

    lastSmoothedOffset += (currentOffset - lastSmoothedOffset) * factor;
    return lastSmoothedOffset;
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
          } else if (contribution < 0) {
            coolingOffset += weightedOffset;
          }

        }
      }
    }

    // Clamp each direction to the max offset
    heatingOffset = Math.min(heatingOffset, MAX_HVAC_OFFSET);
    coolingOffset = Math.min(coolingOffset, MAX_HVAC_OFFSET);

    // Outdoor attenuation: conditioned air dissipates quickly without a roof
    float totalOffset = heatingOffset - coolingOffset;
    boolean isOutdoors = totalOffset != 0.0f && world.canSeeSky(pos);
    if (isOutdoors) {
      totalOffset *= OUTDOOR_ATTENUATION_FACTOR;
    }

    if (debugThisPass) {
      LOGGER.info(String.format("[HVAC-DEBUG]   totals: heating=%.1f cooling=%.1f outdoor=%s finalOffset=%.1f",
          heatingOffset, coolingOffset, isOutdoors, totalOffset));
    }

    return totalOffset;
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
   * Calculates the wall attenuation factor for an HVAC unit's contribution by raycasting from the
   * unit to the target position and counting solid blocks in the path. Each solid block multiplies
   * the contribution by {@link #WALL_ATTENUATION_FACTOR} (0.3), so:
   * <ul>
   *   <li>0 walls: 100% contribution</li>
   *   <li>1 wall: 30% contribution</li>
   *   <li>2 walls: 9% contribution</li>
   *   <li>3+ walls: effectively zero</li>
   * </ul>
   *
   * <p>Uses a simple step-along-ray approach (Bresenham-like) to check blocks along the line
   * between the two positions. Skips the source and destination blocks themselves.</p>
   *
   * <p><b>Performance:</b> Checks ~N block states where N is the distance in blocks. With
   * typical HVAC distances of 3-16 blocks and 2-4 units, this adds 12-64 block state lookups
   * per temperature calculation (every 2 seconds). Negligible cost.</p>
   *
   * @param world the world instance
   * @param from  the HVAC unit position
   * @param to    the query position
   *
   * @return the wall attenuation multiplier between 0.0 and 1.0
   */
  private static float calculateWallAttenuation(World world, BlockPos from, BlockPos to) {
    // Step along the ray from source to target, counting solid blocks
    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();
    double dz = to.getZ() - from.getZ();
    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (length < 1.0) {
      return 1.0f;
    }

    // Normalize direction and step in 0.9-block increments to catch each block
    double stepSize = 0.9;
    int steps = (int) (length / stepSize);
    double sx = dx / length * stepSize;
    double sy = dy / length * stepSize;
    double sz = dz / length * stepSize;

    float attenuation = 1.0f;
    double cx = from.getX() + 0.5;
    double cy = from.getY() + 0.5;
    double cz = from.getZ() + 0.5;

    // Reuse a mutable BlockPos to avoid allocating a new object per raycast step.
    // Typical raycasts are 3-16 steps with 2-4 units — this saves 24-48 allocations
    // per temperature query.
    BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

    // Skip the first step (source block) and last step (target block)
    for (int i = 1; i < steps; i++) {
      cx += sx;
      cy += sy;
      cz += sz;
      checkPos.setPos(cx, cy, cz);
      // Skip if same as source or target
      if (checkPos.equals(from) || checkPos.equals(to)) {
        continue;
      }
      IBlockState state = world.getBlockState(checkPos);
      if (isSolidBlock(state)) {
        attenuation *= WALL_ATTENUATION_FACTOR;
        if (attenuation < 0.01f) {
          return 0.0f; // Early exit — 3+ walls, effectively zero
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
