package com.micatechnologies.minecraft.csm.transit.stop;

/**
 * The four invented bus agencies, in the order the generators list them ({@code AGENCIES} in
 * {@code gen_transit_stops.py}): each flag, shelter and station tile band carries one. The
 * departure board filters on them and prints a route's number in its agency's colour.
 *
 * @since 2026.9
 */
public enum BusAgency {
  CITYLINE("cityline", "CITYLINE", 0x3CC8D8),
  RIVERWAY("riverway", "RIVERWAY", 0xFF9A38),
  VERDANT("verdant", "VERDANT", 0x68D27C),
  EMBERLINE("emberline", "EMBERLINE", 0xFF6450);

  private final String id;
  private final String title;
  private final int textColour;

  BusAgency(String id, String title, int textColour) {
    this.id = id;
    this.title = title;
    this.textColour = textColour;
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
