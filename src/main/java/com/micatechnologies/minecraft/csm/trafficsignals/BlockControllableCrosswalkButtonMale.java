package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import net.minecraft.block.material.Material;

public class BlockControllableCrosswalkButtonMale extends AbstractBlockTrafficSignalAPS {

  public BlockControllableCrosswalkButtonMale() {
    super(Material.ROCK);
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
    return "controllablecrosswalkbuttonmale";
  }

}
