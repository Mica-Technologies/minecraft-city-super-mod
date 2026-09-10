package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What it means for one work zone device to join the one beside it.
 *
 * <p>Barricades and barrier walls are both drawn as a core plus two detachable ends, with the end
 * left off wherever something connects. What differs is what the ends carry — a barricade's is an
 * overhang and an upright, a wall's is the cap over its open section — and the rule for whether
 * two of them join is the same, so it lives here once rather than in each of them.</p>
 *
 * <p>Connection is resolved from the neighbours every time it is asked for rather than stored, so
 * breaking a device out of the middle of a run closes the two halves up without anything having
 * to be notified.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
final class WorkZoneJoins {

  /**
   * Whether a matching device adjoins the model's left-hand end.
   *
   * @since 1.0
   */
  static final PropertyBool CONNECT_LEFT = PropertyBool.create("connectleft");

  /**
   * Whether a matching device adjoins the model's right-hand end.
   *
   * @since 1.0
   */
  static final PropertyBool CONNECT_RIGHT = PropertyBool.create("connectright");

  /**
   * Whether this device has to close a gap to the device on its right.
   *
   * <p>A device spans one cell and a diagonal step between cell centres is sqrt(2) cells, so a
   * run laid at forty-five degrees stands off its neighbour by about four tenths of a cell.
   * Connecting is not enough to look connected: the ends come off, and what is left is a dotted
   * line. So a device joined DIAGONALLY grows a filler that bridges it.</p>
   *
   * <p>Only ever on the right. Filling from both sides of a joint would put two fillers in the
   * same air, and the seam would z-fight down its whole length.</p>
   *
   * @since 1.0
   */
  static final PropertyBool DIAG_FILL = PropertyBool.create("diagfill");

  /**
   * Whether a matching device abuts the block's north, south, west or east side.
   *
   * <p>Separate from the pair above because these are WORLD directions, not the ends of
   * something that faces a way. A road plate is laid in a patch rather than a line and has no
   * facing at all, so "left" and "right" mean nothing to it.</p>
   *
   * @since 1.0
   */
  static final PropertyBool CONNECT_NORTH = PropertyBool.create("connectnorth");

  /** @see #CONNECT_NORTH */
  static final PropertyBool CONNECT_SOUTH = PropertyBool.create("connectsouth");

  /** @see #CONNECT_NORTH */
  static final PropertyBool CONNECT_WEST = PropertyBool.create("connectwest");

  /** @see #CONNECT_NORTH */
  static final PropertyBool CONNECT_EAST = PropertyBool.create("connecteast");

  /**
   * Utility class; not instantiable.
   *
   * @since 1.0
   */
  private WorkZoneJoins() {
    throw new AssertionError("WorkZoneJoins is a utility class and must not be instantiated");
  }

  /**
   * Sets both connection properties on a state from what stands either side of it.
   *
   * @param self   the block asking, so only its own kind counts as a neighbour
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with its connections set
   *
   * @since 1.0
   */
  static IBlockState resolve(Block self, IBlockState state, IBlockAccess access, BlockPos pos) {
    DirectionEight facing = state.getValue(AbstractBlockRotatableHZEight.FACING);
    boolean right = joins(self, access, pos, facing, facing.rotateY());
    return state
        .withProperty(CONNECT_LEFT, joins(self, access, pos, facing, facing.rotateYCCW()))
        .withProperty(CONNECT_RIGHT, right)
        .withProperty(DIAG_FILL, right && facing.isDiagonal());
  }

  /**
   * Sets all four side properties on a state from what abuts it, for a device laid in a patch
   * rather than a line.
   *
   * @param self   the block asking, so only its own kind counts as a neighbour
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with its four sides set
   *
   * @since 1.0
   */
  static IBlockState resolveSides(Block self, IBlockState state, IBlockAccess access,
      BlockPos pos) {
    return state
        .withProperty(CONNECT_NORTH, abuts(self, access, pos, EnumFacing.NORTH))
        .withProperty(CONNECT_SOUTH, abuts(self, access, pos, EnumFacing.SOUTH))
        .withProperty(CONNECT_WEST, abuts(self, access, pos, EnumFacing.WEST))
        .withProperty(CONNECT_EAST, abuts(self, access, pos, EnumFacing.EAST));
  }

  /**
   * Gets whether the same block stands one step in the given direction.
   *
   * @param self      the block asking
   * @param access    the block access
   * @param pos       this device's position
   * @param direction the direction to look in
   *
   * @return true if the neighbour is the same block
   *
   * @since 1.0
   */
  private static boolean abuts(Block self, IBlockAccess access, BlockPos pos,
      EnumFacing direction) {
    return access.getBlockState(pos.offset(direction)).getBlock() == self;
  }

  /**
   * Gets whether the block one step in the given direction is one this should join.
   *
   * <p>Devices join only along their own length, only to the same block, and only to one facing
   * the same way. A run that changes kind or direction partway is two runs, and drawing it as one
   * would butt an orange wall into a concrete one, or a keep-left barricade into a keep-right —
   * whose striping slopes toward the side traffic should pass, so the two contradict each other
   * in the middle of the run.</p>
   *
   * <p>A diagonal run steps diagonally, so the neighbour it looks for shares only a corner with
   * this block rather than a face. That is what a line of devices following a diagonal road
   * actually looks like, and it is why this cannot be written in terms of {@code EnumFacing}.</p>
   *
   * @param self      the block asking
   * @param access    the block access
   * @param pos       this device's position
   * @param facing    this device's facing
   * @param direction the direction to look in
   *
   * @return true if the neighbour is the same block facing the same way
   *
   * @since 1.0
   */
  private static boolean joins(Block self, IBlockAccess access, BlockPos pos,
      DirectionEight facing, DirectionEight direction) {
    // Stepped by the direction's own offsets rather than with BlockPos.offset, because a run
    // laid at forty-five degrees continues into the block diagonally adjacent and EnumFacing
    // cannot name that block at all.
    BlockPos neighbourPos = pos.add(direction.getOffsetX(), 0, direction.getOffsetZ());
    IBlockState neighbour = access.getBlockState(neighbourPos);
    return neighbour.getBlock() == self
        && neighbour.getPropertyKeys().contains(AbstractBlockRotatableHZEight.FACING)
        && neighbour.getValue(AbstractBlockRotatableHZEight.FACING) == facing;
  }
}
