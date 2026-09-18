package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
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
 * A full block of glass that joins others of its kind into one pane of glass: clear, tinted,
 * one-way, wired, bullet-resistant or frosted ({@link GlassKind}).
 *
 * <p>No face is drawn against another block of the same glass, so a wall or box of it has no seams,
 * and a thin dark bronze frame runs only along the outside edges of the whole -- where two faces
 * that are both on the outside meet. Glass of a different kind is a different window, so a frame
 * runs between them.</p>
 *
 * <p>One-way glass is dark on the side it faces -- away from the player who placed it, who is
 * taken to be standing inside -- and clear on the others. It needs nothing but that: Minecraft
 * never draws the back of a face, so from outside only the dark face is seen and from inside only
 * the clear one.</p>
 *
 * <p>Stored: the facing, which only one-way glass uses. The six neighbours are actual state. The
 * models come from {@code dev-env-utils/scripts/gen_glazing.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockGlazing extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockGlazing}.
   *
   * @param registryName {@code glass_<kind>}
   *
   * @since 1.0
   */
  public BlockGlazing(String registryName) {
    super(pendingMaterial(registryName), SoundType.GLASS, "pickaxe", 0,
        GlassKind.fromRegistryName(registryName).hardness(),
        GlassKind.fromRegistryName(registryName).resistance(), 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.GLASS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN);
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
   * Faces away from the player who places it: that is the outside, which one-way glass draws
   * dark.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  private boolean same(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() == this;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(NORTH, same(worldIn, pos.north()))
        .withProperty(EAST, same(worldIn, pos.east()))
        .withProperty(SOUTH, same(worldIn, pos.south()))
        .withProperty(WEST, same(worldIn, pos.west()))
        .withProperty(UP, same(worldIn, pos.up()))
        .withProperty(DOWN, same(worldIn, pos.down()));
  }

  /**
   * No face against the same glass, so a wall of it has no seams.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean shouldSideBeRendered(@Nonnull IBlockState blockState,
      @Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    if (blockAccess.getBlockState(pos.offset(side)).getBlock() == this) {
      return false;
    }
    return super.shouldSideBeRendered(blockState, blockAccess, pos, side);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.SOLID;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}
