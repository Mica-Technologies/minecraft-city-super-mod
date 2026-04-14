package com.micatechnologies.minecraft.csm.hvac;

/**
 * Marker interface for HVAC tile entities that influence chunk temperature. Implemented by heater
 * and cooler tile entities to participate in the {@link HvacTemperatureManager} temperature
 * calculation system.
 *
 * @author Mica Technologies
 * @see HvacTemperatureManager
 * @since 2026.4
 */
public interface IHvacUnit {

  /**
   * Returns the temperature offset in degrees Fahrenheit that this unit contributes when active.
   * Positive values indicate heating, negative values indicate cooling.
   *
   * @return the temperature contribution in degrees Fahrenheit
   */
  float getTemperatureContribution();

  /**
   * Returns whether this HVAC unit is currently active (powered and operating).
   *
   * @return {@code true} if this unit is currently active, {@code false} otherwise
   */
  boolean isHvacActive();

  /**
   * Returns the effective radius of this unit in blocks. The unit will only influence temperature
   * calculations within this radius of its position.
   *
   * @return the effective radius in blocks
   */
  int getEffectiveRadius();

  /**
   * Returns the maximum distance in blocks at which a vent relay can be linked to this unit.
   *
   * @return the maximum vent link distance in blocks
   */
  default int getMaxVentLinkDistance() {
    return 30;
  }

  /**
   * Returns the temperature contribution that a vent relay linked to this unit should provide.
   * This is typically weaker than the unit's own direct contribution.
   *
   * @return the vent relay temperature contribution in degrees Fahrenheit
   */
  default float getVentRelayContribution() {
    return getTemperatureContribution() > 0 ? 15.0f : -15.0f;
  }
}
