package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A slatted timber screen: vertical slats with gaps between, standing along one edge of its cell,
 * as a wall screen, a balcony privacy fin or a sunshade. One class, constructed with its registry
 * name; the generator ({@code dev-env-utils/scripts/gen_screen_blocks.py}) draws every timber.
 *
 * <p>Stored: {@link #FACING}, the edge it stands on (the way to the wall it was placed against).
 * Read from the world: {@link #UP} and {@link #DOWN}, whether the same screen carries on above or
 * below. The slats run on through a stack, and a rail caps the top and the bottom of each run, so a
 * screen two or ten blocks tall is one screen with one head and one sill.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockSlatScreen extends AbstractBlock {

  /** The edge of the cell the screen stands on: the way to the wall it was placed against. */
  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /** The same screen, on the same edge, carries on above (actual state, not stored). */
  public static final PropertyBool UP = PropertyBool.create("up");

  /** The same screen, on the same edge, carries on below (actual state, not stored). */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** The screen's depth from its edge, in blocks: four pixels. */
  private static final double DEPTH = 4 / 16.0D;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockSlatScreen}.
   *
   * @param registryName the registry name, such as {@code slat_screen_teak}
   *
   * @since 1.0
   */
  public BlockSlatScreen(String registryName) {
    super(pendingMaterial(registryName), SoundType.WOOD, "axe", 0, 2F, 5F, 0F, 0);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(UP, false).withProperty(DOWN, false));
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UP, DOWN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /**
   * Against the wall it was placed on; placed on a floor or a ceiling, on the far edge of the
   * cell, ahead of the player, as a screen is set along the edge of a balcony or a terrace.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing edge = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : placer.getHorizontalFacing();
    return getDefaultState().withProperty(FACING, edge);
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(UP, same(worldIn, pos.up(), facing))
        .withProperty(DOWN, same(worldIn, pos.down(), facing));
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    switch (state.getValue(FACING)) {
      case SOUTH:
        return new AxisAlignedBB(0, 0, 1 - DEPTH, 1, 1, 1);
      case EAST:
        return new AxisAlignedBB(1 - DEPTH, 0, 0, 1, 1, 1);
      case WEST:
        return new AxisAlignedBB(0, 0, 0, DEPTH, 1, 1);
      case NORTH:
      default:
        return new AxisAlignedBB(0, 0, 0, 1, 1, DEPTH);
    }
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  /**
   * Solid: the slats are plain timber, and the gaps between them are gaps in the model, not in a
   * texture.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
