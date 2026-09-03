package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSCampbell;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSPolara;
import net.minecraft.block.material.Material;

/**
 * Automated crosswalk push-button block using the Polara APS (accessible pedestrian signal) style.
 * Provides automated audible and tactile pedestrian crossing indications.
 *
 * @author Mica Technologies
 * @since 1.0
 */
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
