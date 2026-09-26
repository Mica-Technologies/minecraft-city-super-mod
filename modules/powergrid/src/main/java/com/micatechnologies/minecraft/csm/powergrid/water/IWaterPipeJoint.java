package com.micatechnologies.minecraft.csm.powergrid.water;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A block a {@link BlockWaterPipe} joins on some of its sides: an inline fitting along its
 * axis, a pump or a tank at its nozzles, a gas yard's line heater or vent stack. A pipe of
 * another service does not join it ({@link #pipeService()}).
 *
 * @since 2026.9
 */
public interface IWaterPipeJoint {

  /**
   * Whether a pipe on {@code side} of this block (at {@code pos}) meets a nozzle or pipe end of
   * it there.
   */
  boolean joinsWaterPipe(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing side);

  /**
   * The service this block's nozzles carry: a pipe joins it only if it carries the same one.
   *
   * @return {@link BlockWaterPipe#WATER} unless the block says otherwise
   */
  default String pipeService() {
    return BlockWaterPipe.WATER;
  }
}
