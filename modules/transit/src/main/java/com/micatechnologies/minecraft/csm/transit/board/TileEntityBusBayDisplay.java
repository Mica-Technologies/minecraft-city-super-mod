package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.transit.stop.BusDepartures;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlag;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * A bay display's tile entity. It saves nothing: the display lists the stop nearest it, the bus
 * stop flag within {@link #REACH_ACROSS} blocks across and {@link #REACH_UP} up or down, found
 * through the station ({@link BusStation}) once a second rather than every frame. The arrivals
 * are counted at that flag's post, so the display agrees with the arrival display on the post and
 * with the departure board. With no stop that close it lists the arrival display's two stock
 * routes, counted where it hangs.
 *
 * @since 2026.9
 */
public class TileEntityBusBayDisplay extends AbstractTileEntity {

  /** How far across the stop's flag may be. */
  public static final int REACH_ACROSS = 4;
  /** How far above or below it may be. */
  public static final int REACH_UP = 6;
  private static final long VIEW_TICKS = 20;

  /** The routes a display lists with no stop near it: the arrival display's. */
  private static final int[] STOCK_ROUTES = {12, 40};

  private long checkedAt = Long.MIN_VALUE;
  private EnumFacing facing = EnumFacing.NORTH;
  private final int[] routes = new int[TileEntityBusStopFlag.PLATES];
  private int routeCount;
  private int stopX;
  private int stopZ;

  /** Looks the facing and the stop up again if the last answer is older than a second. */
  public void refreshView() {
    if (world == null) {
      return;
    }
    long now = world.getTotalWorldTime();
    if (checkedAt != Long.MIN_VALUE && now - checkedAt < VIEW_TICKS && now >= checkedAt) {
      return;
    }
    checkedAt = now;
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof BlockBusBoard) {
      facing = state.getValue(BlockBusBoard.FACING);
    }
    BusStation station = BusStation.at(world, pos);
    int bay = station.nearestBay(pos, REACH_ACROSS, REACH_UP);
    routeCount = 0;
    if (bay >= 0) {
      int[] r = station.bayRoutes(bay);
      for (int i = 0; i < station.bayRouteCount(bay); i++) {
        routes[routeCount++] = r[i];
      }
      stopX = station.bayPos(bay).getX();
      stopZ = station.bayPos(bay).getZ();
    } else {
      for (int route : STOCK_ROUTES) {
        routes[routeCount++] = route;
      }
      stopX = pos.getX();
      stopZ = pos.getZ();
    }
  }

  /**
   * Fills in the next buses of the display's stop, soonest first.
   *
   * @param outRoute   each arrival's route
   * @param outMinutes each arrival's minutes
   *
   * @return how many arrivals
   */
  public int arrivals(int[] outRoute, int[] outMinutes) {
    return BusDepartures.arrivals(routes, routeCount, stopX, stopZ,
        BusDepartures.minuteOf(world.getTotalWorldTime()), outRoute, outMinutes);
  }

  /** The display's facing, as last looked up. */
  public EnumFacing getFacing() {
    return facing;
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1,
        p.getZ() + 1);
  }

  /** The same reach as the flight and departure boards. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return 48.0 * 48.0;
  }

  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
