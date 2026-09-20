package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import java.util.Locale;
import net.minecraft.util.IStringSerializable;

public enum GuideSignShieldType implements IStringSerializable {
  INTERSTATE(0, 0, "Interstate", 0xFFFFFF, 0.62f),
  INTERSTATE_BUSINESS(1, 0, "Interstate Business", 0xFFFFFF, 0.62f),
  US_ROUTE(2, 0, "US Route", 0x101010, 0.62f),
  STATE_SQUARE(3, 0, "State Route (Square)", 0x101010, 0.62f),
  STATE_CIRCLE(4, 0, "State Route (Circle)", 0x101010, 0.62f),
  COUNTY_ROUTE(5, 0, "County Route", 0xF7D117, 0.6f),
  TOLL(6, 0, "Toll Route", 0xFFFFFF, 0.62f),
  BLANK_CUSTOM(7, 0, "Blank/Custom", 0x101010, 0.61f),

  // State-specific markers. Atlas row 1 holds 8, row 2 holds the rest.
  //
  // Every state, DC and province marker below is drawn from its real route marker and carries
  // its route number's placement as well as its width: (max width, cap height, centre x,
  // centre y), all fractions of the shield. These four values and the text colour are
  // measured off the atlas by dev-env-utils/scripts/measure_shield_legends.py -- re-run it
  // (and its --check) rather than editing them by hand.
  CALIFORNIA(0, 1, "California", 0xFFFFFF, 0.71f, 0.38f, 0.5f, 0.633f),
  TEXAS(1, 1, "Texas", 0x101010, 0.74f, 0.4f, 0.5f, 0.391f),
  FLORIDA(2, 1, "Florida", 0x101010, 0.74f, 0.4f, 0.5f, 0.469f),
  NEW_YORK(3, 1, "New York", 0x101010, 0.77f, 0.42f, 0.5f, 0.5f),
  CONNECTICUT(4, 1, "Connecticut", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  MASSACHUSETTS(5, 1, "Massachusetts", 0x101010, 0.77f, 0.42f, 0.5f, 0.5f),
  MAINE(6, 1, "Maine", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  NEW_HAMPSHIRE(7, 1, "New Hampshire", 0x101010, 0.68f, 0.37f, 0.562f, 0.5f),
  RHODE_ISLAND(0, 2, "Rhode Island", 0x101010, 0.74f, 0.4f, 0.5f, 0.609f),
  VERMONT(1, 2, "Vermont", 0x006B54, 0.71f, 0.38f, 0.5f, 0.633f),

  // Remaining 40 states. Ordinals are serialized in sign JSON — always append
  // new entries at the end, never reorder or insert. Atlas row 2 cols 2-7,
  // row 3 cols 0-7, rows 6-9 (in the taller 512x1024 atlas) hold these.
  ALABAMA(2, 2, "Alabama", 0x101010, 0.71f, 0.38f, 0.5f, 0.43f),
  ALASKA(3, 2, "Alaska", 0x101010, 0.57f, 0.31f, 0.617f, 0.453f),
  ARIZONA(4, 2, "Arizona", 0x101010, 0.77f, 0.42f, 0.5f, 0.562f),
  ARKANSAS(5, 2, "Arkansas", 0x101010, 0.65f, 0.35f, 0.453f, 0.422f),
  COLORADO(6, 2, "Colorado", 0x101010, 0.63f, 0.34f, 0.5f, 0.719f),
  DELAWARE(7, 2, "Delaware", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  GEORGIA(0, 3, "Georgia", 0x101010, 0.59f, 0.32f, 0.5f, 0.547f),
  HAWAII(1, 3, "Hawaii", 0x101010, 0.65f, 0.35f, 0.5f, 0.656f),
  IDAHO(2, 3, "Idaho", 0x101010, 0.56f, 0.3f, 0.641f, 0.297f),
  ILLINOIS(3, 3, "Illinois", 0x101010, 0.77f, 0.42f, 0.5f, 0.609f),
  INDIANA(4, 3, "Indiana", 0x101010, 0.77f, 0.42f, 0.5f, 0.609f),
  IOWA(5, 3, "Iowa", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  KANSAS(6, 3, "Kansas", 0x101010, 0.71f, 0.38f, 0.5f, 0.5f),
  KENTUCKY(7, 3, "Kentucky", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  LOUISIANA(0, 6, "Louisiana", 0x101010, 0.51f, 0.28f, 0.516f, 0.625f),
  MARYLAND(1, 6, "Maryland", 0x101010, 0.77f, 0.42f, 0.5f, 0.656f),
  MICHIGAN(2, 6, "Michigan", 0x101010, 0.48f, 0.26f, 0.5f, 0.523f),
  MINNESOTA(3, 6, "Minnesota", 0xFFFFFF, 0.77f, 0.42f, 0.5f, 0.609f),
  MISSISSIPPI(4, 6, "Mississippi", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  MISSOURI(5, 6, "Missouri", 0x101010, 0.63f, 0.34f, 0.508f, 0.523f),
  MONTANA(6, 6, "Montana", 0x101010, 0.74f, 0.4f, 0.5f, 0.594f),
  NEBRASKA(7, 6, "Nebraska", 0x101010, 0.62f, 0.33f, 0.5f, 0.422f),
  NEVADA(0, 7, "Nevada", 0x101010, 0.56f, 0.3f, 0.5f, 0.305f),
  NEW_JERSEY(1, 7, "New Jersey", 0x101010, 0.74f, 0.4f, 0.5f, 0.5f),
  NEW_MEXICO(2, 7, "New Mexico", 0x101010, 0.49f, 0.27f, 0.5f, 0.5f),
  NORTH_CAROLINA(3, 7, "North Carolina", 0x101010, 0.56f, 0.3f, 0.5f, 0.5f),
  NORTH_DAKOTA(4, 7, "North Dakota", 0x101010, 0.71f, 0.38f, 0.469f, 0.664f),
  OHIO(5, 7, "Ohio", 0x101010, 0.73f, 0.39f, 0.492f, 0.484f),
  OKLAHOMA(6, 7, "Oklahoma", 0x101010, 0.77f, 0.42f, 0.5f, 0.609f),
  OREGON(7, 7, "Oregon", 0x101010, 0.74f, 0.4f, 0.5f, 0.422f),
  PENNSYLVANIA(0, 8, "Pennsylvania", 0x101010, 0.68f, 0.36f, 0.5f, 0.453f),
  SOUTH_CAROLINA(1, 8, "South Carolina", 0x003478, 0.72f, 0.39f, 0.5f, 0.617f),
  SOUTH_DAKOTA(2, 8, "South Dakota", 0x101010, 0.74f, 0.4f, 0.5f, 0.484f),
  TENNESSEE(3, 8, "Tennessee", 0x101010, 0.77f, 0.42f, 0.5f, 0.398f),
  UTAH(4, 8, "Utah", 0x101010, 0.48f, 0.26f, 0.5f, 0.523f),
  VIRGINIA(5, 8, "Virginia", 0x101010, 0.77f, 0.42f, 0.5f, 0.383f),
  WASHINGTON(6, 8, "Washington", 0x101010, 0.56f, 0.3f, 0.484f, 0.469f),
  WEST_VIRGINIA(7, 8, "West Virginia", 0x101010, 0.71f, 0.38f, 0.5f, 0.5f),
  WISCONSIN(0, 9, "Wisconsin", 0x101010, 0.77f, 0.42f, 0.5f, 0.484f),
  WYOMING(1, 9, "Wyoming", 0x101010, 0.72f, 0.39f, 0.5f, 0.539f),

  // Washington DC + Canadian provinces. Ordinals are serialized in sign JSON —
  // always append new entries at the end, never reorder or insert. Atlas row 9
  // cols 2-7 holds the first six, row 10 col 0 holds the seventh.
  DISTRICT_OF_COLUMBIA(2, 9, "Washington DC", 0x101010, 0.62f, 0.33f, 0.5f, 0.609f),
  ONTARIO(3, 9, "Ontario", 0x101010, 0.45f, 0.24f, 0.5f, 0.516f),
  QUEBEC(4, 9, "Quebec", 0xFFFFFF, 0.59f, 0.32f, 0.5f, 0.625f),
  NEW_BRUNSWICK(5, 9, "New Brunswick", 0x101010, 0.56f, 0.3f, 0.484f, 0.516f),
  NOVA_SCOTIA(6, 9, "Nova Scotia", 0x101010, 0.59f, 0.32f, 0.5f, 0.5f),
  NEWFOUNDLAND(7, 9, "Newfoundland and Labrador", 0x101010, 0.71f, 0.38f, 0.5f, 0.625f),
  PRINCE_EDWARD_ISLAND(0, 10, "Prince Edward Island", 0x101010, 0.59f, 0.32f, 0.5f, 0.578f),

  // Alto route markers (provided artwork). Each has a wider 3-digit variant in the next
  // atlas cell, selected automatically when the route number is 3+ characters.
  ALTO(1, 10, "Alto", 0x101010, 0.62f, 2, 10, 1.35f),
  ALTO_BLUE(3, 10, "Alto Blue", 0x101010, 0.62f, 4, 10, 1.35f),

  // The bicycle route marker (MUTCD M1-8): a green oval on a white plate with the
  // bicycle symbol above the number. Its four placement values are measured off the
  // book's own drawing by dev-env-utils/scripts/gen_bike_route_shield.py, which also
  // builds the atlas cell -- it is not one of measure_shield_legends.py's MARKERS,
  // which cover the state, DC and province markers. Appended, like everything here.
  BIKE_ROUTE(5, 10, "Bike Route", 0xFFFFFF, 0.29f, 0.27f, 0.497f, 0.693f);

  /**
   * Route number cap height over a generic shield, as a fraction of the shield's size (MUTCD-ish:
   * numerals about 42% of the shield's height). A state marker's measured cap height is never
   * taller than this.
   */
  public static final float DEFAULT_ROUTE_CAP_FRACTION = 0.42f;

  private final int atlasCol;
  private final int atlasRow;
  private final String friendlyName;
  private final int routeTextColor;
  private final float routeTextMaxFraction;
  private final float routeTextCapFraction;
  private final float routeTextCenterX;
  private final float routeTextCenterY;
  // Optional wider variant used for 3+ digit route numbers (like real Interstate
  // shields). wideCol < 0 means the type has no wide variant.
  private final int wideCol;
  private final int wideRow;
  private final float wideAspect;

  /** A generic shield: its route number is centred and set at the default cap height. */
  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor,
      float routeTextMaxFraction) {
    this(atlasCol, atlasRow, friendlyName, routeTextColor, routeTextMaxFraction,
        DEFAULT_ROUTE_CAP_FRACTION, 0.5f, 0.5f, -1, -1, 1.0f);
  }

  /**
   * A marker whose face leaves its route number somewhere other than the middle (above TEXAS,
   * below Colorado's flag, in the corner Idaho's outline leaves free).
   */
  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor,
      float routeTextMaxFraction, float routeTextCapFraction, float routeTextCenterX,
      float routeTextCenterY) {
    this(atlasCol, atlasRow, friendlyName, routeTextColor, routeTextMaxFraction,
        routeTextCapFraction, routeTextCenterX, routeTextCenterY, -1, -1, 1.0f);
  }

  /** A generic shield with a wider variant cell for 3+ digit route numbers. */
  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor,
      float routeTextMaxFraction, int wideCol, int wideRow, float wideAspect) {
    this(atlasCol, atlasRow, friendlyName, routeTextColor, routeTextMaxFraction,
        DEFAULT_ROUTE_CAP_FRACTION, 0.5f, 0.5f, wideCol, wideRow, wideAspect);
  }

  GuideSignShieldType(int atlasCol, int atlasRow, String friendlyName, int routeTextColor,
      float routeTextMaxFraction, float routeTextCapFraction, float routeTextCenterX,
      float routeTextCenterY, int wideCol, int wideRow, float wideAspect) {
    this.atlasCol = atlasCol;
    this.atlasRow = atlasRow;
    this.friendlyName = friendlyName;
    this.routeTextColor = routeTextColor;
    this.routeTextMaxFraction = routeTextMaxFraction;
    this.routeTextCapFraction = routeTextCapFraction;
    this.routeTextCenterX = routeTextCenterX;
    this.routeTextCenterY = routeTextCenterY;
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
   * dark ones, yellow on the blue county pentagon per MUTCD). A state marker uses its
   * real sign's numeral colour: white on California's green spade and Minnesota's blue,
   * green on Vermont's white field, blue on South Carolina's.
   */
  public int getRouteTextColor() {
    return routeTextColor;
  }

  /**
   * Fraction of the shield cell's width the route number may span before it shrinks
   * to fit, as a multiple of {@code SHIELD_SIZE}. For the generic shields this was measured
   * from the atlas cell's interior mid-band (the narrowest horizontal run of opaque pixels
   * between y=26 and y=38 of the 64px cell, scaled by 0.80 and clamped to [0.30, 0.62]). For
   * the state, DC and province markers it is the width of the largest two-digit number that
   * fits inside the part of the face the number is printed on, measured by
   * {@code measure_shield_legends.py}, so a three-digit route shrinks to the same width and
   * stays inside the outline too.
   */
  public float getRouteTextMaxFraction() {
    return routeTextMaxFraction;
  }

  /** Cap height of the route number, as a fraction of the shield's height. */
  public float getRouteTextCapFraction() {
    return routeTextCapFraction;
  }

  /**
   * Horizontal centre of the route number, as a fraction of the shield's width from its left
   * edge (0.5 = centred).
   */
  public float getRouteTextCenterX() {
    return routeTextCenterX;
  }

  /**
   * Vertical centre of the route number, as a fraction of the shield's height from its TOP
   * edge, the way the atlas cell is laid out (0.5 = centred). Renderers whose pixel space runs
   * upwards subtract the offset rather than add it.
   */
  public float getRouteTextCenterY() {
    return routeTextCenterY;
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

  /**
   * The name this marker goes by in a blockstate: the constant's own name in lower case.
   *
   * <p>The dynamic route marker sign carries the marker as a block property, so the shield
   * is painted by an ordinary block model rather than by a renderer, and
   * {@code dev-env-utils/scripts/gen_route_markers.py} writes that blockstate's variant
   * keys from this same enum. Renaming a constant renames its variant, so the generator
   * has to be re-run with it.
   *
   * @return the blockstate name for this marker
   *
   * @since 2026.9.20
   */
  @Override
  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static GuideSignShieldType fromOrdinal(int value) {
    GuideSignShieldType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return INTERSTATE;
    }
    return vals[value];
  }
}
