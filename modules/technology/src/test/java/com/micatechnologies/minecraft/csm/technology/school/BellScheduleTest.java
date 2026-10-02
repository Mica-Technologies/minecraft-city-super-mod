package com.micatechnologies.minecraft.csm.technology.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BellScheduleTest {

  private static BellSchedule.Entry bell(String time) {
    return new BellSchedule.Entry(BellSchedule.parseTime(time), BellTone.BELL, "", true);
  }

  @Test
  @DisplayName("times read as typed, 24-hour, with or without a colon")
  void parsesTimes() {
    assertEquals(8 * 60 + 50, BellSchedule.parseTime("8:50"));
    assertEquals(8 * 60 + 50, BellSchedule.parseTime("08:50"));
    assertEquals(8 * 60 + 50, BellSchedule.parseTime(" 8.50 "));
    assertEquals(8 * 60 + 50, BellSchedule.parseTime("850"));
    assertEquals(13 * 60 + 5, BellSchedule.parseTime("1305"));
    assertEquals(0, BellSchedule.parseTime("0:00"));
    assertEquals(23 * 60 + 59, BellSchedule.parseTime("23:59"));
  }

  @Test
  @DisplayName("what is not a time reads as -1")
  void rejectsNonsense() {
    for (String bad : new String[]{null, "", "24:00", "8:60", "8:5", "abc", "8:5a", "123456",
        ":30", "-1:00", "12345"}) {
      assertEquals(-1, BellSchedule.parseTime(bad), String.valueOf(bad));
    }
  }

  @Test
  @DisplayName("formatting and parsing agree for every minute of the day")
  void formatRoundTrips() {
    for (int m = 0; m < ClockTime.MINUTES_PER_DAY; m++) {
      assertEquals(m, BellSchedule.parseTime(ClockTime.format(m)));
    }
  }

  @Test
  @DisplayName("a bell rings when the clock passes its minute, not before or after")
  void crossedOneMinute() {
    int bell = BellSchedule.parseTime("8:50");
    assertTrue(BellSchedule.crossed(bell, bell - 1, bell));
    assertFalse(BellSchedule.crossed(bell, bell, bell + 1));
    assertFalse(BellSchedule.crossed(bell, bell - 2, bell - 1));
    assertFalse(BellSchedule.crossed(bell, bell, bell), "no time passed");
  }

  @Test
  @DisplayName("a short lag spike is caught up, a time jump is not")
  void catchUpLimit() {
    int bell = 600;
    assertTrue(BellSchedule.crossed(bell, bell - 2, bell + 1));
    assertTrue(BellSchedule.crossed(bell, bell - 1, bell - 1 + BellSchedule.MAX_CATCH_UP_MINUTES));
    assertFalse(BellSchedule.crossed(bell, bell - 1,
        bell + BellSchedule.MAX_CATCH_UP_MINUTES), "a jump wider than the catch-up");
    assertFalse(BellSchedule.crossed(bell, 0, 1000), "/time set across the bell");
    assertFalse(BellSchedule.crossed(bell, bell + 5, bell - 5), "time set backwards");
  }

  @Test
  @DisplayName("a midnight bell rings as the day turns over")
  void crossesMidnight() {
    assertTrue(BellSchedule.crossed(0, 1439, 0));
    assertTrue(BellSchedule.crossed(1, 1438, 1));
    assertFalse(BellSchedule.crossed(1439, 1439, 0));
  }

  @Test
  @DisplayName("due picks the earliest enabled bell a check passed")
  void dueEarliestEnabled() {
    BellSchedule.Entry off = new BellSchedule.Entry(500, BellTone.CHIME, "", false);
    BellSchedule.Entry first = new BellSchedule.Entry(501, BellTone.TONE, "", true);
    BellSchedule.Entry second = new BellSchedule.Entry(502, BellTone.BELL, "", true);
    BellSchedule s = new BellSchedule(Arrays.asList(second, off, first));
    assertSame(first, s.due(499, 502));
    assertSame(second, s.due(501, 502));
    assertNull(s.due(502, 503));
    assertNull(s.due(498, 500), "the only bell passed is off");
  }

  @Test
  @DisplayName("next finds the following bell, round past midnight, never the one now")
  void nextBell() {
    BellSchedule s = new BellSchedule(Arrays.asList(bell("8:00"), bell("8:50"), bell("15:10")));
    assertEquals(BellSchedule.parseTime("8:50"), s.next(BellSchedule.parseTime("8:00")).getMinute());
    assertEquals(BellSchedule.parseTime("8:00"), s.next(BellSchedule.parseTime("20:00")).getMinute());
    assertEquals(BellSchedule.parseTime("15:10"), s.next(BellSchedule.parseTime("9:00")).getMinute());
    BellSchedule single = new BellSchedule(Arrays.asList(bell("8:00")));
    assertEquals(480, single.next(480).getMinute(), "a lone bell is next tomorrow");
    assertNull(new BellSchedule(Arrays.asList(
        new BellSchedule.Entry(480, BellTone.BELL, "", false))).next(0));
    assertNull(BellSchedule.EMPTY.next(0));
  }

  @Test
  @DisplayName("bells are kept sorted and at most twelve")
  void sortedAndBounded() {
    List<BellSchedule.Entry> many = new ArrayList<>();
    for (int i = 20; i > 0; i--) {
      many.add(new BellSchedule.Entry(i * 10, BellTone.BELL, "", true));
    }
    BellSchedule s = new BellSchedule(many);
    assertEquals(BellSchedule.MAX_ENTRIES, s.getEntries().size());
    for (int i = 1; i < s.getEntries().size(); i++) {
      assertTrue(s.getEntries().get(i - 1).getMinute() <= s.getEntries().get(i).getMinute());
    }
  }

  @Test
  @DisplayName("announcement text loses formatting codes and control characters, and is cut")
  void cleansText() {
    assertEquals("Lunch now", BellSchedule.cleanText("  §cLunch\u0007 now "));
    StringBuilder longText = new StringBuilder();
    for (int i = 0; i < 300; i++) {
      longText.append('a');
    }
    assertEquals(BellSchedule.MAX_TEXT, BellSchedule.cleanText(longText.toString()).length());
    assertEquals("", BellSchedule.cleanText(null));
    BellSchedule.Entry e = new BellSchedule.Entry(-1, null, null, true);
    assertEquals(1439, e.getMinute(), "minutes are taken modulo a day");
    assertNotNull(e.getTone());
  }

  @Test
  @DisplayName("tones from the wire fall back to the bell when out of range")
  void toneOrdinals() {
    assertSame(BellTone.BELL, BellTone.byOrdinal(-1));
    assertSame(BellTone.BELL, BellTone.byOrdinal(99));
    for (BellTone t : BellTone.values()) {
      assertSame(t, BellTone.byOrdinal(t.ordinal()));
    }
    assertTrue(BellTone.BELL.ringsBells());
    assertFalse(BellTone.CHIME.ringsBells());
    assertTrue(BellTone.ANNOUNCE.speaks());
  }
}
