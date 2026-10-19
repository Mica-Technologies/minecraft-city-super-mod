package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
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
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;

/**
 * A department colour stripe: a thin wayfinding band hung on a bare wall, at one of four heights.
 * One class, constructed by registry name ({@code wall_stripe_<colour>}).
 *
 * <p>Stored: the facing (the way to the wall, horizontal index, bits 0-1) and the height (bits
 * 2-3, {@link #BANDS}). Stripes of one colour at one height on one wall meet end to end with no
 * trim, so a run reads as one band, and in an inside corner the corner cell draws the band of the
 * other wall's run on its side too ({@code left} / {@code right}, actual state), as the wall
 * finishes do. Placing one beside or on another takes its height, so a run is laid by clicking
 * along the wall. 64 states. The models come from {@code gen_wall_finishes.py}.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockWallStripe extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyInteger HEIGHT = PropertyInteger.create("height", 0, 3);
  public static final PropertyBool LEFT = PropertyBool.create("left");
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /** SHARED with gen_wall_finishes.STRIPE_Y and STRIPE_D: each height's band, in px. */
  static final double[][] BANDS = {{2, 4}, {6, 8}, {10, 12}, {13, 15}};
  private static final double DEPTH = 0.25;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockWallStripe}.
   *
   * @param registryName {@code wall_stripe_<colour>}
   *
   * @since 1.0
   */
  public BlockWallStripe(String registryName) {
    super(pending(registryName), SoundType.CLOTH, null, 0, 0.3F, 1F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(HEIGHT, 2).withProperty(LEFT, false).withProperty(RIGHT, false));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pending(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.CLAY;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, HEIGHT, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(HEIGHT, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(HEIGHT) << 2;
  }

  /**
   * Against the wall clicked (on a floor or ceiling, the wall the player faces), at the height of
   * a stripe beside or below it if there is one, otherwise at about eye level less a little
   * (height 2).
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing wall = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : placer.getHorizontalFacing();
    int height = 2;
    for (EnumFacing side : new EnumFacing[]{wall.rotateY(), wall.rotateYCCW(), EnumFacing.DOWN,
        EnumFacing.UP}) {
      IBlockState other = worldIn.getBlockState(pos.offset(side));
      if (other.getBlock() instanceof BlockWallStripe) {
        height = other.getValue(HEIGHT);
        break;
      }
    }
    return getDefaultState().withProperty(FACING, wall).withProperty(HEIGHT, height);
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing, int height) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(FACING) == facing
        && other.getValue(HEIGHT) == height;
  }

  private static boolean wall(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos at = pos.offset(side);
    return world.getBlockState(at).getBlockFaceShape(world, at, side.getOpposite())
        == BlockFaceShape.SOLID;
  }

  /**
   * In an inside corner with a wall on {@code side}, whether the same stripe's run on that wall
   * reaches this cell: the cell in front of this one along that wall holds it, facing that wall.
   */
  private boolean corner(IBlockAccess world, BlockPos pos, EnumFacing facing, EnumFacing side,
      int height) {
    return wall(world, pos, side) && same(world, pos.offset(facing.getOpposite()), side, height);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    int h = state.getValue(HEIGHT);
    return state.withProperty(LEFT, corner(worldIn, pos, f, f.rotateYCCW(), h))
        .withProperty(RIGHT, corner(worldIn, pos, f, f.rotateY(), h));
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double[] band = BANDS[state.getValue(HEIGHT)];
    return BlockGarageDoor.turn(new AxisAlignedBB(0, band[0] / 16, 0, 1, band[1] / 16,
        DEPTH / 16), state.getValue(FACING));
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
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
