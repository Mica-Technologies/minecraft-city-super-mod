package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
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

    int right = neighbourLevel(rail, rail.getRailKindOnRight(), access, pos, facing,
        facing.rotateY(), true);
    boolean joinsRight = right != NONE;
    boolean joinsLeft = neighbourLevel(rail, rail.getRailKindOnLeft(), access, pos, facing,
        facing.rotateYCCW(), false) != NONE;

    // The slope is read off the RIGHT-hand neighbour alone, and that is enough for a whole run:
    // this cell ramps up to meet the one above it, and that cell in turn ramps up to the next, so
    // each rail's right-hand end lands exactly on its neighbour's left-hand end all the way up.
    GuardrailSlope slope = GuardrailSlope.FLAT;
    if (joinsRight && right > 0) {
      slope = GuardrailSlope.UP;
    } else if (joinsRight && right < 0) {
      slope = GuardrailSlope.DOWN;
    }

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
