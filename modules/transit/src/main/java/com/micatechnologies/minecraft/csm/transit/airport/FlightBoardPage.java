package com.micatechnologies.minecraft.csm.transit.airport;

/**
 * What one flight board's rows show, kept so its renderer can tell when that changes: the flights
 * listed and each one's remark, for the board's page, at the minute last asked about, and a
 * version that moves exactly when any of that (or the renderer's own layout) changes. The
 * renderers compile a board's text into one display list and rebuild it only when the version
 * moves (a flight leaves the page, a remark changes, the page turns, the board is resized), not
 * every game minute, nor every frame.
 *
 * <p>Pure: no GL, no world. One per board, client side, render thread only.</p>
 *
 * @since 2026.10
 */
public final class FlightBoardPage {

  private long[] slots = new long[0];
  private long[] scratch = new long[0];
  private int[] remarks = new int[0];
  private int count;
  private int version;

  /** The inputs last asked about; {@code asked} false until the first call. */
  private boolean asked;
  private boolean lastArrivals;
  private long lastNow;
  private int lastSkip;
  private int lastRows;
  private long lastLayout;

  /**
   * Brings the page up to the minute. Asking about the same minute, list, page and layout again
   * does nothing, so this costs a comparison a frame and a list a game minute.
   *
   * @param arrivals which list the board shows
   * @param now      the minute, as {@link FlightSchedule#minuteOf}
   * @param skip     how many listed flights the pages before this one show
   * @param rows     how many rows the board lists
   * @param layout   anything else the renderer draws the rows from (size, page label), packed;
   *                 a change moves the version
   *
   * @return whether what the board shows changed, and with it the version
   */
  public boolean update(boolean arrivals, long now, int skip, int rows, long layout) {
    if (asked && arrivals == lastArrivals && now == lastNow && skip == lastSkip
        && rows == lastRows && layout == lastLayout) {
      return false;
    }
    boolean changed = !asked || arrivals != lastArrivals || rows != lastRows
        || layout != lastLayout;
    asked = true;
    lastArrivals = arrivals;
    lastNow = now;
    lastSkip = skip;
    lastRows = rows;
    lastLayout = layout;
    if (slots.length != rows) {
      slots = new long[rows];
      scratch = new long[rows];
      remarks = new int[rows];
      changed = true;
    }
    int n = FlightSchedule.list(arrivals, now, skip, scratch);
    if (n != count) {
      changed = true;
    }
    for (int i = 0; i < n; i++) {
      int remark = FlightSchedule.status(scratch[i], arrivals, now).ordinal();
      if (scratch[i] != slots[i] || remark != remarks[i]) {
        changed = true;
        slots[i] = scratch[i];
        remarks[i] = remark;
      }
    }
    count = n;
    if (changed) {
      version++;
    }
    return changed;
  }

  /** How many rows are filled. */
  public int getCount() {
    return count;
  }

  /** The flight in a row. */
  public long getSlot(int row) {
    return slots[row];
  }

  /** The remark in a row, as a {@link FlightSchedule.Status} ordinal. */
  public int getRemark(int row) {
    return remarks[row];
  }

  /** Moves exactly when what the board shows changes; never 0 after the first update. */
  public int getVersion() {
    return version;
  }
}
