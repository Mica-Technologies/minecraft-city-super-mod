package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * Where a door closer's arm is at any angle of the door's swing.
 *
 * <p>A closer is a body on the door leaf, a shoe fixed on the wall above the opening, and a
 * two-link arm between them: the main arm from the body's spindle to the elbow, and the forearm
 * from the elbow to the shoe. Both links are rigid, so once the leaf has carried the spindle to
 * where it is, the elbow can only be where a circle of the main arm's length about the spindle
 * meets one of the forearm's length about the shoe -- on the side of the line between them that
 * the arm folds to (toward the latch while the door is shut, as a parallel arm folds). The closer
 * sits on the push side of the leaf, the side the door swings away from, which is the only side
 * whose arm can reach the frame with the door open: the other face turns to the jamb.</p>
 *
 * <p>Everything here is in pixels, for a left-hinged door that swings in, with the inside to the
 * north -- the frame {@code gen_doors.py} draws in. A right-hinged door is its mirror across the
 * middle of the cell in x, and a door that swings out its mirror in depth. The constants are SHARED
 * with {@code gen_doors.py} (CLOSER_*), which bakes the arm shut and open from the same solution,
 * so a swing drawn from {@link #solve} starts on the one model and ends on the other.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class DoorCloserArm {

  /** SHARED with gen_doors.PIVOT: where the leaf turns. */
  static final double PIVOT_X = 0.875;
  static final double PIVOT_Z = 15.125;
  /** SHARED with gen_doors.CLOSER_SPINDLE: the spindle, with the door shut. */
  static final double SPINDLE_X = 6.0;
  static final double SPINDLE_Z = 17.0;
  /** SHARED with gen_doors.CLOSER_SHOE: the pivot in the shoe, which never moves. */
  static final double SHOE_X = 6.5317;
  static final double SHOE_Z = 18.995;
  /** SHARED with gen_doors.CLOSER_MAIN and CLOSER_FORE: the links' lengths. */
  static final double MAIN = 5.3482;
  static final double FORE = 5.2133;
  /** SHARED with gen_doors.CLOSER_ELBOW_SIDE: which way the arm folds. */
  static final int ELBOW_SIDE = -1;

  /** SHARED with gen_doors: the links' heights and half widths, and the elbow pin's. */
  static final double MAIN_Y0 = 14.5;
  static final double MAIN_Y1 = 15.0;
  static final double FORE_Y0 = 15.0;
  static final double FORE_Y1 = 15.5;
  static final double PIN_Y0 = 14.375;
  static final double PIN_Y1 = 15.625;
  static final double LINK_HALF = 0.5;
  static final double PIN_HALF = 0.625;

  private DoorCloserArm() {
  }

  /**
   * The spindle and the elbow with the door turned {@code degrees} open.
   *
   * @param degrees how far open, 0 (shut) to 90 (open)
   *
   * @return {spindle x, spindle z, elbow x, elbow z}, in pixels
   *
   * @since 1.0
   */
  static double[] solve(double degrees) {
    double a = Math.toRadians(degrees);
    double x = SPINDLE_X - PIVOT_X;
    double z = SPINDLE_Z - PIVOT_Z;
    double sx = PIVOT_X + x * Math.cos(a) + z * Math.sin(a);
    double sz = PIVOT_Z - x * Math.sin(a) + z * Math.cos(a);
    double dx = SHOE_X - sx;
    double dz = SHOE_Z - sz;
    double d = Math.sqrt(dx * dx + dz * dz);
    double along = (MAIN * MAIN - FORE * FORE + d * d) / (2 * d);
    double h = Math.sqrt(Math.max(0.0, MAIN * MAIN - along * along));
    double mx = sx + along * dx / d;
    double mz = sz + along * dz / d;
    return new double[]{sx, sz, mx - ELBOW_SIDE * h * dz / d, mz + ELBOW_SIDE * h * dx / d};
  }
}
