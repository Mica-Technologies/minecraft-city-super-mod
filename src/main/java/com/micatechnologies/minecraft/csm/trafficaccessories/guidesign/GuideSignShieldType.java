package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum GuideSignShieldType {
  INTERSTATE(0, 0, "Interstate", 0xFFFFFF),
  INTERSTATE_BUSINESS(1, 0, "Interstate Business", 0xFFFFFF),
  US_ROUTE(2, 0, "US Route", 0x101010),
  STATE_SQUARE(3, 0, "State Route (Square)", 0x101010),
  STATE_CIRCLE(4, 0, "State Route (Circle)", 0x101010),
  COUNTY_ROUTE(5, 0, "County Route", 0xF7D117),
  TOLL(6, 0, "Toll Route", 0xFFFFFF),
  BLANK_CUSTOM(7, 0, "Blank/Custom", 0x101010),

  // State-specific markers. Atlas row 1 holds 8, row 2 holds the rest.
  // All current state silhouettes use dark fills, so white route text.
  CALIFORNIA(0, 1, "California", 0xFFFFFF),
  TEXAS(1, 1, "Texas", 0xFFFFFF),
  FLORIDA(2, 1, "Florida", 0xFFFFFF),
  NEW_YORK(3, 1, "New York", 0xFFFFFF),
  CONNECTICUT(4, 1, "Connecticut", 0xFFFFFF),
  MASSACHUSETTS(5, 1, "Massachusetts", 0xFFFFFF),
  MAINE(6, 1, "Maine", 0xFFFFFF),
  NEW_HAMPSHIRE(7, 1, "New Hampshire", 0xFFFFFF),
  RHODE_ISLAND(0, 2, "Rhode Island", 0xFFFFFF),
  VERMONT(1, 2, "Vermont", 0xFFFFFF);

  private final int atlasCol;
  private final int atlasRow;
  private final String friendlyName;
  private final int routeTextColor;

  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor) {
    this.atlasCol = atlasCol;
    this.atlasRow = atlasRow;
    this.friendlyName = friendlyName;
    this.routeTextColor = routeTextColor;
  }

  /**
   * Color for the route number drawn over this shield background, chosen to
   * contrast with the atlas cell's fill (black on white/light shields, white on
   * dark ones, yellow on the blue county pentagon per MUTCD).
   */
  public int getRouteTextColor() {
    return routeTextColor;
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
