package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The white PAR confirmation light, hung from a span wire under a span wire mount (#321). In every
 * other way it is {@link BlockPreemptConfirmParWhite}: linked to a circuit with the Signal Link
 * Tool and lit by the controller or redstone the same way.
 *
 * @since 2026.10
 */
public class BlockPreemptConfirmParWhiteSpan extends BlockPreemptConfirmParWhite
    implements IPreemptSpanHung {

  @Override
  public String getBlockRegistryName() {
    return "preempt_confirm_par_white_span";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return PAR_SPAN_BOX;
  }

  @Override
  public float[] getBeaconLensFrom() {
    return PAR_SPAN_LENS_FROM;
  }

  @Override
  public float[] getBeaconLensTo() {
    return PAR_SPAN_LENS_TO;
  }
}
