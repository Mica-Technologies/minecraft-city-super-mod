package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Utility class providing helper methods for querying block adjacency, computing relative facing
 * directions, and other common block-related operations used throughout the mod.
 *
 * @author Mica Technologies
 */
public class BlockUtils {

  /**
   * Precomputed lookup table for {@link #getRelativeFacing(EnumFacing, EnumFacing)}. Index by
   * {@code [baseFacing.ordinal()][direction.ordinal()]} to skip the nested-switch evaluation on
   * every call. Populated once at class load.
   *
   * <p>Hoisted into {@link BlockUtils} so both {@link AbstractBlockTrafficPole} (regular) and
   * {@link AbstractBlockTrafficPoleDiagonal} share a single cache — the diagonal variant had
   * its own local copy prior to this refactor.
   */
  private static final EnumFacing[][] RELATIVE_FACING_TABLE;

  /**
   * Precomputed lookup table mirroring {@link #RELATIVE_FACING_TABLE} but with
   * {@link EnumFacing#getOpposite()} already applied. Used by pole {@code getActualState}
   * implementations that walk outward from the pole body along its rotated axes.
   */
  private static final EnumFacing[][] RELATIVE_FACING_OPPOSITE_TABLE;

  static {
    EnumFacing[] values = EnumFacing.values();
    RELATIVE_FACING_TABLE = new EnumFacing[values.length][values.length];
    RELATIVE_FACING_OPPOSITE_TABLE = new EnumFacing[values.length][values.length];
    for (EnumFacing base : values) {
      for (EnumFacing dir : values) {
        EnumFacing relative = computeRelativeFacing(base, dir);
        RELATIVE_FACING_TABLE[base.ordinal()][dir.ordinal()] = relative;
        RELATIVE_FACING_OPPOSITE_TABLE[base.ordinal()][dir.ordinal()] = relative.getOpposite();
      }
    }
  }

  /**
   * Returns the rotation-adjusted facing that was stored for
   * {@code (baseFacing, direction)} when the lookup table was built.
   * Equivalent to {@link #getRelativeFacing(EnumFacing, EnumFacing)}.{@code getOpposite()}
   * but without the per-call method invocations — this is hot in pole {@code getActualState}
   * paths that run per-render.
   *
   * @param baseFacing the block's facing direction
   * @param direction  the direction relative to the block to translate + invert
   * @return the translated direction, flipped to its opposite
   */
  public static EnumFacing getRelativeFacingOpposite(EnumFacing baseFacing, EnumFacing direction) {
    return RELATIVE_FACING_OPPOSITE_TABLE[baseFacing.ordinal()][direction.ordinal()];
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
    return getIsBlockToSide(worldIn, pos.north(), ignoreBlock);
  }

  /**
   * Determines whether there is a block (full or not) at the given position, optionally ignoring
   * specified block classes.
   * <p>
   * This method checks if there is a block present at the given position in the world. It can also
   * be configured to ignore certain types of blocks, which is useful when you want to exclude
   * specific blocks from the check. The method considers a block to be present if it is not
   * replaceable (e.g., not air or other replaceable blocks).
   * </p>
   *
   * @param worldIn     The world in which the block is located.
   * @param pos         The position of the block to check.
   * @param ignoreBlock Varargs parameter for block classes to be ignored in the check. If a block
   *                    at the position matches any of the provided classes, it will be ignored.
   *
   * @return {@code true} if there is a block at the given position that is not in the ignore list,
   *     {@code false} otherwise.
   */
  public static boolean getIsBlockToSide(IBlockAccess worldIn, BlockPos pos,
      Class<?>... ignoreBlock) {
    Block blockAtPos = worldIn.getBlockState(pos).getBlock();
    boolean defaultMethod = !blockAtPos.isReplaceable(worldIn, pos);

    // Check if the block is in the ignore list
    if (ignoreBlock != null) {
      Class<?> blockClass = blockAtPos.getClass();
      for (Class<?> ignored : ignoreBlock) {
        if (blockClass.equals(ignored) || ignored.isAssignableFrom(blockClass)) {
          return false;
        }
      }
    }

    return defaultMethod;
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
    return getIsBlockToSide(worldIn, pos.east(), ignoreBlock);
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
    return getIsBlockToSide(worldIn, pos.west(), ignoreBlock);
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
    return getIsBlockToSide(worldIn, pos.south(), ignoreBlock);
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
    return getIsBlockToSide(worldIn, pos.up(), ignoreBlock);
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
    return getIsBlockToSide(worldIn, pos.down(), ignoreBlock);
  }

