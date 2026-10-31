package com.micatechnologies.minecraft.csm.venues.ballpark;

/**
 * One family of ballpark pictures laid out round anchors: the field's markings (the home plate
 * area, base cutouts, coach's boxes, on-deck circles, the infield arc in segments, softball's
 * home area and pitcher's circle) or the pitcher's mound. Which cell picture sits at each cell
 * round an anchor, and which its third base side twin shows there, mirrored. The numbers are
 * {@link BallparkLayouts}', written by {@code gen_venues_ballpark.py}.
 *
 * <p>Cells are counted in the anchor's frame: {@code i} to its right and {@code j} forward,
 * the way it faces. A kind index is the anchor group times four plus the anchor's kind.</p>
 *
 * @since 2026.10
 */
public final class BallparkFamily {

  /** How many cell pictures the painted block has (its {@code cell} property's size). */
  final int cells;
  /** How far (in cells, each way) a painted cell looks for its anchor. */
  final int radius;
  /** Whether a twin draws a kind mirrored (the field's markings), or none does (the mound). */
  final boolean mirrors;
  /** For each kind, the cell at each grid square, indexed {@code (j + r) * (2r + 1) + (i + r)};
   * -1 where the kind draws nothing. */
  private final int[][] normal;
  private final int[][] mirrored;

  /**
   * Constructs a family.
   *
   * @param cells   how many cell pictures there are
   * @param radius  how far a picture reaches
   * @param mirrors whether third base side twins draw it
   * @param kinds   for each kind index, {@code i, j, cell, mirrored cell} quadruplets
   */
  BallparkFamily(int cells, int radius, boolean mirrors, int[]... kinds) {
    this.cells = cells;
    this.radius = radius;
    this.mirrors = mirrors;
    int side = 2 * radius + 1;
    this.normal = new int[kinds.length][side * side];
    this.mirrored = new int[kinds.length][side * side];
    for (int k = 0; k < kinds.length; k++) {
      java.util.Arrays.fill(normal[k], -1);
      java.util.Arrays.fill(mirrored[k], -1);
      int[] t = kinds[k];
      for (int o = 0; o + 3 < t.length; o += 4) {
        int at = (t[o + 1] + radius) * side + (t[o] + radius);
        normal[k][at] = t[o + 2];
        mirrored[k][at] = t[o + 3];
      }
    }
  }

  /**
   * The picture at a cell round an anchor.
   *
   * @param kind  the anchor's kind index
   * @param i     cells to the anchor's right
   * @param j     cells forward of it
   * @param third the anchor is a third base side twin: the first base side's picture, mirrored
   *
   * @return the cell, or -1 where the kind draws nothing
   */
  int cellAt(int kind, int i, int j, boolean third) {
    if (third) {
      i = -i;
    }
    if (kind < 0 || kind >= normal.length || Math.abs(i) > radius || Math.abs(j) > radius) {
      return -1;
    }
    int at = (j + radius) * (2 * radius + 1) + (i + radius);
    return third ? mirrored[kind][at] : normal[kind][at];
  }
}
