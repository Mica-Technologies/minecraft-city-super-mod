package com.micatechnologies.minecraft.csm.healthcare.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A board's page list is keyed on what its text is drawn from: the world's minute, the page and
 * the bed count. These hold every value a board can have to its own key, and a minute's ticks to
 * one key, so the list is rebuilt once a game minute and not every frame.
 */
class EdBoardPageKeyTest {

  @Test
  void everyMinutePageAndBedCountHasItsOwnKey() {
    Set<Long> keys = new HashSet<>();
    // An old world: three billion ticks in.
    long base = EdTrackingSchedule.minuteOf(3_000_000_000L);
    int count = 0;
    for (long now = base; now < base + 30; now++) {
      for (int page = 0; page <= 90; page++) {
        for (int beds = 1; beds <= EdTrackingSchedule.MAX_BEDS; beds++) {
          assertTrue(keys.add(TileEntityEdTrackingBoardRenderer.pageKey(now, page, beds)));
          count++;
        }
      }
    }
    assertEquals(count, keys.size());
  }

  @Test
  void aMinutesTicksShareOneKey() {
    // The first tick of a minute.
    long tick = 3_000_000_000L;
    while (EdTrackingSchedule.minuteOf(tick) == EdTrackingSchedule.minuteOf(tick - 1)) {
      tick++;
    }
    long first = EdTrackingSchedule.minuteOf(tick);
    int sameAsFirst = 0;
    for (long t = tick; t < tick + 40; t++) {
      long key = TileEntityEdTrackingBoardRenderer.pageKey(EdTrackingSchedule.minuteOf(t), 2, 24);
      if (key == TileEntityEdTrackingBoardRenderer.pageKey(first, 2, 24)) {
        sameAsFirst++;
      }
    }
    // A game minute is 1000 / 60 ticks: sixteen or seventeen of them.
    assertTrue(sameAsFirst >= 16 && sameAsFirst <= 17, "ticks in the first minute: " + sameAsFirst);
    assertNotEquals(TileEntityEdTrackingBoardRenderer.pageKey(first, 2, 24),
        TileEntityEdTrackingBoardRenderer.pageKey(first + 1, 2, 24));
  }
}
