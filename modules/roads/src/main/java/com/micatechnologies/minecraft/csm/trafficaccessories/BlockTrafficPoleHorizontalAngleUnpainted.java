package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;

public class BlockTrafficPoleHorizontalAngleUnpainted extends AbstractBlockTrafficPoleDiagonal {

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "trafficpolehorizontalangleunpainted";
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

  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return TRAFFIC_POLE_COLOR.UNPAINTED;
  }

}
