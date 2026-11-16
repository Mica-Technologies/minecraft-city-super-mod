package com.micatechnologies.minecraft.csm.transit.arrival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The board's pages must read as the live server's hand-built boards did ("CITYLINE: Riverside",
 * "Bus Route B:" ... "4 mins", then "Northbound - Routes B, 30"), page routes past a size's
 * capacity, and replace the routes with STATION CLOSED or N/A as the template stops show.
 */
class ArrivalBoardTextTest {

  private static final int B = TransitRoute.parse("B");
  private static final int[] ROUTES = {B, 30};
  private static final int[] MINUTES = {4, 0};

  private static BusStopInfo riverside() {
    BusStopInfo info = new BusStopInfo();
    info.setStopName("Riverside");
    info.setDirection("Northbound");
    info.setRoutes("B 30");
    return info;
  }

  @Test
  void liveBoardPageOne() {
    List<ArrivalBoardText.Line> page = ArrivalBoardText.page(riverside(), ROUTES, MINUTES, 2, 6,
        false, 0);
    assertEquals(3, page.size());
    assertEquals("CITYLINE: Riverside", page.get(0).left);
    assertEquals(ArrivalBoardText.KIND_HEADER, page.get(0).kind);
    assertEquals("Bus Route B:", page.get(1).left);
    assertEquals("4 mins", page.get(1).right);
    assertEquals("Bus Route 30:", page.get(2).left);
    assertEquals("DUE", page.get(2).right);
  }

  @Test
  void liveBoardPageTwo() {
    List<ArrivalBoardText.Line> page = ArrivalBoardText.page(riverside(), ROUTES, MINUTES, 2, 6,
        false, 1);
    assertEquals(3, page.size());
    assertEquals("CITYLINE: Riverside", page.get(0).left);
    assertEquals("", page.get(1).left);
    assertEquals("Northbound - Routes B, 30", page.get(2).left);
    assertEquals(2, ArrivalBoardText.pages(riverside(), 2, 6));
    assertTrue(ArrivalBoardText.isRoutePage(riverside(), 2, 6, 0));
    assertFalse(ArrivalBoardText.isRoutePage(riverside(), 2, 6, 1));
  }

  @Test
  void noDirectionMeansOnePage() {
    BusStopInfo info = riverside();
    info.setDirection("");
    assertEquals(1, ArrivalBoardText.pages(info, 2, 6));
    // the page index wraps
    assertEquals("Bus Route B:",
        ArrivalBoardText.page(info, ROUTES, MINUTES, 2, 6, false, 5).get(1).left);
  }

  @Test
  void minutesReadings() {
    assertEquals("DUE", ArrivalBoardText.minutes(0));
    assertEquals("1 min", ArrivalBoardText.minutes(1));
    assertEquals("12 mins", ArrivalBoardText.minutes(12));
  }

  @Test
  void closedReplacesTheRoutes() {
    BusStopInfo info = riverside();
    info.setClosed(true);
    List<ArrivalBoardText.Line> page = ArrivalBoardText.page(info, ROUTES, MINUTES, 2, 6, false,
        0);
    assertEquals(2, page.size());
    assertEquals(ArrivalBoardText.CLOSED, page.get(1).left);
    assertEquals(ArrivalBoardText.KIND_CENTRED, page.get(1).kind);
    assertEquals(2, ArrivalBoardText.pages(info, 2, 6));
  }

  @Test
  void noRoutesReadsNotAvailable() {
    BusStopInfo info = new BusStopInfo();
    info.setAgency(BoardAgency.METRO);
    info.setStopName("Bus Station Name");
    List<ArrivalBoardText.Line> page = ArrivalBoardText.page(info, new int[0], new int[0], 0, 6,
        false, 0);
    assertEquals("METRO: Bus Station Name", page.get(0).left);
    assertEquals(ArrivalBoardText.NO_BUS, page.get(1).right);
    assertEquals(1, ArrivalBoardText.pages(info, 0, 6));
  }

  @Test
  void smallBoardPagesItsRoutes() {
    int[] routes = {1, 2, 3, 4, 5};
    int[] minutes = {1, 2, 3, 4, 5};
    BusStopInfo info = riverside();
    // 5 routes, 2 a page: three route pages and the direction page
    assertEquals(3, ArrivalBoardText.routePages(info, 5, 2));
    assertEquals(4, ArrivalBoardText.pages(info, 5, 2));
    List<ArrivalBoardText.Line> last = ArrivalBoardText.page(info, routes, minutes, 5, 2, false,
        2);
    assertEquals(2, last.size());
    assertEquals("Bus Route 5:", last.get(1).left);
    assertEquals(3, ArrivalBoardText.slots(info, 5, 2, false, 3));
    // the large board lists six under its header: seven slots
    assertEquals(6, ArrivalBoardText.slots(info, 5, 6, false, 3));
  }

  @Test
  void typedDestinationsFollowTheirRoute() {
    BusStopInfo info = riverside();
    info.setDestination(0, "Upper West Side");
    List<ArrivalBoardText.Line> board = ArrivalBoardText.page(info, ROUTES, MINUTES, 2, 6, false,
        0);
    assertEquals("Bus Route B: Upper West Side", board.get(1).left);
    assertEquals("4 mins", board.get(1).right);
    // route 30 has none typed: no destination
    assertEquals("Bus Route 30:", board.get(2).left);
    List<ArrivalBoardText.Line> totem = ArrivalBoardText.page(info, ROUTES, MINUTES, 2, 3, true,
        0);
    assertEquals("B Upper West Side", totem.get(0).left);
    assertEquals("30", totem.get(1).left);
  }

  @Test
  void destinationsGoWithThePlaceOnLaterPages() {
    int[] routes = {1, 2, 3};
    int[] minutes = {1, 2, 3};
    BusStopInfo info = riverside();
    info.setDestination(2, "Harbor");
    List<ArrivalBoardText.Line> second = ArrivalBoardText.page(info, routes, minutes, 3, 2, false,
        1);
    assertEquals("Bus Route 3: Harbor", second.get(1).left);
  }

  @Test
  void compactHasNoHeaderAndTwoDirectionLines() {
    List<ArrivalBoardText.Line> routes = ArrivalBoardText.page(riverside(), ROUTES, MINUTES, 2, 3,
        true, 0);
    assertEquals(2, routes.size());
    assertEquals("B", routes.get(0).left);
    assertEquals("4 mins", routes.get(0).right);
    List<ArrivalBoardText.Line> direction = ArrivalBoardText.page(riverside(), ROUTES, MINUTES, 2,
        3, true, 1);
    assertEquals("Northbound", direction.get(0).left);
    assertEquals("Routes B, 30", direction.get(1).left);
    assertEquals(3, ArrivalBoardText.slots(riverside(), 2, 3, true, 3));
  }

  @Test
  void oneRouteSaysRoute() {
    List<ArrivalBoardText.Line> page = ArrivalBoardText.page(riverside(), new int[]{B},
        new int[]{3}, 1, 6, false, 1);
    assertEquals("Northbound - Route B", page.get(2).left);
  }

  @Test
  void allPagesListsEveryPage() {
    assertEquals(2, ArrivalBoardText.allPages(riverside(), ROUTES, MINUTES, 2, 6, false).size());
  }
}
