package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockSearsLF3 extends AbstractBrightLight {

  @Override
  public String getBlockRegistryName() {
    return "searslf3";
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
    return new AxisAlignedBB(-1.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000);
  }

  @Override
  public int getBrightLightXOffset() {
    return 0;
  }

}
