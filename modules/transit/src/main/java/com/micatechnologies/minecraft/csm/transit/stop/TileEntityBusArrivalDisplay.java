package com.micatechnologies.minecraft.csm.transit.stop;

/**
 * An arrival display's tile entity. It saves nothing: what the display lists is the routes on its
 * stop's flag, looked up on the renderer's schedule ({@link #refreshView}) rather than every
 * frame, and the arrivals follow from those and the world's clock.
 *
 * @since 2026.9
 */
public class TileEntityBusArrivalDisplay extends AbstractTileEntityBusStopFitting {

  /** The routes a display lists on a stop with no flag. */
  private static final int[] STOCK_ROUTES = {12, 40};

  private final int[] routes = new int[TileEntityBusStopFlag.PLATES];
  private int routeCount;

  @Override
  protected void refreshMore() {
    routeCount = 0;
    TileEntityBusStopFlag flag = BusStopStack.flagNear(world, pos);
    if (flag != null) {
      for (int i = 0; i < TileEntityBusStopFlag.PLATES; i++) {
        int route = flag.getRoute(i);
        if (route != 0) {
          routes[routeCount++] = route;
        }
      }
    }
    if (routeCount == 0) {
      for (int route : STOCK_ROUTES) {
        routes[routeCount++] = route;
      }
    }
  }

  /**
   * How many routes the display lists, as last looked up.
   *
   * @return 1 to {@link TileEntityBusStopFlag#PLATES}
   */
  public int getRouteCount() {
    return routeCount;
  }

  /**
   * A route the display lists.
   *
   * @param i 0 to {@link #getRouteCount()} - 1
   *
   * @return the route number
   */
  public int getRoute(int i) {
    return routes[i];
  }

  /** Nothing here reaches a baked model. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
