package com.micatechnologies.minecraft.csm.technology.school;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A bell schedule: up to {@link #MAX_ENTRIES} times of the game day, each with its tone, its
 * announcement text and whether it is on. Days are game days and every day is the same: there is
 * no weekday in Minecraft to set a weekend by.
 *
 * <p>Immutable, sorted by time, and free of Minecraft so the parsing and the due-bell logic can be
 * tested on their own. {@link TileEntityBellController} holds one and asks it, once each game
 * minute, which bell (if any) fell due since the minute it last looked.</p>
 *
 * @since 2026.10
 */
public final class BellSchedule {

  /** Bell times a controller holds. */
  public static final int MAX_ENTRIES = 12;

  /** Characters of announcement text a bell holds. */
  public static final int MAX_TEXT = 120;

  /**
   * Game minutes a check may span and still ring what it passed over. A minute is under a second
   * and the controller looks twice a minute, so a normal gap is one; a gap of a few is a lag
   * spike, worth catching up. Anything wider is the time being set, which should not ring every
   * bell it jumped over.
   */
  public static final int MAX_CATCH_UP_MINUTES = 3;

  /** The empty schedule. */
  public static final BellSchedule EMPTY = new BellSchedule(Collections.emptyList());

  /**
   * One bell.
   */
  public static final class Entry {

    private final int minute;
    private final BellTone tone;
    private final String text;
    private final boolean enabled;

    /**
     * Constructs a bell.
     *
     * @param minute  its minute of the day, 0 to 1,439 (taken modulo a day)
     * @param tone    its tone
     * @param text    its announcement text (cleaned and cut to {@link #MAX_TEXT})
     * @param enabled whether it rings
     */
    public Entry(int minute, BellTone tone, @Nullable String text, boolean enabled) {
      this.minute = Math.floorMod(minute, ClockTime.MINUTES_PER_DAY);
      this.tone = tone == null ? BellTone.BELL : tone;
      this.text = cleanText(text);
      this.enabled = enabled;
    }

    public int getMinute() {
      return minute;
    }

    public BellTone getTone() {
      return tone;
    }

    public String getText() {
      return text;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  private final List<Entry> entries;

  /**
   * Constructs a schedule from bells in any order; it keeps the first {@link #MAX_ENTRIES},
   * sorted by time.
   *
   * @param entries the bells
   */
  public BellSchedule(List<Entry> entries) {
    List<Entry> copy = new ArrayList<>();
    for (Entry e : entries) {
      if (e != null && copy.size() < MAX_ENTRIES) {
        copy.add(e);
      }
    }
    copy.sort(Comparator.comparingInt(Entry::getMinute));
    this.entries = Collections.unmodifiableList(copy);
  }

  /** The bells, sorted by time. */
  public List<Entry> getEntries() {
    return entries;
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }

  /**
   * Whether a bell at {@code minute} fell due between two checks: after {@code lastMinute} and
   * up to and including {@code nowMinute}, across midnight if need be, and only when the two are
   * at most {@link #MAX_CATCH_UP_MINUTES} apart.
   *
   * @param minute     the bell's minute of the day
   * @param lastMinute the minute of the previous check
   * @param nowMinute  the minute of this check
   *
   * @return whether it rings now
   */
  public static boolean crossed(int minute, int lastMinute, int nowMinute) {
    int gap = minutesUntil(lastMinute, nowMinute);
    if (gap == 0 || gap > MAX_CATCH_UP_MINUTES) {
      return false;
    }
    int since = minutesUntil(lastMinute, minute);
    return since >= 1 && since <= gap;
  }

  /**
   * The first enabled bell due between two checks (see {@link #crossed}), or {@code null}. Two
   * bells caught by one late check ring once, as the earlier.
   *
   * @param lastMinute the minute of the previous check
   * @param nowMinute  the minute of this check
   *
   * @return the bell to ring, or {@code null}
   */
  @Nullable
  public Entry due(int lastMinute, int nowMinute) {
    Entry best = null;
    int bestSince = Integer.MAX_VALUE;
    for (Entry e : entries) {
      if (e.isEnabled() && crossed(e.getMinute(), lastMinute, nowMinute)) {
        int since = minutesUntil(lastMinute, e.getMinute());
        if (since < bestSince) {
          best = e;
          bestSince = since;
        }
      }
    }
    return best;
  }

  /**
   * The next enabled bell after {@code nowMinute} (not at it), round past midnight, or
   * {@code null} when none is on.
   *
   * @param nowMinute the minute of the day now
   *
   * @return the next bell, or {@code null}
   */
  @Nullable
  public Entry next(int nowMinute) {
    Entry best = null;
    int bestWait = Integer.MAX_VALUE;
    for (Entry e : entries) {
      if (!e.isEnabled()) {
        continue;
      }
      int wait = minutesUntil(nowMinute, e.getMinute());
      if (wait == 0) {
        wait = ClockTime.MINUTES_PER_DAY;
      }
      if (wait < bestWait) {
        best = e;
        bestWait = wait;
      }
    }
    return best;
  }

  /**
   * Minutes from one minute of the day to the next time the clock reads another, 0 to 1,439.
   *
   * @param from the minute now
   * @param to   the minute wanted
   *
   * @return minutes to wait
   */
  public static int minutesUntil(int from, int to) {
    return Math.floorMod(to - from, ClockTime.MINUTES_PER_DAY);
  }

  /**
   * Reads a 24-hour time as typed: {@code 8:50}, {@code 08:50}, {@code 8.50}, {@code 0850} or
   * {@code 850}, with spaces round it ignored.
   *
   * @param text what was typed
   *
   * @return the minute of the day, or -1 if it is not a time
   */
  public static int parseTime(@Nullable String text) {
    if (text == null) {
      return -1;
    }
    String t = text.trim();
    if (t.isEmpty() || t.length() > 5) {
      return -1;
    }
    String hours;
    String minutes;
    int sep = Math.max(t.indexOf(':'), t.indexOf('.'));
    if (sep >= 0) {
      hours = t.substring(0, sep);
      minutes = t.substring(sep + 1);
    } else if (t.length() == 3 || t.length() == 4) {
      hours = t.substring(0, t.length() - 2);
      minutes = t.substring(t.length() - 2);
    } else {
      return -1;
    }
    if (hours.isEmpty() || hours.length() > 2 || minutes.length() != 2
        || !digits(hours) || !digits(minutes)) {
      return -1;
    }
    int h = Integer.parseInt(hours);
    int m = Integer.parseInt(minutes);
    if (h > 23 || m > 59) {
      return -1;
    }
    return h * 60 + m;
  }

  private static boolean digits(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  /**
   * Announcement text as it is kept: no formatting codes or control characters, trimmed, at
   * most {@link #MAX_TEXT} characters.
   *
   * @param text the text given
   *
   * @return the text kept, possibly empty
   */
  public static String cleanText(@Nullable String text) {
    if (text == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(Math.min(text.length(), MAX_TEXT));
    for (int i = 0; i < text.length() && sb.length() < MAX_TEXT; i++) {
      char c = text.charAt(i);
      if (c == '§') {
        i++;
        continue;
      }
      if (c >= ' ' && c != '\u007f') {
        sb.append(c);
      }
    }
    return sb.toString().trim();
  }
}
