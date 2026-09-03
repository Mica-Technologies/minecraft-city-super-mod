package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Pure (world-free) actuated-timing math for ADVANCED (NEMA) mode, adapting several Econolite ASC/3
 * volume-density features to this mod's presence-count detection model. All times are in game ticks.
 *
 * <p>Because our detectors report a count of waiting vehicles (presence) rather than discrete
 * actuation pulses, "added initial" is driven by the queue length at green start instead of by
 * actuations counted during the preceding red — same intent (serve a longer guaranteed initial
 * green for a longer standing queue), adapted to the available signal.
 *
 * <p>See {@code assets/docs/ADVANCED_MODE_ASC3.md}.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public final class AdvancedActuationTiming {

  private AdvancedActuationTiming() {
  }

  /**
   * Volume-density added initial: extra guaranteed initial green earned by the queue waiting at
   * green start, capped at the max-initial limit.
   *
   * @param queueAtStart    waiting vehicle count when the phase turned green
   * @param perVehicle      ticks of added initial per waiting vehicle (0 disables)
   * @param maxInitial      cap on the added initial (0 = uncapped)
   *
   * @return the added initial green time (ticks), never negative
   */
  public static long addedInitial(long queueAtStart, long perVehicle, long maxInitial) {
    if (perVehicle <= 0L || queueAtStart <= 0L) {
      return 0L;
    }
    long added = queueAtStart * perVehicle;
    if (maxInitial > 0L) {
      added = Math.min(added, maxInitial);
    }
    return Math.max(0L, added);
  }

  /**
   * The actual minimum green indication: the longest of the configured minimum green, the
   * volume-density added initial, and (when a bike call is present) the bike minimum green. This
   * mirrors the ASC/3 rule that actual min green is the longest of several contributing minimums.
   *
   * @param minGreen     configured minimum green
   * @param addedInitial computed volume-density added initial (see {@link #addedInitial})
   * @param bikeMinGreen bike minimum green (applied only when {@code bikeCall})
   * @param bikeCall     whether a bike call is present
   *
   * @return the effective minimum green (ticks)
   */
  public static long effectiveMinGreen(long minGreen, long addedInitial, long bikeMinGreen,
      boolean bikeCall) {
    long min = Math.max(minGreen, addedInitial);
    if (bikeCall && bikeMinGreen > min) {
      min = bikeMinGreen;
    }
    return min;
  }

  /**
   * Secondary-max selection: returns {@code max2} when it is selected and configured, otherwise the
   * primary {@code max1}.
   *
   * @param max1    primary maximum green
   * @param max2    secondary maximum green (0 = not configured)
   * @param useMax2 whether the secondary max is currently selected
   *
   * @return the effective maximum green (ticks)
   */
  public static long effectiveMaxGreen(long max1, long max2, boolean useMax2) {
    return (useMax2 && max2 > 0L) ? max2 : max1;
  }

  /**
   * Volume-density gap reduction: the vehicle extension (passage) shrinks linearly from its full
   * value toward the minimum gap, starting after {@code timeBeforeReduce} of green and reaching the
   * minimum gap after a further {@code timeToReduce}. Disabled (returns the full passage) when the
   * reduction is not configured.
   *
   * @param passage          full vehicle extension / passage time
   * @param minGap           gap-reduction floor
   * @param timeBeforeReduce green time before reduction begins
   * @param timeToReduce     duration of the reduction ramp (0 = disabled)
   * @param greenElapsed     time elapsed in the current green
   *
   * @return the currently-effective passage time (ticks), never below the min gap
   */
  public static long effectivePassage(long passage, long minGap, long timeBeforeReduce,
      long timeToReduce, long greenElapsed) {
    if (timeToReduce <= 0L || minGap <= 0L || minGap >= passage) {
      return passage; // gap reduction disabled or pointless
    }
    if (greenElapsed <= timeBeforeReduce) {
      return passage;
    }
    long into = greenElapsed - timeBeforeReduce;
    if (into >= timeToReduce) {
      return minGap;
    }
    long range = passage - minGap;
    long reduced = passage - (range * into) / timeToReduce;
    return Math.max(minGap, reduced);
  }
}
