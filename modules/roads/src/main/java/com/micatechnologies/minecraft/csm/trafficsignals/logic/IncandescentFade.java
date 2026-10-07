package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Arrays;

/**
 * How bright an incandescent signal lamp looks while it switches: a filament takes time to heat up
 * and glows on after it is switched off, where an LED is on or off within microseconds.
 *
 * <p>The lamp is modelled as its filament's temperature, not as a brightness. With the temperature
 * {@code θ} as a fraction of its operating temperature, heating is
 * {@code dθ/dt = k (θ^-1.2 - θ^4) / c(θ)} and cooling {@code dθ/dt = -k θ^4 / c(θ)}: the power in
 * is {@code V²/R} with tungsten's resistance growing as {@code T^1.2} (so a cold filament draws
 * about fourteen times its running current), the power out is radiation, and the heat capacity
 * {@code c(θ)} grows with temperature. The light the eye gets through a lens of wavelength
 * {@code λ} then follows Wien's law, {@code exp(c2/(λ T_op) (1 - 1/θ))}.</p>
 *
 * <p>That is what gives a filament its look, which no pair of exponentials has. Switched on,
 * nothing shows for the first fifth of the rise while the filament is still below red heat, then
 * the light climbs steeply to full. Switched off, the light halves within a frame or so and then
 * lingers as a dim glow for a couple of hundred milliseconds. A green lens dies first and a red
 * one last, since a cooling filament loses its short wavelengths soonest.</p>
 *
 * <p>Brightness is drawn through the framebuffer's gamma: both the lens blend and the visor tint
 * mix encoded colour values, so the display level is the light raised to {@code 1/2.2}. Drawing
 * the light itself as the blend made the glow vanish too early and the whole fade read as a
 * crossfade.</p>
 *
 * <p>The time scale is set by how long the lamp takes to reach 90% through a red lens,
 * {@link #DEFAULT_RISE_90_MILLIS}: a 120 V traffic lamp, whose filament is thinner than a 12 V
 * automotive bulb's (discernible light after about 50 ms, 90% after about 250 ms) and which, like
 * a 230 V 100 W lamp, settles to its running current within roughly 50 ms. It is a judgement
 * within that range, and {@code /csm incandescent} changes it at run time. The decay is not set
 * separately: it follows from the same filament.</p>
 *
 * <p>Brightness is continuous: a lamp switched again mid-fade (a fast flasher, a phase that ends
 * at once) carries on from the temperature it had rather than jumping to fully on or off.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class IncandescentFade {

  /** Default time for a lamp to reach 90% through a red lens, in milliseconds. */
  public static final float DEFAULT_RISE_90_MILLIS = 120.0f;

  /** Within this of fully on or off (display level) a lamp is drawn as settled. */
  public static final float SETTLED = 0.01f;

  /**
   * A change seen after this long without a frame is drawn as already settled. A head that was
   * off screen (or behind the player) while it changed would otherwise fade the moment it came
   * back into view, long after the lamp actually switched.
   */
  public static final long STALE_MILLIS = 250L;

  /** The filament's operating temperature, in kelvin: a long-life traffic lamp runs cool. */
  static final double OPERATING_KELVIN = 2650.0;

  /** Second radiation constant, in micrometre kelvin. */
  private static final double C2 = 14388.0;

  /** Room temperature as a fraction of the operating temperature. */
  static final float AMBIENT = (float) (300.0 / OPERATING_KELVIN);

  /** Display gamma the blend is drawn through. */
  private static final double GAMMA = 2.2;

  /** Table step, in model milliseconds (the model at {@link #MODEL_K}). */
  private static final float STEP_MILLIS = 1.0f;

  /** The rate the tables are integrated at, per second; run-time speed scales their time. */
  private static final double MODEL_K = 7.0;

  /** Filament temperature heating from cold, one entry per model millisecond. */
  private static final float[] HEATING;

  /** Filament temperature cooling from fully on, one entry per model millisecond. */
  private static final float[] COOLING;

  /** Model milliseconds for a cold lamp to reach 90% through a red lens. */
  private static final float MODEL_RISE_90_MILLIS;

  /** At or above this temperature every lens shows within {@link #SETTLED} of fully on. */
  static final float SETTLED_ON;

  /** At or below this temperature every lens shows within {@link #SETTLED} of dark. */
  static final float SETTLED_OFF;

  private static volatile float rise90Millis = DEFAULT_RISE_90_MILLIS;

  static {
    HEATING = integrate(true);
    COOLING = integrate(false);
    float red90 = HEATING.length;
    double redX = wienExponent(TrafficSignalBulbColor.RED);
    for (int i = 0; i < HEATING.length; i++) {
      if (Math.exp(redX * (1.0 - 1.0 / HEATING[i])) >= 0.9) {
        red90 = i * STEP_MILLIS;
        break;
      }
    }
    MODEL_RISE_90_MILLIS = red90;
    // Settled on: the bluest lens (the steepest) within SETTLED of full; off: the reddest lens
    // (the slowest to go dark) within SETTLED of black
    double lnOn = GAMMA * Math.log(1.0 - SETTLED);
    double lnOff = GAMMA * Math.log(SETTLED);
    SETTLED_ON = (float) (1.0 / (1.0 - lnOn / wienExponent(TrafficSignalBulbColor.GREEN)));
    SETTLED_OFF = (float) (1.0 / (1.0 - lnOff / redX));
  }

  private IncandescentFade() {
  }

  /** Sets how long a lamp takes to reach 90% through a red lens, in milliseconds. */
  public static void setRise90Millis(float millis) {
    rise90Millis = Math.max(10.0f, Math.min(2000.0f, millis));
  }

  /** How long a lamp takes to reach 90% through a red lens, in milliseconds. */
  public static float getRise90Millis() {
    return rise90Millis;
  }

  /** Model milliseconds per real millisecond at the current speed. */
  private static float timeScale() {
    return MODEL_RISE_90_MILLIS / rise90Millis;
  }

  private static double wienExponent(TrafficSignalBulbColor colour) {
    double micrometres;
    switch (colour == null ? TrafficSignalBulbColor.YELLOW : colour) {
      case RED:
        micrometres = 0.625;
        break;
      case GREEN:
        micrometres = 0.505;
        break;
      case YELLOW:
      default:
        micrometres = 0.592;
        break;
    }
    return C2 / (micrometres * OPERATING_KELVIN);
  }

  /** Tungsten's heat capacity as a fraction of its value at the operating temperature. */
  private static double heatCapacity(double theta) {
    return 0.7 + 0.3 * theta;
  }

  private static float[] integrate(boolean heating) {
    float[] out = new float[1200];
    double theta = heating ? AMBIENT : 1.0;
    int sub = 50;
    double dt = STEP_MILLIS / 1000.0 / sub;
    for (int i = 0; i < out.length; i++) {
      out[i] = (float) theta;
      for (int s = 0; s < sub; s++) {
        double in = heating ? Math.pow(theta, -1.2) : 0.0;
        theta += MODEL_K * (in - theta * theta * theta * theta) / heatCapacity(theta) * dt;
      }
    }
    return out;
  }

  /** Where in {@code table} (model milliseconds) the temperature passes {@code theta}. */
  private static float timeAt(float[] table, boolean rising, float theta) {
    int lo = 0;
    int hi = table.length - 1;
    if (rising ? theta <= table[0] : theta >= table[0]) {
      return 0.0f;
    }
    if (rising ? theta >= table[hi] : theta <= table[hi]) {
      return hi * STEP_MILLIS;
    }
    while (hi - lo > 1) {
      int mid = (lo + hi) >>> 1;
      if (rising ? table[mid] < theta : table[mid] > theta) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    float span = table[hi] - table[lo];
    float f = span == 0.0f ? 0.0f : (theta - table[lo]) / span;
    return (lo + f) * STEP_MILLIS;
  }

  private static float sample(float[] table, float modelMillis) {
    float pos = modelMillis / STEP_MILLIS;
    if (pos >= table.length - 1) {
      return table[table.length - 1];
    }
    int i = (int) pos;
    float f = pos - i;
    return table[i] + (table[i + 1] - table[i]) * f;
  }

  /** Filament temperature {@code millis} after power was applied to a lamp at {@code from}. */
  public static float heat(float from, long millis) {
    float t0 = timeAt(HEATING, true, from);
    return sample(HEATING, t0 + millis * timeScale());
  }

  /** Filament temperature {@code millis} after power was removed from a lamp at {@code from}. */
  public static float cool(float from, long millis) {
    float t0 = timeAt(COOLING, false, from);
    return sample(COOLING, t0 + millis * timeScale());
  }

  /**
   * How bright a filament at temperature {@code theta} shows through a lens of {@code colour}, as
   * the display level the blend is drawn at: 0 dark, 1 fully on.
   */
  public static float displayLevel(float theta, TrafficSignalBulbColor colour) {
    if (theta >= 1.0f) {
      return 1.0f;
    }
    if (theta <= AMBIENT) {
      return 0.0f;
    }
    double light = Math.exp(wienExponent(colour) * (1.0 - 1.0 / theta));
    return (float) Math.pow(light, 1.0 / GAMMA);
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
    private float[] fromTheta = new float[0];

    /**
     * Records the lamps a frame shows. Call once per frame, before {@link #temperature}.
     *
     * @param litMask  bit {@code i} set when section {@code i} is lit
     * @param sections the head's section count
     * @param now      the frame's clock, in milliseconds
     */
    public void observe(int litMask, int sections, long now) {
      if (changedAt.length != sections) {
        changedAt = new long[sections];
        fromTheta = new float[sections];
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
          // Where the filament was a moment ago, in the state it is leaving
          fromTheta[i] = temperature(i, (this.litMask & (1 << i)) != 0, now);
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
     * Section {@code section}'s filament temperature now, as a fraction of its operating
     * temperature: {@link #AMBIENT} cold, 1 fully on. Settled lamps answer exactly 1 or
     * {@link #AMBIENT}.
     *
     * @param lit whether the section is switched on
     */
    public float temperature(int section, boolean lit, long now) {
      if (section >= changedAt.length || changedAt[section] == Long.MIN_VALUE
          || now < changedAt[section]) {
        return lit ? 1.0f : AMBIENT;
      }
      long since = now - changedAt[section];
      float theta = lit ? heat(fromTheta[section], since) : cool(fromTheta[section], since);
      if (lit ? theta >= SETTLED_ON : theta <= SETTLED_OFF) {
        // Settled: stop timing it, so later frames take the fast path above
        changedAt[section] = Long.MIN_VALUE;
        return lit ? 1.0f : AMBIENT;
      }
      return theta;
    }

    /**
     * How bright section {@code section} shows through its lens now, as a display level: 0 dark,
     * 1 fully on. Settled lamps answer exactly 0 or 1.
     */
    public float brightness(int section, boolean lit, long now, TrafficSignalBulbColor colour) {
      float theta = temperature(section, lit, now);
      if (theta == 1.0f) {
        return 1.0f;
      }
      if (theta == AMBIENT) {
        return 0.0f;
      }
      return displayLevel(theta, colour);
    }
  }
}
