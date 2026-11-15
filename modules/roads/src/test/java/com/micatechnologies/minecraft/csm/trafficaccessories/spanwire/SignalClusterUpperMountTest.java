package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

/**
 * The geometry an unlinked signal cluster hanger reads from the arm above it, on synthetic input.
 */
class SignalClusterUpperMountTest {

  private static final double EPS = 1.0e-6;

  private static final BlockPos CELL = new BlockPos(660, 12, 565);

  @Test
  void straightArmRunsAlongItsFacing() {
    assertVec(new Vec3d(0, 0, -1), SignalClusterUpperMount.straightAxis(EnumFacing.NORTH));
    assertVec(new Vec3d(1, 0, 0), SignalClusterUpperMount.straightAxis(EnumFacing.EAST));
  }

  @Test
  void diagonalArmRunsAtFortyFiveDegreesTurnedByItsFacing() {
    final double h = Math.sqrt(0.5);
    // North and south share one diagonal, east and west the other, square to it.
    assertVec(new Vec3d(h, 0, -h), SignalClusterUpperMount.diagonalAxis(EnumFacing.NORTH));
    assertVec(new Vec3d(h, 0, -h), SignalClusterUpperMount.diagonalAxis(EnumFacing.SOUTH));
    assertVec(new Vec3d(h, 0, h), SignalClusterUpperMount.diagonalAxis(EnumFacing.EAST));
    final Vec3d ns = SignalClusterUpperMount.diagonalAxis(EnumFacing.NORTH);
    final Vec3d ew = SignalClusterUpperMount.diagonalAxis(EnumFacing.WEST);
    assertEquals(0.0, ns.dotProduct(ew), EPS);
  }

  @Test
  void armKeepsItsRadiusInBlocksAndNormalisesItsAxis() {
    final SignalClusterUpperMount arm = SignalClusterUpperMount.arm(new Vec3d(1, 2, 3),
        new Vec3d(0, 0, 5), 6.0);
    assertEquals(SignalClusterUpperMount.Kind.ARM, arm.getKind());
    assertEquals(6.0 / 16.0, arm.getRadius(), EPS);
    assertVec(new Vec3d(0, 0, 1), arm.getAxis());
  }

  @Test
  void flangeSitsOnTheMiddleOfTheFaceAbove() {
    final SignalClusterUpperMount flange =
        SignalClusterUpperMount.flange(new BlockPos(660, 11, 565));
    assertEquals(SignalClusterUpperMount.Kind.FLANGE, flange.getKind());
    assertVec(new Vec3d(660.5, 12.0, 565.5), flange.getCentre());
  }

  @Test
  void railOverTheMiddleIsClampedWhereItCrosses() {
    // A pipe rising along Z, through the middle of the cell, radius 3.
    final float[][] rails = {{8, 4, 0, 8, 8, 16, 3}};
    final SignalClusterUpperMount arm = SignalClusterUpperMount.fromRails(rails, CELL);
    assertNotNull(arm);
    assertVec(new Vec3d(660.5, 12 + 6.0 / 16.0, 565.5), arm.getCentre());
    assertEquals(3.0 / 16.0, arm.getRadius(), EPS);
    // Along the pipe's own slope, not flattened.
    assertNotEquals(0.0, arm.getAxis().y);
  }

  @Test
  void lowestOfTwoChordsWins() {
    final float[][] rails = {{0, 12, 8, 16, 12, 8, 2}, {0, 3, 8, 16, 3, 8, 2}};
    final SignalClusterUpperMount arm = SignalClusterUpperMount.fromRails(rails, CELL);
    assertNotNull(arm);
    assertEquals(12 + 3.0 / 16.0, arm.getCentre().y, EPS);
  }

  @Test
  void railBesideTheMiddleIsNotClamped() {
    // Along X, five sixteenths off the middle with a radius of three: out of reach.
    final float[][] rails = {{0, 8, 13, 16, 8, 13, 3}};
    assertNull(SignalClusterUpperMount.fromRails(rails, CELL));
    assertNull(SignalClusterUpperMount.fromRails(null, CELL));
  }

  @Test
  void stateKeyTellsMountsApart() {
    final BlockPos hanger = new BlockPos(660, 11, 565);
    final long none = SignalClusterUpperMount.NONE.stateKey();
    final long flange = SignalClusterUpperMount.flange(hanger).stateKey();
    final Vec3d centre = new Vec3d(660.5, 12.5, 565.5);
    final long alongX = SignalClusterUpperMount.arm(centre, new Vec3d(1, 0, 0), 6).stateKey();
    final long alongZ = SignalClusterUpperMount.arm(centre, new Vec3d(0, 0, 1), 6).stateKey();
    final long thin = SignalClusterUpperMount.arm(centre, new Vec3d(1, 0, 0), 4).stateKey();
    assertNotEquals(none, flange);
    assertNotEquals(flange, alongX);
    assertNotEquals(alongX, alongZ);
    assertNotEquals(alongX, thin);
  }

  private static void assertVec(Vec3d expected, Vec3d actual) {
    assertEquals(expected.x, actual.x, EPS, "x");
    assertEquals(expected.y, actual.y, EPS, "y");
    assertEquals(expected.z, actual.z, EPS, "z");
  }
}