  /**
   * Translates a direction relative to a block based on the block's facing direction.
   * <p>
   * This method adjusts the given direction (NORTH, EAST, SOUTH, WEST, UP, DOWN) to account for the
   * rotation of the block itself. For example, if the block is facing EAST and the direction to
   * check is NORTH, this method will translate that to EAST because when the block is rotated to
   * face EAST, what was NORTH relative to the block becomes EAST.
   * </p>
   *
   * @param baseFacing The facing direction of the block itself.
   * @param direction  The direction relative to the block to be translated.
   *
   * @return The actual direction adjusted for the block's facing direction.
   */
  public static EnumFacing getRelativeFacing(EnumFacing baseFacing, EnumFacing direction) {
    return RELATIVE_FACING_TABLE[baseFacing.ordinal()][direction.ordinal()];
  }

  /**
   * Computes (without caching) the rotation-adjusted facing. This is the original switch-based
   * implementation, now invoked only once per (base, direction) pair at class-load time to
   * populate {@link #RELATIVE_FACING_TABLE}.
   */
  private static EnumFacing computeRelativeFacing(EnumFacing baseFacing, EnumFacing direction) {
    switch (baseFacing) {
      case EAST:
        switch (direction) {
          case NORTH:
            return EnumFacing.EAST;
          case EAST:
            return EnumFacing.SOUTH;
          case SOUTH:
            return EnumFacing.WEST;
          case WEST:
            return EnumFacing.NORTH;
          case UP:
            return EnumFacing.UP;
          case DOWN:
            return EnumFacing.DOWN;
        }
      case SOUTH:
        switch (direction) {
          case NORTH:
            return EnumFacing.SOUTH;
          case EAST:
            return EnumFacing.WEST;
          case SOUTH:
            return EnumFacing.NORTH;
          case WEST:
            return EnumFacing.EAST;
          case UP:
            return EnumFacing.UP;
          case DOWN:
            return EnumFacing.DOWN;
        }
      case WEST:
        switch (direction) {
          case NORTH:
            return EnumFacing.WEST;
          case EAST:
            return EnumFacing.NORTH;
          case SOUTH:
            return EnumFacing.EAST;
          case WEST:
            return EnumFacing.SOUTH;
          case UP:
            return EnumFacing.UP;
          case DOWN:
            return EnumFacing.DOWN;
        }
      case UP:
        switch (direction) {
          case NORTH:
            return EnumFacing.UP;
          case EAST:
            return EnumFacing.EAST;
          case SOUTH:
            return EnumFacing.DOWN;
          case WEST:
            return EnumFacing.WEST;
          case UP:
            return EnumFacing.SOUTH;
          case DOWN:
            return EnumFacing.NORTH;
        }
      case DOWN:
        switch (direction) {
          case NORTH:
            return EnumFacing.DOWN;
          case EAST:
            return EnumFacing.EAST;
          case SOUTH:
            return EnumFacing.UP;
          case WEST:
            return EnumFacing.WEST;
          case UP:
            return EnumFacing.NORTH;
          case DOWN:
            return EnumFacing.SOUTH;
        }
      case NORTH:
      default:
        return direction;
    }
  }

  public static EnumFacing getOppositeFacing(EnumFacing baseFacing, EnumFacing direction) {
    switch (baseFacing) {
      case EAST:
        switch (direction) {
          case NORTH:
            return EnumFacing.WEST;
          case EAST:
            return EnumFacing.NORTH;
          case SOUTH:
            return EnumFacing.EAST;
          case WEST:
            return EnumFacing.SOUTH;
          case UP:
            return EnumFacing.UP;
          case DOWN:
            return EnumFacing.DOWN;
        }
      case SOUTH:
        switch (direction) {
          case NORTH:
            return EnumFacing.SOUTH;
          case EAST:
            return EnumFacing.WEST;
          case SOUTH:
            return EnumFacing.NORTH;
          case WEST:
            return EnumFacing.EAST;
          case UP:
            return EnumFacing.UP;
          case DOWN:
            return EnumFacing.DOWN;
        }
      case WEST:
        switch (direction) {
          case NORTH:
            return EnumFacing.EAST;
          case EAST:
            return EnumFacing.SOUTH;
          case SOUTH:
            return EnumFacing.WEST;
          case WEST:
            return EnumFacing.NORTH;
          case UP:
            return EnumFacing.UP;
          case DOWN:
            return EnumFacing.DOWN;
        }
      case UP:
        switch (direction) {
          case NORTH:
            return EnumFacing.DOWN;
          case EAST:
            return EnumFacing.WEST;
          case SOUTH:
            return EnumFacing.UP;
          case WEST:
            return EnumFacing.EAST;
          case UP:
            return EnumFacing.SOUTH;
          case DOWN:
            return EnumFacing.NORTH;
        }
      case DOWN:
        switch (direction) {
          case NORTH:
            return EnumFacing.UP;
          case EAST:
            return EnumFacing.EAST;
          case SOUTH:
            return EnumFacing.DOWN;
          case WEST:
            return EnumFacing.WEST;
          case UP:
            return EnumFacing.NORTH;
          case DOWN:
            return EnumFacing.SOUTH;
        }
      case NORTH:
      default:
        return direction.getOpposite();
    }
  }

}
