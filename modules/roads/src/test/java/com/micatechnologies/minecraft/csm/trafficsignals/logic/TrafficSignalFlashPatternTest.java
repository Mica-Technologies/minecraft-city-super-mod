package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrafficSignalFlashPatternTest {

  @Test
  void offAndBAreExactComplements() {
    // A wig-wag pair is only convincing if exactly one of the two is lit at any instant.
    for (long t = 0; t < 5000; t += 7) {
      assertNotEquals(TrafficSignalFlashPattern.OFF.isFlashLit(t),
          TrafficSignalFlashPattern.B.isFlashLit(t),
          "OFF and B should never agree, at t=" + t);
    }
  }

  @Test
  void offIsDarkForTheFirstHalfSecond() {
    // The pre-pattern behaviour: a flashing bulb blinks off during the first half of the cycle.
    assertFalse(TrafficSignalFlashPattern.OFF.isFlashLit(0L));
    assertFalse(TrafficSignalFlashPattern.OFF.isFlashLit(499L));
    assertTrue(TrafficSignalFlashPattern.OFF.isFlashLit(500L));
    assertTrue(TrafficSignalFlashPattern.OFF.isFlashLit(999L));
    assertFalse(TrafficSignalFlashPattern.OFF.isFlashLit(1000L));
  }

  @Test
  void cFiresFivePulsesThenGoesDark() {
    // Five 50 ms pulses inside the first 500 ms, then dark for the remaining 800 ms.
    int pulses = 0;
    boolean previous = false;
    for (long t = 0; t < 1300; t++) {
      boolean lit = TrafficSignalFlashPattern.C.isFlashLit(t);
      if (lit && !previous) {
        pulses++;
      }
      if (t >= 500) {
        assertFalse(lit, "C should be dark after the burst, at t=" + t);
      }
      previous = lit;
    }
    assertEquals(5, pulses);
  }

  @Test
  void cRepeatsOnTheCycleAndHandlesNegativeTime() {
    for (long t = 0; t < 1300; t += 3) {
      assertEquals(TrafficSignalFlashPattern.C.isFlashLit(t),
          TrafficSignalFlashPattern.C.isFlashLit(t + 1300L), "at t=" + t);
    }
    // gameMillis is a wall-clock derived value; a negative one must not invert the pattern.
    assertEquals(TrafficSignalFlashPattern.C.isFlashLit(75L),
        TrafficSignalFlashPattern.C.isFlashLit(75L - 2600L));
  }

  @Test
  void dFiresTheSameBurstOneBurstLater() {
    // D is C delayed by the 500 ms burst length: same five pulses, in the window C leaves dark.
    int pulses = 0;
    boolean previous = false;
    for (long t = 0; t < 1300; t++) {
      boolean lit = TrafficSignalFlashPattern.D.isFlashLit(t);
      if (lit && !previous) {
        pulses++;
      }
      if (t < 500 || t >= 1000) {
        assertFalse(lit, "D should be dark outside its burst, at t=" + t);
      }
      previous = lit;
    }
    assertEquals(5, pulses);
    for (long t = 0; t < 1300; t++) {
      assertEquals(TrafficSignalFlashPattern.C.isFlashLit(t),
          TrafficSignalFlashPattern.D.isFlashLit(t + 500L), "at t=" + t);
    }
  }

  @Test
  void cAndDNeverLightTogether() {
    // The point of the pair: two heads set to C and D alternate rather than strobing in unison.
    boolean anyDLit = false;
    for (long t = -2600; t < 2600; t++) {
      boolean c = TrafficSignalFlashPattern.C.isFlashLit(t);
      boolean d = TrafficSignalFlashPattern.D.isFlashLit(t);
      assertFalse(c && d, "C and D both lit at t=" + t);
      anyDLit |= d;
    }
    assertTrue(anyDLit);
  }

  @Test
  void patternCyclesThroughEveryValueAndWraps() {
    assertEquals(TrafficSignalFlashPattern.B, TrafficSignalFlashPattern.OFF.getNextPattern());
    assertEquals(TrafficSignalFlashPattern.C, TrafficSignalFlashPattern.B.getNextPattern());
    assertEquals(TrafficSignalFlashPattern.D, TrafficSignalFlashPattern.C.getNextPattern());
    assertEquals(TrafficSignalFlashPattern.OFF, TrafficSignalFlashPattern.D.getNextPattern());
  }

  @Test
  void nbtOrdinalsAreStable() {
    // The ordinal is the persisted form: OFF must stay 0 and B must stay 1, or every signal
    // saved with the legacy boolean migrates to the wrong pattern.
    assertEquals(0, TrafficSignalFlashPattern.OFF.toNBT());
    assertEquals(1, TrafficSignalFlashPattern.B.toNBT());
    assertEquals(2, TrafficSignalFlashPattern.C.toNBT());
    assertEquals(3, TrafficSignalFlashPattern.D.toNBT());
    for (TrafficSignalFlashPattern pattern : TrafficSignalFlashPattern.values()) {
      assertEquals(pattern, TrafficSignalFlashPattern.fromNBT(pattern.toNBT()));
    }
    assertEquals(TrafficSignalFlashPattern.OFF, TrafficSignalFlashPattern.fromNBT(-1));
    assertEquals(TrafficSignalFlashPattern.OFF, TrafficSignalFlashPattern.fromNBT(99));
  }
}
