package com.micatechnologies.minecraft.csm.hvac;

/**
 * Per-consumer asymmetric EMA smoother for HVAC temperature offsets. Tracks an EMA of the
 * HVAC contribution to the local temperature so a display value can rise quickly when the
 * HVAC starts pushing and decay slowly when it stops — mimicking thermal mass without ever
 * sampling stale state from another caller. Each consumer (the HUD overlay, future
 * non-thermostat indicators, etc.) should own one of these and feed it the latest raw
 * reading from {@link HvacTemperatureManager#getTemperatureAt}; the smoother is the side
 * that holds the smoothing state, not the manager.
 *
 * <p>Previously this lived as a static field on {@code HvacTemperatureManager}, shared
 * across every caller and every thread. That worked while the HUD was the only smoothing
 * consumer, but it (a) silently corrupted the smoothed value when a server-side thermostat
 * accidentally called the smoothing variant, and (b) made it impossible to have more than
 * one logical smoother. Moving the state into an instance fixes both.</p>
 *
 * <p>Each {@link #update(float, float)} call takes the freshly read temperature and the
 * baseline at the same position. The smoother tracks the offset (temperature − baseline)
 * rather than the absolute temperature so it doesn't have to be reset when the baseline
 * shifts (e.g. the player walks across a biome boundary).</p>
 */
public class TemperatureSmoother {

  /**
   * EMA factor when HVAC is actively pushing the temperature away from baseline. Fast
   * enough to track the instantaneous offset in ~10-15 seconds, keeping the display
   * responsive.
   */
  private static final float RAMP_FACTOR = 0.08f;

  /**
   * EMA factor when the player leaves an HVAC zone entirely (raw offset ≈ 0, smoothed
   * still significant). Converges to baseline in ~20-30 seconds, simulating the transition
   * from a conditioned space to the outdoors.
   */
  private static final float TRANSITION_FACTOR = 0.04f;

  /**
   * EMA factor when HVAC is still present but reduced (residual decay in progress). Much
   * slower than the transition factor to simulate the room holding its temperature while
   * the conditioned air lingers.
   */
  private static final float DECAY_FACTOR = 0.003f;

  /** Set to NaN until the first {@link #update}, which seeds it directly. */
  private float smoothedOffset = Float.NaN;

  /**
   * Feeds the smoother a fresh reading and returns the smoothed temperature.
   *
   * @param rawTemperature the freshly calculated raw temperature at the query position
   * @param baseline       the biome baseline temperature at the same position
   * @return the smoothed temperature (baseline + smoothed offset)
   */
  public float update(float rawTemperature, float baseline) {
    float currentOffset = rawTemperature - baseline;

    if (Float.isNaN(smoothedOffset)) {
      smoothedOffset = currentOffset;
      return rawTemperature;
    }

    // Four-rate smoothing:
    // 1. RAMP: direction crossed heating↔cooling, or HVAC actively pushing — track quickly
    // 2. TRANSITION: raw ≈ 0, smoothed significant — left zone, moderate decay
    // 3. TRANSITION: raw dropped by more than half — moved significantly away, moderate decay
    // 4. DECAY: small reduction within same space — thermal-mass holdover, very slow
    boolean directionChanged =
        (currentOffset > 0.5f && smoothedOffset < -0.5f)
            || (currentOffset < -0.5f && smoothedOffset > 0.5f);

    float factor;
    if (directionChanged) {
      factor = RAMP_FACTOR;
    } else if (Math.abs(currentOffset) < 0.5f && Math.abs(smoothedOffset) > 1.0f) {
      factor = TRANSITION_FACTOR;
    } else if (Math.abs(currentOffset) < Math.abs(smoothedOffset) * 0.5f) {
      factor = TRANSITION_FACTOR;
    } else if (Math.abs(currentOffset) + 0.5f < Math.abs(smoothedOffset)) {
      factor = DECAY_FACTOR;
    } else {
      factor = RAMP_FACTOR;
    }

    smoothedOffset += (currentOffset - smoothedOffset) * factor;
    return baseline + smoothedOffset;
  }

  /** Drops the current smoothing state so the next {@link #update} call seeds from raw. */
  public void reset() {
    smoothedOffset = Float.NaN;
  }
}
