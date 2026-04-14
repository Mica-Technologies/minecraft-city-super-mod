package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSPolara;
import net.minecraft.block.material.Material;

/**
 * Male-voice crosswalk push-button block using the Polara APS (accessible pedestrian signal) style.
 * Plays a male voice announcement during the walk phase.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableCrosswalkButtonMale extends AbstractBlockTrafficSignalAPSPolara {

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
