package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractTileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;

/**
 * A cell of the full-size stop pole's flag ({@link BlockBusStopPoleFlag}): Core's wayfinding
 * panel cell, and a bus stop for the boards and displays ({@link IBusStopSource}).
 *
 * <p>Its routes are the ids on the flag's route line, the last of lines three and four that has
 * text ({@link TransitRoute#parseLine}), so "B 30" is routes B and 30 and the flag's own words
 * (BUS STOP, the agency's name) are never read as routes. Its agency is the one whose sign ground
 * the flag's background is ({@link BusAgency#ofSignGround}), else a custom one, whose routes a
 * board prints in the brighter of the flag's two colours.</p>
 *
 * <p>Every cell of a panel holds the sign, so each cell would be a stop: only the controller
 * cell (the panel's bottom-left as the reader sees it) is one ({@link #getStopRouteCount} is 0 on
 * the others).</p>
 *
 * @since 2026.11
 */
public class TileEntityBusStopPoleFlag extends AbstractTileEntityWayfindingPanel
    implements IBusStopSource {

  /** The sign the routes were last read from, and what they read. */
  private WayfindingSign parsedFrom;
  private int[] routes = new int[0];

  @Override
  public WayfindingSign defaultSign() {
    return BusStopSignPresets.FLAG_DEFAULT;
  }

  private int[] routes() {
    WayfindingSign sign = getSign();
    if (sign != parsedFrom) {
      parsedFrom = sign;
      String line = sign.getLine(3).trim().isEmpty() ? sign.getLine(2) : sign.getLine(3);
      routes = TransitRoute.parseLine(line);
    }
    return routes;
  }

  private boolean isController() {
    return !isPlaceKnown() || (getCol() == 0 && getRow() == 0);
  }

  @Override
  public boolean isStopBay() {
    return isController();
  }

  @Override
  public BlockPos getStopPos() {
    return getPos();
  }

  @Override
  public int getStopRouteCount() {
    return isController() ? routes().length : 0;
  }

  @Override
  public int getStopRoute(int i) {
    int[] r = routes();
    return i >= 0 && i < r.length ? r[i] : 0;
  }

  @Override
  @Nullable
  public BusAgency getStopAgency() {
    return BusAgency.ofSignGround(getSign().background());
  }

  @Override
  public int getStopColour() {
    BusAgency agency = getStopAgency();
    if (agency != null) {
      return agency.getTextColour();
    }
    int a = getSign().background() & 0xFFFFFF;
    int b = getSign().legend() & 0xFFFFFF;
    return luminance(a) >= luminance(b) ? a : b;
  }

  private static int luminance(int rgb) {
    return ((rgb >> 16) & 0xFF) * 299 + ((rgb >> 8) & 0xFF) * 587 + (rgb & 0xFF) * 114;
  }
}
