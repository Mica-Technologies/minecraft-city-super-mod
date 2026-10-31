package com.micatechnologies.minecraft.csm.venues.bowling;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A bowling lane cell or an approach cell, in maple or in synthetic: one block of the floor, a
 * metre square, laid in the floor's own layer. A lane is two blocks wide, its centre on the block
 * edge between its two columns, so each block is one half of it: the capping between lanes, the
 * gutter and half the lane bed ({@code right} says which half). Laid end to end towards the pins,
 * a lane works out its own markings, so it can be built to any length and most of it is one plain
 * block:
 *
 * <ul>
 *   <li>{@code bowling_lane}: its distance from the foul line is the number of lane blocks behind
 *   it (the foul line is the edge where the approach meets the lane). The first block shows the
 *   foul line, the third the dots at 7 ft, the fifth the targeting arrows (15 ft), the thirteenth
 *   the range finders (40 ft); the last two blocks of a run are the pin deck, the last with the
 *   tail plank and the pit;</li>
 *   <li>{@code bowling_approach}: counted back from the lane, the first block shows the locator
 *   dots just behind the foul line, the fourth the 12 ft dots and the fifth the 15 ft dots.</li>
 * </ul>
 *
 * <p>A full lane is 20 lane blocks (the foul line to the head pin is 60 ft, 18.29 m) after five
 * approach blocks. The markings ({@code mark}) are actual state, found by reading at most 24
 * blocks along the lane when its chunk section is built.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the bowler faces: towards
 * the pins; bit 2 the right half (looking down the lane); bit 3, lanes only, the bumpers. Placed by
 * hand it faces the way the player looks and takes its half from the block beside it. A
 * sneak-click with an empty hand on a lane raises or lowers the bumpers along the whole lane.</p>
 *
 * @since 2026.10
 */
public class BlockBowlingLane extends BlockVenue {

  /** Lane bed or approach. */
  public enum Kind {
    /** The lane: capping, gutter and bed. */
    LANE(7),
    /** The approach: capping and boards, no gutter. */
    APPROACH(4);

    final int marks;

    Kind(int marks) {
      this.marks = marks;
    }
  }

  /** A lane's markings ({@code mark} values). */
  public static final int NONE = 0;
  public static final int FOUL = 1;
  public static final int DOTS = 2;
  public static final int ARROWS = 3;
  public static final int RANGE = 4;
  public static final int DECK_FRONT = 5;
  public static final int DECK_BACK = 6;
  /** An approach's markings. */
  public static final int DOTS_FOUL = 1;
  public static final int DOTS_12 = 2;
  public static final int DOTS_15 = 3;

  /** Where along a lane each marking falls: lane blocks behind it. */
  static final int ROW_FOUL = 0;
  static final int ROW_DOTS = 2;
  static final int ROW_ARROWS = 4;
  static final int ROW_RANGE = 12;

  /** The right half of the lane, looking down it. Stored. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** The bumpers are up (lanes only). Stored. */
  public static final PropertyBool BUMPERS = PropertyBool.create("bumpers");

  /** The kind, for the base constructor, which builds the block state before fields are set. */
  private static final ThreadLocal<Kind> PENDING = new ThreadLocal<>();

  /** How far a lane block looks back for the foul line. */
  private static final int REACH = 24;

  private PropertyInteger mark;
  private final Kind kind;
  private final boolean synthetic;

  /**
   * Constructs a lane or approach block.
   *
   * @param registryName its registry name
   * @param kind         lane or approach
   * @param synthetic    the synthetic finish rather than maple
   */
  public BlockBowlingLane(String registryName, Kind kind, boolean synthetic) {
    super(stash(registryName, kind), Material.WOOD, SoundType.WOOD, 1.5F);
    this.kind = kind;
    this.synthetic = synthetic;
    PENDING.remove();
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(RIGHT, false).withProperty(mark, 0);
    if (kind == Kind.LANE) {
      s = s.withProperty(BUMPERS, false);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, Kind kind) {
    PENDING.set(kind);
    return registryName;
  }

  /** Lane or approach. */
  public Kind getKind() {
    return kind;
  }

  /** The synthetic finish rather than maple. */
  public boolean isSynthetic() {
    return synthetic;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    Kind k = PENDING.get();
    if (k == null) {
      k = Kind.LANE;
    }
    mark = PropertyInteger.create("mark", 0, k.marks - 1);
    if (k == Kind.LANE) {
      return new CsmBlockStateContainer(this, FACING, RIGHT, BUMPERS, mark);
    }
    return new CsmBlockStateContainer(this, FACING, RIGHT, mark);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(RIGHT, (meta & 4) != 0);
    if (kind == Kind.LANE) {
      s = s.withProperty(BUMPERS, (meta & 8) != 0);
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex() | (state.getValue(RIGHT) ? 4 : 0);
    if (kind == Kind.LANE && state.getValue(BUMPERS)) {
      meta |= 8;
    }
    return meta;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  // --- what a block shows --------------------------------------------------------------------

  /** Whether {@code pos} holds a block of this kind (either finish) facing {@code f}. */
  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing f) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockBowlingLane
        && ((BlockBowlingLane) s.getBlock()).kind == kind && s.getValue(FACING) == f;
  }

  /**
   * The marking a block shows, from its place along the lane.
   *
   * @param world the world
   * @param pos   the block
   * @param f     its facing, towards the pins
   *
   * @return its {@code mark}
   */
  int markAt(IBlockAccess world, BlockPos pos, EnumFacing f) {
    if (kind == Kind.LANE) {
      if (!same(world, pos.offset(f), f)) {
        return DECK_BACK;
      }
      if (!same(world, pos.offset(f, 2), f)) {
        return DECK_FRONT;
      }
      int behind = 0;
      BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
      EnumFacing back = f.getOpposite();
      while (behind < REACH) {
        at.move(back);
        if (!same(world, at, f)) {
          break;
        }
        behind++;
      }
      switch (behind) {
        case ROW_FOUL:
          return FOUL;
        case ROW_DOTS:
          return DOTS;
        case ROW_ARROWS:
          return ARROWS;
        case ROW_RANGE:
          return RANGE;
        default:
          return NONE;
      }
    }
    // an approach: counted back from the lane ahead of it
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
    for (int ahead = 0; ahead < 6; ahead++) {
      at.move(f);
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() instanceof BlockBowlingLane && s.getValue(FACING) == f) {
        if (((BlockBowlingLane) s.getBlock()).kind == Kind.LANE) {
          return ahead == 0 ? DOTS_FOUL : ahead == 3 ? DOTS_12 : ahead == 4 ? DOTS_15 : NONE;
        }
        continue;
      }
      return NONE;
    }
    return NONE;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(mark, markAt(world, pos, state.getValue(FACING)));
  }

