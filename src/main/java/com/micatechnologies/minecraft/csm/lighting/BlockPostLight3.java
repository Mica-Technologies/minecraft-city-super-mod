package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockPostLight3 extends AbstractBrightLightPoleColored {

  @Override
  public String getBlockRegistryName() {
    return "postlight3";
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
    return new AxisAlignedBB(-0.125000, 0.000000, -0.125000, 1.125000, 1.437500, 1.125000);
  }

  @Override
  public int getBrightLightXOffset() {
    return 1;
  }

}
