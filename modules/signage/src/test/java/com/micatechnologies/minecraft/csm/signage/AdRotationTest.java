package com.micatechnologies.minecraft.csm.signage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdRotationTest {

  private static final List<String> POOL = Arrays.asList("a", "b", "c", "d", "e");

  @Test
  void aSingleAdNeverChanges() {
    for (long t = 0; t < 100_000; t += 997) {
      assertEquals(0, AdRotation.SINGLE.select(POOL, t, 5, 42));
    }
  }

  @Test
  void inOrderStepsOnceAnInterval() {
    int first = AdRotation.ALL_IN_ORDER.select(POOL, 0, 10, 0);
    assertEquals(first, AdRotation.ALL_IN_ORDER.select(POOL, 199, 10, 0));
    assertEquals((first + 1) % 5, AdRotation.ALL_IN_ORDER.select(POOL, 200, 10, 0));
    // The seed offsets the start, so boards side by side are not in step.
    assertNotEquals(AdRotation.ALL_IN_ORDER.select(POOL, 0, 10, 0),
        AdRotation.ALL_IN_ORDER.select(POOL, 0, 10, 1));
  }

  @Test
  void aShuffledRoundShowsEveryAdOnce() {
    int interval = 5;
    long ticksPerAd = interval * 20L;
    for (long round = 0; round < 4; round++) {
      Set<Integer> seen = new HashSet<>();
      for (int i = 0; i < POOL.size(); i++) {
        long t = (round * POOL.size() + i) * ticksPerAd;
        seen.add(AdRotation.ALL_SHUFFLED.select(POOL, t, interval, 7));
      }
      assertEquals(POOL.size(), seen.size(), "round " + round + " repeated an ad");
    }
  }

  @Test
  void theShuffleIsTheSameEverywhere() {
    assertArrayEquals(AdRotation.shuffle(10, 12345L), AdRotation.shuffle(10, 12345L));
    List<Integer> sorted = new ArrayList<>();
    for (int i : AdRotation.shuffle(10, 99L)) {
      sorted.add(i);
    }
    sorted.sort(null);
    for (int i = 0; i < 10; i++) {
      assertEquals(i, (int) sorted.get(i));
    }
  }

  @Test
  void intervalsAreClampedAndBadOrdinalsAreSafe() {
    assertEquals(AdRotation.MIN_INTERVAL, AdRotation.clampInterval(0));
    assertEquals(AdRotation.MAX_INTERVAL, AdRotation.clampInterval(100_000));
    assertEquals(AdRotation.SINGLE, AdRotation.fromOrdinal(-1));
    assertEquals(AdLight.UNLIT, AdLight.fromOrdinal(42));
    assertEquals(AdBoardAlign.CENTRE, AdBoardAlign.fromOrdinal(9));
  }

  @Test
  void theControllerColumnFollowsTheAlignment() {
    assertEquals(0, AdBoardAlign.LEFT.controllerColumn(15));
    assertEquals(14, AdBoardAlign.RIGHT.controllerColumn(15));
    assertEquals(7, AdBoardAlign.CENTRE.controllerColumn(15));
    assertEquals(1, AdBoardAlign.CENTRE.controllerColumn(4));
    assertEquals(0, AdBoardAlign.CENTRE.controllerColumn(1));
  }

  @Test
  void aTransitionEasesFromZeroToOneAndThenStays() {
    assertEquals(0.0, AdTransition.progress(0), 1e-9);
    assertEquals(0.5, AdTransition.progress(AdTransition.TICKS / 2.0), 1e-9);
    assertEquals(1.0, AdTransition.progress(AdTransition.TICKS), 1e-9);
    assertEquals(1.0, AdTransition.progress(5000), 1e-9);
    assertTrue(AdTransition.progress(2) < AdTransition.progress(3));
    assertEquals(AdTransition.CUT, AdTransition.fromOrdinal(7));
  }

  @Test
  void lightFollowsItsSetting() {
    assertFalse(AdLight.UNLIT.isLit(0.2F, true));
    assertTrue(AdLight.LIT.isLit(1F, false));
    assertTrue(AdLight.NIGHT.isLit(0.2F, false));
    assertFalse(AdLight.NIGHT.isLit(1F, false));
    assertTrue(AdLight.REDSTONE.isLit(1F, true));
    assertFalse(AdLight.REDSTONE.isLit(0.2F, false));
  }
}
