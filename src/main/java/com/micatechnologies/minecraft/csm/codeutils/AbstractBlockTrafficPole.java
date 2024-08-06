package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.trafficsignals.AbstractBlockControllableCrosswalkSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter1;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter2;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
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

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlockRotatableNSEWUD} with the addition of traffic pole mounting properties
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @see AbstractBlockRotatableNSEWUD
 */
public abstract class AbstractBlockTrafficPole extends AbstractBlockRotatableNSEWUD {

  /**
   * The east mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_EAST = PropertyBool.create("mounteast");

  /**
   * The west mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_WEST = PropertyBool.create("mountwest");

  /**
   * The up mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_UP = PropertyBool.create("mountup");

  /**
   * The down mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_DOWN = PropertyBool.create("mountdown");

  /**
   * The list of global ignore blocks.
   */
  public static final Class<?>[] IGNORE_BLOCK = {AbstractBlockControllableCrosswalkSignal.class,
      BlockControllableCrosswalkTweeter1.class, BlockControllableCrosswalkTweeter2.class};


  /**
   * Constructs an {@link AbstractBlockTrafficPole} instance.
   *
   * @since 1.0
   */
  public AbstractBlockTrafficPole() {
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
    return SQUARE_BOUNDING_BOX;
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
    return true;
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
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN);
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

    // Update the block state with the presence of blocks in each direction
    return state.withProperty(MOUNT_EAST, isBlockToEast).withProperty(MOUNT_WEST, isBlockToWest)
        .withProperty(MOUNT_UP, isBlockAbove).withProperty(MOUNT_DOWN, isBlockBelow);
  }

  /**
   * Abstract method which must be implemented to return the block classes of blocks which should be
   * ignored when checking for adjacent blocks.
   *
   * @return Array of block classes to ignore when checking for adjacent blocks.
   */
  public abstract Class<?>[] getIgnoreBlock();
}
