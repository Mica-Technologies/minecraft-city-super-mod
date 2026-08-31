package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

/**
 * A block that can hang from a span of messenger cable.
 *
 * <p>Implemented by the <em>payload</em> — the signal head, the sign, the disconnect box — not by
 * anything in this package. That direction is deliberate and is the whole point of the interface:
 * a new kind of thing becomes hangable by declaring it, with no edit to the span wire code and no
 * list here that someone has to remember to add to.
 *
 * <p>Two things follow from implementing it:
 *
 * <ul>
 *   <li>The mount above draws the hardware appropriate to what it is carrying — see
 *   {@link #needsSpanHangerStrap()}.</li>
 *   <li>The payload may take the span's vertical offset, so it hangs on the cable's curve rather
 *   than on the block grid, by calling {@link SpanWireHangOffset#computeFor}. That part is opt-in
 *   rather than automatic, because it is only possible for a block drawn by a tile entity
 *   renderer: a plain JSON block model cannot be moved a fraction of a block, so the static signs
 *   hang at whole-block heights and simply do not call it.</li>
 * </ul>
 */
public interface ISpanWireHangable {

  /**
   * Whether the mount above should draw a strap down onto this block.
   *
   * <p>True for anything that is simply hung — a sign, a box — which needs something visible
   * joining it to the mount. False for anything that already carries its own mounting hardware
   * and would double up: a signal head draws its own mast and bracket, so a strap on top of that
   * reads as two mounts stacked.
   */
  default boolean needsSpanHangerStrap() {
    return true;
  }

  /**
   * Where, horizontally, the mount above should bring its hardware down to meet this block --
   * as an offset from the middle of the block, in blocks.
   *
   * <p>Zero for anything centred in its own block, which is why that is the default. It exists
   * for the things that are not: a signal head's body is set well back in its block, with the
   * visors hanging off the front, so a drop coming down the middle of the block lands on a visor
   * rather than on the roof of the housing.
   *
   * <p>Asked of the payload rather than worked out by the mount, for the same reason as the rest
   * of this interface: the span wire package does not know what a visor is, and should not learn.
   * The block that has the geometry is the block that answers.
   */
  default Vec3d getSpanHardwareOffset(IBlockAccess world, BlockPos pos, IBlockState state) {
    return Vec3d.ZERO;
  }
}
