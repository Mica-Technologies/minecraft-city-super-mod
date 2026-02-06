package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockDSLFR extends AbstractBrightLight {

  @Override
  public String getBlockRegistryName() {
    return "dslfr";
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
    return new AxisAlignedBB(-0.062500, 0.437500, -1.000000, 1.062500, 1.000000, 1.000000);
  }

  @Override
  public int getBrightLightXOffset() {
    return 0;
  }

}
