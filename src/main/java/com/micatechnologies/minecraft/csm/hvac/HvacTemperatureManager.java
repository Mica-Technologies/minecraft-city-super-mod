package com.micatechnologies.minecraft.csm.hvac;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Server-side temperature calculation engine that tracks per-chunk temperature data. Temperature is
 * computed from the biome baseline, modified by active HVAC tile entities ({@link IHvacUnit}) in the
 * chunk. Results are cached per chunk and recalculated every {@link #CACHE_LIFETIME_TICKS} ticks (5
 * seconds) to avoid expensive per-tick recomputation.
 *
 * <p><b>Granularity:</b> The biome baseline is cached per-chunk, but HVAC influence is calculated
 * per-position using distance weighting. This means two positions in the same chunk can have
 * different temperatures if they are different distances from HVAC equipment. A heater and cooler
 * in the same chunk will each dominate their local area rather than canceling out.</p>
 *
 * <p><b>Thread safety:</b> This class uses a per-dimension cache map. All access is expected to
 * occur on the server thread. If accessed from multiple threads, external synchronization is
 * required.</p>
 *
 * @author Mica Technologies
 * @see IHvacUnit
 * @since 2026.4
 */
public class HvacTemperatureManager {

  /**
   * Number of ticks between cache recalculations. At 20 TPS this equals 2 seconds. Kept short
   * so temperature changes are visible quickly when moving between chunks or toggling HVAC.
   */
  private static final long CACHE_LIFETIME_TICKS = 40L;

  /**
   * Maximum temperature contribution from a single HVAC unit at point-blank range, in °F.
   */
  private static final float MAX_SINGLE_UNIT_OFFSET = 15.0f;

  /**
   * Maximum total HVAC offset (positive or negative) that can be applied to a position's
   * temperature, in degrees Fahrenheit.
   */
  private static final float MAX_HVAC_OFFSET = 40.0f;

  /**
   * Distance in blocks within which an HVAC unit has full effect (100% contribution).
   */
  private static final double FULL_EFFECT_DISTANCE = 3.0;

  /**
   * Distance in blocks beyond which an HVAC unit has no effect (0% contribution).
   */
  private static final double MAX_EFFECT_DISTANCE = 16.0;

  /**
   * Number of chunks in each direction to scan for HVAC units (1 = 3x3 grid around player).
   */
  private static final int CHUNK_SCAN_RADIUS = 1;

  /**
   * Duration in milliseconds over which temperature transitions gradually when HVAC units
   * turn on or off. Configurable constant — default 10 minutes (600,000 ms). Uses
   * System.currentTimeMillis() timestamps, not world ticks, so transitions are smooth
   * regardless of TPS. Not persisted in NBT — resets on restart.
   */
  private static final long TRANSITION_DURATION_MS = 600_000L;

  /**
   * Single transition tracker for the most recently queried position. Since the HUD typically
   * queries one position (the player's), a single tracker is sufficient. When the player moves,
   * the tracker detects the position change and fast-forwards to the current offset to avoid
   * stale transitions from a previous location. Transient — not saved to disk.
   */
  /**
   * Grid size for position rounding in the transition tracker. The player must move at least
   * this many blocks before the transition resets. Prevents micro-movement from constantly
   * resetting the gradual transition.
   */
  private static final int TRANSITION_GRID_SIZE = 4;

  private static TransitionData activeTransition = null;
  private static long activeTransitionGridKey = Long.MIN_VALUE;

  /**
   * Multiplier applied to the HVAC contribution for each solid block between the unit and the
   * query position. A value of 0.3 means each wall reduces the effect to 30% of what it was,
   * so one wall = 30%, two walls = 9%, three walls = 2.7% (effectively zero).
   */
  private static final float WALL_ATTENUATION_FACTOR = 0.3f;

  /**
   * Maximum number of blocks to scan upward when checking for a roof in the indoor detection
   * algorithm.
   */
  private static final int INDOOR_SCAN_HEIGHT = 8;

  /**
   * Maximum number of blocks to scan in each cardinal direction when checking for walls in the
   * indoor detection algorithm.
   */
  private static final int INDOOR_SCAN_HORIZONTAL = 8;

