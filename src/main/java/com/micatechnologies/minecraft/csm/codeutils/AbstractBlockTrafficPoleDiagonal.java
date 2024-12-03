package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlockTrafficPole} with the addition of diagonal traffic pole mounting properties
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @see AbstractBlockTrafficPole
 */
public abstract class AbstractBlockTrafficPoleDiagonal extends AbstractBlockTrafficPole {

  /**
   * The east mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_NORTH = PropertyBool.create("mountnorth");

  /**
   * The west mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_SOUTH = PropertyBool.create("mountsouth");

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
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
        MOUNT_NORTH, MOUNT_SOUTH);
  }

  /**
   * Updates the block state based on the presence of adjacent blocks, considering the block's
   * facing direction.
   * <p>
   * This method is called to retrieve the actual state of the block, taking into account its
   * environment. It checks for the presence of blocks in each direction (east, west, north, south,
   * up, down) relative to the block's facing direction. The method uses helper methods to determine
   * if there are blocks adjacent to the block and updates the block state with properties
   * indicating the presence of these blocks.
   * </p>
   *
   * @param state   The current block state.
   * @param worldIn The world in which the block is located.
   * @param pos     The position of the block in the world.
   *
   * @return The updated block state with properties indicating the presence of adjacent blocks.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);

    Class<?>[] blockIgnoreBlock = getIgnoreBlock();
    Class<?>[] ignoreBlock;
    if (blockIgnoreBlock == null) {
      ignoreBlock = IGNORE_BLOCK;
    } else {
      ignoreBlock = new Class<?>[IGNORE_BLOCK.length + blockIgnoreBlock.length];
      System.arraycopy(IGNORE_BLOCK, 0, ignoreBlock, 0, IGNORE_BLOCK.length);
      System.arraycopy(blockIgnoreBlock, 0, ignoreBlock, IGNORE_BLOCK.length,
          blockIgnoreBlock.length);
    }

    // Check for blocks in each direction relative to the block's facing direction
    boolean isBlockToEast = BlockUtils.getIsBlockToSide(worldIn,
        pos.offset(BlockUtils.getRelativeFacing(facing, EnumFacing.EAST).getOpposite()),
        ignoreBlock);
    boolean isBlockToWest = BlockUtils.getIsBlockToSide(worldIn,
        pos.offset(BlockUtils.getRelativeFacing(facing, EnumFacing.WEST).getOpposite()),
        ignoreBlock);
    boolean isBlockAbove = BlockUtils.getIsBlockToSide(worldIn,
        pos.offset(BlockUtils.getRelativeFacing(facing, EnumFacing.UP).getOpposite()), ignoreBlock);
    boolean isBlockBelow = BlockUtils.getIsBlockToSide(worldIn,
        pos.offset(BlockUtils.getRelativeFacing(facing, EnumFacing.DOWN).getOpposite()),
        ignoreBlock);
    boolean isBlockToNorth = BlockUtils.getIsBlockToSide(worldIn,
        pos.offset(BlockUtils.getRelativeFacing(facing, EnumFacing.NORTH).getOpposite()),
        ignoreBlock);
    boolean isBlockToSouth = BlockUtils.getIsBlockToSide(worldIn,
        pos.offset(BlockUtils.getRelativeFacing(facing, EnumFacing.SOUTH).getOpposite()),
        ignoreBlock);

    // Update the block state with the presence of blocks in each direction
    return state.withProperty(MOUNT_EAST, isBlockToEast)
        .withProperty(MOUNT_WEST, isBlockToWest)
        .withProperty(MOUNT_UP, isBlockAbove)
        .withProperty(MOUNT_DOWN, isBlockBelow)
        .withProperty(MOUNT_NORTH, isBlockToNorth)
        .withProperty(MOUNT_SOUTH, isBlockToSouth);
  }
}
