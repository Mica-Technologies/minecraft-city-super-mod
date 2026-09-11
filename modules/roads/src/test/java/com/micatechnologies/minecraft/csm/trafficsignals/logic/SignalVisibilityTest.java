package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.SignalVisibility.Direction;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SignalVisibilityTest {

  private static final double EPS = 1.0e-6;

  // Rotation angles as the head renderer applies them (DirectionSixteen / getBaseFacingAngle).
  private static final double NORTH = 0.0;
  private static final double WEST = 90.0;
  private static final double SOUTH = 180.0;
  private static final double EAST = 270.0;
  private static final double NORTH_EAST = 315.0;

  // region: frames

  @Test
  void viewerStraightAheadOfEachFacingIsAtZeroAzimuth() {
    // Head at the origin; the viewer stands 10 blocks away in the facing direction.
    assertEquals(0.0, SignalVisibility.directionFromLens(0, 0, 0, 0, 0, -10, NORTH).azimuthDeg, EPS);
    assertEquals(0.0, SignalVisibility.directionFromLens(0, 0, 0, 0, 0, 10, SOUTH).azimuthDeg, EPS);
    assertEquals(0.0, SignalVisibility.directionFromLens(0, 0, 0, 10, 0, 0, EAST).azimuthDeg, EPS);
    assertEquals(0.0, SignalVisibility.directionFromLens(0, 0, 0, -10, 0, 0, WEST).azimuthDeg, EPS);
    assertEquals(0.0,
        SignalVisibility.directionFromLens(0, 0, 0, 10, 0, -10, NORTH_EAST).azimuthDeg, EPS);
  }

  @Test
  void viewerBehindTheHeadIsFlaggedForEveryFacing() {
    assertTrue(SignalVisibility.directionFromLens(0, 0, 0, 0, 0, 10, NORTH).behind);
    assertTrue(SignalVisibility.directionFromLens(0, 0, 0, -10, 0, 0, EAST).behind);
    assertFalse(SignalVisibility.directionFromLens(0, 0, 0, 10, 0, 0, EAST).behind);
  }

  @Test
  void azimuthIsPositiveToTheHeadsRight() {
    // A north-facing head's right is east (+X).
    Direction d = SignalVisibility.directionFromLens(0, 0, 0, 10, 0, -10, NORTH);
    assertEquals(45.0, d.azimuthDeg, EPS);
    // An east-facing head's right is south (+Z).
    d = SignalVisibility.directionFromLens(0, 0, 0, 10, 0, 10, EAST);
    assertEquals(45.0, d.azimuthDeg, EPS);
    d = SignalVisibility.directionFromLens(0, 0, 0, 10, 0, -10, EAST);
    assertEquals(-45.0, d.azimuthDeg, EPS);
  }

  @Test
  void elevationIsNegativeBelowTheLensAndIndependentOfFacing() {
    // 10 blocks out, 10 blocks down: 45° below.
    Direction d = SignalVisibility.directionFromLens(0, 10, 0, 0, 0, -10, NORTH);
    assertEquals(-45.0, d.elevationDeg, EPS);
    d = SignalVisibility.directionFromLens(0, 10, 0, 10, 0, 0, EAST);
    assertEquals(-45.0, d.elevationDeg, EPS);
    assertEquals(Math.sqrt(200.0), d.distance, EPS);
  }

  // endregion

  // region: fade

  @Test
  void fadeIsOneBelowFullZeroBeyondAndLinearBetween() {
    assertEquals(1.0f, SignalVisibility.fade(0.0, 8.0, 20.0), EPS);
    assertEquals(1.0f, SignalVisibility.fade(8.0, 8.0, 20.0), EPS);
    assertEquals(0.5f, SignalVisibility.fade(14.0, 8.0, 20.0), EPS);
    assertEquals(0.0f, SignalVisibility.fade(20.0, 8.0, 20.0), EPS);
    assertEquals(0.0f, SignalVisibility.fade(90.0, 8.0, 20.0), EPS);
  }

  // endregion

  // region: vertical louvers

  @Test
  void verticalLouversAreFullAheadAndDarkToTheSide() {
    assertEquals(1.0f, SignalVisibility.verticalLouverFactor(dir(0.0, -20.0)), EPS);
    assertEquals(1.0f, SignalVisibility.verticalLouverFactor(dir(-SignalVisibility.VERTICAL_FULL_DEG, -20.0)), EPS);
    assertEquals(0.0f, SignalVisibility.verticalLouverFactor(dir(SignalVisibility.VERTICAL_ZERO_DEG, -20.0)), EPS);
    assertEquals(0.0f, SignalVisibility.verticalLouverFactor(dir(60.0, -20.0)), EPS);
    float mid = SignalVisibility.verticalLouverFactor(
        dir((SignalVisibility.VERTICAL_FULL_DEG + SignalVisibility.VERTICAL_ZERO_DEG) / 2.0, 0.0));
    assertEquals(0.5f, mid, EPS);
  }

  @Test
  void verticalLouversAreDarkFromBehind() {
    Direction behind = SignalVisibility.directionFromLens(0, 0, 0, 0, 0, 10, NORTH);
    assertEquals(0.0f, SignalVisibility.verticalLouverFactor(behind), EPS);
  }

  @Test
  void verticalLouversFollowTheHeadsTilt() {
    // A viewer 20° to the east of north is outside a north-facing head's band but inside the
    // band of the same head turned to NNE (22.5°).
    double x = 10.0 * Math.sin(Math.toRadians(20.0));
    double z = -10.0 * Math.cos(Math.toRadians(20.0));
    Direction straight = SignalVisibility.directionFromLens(0, 0, 0, x, 0, z, NORTH);
    Direction tilted = SignalVisibility.directionFromLens(0, 0, 0, x, 0, z, 337.5);
    assertEquals(0.0f, SignalVisibility.verticalLouverFactor(straight), EPS);
    assertEquals(1.0f, SignalVisibility.verticalLouverFactor(tilted), EPS);
  }

  // endregion

  // region: horizontal louvers

  @Test
  void horizontalLouversAreFullInsideTheBandAndFadeOutside() {
    assertEquals(1.0f, SignalVisibility.horizontalLouverFactor(dir(0.0, -20.0), -30.0, -15.0), EPS);
    assertEquals(1.0f, SignalVisibility.horizontalLouverFactor(dir(0.0, -30.0), -30.0, -15.0), EPS);
    assertEquals(0.0f, SignalVisibility.horizontalLouverFactor(dir(0.0, -5.0), -30.0, -15.0), EPS);
    assertEquals(0.0f, SignalVisibility.horizontalLouverFactor(dir(0.0, -45.0), -30.0, -15.0), EPS);
    float half = SignalVisibility.horizontalLouverFactor(
        dir(0.0, -15.0 + SignalVisibility.HORIZONTAL_FADE_DEG / 2.0), -30.0, -15.0);
    assertEquals(0.5f, half, EPS);
  }

  @Test
  void defaultBandIsCentredOnTheSlatAngle() {
    double[] band = SignalVisibility.defaultHorizontalBand(24.0);
    assertEquals(-24.0 - SignalVisibility.DEFAULT_SLAT_HALF_BAND_DEG, band[0], EPS);
    assertEquals(-24.0 + SignalVisibility.DEFAULT_SLAT_HALF_BAND_DEG, band[1], EPS);
  }

  @Test
  void programmedBandSpansTheNearAndFarDriverEyes() {
    // Head lens 8 blocks above a road at y=64 (surface 65, eye 66.6): a near point 10 blocks out
    // and a far point 40 blocks out, straight ahead.
    SignalVisibilityArea area = SignalVisibilityArea.of(Arrays.asList(
        new int[]{-1, 64, -10}, new int[]{1, 64, -40}));
    assertNotNull(area);
    double lensY = 65.0 + 8.0;
    double[] band = SignalVisibility.programmedHorizontalBand(area, 0.5, lensY, 0.5);
    double drop = lensY - (65.0 + SignalVisibility.DRIVER_EYE_HEIGHT);
    // Nearest vertex is (-0.5, -9.5) -> 10 blocks out; farthest is (1.5, -39.5) -> ~40 blocks.
    double near = Math.toDegrees(Math.atan2(-drop, Math.hypot(-0.5 - 0.5, -9.5 - 0.5)));
    double far = Math.toDegrees(Math.atan2(-drop, Math.hypot(1.5 - 0.5, -39.5 - 0.5)));
    assertEquals(near - SignalVisibility.HORIZONTAL_PAD_DEG, band[0], EPS);
    assertEquals(far + SignalVisibility.HORIZONTAL_PAD_DEG, band[1], EPS);
    assertTrue(band[0] < band[1]);
  }

  @Test
  void slatAdjustAimsAtTheBandCentreAndStaysWithinLimits() {
    // Band centred 24° below horizontal with a 9° visor and 15° built in: no adjustment.
    assertEquals(0.0, SignalVisibility.horizontalSlatExtraTiltAdjust(-30.0, -18.0, 9.0, 15.0), EPS);
    // Band centred 40° below: extra should be 31°, i.e. +16 over the built-in 15.
    assertEquals(16.0, SignalVisibility.horizontalSlatExtraTiltAdjust(-50.0, -30.0, 9.0, 15.0), EPS);
    // A band shallower than the visor's own tilt clamps at zero extra: -15 adjustment.
    assertEquals(-15.0, SignalVisibility.horizontalSlatExtraTiltAdjust(-8.0, -2.0, 9.0, 15.0), EPS);
    // A very steep band clamps at the maximum.
    assertEquals(SignalVisibility.MAX_SLAT_EXTRA_TILT_DEG - 15.0,
        SignalVisibility.horizontalSlatExtraTiltAdjust(-85.0, -75.0, 9.0, 15.0), EPS);
  }

  // endregion

  // region: programmable

  private static SignalVisibilityArea approach() {
    // A 6-wide lane running north from z=-10 to z=-40, road at y=64.
    return SignalVisibilityArea.of(Arrays.asList(
        new int[]{-3, 64, -10}, new int[]{3, 64, -40}));
  }

  private static final double LENS_Y = 65.0 + 6.0;
  private static final double EYE_Y = 65.0 + SignalVisibility.DRIVER_EYE_HEIGHT;

  @Test
  void unprogrammedHeadIsVisibleFromEverywhere() {
    assertEquals(1.0f, SignalVisibility.programmableFactor(null, 0, LENS_Y, 0, 100, 0, 100), EPS);
  }

  @Test
  void driverInsideTheAreaSeesTheLens() {
    assertEquals(1.0f,
        SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, EYE_Y, -25.0), EPS);
  }

  @Test
  void driverFarOutsideTheAreaSeesNothing() {
    // Across the intersection, 25 blocks south of a head whose area is to the north.
    assertEquals(0.0f,
        SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, EYE_Y, 25.0), EPS);
    // Well off to the side of the lane.
    assertEquals(0.0f,
        SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 30.0, EYE_Y, -25.0), EPS);
  }

  @Test
  void driverJustPastTheEdgeSeesAFadingLens() {
    // The lane's east edge is at x=3.5; stand a little past it, 25 blocks out.
    float f = SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 4.5, EYE_Y, -25.0);
    assertTrue(f > 0.0f && f < 1.0f, "expected a partial factor, got " + f);
    float nearer = SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 4.0, EYE_Y, -25.0);
    assertTrue(nearer > f, "closer to the edge should be brighter");
  }

  @Test
  void projectionThroughTheLensUsesTheEyePlaneNotTheEyeItself() {
    // A viewer hovering at half the lens height, 12 blocks out, projects to 24 blocks out --
    // inside the lane -- even though 12 blocks is inside too. Then at 4 blocks out and half
    // height it projects to 8 blocks, which is short of the area's near edge at 9.5.
    double halfWay = (LENS_Y + EYE_Y) / 2.0;
    assertEquals(1.0f,
        SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, halfWay, -12.0), EPS);
    float shortOfIt = SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, halfWay, -4.0);
    assertTrue(shortOfIt < 1.0f);
  }

  @Test
  void viewerLevelWithOrAboveTheLensOnlyGetsTheLeak() {
    float far = SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, LENS_Y + 5.0, -30.0);
    assertEquals(0.0f, far, EPS);
    float near = SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, LENS_Y + 1.0, -1.0);
    assertEquals(SignalVisibility.leakFactor(Math.hypot(1.0, 1.5)), near, EPS);
    assertTrue(near > 0.0f);
  }

  @Test
  void leakFadesWithDistance() {
    assertEquals((float) SignalVisibility.LEAK_MAX, SignalVisibility.leakFactor(0.0), EPS);
    assertEquals((float) (SignalVisibility.LEAK_MAX / 2.0),
        SignalVisibility.leakFactor(SignalVisibility.LEAK_RANGE / 2.0), EPS);
    assertEquals(0.0f, SignalVisibility.leakFactor(SignalVisibility.LEAK_RANGE), EPS);
    assertEquals(0.0f, SignalVisibility.leakFactor(100.0), EPS);
  }

  @Test
  void standingUnderTheHeadOutsideTheAreaShowsTheLeak() {
    // 2 blocks south of the head on the ground: outside, but close.
    float f = SignalVisibility.programmableFactor(approach(), 0.5, LENS_Y, 0.5, 0.5, EYE_Y, 2.5);
    double distance = Math.sqrt(2.0 * 2.0 + (LENS_Y - EYE_Y) * (LENS_Y - EYE_Y));
    assertEquals(SignalVisibility.leakFactor(distance), f, EPS);
  }

  // endregion

  private static Direction dir(double azimuthDeg, double elevationDeg) {
    return new Direction(azimuthDeg, elevationDeg, 10.0, false);
  }
}
