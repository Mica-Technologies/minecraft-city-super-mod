package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The red 360 degree confirmation dome, hung from a span wire under a span wire mount (#321). In
 * every other way it is {@link BlockPreemptConfirmDomeRed}: linked to a circuit with the Signal
 * Link Tool, lit by the controller or redstone the same way, and for every emergency preempt.
 *
 * @since 2026.10
 */
public class BlockPreemptConfirmDomeRedSpan extends BlockPreemptConfirmDomeRed
    implements IPreemptSpanHung {

  @Override
  public String getBlockRegistryName() {
    return "preempt_confirm_dome_red_span";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return DOME_SPAN_BOX;
  }

  @Override
  public float[] getBeaconLensFrom() {
    return DOME_SPAN_LENS_FROM;
  }

  @Override
  public float[] getBeaconLensTo() {
    return DOME_SPAN_LENS_TO;
  }
}
