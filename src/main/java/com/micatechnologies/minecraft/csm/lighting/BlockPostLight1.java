package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockPostLight1 extends AbstractBrightLightPoleColored {

  @Override
  public String getBlockRegistryName() {
    return "postlight1";
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
    return new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.000000, 0.937500);
  }

  @Override
  public int getBrightLightXOffset() {
    return 1;
  }

}
