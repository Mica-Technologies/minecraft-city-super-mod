package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

@MethodsReturnNonnullByDefault
public abstract class AbstractBlockSign extends AbstractBlockRotatableHZEight {

  public static final PropertyBool DOWNWARD = PropertyBool.create("downward");
  public static final PropertyBool SETBACK = PropertyBool.create("setback");

  public AbstractBlockSign() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
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
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB bb =
        new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 0.218750);

    boolean isDownward = state.getValue(DOWNWARD);
    boolean isSetback = state.getValue(SETBACK);

    if (isDownward) {
      bb = new AxisAlignedBB(0.000000, -0.500000, 0.000000, 1.000000, 1.000000, 0.218750);
    }

    if (isSetback) {
      bb = RotationUtils.rotateBoundingBoxByFacing(bb, EnumFacing.SOUTH);
    }

    return bb;
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  public boolean getBlockBelowIsSlab(IBlockAccess source, BlockPos pos) {
    return source.getBlockState(pos.down()).getBlock() instanceof BlockSlab;
  }

  public boolean getBlockIsInFrontOfSignalArm(IBlockAccess source, BlockPos pos) {
    IBlockState sourceBlockState = source.getBlockState(pos);
    if (!(sourceBlockState.getBlock() instanceof AbstractBlockSign)) {
      return false;
    }
    DirectionEight facing8 = source.getBlockState(pos).getValue(FACING);
    if (facing8 == DirectionEight.N) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.NORTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.S) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.E) {
      return source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.W) {
      return source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.NE) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.NORTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.NW) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.NORTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.SE) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.EAST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    } else if (facing8 == DirectionEight.SW) {
      return source.getBlockState(pos.offset(
              BlockUtils.getRelativeFacing(EnumFacing.SOUTH, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole || source.getBlockState(
              pos.offset(BlockUtils.getRelativeFacing(EnumFacing.WEST, EnumFacing.SOUTH)))
          .getBlock() instanceof AbstractBlockTrafficPole;
    }
    return false;
  }

  public boolean getShouldSetback(IBlockAccess source, BlockPos pos) {
    // Priority 1: Check if the block is in front of a signal arm and should be set back
    if (getBlockIsInFrontOfSignalArm(source, pos)) {
      return true;
    }

    // Priority 2: Check the sign directly above
    if (source.getBlockState(pos.up()).getBlock() instanceof AbstractBlockSign) {
      // Only inherit setback if the sign above is in front of a signal arm
      if (getBlockIsInFrontOfSignalArm(source, pos.up())) {
        return true;
      }
    }

    // Priority 3: Check the sign directly below
    if (source.getBlockState(pos.down()).getBlock() instanceof AbstractBlockSign) {
      // Only inherit setback if the sign below is in front of a signal arm
      if (getBlockIsInFrontOfSignalArm(source, pos.down())) {
        return true;
      }
    }

    // No valid reason for setback
    return false;
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
    return new BlockStateContainer(this, FACING, DOWNWARD, SETBACK);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    return state.withProperty(DOWNWARD, getBlockBelowIsSlab(worldIn, pos))
        .withProperty(SETBACK, getShouldSetback(worldIn, pos));
  }
}
