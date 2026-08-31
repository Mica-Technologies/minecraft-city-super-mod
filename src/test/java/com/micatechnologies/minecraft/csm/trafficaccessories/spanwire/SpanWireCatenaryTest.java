package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The cable solver is the one piece of the span wire system that is pure math, so it is the one
 * piece that can be pinned down exactly rather than eyeballed in game. These tests exist because
 * a solver that is subtly wrong does not crash -- it just draws a cable that looks a bit off,
 * which is very hard to tell from a modelling mistake once there is geometry on top of it.
 */
class SpanWireCatenaryTest {

  private static final double EPSILON = 1.0e-6;

  /** Numerically integrates the curve so the solved length can be checked against the ask. */
  private static double measureLength(SpanWireCatenary cable, int steps) {
    double total = 0.0;
    Vec3d previous = cable.pointAt(0.0);
    for (int i = 1; i <= steps; i++) {
      final Vec3d current = cable.pointAt(i / (double) steps);
      total += current.distanceTo(previous);
      previous = current;
    }
    return total;
  }

  @Test
  void cablePassesThroughBothItsEnds() {
    final Vec3d start = new Vec3d(10.0, 70.0, 10.0);
    final Vec3d end = new Vec3d(30.0, 70.0, 10.0);
    final SpanWireCatenary cable = SpanWireCatenary.between(start, end, 1.005);

    assertEquals(start.x, cable.pointAt(0.0).x, EPSILON);
    assertEquals(start.y, cable.pointAt(0.0).y, EPSILON);
    assertEquals(start.z, cable.pointAt(0.0).z, EPSILON);
    assertEquals(end.x, cable.pointAt(1.0).x, EPSILON);
    assertEquals(end.y, cable.pointAt(1.0).y, EPSILON);
    assertEquals(end.z, cable.pointAt(1.0).z, EPSILON);
  }