  /**
   * Per-dimension cache of chunk temperature data, keyed by the dimension ID of the world.
   */
  private static final Map<Integer, Map<Long, ChunkTempData>> dimensionCaches = new HashMap<>();

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
    // Biome baseline is cached per-chunk (doesn't vary within a chunk)
    float baseline = getCachedBaseline(world, pos);
    // HVAC offset is calculated per-position using distance weighting (cheap)
    float currentOffset = calculateHvacOffset(world, pos);
    // Apply gradual transition when offset changes (unit on/off)
    float smoothedOffset = applyTransition(world, pos, currentOffset);
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

    ChunkTempData data = cache.get(chunkKey);
    if (data != null && (currentTick - data.timestamp) < CACHE_LIFETIME_TICKS) {
      return data.temperature;
    }

    float biomeTemp = world.getBiome(pos).getTemperature(pos);
    float baselineTempF = biomeTemp * 90.0f - 4.0f;
    cache.put(chunkKey, new ChunkTempData(baselineTempF, currentTick,
        isIndoors(world, pos)));
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
   * Applies a gradual transition when the HVAC offset at a position changes. Instead of
   * snapping instantly from the old temperature to the new one, this lerps over
   * {@link #TRANSITION_DURATION_MS} using system clock timestamps. Zero computation cost
   * beyond a map lookup and a single lerp. Not persisted — resets on restart.
   *
   * @param world         the world instance
   * @param pos           the query position
   * @param currentOffset the freshly calculated HVAC offset
   *
   * @return the smoothed offset value, transitioning from the previous offset
   */
  private static float applyTransition(World world, BlockPos pos, float currentOffset) {
    long now = System.currentTimeMillis();

    // Round position to a coarse grid so subtle player movement doesn't reset the transition.
    // Only reset when the player moves to a clearly different area (4+ blocks).
    int gridX = Math.floorDiv(pos.getX(), TRANSITION_GRID_SIZE);
    int gridZ = Math.floorDiv(pos.getZ(), TRANSITION_GRID_SIZE);
    int gridY = Math.floorDiv(pos.getY(), TRANSITION_GRID_SIZE);
    long gridKey = ((long) gridX) ^ (((long) gridZ) << 21) ^ (((long) gridY) << 42);

    if (activeTransitionGridKey != gridKey) {
      activeTransitionGridKey = gridKey;
      activeTransition = new TransitionData(currentOffset, currentOffset, now);
      return currentOffset;
    }

    // Check if the target offset has changed significantly (> 0.5°F)
    if (Math.abs(currentOffset - activeTransition.targetOffset) > 0.5f) {
      // Offset changed — start a new transition from wherever we currently are
      float currentSmoothed = activeTransition.getSmoothedOffset(now);
      activeTransition.previousOffset = currentSmoothed;
      activeTransition.targetOffset = currentOffset;
      activeTransition.transitionStartMs = now;
    }

    return activeTransition.getSmoothedOffset(now);
  }

  /**
   * Transient data for smooth temperature transitions. Stores the previous and target HVAC
   * offsets plus the transition start time. Not persisted in NBT — resets on server/client
   * restart, which is acceptable for a cosmetic feature.
   */
  private static class TransitionData {

    float previousOffset;
    float targetOffset;
    long transitionStartMs;

    TransitionData(float previousOffset, float targetOffset, long transitionStartMs) {
      this.previousOffset = previousOffset;
      this.targetOffset = targetOffset;
      this.transitionStartMs = transitionStartMs;
    }

    /**
     * Returns the smoothed offset by lerping between previous and target based on elapsed time.
     */
    float getSmoothedOffset(long nowMs) {
      long elapsed = nowMs - transitionStartMs;
      if (elapsed >= TRANSITION_DURATION_MS) {
        return targetOffset;
      }
      float progress = (float) elapsed / (float) TRANSITION_DURATION_MS;
      return previousOffset + (targetOffset - previousOffset) * progress;
    }
  }

  /**
   * Determines whether the given position is considered "indoors" by checking for a roof overhead
   * and walls in the cardinal directions.
   *
   * <p>The algorithm works as follows:</p>
   * <ol>
   *   <li>Scan upward from the position for up to {@link #INDOOR_SCAN_HEIGHT} blocks looking for
   *   a solid (non-air, non-replaceable) block as a roof.</li>
   *   <li>If a roof is found, scan in each of the 4 cardinal directions (north, south, east,
   *   west) at the player's Y level for up to {@link #INDOOR_SCAN_HORIZONTAL} blocks looking for
   *   solid blocks as walls.</li>
   *   <li>The position is considered indoors if a roof was found and at least 3 of the 4 cardinal
   *   directions have a wall within range.</li>
   * </ol>
   *
   * <p>This method is called per-chunk (not per-tick) and its result is cached along with the
   * chunk temperature data.</p>
   *
   * @param world the world instance
   * @param pos   the position to check
   *
   * @return {@code true} if the position is considered indoors, {@code false} otherwise
   */
  public static boolean isIndoors(World world, BlockPos pos) {
    // Roof check: scan upward for a solid block
    boolean hasRoof = false;
    for (int dy = 1; dy <= INDOOR_SCAN_HEIGHT; dy++) {
      BlockPos above = pos.up(dy);
      IBlockState state = world.getBlockState(above);
      if (isSolidBlock(state)) {
        hasRoof = true;
        break;
      }
    }

    if (!hasRoof) {
      return false;
    }

    // Wall check: scan 4 cardinal directions at player Y level
    int wallCount = 0;
    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int[] dir : directions) {
      for (int dist = 1; dist <= INDOOR_SCAN_HORIZONTAL; dist++) {
        BlockPos wallPos = pos.add(dir[0] * dist, 0, dir[1] * dist);
        IBlockState state = world.getBlockState(wallPos);
        if (isSolidBlock(state)) {
          wallCount++;
          break;
        }
      }
    }

    return wallCount >= 3;
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
   * <p>Each active unit's contribution is scaled by distance:</p>
   * <ul>
   *   <li>0–{@link #FULL_EFFECT_DISTANCE} blocks: 100% of the unit's contribution</li>
   *   <li>{@link #FULL_EFFECT_DISTANCE}–{@link #MAX_EFFECT_DISTANCE} blocks: linear falloff
   *   from 100% to 0%</li>
   *   <li>Beyond {@link #MAX_EFFECT_DISTANCE} blocks: no effect</li>
   * </ul>
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
    double maxEffectDistSq = MAX_EFFECT_DISTANCE * MAX_EFFECT_DISTANCE;

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
          if (distSq > maxEffectDistSq) {
            continue;
          }

          double dist = Math.sqrt(distSq);
          float distWeight = calculateDistanceWeight(dist);
          float wallWeight = calculateWallAttenuation(world, te.getPos(), pos);
          float weight = distWeight * wallWeight;
          float contribution = unit.getTemperatureContribution();
          float weightedOffset = Math.abs(contribution) * weight;

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

    return heatingOffset - coolingOffset;
  }

  /**
   * Calculates the distance weight for an HVAC unit's contribution. Returns 1.0 for distances
   * within {@link #FULL_EFFECT_DISTANCE}, linearly falls off to 0.0 at
   * {@link #MAX_EFFECT_DISTANCE}, and returns 0.0 beyond that.
   *
   * @param distance the distance in blocks from the unit to the query position
   *
   * @return the weight factor between 0.0 and 1.0
   */
  private static float calculateDistanceWeight(double distance) {
    if (distance <= FULL_EFFECT_DISTANCE) {
      return 1.0f;
    }
    if (distance >= MAX_EFFECT_DISTANCE) {
      return 0.0f;
    }
    return (float) ((MAX_EFFECT_DISTANCE - distance) / (MAX_EFFECT_DISTANCE - FULL_EFFECT_DISTANCE));
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

    // Skip the first step (source block) and last step (target block)
    for (int i = 1; i < steps; i++) {
      cx += sx;
      cy += sy;
      cz += sz;
      BlockPos checkPos = new BlockPos(cx, cy, cz);
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
   * suitable for roof or wall detection in the indoor check algorithm.
   *
   * @param state the block state to check
   *
   * @return {@code true} if the block is solid enough to count as a roof or wall
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
    return dimensionCaches.computeIfAbsent(dimensionId, k -> new HashMap<>());
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
   * calculated temperature, the world tick at which it was computed, and whether the sampled
   * position was considered indoors.
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
     * Whether the sampled position in this chunk was considered indoors at the time of
     * computation.
     */
    final boolean indoors;

    /**
     * Constructs a new chunk temperature data entry.
     *
     * @param temperature the temperature in degrees Fahrenheit
     * @param timestamp   the world tick time of computation
     * @param indoors     whether the sampled position was indoors
     */
    ChunkTempData(float temperature, long timestamp, boolean indoors) {
      this.temperature = temperature;
      this.timestamp = timestamp;
      this.indoors = indoors;
    }
  }
}
