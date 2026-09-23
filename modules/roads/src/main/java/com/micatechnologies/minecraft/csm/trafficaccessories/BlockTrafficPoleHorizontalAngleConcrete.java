package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;

/**
 * The angled thin traffic pole in concrete. Round only: an angle is an arm, and the arm hardware
 * on a real octagonal concrete pole is round steel. See {@link BlockTrafficPoleConcrete}.
 *
 * @since 2026.9
 */
public class BlockTrafficPoleHorizontalAngleConcrete extends AbstractBlockTrafficPoleDiagonal {

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   */
  @Override
  public String getBlockRegistryName() {
    return "trafficpolehorizontalangleconcrete";
  }

  /**
   * Method which returns the block classes of blocks which should be ignored when checking for
   * adjacent blocks.
   *
   * @return Array of block classes to ignore when checking for adjacent blocks.
   */
  @Override
  public Class<?>[] getIgnoreBlock() {
    return null;
  }

  /**
   * Silver, as for {@link BlockTrafficPoleConcrete#getTrafficPoleColor()}.
   */
  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return TRAFFIC_POLE_COLOR.SILVER;
  }
}
