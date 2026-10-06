package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The red 360 degree confirmation dome, on top of the mast arm.
 *
 * @since 2026.10
 */
public class BlockPreemptConfirmDomeRed extends BlockPreemptConfirmationLight {

  @Override
  public String getBlockRegistryName() {
    return "preempt_confirm_dome_red";
  }

  @Override
  public float getBeaconColorR() {
    return 1.0f;
  }

  @Override
  public float getBeaconColorG() {
    return 0.15f;
  }

  @Override
  public float getBeaconColorB() {
    return 0.1f;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return DOME_BOX;
  }

  @Override
  public float[] getBeaconLensFrom() {
    return DOME_LENS_FROM;
  }

  @Override
  public float[] getBeaconLensTo() {
    return DOME_LENS_TO;
  }

  /** Red, so it lights for every emergency preempt, as the beacon does. */
  @Override
  public boolean isLitForAnyEmergencyPreempt() {
    return true;
  }

  /** The dome keeps the beacon's emergency double flash. */
  @Override
  public long getBeaconCycleMillis() {
    return 0L;
  }
}
