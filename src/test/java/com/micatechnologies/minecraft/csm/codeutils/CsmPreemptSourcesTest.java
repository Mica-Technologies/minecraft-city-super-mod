package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.codeutils.CsmPreemptEmitter.Kind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The preempt detector's geometry: a detector at the origin looking south (+z), range 120, 20
 * degrees either side.
 */
class CsmPreemptSourcesTest {

  private static boolean sees(double x, double z, double headingX, double headingZ) {
    return CsmPreemptSources.sees(new CsmPreemptEmitter(x, 0, z, headingX, headingZ,
        Kind.EMERGENCY), 0, 0, 0, 0, 1, 120, 20);
  }

  @Test
  @DisplayName("a vehicle up the approach, driving towards the detector, is seen")
  void approachingIsSeen() {
    assertTrue(sees(0, 100, 0, -1));
    assertTrue(sees(10, 60, 0, -1), "a little off the centre line, inside the cone");
  }

  @Test
  @DisplayName("a vehicle driving away down the same road is not seen")
  void recedingIsNotSeen() {
    assertFalse(sees(0, 100, 0, 1));
  }

  @Test
  @DisplayName("beyond the range, outside the cone, or behind the detector: not seen")
  void outOfViewIsNotSeen() {
    assertFalse(sees(0, 130, 0, -1), "past the range");
    assertFalse(sees(60, 60, -1, -1), "45 degrees off: outside a 20 degree cone");
    assertFalse(sees(0, -50, 0, 1), "behind the detector, even driving towards it");
  }

  @Test
  @DisplayName("an emitter beam is wide enough for a vehicle still turning in")
  void turningInIsSeen() {
    assertTrue(sees(0, 80, -0.5, -1), "facing about 27 degrees off the detector");
  }

  @Test
  @DisplayName("an emitter with no heading counts from any direction inside the cone")
  void noHeadingCounts() {
    assertTrue(sees(0, 50, 0, 0));
  }
}
