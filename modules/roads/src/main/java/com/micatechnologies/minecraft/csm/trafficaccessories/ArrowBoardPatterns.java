package com.micatechnologies.minecraft.csm.trafficaccessories;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the lamp stages {@link ArrowBoardPattern} displays.
 *
 * <p>Separated from the enum because an enum constructor cannot run this much before its own
 * fields exist, and because the right-hand and left-hand forms of every pattern are built from
 * one description here and mirrored. Writing the left-hand ones out separately is how a pair
 * ends up disagreeing about where its barbs are.</p>
 *
 * <p>Positions are {@code {column, row}} on the panel grid, column 0 at the left and row 0 at the
 * top.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
final class ArrowBoardPatterns {

  /** The middle row of the five, where an arrow's shaft runs. */
  private static final int MID_ROW = ArrowBoardGeometry.GRID_ROWS / 2;

  /** The rightmost column, where a right-pointing arrow's tip sits. */
  private static final int LAST_COL = ArrowBoardGeometry.GRID_COLS - 1;

  /**
   * How many columns apart the chasing chevrons sit.
   *
   * <p>Two, not one: at one column apart their limbs touch and the three read as a single solid
   * arrowhead rather than as three marks with air between them. On a seven-column grid this puts
   * them at columns 6, 4 and 2, and the tail chevron's limbs still land inside the panel.</p>
   */
  private static final int CHEVRON_SPACING = 2;

  private ArrowBoardPatterns() {
    throw new AssertionError("ArrowBoardPatterns is a factory and must not be instantiated");
  }

  /**
   * The sequential chevron: three separated chevrons CHASING one another toward the point, then
   * all three together, then dark.
   *
   * <p>Two things make this read as a chevron travelling rather than as an arrow being drawn.
   * The chevrons are {@link #CHEVRON_SPACING} columns apart, so each is a distinct mark with a
   * gap either side instead of merging into one solid wedge; and each lights ALONE in its turn
   * rather than being added to the ones before it. Lighting them cumulatively at adjacent
   * columns — which is what this did first — is a growing arrowhead, which is a different device
   * altogether.</p>
   *
   * <p>The run goes from the TAIL toward the point, so the motion carries the way the arrow is
   * sending traffic. The dark stage at the end is what separates one run from the next; without
   * it the sequence runs into itself and reads as a flicker.</p>
   *
   * @param mirrored whether to point left instead of right
   *
   * @return the stages
   *
   * @since 1.0
   */
  static int[][][] chevronSequence(boolean mirrored) {
    List<int[]> tail = chevronAt(LAST_COL - 2 * CHEVRON_SPACING);
    List<int[]> middle = chevronAt(LAST_COL - CHEVRON_SPACING);
    List<int[]> head = chevronAt(LAST_COL);

    List<int[]> all = new ArrayList<>(tail);
    all.addAll(middle);
    all.addAll(head);

    return stages(mirrored, tail, middle, head, all, new ArrayList<>());
  }

  /**
   * One chevron: the apex on the middle row at the given column, with a limb running back and
   * away from it in each direction.
   *
   * @param apexColumn the column the apex sits in
   *
   * @return the lamps of that chevron
   *
   * @since 1.0
   */
  private static List<int[]> chevronAt(int apexColumn) {
    List<int[]> lamps = new ArrayList<>();
    lamps.add(new int[]{apexColumn, MID_ROW});
    for (int step = 1; step <= MID_ROW; step++) {
      int column = apexColumn - step;
      if (column < 0) {
        break;
      }
      lamps.add(new int[]{column, MID_ROW - step});
      lamps.add(new int[]{column, MID_ROW + step});
    }
    return lamps;
  }

  /**
   * The flashing arrow: a shaft with a head, on and then off.
   *
   * @param mirrored whether to point left instead of right
   *
   * @return the stages
   *
   * @since 1.0
   */
  static int[][][] flashingArrow(boolean mirrored) {
    return stages(mirrored, fullArrow(), new ArrayList<>());
  }

  /**
   * The sequential arrow: the shaft lights along its length toward the head, the head appears
   * once the shaft is complete, then the whole arrow goes dark.
   *
   * <p>This one ACCUMULATES, unlike the chasing chevron, and that is the difference between the
   * two modes rather than an oversight. A sequential arrow draws itself toward where it is
   * sending traffic; a sequential chevron sends separate marks travelling the same way. Building
   * both the same way would leave the board with two settings that look alike.</p>
   *
   * @param mirrored whether to point left instead of right
   *
   * @return the stages
   *
   * @since 1.0
   */
  static int[][][] sequentialArrow(boolean mirrored) {
    List<List<int[]>> built = new ArrayList<>();
    List<int[]> shaft = new ArrayList<>();
    for (int column = 0; column <= LAST_COL; column++) {
      shaft.add(new int[]{column, MID_ROW});
      // Two lamps a stage, so a seven-lamp shaft grows in a handful of steps rather than
      // creeping across one lamp at a time.
      if (column % 2 == 1) {
        built.add(new ArrayList<>(shaft));
      }
    }
    built.add(fullArrow());
    built.add(new ArrayList<>());

    @SuppressWarnings("unchecked")
    List<int[]>[] asArray = built.toArray(new List[0]);
    return stages(mirrored, asArray);
  }

  /**
   * The complete arrow: the whole shaft with the head on the end of it.
   *
   * @return the lamps
   *
   * @since 1.0
   */
  private static List<int[]> fullArrow() {
    List<int[]> arrow = new ArrayList<>();
    for (int column = 0; column <= LAST_COL; column++) {
      arrow.add(new int[]{column, MID_ROW});
    }
    arrow.addAll(chevronAt(LAST_COL));
    return arrow;
  }

  /**
   * The flashing caution: four corner lamps, on and then off.
   *
   * @return the stages
   *
   * @since 1.0
   */
  static int[][][] flashingCaution() {
    List<int[]> corners = new ArrayList<>();
    corners.add(new int[]{0, 0});
    corners.add(new int[]{LAST_COL, 0});
    corners.add(new int[]{0, ArrowBoardGeometry.GRID_ROWS - 1});
    corners.add(new int[]{LAST_COL, ArrowBoardGeometry.GRID_ROWS - 1});
    return stages(false, corners, new ArrayList<>());
  }

  /**
   * Packs stages into the enum's array form, mirroring every one of them if asked.
   *
   * @param mirrored whether to flip the stages left to right
   * @param source   the stages, in order
   *
   * @return the stages
   *
   * @since 1.0
   */
  @SafeVarargs
  private static int[][][] stages(boolean mirrored, List<int[]>... source) {
    int[][][] out = new int[source.length][][];
    for (int i = 0; i < source.length; i++) {
      List<int[]> lamps = source[i];
      out[i] = new int[lamps.size()][];
      for (int j = 0; j < lamps.size(); j++) {
        int[] lamp = lamps.get(j);
        out[i][j] = mirrored ? new int[]{LAST_COL - lamp[0], lamp[1]} : new int[]{lamp[0], lamp[1]};
      }
    }
    return out;
  }
}
