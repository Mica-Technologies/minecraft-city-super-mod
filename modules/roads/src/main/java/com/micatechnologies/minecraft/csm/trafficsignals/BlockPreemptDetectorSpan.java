package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficaccessories.IPreemptSpanHung;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The preempt detector hung from a span wire (#321), for a junction strung on span wire rather
 * than mast arms: place it in the cell directly under a span wire mount, facing up the approach it
 * serves, and the mount brings its drop down to the detector's hanger.
 * <p>
 * It is {@link BlockPreemptDetector} in everything but its model and box: the same tile entity,
 * linked to a circuit with the Signal Link Tool, called by an emitter it can see or by redstone,
 * and its range and cone stepped by right-click and sneak-right-click.
 *
 * @since 2026.10
 */
public class BlockPreemptDetectorSpan extends BlockPreemptDetector implements IPreemptSpanHung {

  /**
   * The hung head and its hanger: the head's top at 11.8 px, the pipe on up to the top of the
   * cell (from {@code gen_preempt_detector.py}).
   */
  private static final AxisAlignedBB SPAN_BOUNDING_BOX =
      new AxisAlignedBB(6.4 / 16.0, 5.5 / 16.0, 6.4 / 16.0, 9.6 / 16.0, 1.0, 9.6 / 16.0);

  @Override
  public String getBlockRegistryName() {
    return "preempt_detector_span";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SPAN_BOUNDING_BOX;
  }
}
