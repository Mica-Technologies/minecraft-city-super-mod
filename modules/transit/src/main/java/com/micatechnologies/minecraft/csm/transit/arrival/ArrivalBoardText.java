package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
import java.util.ArrayList;
import java.util.List;

/**
 * What an arrival board's pages say, line by line, from its data ({@link BusStopInfo}), its routes
 * and each route's minutes to the next bus: pure, so the layout is tested without a game. The
 * renderer measures and places the lines; this only decides their words.
 *
 * <p><b>Pages.</b> First the route pages: the header, then one line a route
 * ("Bus Route B:" ... "4 mins", or "Bus Route B: Upper West Side" ... "4 mins" when a
 * destination was typed for it), {@code perPage} routes a page, in the order the routes are
 * listed. With no routes at all, one line reading "N/A mins"; closed, one line reading STATION
 * CLOSED in place of every route line (one page). Then, if there is a direction line, the
 * direction page: the header, a blank line and "Northbound - Routes B, 30", as the live server's
 * hand-built boards read. The board turns its pages every
 * {@code TileEntityBusArrivalDisplayRenderer.PAGE_TICKS}, every board in step.</p>
 *
 * <p><b>Compact</b> (the info totem's narrow panel): no header (the totem's name panel says it),
 * a route line is just its id ("B ... 4 mins"), and the direction page is the direction and the
 * routes on two lines.</p>
 *
 * @since 2026.11
 */
public final class ArrivalBoardText {

  /** The header: the agency's name and the stop's, in the agency's colour. */
  public static final int KIND_HEADER = 0;
  /** A line of the screen's amber text: left words, right-aligned reading. */
  public static final int KIND_AMBER = 1;
  /** Amber, centred: STATION CLOSED. */
  public static final int KIND_CENTRED = 2;

  /** A reading for a route with no bus: the live template stops' text. */
  public static final String NO_BUS = "N/A mins";
  /** The widest reading a route line can have, which the layout leaves room for. */
  public static final String WIDEST_READING = "88 mins";
  public static final String CLOSED = "STATION CLOSED";

  private ArrivalBoardText() {
  }

  /** One line of a page. */
  public static final class Line {

    public final String left;
    public final String right;
    public final int kind;

    Line(String left, String right, int kind) {
      this.left = left;
      this.right = right;
      this.kind = kind;
    }

    @Override
    public String toString() {
      return right.isEmpty() ? left : left + " | " + right;
    }
  }

  /**
   * A route's reading.
   *
   * @param minutes minutes to the next bus; 0 is due
   *
   * @return "DUE", "1 min" or "n mins"
   */
  public static String minutes(int minutes) {
    if (minutes <= 0) {
      return "DUE";
    }
    return minutes == 1 ? "1 min" : minutes + " mins";
  }

  /**
   * How many route pages a board has.
   *
   * @param info    its data
   * @param count   how many routes it lists
   * @param perPage route lines a page
   *
   * @return at least 1
   */
  public static int routePages(BusStopInfo info, int count, int perPage) {
    if (info.isClosed() || count <= 0) {
      return 1;
    }
    int per = Math.max(1, perPage);
    return (count + per - 1) / per;
  }

  /**
   * How many pages a board turns through: its route pages and, with a direction line, the
   * direction page.
   *
   * @param info    its data
   * @param count   how many routes it lists
   * @param perPage route lines a page
   *
   * @return at least 1
   */
  public static int pages(BusStopInfo info, int count, int perPage) {
    return routePages(info, count, perPage) + (info.getDirection().isEmpty() ? 0 : 1);
  }

  /**
   * Whether a page is a route page, whose readings change with the minute.
   *
   * @param info    its data
   * @param count   how many routes it lists
   * @param perPage route lines a page
   * @param page    the page
   *
   * @return true for a route page
   */
  public static boolean isRoutePage(BusStopInfo info, int count, int perPage, int page) {
    return page < routePages(info, count, perPage);
  }

