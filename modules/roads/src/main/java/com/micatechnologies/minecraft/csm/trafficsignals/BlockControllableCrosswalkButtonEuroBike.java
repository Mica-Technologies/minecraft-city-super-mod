package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPSEuro;

/**
 * European push button with the bicycle symbol on its front.
 *
 * @since 2026.10
 */
public class BlockControllableCrosswalkButtonEuroBike extends AbstractBlockTrafficSignalAPSEuro {

  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalkbuttoneurobike";
  }
}
