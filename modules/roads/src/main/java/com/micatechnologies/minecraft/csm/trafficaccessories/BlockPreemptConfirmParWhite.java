package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * The white PAR confirmation light, on top of the mast arm facing the approach.
 *
 * @since 2026.10
 */
public class BlockPreemptConfirmParWhite extends BlockPreemptConfirmationLight {

  @Override
  public String getBlockRegistryName() {
    return "preempt_confirm_par_white";
  }

  @Override
  public float getBeaconColorR() {
    return 1.0f;
  }

  @Override
  public float getBeaconColorG() {
    return 0.97f;
  }

  @Override
  public float getBeaconColorB() {
    return 0.88f;
  }
}
