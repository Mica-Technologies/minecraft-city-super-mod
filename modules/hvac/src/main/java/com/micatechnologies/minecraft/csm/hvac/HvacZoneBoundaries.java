package com.micatechnologies.minecraft.csm.hvac;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;

/**
 * Where one world's HVAC Zone Boundary markers are (#305), by column, so the room finder can ask
 * whether a column holds one without reading a block. Filled by the markers' tile entities as
 * their chunks load and emptied as they unload or break, as anchors are; a room with no marker
 * near it pays one map lookup a column it floods.
 *
 * <p>A marker splits the air of its column, from the floor to the ceiling it hangs under, out of
 * every room: to the room finder that run of cells is a {@link ThermalCellSource#WALL} that air
 * crosses as freely as it crosses open air ({@link ThermalScanner#BOUNDARY_FACTOR}). A line of
 * markers across an open floor therefore splits it into rooms that stay coupled to each other
 * almost as one, without a wall.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
final class HvacZoneBoundaries {

  /** Column ({@code ThermalScanner.pack(x, 0, z)}) to the y of each marker in it. */
  private final Long2ObjectOpenHashMap<IntArrayList> columns = new Long2ObjectOpenHashMap<>();

  void add(BlockPos pos) {
    IntArrayList ys = columns.computeIfAbsent(ThermalScanner.pack(pos.getX(), 0, pos.getZ()),
        k -> new IntArrayList(1));
    if (!ys.contains(pos.getY())) {
      ys.add(pos.getY());
    }
  }

  void remove(BlockPos pos) {
    long key = ThermalScanner.pack(pos.getX(), 0, pos.getZ());
    IntArrayList ys = columns.get(key);
    if (ys == null) {
      return;
    }
    ys.rem(pos.getY());
    if (ys.isEmpty()) {
      columns.remove(key);
    }
  }

  /** The y of each marker in a column, or null when it has none. */
  IntArrayList column(int x, int z) {
    return columns.isEmpty() ? null : columns.get(ThermalScanner.pack(x, 0, z));
  }

  int size() {
    int n = 0;
    for (IntArrayList ys : columns.values()) {
      n += ys.size();
    }
    return n;
  }
}
