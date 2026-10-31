package com.micatechnologies.minecraft.csm.venues.field;

import java.util.Arrays;

/**
 * One sport's field pictures laid out round anchors: which painted cell picture sits at each cell
 * round each anchor kind, and whether it is shown turned 180 degrees. The numbers are
 * {@link FieldLayouts}', written by {@code gen_venues_football.py}.
 *
 * <p>Cells are counted in the anchor's frame: {@code i} to its right and {@code j} forward, the
 * way it faces (north, or east for a field laid along z).</p>
 *
 * @since 2026.10
 */
public final class FieldFamily {

  /** How many cell pictures the painted block has (its {@code cell} property's size). */
  final int cells;
  /** How far (in cells, each way) a painted cell looks for its anchor. */
  final int radius;
  /** For each kind, {@code cell * 2 + half} at each grid square, -1 where it draws nothing. */
  private final int[][] tables;

  /**
   * Constructs a family.
   *
   * @param cells  how many cell pictures there are
   * @param radius how far a picture reaches
   * @param kinds  for each kind, {@code i, j, cell * 2 + half} triplets
   */
  FieldFamily(int cells, int radius, int[]... kinds) {
    this.cells = cells;
    this.radius = radius;
    int side = 2 * radius + 1;
    this.tables = new int[kinds.length][side * side];
    for (int k = 0; k < kinds.length; k++) {
      Arrays.fill(tables[k], -1);
      int[] t = kinds[k];
      for (int o = 0; o + 2 < t.length; o += 3) {
        tables[k][(t[o + 1] + radius) * side + (t[o] + radius)] = t[o + 2];
      }
    }
  }

  /** How many anchor kinds. */
  int kinds() {
    return tables.length;
  }

  /**
   * The picture at a cell round an anchor.
   *
   * @param kind the anchor's kind
   * @param i    cells to the anchor's right
   * @param j    cells forward of it
   *
   * @return {@code cell * 2 + half}, or -1 where the kind draws nothing
   */
  int at(int kind, int i, int j) {
    if (kind < 0 || kind >= tables.length || Math.abs(i) > radius || Math.abs(j) > radius) {
      return -1;
    }
    return tables[kind][(j + radius) * (2 * radius + 1) + (i + radius)];
  }
}
