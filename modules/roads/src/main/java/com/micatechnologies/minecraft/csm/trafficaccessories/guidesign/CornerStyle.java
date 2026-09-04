package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum CornerStyle {
  ROUND("Round"),
  SHARP("Sharp");

  private final String friendlyName;

  CornerStyle(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public CornerStyle next() {
    CornerStyle[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static CornerStyle fromOrdinal(int value) {
    CornerStyle[] vals = values();
    if (value < 0 || value >= vals.length) {
      return ROUND;
    }
    return vals[value];
  }
}
