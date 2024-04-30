package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
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

public class BlockTrafficPoleLargeUnpainted extends AbstractBlockRotatableNSEWUD {

  public BlockTrafficPoleLargeUnpainted() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
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
    return "trafficpoleverticalunpainted";
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
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
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

  public static final PropertyBool MOUNT_EAST = PropertyBool.create("mounteast");
  public static final PropertyBool MOUNT_WEST = PropertyBool.create("mountwest");
  public static final PropertyBool MOUNT_NORTH = PropertyBool.create("mountnorth");
  public static final PropertyBool MOUNT_SOUTH = PropertyBool.create("mountsouth");
  public static final PropertyBool MOUNT_UP = PropertyBool.create("mountup");
  public static final PropertyBool MOUNT_DOWN = PropertyBool.create("mountdown");

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
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_NORTH, MOUNT_SOUTH,
        MOUNT_UP, MOUNT_DOWN);
  }


  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state, @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    Class<?>[] ignoreBlock = {BlockTrafficPoleLargeGray.class};
    boolean isBlockToEast = BlockUtils.getIsBlockToEast(worldIn, pos, ignoreBlock);
    boolean isBlockToWest = BlockUtils.getIsBlockToWest(worldIn, pos, ignoreBlock);
    boolean isBlockToNorth = BlockUtils.getIsBlockToNorth(worldIn, pos, ignoreBlock);
    boolean isBlockToSouth = BlockUtils.getIsBlockToSouth(worldIn, pos, ignoreBlock);
    boolean isBlockAbove = BlockUtils.getIsBlockAbove(worldIn, pos, ignoreBlock);
    boolean isBlockBelow = BlockUtils.getIsBlockBelow(worldIn, pos, ignoreBlock);
    return state.withProperty(MOUNT_EAST, isBlockToEast)
        .withProperty(MOUNT_WEST, isBlockToWest)
        .withProperty(MOUNT_NORTH, isBlockToNorth)
        .withProperty(MOUNT_SOUTH, isBlockToSouth)
        .withProperty(MOUNT_UP, isBlockAbove)
        .withProperty(MOUNT_DOWN, isBlockBelow);
  }
}
