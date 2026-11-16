package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.transit.stop.BusAgency;
import javax.annotation.Nullable;

/**
 * The agency an arrival board or info totem is set to ({@link BusStopInfo}): one of the four
 * built-in bus agencies ({@link BusAgency}), METRO (the network, as the live server's poles show
 * it), or a custom agency whose name and two colours the board carries itself.
 *
 * <p>The ordinal is what the board saves and sends, so the order is fixed: only append.</p>
 *
 * @since 2026.11
 */
public enum BoardAgency {
  CITYLINE(BusAgency.CITYLINE),
  RIVERWAY(BusAgency.RIVERWAY),
  VERDANT(BusAgency.VERDANT),
  EMBERLINE(BusAgency.EMBERLINE),
  METRO(null),
  CUSTOM(null);

  /** METRO's sign ground: near-black, the roundel's. */
  public static final int METRO_GROUND = 0x1E2228;
  /** METRO's lettering and screen text: white. */
  public static final int METRO_LEGEND = 0xF4F4F4;

  private static final BoardAgency[] VALUES = values();

  private final BusAgency bus;

  BoardAgency(@Nullable BusAgency bus) {
    this.bus = bus;
  }

  /**
   * The built-in bus agency this is.
   *
   * @return the agency, or null for METRO and CUSTOM
   */
  @Nullable
  public BusAgency getBusAgency() {
    return bus;
  }

  /**
   * The agency with an ordinal, as read from a save or the wire.
   *
   * @param ordinal the ordinal
   *
   * @return the agency, or null for an ordinal no agency has
   */
  @Nullable
  public static BoardAgency byOrdinal(int ordinal) {
    return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
  }

  /**
   * The name the board prints in its header.
   *
   * @param info the board's data, for a custom agency's own name
   *
   * @return capitals for a built-in agency ("CITYLINE", "METRO"), the custom name as typed
   */
  public String title(BusStopInfo info) {
    if (bus != null) {
      return bus.getTitle();
    }
    return this == METRO ? "METRO" : info.getCustomName();
  }

  /**
   * The ground of the agency's signs: the totem's name panel and the board's top band.
   *
   * @param info the board's data, for a custom agency's colours
   *
   * @return 0xRRGGBB
   */
  public int ground(BusStopInfo info) {
    if (bus != null) {
      return bus.getSignGround();
    }
    return this == METRO ? METRO_GROUND : info.getCustomGround();
  }

  /**
   * The lettering on the agency's signs: the totem's stop name.
   *
   * @param info the board's data, for a custom agency's colours
   *
   * @return 0xRRGGBB
   */
  public int legend(BusStopInfo info) {
    if (bus != null) {
      return bus.getSignLegend();
    }
    return this == METRO ? METRO_LEGEND : info.getCustomLegend();
  }

  /**
   * The colour of the agency's name lit on a dark screen: a built-in agency's text colour, METRO
   * white, a custom agency the brighter of its two colours (as a custom flag's routes are
   * printed on the departure boards).
   *
   * @param info the board's data, for a custom agency's colours
   *
   * @return 0xRRGGBB
   */
  public int screenColour(BusStopInfo info) {
    if (bus != null) {
      return bus.getTextColour();
    }
    if (this == METRO) {
      return METRO_LEGEND;
    }
    int a = info.getCustomGround();
    int b = info.getCustomLegend();
    return luminance(a) >= luminance(b) ? a : b;
  }

  private static int luminance(int rgb) {
    return ((rgb >> 16) & 0xFF) * 299 + ((rgb >> 8) & 0xFF) * 587 + (rgb & 0xFF) * 114;
  }
}
