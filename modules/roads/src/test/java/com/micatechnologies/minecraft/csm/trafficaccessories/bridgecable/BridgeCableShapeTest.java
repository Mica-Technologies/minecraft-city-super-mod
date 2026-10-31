package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The shapes are what a builder sets numbers against ("sag 6", "hangers every 5"), so they are
 * held to those numbers exactly rather than to "looks about right".
 */
class BridgeCableShapeTest {

  private static final double EPSILON = 1.0e-9;

  @Test
  void parabolaPassesThroughBothEnds() {
    final Vec3d a = new Vec3d(-1319.5, 111.8125, -2396.5);
    final Vec3d b = new Vec3d(-1319.5, 111.8125, -2280.5);
    final BridgeCableShape.Curve curve = BridgeCableShape.parabola(a, b, 6.0);
    assertEquals(0.0, curve.pointAt(0.0).distanceTo(a), EPSILON);
    assertEquals(0.0, curve.pointAt(1.0).distanceTo(b), EPSILON);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.5, 6.0, 12.0})
  void parabolaHangsItsSagBelowTheChordAtMidspan(double sag) {
    // Ends at different heights: the sag is measured from the chord, not from the lower end.
    final Vec3d a = new Vec3d(0, 111, 0);
    final Vec3d b = new Vec3d(0, 85, 40);
    final BridgeCableShape.Curve curve = BridgeCableShape.parabola(a, b, sag);
    final double chordMid = (a.y + b.y) / 2.0;
    assertEquals(chordMid - sag, curve.pointAt(0.5).y, EPSILON);
  }

  @Test
  void catenaryHangsTheSagAskedFor() {
    final Vec3d a = new Vec3d(0, 90, 0);
    final Vec3d b = new Vec3d(30, 90, 0);
    final BridgeCableShape.Curve curve = BridgeCableShape.catenary(a, b, 1.5);
    double lowest = Double.MAX_VALUE;
    for (int i = 0; i <= 200; i++) {
      lowest = Math.min(lowest, curve.pointAt(i / 200.0).y);
    }
    assertEquals(90.0 - 1.5, lowest, 0.01);
    assertEquals(0.0, curve.pointAt(0.0).distanceTo(a), 1.0e-6);
    assertEquals(0.0, curve.pointAt(1.0).distanceTo(b), 1.0e-3);
  }

  @Test
  void zeroSagIsStraight() {
    final Vec3d a = new Vec3d(0, 100, 0);
    final Vec3d b = new Vec3d(20, 80, 0);
    final Vec3d middle = BridgeCableShape.parabola(a, b, 0.0).pointAt(0.5);
    assertEquals(90.0, middle.y, EPSILON);
    assertEquals(90.0, BridgeCableShape.catenary(a, b, 0.0).pointAt(0.5).y, EPSILON);
  }

  @Test
  void hangersEveryFiveBlocksAlongAHundredBlockSpan() {
    final Vec3d a = new Vec3d(0.5, 111, 0.5);
    final Vec3d b = new Vec3d(0.5, 111, 100.5);
    final List<Double> stations = BridgeCableShape.hangerStations(a, b, 5.0);
    // 5, 10, ... 95: none on the far saddle itself
    assertEquals(19, stations.size());
    assertEquals(0.05, stations.get(0), EPSILON);
    assertEquals(0.95, stations.get(stations.size() - 1), EPSILON);
    // and each lands on a block centre, since the cable starts on one
    for (double t : stations) {
      final double z = a.z + (b.z - a.z) * t;
      assertEquals(0.5, z - Math.floor(z), 1.0e-9);
    }
  }

  @Test
  void noHangerWithinHalfABlockOfTheFarEnd() {
    final List<Double> stations = BridgeCableShape.hangerStations(new Vec3d(0, 0, 0),
        new Vec3d(10.3, 0, 0), 5.0);
    assertEquals(1, stations.size());
  }

  @Test
  void hangerCountIsCapped() {
    assertEquals(BridgeCableLink.MAX_HANGERS, BridgeCableShape.hangerStations(new Vec3d(0, 0, 0),
        new Vec3d(10000, 0, 0), 1.0).size());
  }

  @Test
  void aPlainStayIsOneSegmentAndAStripedOneIsWound() {
    final Vec3d a = new Vec3d(0, 117, 0);
    final Vec3d b = new Vec3d(0, 84, 50);
    assertEquals(1, BridgeCableShape.segmentsFor(BridgeCableStyle.STAY, a, b, false));
    final int striped = BridgeCableShape.segmentsFor(BridgeCableStyle.STAY, a, b, true);
    assertTrue(striped >= 2 * (int) a.distanceTo(b), "striped segments: " + striped);
  }

  @Test
  void aHangingCableIsSampledAboutOnceABlock() {
    final Vec3d a = new Vec3d(0, 111, 0);
    final Vec3d b = new Vec3d(0, 111, 120);
    assertEquals(120, BridgeCableShape.segmentsFor(BridgeCableStyle.MAIN, a, b, false));
    assertEquals(BridgeCableShape.HANGING_MIN_SEGMENTS,
        BridgeCableShape.segmentsFor(BridgeCableStyle.ROPE, a, new Vec3d(0, 111, 3), false));
  }
}
