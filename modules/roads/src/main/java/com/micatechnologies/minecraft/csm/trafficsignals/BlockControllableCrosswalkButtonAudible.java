package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSCampbell;
import net.minecraft.block.material.Material;

/**
 * Audible crosswalk push-button block using the Campbell APS (accessible pedestrian signal) style.
 * Emits an audible locator tone during the walk phase.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableCrosswalkButtonAudible extends AbstractBlockTrafficSignalAPSCampbell {

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