  // --- placing and clicking ------------------------------------------------------------------

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX,
      float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing();
    IBlockState s = getDefaultState().withProperty(FACING, f);
    // the same column first: a lane laid end to end keeps its half
    for (BlockPos p : new BlockPos[]{pos.offset(f.getOpposite()), pos.offset(f)}) {
      IBlockState n = world.getBlockState(p);
      if (n.getBlock() instanceof BlockBowlingLane && n.getValue(FACING) == f) {
        return s.withProperty(RIGHT, n.getValue(RIGHT));
      }
    }
    // beside a lane's left half, this is its right half
    IBlockState left = world.getBlockState(pos.offset(f.rotateYCCW()));
    boolean right = left.getBlock() instanceof BlockBowlingLane && left.getValue(FACING) == f
        && !left.getValue(RIGHT);
    return s.withProperty(RIGHT, right);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (kind != Kind.LANE || !player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      boolean up = !state.getValue(BUMPERS);
      EnumFacing f = state.getValue(FACING);
      // the other half is across the lane's centre
      EnumFacing across = state.getValue(RIGHT) ? f.rotateYCCW() : f.rotateY();
      int n = 0;
      for (BlockPos column : new BlockPos[]{pos, pos.offset(across)}) {
        n += setBumpers(world, column, f, f, up) + setBumpers(world, column.offset(f.getOpposite()),
            f, f.getOpposite(), up);
      }
      player.sendStatusMessage(new TextComponentTranslation(up ? "csm.venues.bowling.bumpers_up"
          : "csm.venues.bowling.bumpers_down", n), true);
    }
    return true;
  }

  /** Sets the bumpers of the lane blocks from {@code from} on, stepping {@code step}. */
  private int setBumpers(World world, BlockPos from, EnumFacing f, EnumFacing step, boolean up) {
    int n = 0;
    BlockPos at = from;
    for (int i = 0; i < 64; i++) {
      IBlockState s = world.getBlockState(at);
      if (!(s.getBlock() instanceof BlockBowlingLane) || s.getValue(FACING) != f
          || ((BlockBowlingLane) s.getBlock()).kind != Kind.LANE) {
        break;
      }
      if (s.getValue(BUMPERS) != up) {
        world.setBlockState(at, s.withProperty(BUMPERS, up), 2);
        n++;
      }
      at = at.offset(step);
    }
    return n;
  }

  // --- shape and drawing ---------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  /**
   * The long sides and the bottom are whole, so a neighbour's faces against them are culled; a
   * lane's ends are not (the gutter is open there; an approach's are) and the top never is.
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos,
      EnumFacing face) {
    if (kind == Kind.APPROACH) {
      return face != EnumFacing.UP;
    }
    return face == EnumFacing.DOWN
        || (face != EnumFacing.UP && face.getAxis() != state.getValue(FACING).getAxis());
  }

  /** A lane's end face is hidden only against another lane block, whose profile matches it. */
  @Override
  @SuppressWarnings("deprecation")
  public boolean shouldSideBeRendered(IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, EnumFacing side) {
    EnumFacing f = state.getValue(FACING);
    if (kind == Kind.LANE && side.getAxis() == f.getAxis()) {
      IBlockState n = world.getBlockState(pos.offset(side));
      return !(n.getBlock() instanceof BlockBowlingLane
          && ((BlockBowlingLane) n.getBlock()).kind == kind && n.getValue(FACING).getAxis()
          == f.getAxis());
    }
    return super.shouldSideBeRendered(state, world, pos, side);
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
