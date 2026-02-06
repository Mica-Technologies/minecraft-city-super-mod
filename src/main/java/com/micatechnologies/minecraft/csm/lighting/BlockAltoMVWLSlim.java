package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockAltoMVWLSlim extends AbstractBrightLight {

  @Override
  public String getBlockRegistryName() {
    return "altomvwlslim";
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
    return new AxisAlignedBB(0.406250, 0.237500, 0.812500, 0.656250, 0.737500, 1.000000);
  }

  @Override
  public int getBrightLightXOffset() {
    return 0;
  }

}
