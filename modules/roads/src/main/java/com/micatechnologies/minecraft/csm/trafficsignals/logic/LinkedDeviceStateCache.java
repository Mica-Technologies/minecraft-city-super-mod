package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a controller knows about its linked devices without loading their chunks.
 *
 * <p>Building a phase reads static facts about the heads it lights: which way a head faces, what
 * kind of block it is, which legend a blankout box shows. On a server, {@code getBlockState} and
 * {@code getTileEntity} load the chunk they read from, and NORMAL mode rebuilds its active
 * circuit's phase every controller tick, so a controller kept every chunk its heads stood in
 * loaded (or loaded it again every second after the server dropped it).</p>
 *
 * <p>Nothing in an unloaded chunk can change, so the last value read while it was loaded is
 * still the value: this reads fresh while the chunk is loaded and remembers the result, and
 * answers from memory while it is not. A position never read since the world loaded is read
 * once, chunk load and all, so what a phase is built from never depends on which chunks happen to
 * be loaded.</p>
 *
 * <p>One table per world, held weakly, so a world that unloads takes its table with it. The
 * tables only ever hold the positions controllers ask about.</p>
 *
 * @since 2026.10
 */
public final class LinkedDeviceStateCache {

  /** Stands in for a remembered null, since the tables cannot hold null values. */
  private static final Object NONE = new Object();

  private static final Map<World, Map<BlockPos, Object>> STATES = new WeakHashMap<>();
  private static final Map<World, Map<String, Map<BlockPos, Object>>> TILE_VALUES =
      new WeakHashMap<>();

  private LinkedDeviceStateCache() {}

  /**
   * The block state at {@code pos}: read now if its chunk is loaded, else the last one read.
   *
   * @param world the world, not null
   * @param pos   the position of a linked device
   *
   * @return the block state
   */
  public static IBlockState blockState(World world, BlockPos pos) {
    Map<BlockPos, Object> table = STATES.computeIfAbsent(world, w -> new HashMap<>());
    return lookup(table, pos, world.isBlockLoaded(pos), () -> world.getBlockState(pos));
  }

  /**
   * A value read off the tile entity at {@code pos}, with the same rule as
   * {@link #blockState(World, BlockPos)}. Each distinct {@code key} has its own table, so two
   * readers of one position never answer for each other.
   *
   * @param world  the world, not null
   * @param pos    the position of a linked device
   * @param key    names what {@code reader} reads, e.g. "blankoutType"
   * @param reader reads the value off the tile entity, which may be null
   * @param <T>    the value's type
   *
   * @return the value, or null
   */
  public static <T> T tileValue(World world, BlockPos pos, String key,
      Function<TileEntity, T> reader) {
    Map<BlockPos, Object> table = TILE_VALUES.computeIfAbsent(world, w -> new HashMap<>())
        .computeIfAbsent(key, k -> new HashMap<>());
    return lookup(table, pos, world.isBlockLoaded(pos),
        () -> reader.apply(world.getTileEntity(pos)));
  }

  /**
   * The rule itself, apart from any world: read and remember when {@code loaded}, otherwise
   * answer from {@code table}, reading only when nothing was ever remembered.
   */
  @SuppressWarnings("unchecked")
  static <T> T lookup(Map<BlockPos, Object> table, BlockPos pos, boolean loaded,
      Supplier<T> read) {
    if (!loaded) {
      Object remembered = table.get(pos);
      if (remembered != null) {
        return remembered == NONE ? null : (T) remembered;
      }
    }
    T value = read.get();
    // An immutable key: a caller's MutableBlockPos would otherwise move under the table.
    table.put(pos.toImmutable(), value == null ? NONE : value);
    return value;
  }
}
