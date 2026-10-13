package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSEuro;

/**
 * European push button with the pedestrian symbol on its front.
 *
 * @since 2026.10
 */
public class BlockControllableCrosswalkButtonEuro extends AbstractBlockTrafficSignalAPSEuro {

  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalkbuttoneuro";
  }
}
