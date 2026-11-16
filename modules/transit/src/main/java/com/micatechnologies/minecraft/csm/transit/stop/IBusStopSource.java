package com.micatechnologies.minecraft.csm.transit.stop;

import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;

/**
 * A tile entity that marks a bus stop and the routes calling at it: the stop flag on a sign post,
 * and the full-size stop pole's flag sign. A departure board, a bay display and an arrival display
 * read either through this, so a stop built either way is a bay on the boards and counts down the
 * same buses. The countdown is seeded by the source's column ({@link BusDepartures#minutesToNext}).
 *
 * @since 2026.11
 */
public interface IBusStopSource {

  /** Where the stop is: its column seeds the countdown. */
  BlockPos getStopPos();

  /** How many routes call at the stop, at most {@link TransitRoute#MAX_PER_LINE}. */
  int getStopRouteCount();

  /**
   * A route calling at the stop.
   *
   * @param i 0 to {@link #getStopRouteCount} less one
   *
   * @return its key ({@link TransitRoute})
   */
  int getStopRoute(int i);

  /** The stop's agency, or null for a custom one. */
  @Nullable
  BusAgency getStopAgency();

  /** The colour a board prints the stop's routes in, 0xRRGGBB. */
  int getStopColour();
}
