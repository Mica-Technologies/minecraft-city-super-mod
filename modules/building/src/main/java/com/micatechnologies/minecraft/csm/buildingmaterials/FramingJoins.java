package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What it means for one framed wall to join the next.
 *
 * <p>The four connection properties are absolute compass directions rather than the
 * facing-relative left/right the work zone runs use. A guardrail or a barricade is a line: it has
 * a front, and everything about it is described from there. A wall is a junction — it may carry on
 * in one direction, turn a corner, branch into a T or cross — and naming those four sides after
 * whichever way the block happened to be placed would mean the same physical corner is described
 * differently depending on which of its two walls is asked.</p>
 *
 * <p>Absolute directions also mean the blockstate can rotate one arm model into four places, which
 * is how a wall draws every junction shape from two models instead of sixteen.</p>
 *
 * @version 1.0
 * @see ICsmFramingMember
 * @since 2026.9
 */
public final class FramingJoins {

  /** A framing member continues to the north of this block. */
  public static final PropertyBool NORTH = PropertyBool.create("north");

  /** A framing member continues to the east of this block. */
  public static final PropertyBool EAST = PropertyBool.create("east");

  /** A framing member continues to the south of this block. */
  public static final PropertyBool SOUTH = PropertyBool.create("south");

  /** A framing member continues to the west of this block. */
  public static final PropertyBool WEST = PropertyBool.create("west");

  /**
   * A framing member continues above this block.
   *
   * <p>Vertical neighbours carry no arm — a wall does not branch upward. These two exist so that
   * the TRACK can be left out where one course meets the next. A real stud runs the full height of
   * the wall in one piece and is fixed into track only at the floor and at the ceiling; drawing
   * track at every course turns a three-block wall into three stacked walls.</p>
   *
   * @since 1.0
   */
  public static final PropertyBool UP = PropertyBool.create("up");

  /**
   * A framing member continues below this block.
   *
   * @see #UP
   * @since 1.0
   */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  private FramingJoins() {
    throw new AssertionError("FramingJoins is a utility class and must not be instantiated");
  }

  /**
   * Sets all four connections from what stands around the block.
   *
   * @param self   the block asking
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with all four connections resolved
   *
   * @since 1.0
   */
  public static IBlockState resolve(Block self, IBlockState state, IBlockAccess access,
      BlockPos pos) {
    if (!(self instanceof ICsmFramingMember)) {
      return state;
    }
    ICsmFramingMember member = (ICsmFramingMember) self;
    return state
        .withProperty(NORTH, joins(member, access, pos, EnumFacing.NORTH))
        .withProperty(EAST, joins(member, access, pos, EnumFacing.EAST))
        .withProperty(SOUTH, joins(member, access, pos, EnumFacing.SOUTH))
        .withProperty(WEST, joins(member, access, pos, EnumFacing.WEST))
        .withProperty(UP, joins(member, access, pos, EnumFacing.UP))
        .withProperty(DOWN, joins(member, access, pos, EnumFacing.DOWN));
  }

  /**
   * Whether the block one step in {@code side} is framing this wall joins.
   *
   * <p>Only another framing member counts. A stud wall run into a solid block does NOT connect to
   * it: the wall ends there and shows its end stud, which is what a real wall does where it dies
   * into masonry. Connecting would leave an arm reaching into the neighbour's face, drawn and then
   * hidden.</p>
   *
   * <p>The same test answers for up and down, where it means the course continues rather than that
   * the wall branches.</p>
   *
   * @param member the framing member asking
   * @param access the block access
   * @param pos    the position of the block asking
   * @param side   the side to look at
   *
   * @return true if the two join
   *
   * @since 1.0
   */
  private static boolean joins(ICsmFramingMember member, IBlockAccess access, BlockPos pos,
      EnumFacing side) {
    Block neighbour = access.getBlockState(pos.offset(side)).getBlock();
    return neighbour instanceof ICsmFramingMember
        && member.acceptsFraming(((ICsmFramingMember) neighbour).getFramingKind());
  }
}
