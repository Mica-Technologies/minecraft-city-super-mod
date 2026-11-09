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
   * @return the number of block cells, including the root (cell 0)
   */
  int getCellCount();

  /**
   * @param index the cell index
   *
   * @return the cell's offset from the root as {@code {alongArm, up}} in blocks, unrotated
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
   * The {@code shape} value for a cell.
   *
   * @param cell      the cell index
   * @param mountMask the mount mask, for arms that carry mount hardware
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
