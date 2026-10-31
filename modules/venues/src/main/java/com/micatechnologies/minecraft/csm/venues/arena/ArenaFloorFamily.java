package com.micatechnologies.minecraft.csm.venues.arena;

/**
 * One family of painted event floor markings (the rink's or the court's): how many cell
 * pictures its painted block has, how far a painted block looks for an anchor, and for each of
 * the anchor's four kinds which picture sits at each cell round it. The numbers are
 * {@link ArenaFloorLayouts}', written by {@code gen_venues_arena.py}.
 *
 * <p>Cells are counted in the anchor's frame: {@code i} to its right and {@code j} forward,
 * the way it faces. Cell 0 is plain.</p>
 *
 * @since 2026.10
 */
public final class ArenaFloorFamily {

  /** The family's key in lang keys: {@code ice} or {@code court}. */
  final String key;
  /** How many cell pictures the painted block has (its {@code cell} property's size). */
  final int cells;
  /** How far (in cells, each way) any kind's picture reaches from its anchor. */
  final int radius;
  /** For each kind, the picture at each cell, indexed {@code (j + r) * (2r + 1) + (i + r)}. */
  private final int[][] grids;

  /**
   * Constructs a family.
   *
   * @param key    its key in lang keys
   * @param cells  how many cell pictures there are
   * @param radius how far a picture reaches
   * @param kinds  for each anchor kind, {@code i, j, cell} triplets of the cells that are not
   *               plain
   */
  ArenaFloorFamily(String key, int cells, int radius, int[]... kinds) {
    this.key = key;
    this.cells = cells;
    this.radius = radius;
    int side = 2 * radius + 1;
    this.grids = new int[kinds.length][side * side];
    for (int k = 0; k < kinds.length; k++) {
      int[] t = kinds[k];
      for (int o = 0; o + 2 < t.length; o += 3) {
        grids[k][(t[o + 1] + radius) * side + (t[o] + radius)] = t[o + 2];
      }
    }
  }

  /**
   * The picture at a cell round an anchor.
   *
   * @param kind the anchor's kind
   * @param i    cells to the anchor's right
   * @param j    cells forward of it
   *
   * @return the cell picture, 0 for plain
   */
  int cellAt(int kind, int i, int j) {
    if (kind < 0 || kind >= grids.length || Math.abs(i) > radius || Math.abs(j) > radius) {
      return 0;
    }
    return grids[kind][(j + radius) * (2 * radius + 1) + (i + radius)];
  }
}
