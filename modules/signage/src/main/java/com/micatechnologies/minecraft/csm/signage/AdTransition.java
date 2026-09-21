package com.micatechnologies.minecraft.csm.signage;

/**
 * How a rotating board changes from one ad to the next.
 *
 * <p>The ordinal is stored in NBT and sent in packets: append only.</p>
 */
public enum AdTransition {
  /** The next ad replaces the last at once, as a re-papered board or a plain screen does. */
  CUT,
  /** The next ad fades in over the last, as a digital screen does. */
  FADE,
  /** The next ad rolls up from below and pushes the last out of the top, as a scroller does. */
  SLIDE;

  /** How long a fade or a slide takes, in ticks. */
  public static final int TICKS = 16;

  /** The transition with the given ordinal, or {@link #CUT} for anything out of range. */
  public static AdTransition fromOrdinal(int ordinal) {
    AdTransition[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CUT;
  }

  /** The next transition, for a button that cycles. */
  public AdTransition next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /**
   * How far through a change a board is: 0 as it starts, 1 once it is over (and for the whole of
   * the rest of the interval), eased in and out.
   *
   * @param ticksIntoStep ticks since the current ad came up, partial ticks included
   */
  public static double progress(double ticksIntoStep) {
    if (ticksIntoStep >= TICKS) {
      return 1.0;
    }
    double p = Math.max(0.0, ticksIntoStep / TICKS);
    return p * p * (3 - 2 * p);
  }
}
