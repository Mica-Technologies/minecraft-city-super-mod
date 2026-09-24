package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import javax.annotation.Nonnull;
import net.minecraft.util.BlockRenderLayer;

/**
 * A free-standing laundry appliance that works: the front-loading washing machine, which
 * repairs armour, and the dryer ({@link LaundryAppliances}). It stands in no cabinet run; its
 * top is at a countertop's height, so a small piece (the steam iron) stands on it. Its round
 * door window is a cutout on a square, lit while it runs, so it draws in the cutout layer.
 *
 * @since 2026.9
 */
public class BlockLaundryAppliance extends BlockBuiltInAppliance {

  /**
   * Constructs a laundry appliance.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param spec         what kind of appliance it is
   */
  public BlockLaundryAppliance(String registryName, int[] box, ApplianceSpec spec) {
    super(registryName, box, spec, null);
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
