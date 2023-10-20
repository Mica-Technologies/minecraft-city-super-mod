package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Enum representing the different modes of the signal configuration tool item and corresponding
 * friendly names.
 *
 * @author Mica Technologies
 * @version 1.0
 */
public enum ItemSignalConfigurationToolMode {
  CYCLE_SIGNAL_COLORS("Change Signal Color/State"),
  REORIENT_SENSOR("Reorient Sensor"),
  TOGGLE_CONTROLLER_NIGHTLY_FLASH_SETTING("Toggle Controller Nightly Flash Setting"),
  TOGGLE_CONTROLLER_POWER_LOSS_FLASH_SETTING("Toggle Controller Power Loss Flash Setting"),
  TOGGLE_CONTROLLER_OVERLAP_PEDESTRIAN_SIGNALS_SETTING(
      "Toggle Controller Overlap Pedestrian Signals Setting"),
  CLEAR_CONTROLLER_FAULTS("Clear Controller Faults"),
  CREATE_SIGNAL_OVERLAPS("Create Signal Overlaps");

  /**
   * The friendly name for the {@link ItemSignalConfigurationToolMode} enum value.
   *
   * @since 1.0
   */
  private final String friendlyName;

  /**
   * Constructs a new {@link ItemSignalConfigurationToolMode} enum value with the given friendly
   * name.
   *
   * @param friendlyName The friendly name for the {@link ItemSignalConfigurationToolMode} enum
   *                     value.
   *
   * @since 1.0
   */
  ItemSignalConfigurationToolMode(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  /**
   * Gets the friendly name for the {@link ItemSignalConfigurationToolMode} enum value.
   *
   * @return The friendly name for the {@link ItemSignalConfigurationToolMode} enum value.
   *
   * @since 1.0
   */
  public String getFriendlyName() {
    return friendlyName;
  }
}
