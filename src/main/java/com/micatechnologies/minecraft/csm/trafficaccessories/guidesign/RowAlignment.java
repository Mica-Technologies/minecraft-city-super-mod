package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum RowAlignment {
  LEFT("Left"),
  CENTER("Center"),
  RIGHT("Right");

  private final String friendlyName;

  RowAlignment(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public RowAlignment next() {
    RowAlignment[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static RowAlignment fromOrdinal(int value) {
    RowAlignment[] vals = values();
    if (value < 0 || value >= vals.length) {
      return CENTER;
    }
    return vals[value];
  }
}
