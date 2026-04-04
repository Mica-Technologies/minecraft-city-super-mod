package com.micatechnologies.minecraft.csm.trafficsignals;

public enum ItemSignalHeadConfigToolMode {
  OPEN_GUI("Open Config GUI"),
  CYCLE_BODY_COLOR("Cycle Body Color"),
  CYCLE_DOOR_COLOR("Cycle Door Color"),
  CYCLE_VISOR_COLOR("Cycle Visor Color"),
  CYCLE_VISOR_TYPE("Cycle Visor Type"),
  CYCLE_BODY_TILT("Cycle Body Tilt"),
  CYCLE_BULB_STYLE("Cycle Bulb Style"),
  CYCLE_BULB_TYPE("Cycle Bulb Type"),
  CYCLE_SIGNAL_COLOR("Cycle Signal Color"),
  TOGGLE_ALTERNATE_FLASH("Toggle Alternate Flash"),
  CYCLE_MOUNT_TYPE("Cycle Mount Type");

  private final String friendlyName;

  ItemSignalHeadConfigToolMode(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }
}
