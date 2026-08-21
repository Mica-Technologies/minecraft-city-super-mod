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
  VERMONT(1, 2, "Vermont", 0xFFFFFF),

  // Remaining 40 states. Ordinals are serialized in sign JSON — always append
  // new entries at the end, never reorder or insert. Atlas row 2 cols 2-7,
  // row 3 cols 0-7, rows 6-9 (in the taller 512x1024 atlas) hold these.
  ALABAMA(2, 2, "Alabama", 0xFFFFFF),
  ALASKA(3, 2, "Alaska", 0xFFFFFF),
  ARIZONA(4, 2, "Arizona", 0xFFFFFF),
  ARKANSAS(5, 2, "Arkansas", 0xFFFFFF),
  COLORADO(6, 2, "Colorado", 0x101010),
  DELAWARE(7, 2, "Delaware", 0xFFFFFF),
  GEORGIA(0, 3, "Georgia", 0xFFFFFF),
  HAWAII(1, 3, "Hawaii", 0xFFFFFF),
  IDAHO(2, 3, "Idaho", 0xFFFFFF),
  ILLINOIS(3, 3, "Illinois", 0xFFFFFF),
  INDIANA(4, 3, "Indiana", 0xFFFFFF),
  IOWA(5, 3, "Iowa", 0x101010),
  KANSAS(6, 3, "Kansas", 0x101010),
  KENTUCKY(7, 3, "Kentucky", 0xFFFFFF),
  LOUISIANA(0, 6, "Louisiana", 0xFFFFFF),
  MARYLAND(1, 6, "Maryland", 0xFFFFFF),
  MICHIGAN(2, 6, "Michigan", 0xFFFFFF),
  MINNESOTA(3, 6, "Minnesota", 0x101010),
  MISSISSIPPI(4, 6, "Mississippi", 0xFFFFFF),
  MISSOURI(5, 6, "Missouri", 0x101010),
  MONTANA(6, 6, "Montana", 0xFFFFFF),
  NEBRASKA(7, 6, "Nebraska", 0xFFFFFF),
  NEVADA(0, 7, "Nevada", 0xFFFFFF),
  NEW_JERSEY(1, 7, "New Jersey", 0xFFFFFF),
  NEW_MEXICO(2, 7, "New Mexico", 0xF7D117),
  NORTH_CAROLINA(3, 7, "North Carolina", 0x101010),
  NORTH_DAKOTA(4, 7, "North Dakota", 0x101010),
  OHIO(5, 7, "Ohio", 0x101010),
  OKLAHOMA(6, 7, "Oklahoma", 0xFFFFFF),
  OREGON(7, 7, "Oregon", 0xFFFFFF),
  PENNSYLVANIA(0, 8, "Pennsylvania", 0xFFFFFF),
  SOUTH_CAROLINA(1, 8, "South Carolina", 0xFFFFFF),
  SOUTH_DAKOTA(2, 8, "South Dakota", 0xFFFFFF),
  TENNESSEE(3, 8, "Tennessee", 0xFFFFFF),
  UTAH(4, 8, "Utah", 0x101010),
  VIRGINIA(5, 8, "Virginia", 0xFFFFFF),
  WASHINGTON(6, 8, "Washington", 0x101010),
  WEST_VIRGINIA(7, 8, "West Virginia", 0xFFFFFF),
  WISCONSIN(0, 9, "Wisconsin", 0xFFFFFF),
  WYOMING(1, 9, "Wyoming", 0xFFFFFF),

  // Washington DC + Canadian provinces. Ordinals are serialized in sign JSON —
  // always append new entries at the end, never reorder or insert. Atlas row 9
  // cols 2-7 holds the first six, row 10 col 0 holds the seventh.
  DISTRICT_OF_COLUMBIA(2, 9, "Washington DC", 0x101010),
  ONTARIO(3, 9, "Ontario", 0x101010),
  QUEBEC(4, 9, "Quebec", 0x101010),
  NEW_BRUNSWICK(5, 9, "New Brunswick", 0xFFFFFF),
  NOVA_SCOTIA(6, 9, "Nova Scotia", 0xFFFFFF),
  NEWFOUNDLAND(7, 9, "Newfoundland and Labrador", 0xFFFFFF),
  PRINCE_EDWARD_ISLAND(0, 10, "Prince Edward Island", 0xFFFFFF);

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
