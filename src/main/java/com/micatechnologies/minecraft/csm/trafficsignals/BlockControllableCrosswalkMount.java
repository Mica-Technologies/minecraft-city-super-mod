package com.micatechnologies.minecraft.csm.trafficsignals;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockControllableCrosswalkMount extends AbstractBlockControllableCrosswalkSignal {

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalkmount";
  }

  @Override
  public float getCountdownZOffset() {
    return 0.125f; // rear model: north face at Z=6/16
  }

  @Override
  public float getCountdownYCenter() {
    return 0.625f; // rear model: element Y 2-18, center at 10/16
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
        return new AxisAlignedBB(0.000000, 0.000000, 0.375000, 1.000000, 1.250000, 1.375000);
    }
}
