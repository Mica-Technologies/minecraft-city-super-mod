package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

/**
 * A block that stands in a run of kitchen cabinets without being a cabinet: the range and the
 * dishwasher, which sit in the base run under (or level with) the countertop. A cabinet beside
 * one carries its run on through it, so the cabinet shows no end panel against it and the
 * countertop line reads as unbroken, whatever the fitting's own finish.
 *
 * @since 2026.9
 */
public interface IKitchenFitting {

  /**
   * Whether this fitting, in {@code state}, carries on a run of {@code line} facing
   * {@code runFacing} that arrives from {@code side} (the world direction from this fitting to
   * the run's next block).
   *
   * @param state     this fitting's state
   * @param side      the side the run arrives from
   * @param runFacing the way the run faces
   * @param line      the run's line
   *
   * @return whether the run continues through it
   */
  boolean fitsRun(IBlockState state, EnumFacing side, EnumFacing runFacing, KitchenLine line);
}
