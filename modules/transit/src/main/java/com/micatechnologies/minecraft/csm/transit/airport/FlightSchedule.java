package com.micatechnologies.minecraft.csm.transit.airport;

/**
 * The airport's made-up flight schedule: what the flight information boards list, what the self
 * check-in kiosk prints a boarding pass for and what the boarding pass scanner checks. It is a
 * pure function of the world's clock, so it saves nothing, needs no packets, and every board,
 * kiosk and scanner in the world agrees with every other.
 *
 * <p><b>Time.</b> The schedule runs on the world's time of day, the one the platform clock shows
 * (a day of 24,000 ticks starting at 6:00), so the board's times agree with the clocks and the
 * sun. A game minute is 16.7 ticks, so an airport is busy: a flight an hour on the board goes by
 * in 50 real seconds. A world with the daylight cycle turned off freezes the board.</p>
 *
 * <p><b>Flights.</b> Each list (departures, arrivals) has a flight in every {@link #SLOT_MINUTES}
 * of the day, {@link #SLOTS_PER_DAY} a day, and the same flights every day, as an airline's
 * timetable repeats: a flight is fixed by its slot's place in the day (its "daily" index), which
 * gives it an airline, a number, a city, a gate, a time a few minutes into its slot, and now and
 * then a delay or a cancellation. Everything named is invented: four airlines and sixteen cities
 * with no real counterpart. The names are drawn in {@code gen_transit_airport.py} too (the check-in
 * desks' airline panels), which keeps the same airlines in the same order.</p>
 *
 * <p>A slot is counted from the world's first 6:00, as a {@code long}, so a flight is known by its
 * slot for as long as it is on a board or printed on a pass.</p>
 *
 * @since 2026.9
 */
public final class FlightSchedule {

  /** The invented airlines, in the check-in desk's order (its {@code airline} property). */
  public static final String[] AIRLINES = {"KESTREL AIR", "LANTERN AIRWAYS", "WILLOWJET",
      "SAXTON AIRLINES"};
  /** Each airline's two-letter code, as a flight number starts. */
  public static final String[] CODES = {"KT", "LW", "WJ", "SX"};
  /** The invented cities a flight goes to or comes from. */
  public static final String[] CITIES = {"PORT VESTA", "NEW HALDEN", "CALDER BAY", "EASTMERE",
      "SILVERMOOR", "KINGSREACH", "LAKE ARDEN", "SAN MIREO", "GLENMARROW", "VALCREST",
      "HOLLOW PINES", "WESTORIA", "CAPE LORNE", "RAVENHOLT", "AMBERFIELD", "ST AUBREN"};

  /** The gates flights leave from: A to D, 1 to {@link #GATE_NUMBERS}, as the gate sign's. */
  public static final int GATE_NUMBERS = 20;
  private static final String GATE_LETTERS = "ABCD";

  /** A flight every this many minutes, in each list. */
  public static final int SLOT_MINUTES = 20;
  /** Flights a day in each list. */
  public static final int SLOTS_PER_DAY = 24 * 60 / SLOT_MINUTES;
  /** How late a delayed flight is, in minutes. */
  public static final int DELAY_MINUTES = 30;
  /** How long a departed flight stays on the board, and a landed one. */
  private static final int DEPARTED_SHOWN = 10;
  private static final int LANDED_SHOWN = 20;

  /** What the remarks column says. */
  public enum Status {
    ON_TIME("ON TIME", 0xE8ECF4),
    BOARDING("BOARDING", 0x5CE07A),
    FINAL_CALL("FINAL CALL", 0xFFC830),
    GATE_CLOSED("GATE CLOSED", 0xFF5A4A),
    DEPARTED("DEPARTED", 0x8A94A8),
    DELAYED("DELAYED", 0xFFC830),
    CANCELLED("CANCELLED", 0xFF5A4A),
    LANDING("LANDING", 0xE8ECF4),
    LANDED("LANDED", 0x5CE07A);

    private final String text;
    private final int colour;

    Status(String text, int colour) {
      this.text = text;
      this.colour = colour;
    }

    /**
     * What the board says.
     *
     * @return the remark, in capitals
     */
    public String getText() {
      return text;
    }

    /**
     * The colour the board says it in.
     *
     * @return 0xRRGGBB
     */
    public int getColour() {
      return colour;
    }
  }

  private FlightSchedule() {
  }

  /**
   * The minute a world time falls in, counted from the world's first midnight (tick 0 is 6:00).
   *
   * @param worldTime the world's time, {@code World#getWorldTime()}
   *
   * @return the minute
   */
  public static long minuteOf(long worldTime) {
    return Math.floorDiv(worldTime * 60L, 1000L) + 6 * 60;
  }

  /**
   * A time of day as the board shows it.
   *
   * @param minute a minute from {@link #minuteOf}
   *
   * @return "HH:MM"
   */
  public static String clock(long minute) {
    int m = (int) Math.floorMod(minute, 24L * 60L);
    int h = m / 60;
    m %= 60;
    return (h < 10 ? "0" : "") + h + (m < 10 ? ":0" : ":") + m;
  }

  /** A slot's place in the day, which is all a flight is made from. */
  public static int dailyOf(long slot) {
    return (int) Math.floorMod(slot, (long) SLOTS_PER_DAY);
  }

