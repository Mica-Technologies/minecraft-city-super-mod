package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

public enum GuideSignShieldType {
  INTERSTATE(0, 0, "Interstate", 0xFFFFFF, 0.62f),
  INTERSTATE_BUSINESS(1, 0, "Interstate Business", 0xFFFFFF, 0.62f),
  US_ROUTE(2, 0, "US Route", 0x101010, 0.62f),
  STATE_SQUARE(3, 0, "State Route (Square)", 0x101010, 0.62f),
  STATE_CIRCLE(4, 0, "State Route (Circle)", 0x101010, 0.62f),
  COUNTY_ROUTE(5, 0, "County Route", 0xF7D117, 0.6f),
  TOLL(6, 0, "Toll Route", 0xFFFFFF, 0.62f),
  BLANK_CUSTOM(7, 0, "Blank/Custom", 0x101010, 0.61f),

  // State-specific markers. Atlas row 1 holds 8, row 2 holds the rest.
  // All current state silhouettes use dark fills, so white route text.
  CALIFORNIA(0, 1, "California", 0xFFFFFF, 0.49f),
  TEXAS(1, 1, "Texas", 0xFFFFFF, 0.45f),
  FLORIDA(2, 1, "Florida", 0xFFFFFF, 0.3f),
  NEW_YORK(3, 1, "New York", 0xFFFFFF, 0.62f),
  CONNECTICUT(4, 1, "Connecticut", 0xFFFFFF, 0.62f),
  MASSACHUSETTS(5, 1, "Massachusetts", 0xFFFFFF, 0.62f),
  MAINE(6, 1, "Maine", 0xFFFFFF, 0.62f),
  NEW_HAMPSHIRE(7, 1, "New Hampshire", 0xFFFFFF, 0.62f),
  RHODE_ISLAND(0, 2, "Rhode Island", 0xFFFFFF, 0.54f),
  VERMONT(1, 2, "Vermont", 0xFFFFFF, 0.61f),

  // Remaining 40 states. Ordinals are serialized in sign JSON — always append
  // new entries at the end, never reorder or insert. Atlas row 2 cols 2-7,
  // row 3 cols 0-7, rows 6-9 (in the taller 512x1024 atlas) hold these.
  ALABAMA(2, 2, "Alabama", 0xFFFFFF, 0.62f),
  ALASKA(3, 2, "Alaska", 0xFFFFFF, 0.6f),
  ARIZONA(4, 2, "Arizona", 0xFFFFFF, 0.62f),
  ARKANSAS(5, 2, "Arkansas", 0xFFFFFF, 0.56f),
  COLORADO(6, 2, "Colorado", 0x101010, 0.62f),
  DELAWARE(7, 2, "Delaware", 0xFFFFFF, 0.62f),
  GEORGIA(0, 3, "Georgia", 0xFFFFFF, 0.62f),
  HAWAII(1, 3, "Hawaii", 0xFFFFFF, 0.62f),
  IDAHO(2, 3, "Idaho", 0xFFFFFF, 0.62f),
  ILLINOIS(3, 3, "Illinois", 0xFFFFFF, 0.62f),
  INDIANA(4, 3, "Indiana", 0xFFFFFF, 0.56f),
  IOWA(5, 3, "Iowa", 0x101010, 0.62f),
  KANSAS(6, 3, "Kansas", 0x101010, 0.62f),
  KENTUCKY(7, 3, "Kentucky", 0xFFFFFF, 0.6f),
  LOUISIANA(0, 6, "Louisiana", 0xFFFFFF, 0.56f),
  MARYLAND(1, 6, "Maryland", 0xFFFFFF, 0.6f),
  MICHIGAN(2, 6, "Michigan", 0xFFFFFF, 0.61f),
  MINNESOTA(3, 6, "Minnesota", 0x101010, 0.62f),
  MISSISSIPPI(4, 6, "Mississippi", 0xFFFFFF, 0.62f),
  MISSOURI(5, 6, "Missouri", 0x101010, 0.62f),
  MONTANA(6, 6, "Montana", 0xFFFFFF, 0.62f),
  NEBRASKA(7, 6, "Nebraska", 0xFFFFFF, 0.6f),
  NEVADA(0, 7, "Nevada", 0xFFFFFF, 0.56f),
  NEW_JERSEY(1, 7, "New Jersey", 0xFFFFFF, 0.62f),
  NEW_MEXICO(2, 7, "New Mexico", 0xF7D117, 0.62f),
  NORTH_CAROLINA(3, 7, "North Carolina", 0x101010, 0.56f),
  NORTH_DAKOTA(4, 7, "North Dakota", 0x101010, 0.62f),
  OHIO(5, 7, "Ohio", 0x101010, 0.62f),
  OKLAHOMA(6, 7, "Oklahoma", 0xFFFFFF, 0.6f),
  OREGON(7, 7, "Oregon", 0xFFFFFF, 0.62f),
  PENNSYLVANIA(0, 8, "Pennsylvania", 0xFFFFFF, 0.5f),
  SOUTH_CAROLINA(1, 8, "South Carolina", 0xFFFFFF, 0.56f),
  SOUTH_DAKOTA(2, 8, "South Dakota", 0xFFFFFF, 0.62f),
  TENNESSEE(3, 8, "Tennessee", 0xFFFFFF, 0.6f),
  UTAH(4, 8, "Utah", 0x101010, 0.48f),
  VIRGINIA(5, 8, "Virginia", 0xFFFFFF, 0.62f),
  WASHINGTON(6, 8, "Washington", 0x101010, 0.53f),
  WEST_VIRGINIA(7, 8, "West Virginia", 0xFFFFFF, 0.62f),
  WISCONSIN(0, 9, "Wisconsin", 0xFFFFFF, 0.62f),
  WYOMING(1, 9, "Wyoming", 0xFFFFFF, 0.62f),

