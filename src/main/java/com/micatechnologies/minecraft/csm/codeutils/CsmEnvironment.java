package com.micatechnologies.minecraft.csm.codeutils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What the world feels like at a position, for anything that displays it. Core owns the
 * question so that a block which merely <em>reads</em> a temperature — a computer's status
 * screen, say — does not have to be built alongside the HVAC system that answers it best.
 *
 * <p>The answer comes from a {@link TemperatureProvider}. With no provider installed the
 * default one returns the biome baseline, which is exactly what the HVAC system returns when
 * there is no HVAC equipment in range; installing HVAC swaps in its own provider, which adds
 * the offset its equipment contributes. The baseline itself lives here rather than in HVAC
 * because it is world data, not equipment data, and both answers have to agree on it.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public class CsmEnvironment {

  /**
   * Answers what the temperature is at a position, in degrees Fahrenheit.
   *
   * @since 1.0
   */
  @FunctionalInterface
  public interface TemperatureProvider {

    /**
     * Gets the temperature at the given position.
     *
     * @param world the world to read
     * @param pos   the position to read at
     *
     * @return the temperature in degrees Fahrenheit
     *
     * @since 1.0
     */
    float getTemperatureAt(World world, BlockPos pos);
  }

  /**
   * Number of ticks between baseline cache recalculations. At 20 TPS this equals 2 seconds.
   * Kept short so temperature changes are visible quickly when moving between chunks.
   */
  private static final long CACHE_LIFETIME_TICKS = 40L;

  /**
   * How often (in ticks) to sweep stale entries from the chunk baseline cache. At 20 TPS this
   * equals 30 seconds — frequent enough to prevent unbounded growth during exploration,
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
   * Per-dimension cache of chunk baseline temperatures, keyed by the dimension ID of the world.
   * A {@code ConcurrentHashMap} so the server tick thread and the client render thread can both
   * query without external synchronization.
   */
  private static final Map<Integer, Map<Long, ChunkTempData>> dimensionCaches =
      new ConcurrentHashMap<>();

  /**
   * The installed temperature provider. Volatile because a module installs it on the main
   * thread while reads happen on the tick and render threads.
   */
  private static volatile TemperatureProvider temperatureProvider =
      CsmEnvironment::getBaselineTemperatureAt;

  /**
   * Installs the provider that answers temperature queries. Called from the HVAC module's
   * pre-initialization; the last provider installed wins.
   *
   * @param provider the provider to answer with, or {@code null} to go back to the baseline
   *
   * @since 1.0
   */
  public static void setTemperatureProvider(TemperatureProvider provider) {
    temperatureProvider = provider != null ? provider : CsmEnvironment::getBaselineTemperatureAt;
  }

  /**
   * Gets the temperature in degrees Fahrenheit at the given position, as the installed provider
   * reports it.
   *
   * @param world the world to read
   * @param pos   the position to read at
   *
   * @return the temperature in degrees Fahrenheit
   *
   * @since 1.0
   */
  public static float getTemperatureAt(World world, BlockPos pos) {
    return temperatureProvider.getTemperatureAt(world, pos);
  }

  /**
   * Gets the biome baseline temperature for the chunk containing the given position, with no
   * equipment contribution of any kind. Cached per chunk for {@link #CACHE_LIFETIME_TICKS}
   * ticks, because it does not vary inside a chunk.
   *
   * <p>Biome baseline mapping: {@code tempF = biomeTemp * 90 - 4} — approximately -4°F at
   * biome temp 0.0, ~68°F at 0.8 (plains), and ~176°F at 2.0 (desert).</p>
   *
   * @param world the world to read
   * @param pos   the position to read at
   *
   * @return the baseline temperature in degrees Fahrenheit
   *
   * @since 1.0
   */
  public static float getBaselineTemperatureAt(World world, BlockPos pos) {
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
   * Drops the cached baseline for the given chunk, forcing a recalculation on the next query.
   *
   * @param world  the world instance
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   *
   * @since 1.0
   */
  public static void invalidateBaselineChunk(World world, int chunkX, int chunkZ) {
    Map<Long, ChunkTempData> cache = getOrCreateCache(world);
    cache.remove(chunkKeyFromCoords(chunkX, chunkZ));
  }

  /**
   * Retrieves or creates the chunk baseline cache for the given world's dimension.
   *
   * @param world the world instance
   *
   * @return the chunk baseline cache map for this dimension
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
   * Computes a chunk cache key from chunk coordinates using Minecraft's {@code ChunkPos}
   * encoding.
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
   * Internal data class that holds the cached baseline for a single chunk. Stores the
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