  private static int hash(int daily, boolean arrivals) {
    int h = daily * 0x9E3779B1 + (arrivals ? 0x7F4A7C15 : 0x2545F491);
    h ^= h >>> 15;
    h *= 0x85EBCA77;
    h ^= h >>> 13;
    h *= 0xC2B2AE3D;
    h ^= h >>> 16;
    return h & 0x7FFFFFFF;
  }

  /**
   * When a flight is scheduled: a few minutes into its slot, on the five.
   *
   * @return the minute, as {@link #minuteOf} counts
   */
  public static long scheduled(long slot, boolean arrivals) {
    return slot * SLOT_MINUTES + 6 * 60 + 5L * (hash(dailyOf(slot), arrivals) & 3);
  }

  /** The flight's airline, an index into {@link #AIRLINES}. */
  public static int airline(long slot, boolean arrivals) {
    return (hash(dailyOf(slot), arrivals) >>> 2) & 3;
  }

  /**
   * The flight number, "KT 214".
   *
   * @return the code and number
   */
  public static String flight(long slot, boolean arrivals) {
    int h = hash(dailyOf(slot), arrivals);
    return CODES[(h >>> 2) & 3] + " " + (100 + (h >>> 4) % 900);
  }

  /** The city the flight goes to (departures) or comes from (arrivals). */
  public static String city(long slot, boolean arrivals) {
    return CITIES[(hash(dailyOf(slot), arrivals) >>> 14) % CITIES.length];
  }

  /**
   * The flight's gate as the gate sign counts them: 1 is A1, 20 A20, 21 B1, up to 80.
   *
   * @return 1 to 80
   */
  public static int gateValue(long slot, boolean arrivals) {
    return 1 + (hash(dailyOf(slot), arrivals) >>> 18) % (GATE_LETTERS.length() * GATE_NUMBERS);
  }

  /**
   * A gate as it is written, "B7".
   *
   * @param value 1 to 80, as {@link #gateValue}
   *
   * @return the gate
   */
  public static String gateName(int value) {
    int v = Math.max(1, Math.min(GATE_LETTERS.length() * GATE_NUMBERS, value)) - 1;
    return GATE_LETTERS.charAt(v / GATE_NUMBERS) + Integer.toString(v % GATE_NUMBERS + 1);
  }

  /** Whether the flight is cancelled: about one in thirty. */
  public static boolean cancelled(long slot, boolean arrivals) {
    return (hash(dailyOf(slot), arrivals) >>> 9) % 31 == 0;
  }

  /** Whether the flight runs {@link #DELAY_MINUTES} late: about one in nine. */
  public static boolean delayed(long slot, boolean arrivals) {
    return !cancelled(slot, arrivals) && (hash(dailyOf(slot), arrivals) >>> 11) % 9 == 0;
  }

  /** When the flight actually goes (or lands): the scheduled time, plus any delay. */
  private static long actual(long slot, boolean arrivals) {
    return scheduled(slot, arrivals) + (delayed(slot, arrivals) ? DELAY_MINUTES : 0);
  }

  /**
   * What the remarks column says of a flight at a minute.
   *
   * @param slot     the flight
   * @param arrivals which list it is in
   * @param now      the minute, as {@link #minuteOf}
   *
   * @return its status
   */
  public static Status status(long slot, boolean arrivals, long now) {
    if (cancelled(slot, arrivals)) {
      return Status.CANCELLED;
    }
    long left = actual(slot, arrivals) - now;
    if (arrivals) {
      if (left < 0) {
        return Status.LANDED;
      }
      if (delayed(slot, arrivals)) {
        return Status.DELAYED;
      }
      return left < 15 ? Status.LANDING : Status.ON_TIME;
    }
    if (left < 0) {
      return Status.DEPARTED;
    }
    if (left < 10) {
      return Status.GATE_CLOSED;
    }
    if (left < 20) {
      return Status.FINAL_CALL;
    }
    if (left < 45) {
      return Status.BOARDING;
    }
    return delayed(slot, arrivals) ? Status.DELAYED : Status.ON_TIME;
  }

  /** Whether the flight is still on the board at a minute. */
  public static boolean shown(long slot, boolean arrivals, long now) {
    long since = now - (cancelled(slot, arrivals) ? scheduled(slot, arrivals)
        : actual(slot, arrivals));
    return since <= (arrivals ? LANDED_SHOWN : DEPARTED_SHOWN);
  }

  /**
   * The flights a board lists at a minute, in scheduled order, after skipping the first few (the
   * pages the monitors before it in a bank show).
   *
   * @param arrivals which list
   * @param now      the minute, as {@link #minuteOf}
   * @param skip     how many listed flights to pass over
   * @param out      filled with the slots of the flights listed
   *
   * @return how many were filled in: always all of {@code out}, since the schedule never ends
   */
  public static int list(boolean arrivals, long now, int skip, long[] out) {
    // the earliest slot still shown: a delayed flight shown after it lands or leaves
    long slot = Math.floorDiv(now - 6 * 60 - DELAY_MINUTES - LANDED_SHOWN - 20, SLOT_MINUTES);
    int n = 0;
    for (int guard = 0; n < out.length && guard < 4096; guard++, slot++) {
      if (!shown(slot, arrivals, now)) {
        continue;
      }
      if (skip > 0) {
        skip--;
        continue;
      }
      out[n++] = slot;
    }
    return n;
  }
}
