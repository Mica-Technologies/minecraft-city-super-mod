package com.micatechnologies.minecraft.csm.healthcare.clinical;

/**
 * The emergency department's made-up patient list, which the ED tracking board shows: a pure
 * function of the world's clock and the board's seed, so it saves nothing, sends no packets and
 * costs the server nothing (the way the Transit flight boards' schedule works).
 *
 * <p><b>Time.</b> The world's time of day as the Transit boards read it ({@link #minuteOf}): a
 * day of 24,000 ticks from 6:00, a game minute 16.7 ticks.</p>
 *
 * <p><b>Beds.</b> Each bed runs a visit every {@link #PERIOD} minutes, offset by a hash of the bed
 * and the seed. A visit lasts 200 to 439 minutes and goes TRIAGE, WAITING, IN ROOM, LAB or
 * IMAGING, IN ROOM, then ADMIT or DISCHARGE; the bed is then CLEANING for
 * {@link #CLEANING_MINUTES} and OPEN until the next visit. A patient is shown by initials only,
 * from a table of {@link #PATIENTS} invented pairs, and every four beds have a nurse (initials)
 * for each twelve-hour shift. Nothing names a real person.</p>
 *
 * <p>Every value the renderer keys a display list by is bounded: a bed's label
 * ({@link #MAX_BEDS}), a patient ({@link #PATIENTS}), a nurse ({@link #NURSES}), a status, an
 * elapsed time below {@link #PERIOD}, and the minute of the day.</p>
 *
 * @since 2026.10
 */
public final class EdTrackingSchedule {

  /** The most beds a board lists. */
  public static final int MAX_BEDS = 40;
  /** The beds a board lists unless its NBT says otherwise. */
  public static final int DEFAULT_BEDS = 24;
  /** A visit begins in each bed every this many minutes. */
  public static final int PERIOD = 480;
  /** How long a bed is cleaned after a visit. */
  public static final int CLEANING_MINUTES = 20;
  /** Invented initials. */
  public static final int PATIENTS = 64;
  /** Invented nurses' initials. */
  public static final int NURSES = 12;

  private static final String FIRST = "ABCDEFGHJKLMNPRS";
  private static final String LAST = "BCDFGHKLMNPRSTWY";
  private static final String[] NURSE = {"AK", "BT", "CR", "DM", "EL", "FS", "GH", "JW", "KO",
      "LP", "MN", "RV"};

  /** What a bed's status column says, and its colour. */
  public enum Status {
    TRIAGE("TRIAGE", 0xFFC830),
    WAITING("WAITING", 0xE8ECF4),
    IN_ROOM("IN ROOM", 0x5CE07A),
    LAB("LAB", 0x58D0F0),
    IMAGING("IMAGING", 0xC890F0),
    ADMIT("ADMIT", 0x6FA8FF),
    DISCHARGE("DISCHARGE", 0x9CF0B0),
    CLEANING("CLEANING", 0x8A94A8),
    OPEN("OPEN", 0x5A6478);

    private final String text;
    private final int colour;

    Status(String text, int colour) {
      this.text = text;
      this.colour = colour;
    }

    public String getText() {
      return text;
    }

    public int getColour() {
      return colour;
    }

    /** Whether a patient is in the bed (the patient and time columns are shown). */
    public boolean occupied() {
      return this != CLEANING && this != OPEN;
    }
  }

  private EdTrackingSchedule() {
  }

  /**
   * The minute a world time is, counted from the world's first 6:00 (the Transit boards' clock).
   *
   * @param worldTime the world's time
   *
   * @return the minute
   */
  public static long minuteOf(long worldTime) {
    return Math.floorDiv(worldTime * 60L, 1000L) + 6 * 60;
  }

  /**
   * A time of day as "HH:MM".
   *
   * @param minute a minute from {@link #minuteOf}
   *
   * @return the clock text
   */
  public static String clock(long minute) {
    int m = (int) Math.floorMod(minute, 24L * 60L);
    int h = m / 60;
    m %= 60;
    return (h < 10 ? "0" : "") + h + (m < 10 ? ":0" : ":") + m;
  }

