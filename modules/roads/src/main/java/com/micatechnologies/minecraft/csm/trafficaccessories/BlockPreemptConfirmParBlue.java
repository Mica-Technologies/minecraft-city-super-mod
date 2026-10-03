package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * The blue PAR confirmation light, on top of the mast arm facing the approach.
 *
 * @since 2026.10
 */
public class BlockPreemptConfirmParBlue extends BlockPreemptConfirmationLight {

  @Override
  public String getBlockRegistryName() {
    return "preempt_confirm_par_blue";
  }

  @Override
  public float getBeaconColorR() {
    return 0.2f;
  }

  @Override
  public float getBeaconColorG() {
    return 0.4f;
  }

  @Override
  public float getBeaconColorB() {
    return 1.0f;
  }
}
