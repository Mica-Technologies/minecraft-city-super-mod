package com.micatechnologies.minecraft.csm.lifesafety;

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
