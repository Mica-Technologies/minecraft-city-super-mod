package com.micatechnologies.minecraft.csm.technology.school;

/**
 * The arithmetic of a clock that tells the world's time: where its hands point, which minute of
 * the day it is, and the corners of a hand. Kept free of Minecraft so it can be tested on its
 * own; {@link TileEntitySchoolClockRenderer} and {@link TileEntityBellController} both use it.
 *
 * <p>A day is 24,000 ticks and starts at 6:00, as the vanilla clock's does, so tick 0 is six in
 * the morning and tick 18,000 is midnight. A game minute is therefore 16 2/3 ticks, under a
 * second; a second hand true to that would spin round more than once a second, so the sweep hand
 * turns once every 1,200 ticks (a real minute) instead, as an ornament.</p>
 *
 * @since 2026.10
 */
public final class ClockTime {

  /** Ticks in a day. */
  public static final long TICKS_PER_DAY = 24000L;

  /** Minutes in a day. */
  public static final int MINUTES_PER_DAY = 1440;

  /** Ticks for one turn of the sweep hand: a real minute at 20 ticks a second. */
  public static final long SWEEP_TICKS = 1200L;

  /** Ticks into the day at which the clock reads midnight. */
  private static final long MIDNIGHT_OFFSET = 6000L;

  private ClockTime() {
  }

  /**
   * The world's time of day as ticks since midnight, 0 to 23,999.
   *
   * @param worldTime the world time ({@code World.getWorldTime()}), any value
   *
   * @return ticks since midnight
   */
  public static long ticksSinceMidnight(long worldTime) {
    return Math.floorMod(worldTime + MIDNIGHT_OFFSET, TICKS_PER_DAY);
  }

  /**
   * The minute of the day the clock reads, 0 (midnight) to 1,439 (23:59).
   *
   * @param worldTime the world time
   *
   * @return the minute of the day
   */
  public static int minuteOfDay(long worldTime) {
    return (int) (ticksSinceMidnight(worldTime) * MINUTES_PER_DAY / TICKS_PER_DAY);
  }

  /**
   * The hour hand's angle, clockwise from twelve as seen from the front.
   *
   * @param worldTime the world time
   *
   * @return degrees, 0 to under 360
   */
  public static double hourHandDegrees(long worldTime) {
    double hours = ticksSinceMidnight(worldTime) / 1000.0;
    return (hours % 12.0) * 30.0;
  }

  /**
   * The minute hand's angle, clockwise from twelve as seen from the front.
   *
   * @param worldTime the world time
   *
   * @return degrees, 0 to under 360
   */
  public static double minuteHandDegrees(long worldTime) {
    return (ticksSinceMidnight(worldTime) % 1000L) * 360.0 / 1000.0;
  }

  /**
   * The sweep hand's angle: one turn every {@link #SWEEP_TICKS} ticks of total world time.
   *
   * @param totalWorldTime the total world time ({@code World.getTotalWorldTime()})
   * @param partialTicks   the part of a tick since it, for a smooth sweep
   *
   * @return degrees, 0 to under 360
   */
  public static double sweepHandDegrees(long totalWorldTime, float partialTicks) {
    double t = Math.floorMod(totalWorldTime, SWEEP_TICKS) + partialTicks;
    return (t % SWEEP_TICKS) * 360.0 / SWEEP_TICKS;
  }

  /**
   * The four corners of a hand on its dial, in the dial's plane: x to the viewer's right, y up,
   * the dial's middle at the origin. The hand is a tapered quad from {@code tail} behind the
   * middle to {@code length} past it, {@code halfWidth} either side at the tail and six tenths
   * of that at the tip, turned clockwise from twelve by {@code degrees}.
   *
   * @param degrees   the hand's angle, clockwise from twelve
   * @param length    how far the tip is from the middle
   * @param tail      how far the tail reaches behind the middle
   * @param halfWidth half the hand's width at the tail
   *
   * @return {x0, y0, x1, y1, x2, y2, x3, y3}, going round the quad
   */
  public static double[] handCorners(double degrees, double length, double tail,
      double halfWidth) {
    double a = Math.toRadians(degrees);
    double c = Math.cos(a);
    double s = Math.sin(a);
    double[][] local = {{-halfWidth, -tail}, {halfWidth, -tail}, {halfWidth * 0.6, length},
        {-halfWidth * 0.6, length}};
    double[] out = new double[8];
    for (int i = 0; i < 4; i++) {
      double px = local[i][0];
      double py = local[i][1];
      out[i * 2] = px * c + py * s;
      out[i * 2 + 1] = -px * s + py * c;
    }
    return out;
  }

  /**
   * Formats a minute of the day as 24-hour {@code HH:MM}.
   *
   * @param minuteOfDay 0 to 1,439
   *
   * @return the time, e.g. {@code 08:50}
   */
  public static String format(int minuteOfDay) {
    int m = Math.floorMod(minuteOfDay, MINUTES_PER_DAY);
    return String.format("%02d:%02d", m / 60, m % 60);
  }
}
