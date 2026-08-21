package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

/**
 * Where a shield element's banner word sits relative to the shield: above it (the
 * default, in the row's reserved banner zone) or beside it (vertically centered,
 * widening the element instead of raising the row).
 */
public enum BannerPosition {
  ABOVE("Above"),
  LEFT("Left"),
  RIGHT("Right");

  private final String friendlyName;

  BannerPosition(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public BannerPosition next() {
    BannerPosition[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static BannerPosition fromOrdinal(int value) {
    BannerPosition[] vals = values();
    if (value < 0 || value >= vals.length) {
      return ABOVE;
    }
    return vals[value];
  }
}
