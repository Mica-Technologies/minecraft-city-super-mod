package com.micatechnologies.minecraft.csm.trafficsigns;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

public class BlockSignpost extends AbstractBlockSign {

  public static final PropertyBool DOWNWARD = PropertyBool.create("downward");

  @Override
  public String getBlockRegistryName() {
    return "signpost";
  }

  /**
   * Creates a new {@link BlockStateContainer} for the block with the required property for
   * rotation.
   *
   * @return a new {@link BlockStateContainer} for the block
   *
   * @see Block#createBlockState()
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, DOWNWARD);
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
        ? new AxisAlignedBB(0.000000, -0.500000, -0.000625, 1.000000, 1.000000, 0.218750)
        : new AxisAlignedBB(0.000000, 0.000000, -0.000625, 1.000000, 1.000000, 0.218750);
  }

  public boolean getBlockBelowIsSlab(IBlockAccess source, BlockPos pos) {
    return source.getBlockState(pos.down()).getBlock() instanceof BlockSlab;
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state, @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    return state.withProperty(DOWNWARD, getBlockBelowIsSlab(worldIn, pos));
  }
}
