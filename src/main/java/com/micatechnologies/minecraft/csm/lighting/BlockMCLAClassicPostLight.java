package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockMCLAClassicPostLight extends AbstractBrightLight {

  @Override
  public String getBlockRegistryName() {
    return "mclaclassicpostlight";
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
    return new AxisAlignedBB(0.117188, 0.000000, 0.117188, 0.882813, 1.250000, 0.882813);
  }

  @Override
  public int getBrightLightXOffset() {
    return 1;
  }

}
