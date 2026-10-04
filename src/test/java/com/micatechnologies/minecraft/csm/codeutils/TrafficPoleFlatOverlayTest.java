package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A traffic pole draws no mount toward a flat overlay: road paint, floor finishes, covers.
 */
class TrafficPoleFlatOverlayTest {

  @Test
  @DisplayName("a full-footprint skin a sixteenth tall or less is an overlay")
  void flatSkinIsOverlay() {
    // road paint drawn a sixteenth tall, pulled down onto the road below it
    assertTrue(AbstractBlockTrafficPole.isFlatOverlay(
        new AxisAlignedBB(0, -0.25, 0, 1, -0.25 + 1.0 / 16, 1)));
    // a carpet-height overlay on the floor of its own cell
    assertTrue(AbstractBlockTrafficPole.isFlatOverlay(new AxisAlignedBB(0, 0, 0, 1, 1.0 / 16, 1)));
  }

  @Test
  @DisplayName("anything taller, or not covering its footprint, is not")
  void otherShapesAreNot() {
    assertFalse(AbstractBlockTrafficPole.isFlatOverlay(new AxisAlignedBB(0, 0, 0, 1, 1, 1)));
    assertFalse(AbstractBlockTrafficPole.isFlatOverlay(new AxisAlignedBB(0, 0, 0, 1, 0.5, 1)));
    assertFalse(AbstractBlockTrafficPole.isFlatOverlay(new AxisAlignedBB(0, 0, 0, 1, 0.125, 1)));
    // a thin plate that does not cover its cell (a small sign, a sensor)
    assertFalse(AbstractBlockTrafficPole.isFlatOverlay(
        new AxisAlignedBB(0.3, 0, 0.3, 0.7, 1.0 / 16, 0.7)));
    assertFalse(AbstractBlockTrafficPole.isFlatOverlay(null));
  }
}
