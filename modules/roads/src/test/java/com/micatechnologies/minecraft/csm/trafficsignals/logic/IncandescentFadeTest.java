package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IncandescentFadeTest {

  private static final TrafficSignalBulbColor RED = TrafficSignalBulbColor.RED;
  private static final TrafficSignalBulbColor GREEN = TrafficSignalBulbColor.GREEN;

  /** The light (not the display level) a lamp heating from cold shows through red. */
  private static double redLightRising(long millis) {
    return Math.pow(IncandescentFade.displayLevel(
        IncandescentFade.heat(IncandescentFade.AMBIENT, millis), RED), 2.2);
  }

  private static float displayCooling(long millis, TrafficSignalBulbColor colour) {
    return IncandescentFade.displayLevel(IncandescentFade.cool(1.0f, millis), colour);
  }

  @Test
  void nothingShowsWhileTheFilamentIsBelowRedHeat() {
    assertTrue(IncandescentFade.displayLevel(
            IncandescentFade.heat(IncandescentFade.AMBIENT, 15), RED) < 0.05f,
        "a cold filament should not glow in its first frame");
  }

  @Test
  void redReachesNinetyPercentAtTheConfiguredTime() {
    float rise = IncandescentFade.DEFAULT_RISE_90_MILLIS;
    assertTrue(redLightRising((long) (rise - 8)) < 0.9, "too quick");
    assertTrue(redLightRising((long) (rise + 8)) >= 0.9, "too slow");
  }

  @Test
  void switchedOffTheLightHalvesAtOnceThenGlows() {
    assertTrue(Math.pow(displayCooling(20, RED), 2.2) <= 0.5,
        "the light should halve within a frame or so");
    assertTrue(displayCooling(150, RED) > 0.02f, "the dim glow should linger");
  }

  @Test
  void greenGoesDarkBeforeRed() {
    for (long t = 10; t <= 150; t += 20) {
      assertTrue(displayCooling(t, GREEN) < displayCooling(t, RED), "at " + t + " ms");
    }
  }

  @Test
  void heatingAndCoolingAreMonotonic() {
    float previous = IncandescentFade.AMBIENT;
    for (long t = 0; t <= 400; t += 5) {
      float theta = IncandescentFade.heat(IncandescentFade.AMBIENT, t);
      assertTrue(theta >= previous, "heating at " + t);
      previous = theta;
    }
    previous = 1.0f;
    for (long t = 0; t <= 400; t += 5) {
      float theta = IncandescentFade.cool(1.0f, t);
      assertTrue(theta <= previous, "cooling at " + t);
      previous = theta;
    }
  }

  @Test
  void firstFrameIsSettled() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b001, 3, 1000);
    assertEquals(1f, tracker.brightness(0, true, 1000, RED));
    assertEquals(0f, tracker.brightness(1, false, 1000, RED));
  }

  @Test
  void switchingOnFadesInThenSettles() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b000, 3, 1000);
    tracker.observe(0b100, 3, 1016);
    float early = tracker.brightness(2, true, 1016 + 60, RED);
    assertTrue(early > 0f && early < 1f, "mid-fade, was " + early);
    assertEquals(1f, tracker.brightness(2, true, 1016 + 1000, RED));
  }

  @Test
  void switchingOffFadesOutThenSettles() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b001, 3, 1000);
    tracker.observe(0b000, 3, 1016);
    float early = tracker.brightness(0, false, 1016 + 50, RED);
    assertTrue(early > 0f && early < 1f, "mid-fade, was " + early);
    assertEquals(0f, tracker.brightness(0, false, 1016 + 1000, RED));
  }

  @Test
  void switchingBackMidFadeCarriesOnFromTheSameTemperature() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b000, 1, 1000);
    tracker.observe(0b001, 1, 1016);
    float atReversal = tracker.brightness(0, true, 1076, RED);
    tracker.observe(0b000, 1, 1076);
    assertEquals(atReversal, tracker.brightness(0, false, 1076, RED), 1e-3f);

    float coolingAt = tracker.brightness(0, false, 1100, RED);
    tracker.observe(0b001, 1, 1100);
    assertEquals(coolingAt, tracker.brightness(0, true, 1100, RED), 1e-3f);
  }

  @Test
  void changeSeenAfterAGapIsNotFaded() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b000, 1, 1000);
    tracker.observe(0b001, 1, 1000 + IncandescentFade.STALE_MILLIS + 1);
    assertEquals(1f, tracker.brightness(0, true, 1000 + IncandescentFade.STALE_MILLIS + 1, RED));
  }

  @Test
  void sectionCountChangeResets() {
    IncandescentFade.Tracker tracker = new IncandescentFade.Tracker();
    tracker.observe(0b001, 3, 1000);
    tracker.observe(0b011, 4, 1016);
    assertEquals(1f, tracker.brightness(1, true, 1016, RED));
  }
}
