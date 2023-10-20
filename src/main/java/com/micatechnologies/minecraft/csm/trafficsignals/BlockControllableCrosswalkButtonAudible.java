package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import net.minecraft.block.material.Material;

public class BlockControllableCrosswalkButtonAudible extends AbstractBlockTrafficSignalAPS {

  public BlockControllableCrosswalkButtonAudible() {
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
    return "controllablecrosswalkbuttonaudible";

  }
}
