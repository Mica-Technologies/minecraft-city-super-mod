package com.micatechnologies.minecraft.csm.signage;

import java.util.List;
import java.util.Random;

/**
 * Which ads a board shows, and in what order: one ad, or every ad (or every ad in one category)
 * in turn, in order or shuffled, changing at the board's interval.
 *
 * <p>What is showing is worked out from the world time and the board's position alone, so every
 * client shows the same ad at the same moment with nothing ticking and nothing sent. The position
 * is also why two boards side by side do not show the same ad in step.</p>
 *
 * <p>The ordinal is stored in NBT and sent in packets: append only.</p>
 */
public enum AdRotation {
  /** One ad, always. */
  SINGLE(false, false),
  /** Every ad in turn, in the library's order. */
  ALL_IN_ORDER(false, false),
  /** Every ad, shuffled afresh each time round. */
  ALL_SHUFFLED(false, true),
  /** Every ad in one category in turn. */
  CATEGORY_IN_ORDER(true, false),
  /** Every ad in one category, shuffled. */
  CATEGORY_SHUFFLED(true, true);

  /** The shortest interval between ads, in seconds. */
  public static final int MIN_INTERVAL = 5;
  /** The longest, ten minutes, in seconds. */
  public static final int MAX_INTERVAL = 600;

  private final boolean category;
  private final boolean shuffled;

  AdRotation(boolean category, boolean shuffled) {
    this.category = category;
    this.shuffled = shuffled;
  }

  /** The rotation with the given ordinal, or {@link #SINGLE} for anything out of range. */
  public static AdRotation fromOrdinal(int ordinal) {
    AdRotation[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SINGLE;
  }

  /** The next rotation, for a button that cycles. */
  public AdRotation next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /** Whether this rotation draws from one category rather than every ad. */
  public boolean usesCategory() {
    return category;
  }

  /** Whether the order is shuffled. */
  public boolean isShuffled() {
    return shuffled;
  }

  /** Clamps an interval, in seconds, to what a board allows. */
  public static int clampInterval(int seconds) {
    return Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, seconds));
  }

  /**
   * Which of {@code pool} is showing.
   *
   * @param pool      the ads this rotation draws from, in library order; must not be empty
   * @param worldTime the world's total time, in ticks
   * @param interval  seconds each ad is shown
   * @param seed      the board's own number, from its position
   *
   * @return the index in {@code pool} of the ad showing
   */
  public int select(List<?> pool, long worldTime, int interval, long seed) {
    int n = pool.size();
    if (this == SINGLE || n <= 1) {
      return 0;
    }
    long step = Math.max(0, worldTime) / (clampInterval(interval) * 20L);
    if (!shuffled) {
      return (int) Math.floorMod(step + seed, (long) n);
    }
    long round = step / n;
    int index = (int) (step % n);
    return shuffle(n, seed * 31 + round)[index];
  }

  /**
   * A permutation of {@code 0..n-1}, the same for the same seed on every machine: a Fisher-Yates
   * shuffle driven by {@link Random}, whose sequence Java fixes for a given seed.
   */
  static int[] shuffle(int n, long seed) {
    int[] order = new int[n];
    for (int i = 0; i < n; i++) {
      order[i] = i;
    }
    Random random = new Random(seed);
    for (int i = n - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int t = order[i];
      order[i] = order[j];
      order[j] = t;
    }
    return order;
  }
}
