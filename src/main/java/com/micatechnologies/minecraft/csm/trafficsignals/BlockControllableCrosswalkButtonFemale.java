package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSCampbell;
import net.minecraft.block.material.Material;

/**
 * Female-voice crosswalk push-button block using the Campbell APS (accessible pedestrian signal)
 * style. Plays a female voice announcement during the walk phase.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableCrosswalkButtonFemale extends AbstractBlockTrafficSignalAPSCampbell {

  public BlockControllableCrosswalkButtonFemale() {
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
    return "controllablecrosswalkbuttonfemale";
  }

}
