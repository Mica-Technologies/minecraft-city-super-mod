package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

/**
 * A wayfinding panel's colours ({@code s}): its background, its legend and arrows, and the tile
 * behind a pictogram. A pictogram's art on its tile is drawn in the background colour, so a light
 * tile on a dark sign and a dark tile on a light one both read. The custom colours ({@code bg},
 * {@code fg}, {@link WayfindingSign}) override a scheme's.
 *
 * <p>Saved by {@link #getId() id}, never by ordinal: every id the airport and hospital panels saved
 * is here with its colours, so a sign keeps its look. The order is the editor's.</p>
 *
 * @since 2026.10
 */
public enum WayfindingScheme {
  CORPORATE("corporate", 0xE3E6E9, 0x262B31, 0x262B31),
  NEUTRAL("neutral", 0x1C2A44, 0xF2F4F8, 0xF2F4F8),
  TRANSIT("transit", 0x15171A, 0xFFFFFF, 0xFFFFFF),
  HOSPITAL("hospital", 0xF4F6F8, 0x1C4E8C, 0x1C4E8C),
  AIRPORT("airport", 0x2A2C30, 0xFAC81E, 0xFAC81E),
  METRO("metro", 0x12204A, 0xF2F4F8, 0xF2F4F8),
  EXIT("exit", 0x0B6B3A, 0xF2F4F8, 0xF2F4F8),
  INFO("info", 0x1F5DAA, 0xF2F4F8, 0xF2F4F8),
  EMERGENCY("emergency", 0xB3201C, 0xFFFFFF, 0xFFFFFF),
  IMAGING("imaging", 0x1F5DAA, 0xFFFFFF, 0xFFFFFF),
  SURGERY("surgery", 0x1E7A44, 0xFFFFFF, 0xFFFFFF),
  MATERNITY("maternity", 0xC2477E, 0xFFFFFF, 0xFFFFFF),
  OUTPATIENT("outpatient", 0x13807C, 0xFFFFFF, 0xFFFFFF),
  INPATIENT("inpatient", 0x5B3C8C, 0xFFFFFF, 0xFFFFFF),
  LABORATORY("laboratory", 0xD9701E, 0xFFFFFF, 0xFFFFFF),
  ADMIN("admin", 0x5E6670, 0xFFFFFF, 0xFFFFFF);

  private final String id;
  private final int background;
  private final int legend;
  private final int pictogram;

  WayfindingScheme(String id, int background, int legend, int pictogram) {
    this.id = id;
    this.background = background;
    this.legend = legend;
    this.pictogram = pictogram;
  }

  public String getId() {
    return id;
  }

  public int getBackground() {
    return background;
  }

  public int getLegend() {
    return legend;
  }

  /** The colour of a pictogram's tile. */
  public int getPictogram() {
    return pictogram;
  }

  public WayfindingScheme next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /**
   * The scheme with this id.
   *
   * @param id       the id
   * @param fallback the scheme for an id not known
   * @return the scheme
   */
  public static WayfindingScheme byId(String id, WayfindingScheme fallback) {
    for (WayfindingScheme s : values()) {
      if (s.id.equals(id)) {
        return s;
      }
    }
    return fallback;
  }
}
