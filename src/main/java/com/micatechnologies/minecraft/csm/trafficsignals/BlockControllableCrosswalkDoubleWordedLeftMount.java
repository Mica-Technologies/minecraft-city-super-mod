package com.micatechnologies.minecraft.csm.trafficsignals;

public class BlockControllableCrosswalkDoubleWordedLeftMount
    extends AbstractBlockControllableCrosswalkSignal {

  @Override
  public float getCountdownZOffset() {
    return -1; // double-worded format: countdown overlay not supported
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalkdoublewordedleftmount";
  }
}
