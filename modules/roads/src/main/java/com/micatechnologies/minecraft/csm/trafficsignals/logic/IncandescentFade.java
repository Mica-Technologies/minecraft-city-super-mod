package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Arrays;

/**
 * How bright an incandescent signal lamp is while it switches: a filament takes time to heat up
 * and longer to cool down, where an LED is on or off within microseconds.
 *
 * <p>The numbers come from measured signal lamps. A 12 V automotive signal bulb rises in about
 * 140 ms (0 to 90% in up to 250 ms) and decays in about 120 ms (Agilent/Broadcom application note
 * AN 1155-3, "LED Stop Lamps Help Reduce the Number and Severity of Automobile Accidents"). A
 * 120 V traffic lamp's filament is thinner, so a little quicker. Both directions are modelled as
 * exponentials, the heating one faster: a lamp is driven while it heats, and only radiates while
 * it cools. {@link #RISE_TAU_MILLIS} puts the lamp at 90% about 115 ms after power is applied,
 * {@link #DECAY_TAU_MILLIS} at 10% about 205 ms after it is removed.</p>
 *
 * <p>Brightness is continuous: a lamp switched again mid-fade (a fast flasher, a phase that ends
 * at once) starts from where it was rather than jumping to fully on or off.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class IncandescentFade {

  /** Time constant of the filament heating, in milliseconds. */
  public static final float RISE_TAU_MILLIS = 50.0f;

  /** Time constant of the filament cooling, in milliseconds. */
  public static final float DECAY_TAU_MILLIS = 90.0f;

  /** Within this of fully on or off a lamp is drawn as settled, so the fade quad is skipped. */
  public static final float SETTLED = 0.01f;

  /**
   * A change seen after this long without a frame is drawn as already settled. A head that was
   * off screen (or behind the player) while it changed would otherwise fade the moment it came
   * back into view, long after the lamp actually switched.
   */
  public static final long STALE_MILLIS = 250L;

  private IncandescentFade() {
  }

  /** Brightness {@code millis} after power was applied to a lamp that was at {@code from}. */
  public static float rise(float from, long millis) {
    return 1.0f - (1.0f - from) * (float) Math.exp(-millis / RISE_TAU_MILLIS);
  }

  /** Brightness {@code millis} after power was removed from a lamp that was at {@code from}. */
  public static float decay(float from, long millis) {
    return from * (float) Math.exp(-millis / DECAY_TAU_MILLIS);
  }

  /**
   * One head's lamps, as the renderer last saw them. Client only and never saved: it holds only
   * what the frames have shown, so a reloaded head simply starts settled.
   */
  public static final class Tracker {

    private int litMask;
    private long lastSeenMillis;
    private boolean seen;
    private long[] changedAt = new long[0];
    private float[] fromLevel = new float[0];

    /**
     * Records the lamps a frame shows. Call once per frame, before {@link #brightness}.
     *
     * @param litMask  bit {@code i} set when section {@code i} is lit
     * @param sections the head's section count
     * @param now      the frame's clock, in milliseconds
     */
    public void observe(int litMask, int sections, long now) {
      if (changedAt.length != sections) {
        changedAt = new long[sections];
        fromLevel = new float[sections];
        Arrays.fill(changedAt, Long.MIN_VALUE);
        seen = false;
      }
      boolean fresh = seen && now >= lastSeenMillis && now - lastSeenMillis <= STALE_MILLIS;
      int changed = seen ? (litMask ^ this.litMask) : 0;
      for (int i = 0; i < sections && changed != 0; i++) {
        if ((changed & (1 << i)) == 0) {
          continue;
        }
        if (fresh) {
          // Where the lamp was a moment ago, in the state it is leaving
          fromLevel[i] = brightness(i, (this.litMask & (1 << i)) != 0, now);
          changedAt[i] = now;
        } else {
          changedAt[i] = Long.MIN_VALUE;
        }
      }
      this.litMask = litMask;
      lastSeenMillis = now;
      seen = true;
    }

    /**
     * How bright section {@code section}'s lamp is now, 0 (cold) to 1 (fully on).
     *
     * @param lit whether the section is switched on
     */
    public float brightness(int section, boolean lit, long now) {
      if (section >= changedAt.length || changedAt[section] == Long.MIN_VALUE
          || now < changedAt[section]) {
        return lit ? 1.0f : 0.0f;
      }
      long since = now - changedAt[section];
      float level = lit ? rise(fromLevel[section], since) : decay(fromLevel[section], since);
      if (lit ? level >= 1.0f - SETTLED : level <= SETTLED) {
        // Settled: stop timing it, so later frames take the fast path above
        changedAt[section] = Long.MIN_VALUE;
        return lit ? 1.0f : 0.0f;
      }
      return level;
    }
  }
}
