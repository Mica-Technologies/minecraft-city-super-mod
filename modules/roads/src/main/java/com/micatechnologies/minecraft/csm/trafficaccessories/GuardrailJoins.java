package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What it means for one guardrail to join the next, and how a run follows a hill.
 *
 * <p>Separate from {@link WorkZoneJoins} rather than an option on it, because the two answer the
 * question differently and both answers are right. A barricade joins only the identical block; a
 * guardrail joins on the {@link ICsmGuardrailRail#getRailKind() rail}, so a run may change post
 * material or pick up a second rail part way along and still read as one run. It reuses that
 * class's PROPERTIES, which are the same three, and none of its logic.</p>
 *
 * <p>The other difference is Y. Every other run in this tab is flat, so its neighbour is the block
 * beside it and nowhere else. A guardrail run climbs, so a neighbour one block up or down counts
 * too — and when there is one, the rail ramps across the cell to meet it rather than stepping.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
final class GuardrailJoins {

  /** How the rail runs across the cell; see {@link GuardrailSlope}. */
  static final PropertyEnum<GuardrailSlope> SLOPE =
      PropertyEnum.create("slope", GuardrailSlope.class);

  private GuardrailJoins() {
    throw new AssertionError("GuardrailJoins is a utility class and must not be instantiated");
  }

  /**
   * Sets the connections, the diagonal filler and the slope from what stands either side.
   *
   * @param self   the block asking
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with all four resolved
   *
   * @since 1.0
   */
  static IBlockState resolve(Block self, IBlockState state, IBlockAccess access, BlockPos pos) {
    if (!(self instanceof ICsmGuardrailRail)) {
      return state;
    }
    ICsmGuardrailRail rail = (ICsmGuardrailRail) self;
    DirectionEight facing = state.getValue(AbstractBlockRotatableHZEight.FACING);

    DirectionEight toRight = facing.rotateY();
    DirectionEight toLeft = facing.rotateYCCW();
    int right = neighbourLevel(rail, rail.getRailKindOnRight(), access, pos, facing,
        toRight, true);
    int left = neighbourLevel(rail, rail.getRailKindOnLeft(), access, pos, facing,
        toLeft, false);
    boolean joinsRight = right != NONE;
    boolean joinsLeft = left != NONE;

    // A ramp is drawn in the LOWER cell of the two it joins, whichever side the higher one is on:
    // UP when it is to the right, DOWN when it is to the left. The higher cell stands on something
    // solid, and a rail ramping down out of it would finish inside that block — drawn and then
    // hidden, so the ramp seemed to rise out of the top of the wall while the lower run butted
    // into its side. The lower cell has only air above its rail, so a ramp there is seen whole.
    //
    // Heights are read as real HEIGHTS, not as differences of block positions. A guardrail settles
    // onto whatever it stands on, so a cell on bare ground and its neighbour a block up on a snow
    // layer are one block apart in Y and a couple of sixteenths apart in the world. Asking the
    // block positions answers that with a whole block's ramp, which is the rail diving into the
    // ground that this replaced.
    double here = settledHeight(access, pos);
    GuardrailSlope slope = GuardrailSlope.forRises(
        riseTo(access, pos, toRight, right, here), riseTo(access, pos, toLeft, left, here));

    return state
        .withProperty(WorkZoneJoins.CONNECT_LEFT, joinsLeft)
        .withProperty(WorkZoneJoins.CONNECT_RIGHT, joinsRight)
        .withProperty(WorkZoneJoins.DIAG_FILL, joinsRight && facing.isDiagonal())
        .withProperty(SLOPE, slope);
  }

  /**
   * Turns an end treatment the right way round for the end of the run it is standing at.
   *
   * <p>An end is chiral, and which hand it needs is not something the player should have to know:
   * a shoe at the right-hand end of a run is the mirror of the one at the left. So it looks for the
   * rail and mirrors itself when the run lies to its RIGHT — the far end from the one the unmirrored
   * shape was drawn for.</p>
   *
   * <p>With a run on both sides, or on neither, it stays unmirrored: a lone end block has no run to
   * take its hand from, and one in the middle of a run is a mistake the player can see.</p>
   *
   * @param self   the end block asking
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with its mirror resolved
   *
   * @since 1.0
   */
  static IBlockState resolveEnd(Block self, IBlockState state, IBlockAccess access, BlockPos pos) {
    if (!(self instanceof ICsmGuardrailRail)) {
      return state;
    }
    ICsmGuardrailRail rail = (ICsmGuardrailRail) self;
    DirectionEight facing = state.getValue(AbstractBlockRotatableHZEight.FACING);

    boolean runLeft = neighbourLevel(rail, rail.getRailKindOnLeft(), access, pos, facing,
        facing.rotateYCCW(), false) != NONE;
    boolean runRight = neighbourLevel(rail, rail.getRailKindOnRight(), access, pos, facing,
        facing.rotateY(), true) != NONE;

    return state.withProperty(BlockGuardrailEnd.MIRRORED, runRight && !runLeft);
  }

  /**
   * Whether the run on this block's LEFT is carrying {@code kind}.
   *
   * <p>How a transition piece works out which way round it is drawn: it was built to END with one
   * of its two rails, so finding that rail on its left means the run reads through it backwards
   * and the model wants mirroring.</p>
   *
   * @param kind   the rail to look for
   * @param state  this block's resolved state, for its facing
   * @param access the block access
   * @param pos    this block's position
   *
   * @return true if a run carrying {@code kind} adjoins the left-hand end
   *
   * @since 1.0
   */
  static boolean presentsRailOnLeft(String kind, IBlockState state, IBlockAccess access,
      BlockPos pos) {
    // STRICT, unlike the join test: it asks what the neighbour actually presents, not what the
    // two of them would tolerate. A transition accepts either of its rails, so going through the
    // mutual test here would answer yes whichever way round the run reads, and the mirror would
    // never flip.
    DirectionEight facing = state.getValue(AbstractBlockRotatableHZEight.FACING);
    DirectionEight direction = facing.rotateYCCW();
    BlockPos step = pos.add(direction.getOffsetX(), 0, direction.getOffsetZ());
    for (int dy : new int[]{0, 1, -1}) {
      IBlockState neighbour = access.getBlockState(step.up(dy));
      ICsmGuardrailRail rail = railOf(neighbour.getBlock());
      if (rail == null
          || !neighbour.getPropertyKeys().contains(AbstractBlockRotatableHZEight.FACING)
          || neighbour.getValue(AbstractBlockRotatableHZEight.FACING) != facing) {
        continue;
      }
      // Its RIGHT-hand end is the one facing us.
      if (kind.equals(rail.getRailKindOnRight())) {
        return true;
      }
    }
    return false;
  }

  /**
   * How tall a guardrail stops the player, in blocks, whatever height its rail is drawn at.
   *
   * <p>A standing jump clears a block and a quarter, and no rail in this system is drawn that
   * tall, so a box that stopped where the steel does would be a barrier you walk over. A vanilla
   * fence solves it the same way and at the same height.</p>
   *
   * @since 1.0
   */
  private static final double COLLISION_HEIGHT = 1.5;

  /**
   * Raises a drawn bounding box to the height a guardrail actually stops the player at.
   *
   * <p>Measured from the box's own floor, not the cell's. A run settled onto a snow layer or a
   * sloped road has a box that starts below its cell, and measuring from the cell would give that
   * run a lower barrier than the one beside it on bare ground.</p>
   *
   * @param drawn the box the block is drawn and selected at
   *
   * @return the box the player collides with
   *
   * @since 1.0
   */
  static AxisAlignedBB standTall(AxisAlignedBB drawn) {
    double top = drawn.minY + COLLISION_HEIGHT;
    if (top <= drawn.maxY) {
      return drawn;
    }
    return new AxisAlignedBB(drawn.minX, drawn.minY, drawn.minZ, drawn.maxX, top, drawn.maxZ);
  }

  /**
   * Where a guardrail at {@code pos} actually draws its rail, in blocks.
   *
   * <p>Its cell's Y plus however far it has settled onto the surface below — which is what makes
   * this different from the block position, and is the number every height comparison here wants.
   * A run laid across snow layers of different depths is at one Y and half a dozen heights.</p>
   */
  private static double settledHeight(IBlockAccess access, BlockPos pos) {
    return pos.getY() + RoadSurfaceHeight.offsetFor(access, pos);
  }

  /**
   * How far above {@code here} the rail joined one step in {@code direction} sits, or negative
   * infinity when nothing joins on that side.
   */
  private static double riseTo(IBlockAccess access, BlockPos pos, DirectionEight direction,
      int level, double here) {
    if (level == NONE) {
      return Double.NEGATIVE_INFINITY;
    }
    BlockPos neighbour = pos.add(direction.getOffsetX(), level, direction.getOffsetZ());
    return settledHeight(access, neighbour) - here;
  }

  /** Returned by {@link #neighbourLevel} when there is no guardrail that way at all. */
  private static final int NONE = Integer.MIN_VALUE;

  /**
   * How far up or down the guardrail one step in {@code direction} sits, or {@link #NONE}.
   *
   * <p>Level is checked first so a flat run never mistakes a rail on the terrace above it for its
   * own continuation. An end treatment counts as a neighbour — a run joins INTO one — but this is
   * only ever asked of a rail, so nothing joins through it.</p>
   */
  private static int neighbourLevel(ICsmGuardrailRail self, String kind, IBlockAccess access,
      BlockPos pos, DirectionEight facing, DirectionEight direction, boolean steppingRight) {
    BlockPos step = pos.add(direction.getOffsetX(), 0, direction.getOffsetZ());
    for (int dy : new int[]{0, 1, -1}) {
      if (matches(self, kind, access, step.up(dy), facing, steppingRight)) {
        return dy;
      }
    }
    return NONE;
  }

  /**
   * True if the block at {@code pos} is a guardrail facing the same way and presenting the same
   * rail at the end that faces us.
   *
   * <p>Which of the neighbour's ends that is follows from which way we stepped: reaching to our
   * RIGHT puts us against its left-hand end, and the other way round. Two blocks with the same
   * facing lie head to tail, not head to head.</p>
   */
  private static boolean matches(ICsmGuardrailRail self, String kind, IBlockAccess access,
      BlockPos pos, DirectionEight facing, boolean steppingRight) {
    IBlockState neighbour = access.getBlockState(pos);
    ICsmGuardrailRail rail = railOf(neighbour.getBlock());
    if (rail == null
        || !neighbour.getPropertyKeys().contains(AbstractBlockRotatableHZEight.FACING)
        || neighbour.getValue(AbstractBlockRotatableHZEight.FACING) != facing) {
      return false;
    }
    // Stepping to our RIGHT puts us against the neighbour's left-hand end, and puts our right-hand
    // end against it. Two blocks with the same facing lie head to tail.
    boolean theirEndIsLeft = steppingRight;
    String theirKind = theirEndIsLeft ? rail.getRailKindOnLeft() : rail.getRailKindOnRight();

    // Either accepting the other is enough. For a plain rail both clauses ask the same question,
    // because it presents and accepts one section. A transition presents a fixed rail at each of
    // its ends and those do NOT swap when it is drawn mirrored -- so when it is the one reaching
    // out, the first clause fails and the second is what joins the run up.
    return rail.acceptsRail(kind, theirEndIsLeft) || self.acceptsRail(theirKind, !steppingRight);
  }

  @Nullable
  private static ICsmGuardrailRail railOf(Block block) {
    return block instanceof ICsmGuardrailRail ? (ICsmGuardrailRail) block : null;
  }
}
