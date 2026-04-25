package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum GuideSignBannerType {
  NONE("None"),
  NORTH("North"),
  SOUTH("South"),
  EAST("East"),
  WEST("West"),
  TO("To"),
  LOOP("Loop"),
  SPUR("Spur"),
  BUSINESS("Business"),
  TOLL("Toll"),
  ALTERNATE("Alternate"),
  BYPASS("Bypass"),
  CONNECTOR("Connector"),
  TRUCK("Truck");

  private final String friendlyName;

  GuideSignBannerType(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public String getBannerText() {
    if (this == NONE) {
      return "";
    }
    return friendlyName.toUpperCase();
  }

  public GuideSignBannerType next() {
    GuideSignBannerType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static GuideSignBannerType fromOrdinal(int value) {
    GuideSignBannerType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return NONE;
    }
    return vals[value];
  }
}
