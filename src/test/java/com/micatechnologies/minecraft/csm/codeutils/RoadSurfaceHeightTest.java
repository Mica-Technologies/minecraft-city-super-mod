package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RoadSurfaceHeight}, covering the decision that turns the bounding box of
 * the block below into the height a block settles onto.
 *
 * <p>The world lookup is deliberately not exercised here — it is a handful of guards around
 * {@link RoadSurfaceHeight#surfaceFromBox}, and standing up a block registry to reach them would
 * test the harness rather than the rule.</p>
 */
class RoadSurfaceHeightTest {

  /** Tolerance for comparing block-space heights. */
  private static final double TOL = 1e-9;

  /**
   * Builds a full-footprint box between the two heights given.
   *
   * @param minY the bottom of the box
   * @param maxY the top of the box
   *
   * @return the box
   */
  private static AxisAlignedBB surface(double minY, double maxY) {
    return new AxisAlignedBB(0.0, minY, 0.0, 1.0, maxY, 1.0);
  }

  @Test
  void fullBlockShouldNotOffset() {
    assertEquals(RoadSurfaceHeight.NO_OFFSET,
        RoadSurfaceHeight.surfaceFromBox(surface(0.0, 1.0)), TOL);
    assertEquals(0.0, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(surface(0.0, 1.0))), TOL);
  }

  @Test
  void bottomSlabShouldOffsetByHalfABlock() {
    assertEquals(0.5, RoadSurfaceHeight.surfaceFromBox(surface(0.0, 0.5)), TOL);
    assertEquals(-0.5, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(surface(0.0, 0.5))), TOL);
  }

  @Test
  void everySixteenthStepShouldOffsetByItsRemainder() {
    // A road that climbs is built from blocks whose top face sits at (step + 1) / 16.
    for (int step = 0; step < 16; step++) {
      double top = (step + 1) / 16.0;
      double surface = RoadSurfaceHeight.surfaceFromBox(surface(0.0, top));
      double offset = RoadSurfaceHeight.offsetForSurface(surface);
      if (step == 15) {
        // The topmost step fills its cell, so there is nothing to settle by.
        assertEquals(0.0, offset, TOL, "step " + step);
      } else {
        assertEquals(top, surface, TOL, "step " + step);
        assertEquals(-(1.0 - top), offset, TOL, "step " + step);
      }
    }
  }

  @Test
  void snowLayerShouldOffsetOntoItsTop() {
    assertEquals(0.125, RoadSurfaceHeight.surfaceFromBox(surface(0.0, 0.125)), TOL);
    assertEquals(-0.875, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(surface(0.0, 0.125))), TOL);
  }

  @Test
  void degenerateEmptyBoxShouldNotOffset() {
    // Some road variants report a zero box; taking its top would drop a device a full metre.
    AxisAlignedBB empty = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    assertEquals(RoadSurfaceHeight.NO_OFFSET, RoadSurfaceHeight.surfaceFromBox(empty), TOL);
    assertEquals(0.0, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(empty)), TOL);
  }

  @Test
  void nullBoxShouldNotOffset() {
    assertEquals(RoadSurfaceHeight.NO_OFFSET, RoadSurfaceHeight.surfaceFromBox(null), TOL);
  }

  @Test
  void partialFootprintShouldNotOffset() {
    // A fence post: tall enough to look like a surface, too narrow to be one.
    AxisAlignedBB post = new AxisAlignedBB(0.375, 0.0, 0.375, 0.625, 0.9, 0.625);
    assertEquals(RoadSurfaceHeight.NO_OFFSET, RoadSurfaceHeight.surfaceFromBox(post), TOL);
  }

  @Test
  void partialFootprintOnOneAxisOnlyShouldNotOffset() {
    AxisAlignedBB halfSlab = new AxisAlignedBB(0.0, 0.0, 0.0, 0.5, 0.5, 1.0);
    assertEquals(RoadSurfaceHeight.NO_OFFSET, RoadSurfaceHeight.surfaceFromBox(halfSlab), TOL);
  }

  @Test
  void flatMarkingOnAClimbingRoadShouldSettleOntoItsTop() {
    // A flat marking occupies the cell above the road and is drawn pulled down onto it, so on a
    // road that does not fill its cell the marking's own box sits below its cell too. A device
    // stands on top of the marking, as it would on a painted stripe.
    double roadTop = 0.5;
    AxisAlignedBB marking = surface(-1.0 + roadTop, -1.0 + roadTop + 0.0625);
    assertEquals(-0.4375, RoadSurfaceHeight.surfaceFromBox(marking), TOL);
    assertEquals(-1.4375, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(marking)), TOL);
  }

  @Test
  void flatMarkingOnAFullRoadShouldSettleOntoItsTopToo() {
    // The same marking on a road that does fill its cell. The device stands on the marking here
    // as well: one rule, one result, whatever the road height.
    AxisAlignedBB marking = surface(0.0, 0.0625);
    assertEquals(0.0625, RoadSurfaceHeight.surfaceFromBox(marking), TOL);
    assertEquals(-0.9375, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(marking)), TOL);
  }

  @Test
  void boxWhoseTopIsBeyondTheClampShouldNotOffset() {
    assertEquals(RoadSurfaceHeight.NO_OFFSET,
        RoadSurfaceHeight.surfaceFromBox(surface(-2.5, -1.5)), TOL);
  }

  @Test
  void tallBoxBelowItsCellShouldUseItsTopNotItsBottom() {
    // A full-height barrier is also drawn pulled down onto the road, so it too has a bottom
    // below its own cell. Settling onto its bottom would sink a device through it.
    AxisAlignedBB barrier = surface(-0.5, 0.5);
    assertEquals(0.5, RoadSurfaceHeight.surfaceFromBox(barrier), TOL);
    assertEquals(-0.5, RoadSurfaceHeight.offsetForSurface(
        RoadSurfaceHeight.surfaceFromBox(barrier)), TOL);
  }

  @Test
  void boxOverfillingItsCellShouldNotOffset() {
    assertEquals(RoadSurfaceHeight.NO_OFFSET,
        RoadSurfaceHeight.surfaceFromBox(surface(0.0, 1.5)), TOL);
  }

  @Test
  void dropShouldBeClampedToTwoBlocks() {
    // Nothing legitimate asks for more than two, so anything that does is refused rather than
    // sinking a device through the world.
    assertEquals(-2.0, RoadSurfaceHeight.offsetForSurface(-5.0), TOL);
    assertEquals(-2.0, RoadSurfaceHeight.offsetForSurface(-1.0), TOL);
  }

  @Test
  void offsetShouldNeverBePositive() {
    for (double surface = -3.0; surface <= 2.0; surface += 0.05) {
      assertTrue(RoadSurfaceHeight.offsetForSurface(surface) <= 0.0,
          "surface " + surface + " produced a positive offset");
    }
  }

  @Test
  void offsetShouldNeverExceedTheClamp() {
    for (double surface = -3.0; surface <= 2.0; surface += 0.05) {
      assertTrue(RoadSurfaceHeight.offsetForSurface(surface) >= -2.0,
          "surface " + surface + " produced a drop beyond the clamp");
    }
  }
}
