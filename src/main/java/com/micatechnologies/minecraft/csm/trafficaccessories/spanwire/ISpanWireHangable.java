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
 *   <li>The mount above draws the hardware appropriate to what it is carrying — where its drop
 *   reaches, whether a tether ties to it, and whether it is fed.</li>
 *   <li>The payload may take the span's vertical offset, so it hangs on the cable's curve rather
 *   than on the block grid, by calling {@link SpanWireHangOffset#computeFor}. That part is opt-in
 *   rather than automatic, because it is only possible for a block drawn by a tile entity
 *   renderer: a plain JSON block model cannot be moved a fraction of a block, so the static signs
 *   hang at whole-block heights and simply do not call it.</li>
 * </ul>
 */
public interface ISpanWireHangable {

  /**
   * Whether this block is fed by the span's lashed conductors.
   *
   * <p>Decides whether the mount above shows the coiled slack at its clamp. That coil is surplus
   * conductor left over from dropping power into the thing below, so it belongs on a signal head
   * and not on a sign: a sign is bolted to the wire and wired to nothing, and a coil at its clamp
   * is hardware for a circuit that does not exist.
   *
   * <p>True by default, because most of what hangs from a span is powered.
   */
  default boolean needsSpanConductorFeed() {
    return true;
  }

  /**
   * Whether this block actually moves when its mount offers a vertical rise.
   *
   * <p>False by default, and the default is the common case: a block drawn from a JSON model sits
   * at whole-block heights and cannot be shifted a fraction of a block, so it stays put no matter
   * what the mount offers. Only a payload drawn by a tile entity renderer can take the offset --
   * a signal head does, through {@link SpanWireHangOffset#computeFor}.
   *
   * <p>The mount needs to know, because it adds that same rise to the geometry a payload reports
   * in order to place its hardware. Adding it for a payload that never moved put the drop's foot
   * a rise above the thing it was supposed to be holding.
   */
  default boolean takesSpanRise() {
    return false;
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

  /**
   * The world height a box span's lower tether should tie to on this block -- its underside --
   * or {@code NaN} for a payload that should not be tied at all.
   *
   * <p>{@code NaN} is the default, and it is the right default: a box span's tether exists to stop
   * <em>signal heads</em> turning in wind. A sign hung on the same span is not tied to it, so
   * anything that does not explicitly want a tie gets none rather than growing a stub to nowhere.
   *
   * <p>Asked of the payload because only the payload knows how far down it actually reaches. A
   * head's underside moves with its section count, its size and the rise the span gives it, so
   * any constant kept on the span side would be right for one configuration and wrong for the
   * rest.
   */
  default double getSpanTetherTieY(IBlockAccess world, BlockPos pos, IBlockState state) {
    return Double.NaN;
  }

  /**
   * The world height a mount's drop should come down to on this block -- its top -- or
   * {@code NaN} to stop at the mount's own attach height instead.
   *
   * <p>{@code NaN} is right for anything that <em>rises</em> to meet the cable: a signal head
   * takes the span's vertical offset, so it comes up to the hardware and the hardware does not
   * have to go looking for it.
   *
   * <p>It is wrong for everything else, and that is what this exists for. A sign hangs at whole
   * block heights and cannot take that offset, so a drop stopping at the mount's attach height --
   * three quarters of the way up the mount's own block -- ends a whole three quarters of a block
   * above the sign, holding nothing.
   */
  default double getSpanHangerTopY(IBlockAccess world, BlockPos pos, IBlockState state) {
    return Double.NaN;
  }
}
