package com.micatechnologies.minecraft.csm.transit.stop;

import net.minecraft.util.IStringSerializable;

/**
 * The bus stop poles: two shapes, galvanized or painted in an agency colour. A fitting on a pole
 * (a flag, a case, the arrival display) draws its own length of pole in the style of the pole
 * below it, read into its actual state as {@link BlockBusStopFitting#POLE}.
 *
 * <p>Actual state only, so the order is free to change; but {@code gen_transit_stops.py} reads
 * the constants from this file and writes one set of pole models per constant, so a constant
 * added here needs a matching entry in that generator's {@code STYLES}.</p>
 *
 * @since 2026.9
 */
public enum BusStopPoleStyle implements IStringSerializable {
  ROUND_GALVANIZED(true),
  SQUARE_GALVANIZED(false),
  ROUND_TEAL(true),
  SQUARE_NAVY(false),
  ROUND_GREEN(true),
  SQUARE_RED(false);

  private final boolean round;

  BusStopPoleStyle(boolean round) {
    this.round = round;
  }

  /**
   * Whether the pole is round (a tube) rather than square.
   *
   * @return true for a round pole
   */
  public boolean isRound() {
    return round;
  }

  @Override
  public String getName() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
