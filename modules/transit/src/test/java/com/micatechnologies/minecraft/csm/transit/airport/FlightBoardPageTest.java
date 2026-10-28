package com.micatechnologies.minecraft.csm.transit.airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A flight board's text list is compiled again exactly when {@link FlightBoardPage}'s version
 * moves, so the version must move whenever what the board shows changes (or the board would
 * show a stale row) and only then (or every board would recompile every game minute). Checked
 * against {@link FlightSchedule} itself over two game days, minute by minute.
 */
class FlightBoardPageTest {

  /** What a board shows at a minute, straight from the schedule: slots, then remark ordinals. */
  private static long[] shown(boolean arrivals, long now, int skip, int rows) {
    long[] slots = new long[rows];
    int n = FlightSchedule.list(arrivals, now, skip, slots);
    long[] out = new long[2 * n];
    for (int i = 0; i < n; i++) {
      out[i] = slots[i];
      out[n + i] = FlightSchedule.status(slots[i], arrivals, now).ordinal();
    }
    return out;
  }

  private static long[] read(FlightBoardPage page) {
    int n = page.getCount();
    long[] out = new long[2 * n];
    for (int i = 0; i < n; i++) {
      out[i] = page.getSlot(i);
      out[n + i] = page.getRemark(i);
    }
    return out;
  }

  @ParameterizedTest
  @CsvSource({"false, 0, 7", "true, 0, 7", "false, 14, 7", "false, 0, 24", "true, 24, 24",
      "false, 0, 1"})
  void versionMovesExactlyWhenTheRowsChange(boolean arrivals, int skip, int rows) {
    FlightBoardPage page = new FlightBoardPage();
    long start = FlightSchedule.minuteOf(0);
    assertTrue(page.update(arrivals, start, skip, rows, 0L), "the first update is news");
    assertEquals(1, page.getVersion());
    long[] before = shown(arrivals, start, skip, rows);
    int changes = 0;
    for (long now = start + 1; now < start + 2 * 24 * 60; now++) {
      long[] after = shown(arrivals, now, skip, rows);
      boolean differs = !Arrays.equals(before, after);
      int version = page.getVersion();
      assertEquals(differs, page.update(arrivals, now, skip, rows, 0L), "minute " + now);
      assertEquals(differs ? version + 1 : version, page.getVersion(), "minute " + now);
      assertTrue(Arrays.equals(after, read(page)), "minute " + now);
      // the same minute again, as every frame within it asks
      assertFalse(page.update(arrivals, now, skip, rows, 0L));
      if (differs) {
        changes++;
      }
      before = after;
    }
    // The point of the list: it is not rebuilt every game minute.
    assertTrue(changes > 0 && changes < 2 * 24 * 60 / 2, changes + " changes in two days");
  }

  @Test
  void turningThePageOrResizingIsNews() {
    FlightBoardPage page = new FlightBoardPage();
    long now = FlightSchedule.minuteOf(6000);
    page.update(false, now, 0, 12, 1L);
    assertTrue(page.update(false, now, 12, 12, 1L), "next page");
    // the same rows under a different layout (the page label, the board's size)
    assertTrue(page.update(false, now, 12, 12, 2L), "layout");
    assertTrue(page.update(false, now, 12, 8, 2L), "fewer rows");
    assertTrue(page.update(true, now, 12, 8, 2L), "arrivals");
    assertFalse(page.update(true, now, 12, 8, 2L));
  }

  @Test
  void theLargeBoardsLayoutKeyTellsItsInputsApart() {
    long a = TileEntityFlightBoardLargeRenderer.layoutKey(4, 2, 0, 3);
    assertFalse(a == TileEntityFlightBoardLargeRenderer.layoutKey(4, 2, 1, 3));
    assertFalse(a == TileEntityFlightBoardLargeRenderer.layoutKey(2, 4, 0, 3));
    assertFalse(a == TileEntityFlightBoardLargeRenderer.layoutKey(4, 2, 0, 2));
    assertEquals(a, TileEntityFlightBoardLargeRenderer.layoutKey(4, 2, 0, 3));
  }
}
