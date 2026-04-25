package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum PostType {
  LEFT("Left"),
  RIGHT("Right"),
  CENTER("Center"),
  OVERHEAD("Overhead"),
  RURAL("Rural");

  private final String friendlyName;

  PostType(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public PostType next() {
    PostType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static PostType fromOrdinal(int value) {
    PostType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return OVERHEAD;
    }
    return vals[value];
  }
}
