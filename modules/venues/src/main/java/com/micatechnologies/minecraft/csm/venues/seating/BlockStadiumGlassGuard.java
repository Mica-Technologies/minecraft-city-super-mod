package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A frameless glass guard on an aluminium base shoe with a stainless cap rail, for the front of
 * a tier, a suite or a cross aisle, and the sides of a vomitory. Cells side by side make one
 * guard: the glass, shoe and cap run edge to edge of every cell with no post, so a run needs no
 * end piece and no joining state.
 *
 * <p>Three blocks, by the slope they run on (see {@link StadiumSlope}):</p>
 * <ul>
 *   <li>{@code stadium_glass_guard}, level: the glass along the front edge of its cell (the
 *   side it faces). <b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), towards the drop.</li>
 *   <li>{@code stadium_glass_guard_slope}, on a 1 in 2 aisle: the glass along the left or right
 *   edge of its cell, running up the aisle. <b>Metadata</b>: bits 0-1 the facing, the way down
 *   the aisle; bit 2 the right edge (as seen looking down the aisle); bit 3 the high piece (over
 *   the stair, a block above the tread before it).</li>
 *   <li>{@code stadium_glass_guard_steep}, on a 1 in 1 aisle: as the slope piece with no high
 *   bit.</li>
 * </ul>
 * <p>A sloped cell's glass stands on the steps under it and its top follows the nosing line,
 * so it is a shape no element can draw: the models are OBJ. The glass draws in the translucent
 * layer.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumGlassGuard extends BlockVenue {

  /** Which slope a guard block runs on. */
  public enum Kind {
    /** Level, along the front edge. */
    LEVEL,
    /** 1 in 2, low and high pieces, left or right edge. */
    SLOPE,
    /** 1 in 1, left or right edge. */
    STEEP
  }

  /** The glass on the right edge (looking down the aisle). Stored, bit 2; sloped only. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** The high piece of a 1 in 2 slope. Stored, bit 3; 1 in 2 only. */
  public static final PropertyBool HIGH = PropertyBool.create("high");

  private static final ThreadLocal<Kind> PENDING = new ThreadLocal<>();

  private static final AxisAlignedBB LEVEL_SELECT = BlockStadiumSeat.box(0, 0, 0, 16, 16.5, 3);
  private static final AxisAlignedBB LEVEL_BOX = new AxisAlignedBB(0, 0, 0, 1, 1.5, 3 / 16.0);
  private static final AxisAlignedBB LEFT_SELECT = BlockStadiumSeat.box(0, 0, 0, 3, 16, 16);
  private static final AxisAlignedBB RIGHT_SELECT = BlockStadiumSeat.box(13, 0, 0, 16, 16, 16);
  private static final AxisAlignedBB LEFT_BOX = new AxisAlignedBB(0, 0, 0, 3 / 16.0, 1.5, 1);
  private static final AxisAlignedBB RIGHT_BOX = new AxisAlignedBB(13 / 16.0, 0, 0, 1, 1.5, 1);

  private final Kind kind;

  /**
   * Constructs a guard block.
   *
   * @param registryName its registry name
   * @param kind         the slope it runs on
   */
  public BlockStadiumGlassGuard(String registryName, Kind kind) {
    super(registryName, stashKind(kind), SoundType.GLASS, 1.0F);
    this.kind = kind;
    PENDING.remove();
    IBlockState base = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    if (kind() != Kind.LEVEL) {
      base = base.withProperty(RIGHT, false);
    }
    if (kind() == Kind.SLOPE) {
      base = base.withProperty(HIGH, false);
    }
    setDefaultState(base);
  }

  private static Material stashKind(Kind kind) {
    PENDING.set(kind);
    return Material.GLASS;
  }

  private Kind kind() {
    return kind != null ? kind : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    List<IProperty<?>> props = new ArrayList<>();
    props.add(FACING);
    Kind k = kind();
    if (k != Kind.LEVEL) {
      props.add(RIGHT);
    }
    if (k == Kind.SLOPE) {
      props.add(HIGH);
    }
    return new CsmBlockStateContainer(this, props.toArray(new IProperty<?>[0]));
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    if (kind() != Kind.LEVEL) {
      s = s.withProperty(RIGHT, (meta & 4) != 0);
    }
    if (kind() == Kind.SLOPE) {
      s = s.withProperty(HIGH, (meta & 8) != 0);
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex();
    if (state.getPropertyKeys().contains(RIGHT) && state.getValue(RIGHT)) {
      meta |= 4;
    }
    if (state.getPropertyKeys().contains(HIGH) && state.getValue(HIGH)) {
      meta |= 8;
    }
    return meta;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (kind == Kind.LEVEL) {
      return LEVEL_SELECT;
    }
    return state.getValue(RIGHT) ? RIGHT_SELECT : LEFT_SELECT;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    AxisAlignedBB box = kind == Kind.LEVEL ? LEVEL_BOX
        : state.getValue(RIGHT) ? RIGHT_BOX : LEFT_BOX;
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(box, state.getValue(FACING)));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (kind == Kind.LEVEL || !player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      // Steps through the edge, then (on 1 in 2) the high piece: left low, right low, left
      // high, right high.
      IBlockState next = state.withProperty(RIGHT, !state.getValue(RIGHT));
      if (kind == Kind.SLOPE && state.getValue(RIGHT)) {
        next = next.withProperty(HIGH, !state.getValue(HIGH));
      }
      world.setBlockState(pos, next, 3);
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}
