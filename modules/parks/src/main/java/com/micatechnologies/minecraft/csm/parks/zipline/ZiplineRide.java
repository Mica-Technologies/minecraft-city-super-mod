package com.micatechnologies.minecraft.csm.parks.zipline;

/**
 * How fast a zipline rider goes: one step of the ride, kept free of the game so it can be tested.
 *
 * <p>Downhill, gravity along the cable speeds the rider up and air drag holds them back, to a top
 * speed no faster than the configured cap. The trolley pulls at the configured minimum, so a level
 * or rising line still runs, and a rider never stalls mid-line. Before the stop point a brake zone
 * slows anyone to {@link #ARRIVAL_SPEED} by a steady deceleration, whatever the line, so the
 * landing is a walk off the trolley. A rider closer than {@link #SPACING} behind another is held
 * back to keep the gap.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public final class ZiplineRide {

  /** Gravity along the cable, in blocks per second squared: gentler than a fall, as a trolley is. */
  static final double GRAVITY = 14.0;
  /** Air and sheave drag, per block: terminal speed on a slope of sine {@code s} is sqrt(g s / k). */
  static final double DRAG = 0.035;
  /** How fast a rider reaches the stop point. */
  static final double ARRIVAL_SPEED = 1.5;
  /** The brakes' deceleration, in blocks per second squared. */
  static final double BRAKING = 7.0;
  /** The least gap, along the cable, between two riders on one line. */
  public static final double SPACING = 6.0;
  /** One game tick, in seconds. */
  static final double TICK = 0.05;

  private ZiplineRide() {
  }

  /**
   * The speed for the next tick.
   *
   * @param speed     this tick's speed, blocks a second
   * @param slope     the cable's rise per block here (negative downhill)
   * @param toStop    how far the rider is from the stop point, blocks
   * @param braking   whether the rider is in the brake zone
   * @param gapAhead  how far ahead along the cable the next rider is, or infinity
   * @param minSpeed  the trolley's own pull
   * @param maxSpeed  the top speed
   *
   * @return the next speed, never negative
   *
   * @since 1.0
   */
  public static double next(double speed, double slope, double toStop, boolean braking,
      double gapAhead, double minSpeed, double maxSpeed) {
    double v = speed + (-GRAVITY * slope - DRAG * speed * speed) * TICK;
    v = Math.max(minSpeed, Math.min(maxSpeed, v));
    if (braking) {
      // The speed from which BRAKING stops at ARRIVAL_SPEED in exactly the distance left.
      double brakeCap = Math.sqrt(ARRIVAL_SPEED * ARRIVAL_SPEED
          + 2 * BRAKING * Math.max(0, toStop));
      v = Math.min(v, Math.max(ARRIVAL_SPEED, brakeCap));
    }
    if (gapAhead < Double.POSITIVE_INFINITY) {
      // Close no nearer than SPACING this tick.
      v = Math.min(v, Math.max(0, (gapAhead - SPACING) / TICK));
    }
    return Math.max(0, v);
  }
}
