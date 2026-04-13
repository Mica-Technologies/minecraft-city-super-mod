package com.micatechnologies.minecraft.csm.hvac;

/**
 * Interface for thermostat tile entities that can display temperature information via TESR.
 * Implemented by both {@link TileEntityHvacThermostat} (primary) and
 * {@link TileEntityHvacZoneThermostat} (zone) to share a common renderer.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public interface IHvacThermostatDisplay {

  /**
   * Returns the current (smoothed) temperature reading at this thermostat's position.
   */
  float getCurrentTemperature();

  /**
   * Returns the low setpoint in degrees Fahrenheit.
   */
  int getTargetTempLow();

  /**
   * Returns the high setpoint in degrees Fahrenheit.
   */
  int getTargetTempHigh();

  /**
   * Returns whether the thermostat is currently calling for heating or cooling.
   */
  boolean isCalling();
}