  @Test
  void unevenEndsAreBothHitAndSagTheSameEitherWay() {
    final SpanWireCatenary rising = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 73.0, 0.0), 1.005);
    final SpanWireCatenary falling = SpanWireCatenary.between(
        new Vec3d(0.0, 73.0, 0.0), new Vec3d(20.0, 70.0, 0.0), 1.005);

    assertEquals(73.0, rising.pointAt(1.0).y, EPSILON);
    assertEquals(70.0, falling.pointAt(1.0).y, EPSILON);
    // A cable does not care which end is higher.
    assertEquals(rising.sag(), falling.sag(), 1.0e-9);
  }

  @Test
  void solvedCableIsAsLongAsItWasAskedToBe() {
    final Vec3d start = new Vec3d(0.0, 70.0, 0.0);
    final Vec3d end = new Vec3d(24.0, 70.0, 0.0);
    final double slack = 1.01;
    final SpanWireCatenary cable = SpanWireCatenary.between(start, end, slack);

    final double expected = start.distanceTo(end) * slack;
    assertEquals(expected, measureLength(cable, 200000), 1.0e-4);
  }

  /**
   * The property that makes a single default slack usable: the sag stays the same fraction of the
   * span at every span length, so a builder never has to re-tune it for a wider intersection.
   */
  @ParameterizedTest
  @ValueSource(ints = {4, 8, 16, 24, 40, 64})
  void defaultSlackGivesFivePercentSagAtEverySpanLength(int span) {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(span, 70.0, 0.0), SpanWireCatenary.DEFAULT_SLACK);

    assertEquals(0.05, cable.sag() / span, 0.001,
        "sag should be 5% of the span at " + span + " blocks");
  }

  @Test
  void aLevelCableSagsToItsMidpoint() {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 70.0, 0.0), SpanWireCatenary.DEFAULT_SLACK);

    final double midHeight = cable.heightAt(0.5);
    assertTrue(midHeight < 70.0, "cable should hang below its supports");
    assertEquals(70.0 - cable.sag(), midHeight, 1.0e-3);
    // Level cable, so the low point is level too.
    assertEquals(0.0, cable.slopeAt(0.5), 1.0e-6);
    assertTrue(cable.slopeAt(0.1) < 0.0, "cable should still be falling before midspan");
    assertTrue(cable.slopeAt(0.9) > 0.0, "cable should be rising after midspan");
  }

  @Test
  void tooLittleSlackGivesAStraightCable() {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 74.0, 0.0), 1.0);

    assertTrue(cable.isTaut());
    assertEquals(0.0, cable.sag(), EPSILON);
    // Straight means linear interpolation between the ends.
    assertEquals(72.0, cable.heightAt(0.5), EPSILON);
  }

  @Test
  void verticalSpanIsHandledWithoutBlowingUp() {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(5.0, 70.0, 5.0), new Vec3d(5.0, 80.0, 5.0), 1.05);

    assertTrue(cable.isTaut(), "a vertical span has no horizontal extent to hang across");
    assertEquals(75.0, cable.heightAt(0.5), EPSILON);
    assertFalse(Double.isNaN(cable.slopeAt(0.5)));
    assertEquals(0.0, cable.getHorizontalLength(), EPSILON);
  }

  @Test
  void zeroLengthSpanProducesNoNaN() {
    final Vec3d point = new Vec3d(3.0, 70.0, 3.0);
    final SpanWireCatenary cable = SpanWireCatenary.between(point, point, 1.05);

    assertFalse(Double.isNaN(cable.heightAt(0.0)));
    assertFalse(Double.isNaN(cable.heightAt(1.0)));
    assertFalse(Double.isNaN(cable.sag()));
    assertEquals(0.0, cable.parameterAt(3.0, 3.0), EPSILON);
  }

  /**
   * Newton's method has to survive both ends of the range: a nearly taut cable drives the root
   * toward zero, where the derivative vanishes, and a very slack one drives it large.
   */
  @ParameterizedTest
  @ValueSource(doubles = {1.000001, 1.00001, 1.0001, 1.001, 1.005, 1.02, 1.1, 1.5, 2.0, 5.0})
  void solverIsStableAcrossTheWholeSlackRange(double slack) {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 70.0, 0.0), slack);

    assertFalse(Double.isNaN(cable.heightAt(0.5)), "slack " + slack + " produced NaN");
    assertEquals(70.0, cable.heightAt(0.0), EPSILON);
    assertEquals(70.0, cable.heightAt(1.0), 1.0e-4);
    assertTrue(cable.sag() >= 0.0);
  }

  @Test
  void moreSlackAlwaysMeansMoreSag() {
    double previous = -1.0;
    for (double slack : new double[]{1.001, 1.002, 1.005, 1.01, 1.02, 1.05}) {
      final SpanWireCatenary cable = SpanWireCatenary.between(
          new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 70.0, 0.0), slack);
      final double sag = cable.sag();
      assertTrue(sag > previous, "sag should increase with slack, broke at " + slack);
      previous = sag;
    }
  }

  @Test
  void parameterAtProjectsOntoTheSpanAndClamps() {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 70.0, 0.0), 1.005);

    assertEquals(0.0, cable.parameterAt(0.0, 0.0), EPSILON);
    assertEquals(0.5, cable.parameterAt(10.0, 0.0), EPSILON);
    assertEquals(1.0, cable.parameterAt(20.0, 0.0), EPSILON);
    // Off the end, and off to the side: both clamp rather than running away.
    assertEquals(0.0, cable.parameterAt(-40.0, 0.0), EPSILON);
    assertEquals(1.0, cable.parameterAt(90.0, 0.0), EPSILON);
    assertEquals(0.5, cable.parameterAt(10.0, 8.0), EPSILON);
  }

  /**
   * A skewed intersection is the normal case, not the exotic one. The solver was always built
   * from two points rather than an axis, so this is a guard against that being lost rather than a
   * new capability -- but it is the guard that lets hanger discovery walk a diagonal chord.
   */
  @Test
  void aDiagonalSpanSolvesLikeAStraightOneOfTheSameLength() {
    final Vec3d start = new Vec3d(0.0, 70.0, 0.0);
    // 15-20-25 triangle: a diagonal span whose chord is exactly 25 blocks.
    final SpanWireCatenary diagonal = SpanWireCatenary.between(
        start, new Vec3d(15.0, 70.0, 20.0), SpanWireCatenary.DEFAULT_SLACK);
    final SpanWireCatenary straight = SpanWireCatenary.between(
        start, new Vec3d(25.0, 70.0, 0.0), SpanWireCatenary.DEFAULT_SLACK);

    assertEquals(25.0, diagonal.getHorizontalLength(), 1.0e-9);
    assertEquals(straight.sag(), diagonal.sag(), 1.0e-9);
    assertEquals(straight.heightAt(0.3), diagonal.heightAt(0.3), 1.0e-9);

    // Both ends still hit exactly, and the curve follows the diagonal in plan view.
    assertEquals(15.0, diagonal.pointAt(1.0).x, EPSILON);
    assertEquals(20.0, diagonal.pointAt(1.0).z, EPSILON);
    assertEquals(7.5, diagonal.pointAt(0.5).x, EPSILON);
    assertEquals(10.0, diagonal.pointAt(0.5).z, EPSILON);
  }

  @Test
  void parameterAtProjectsOntoADiagonalChord() {
    final SpanWireCatenary cable = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(15.0, 70.0, 20.0), SpanWireCatenary.DEFAULT_SLACK);

    assertEquals(0.5, cable.parameterAt(7.5, 10.0), EPSILON);
    // A point off to the side of the line still projects onto it, which is what lets a mount
    // that is not exactly on the chord be placed correctly along the span.
    assertEquals(0.5, cable.parameterAt(7.5 + 0.8, 10.0 - 0.6), 1.0e-6);
  }

  @Test
  void spanRunningAlongZBehavesTheSameAsOneAlongX() {
    final SpanWireCatenary alongX = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(20.0, 70.0, 0.0), SpanWireCatenary.DEFAULT_SLACK);
    final SpanWireCatenary alongZ = SpanWireCatenary.between(
        new Vec3d(0.0, 70.0, 0.0), new Vec3d(0.0, 70.0, 20.0), SpanWireCatenary.DEFAULT_SLACK);

    assertEquals(alongX.sag(), alongZ.sag(), 1.0e-9);
    assertEquals(alongX.heightAt(0.25), alongZ.heightAt(0.25), 1.0e-9);
  }
}
