package com.micatechnologies.minecraft.csm.trafficaccessories.truss;

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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A galvanized box truss for overhead sign structures, and for anything else a truss carries:
 * four chords at the corners, laced on every face (gen_sign_truss.py).
 *
 * <p>Its axis is stored. Along x or z it spans (a sign bridge's span, a cantilever's arm); along y
 * it stands (a bridge's leg, a cantilever's upright). Placed like a log: its axis is the axis of
 * the face it is placed against. Everything else comes from the neighbours: an end frame where the
 * truss does not carry on along its axis ({@link #END_NEG}, {@link #END_POS}), and a base plate with
 * anchor bolts where a standing truss stands on something that is not truss ({@link #BASE}).</p>
 *
 * <p>The 2x2 truss is the same block at twice the section: each block is one quarter, and works
 * out which from its neighbours across the section ({@link #A_HIGH}, {@link #B_HIGH}: whether the
 * section carries on below it along the plane's first and second axes), as the tower crane's 2x2
 * mast does. Nothing about it is stored but its axis.</p>
 *
 * <p>It collides as a full block: between its chords a 1x1 truss is twelve sixteenths wide, which
 * a player would otherwise fall into from on top.</p>
 *
 * @since 2026.10
 */
public class BlockSignTruss extends AbstractBlock {

  public static final PropertyEnum<EnumFacing.Axis> AXIS =
      PropertyEnum.create("axis", EnumFacing.Axis.class);
  /** Nothing carries the truss on towards the negative end of its axis: an end frame. */
  public static final PropertyBool END_NEG = PropertyBool.create("end_neg");
  /** Nothing carries it on towards the positive end. */
  public static final PropertyBool END_POS = PropertyBool.create("end_pos");
  /** A standing truss on something that is not truss: a base plate. */
  public static final PropertyBool BASE = PropertyBool.create("base");
  /** A 2x2 quarter on the high side of the section's first cross axis. */
  public static final PropertyBool A_HIGH = PropertyBool.create("a_high");
  /** A 2x2 quarter on the high side of the section's second cross axis. */
  public static final PropertyBool B_HIGH = PropertyBool.create("b_high");

  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final boolean large;

  /**
   * Constructs a truss.
   *
   * @param registryName its registry name
   * @param large        whether it is the 2x2 section (each block a quarter)
   */
  public BlockSignTruss(String registryName, boolean large) {
    super(stash(registryName, large), SoundType.METAL, "pickaxe", 1, 3.0F, 12.0F, 0F, 0);
    this.registryName = registryName;
    this.large = large;
    PENDING.remove();
    IBlockState d = blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y)
        .withProperty(END_NEG, false).withProperty(END_POS, false).withProperty(BASE, false);
    if (large) {
      d = d.withProperty(A_HIGH, false).withProperty(B_HIGH, false);
    }
    setDefaultState(d);
  }

  private static Material stash(String registryName, boolean large) {
    PENDING.set(new Object[]{registryName, large});
    return Material.IRON;
  }

  private boolean isLarge() {
    Object[] p = PENDING.get();
    return p != null ? (Boolean) p[1] : large;
  }

  @Override
  public String getBlockRegistryName() {
    Object[] p = PENDING.get();
    return registryName != null ? registryName : (String) p[0];
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return isLarge()
        ? new CsmBlockStateContainer(this, AXIS, END_NEG, END_POS, BASE, A_HIGH, B_HIGH)
        : new CsmBlockStateContainer(this, AXIS, END_NEG, END_POS, BASE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, EnumFacing.Axis.values()[Math.min(2, meta & 3)]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS).ordinal();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, facing.getAxis());
  }

  /** Whether a truss of this size on this axis is at a position. */
  private boolean isTruss(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockSignTruss && ((BlockSignTruss) s.getBlock()).large == large
        && s.getValue(AXIS) == axis;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing.Axis axis = state.getValue(AXIS);
    EnumFacing neg = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis);
    EnumFacing pos_ = neg.getOpposite();
    boolean endNeg = !isTruss(world, pos.offset(neg), axis);
    boolean base = axis == EnumFacing.Axis.Y && endNeg
        && !(world.getBlockState(pos.down()).getBlock() instanceof BlockSignTruss)
        && !world.isAirBlock(pos.down());
    state = state.withProperty(END_NEG, endNeg)
        .withProperty(END_POS, !isTruss(world, pos.offset(pos_), axis))
        .withProperty(BASE, base);
    if (large) {
      // the section's plane: a and b as gen_sign_truss.py's LARGE_CORNERS names them
      EnumFacing aNeg;
      EnumFacing bNeg;
      if (axis == EnumFacing.Axis.Y) {
        aNeg = EnumFacing.WEST;
        bNeg = EnumFacing.NORTH;
      } else if (axis == EnumFacing.Axis.Z) {
        aNeg = EnumFacing.WEST;
        bNeg = EnumFacing.DOWN;
      } else {
        aNeg = EnumFacing.NORTH;
        bNeg = EnumFacing.DOWN;
      }
      state = state.withProperty(A_HIGH, isTruss(world, pos.offset(aNeg), axis))
          .withProperty(B_HIGH, isTruss(world, pos.offset(bNeg), axis));
    }
    return state;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
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

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }
}
