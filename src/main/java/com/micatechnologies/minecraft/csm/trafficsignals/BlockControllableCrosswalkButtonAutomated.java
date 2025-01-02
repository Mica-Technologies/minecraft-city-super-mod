package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSCampbell;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSPolara;
import net.minecraft.block.material.Material;

public class BlockControllableCrosswalkButtonAutomated extends
    AbstractBlockTrafficSignalAPSPolara {

  public BlockControllableCrosswalkButtonAutomated() {
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
    return "controllablecrosswalkbuttonautomated";

  }
}
