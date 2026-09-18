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
 * A wall finish hung on the face of any wall, as a blind hangs against a window: painted drywall,
 * ceramic wall tile, a fabric acoustic panel, beadboard or a wood slat wall. One class, constructed
 * by registry name ({@code wall_<kind>_<colour>}).
 *
 * <p>Its facing is the way to the wall, taken from the face it was placed against. Finishes of one
 * kind on the same wall join into one surface, and trim is drawn only where it belongs -- a tile's
 * bullnose cap and a beadboard's chair rail along the top course, an edge trim or an acoustic
 * panel's frame at the ends of the run -- from the joins, which are actual state (left and right
 * as seen from the room, facing the wall). The models come from
 * {@code dev-env-utils/scripts/gen_wall_finishes.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWallFinish extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool LEFT = PropertyBool.create("left");
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  /** How far it stands off the wall; SHARED with gen_wall_finishes.DEPTH. */
  private final AxisAlignedBB north;

  /**
   * Constructs a {@link BlockWallFinish}.
   *
   * @param registryName {@code wall_<kind>_<colour>}
   *
   * @since 1.0
   */
  public BlockWallFinish(String registryName) {
    super(pendingMaterial(registryName), sound(registryName),
        registryName.contains("tile") ? "pickaxe" : null, 0, 0.6F, 2F, 0F, 0);
    this.registryName = registryName;
    this.north = new AxisAlignedBB(0, 0, 0, 1, 1, depth(registryName) / 16.0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    if (registryName.contains("acoustic")) {
      return Material.CLOTH;
    }
    if (registryName.contains("tile")) {
      return Material.ROCK;
    }
    // Drywall comes off by hand, as clay does; rock would need a pickaxe to drop anything.
    return registryName.contains("paint") ? Material.CLAY : Material.WOOD;
  }

  private static SoundType sound(String registryName) {
    if (registryName.contains("acoustic")) {
      return SoundType.CLOTH;
    }
    return registryName.contains("beadboard") || registryName.contains("slatwall")
        ? SoundType.WOOD : SoundType.STONE;
  }

  private static double depth(String registryName) {
    if (registryName.contains("paint")) {
      return 0.25;
    }
    if (registryName.contains("acoustic")) {
      return 1.5;
    }
    return registryName.contains("slatwall") ? 1.5 : 1.0;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, RIGHT, UP, DOWN);
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
   * Against the wall it was placed on; placed on a floor or ceiling, against the wall the player
   * faces.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing wall = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : placer.getHorizontalFacing();
    return getDefaultState().withProperty(FACING, wall);
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
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(LEFT, same(worldIn, pos.offset(f.rotateYCCW()), f))
        .withProperty(RIGHT, same(worldIn, pos.offset(f.rotateY()), f))
        .withProperty(UP, same(worldIn, pos.up(), f))
        .withProperty(DOWN, same(worldIn, pos.down(), f));
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(north, state.getValue(FACING));
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