  // Washington DC + Canadian provinces. Ordinals are serialized in sign JSON —
  // always append new entries at the end, never reorder or insert. Atlas row 9
  // cols 2-7 holds the first six, row 10 col 0 holds the seventh.
  DISTRICT_OF_COLUMBIA(2, 9, "Washington DC", 0x101010, 0.62f),
  ONTARIO(3, 9, "Ontario", 0x101010, 0.54f),
  QUEBEC(4, 9, "Quebec", 0x101010, 0.62f),
  NEW_BRUNSWICK(5, 9, "New Brunswick", 0xFFFFFF, 0.62f),
  NOVA_SCOTIA(6, 9, "Nova Scotia", 0xFFFFFF, 0.62f),
  NEWFOUNDLAND(7, 9, "Newfoundland and Labrador", 0xFFFFFF, 0.62f),
  PRINCE_EDWARD_ISLAND(0, 10, "Prince Edward Island", 0xFFFFFF, 0.62f),

  // Alto route markers (provided artwork). Each has a wider 3-digit variant in the next
  // atlas cell, selected automatically when the route number is 3+ characters.
  ALTO(1, 10, "Alto", 0x101010, 0.62f, 2, 10, 1.35f),
  ALTO_BLUE(3, 10, "Alto Blue", 0x101010, 0.62f, 4, 10, 1.35f);

  private final int atlasCol;
  private final int atlasRow;
  private final String friendlyName;
  private final int routeTextColor;
  private final float routeTextMaxFraction;
  // Optional wider variant used for 3+ digit route numbers (like real Interstate
  // shields). wideCol < 0 means the type has no wide variant.
  private final int wideCol;
  private final int wideRow;
  private final float wideAspect;

  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor,
      float routeTextMaxFraction) {
    this(atlasCol, atlasRow, friendlyName, routeTextColor, routeTextMaxFraction, -1, -1, 1.0f);
  }

  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor,
      float routeTextMaxFraction, int wideCol, int wideRow, float wideAspect) {
    this.atlasCol = atlasCol;
    this.atlasRow = atlasRow;
    this.friendlyName = friendlyName;
    this.routeTextColor = routeTextColor;
    this.routeTextMaxFraction = routeTextMaxFraction;
    this.wideCol = wideCol;
    this.wideRow = wideRow;
    this.wideAspect = wideAspect;
  }

  /** Whether the wide variant should be used for the given route number. */
  public boolean usesWideVariant(String routeNumber) {
    return wideCol >= 0 && routeNumber != null && routeNumber.length() >= 3;
  }

  public int getWideCol() {
    return wideCol;
  }

  public int getWideRow() {
    return wideRow;
  }

  /** Width/height ratio the wide variant renders at (1.0 for the square base cell). */
  public float getWideAspect() {
    return wideAspect;
  }

  /**
   * Color for the route number drawn over this shield background, chosen to
   * contrast with the atlas cell's fill (black on white/light shields, white on
   * dark ones, yellow on the blue county pentagon per MUTCD).
   */
  public int getRouteTextColor() {
    return routeTextColor;
  }

  /**
   * Fraction of the shield cell's width the route number may span before it shrinks
   * to fit, as a multiple of {@code SHIELD_SIZE}. Measured per shield from the atlas
   * cell's interior mid-band (the narrowest horizontal run of opaque pixels between
   * y=26 and y=38 of the 64px cell), scaled by an 0.80 safety margin and clamped to
   * [0.30, 0.62] — narrow silhouettes (Florida's peninsula, the diamond states, Utah's
   * beehive) get a tighter fraction than wide ones (Interstate, rounded squares) so a
   * 2-digit route number never spills past its shield's outline.
   */
  public float getRouteTextMaxFraction() {
    return routeTextMaxFraction;
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
