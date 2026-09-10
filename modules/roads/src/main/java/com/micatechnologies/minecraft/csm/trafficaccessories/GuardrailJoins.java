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
    String kind = ((ICsmGuardrailRail) self).getRailKind();
    DirectionEight facing = state.getValue(AbstractBlockRotatableHZEight.FACING);

    int right = neighbourLevel(kind, access, pos, facing, facing.rotateY());
    boolean joinsRight = right != NONE;
    boolean joinsLeft = neighbourLevel(kind, access, pos, facing, facing.rotateYCCW()) != NONE;

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
    String kind = ((ICsmGuardrailRail) self).getRailKind();
    DirectionEight facing = state.getValue(AbstractBlockRotatableHZEight.FACING);

    boolean runLeft = neighbourLevel(kind, access, pos, facing, facing.rotateYCCW()) != NONE;
    boolean runRight = neighbourLevel(kind, access, pos, facing, facing.rotateY()) != NONE;

    return state.withProperty(BlockGuardrailEnd.MIRRORED, runRight && !runLeft);
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
  private static int neighbourLevel(String kind, IBlockAccess access, BlockPos pos,
      DirectionEight facing, DirectionEight direction) {
    BlockPos step = pos.add(direction.getOffsetX(), 0, direction.getOffsetZ());
    for (int dy : new int[]{0, 1, -1}) {
      if (matches(kind, access, step.up(dy), facing)) {
        return dy;
      }
    }
    return NONE;
  }

  /** True if the block at {@code pos} is a guardrail carrying the same rail and facing the same way. */
  private static boolean matches(String kind, IBlockAccess access, BlockPos pos,
      DirectionEight facing) {
    IBlockState neighbour = access.getBlockState(pos);
    ICsmGuardrailRail rail = railOf(neighbour.getBlock());
    if (rail == null || !kind.equals(rail.getRailKind())) {
      return false;
    }
    if (!neighbour.getPropertyKeys().contains(AbstractBlockRotatableHZEight.FACING)) {
      return false;
    }
    return neighbour.getValue(AbstractBlockRotatableHZEight.FACING) == facing;
  }

  @Nullable
  private static ICsmGuardrailRail railOf(Block block) {
    return block instanceof ICsmGuardrailRail ? (ICsmGuardrailRail) block : null;
  }
}
