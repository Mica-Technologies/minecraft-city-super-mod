package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;

public class BlockTrafficPoleLargeTan extends AbstractBlockTrafficPole {

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "trafficpoleverticaltan";
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
   * Method which returns the color of the traffic pole.
   *
   * @return The color of the traffic pole.
   */
  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return TRAFFIC_POLE_COLOR.TAN;
  }
}
