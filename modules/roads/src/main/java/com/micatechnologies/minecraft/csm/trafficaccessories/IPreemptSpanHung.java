package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.ISpanWireHangable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A preemption device hung from a span wire (#321): the span wire version of the preempt detector
 * or of a confirmation light, placed in the cell directly under a span wire mount.
 *
 * <p>Each one is a subclass of the arm-mounted device it copies, so linking, detection and
 * lighting are that device's own; all this adds is how it hangs. Its model
 * ({@code gen_preempt_detector.py}, {@code <name>_span}) is the device without its mount stub,
 * raised to near the top of its cell on a collar and a short pipe that reaches the top of the
 * cell. A block model cannot take the span's sub-block rise, so it hangs at whole-block height
 * like a sign, and the mount above brings its drop down to the pipe.</p>
 *
 * @since 2026.10
 */
public interface IPreemptSpanHung extends ISpanWireHangable {

  /** The drop comes down to the top of the cell, where the device's own hanger pipe ends. */
  @Override
  default double getSpanHangerTopY(IBlockAccess world, BlockPos pos, IBlockState state) {
    return pos.getY() + 1.0;
  }
}
