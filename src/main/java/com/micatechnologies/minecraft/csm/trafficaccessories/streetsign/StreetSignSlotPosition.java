package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

/**
 * Which end of the blade an optional slot (block number, emblem, arrow) sits on, or
 * {@link #NONE} when the slot is switched off entirely.
 *
 * <p><b>Append-only: ordinals are serialized</b> in the sign's JSON.
 */
public enum StreetSignSlotPosition {
  NONE("Off"),
  LEFT("Left"),
  RIGHT("Right");

  private final String friendlyName;

  StreetSignSlotPosition(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public boolean isShown() {
    return this != NONE;
  }

  public StreetSignSlotPosition next() {
    StreetSignSlotPosition[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static StreetSignSlotPosition fromOrdinal(int value) {
    StreetSignSlotPosition[] vals = values();
    if (value < 0 || value >= vals.length) {
      return NONE;
    }
    return vals[value];
  }
}
