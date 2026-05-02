package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum GuideSignShieldType {
  INTERSTATE(0, 0, "Interstate"),
  INTERSTATE_BUSINESS(1, 0, "Interstate Business"),
  US_ROUTE(2, 0, "US Route"),
  STATE_SQUARE(3, 0, "State Route (Square)"),
  STATE_CIRCLE(4, 0, "State Route (Circle)"),
  COUNTY_ROUTE(5, 0, "County Route"),
  TOLL(6, 0, "Toll Route"),
  BLANK_CUSTOM(7, 0, "Blank/Custom"),

  // State-specific markers. Atlas row 1 holds 8, row 2 holds the rest.
  CALIFORNIA(0, 1, "California"),
  TEXAS(1, 1, "Texas"),
  FLORIDA(2, 1, "Florida"),
  NEW_YORK(3, 1, "New York"),
  CONNECTICUT(4, 1, "Connecticut"),
  MASSACHUSETTS(5, 1, "Massachusetts"),
  MAINE(6, 1, "Maine"),
  NEW_HAMPSHIRE(7, 1, "New Hampshire"),
  RHODE_ISLAND(0, 2, "Rhode Island"),
  VERMONT(1, 2, "Vermont");

  private final int atlasCol;
  private final int atlasRow;
  private final String friendlyName;

  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName) {
    this.atlasCol = atlasCol;
    this.atlasRow = atlasRow;
    this.friendlyName = friendlyName;
  }

  public int getAtlasCol() {
    return atlasCol;
  }

  public int getAtlasRow() {
    return atlasRow;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public GuideSignShieldType next() {
    GuideSignShieldType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public GuideSignShieldType prev() {
    GuideSignShieldType[] vals = values();
    return vals[(ordinal() - 1 + vals.length) % vals.length];
  }

  public static GuideSignShieldType fromOrdinal(int value) {
    GuideSignShieldType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return INTERSTATE;
    }
    return vals[value];
  }
}