  /**
   * An elapsed time as "H:MM".
   *
   * @param minutes minutes, 0 to {@link #PERIOD}
   *
   * @return the text
   */
  public static String elapsed(int minutes) {
    int m = minutes % 60;
    return minutes / 60 + (m < 10 ? ":0" : ":") + m;
  }

  /**
   * A bed's label: two trauma bays, numbered rooms, and four hallway beds at the end.
   *
   * @param bed the bed's place on the board, 0 to {@link #MAX_BEDS} - 1
   *
   * @return the label
   */
  public static String bedLabel(int bed) {
    if (bed < 2) {
      return "TR" + (bed + 1);
    }
    if (bed >= MAX_BEDS - 4) {
      return "HW" + (bed - (MAX_BEDS - 4) + 1);
    }
    return Integer.toString(bed - 1);
  }

  /**
   * A patient's initials.
   *
   * @param patient 0 to {@link #PATIENTS} - 1
   *
   * @return "A.B."
   */
  public static String initials(int patient) {
    return FIRST.charAt(patient % FIRST.length()) + "." + LAST.charAt(
        (patient * 7 + patient / FIRST.length()) % LAST.length()) + ".";
  }

  /**
   * A nurse's initials.
   *
   * @param nurse 0 to {@link #NURSES} - 1
   *
   * @return the initials
   */
  public static String nurse(int nurse) {
    return NURSE[nurse % NURSE.length];
  }

  private static int hash(int a, int b, int c) {
    int h = a * 0x9E3779B1 ^ b * 0x85EBCA77 ^ c * 0xC2B2AE3D;
    h ^= h >>> 15;
    h *= 0x2C1B3C6D;
    h ^= h >>> 12;
    return h & 0x7FFFFFFF;
  }

  /** One bed's line at a minute. */
  public static final class Line {

    /** The status. */
    public Status status;
    /** The patient (initials index), when occupied. */
    public int patient;
    /** Minutes since the patient arrived, when occupied. */
    public int minutes;
    /** The nurse. */
    public int nurse;
  }

  /**
   * Works out a bed's line.
   *
   * @param bed    the bed's place on the board
   * @param seed   the board's seed (departments with different seeds show different patients)
   * @param minute the minute, from {@link #minuteOf}
   * @param out    where to write it
   */
  public static void line(int bed, int seed, long minute, Line out) {
    long t = minute + hash(bed, seed, 1) % PERIOD;
    int visit = (int) Math.floorDiv(t, (long) PERIOD);
    int e = (int) Math.floorMod(t, (long) PERIOD);
    int h = hash(bed, visit, seed);
    int length = 200 + h % 240;
    out.nurse = hash(bed / 4, (int) Math.floorDiv(minute, 720L), seed) % NURSES;
    out.patient = h / 240 % PATIENTS;
    out.minutes = e;
    if (e >= length + CLEANING_MINUTES) {
      out.status = Status.OPEN;
      return;
    }
    if (e >= length) {
      out.status = Status.CLEANING;
      return;
    }
    int wait = (h >>> 8) % 60;
    int rest = length - 15 - wait - 45;
    if (e < 15) {
      out.status = Status.TRIAGE;
    } else if (e < 15 + wait) {
      out.status = Status.WAITING;
    } else if (e < 15 + wait + rest / 3) {
      out.status = Status.IN_ROOM;
    } else if (e < 15 + wait + 2 * rest / 3) {
      out.status = (h >>> 14 & 1) == 0 ? Status.LAB : Status.IMAGING;
    } else if (e < length - 45) {
      out.status = (h >>> 15 & 3) == 0 ? Status.IMAGING : Status.IN_ROOM;
    } else {
      out.status = h % 3 == 0 ? Status.ADMIT : Status.DISCHARGE;
    }
  }
}
