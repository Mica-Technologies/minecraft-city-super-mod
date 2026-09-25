package com.micatechnologies.minecraft.csm.transit.stop;

/**
 * The buses' made-up but steady timetable, which the arrival display on a stop's post, the bay
 * display hung over a bay and the bus station departure board all read, so that all three agree
 * to the minute about every bus.
 *
 * <p>A route's headway (6 to 16 minutes) and destination are fixed by its number, so a route goes
 * to the same place at every stop. Where in its cycle a route is depends on the world's clock and
 * the stop: the column of the stop's post ({@link #minutesToNext}), so neighbouring stops differ
 * while the flag, the display on the same post and a board across the concourse, each looking at
 * that one post, work out the same wait. The countdown runs a minute each real minute
 * ({@link #TICKS_PER_MINUTE}), on the world's total time, not its time of day: a bus a few
 * minutes off should be a few minutes off in the player's own time.</p>
 *
 * <p>Nothing here is saved or sent. It is a pure function of the clock, the route and the stop.</p>
 *
 * @since 2026.9
 */
public final class BusDepartures {

  /** Ticks in a minute of the countdown: one real minute. */
  public static final long TICKS_PER_MINUTE = 1200;

  /** Invented, and generic on purpose: no real place is named. */
  public static final String[] DESTINATIONS = {
      "DOWNTOWN", "UPTOWN", "HARBOR", "AIRPORT", "UNIVERSITY", "STADIUM", "HOSPITAL",
      "RIVERSIDE", "OLD TOWN", "LAKESIDE", "TRANSIT CTR", "NORTH END", "SOUTH END", "WEST SIDE",
      "EAST SIDE", "CITY HALL"};

  /** The destinations as a voice reads them out. Same order as {@link #DESTINATIONS}. */
  public static final String[] DESTINATIONS_SPOKEN = {
      "Downtown", "Uptown", "Harbor", "Airport", "University", "Stadium", "Hospital",
      "Riverside", "Old Town", "Lakeside", "Transit Center", "North End", "South End",
      "West Side", "East Side", "City Hall"};

  /** The longest wait listed: the second bus of a route with the longest headway. */
  public static final int MAX_MINUTES = 32;

  /** Buses of each route listed: the next and the one after. */
  public static final int PER_ROUTE = 2;

  private BusDepartures() {
  }

  /**
   * The countdown's minute at a world time.
   *
   * @param totalWorldTime {@code World#getTotalWorldTime()}
   *
   * @return the minute
   */
  public static long minuteOf(long totalWorldTime) {
    return totalWorldTime / TICKS_PER_MINUTE;
  }

  /**
   * A route's minutes between buses, fixed by its number.
   *
   * @param route 1 to 99
   *
   * @return 6 to 16
   */
  public static int headwayOf(int route) {
    return 6 + (route * 5) % 11;
  }

  /**
   * A route's destination, fixed by its number.
   *
   * @param route 1 to 99
   *
   * @return an index into {@link #DESTINATIONS}
   */
  public static int destinationOf(int route) {
    return (route * 7 + 3) % DESTINATIONS.length;
  }

  /**
   * How many minutes until the next bus of a route calls at a stop. Zero is "due": under a
   * minute away.
   *
   * @param route  the route
   * @param x      the stop post's x
   * @param z      the stop post's z
   * @param minute the minute, from {@link #minuteOf}
   *
   * @return 0 to the route's headway less one
   */
  public static int minutesToNext(int route, int x, int z, long minute) {
    int seed = (x * 31 + z * 17) & 0xFFFF;
    int headway = headwayOf(route);
    int phase = (int) ((minute + route * 13L + seed) % headway);
    return headway - 1 - phase;
  }

  /**
   * The next {@link #PER_ROUTE} buses of each of a stop's routes, soonest first, as the arrival
   * display lists them.
   *
   * @param routes     the stop's routes
   * @param count      how many of {@code routes} to use
   * @param x          the stop post's x
   * @param z          the stop post's z
   * @param minute     the minute, from {@link #minuteOf}
   * @param outRoute   filled with each arrival's route; at least {@code count * PER_ROUTE} long
   * @param outMinutes filled with each arrival's minutes, the same length
   *
   * @return how many arrivals were filled in
   */
  public static int arrivals(int[] routes, int count, int x, int z, long minute, int[] outRoute,
      int[] outMinutes) {
    int n = 0;
    for (int i = 0; i < count; i++) {
      int route = routes[i];
      int next = minutesToNext(route, x, z, minute);
      n = insert(n, route, next, outRoute, outMinutes);
      n = insert(n, route, next + headwayOf(route), outRoute, outMinutes);
    }
    return n;
  }

  private static int insert(int n, int route, int minutes, int[] outRoute, int[] outMinutes) {
    int at = n;
    while (at > 0 && outMinutes[at - 1] > minutes) {
      outMinutes[at] = outMinutes[at - 1];
      outRoute[at] = outRoute[at - 1];
      at--;
    }
    outMinutes[at] = minutes;
    outRoute[at] = route;
    return n + 1;
  }
}
