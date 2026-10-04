package com.micatechnologies.minecraft.csm.trafficaccessories.truss;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The low-profile overhead sign truss: a light, shallow span of thin pipe chords laced with
 * verticals and V diagonals, carried on twin-post support frames, as most newer sign bridges are
 * built (gen_sign_truss.py). Beside the heavier box trusses, not instead of them.
 *
 * <p>One block, four kinds, stored ({@link Kind}): a span along x or z, or a support frame whose
 * two posts are spaced to carry a span along x or z. Placed against the side of a block it is a
 * span along that face's axis, like a log; placed on top of or under a block it is a frame, set
 * to carry a span across the way the player is looking (who stands on the road, looking along
 * it). Everything else is actual state: a span's end runs its chords into the posts of a frame
 * beside it ({@link #JOIN_NEG}, {@link #JOIN_POS}) or closes with an end frame ({@link #END_NEG},
 * {@link #END_POS}), and a frame caps its posts at the top and stands on base plates on the
 * ground ({@link #BASE}).</p>
 *
 * @since 2026.10
 */
public class BlockSignTrussLight extends AbstractBlock implements ISignTruss {

  /** What the block is: a span along an axis, or a frame carrying a span along an axis. */
  public enum Kind implements IStringSerializable {
    SPAN_X("span_x"), SPAN_Z("span_z"), FRAME_X("frame_x"), FRAME_Z("frame_z");

    private final String name;

    Kind(String name) {
      this.name = name;
    }

    @Override
    @Nonnull
    public String getName() {
      return name;
    }

    public boolean isSpan() {
      return this == SPAN_X || this == SPAN_Z;
    }

    /** The axis of the span this is, or carries. */
    public EnumFacing.Axis spanAxis() {
      return this == SPAN_X || this == FRAME_X ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
    }
  }

  public static final PropertyEnum<Kind> KIND = PropertyEnum.create("kind", Kind.class);
  /** Along the span or up the frame, nothing of the same carries on towards the negative end. */
  public static final PropertyBool END_NEG = PropertyBool.create("end_neg");
  public static final PropertyBool END_POS = PropertyBool.create("end_pos");
  /** A span's negative end meets a frame that carries it: chords run into its posts. */
  public static final PropertyBool JOIN_NEG = PropertyBool.create("join_neg");
  public static final PropertyBool JOIN_POS = PropertyBool.create("join_pos");
  /** A frame standing on something that is not truss or air: base plates. */
  public static final PropertyBool BASE = PropertyBool.create("base");

  /** The span's top chord's top, in its cell (gen_sign_truss.py's LT_TOP). */
  private static final float SPAN_TOP = 14.0f;
  /** How far in from its cell's face the span's front chords are. */
  private static final float SPAN_FRONT = 3.0f;

  // a frame carrying a span along x: its posts spaced along z
  private static final AxisAlignedBB FRAME_BOX_X =
      new AxisAlignedBB(6 / 16.0, 0, 2 / 16.0, 10 / 16.0, 1, 14 / 16.0);
  private static final AxisAlignedBB FRAME_BOX_Z =
      new AxisAlignedBB(2 / 16.0, 0, 6 / 16.0, 14 / 16.0, 1, 10 / 16.0);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockSignTrussLight(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 12.0F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(KIND, Kind.SPAN_X)
        .withProperty(END_NEG, false).withProperty(END_POS, false)
        .withProperty(JOIN_NEG, false).withProperty(JOIN_POS, false)
        .withProperty(BASE, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, KIND, END_NEG, END_POS, JOIN_NEG, JOIN_POS, BASE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(KIND, Kind.values()[meta & 3]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(KIND).ordinal();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    Kind kind;
    if (facing.getAxis() == EnumFacing.Axis.Y) {
      // the span runs across the road the player is looking along
      kind = placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.Z
          ? Kind.FRAME_X : Kind.FRAME_Z;
    } else {
      kind = facing.getAxis() == EnumFacing.Axis.X ? Kind.SPAN_X : Kind.SPAN_Z;
    }
    return getDefaultState().withProperty(KIND, kind);
  }

  private Kind kindAt(IBlockAccess world, BlockPos pos) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this ? s.getValue(KIND) : null;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    Kind kind = state.getValue(KIND);
    if (kind.isSpan()) {
      EnumFacing neg = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE,
          kind.spanAxis());
      Kind n = kindAt(world, pos.offset(neg));
      Kind p = kindAt(world, pos.offset(neg.getOpposite()));
      boolean joinNeg = n != null && !n.isSpan() && n.spanAxis() == kind.spanAxis();
      boolean joinPos = p != null && !p.isSpan() && p.spanAxis() == kind.spanAxis();
      return state.withProperty(JOIN_NEG, joinNeg).withProperty(JOIN_POS, joinPos)
          .withProperty(END_NEG, n != kind && !joinNeg)
          .withProperty(END_POS, p != kind && !joinPos)
          .withProperty(BASE, false);
    }
    boolean endNeg = kindAt(world, pos.down()) != kind;
    boolean base = endNeg && !(world.getBlockState(pos.down()).getBlock() instanceof ISignTruss)
        && !world.isAirBlock(pos.down());
    return state.withProperty(END_NEG, endNeg)
        .withProperty(END_POS, kindAt(world, pos.up()) != kind)
        .withProperty(BASE, base).withProperty(JOIN_NEG, false).withProperty(JOIN_POS, false);
  }

  @Override
  public float getSignTrussTop(IBlockState state) {
    return state.getValue(KIND).isSpan() ? SPAN_TOP : -1;
  }

  @Override
  public float getSignTrussFrontInset(IBlockState state) {
    return SPAN_FRONT;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    switch (state.getValue(KIND)) {
      case FRAME_X:
        return FRAME_BOX_X;
      case FRAME_Z:
        return FRAME_BOX_Z;
      default:
        return SQUARE_BOUNDING_BOX;
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        getBlockBoundingBox(state, world, pos));
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
