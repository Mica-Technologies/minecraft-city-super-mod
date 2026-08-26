package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

/**
 * Where a slot sits vertically inside the blade's content band. Real blades put the block
 * number hard against the top or bottom edge far more often than centered, which is why
 * this exists separately from {@link StreetSignSlotPosition}.
 *
 * <p><b>Append-only: ordinals are serialized</b> in the sign's JSON.
 */
public enum StreetSignVerticalPos {
  TOP("Top"),
  MIDDLE("Middle"),
  BOTTOM("Bottom");

  private final String friendlyName;

  StreetSignVerticalPos(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public StreetSignVerticalPos next() {
    StreetSignVerticalPos[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static StreetSignVerticalPos fromOrdinal(int value) {
    StreetSignVerticalPos[] vals = values();
    if (value < 0 || value >= vals.length) {
      return MIDDLE;
    }
    return vals[value];
  }
}
