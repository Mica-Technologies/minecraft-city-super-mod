package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Enum representing the configurable properties of a traffic signal controller that can be
 * cycled via the signal controller configuration GUI.
 */
public enum SignalControllerConfigAction {
  SWITCH_MODE,
  CYCLE_YELLOW_TIME,
  CYCLE_ALL_RED_TIME,
  CYCLE_FLASH_DONT_WALK_TIME,
  CYCLE_DEDICATED_PED_SIGNAL_TIME,
  CYCLE_MIN_GREEN_TIME,
  CYCLE_MAX_GREEN_TIME,
  CYCLE_MIN_GREEN_TIME_SECONDARY,
  CYCLE_MAX_GREEN_TIME_SECONDARY,
  CYCLE_LPI_TIME,
  TOGGLE_NIGHTLY_FLASH,
  TOGGLE_POWER_LOSS_FLASH,
  TOGGLE_OVERLAP_PED_SIGNALS,
  TOGGLE_ALL_RED_FLASH,
  CYCLE_RAMP_METER_NIGHT_MODE,
  CLEAR_FAULTS
}
