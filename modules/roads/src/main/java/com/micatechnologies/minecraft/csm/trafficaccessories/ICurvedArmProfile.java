package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;

/**
 * The shape of a multi-block arm placed by {@link BlockTrafficPoleMastArmCurve}: which cells it
 * occupies, each cell's hitbox, and how a cell's model is chosen. Both arm families generate one:
 * the mast arm curves ({@link MastArmCurveProfile}, gen_mast_arm_curves.py) and the luminaire arms
 * ({@link LuminaireArmProfile}, gen_luminaire_arms.py), so placement, demolition and the pole fit
 * are written once.
 *
 * @since 2026.11
 */
public interface ICurvedArmProfile {

  /**
   * How many mount directions a {@code mountMask} can hold. Bit order: down, up, east (to the
   * right of the arm's facing), west, ahead (along the facing), back. The first four are
   * {@link MastArmCurveProfile}'s {@code MOUNT_*} bits.
   */
  int MOUNT_DIRECTION_COUNT = 6;

  /**
   * Each mount direction's neighbour as {@code {alongArm, up, side}}, in bit order.
   */
  int[][] MOUNT_DIRECTION_OFFSETS = {
      {0, -1, 0}, {0, 1, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};

  /**
   * The pole of a joint cell whose pole is straight behind it, as {@link #getPoleOffset} gives it.
   */
  int[] POLE_BEHIND = {-1, 0};

  /**
   * The pole of a joint cell whose pole is straight ahead of it.
   */
  int[] POLE_AHEAD = {1, 0};

  /**
   * @return the number of block cells, including the root (cell 0)
   */
  int getCellCount();

  /**
   * A cell's offset from the position the arm was placed at, unrotated: {@code {alongArm, up}},
   * or {@code {alongArm, up, side}} for an arm that leaves the line of its facing, {@code side}
   * counting to the right of it. Cell 0 is at the placed position unless its offset says
   * otherwise: a diagonal truss arm's first cell is one step to the right, diagonal to the pole.
   *
   * @param index the cell index
   *
   * @return the cell's offset in blocks
   */
  int[] getCellOffset(int index);

  /**
   * @param index the cell index
   *
   * @return the cell's measured box in 0..1 for the unrotated (facing north) orientation
   */
  double[] getCellBox(int index);

  /**
   * @return how many values the blockstate's {@code shape} property takes
   */
  int getShapeCount();

  /**
   * Whether a cell holds a joint to a pole, and on which side: {@code 1} for a pole behind it
   * (against the arm's facing, as the root's is), {@code -1} for a pole ahead of it (the second
   * arm of a double arm, past the pole), {@code 0} for none. A cell with a pole joint reads the
   * pole's width, since its model is generated per {@link CsmPoleFit}.
   *
   * @param cell the cell index
   *
   * @return the side of the pole, or {@code 0}
   */
  int poleSide(int cell);

  /**
   * Where a joint cell's pole stands, as {@code {alongArm, side}} from the cell, unrotated. The
   * default follows {@link #poleSide}; a diagonal arm's pole is diagonally behind its first cell.
   * The array is shared: do not modify it.
   *
   * @param cell the cell index
   *
   * @return the pole's offset, or {@code null} for a cell without a pole joint
   */
  default int[] getPoleOffset(int cell) {
    int side = poleSide(cell);
    return side == 0 ? null : side > 0 ? POLE_BEHIND : POLE_AHEAD;
  }

  /**
   * Which sides of a cell can grow mount hardware, as a mask in {@link #MOUNT_DIRECTION_OFFSETS}'
   * bit order. Only these sides are tested, and only they appear in the {@code mountMask} handed
   * to {@link #shapeIndex}. The default is the mast arm curves' four: down, up, east and west.
   *
   * @param cell the cell index
   *
   * @return the mask of sides
   */
  default int getMountDirections(int cell) {
    return 0xF;
  }

  /**
   * The {@code shape} value for a cell.
   *
   * @param cell      the cell index
   * @param mountMask the mount mask, for arms that carry mount hardware: the sides in
   *                  {@link #getMountDirections} that have something mountable against them
   * @param fit       how the cell meets its pole; ignored for a cell without a pole joint
   *
   * @return the packed shape value
   */
  int shapeIndex(int cell, int mountMask, CsmPoleFit fit);

  /**
   * @return a short name for messages, such as "6x2 mast arm curve"
   */
  String describe();
}
