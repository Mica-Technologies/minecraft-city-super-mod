package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * Enumeration of operating modes for the {@link ItemFireAlarmConfigTool}, defining the
 * available actions when using the tool on a fire alarm control panel.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public enum ItemFireAlarmConfigToolMode {
  OPEN_GUI("Open Panel Config GUI"),
  AUDIBLE_SILENCE("Audible Silence"),
  RESET_PANEL("Reset Panel"),
  CYCLE_VOICE_EVAC_SOUND("Cycle Voice Evac Sound");

  private final String friendlyName;

  ItemFireAlarmConfigToolMode(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }
}
