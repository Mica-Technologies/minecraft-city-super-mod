package com.micatechnologies.minecraft.csm.trafficsignals;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

public class BlockControllableCrosswalkDoubleWordedRightMount
    extends AbstractBlockControllableCrosswalkSignal {

  @Override
  public float getCountdownZOffset() {
    return -1; // double-worded format: countdown overlay not supported
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalkdoublewordedrightmount";
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
        return new AxisAlignedBB(-0.250000, -0.125000, 0.000000, 0.875000, 1.625000, 0.687500);
    }
}
