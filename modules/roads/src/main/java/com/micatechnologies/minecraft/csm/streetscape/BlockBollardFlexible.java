package com.micatechnologies.minecraft.csm.streetscape;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A flexible delineator: a plastic tube on a bolted base that folds over when it is hit and
 * springs back. It can be walked and driven through, so it has no collision box; its selection
 * box is the tube's, so it can still be clicked and broken.
 *
 * @version 1.0
 */
public class BlockBollardFlexible extends BlockUtilityBox {

  public BlockBollardFlexible(String registryName, UtilityBoxSpec spec) {
    super(registryName, spec);
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
    return true;
  }
}
