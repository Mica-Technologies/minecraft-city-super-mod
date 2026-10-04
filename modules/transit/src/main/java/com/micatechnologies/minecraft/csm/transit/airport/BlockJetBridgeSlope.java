package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A sloped length of jet bridge tunnel: the level tunnel's corridor running down (or up) towards
 * the aircraft, as a real bridge runs from the terminal's door to the lower (or higher) sill of
 * the aircraft.
 *
 * <p>A run of pieces drops one whole block: over eight pieces ({@link Grade#DOWN_8}, about 7
 * degrees, near a real bridge) or four ({@link Grade#DOWN_4}, about 14, for a short bridge), so
 * the piece after the run meets a level tunnel, the cab or the next run a block lower. Each piece
 * works out where it is along its run ({@link #STEP}) by counting the pieces of the same grade
 * and facing above it on its own level, so a run is only placed in a line: eight (or four) on one
 * level, then carry on a block lower. The facing is the way to the aircraft, as for the other
 * pieces; an up grade climbs that way. Sneak and use with an empty hand to change the grade.</p>
 *
 * <p>The model is the level tunnel sheared along its length (OBJ, gen_transit_airside.py's
 * {@code jet_bridge_slope}, one a grade and step, its low end at the model's north). The floor
 * collides as steps of a sixteenth, which a player walks up without jumping; see
 * {@link #boxes} for why it leads the drawn slope and the roof collides above it.</p>
 *
 * @since 2026.10
 */
public class BlockJetBridgeSlope extends BlockPlatformFixture {

  /** How a piece slopes, towards the aircraft. */
  public enum Grade implements IStringSerializable {
    DOWN_8(8, false), DOWN_4(4, false), UP_8(8, true), UP_4(4, true);

    /** Pieces in a run, which drops (or climbs) one block. */
    final int pieces;
    /** Whether it climbs towards the aircraft. */
    final boolean up;

    Grade(int pieces, boolean up) {
      this.pieces = pieces;
      this.up = up;
    }

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT).replace("_", "");
    }
  }

  public static final PropertyEnum<Grade> GRADE = PropertyEnum.create("grade", Grade.class);
  /** Where the piece is along its run, from its high end: 0 to pieces - 1. */
  public static final PropertyInteger STEP = PropertyInteger.create("step", 0, 7);
  public static final PropertyBool AHEAD = BlockJetBridge.AHEAD;
  public static final PropertyBool BEHIND = BlockJetBridge.BEHIND;

  /** The light inside the bridge, as the other pieces. */
  private static final int LIGHT = 9;

  // the level tunnel's section, in sixteenths (gen_transit_airside.py's JB_* numbers)
  private static final double[] SECTION = {-8, 0, 0, 24, 31.6, 16};
  private static final double WALL_SMALL = 1.5;
  private static final double FLOOR = BlockJetBridge.FLOOR;
  private static final double CEIL_SMALL = 30.5;

  private final boolean large;
  // this piece's section: the level tunnel's, or the large bridge's (BlockJetBridge#toLarge)
  private final double x0;
  private final double x1;
  private final double wall;
  private final double ceil;
  private final double roof;

  public BlockJetBridgeSlope(String registryName) {
    this(registryName, false);
  }

  /**
   * Constructs a sloped tunnel, level-bridge or large.
   *
   * @param registryName its registry name
   * @param large        whether it is the large bridge's, three blocks wide and four tall
   */
  public BlockJetBridgeSlope(String registryName, boolean large) {
    super(registryName, floorBox(large), false, LIGHT);
    this.large = large;
    double[] section = large ? BlockJetBridge.toLarge(SECTION, true) : SECTION;
    double[] inner = large ? BlockJetBridge.toLarge(new double[]{-8 + WALL_SMALL, 0, 0,
        24 - WALL_SMALL, CEIL_SMALL, 16}, true) : new double[]{-8 + WALL_SMALL, 0, 0,
        24 - WALL_SMALL, CEIL_SMALL, 16};
    this.x0 = section[0];
    this.x1 = section[3];
    this.roof = section[4];
    this.wall = inner[0] - section[0];
    this.ceil = inner[4];
    setDefaultState(getDefaultState().withProperty(GRADE, Grade.DOWN_8).withProperty(STEP, 0)
        .withProperty(AHEAD, false).withProperty(BEHIND, false));
  }

  private static double[] floorBox(boolean large) {
    double[] floor = {-8, 0, 0, 24, FLOOR, 16};
    return large ? BlockJetBridge.toLarge(floor, true) : floor;
  }

  /** Whether this is the large bridge's sloped tunnel. */
  public boolean isLarge() {
    return large;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, GRADE, STEP, AHEAD, BEHIND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(GRADE, Grade.values()[(meta >> 2) & 3]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(GRADE).ordinal() << 2;
  }

  /** Downhill, which is where the model's north (its low end) is turned to. */
  private static EnumFacing downhill(IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    return state.getValue(GRADE).up ? facing.getOpposite() : facing;
  }

  /** Where the piece is along its run: the pieces of its run above it on its own level. */
  private int step(IBlockAccess world, BlockPos pos, IBlockState state) {
    Grade grade = state.getValue(GRADE);
    EnumFacing uphill = downhill(state).getOpposite();
    int count = 0;
    BlockPos p = pos.offset(uphill);
    while (count < grade.pieces) {
      IBlockState s = world.getBlockState(p);
      if (s.getBlock() != this || s.getValue(GRADE) != grade
          || s.getValue(FACING) != state.getValue(FACING)) {
        break;
      }
      count++;
      p = p.offset(uphill);
    }
    return count % grade.pieces;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(STEP, step(world, pos, state))
        .withProperty(AHEAD, continues(world, pos, facing))
        .withProperty(BEHIND, continues(world, pos, facing.getOpposite()));
  }

  /**
   * Whether the bridge carries on from this piece towards {@code dir}: any piece of jet bridge on
   * the same axis right beyond its end, on its level or a block above or below (a run's low end
   * meets the level below, its high end the level above).
   */
  private boolean continues(IBlockAccess world, BlockPos pos, EnumFacing dir) {
    for (int dy = -1; dy <= 1; dy++) {
      if (BlockJetBridge.isBridgeOnAxis(world, pos.offset(dir).up(dy), dir.getAxis(), large)
          || BlockJetBridge.isRotundaOnAxis(world, pos.offset(dir, 2).up(dy), dir.getAxis(),
          large)) {
        return true;
      }
    }
    return false;
  }

  /** A player's length (and width) in sixteenths: what stands on several stretches at once. */
  private static final double PLAYER = 9.6;

  /**
   * The boxes of a piece at a step, facing north (low end north), in sixteenths.
   *
   * <p>The floor drops a sixteenth a stretch, but a player is longer than a stretch and stands on
   * the highest floor under them. So the floor collides a player's length ahead of where it is
   * drawn ({@code lead} stretches lower, never below the level beneath): a player's back is down
   * on the level below by the time their front reaches it, under the cab's roof. And the roof
   * collides {@code rise} stretches above where it is drawn, so it clears a player whose back is
   * still on a higher stretch (or on the level tunnel above the run). The camera never sees either
   * difference; feet sink a sixteenth or two into the carpet.</p>
   */
  private double[][] boxes(Grade grade, int step) {
    int drop = 16 / grade.pieces;             // sixteenths a piece drops
    double length = 16.0 / drop;              // each sixteenth of drop takes this much floor
    int lead = (int) Math.floor(PLAYER / length);
    int rise = (int) Math.ceil(PLAYER / length) + 1;
    double[][] out = new double[drop * 2 + 2][];
    for (int k = 0; k < drop; k++) {
      double z1 = 16 - k * length;
      double z0 = z1 - length;
      int g = step * drop + k;                // the stretch along the whole run, 0 to 15
      double floor = Math.min(16, g + 1 + lead);
      out[k * 2] = new double[]{x0, -floor, z0, x1, FLOOR - floor, z1};
      out[k * 2 + 1] = new double[]{x0, ceil - g + rise, z0, x1, roof - g + rise, z1};
    }
    double low = (step + 1) * drop;
    double high = step * drop;
    out[drop * 2] = new double[]{x0, FLOOR - low, 0, x0 + wall, ceil - high, 16};
    out[drop * 2 + 1] = new double[]{x1 - wall, FLOOR - low, 0, x1, ceil - high, 16};
    return out;
  }

  private static AxisAlignedBB box(double[] b) {
    return new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0,
        b[5] / 16.0);
  }

  /** What the player aims at: the floor, so the corridor's inside can still be clicked. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (!(state.getBlock() instanceof BlockJetBridgeSlope)) {
      return super.getBlockBoundingBox(state, source, pos);
    }
    Grade grade = state.getValue(GRADE);
    int step = step(source, pos, state);
    int drop = 16 / grade.pieces;
    return box(new double[]{x0, -(step + 1) * drop, 0, x1, FLOOR - step * drop, 16});
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing turn = downhill(state);
    for (double[] b : boxes(state.getValue(GRADE), step(world, pos, state))) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(box(b), turn));
    }
  }

  /** Sneak and use with an empty hand: the next grade. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
    if (!world.isRemote) {
      Grade next = Grade.values()[(state.getValue(GRADE).ordinal() + 1) % Grade.values().length];
      world.setBlockState(pos, state.withProperty(GRADE, next), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.jet_bridge_slope",
          new TextComponentTranslation("csm.transit.jet_bridge_slope." + next.getName())), true);
    }
    return true;
  }
}
