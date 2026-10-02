package com.micatechnologies.minecraft.csm.technology.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClockTimeTest {

  private static final double EPS = 1e-9;

  @Test
  @DisplayName("tick 0 is six in the morning, 18000 midnight, as the vanilla clock reads")
  void dayStartsAtSix() {
    assertEquals(6 * 60, ClockTime.minuteOfDay(0));
    assertEquals(12 * 60, ClockTime.minuteOfDay(6000));
    assertEquals(0, ClockTime.minuteOfDay(18000));
    assertEquals(23 * 60 + 59, ClockTime.minuteOfDay(17999));
    assertEquals(6 * 60, ClockTime.minuteOfDay(24000), "the next day");
    assertEquals(5 * 60, ClockTime.minuteOfDay(-1000), "negative world time wraps");
  }

  @Test
  @DisplayName("a game minute is 16 2/3 ticks")
  void minuteLength() {
    // 08:50 is 2 h 50 min after six: 2,833.3 ticks.
    assertEquals(8 * 60 + 49, ClockTime.minuteOfDay(2833));
    assertEquals(8 * 60 + 50, ClockTime.minuteOfDay(2834));
  }

  @Test
  @DisplayName("the hands point where a clock's would")
  void handAngles() {
    assertEquals(180.0, ClockTime.hourHandDegrees(0), EPS, "six o'clock");
    assertEquals(0.0, ClockTime.minuteHandDegrees(0), EPS);
    assertEquals(0.0, ClockTime.hourHandDegrees(6000), EPS, "noon");
    assertEquals(90.0, ClockTime.hourHandDegrees(-3000 + 24000), EPS, "three o'clock");
    // 10:10 is 4 h 10 min after six.
    long tenTen = 4000 + 1000 * 10 / 60;
    assertEquals(305.0, ClockTime.hourHandDegrees(tenTen), 0.1);
    assertEquals(60.0, ClockTime.minuteHandDegrees(tenTen), 0.4);
  }

  @Test
  @DisplayName("the sweep hand turns once every 1,200 ticks")
  void sweep() {
    assertEquals(0.0, ClockTime.sweepHandDegrees(0, 0F), EPS);
    assertEquals(90.0, ClockTime.sweepHandDegrees(300, 0F), EPS);
    assertEquals(0.0, ClockTime.sweepHandDegrees(1200, 0F), EPS);
    assertEquals(359.85, ClockTime.sweepHandDegrees(1199, 0.5F), 1e-6);
    assertEquals(45.15, ClockTime.sweepHandDegrees(150 + 1200 * 7, 0.5F), 1e-6);
  }

  @Test
  @DisplayName("a hand at twelve points up and at three to the viewer's right")
  void handCorners() {
    double[] up = ClockTime.handCorners(0, 5, 1, 0.5);
    // the tip corners are at y = 5, the tail ones at y = -1
    assertEquals(5.0, up[5], EPS);
    assertEquals(5.0, up[7], EPS);
    assertEquals(-1.0, up[1], EPS);
    double[] three = ClockTime.handCorners(90, 5, 1, 0.5);
    assertEquals(5.0, three[4], EPS);
    assertEquals(5.0, three[6], EPS);
    assertTrue(Math.abs(three[5]) <= 0.5 && Math.abs(three[7]) <= 0.5);
    double[] six = ClockTime.handCorners(180, 5, 1, 0.5);
    assertEquals(-5.0, six[5], EPS);
  }

  @Test
  @DisplayName("format writes 24-hour HH:MM")
  void formats() {
    assertEquals("08:50", ClockTime.format(530));
    assertEquals("00:00", ClockTime.format(0));
    assertEquals("23:59", ClockTime.format(-1));
  }
}
