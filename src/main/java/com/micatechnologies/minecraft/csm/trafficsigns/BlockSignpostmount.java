package com.micatechnologies.minecraft.csm.trafficsigns;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

public class BlockSignpostmount extends AbstractBlockSign {

  @Override
  public String getBlockRegistryName() {
    return "signpostmount";
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
  public @NotNull AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return getBlockBelowIsSlab(source, pos)
        ? new AxisAlignedBB(0.406250, -0.500000, 0.031250, 0.593750, 1.000000, 0.218750)
        : new AxisAlignedBB(0.406250, 0.000000, 0.031250, 0.593750, 1.000000, 0.218750);
  }
}
