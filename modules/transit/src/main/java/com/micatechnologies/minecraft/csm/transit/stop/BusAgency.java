package com.micatechnologies.minecraft.csm.transit.stop;

/**
 * The four invented bus agencies, in the order the generators list them ({@code AGENCIES} in
 * {@code gen_transit_stops.py}): each flag, shelter and station tile band carries one. The
 * departure board filters on them and prints a route's number in its agency's colour.
 *
 * @since 2026.9
 */
public enum BusAgency {
  CITYLINE("cityline", "CITYLINE", 0x3CC8D8, 0x0E5E6F, 0xF4C428),
  RIVERWAY("riverway", "RIVERWAY", 0xFF9A38, 0x1F3A73, 0xF0F2F0),
  VERDANT("verdant", "VERDANT", 0x68D27C, 0x24743C, 0xF0F2F0),
  EMBERLINE("emberline", "EMBERLINE", 0xFF6450, 0xB02420, 0xF0F2F0);

  private final String id;
  private final String title;
  private final int textColour;
  private final int signGround;
  private final int signLegend;

  BusAgency(String id, String title, int textColour, int signGround, int signLegend) {
    this.id = id;
    this.title = title;
    this.textColour = textColour;
    this.signGround = signGround;
    this.signLegend = signLegend;
  }

  /**
   * The ground of the agency's signs (its flag, its stop-name blade): the livery colour the
   * shelters' fascias are painted in.
   *
   * @return 0xRRGGBB
   */
  public int getSignGround() {
    return signGround;
  }

  /**
   * The lettering on the agency's signs: CITYLINE's yellow, the others' white.
   *
   * @return 0xRRGGBB
   */
  public int getSignLegend() {
    return signLegend;
  }

  /**
   * The agency whose sign ground a colour is, for a sign set in the editor.
   *
   * @param rgb a sign's background, 0xRRGGBB
   *
   * @return the agency, or null for a colour none of them uses (a custom agency)
   */
  public static BusAgency ofSignGround(int rgb) {
    for (BusAgency agency : values()) {
      if (agency.signGround == (rgb & 0xFFFFFF)) {
        return agency;
      }
    }
    return null;
  }

  /**
   * The id every block of the agency's registry name ends in.
   *
   * @return lower case, "cityline"
   */
  public String getId() {
    return id;
  }

  /**
   * The name as the agency prints it.
   *
   * @return capitals, "CITYLINE"
   */
  public String getTitle() {
    return title;
  }

  /**
   * The agency's colour as text lit on a dark screen: its livery colour, lightened until it
   * reads there (navy RIVERWAY is shown in its orange accent).
   *
   * @return 0xRRGGBB
   */
  public int getTextColour() {
    return textColour;
  }

  /**
   * The agency a registry name ends in.
   *
   * @param registryName a flag's (or shelter's) registry name
   *
   * @return its agency, CITYLINE if it names none
   */
  public static BusAgency ofRegistryName(String registryName) {
    if (registryName != null) {
      for (BusAgency agency : values()) {
        if (registryName.endsWith("_" + agency.id)) {
          return agency;
        }
      }
    }
    return CITYLINE;
  }
}
