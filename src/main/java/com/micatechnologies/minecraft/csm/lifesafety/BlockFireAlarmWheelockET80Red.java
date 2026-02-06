package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockFireAlarmWheelockET80Red extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmwheelocket80red";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:simplex_4051_marchtime";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 130;
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
    return new AxisAlignedBB(0.125000, 0.250000, 0.687500, 0.875000, 1.000000, 1.000000);
  }
}
