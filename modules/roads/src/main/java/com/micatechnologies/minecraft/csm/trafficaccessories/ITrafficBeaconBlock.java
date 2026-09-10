package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * Interface for traffic beacon blocks that should render a periodic strobe flash via
 * {@link TileEntityTrafficBeaconRenderer}. Provides the beacon lens geometry and color.
 */
public interface ITrafficBeaconBlock {

  float[] getBeaconLensFrom();

  float[] getBeaconLensTo();

  float getBeaconColorR();

  float getBeaconColorG();

  float getBeaconColorB();

  /**
   * Gets the length of one full flash cycle in milliseconds, or {@code 0} to keep the
   * emergency-style double flash the traffic beacons have always used.
   *
   * <p>A work zone warning light is not a strobe and not a signal head: it is one short pulse a
   * second, inside the 55 to 75 flashes a minute a Type A light is specified at. What makes it
   * read as that rather than as a blinking lamp is the DUTY CYCLE — the pulse is a small
   * fraction of the second, not half of it.</p>
   *
   * @return the cycle length in milliseconds, or {@code 0} for the default double flash
   *
   * @since 2026.9
   */
  default long getBeaconCycleMillis() {
    return 0L;
  }

  /**
   * Gets how long the beacon is at full brightness within each cycle, in milliseconds.
   *
   * @return the lit time in milliseconds
   *
   * @see #getBeaconCycleMillis()
   * @since 2026.9
   */
  default long getBeaconPulseMillis() {
    return 90L;
  }

  /**
   * Gets how long the beacon takes to fade out after its pulse, in milliseconds.
   *
   * <p>A filament warning light does not switch off instantly, and a hard cut reads as a
   * flickering texture rather than as a lamp.</p>
   *
   * @return the fade time in milliseconds
   *
   * @see #getBeaconCycleMillis()
   * @since 2026.9
   */
  default long getBeaconFadeMillis() {
    return 70L;
  }
}
