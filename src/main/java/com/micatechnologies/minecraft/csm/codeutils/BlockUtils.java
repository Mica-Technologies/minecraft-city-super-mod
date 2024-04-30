package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockUtils {

  public static boolean getIsBlockToSide(IBlockAccess worldIn, BlockPos pos, EnumFacing facing,
      Class<?>... ignoreBlock) {
    BlockPos side = pos.offset(facing);
    boolean oldMethod = !worldIn.getBlockState(side).getBlock().isReplaceable(worldIn, side);

    // Check if the block is in the ignore list
    if (ignoreBlock != null) {
      for (Class<?> block : ignoreBlock) {
        if (worldIn.getBlockState(side).getBlock().getClass().equals(block)) {
          return false;
        }
      }
    }

    return oldMethod;
  }

  public static boolean getIsBlockToSideAdjusted(IBlockAccess worldIn, BlockPos pos,
      EnumFacing facing, EnumFacing baseFacing, Class<?>... ignoreBlock) {
    // Get enum facing based on the base facing (NSEWUD).
    // The default base facing is north, we need to adjust the facing based on the base facing.
    EnumFacing adjustedFacing = facing;
    if (baseFacing != EnumFacing.NORTH) {
      switch (facing) {
        case NORTH:
          adjustedFacing = baseFacing;
          break;
        case EAST:
          adjustedFacing = baseFacing.rotateY();
          break;
        case SOUTH:
          adjustedFacing = baseFacing.getOpposite();
          break;
        case WEST:
          adjustedFacing = baseFacing.rotateYCCW();
          break;
        case UP:
          adjustedFacing = EnumFacing.UP;
          break;
        case DOWN:
          adjustedFacing = EnumFacing.DOWN;
          break;
      }
    }

    //
  }

  /**
   * Retrieves whether there is a block (full or not) to the north of the given block.
   *
   * @param worldIn The world.
   * @param pos     The position of the block.
   *
   * @return {@code true} if there is a block to the north, {@code false} otherwise.
   */
  public static boolean getIsBlockToNorth(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    return getIsBlockToSide(worldIn, pos, pos.north(), ignoreBlock);
  }

  /**
   * Retrieves whether there is a block (full or not) to the east of the given block.
   *
   * @param worldIn The world.
   * @param pos     The position of the block.
   *
   * @return {@code true} if there is a block to the east, {@code false} otherwise.
   */
  public static boolean getIsBlockToEast(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    return getIsBlockToSide(worldIn, pos, pos.east(), ignoreBlock);
  }

  /**
   * Retrieves whether there is a block (full or not) to the west of the given block.
   *
   * @param worldIn The world.
   * @param pos     The position of the block.
   *
   * @return {@code true} if there is a block to the west, {@code false} otherwise.
   */
  public static boolean getIsBlockToWest(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    return getIsBlockToSide(worldIn, pos, pos.west(), ignoreBlock);
  }

  /**
   * Retrieves whether there is a block (full or not) to the south of the given block.
   *
   * @param worldIn The world.
   * @param pos     The position of the block.
   *
   * @return {@code true} if there is a block to the south, {@code false} otherwise.
   */
  public static boolean getIsBlockToSouth(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    return getIsBlockToSide(worldIn, pos, pos.south(), ignoreBlock);
  }

  /**
   * Retrieves whether there is a block (full or not) above the given block.
   *
   * @param worldIn The world.
   * @param pos     The position of the block.
   *
   * @return {@code true} if there is a block above, {@code false} otherwise.
   */
  public static boolean getIsBlockAbove(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    return getIsBlockToSide(worldIn, pos, pos.up(), ignoreBlock);
  }

  /**
   * Retrieves whether there is a block (full or not) below the given block.
   *
   * @param worldIn The world.
   * @param pos     The position of the block.
   *
   * @return {@code true} if there is a block below, {@code false} otherwise.
   */
  public static boolean getIsBlockBelow(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    return getIsBlockToSide(worldIn, pos, pos.down(), ignoreBlock);
  }
}
