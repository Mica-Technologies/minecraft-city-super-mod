package com.micatechnologies.minecraft.csm.codeutils.light;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The light-up air ({@code csm:lightupair}): an invisible, passable, replaceable block giving
 * light level 15, which a lit light places where its light should fall, since a fixture high up
 * cannot light the ground from its own cell. Core's, so every module may use it: Lighting's
 * fixtures put one in the column below them ({@link CsmLightupAir#projectColumn}), Sports &amp;
 * Venues' stadium light heads a grid of them over the field ({@link CsmLightFootprints}).
 *
 * <p>It stores nothing about which light placed it. Who may remove one is the placing code's
 * business; see {@link CsmLightupAir} and {@link CsmLightFootprints}.</p>
 *
 * <p>Registered from Core's hidden tab ({@code CsmTabCoreHidden}), so it exists whichever modules
 * are installed.</p>
 */
public class BlockLightupAir extends AbstractBlockRotatableNSEWUD {

  /** The registered instance, set by its constructor (Core's hidden tab builds exactly one). */
  private static BlockLightupAir instance;

  public BlockLightupAir() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 1F, 0);
    instance = this;
  }

  /**
   * The registered light-up air block.
   *
   * @return the block, or {@code null} before Core's hidden tab has built it
   */
  public static BlockLightupAir get() {
    return instance;
  }

  @Override
  public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
    return true;
  }

  @Override
  public boolean isReplaceable(IBlockAccess blockAccess, BlockPos pos) {
    return true;
  }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @javax.annotation.Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn,
      BlockPos pos) {
    return NULL_AABB;
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
    return "lightupair";
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
    return new AxisAlignedBB(0D, 0D, 0D, 0D, 0.05D, 0D);
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
    return BlockRenderLayer.CUTOUT;
  }
}

