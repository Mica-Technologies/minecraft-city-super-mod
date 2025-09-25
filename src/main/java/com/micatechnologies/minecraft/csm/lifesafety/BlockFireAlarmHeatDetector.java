package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFireAlarmHeatDetector extends AbstractBlockFireAlarmDetector {

  @Override
  public String getBlockRegistryName() {
    return "firealarmheatdetector";
  }

  @Override
  public void onFire(World world, BlockPos blockPos, IBlockState blockState) {
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
    return new AxisAlignedBB(0.125000, 0.125000, 0.900000, 0.875000, 0.875000, 1.000000);
  }
}
