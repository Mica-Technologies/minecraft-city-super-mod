package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Pure-function tests for ADVANCED-mode actuated/volume-density timing math. */
class AdvancedActuationTimingTest {

  @Nested
  @DisplayName("addedInitial (volume-density)")
  class AddedInitial {
    @Test
    @DisplayName("disabled (perVehicle 0) or empty queue -> 0")
    void disabled() {
      assertEquals(0L, AdvancedActuationTiming.addedInitial(5, 0, 0));
      assertEquals(0L, AdvancedActuationTiming.addedInitial(0, 20, 0));
    }

    @Test
    @DisplayName("queue * perVehicle, capped at maxInitial")
    void scalesAndCaps() {
      assertEquals(60L, AdvancedActuationTiming.addedInitial(3, 20, 0));
      assertEquals(40L, AdvancedActuationTiming.addedInitial(3, 20, 40));
      assertEquals(60L, AdvancedActuationTiming.addedInitial(3, 20, 100));
    }
  }

  @Nested
  @DisplayName("effectiveMinGreen")
  class EffMinGreen {
    @Test
    @DisplayName("longest of min green, added initial, and (on bike call) bike min green")
    void longestOf() {
      assertEquals(100L, AdvancedActuationTiming.effectiveMinGreen(100, 40, 0, false));
      assertEquals(140L, AdvancedActuationTiming.effectiveMinGreen(100, 140, 0, false));
      assertEquals(200L, AdvancedActuationTiming.effectiveMinGreen(100, 40, 200, true));
      assertEquals(100L, AdvancedActuationTiming.effectiveMinGreen(100, 40, 200, false),
          "bike min green ignored without a bike call");
    }
  }

  @Nested
  @DisplayName("effectiveMaxGreen (Max 2 selection)")
  class EffMaxGreen {
    @Test
    @DisplayName("max2 only when selected and configured")
    void selection() {
      assertEquals(600L, AdvancedActuationTiming.effectiveMaxGreen(600, 300, false));
      assertEquals(300L, AdvancedActuationTiming.effectiveMaxGreen(600, 300, true));
      assertEquals(600L, AdvancedActuationTiming.effectiveMaxGreen(600, 0, true),
          "max2 of 0 falls back to max1");
    }
  }

  @Nested
  @DisplayName("effectivePassage (gap reduction)")
  class EffPassage {
    @Test
    @DisplayName("disabled cases return the full passage")
    void disabled() {
      assertEquals(40L, AdvancedActuationTiming.effectivePassage(40, 10, 0, 0, 500));
      assertEquals(40L, AdvancedActuationTiming.effectivePassage(40, 0, 0, 100, 500));
      assertEquals(40L, AdvancedActuationTiming.effectivePassage(40, 50, 0, 100, 500),
          "min gap >= passage is a no-op");
    }

    @Test
    @DisplayName("full before the reduce delay, min gap after the ramp, linear between")
    void ramp() {
      // passage 40 -> min gap 10 over 100 ticks, starting after 20 ticks of green.
      assertEquals(40L, AdvancedActuationTiming.effectivePassage(40, 10, 20, 100, 10));
      assertEquals(40L, AdvancedActuationTiming.effectivePassage(40, 10, 20, 100, 20));
      assertEquals(10L, AdvancedActuationTiming.effectivePassage(40, 10, 20, 100, 200));
      // halfway through the ramp (50 of 100 into it): 40 - 30*0.5 = 25.
      long mid = AdvancedActuationTiming.effectivePassage(40, 10, 20, 100, 70);
      assertTrue(mid > 10 && mid < 40, "midpoint should be partway reduced, got " + mid);
      assertEquals(25L, mid);
    }
  }
}