  /**
   * A page's lines.
   *
   * @param info    the board's data
   * @param routes  the routes it lists ({@link TransitRoute} keys); the first {@code count}
   * @param minutes each route's minutes to the next bus, the same order
   * @param count   how many routes
   * @param perPage route lines a page
   * @param compact the totem's panel: no header, short route lines
   * @param page    the page, 0 up; taken modulo the page count
   *
   * @return the lines, top to bottom
   */
  public static List<Line> page(BusStopInfo info, int[] routes, int[] minutes, int count,
      int perPage, boolean compact, int page) {
    List<Line> out = new ArrayList<>();
    int pages = pages(info, count, perPage);
    int p = Math.floorMod(page, pages);
    int routePages = routePages(info, count, perPage);
    if (!compact) {
      out.add(new Line(info.header(), "", KIND_HEADER));
    }
    if (p >= routePages) {
      String list = routeList(routes, count);
      if (compact) {
        out.add(new Line(info.getDirection(), "", KIND_AMBER));
        if (!list.isEmpty()) {
          out.add(new Line(list, "", KIND_AMBER));
        }
      } else {
        out.add(new Line("", "", KIND_AMBER));
        out.add(new Line(list.isEmpty() ? info.getDirection()
            : info.getDirection() + " - " + list, "", KIND_AMBER));
      }
      return out;
    }
    if (info.isClosed()) {
      out.add(new Line(CLOSED, "", KIND_CENTRED));
      return out;
    }
    if (count <= 0) {
      out.add(new Line(compact ? "" : "Next bus:", NO_BUS, KIND_AMBER));
      return out;
    }
    int per = Math.max(1, perPage);
    for (int i = p * per; i < count && i < (p + 1) * per; i++) {
      out.add(new Line(routeLabel(info, routes[i], i, compact), minutes(minutes[i]),
          KIND_AMBER));
    }
    return out;
  }

  /**
   * Every page's lines, for a layout that must fit them all at one size.
   *
   * @return one list a page
   */
  public static List<List<Line>> allPages(BusStopInfo info, int[] routes, int[] minutes,
      int count, int perPage, boolean compact) {
    int pages = pages(info, count, perPage);
    List<List<Line>> out = new ArrayList<>(pages);
    for (int p = 0; p < pages; p++) {
      out.add(page(info, routes, minutes, count, perPage, compact, p));
    }
    return out;
  }

  /**
   * How many line slots a board's layout needs: the most lines any of its pages has, and at least
   * {@code minSlots}, so a board's text keeps one size and place through its pages.
   *
   * @return the slots
   */
  public static int slots(BusStopInfo info, int count, int perPage, boolean compact,
      int minSlots) {
    int body;
    if (info.isClosed() || count <= 0) {
      body = 1;
    } else {
      body = Math.min(count, Math.max(1, perPage));
    }
    if (!info.getDirection().isEmpty()) {
      body = Math.max(body, 2);
    }
    return Math.max(minSlots, body + (compact ? 0 : 1));
  }

  /**
   * "Bus Route B:", then the destination typed for it if there is one
   * ("Bus Route B: Upper West Side"); compact, "B" or "B Upper West Side".
   *
   * @param index the route's place in the routes the board lists, which its destination is
   *              stored under
   */
  static String routeLabel(BusStopInfo info, int route, int index, boolean compact) {
    String id = TransitRoute.label(route);
    String destination = info.getDestination(index);
    if (compact) {
      return destination.isEmpty() ? id : id + " " + destination;
    }
    return destination.isEmpty() ? "Bus Route " + id + ":"
        : "Bus Route " + id + ": " + destination;
  }

  /** "Routes B, 30", "Route B", or "" with none. */
  static String routeList(int[] routes, int count) {
    if (count <= 0) {
      return "";
    }
    StringBuilder out = new StringBuilder(count == 1 ? "Route " : "Routes ");
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        out.append(", ");
      }
      out.append(TransitRoute.label(routes[i]));
    }
    return out.toString();
  }
}
