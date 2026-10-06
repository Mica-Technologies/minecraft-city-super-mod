package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IncandescentFadeTest {

  @Test
  void riseReachesNinetyPercentNearMeasuredLampTimes() {
    assertTrue(IncandescentFade.rise(0f, 100) < 0.9f, "too quick to be a filament");
    assertTrue(IncandescentFade.rise(0f, 130) >= 0.9f, "too slow for a 120 V signal lamp");
  }

  @Test
  void decayIsSlowerThanRise() {
    for (long t = 20; t <= 300; t += 20) {
      assertTrue(IncandescentFade.decay(1f, t) > 1f - IncandescentFade.rise(0f, t),
          "a cooling lamp should still be brighter than a heating one is dark at " + t + " ms");
    }
  }

  @Test
  void firstFrameIsSettled() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b001, 3, 1000);
    assertEquals(1f, tracker.brightness(0, true, 1000));
    assertEquals(0f, tracker.brightness(1, false, 1000));
  }

  @Test
  void switchingOnFadesInThenSettles() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b000, 3, 1000);
    tracker.observe(0b100, 3, 1016);
    float early = tracker.brightness(2, true, 1016 + 30);
    assertTrue(early > 0f && early < 1f, "mid-fade, was " + early);
    assertEquals(1f, tracker.brightness(2, true, 1016 + 1000));
  }

  @Test
  void switchingOffFadesOutThenSettles() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b001, 3, 1000);
    tracker.observe(0b000, 3, 1016);
    float early = tracker.brightness(0, false, 1016 + 50);
    assertTrue(early > 0f && early < 1f, "mid-fade, was " + early);
    assertEquals(0f, tracker.brightness(0, false, 1016 + 1000));
  }

  @Test
  void switchingBackMidFadeStartsFromCurrentLevel() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b000, 1, 1000);
    tracker.observe(0b001, 1, 1016);
    float atReversal = tracker.brightness(0, true, 1056);
    tracker.observe(0b000, 1, 1056);
    assertEquals(atReversal, tracker.brightness(0, false, 1056), 1e-4f);
  }

  @Test
  void changeSeenAfterAGapIsNotFaded() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b000, 1, 1000);
    tracker.observe(0b001, 1, 1000 + IncandescentFade.STALE_MILLIS + 1);
    assertEquals(1f, tracker.brightness(0, true, 1000 + IncandescentFade.STALE_MILLIS + 1));
  }

  @Test
  void sectionCountChangeResets() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b001, 3, 1000);
    tracker.observe(0b011, 4, 1016);
    assertEquals(1f, tracker.brightness(1, true, 1016));
  }
}
