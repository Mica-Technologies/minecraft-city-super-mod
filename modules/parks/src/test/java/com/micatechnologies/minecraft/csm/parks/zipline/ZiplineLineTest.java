package com.micatechnologies.minecraft.csm.parks.zipline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

/** The zipline's shape: ends on the sheaves, a shallow sag, arc lengths and slopes. */
class ZiplineLineTest {

  private static final BlockPos LAUNCH = new BlockPos(0, 80, 0);
  private static final BlockPos LANDING = new BlockPos(60, 68, 0);
  private final ZiplineLine line = ZiplineLine.of(LAUNCH, LANDING);

  @Test
  void theCableEndsOnBothSheaves() {
    assertClose(ZiplineLine.cablePoint(LAUNCH), line.point(0));
    assertClose(ZiplineLine.cablePoint(LANDING), line.point(line.length()));
  }

  @Test
  void itSagsTwoPercentOfTheSpanAtMidspan() {
    Vec3d mid = line.pointAtT(0.5);
    double chordY = (ZiplineLine.cablePoint(LAUNCH).y + ZiplineLine.cablePoint(LANDING).y) / 2;
    assertEquals(0.02 * 60, chordY - mid.y, 1.0e-9);
  }

  @Test
  void theLengthIsALittleOverTheStraightSpan() {
    assertTrue(line.length() > line.span());
    assertTrue(line.length() < line.span() * 1.01);
  }

  @Test
  void distancesAlongTheCableAreArcLengths() {
    double s = 20.0;
    Vec3d a = line.point(s);
    Vec3d b = line.point(s + 0.5);
    assertEquals(0.5, a.distanceTo(b), 0.01);
  }

  @Test
  void aFallingLineSlopesDownhill() {
    assertTrue(line.slope(line.startDistance()) < 0);
    assertEquals(12.0, line.drop(), 1.0e-9);
  }

  @Test
  void theRideStartsBrakesAndStopsInOrder() {
    assertEquals(1.0, line.startDistance(), 1.0e-9);
    assertEquals(line.length() - 1.5, line.stopDistance(), 1.0e-9);
    assertTrue(line.brakeDistance() > line.startDistance());
    assertTrue(line.brakeDistance() < line.stopDistance());
    assertEquals(8.0, line.stopDistance() - line.brakeDistance(), 1.0e-9);
  }

  @Test
  void itFacesFromLaunchToLanding() {
    // Due east: Minecraft's yaw for +X is -90.
    assertEquals(-90.0F, line.yaw(), 1.0e-4F);
  }

  private static void assertClose(Vec3d expected, Vec3d actual) {
    assertEquals(expected.x, actual.x, 1.0e-9);
    assertEquals(expected.y, actual.y, 1.0e-9);
    assertEquals(expected.z, actual.z, 1.0e-9);
  }
}
