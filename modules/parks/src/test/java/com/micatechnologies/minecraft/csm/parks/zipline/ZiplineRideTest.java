package com.micatechnologies.minecraft.csm.parks.zipline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/** A zipline ride, stepped as the trolley steps it: speeds up, tops out, brakes, keeps apart. */
class ZiplineRideTest {

  private static final double MIN = 3.0;
  private static final double MAX = 14.0;
  private static final double INF = Double.POSITIVE_INFINITY;

  @Test
  void aSteepLineSpeedsUpToTheCapAndNoFurther() {
    double v = MIN;
    for (int i = 0; i < 400; i++) {
      v = ZiplineRide.next(v, -0.8, 1000, false, INF, MIN, MAX);
    }
    assertEquals(MAX, v, 1.0e-9);
  }

  @Test
  void aGentlerLineTopsOutWhereDragMeetsGravity() {
    double v = MIN;
    for (int i = 0; i < 2000; i++) {
      v = ZiplineRide.next(v, -0.4, 1000, false, INF, MIN, MAX);
    }
    assertEquals(Math.sqrt(ZiplineRide.GRAVITY * 0.4 / ZiplineRide.DRAG), v, 0.05);
  }

  @Test
  void aLevelLineRunsAtTheTrolleysOwnPull() {
    double v = MIN;
    for (int i = 0; i < 100; i++) {
      v = ZiplineRide.next(v, 0.0, 1000, false, INF, MIN, MAX);
    }
    assertEquals(MIN, v, 1.0e-9);
  }

  @Test
  void theBrakesBringAFastRiderInAtAWalk() {
    // A whole ride on a long, steep line: the rider arrives at the stop at arrival speed.
    ZiplineLine line = ZiplineLine.of(new BlockPos(0, 120, 0), new BlockPos(100, 80, 0));
    double s = line.startDistance();
    double v = MIN;
    double peak = 0;
    int ticks = 0;
    while (s < line.stopDistance() && ticks++ < 10_000) {
      v = ZiplineRide.next(v, line.slope(s), line.stopDistance() - s,
          s >= line.brakeDistance(), INF, MIN, MAX);
      peak = Math.max(peak, v);
      s = Math.min(line.stopDistance(), s + v * ZiplineRide.TICK);
    }
    assertTrue(peak > 10, "it got going: " + peak);
    assertTrue(v <= 2.5, "it came in slow: " + v);
  }

  @Test
  void aRiderBehindKeepsTheirDistance() {
    double v = ZiplineRide.next(MAX, -0.4, 1000, false, ZiplineRide.SPACING + 0.2, MIN, MAX);
    assertTrue(v * ZiplineRide.TICK <= 0.2 + 1.0e-9);
    assertEquals(0.0, ZiplineRide.next(MAX, -0.4, 1000, false, ZiplineRide.SPACING - 1, MIN,
        MAX), 1.0e-9);
  }
}
