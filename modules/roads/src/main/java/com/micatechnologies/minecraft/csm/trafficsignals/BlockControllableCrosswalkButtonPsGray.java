package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSCampbell;
import net.minecraft.block.material.Material;

/**
 * Gray-colored crosswalk push-button block using the Campbell APS (accessible pedestrian signal)
 * style. A cosmetic variant of the standard Campbell APS button with a gray housing.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableCrosswalkButtonPsGray extends AbstractBlockTrafficSignalAPSCampbell {

  public BlockControllableCrosswalkButtonPsGray() {
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
    return "controllablecrosswalkbuttonpsgray";

  }
}
