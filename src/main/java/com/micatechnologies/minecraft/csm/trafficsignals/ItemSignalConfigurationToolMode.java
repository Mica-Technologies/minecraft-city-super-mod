package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Enum representing the different modes of the signal configuration tool item and corresponding
 * friendly names.
 *
 * @author Mica Technologies
 * @version 1.0
 */
public enum ItemSignalConfigurationToolMode {
  OPEN_GUI("Open Controller Config GUI"),
  CYCLE_SIGNAL_COLORS("Change Signal Color/State"),
  REORIENT_SENSOR("Reorient Sensor"),
  TOGGLE_CONTROLLER_NIGHTLY_FLASH_SETTING("Toggle Controller Nightly Flash Setting"),
  TOGGLE_CONTROLLER_POWER_LOSS_FLASH_SETTING("Toggle Controller Power Loss Flash Setting"),
  TOGGLE_CONTROLLER_OVERLAP_PEDESTRIAN_SIGNALS_SETTING(
      "Toggle Controller Overlap Pedestrian Signals Setting"),
  CYCLE_CONTROLLER_LPI_SETTING("Cycle Controller Lead Pedestrian Interval Setting"),
  CYCLE_CONTROLLER_YELLOW_TIME("Cycle Controller Yellow Time"),
  CYCLE_CONTROLLER_ALL_RED_TIME("Cycle Controller All Red Time"),
  CYCLE_CONTROLLER_FLASH_DONT_WALK_TIME("Cycle Controller Ped Clearance Time"),
  CYCLE_CONTROLLER_DEDICATED_PED_SIGNAL_TIME("Cycle Controller Ped Signal Time"),
  CYCLE_CONTROLLER_MIN_GREEN_TIME("Cycle Controller Min Green Time"),
  CYCLE_CONTROLLER_MAX_GREEN_TIME("Cycle Controller Max Green Time"),
  CYCLE_CONTROLLER_MIN_GREEN_TIME_SECONDARY("Cycle Controller Min Green Time (Secondary)"),
  CYCLE_CONTROLLER_MAX_GREEN_TIME_SECONDARY("Cycle Controller Max Green Time (Secondary)"),
  TOGGLE_CONTROLLER_ALL_RED_FLASH("Toggle Controller All Red Flash"),
  CYCLE_CONTROLLER_RAMP_METER_NIGHT_MODE("Cycle Controller Ramp Meter Night Mode"),
  CLEAR_CONTROLLER_FAULTS("Clear Controller Faults"),
  CREATE_SIGNAL_OVERLAPS("Create Signal Overlaps"),
  CHANGE_APS_ARROW_DIRECTION("Change APS Arrow Direction");

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
