package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

/**
 * The colour a plaque's numerals and street line are drawn in, saved by id. {@code default} is
 * the style's own (black infill on brass, gold on cast iron, blue on enamel, ...).
 *
 * @since 2026.11
 */
public enum PlaqueInk {
  DEFAULT("default", -1),
  BLACK("black", 0x151515),
  WHITE("white", 0xF4F1EA),
  GOLD("gold", 0xE2B84A),
  BRASS("brass", 0xC49A3C),
  BRONZE("bronze", 0x6B4A2B),
  BLUE("blue", 0x1F3F8F),
  STEEL("steel", 0xB9BEC4),
  RED("red", 0x9B2226),
  GREEN("green", 0x24513A),
  SLATE("slate", 0x4A4640);

  private final String id;
  private final int rgb;

  PlaqueInk(String id, int rgb) {
    this.id = id;
    this.rgb = rgb;
  }

  public String getId() {
    return id;
  }

  /** The colour, {@code 0xRRGGBB}; -1 for {@link #DEFAULT}. */
  public int getRgb() {
    return rgb;
  }

  public PlaqueInk next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /**
   * The ink with this id.
   *
   * @return it, or {@link #DEFAULT} for an unknown id
   */
  public static PlaqueInk byId(String id) {
    for (PlaqueInk i : values()) {
      if (i.id.equals(id)) {
        return i;
      }
    }
    return DEFAULT;
  }
}
