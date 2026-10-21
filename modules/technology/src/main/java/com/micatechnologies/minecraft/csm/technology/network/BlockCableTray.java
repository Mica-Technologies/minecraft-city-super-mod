package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A length of cable tray (a wire basket or a ladder rack), the run a building's network cabling
 * lies in above the ceiling or over a row of racks.
 *
 * <p>Stored: the run's axis (along the placer's look) and whether it hangs at the top of its
 * block on rods to the ceiling or lies at the bottom (on top of a rack, say), as a slab is placed:
 * the underside of a block, or a side clicked in its upper half, hangs it. Everything else is
 * actual state: {@code north}, {@code east}, {@code south} and {@code west} say which way the
 * tray reaches out of its middle. A tray runs its full length along its own axis, open at a run's
 * end; it also reaches sideways to a tray beside it at the same height whose own run points at it,
 * which makes a tee, a corner or a crossing. Where it turns, an end with no tray beyond it is
 * closed with a rail rather than left reaching two pixels past the corner. Two parallel runs side
 * by side do not join into a mat. The side
 * rails (and a hanging tray's rods) are drawn only on a side the tray does not reach out of. The
 * models come from {@code dev-env-utils/scripts/gen_technology_networking.py}.</p>
 *
 * @since 2026.10
 */
public class BlockCableTray extends AbstractBlock {

  public static final PropertyEnum<Axis> AXIS = PropertyEnum.create("axis", Axis.class, Axis.X,
      Axis.Z);
  public static final PropertyBool TOP = PropertyBool.create("top");
  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boxBottom;
  private final AxisAlignedBB boxTop;

  /**
   * @param registryName the block's registry name
   * @param bottom       the tray's height lying at the bottom of its block, in sixteenths: y0, y1
   * @param top          its height hung at the top, rods included: y0, y1
   */
  public BlockCableTray(String registryName, double[] bottom, double[] top) {
    super(pending(registryName), SoundType.METAL, "pickaxe", 0, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.boxBottom = new AxisAlignedBB(0, bottom[0] / 16, 0, 1, bottom[1] / 16, 1);
    this.boxTop = new AxisAlignedBB(0, top[0] / 16, 0, 1, top[1] / 16, 1);
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, Axis.Z).withProperty(TOP, false)
        .withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
        .withProperty(WEST, false));
  }

  private static Material pending(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, TOP, NORTH, EAST, SOUTH, WEST);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? Axis.Z : Axis.X)
        .withProperty(TOP, (meta & 2) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(AXIS) == Axis.X ? 1 : 0) | (state.getValue(TOP) ? 2 : 0);
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    boolean top = facing == EnumFacing.DOWN || (facing != EnumFacing.UP && hitY > 0.5F);
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis())
        .withProperty(TOP, top);
  }

  /**
   * Whether the tray reaches out of its middle towards {@code side}: sideways to a tray at the
   * same height whose run points this way; along its own run to any tray at the same height, and
   * at an open end too, unless the tray turns here (reaches sideways), when that end is closed.
   */
  private boolean reaches(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing side) {
    Axis axis = state.getValue(AXIS);
    if (side.getAxis() != axis) {
      return pointsHere(world, pos, state, side);
    }
    if (trayAt(world, pos, state, side) != null) {
      return true;
    }
    for (EnumFacing across : EnumFacing.HORIZONTALS) {
      if (across.getAxis() != axis && pointsHere(world, pos, state, across)) {
        return false;
      }
    }
    return true;
  }

  /** The tray beside {@code pos} towards {@code side} at the same height, or null. */
  @Nullable
  private static IBlockState trayAt(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() instanceof BlockCableTray
        && other.getValue(TOP) == state.getValue(TOP) ? other : null;
  }

  /** Whether a tray beside towards {@code side}, at the same height, runs towards this one. */
  private static boolean pointsHere(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side) {
    IBlockState other = trayAt(world, pos, state, side);
    return other != null && other.getValue(AXIS) == side.getAxis();
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(NORTH, reaches(worldIn, pos, state, EnumFacing.NORTH))
        .withProperty(EAST, reaches(worldIn, pos, state, EnumFacing.EAST))
        .withProperty(SOUTH, reaches(worldIn, pos, state, EnumFacing.SOUTH))
        .withProperty(WEST, reaches(worldIn, pos, state, EnumFacing.WEST));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(TOP) ? boxTop : boxBottom;
  }

  @Nonnull
  @Override
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

  /** Cutout, not mipped: a mipped mesh of one-texel wires fades out at a distance. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
