package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

/**
 * What each address plaque block is: its plate (a tile on the body sheet, or none), how deep the
 * plate stands off the wall, the default font and ink, and how the numerals are drawn on it.
 * One block per style, constructed by registry name ({@link BlockAddressPlaque}).
 *
 * <p>SHARED with {@code gen_address_plaques.py} (its {@code BLOCKS} and {@code DEPTH}): the
 * registry names, tiles, defaults and depths.</p>
 *
 * @since 2026.11
 */
public enum PlaqueStyle {
  BRASS("address_plaque_brass", "brass", 0.5f, 1.0f, 0x9A7A2E, PlaqueFont.CLASSIC,
      PlaqueInk.BLACK, Treatment.FLAT),
  CAST_IRON("address_plaque_cast_iron", "cast_iron", 0.75f, 1.0f, 0x1B1B1B, PlaqueFont.CLASSIC,
      PlaqueInk.GOLD, Treatment.RAISED),
  ENAMEL("address_plaque_enamel", "enamel", 0.5f, 1.0f, 0x2A4A8E, PlaqueFont.DIDONE,
      PlaqueInk.BLUE, Treatment.FLAT),
  STONE("address_plaque_stone", "stone", 2.0f, 1.0f, 0xB8AE9A, PlaqueFont.CLASSIC,
      PlaqueInk.SLATE, Treatment.INCISED),
  WOOD("address_plaque_wood", "wood", 1.0f, 1.0f, 0x23402A, PlaqueFont.CLASSIC,
      PlaqueInk.WHITE, Treatment.FLAT),
  STEEL("address_plaque_steel", "steel", 0.5f, 0.6f, 0x9EA3A8, PlaqueFont.MODERN,
      PlaqueInk.BLACK, Treatment.STANDOFF),
  NUMERALS("address_numerals", null, 0f, 0f, 0, PlaqueFont.CLASSIC, PlaqueInk.BRASS,
      Treatment.STANDOFF),
  TRANSOM_DECAL("address_transom_decal", null, 0f, 0f, 0, PlaqueFont.DIDONE, PlaqueInk.GOLD,
      Treatment.OUTLINED),
  LAMP_BOX("address_lamp_box", "lamp_housing", 3.0f, 1.25f, 0x3B2F25, PlaqueFont.MODERN,
      PlaqueInk.BLACK, Treatment.FLAT);

  /** How the numerals are drawn. */
  public enum Treatment {
    /** Painted or infilled, flush on the face. */
    FLAT,
    /** Cast with the plate: a darker copy below gives the numerals a side. */
    RAISED,
    /** Cut into stone: a light edge below and right of the dark letter. */
    INCISED,
    /** Each numeral its own piece, held off the face on pins: three layers deep. */
    STANDOFF,
    /** Gold leaf with a dark outline, on glass: drawn on both sides. */
    OUTLINED
  }

  /** The light a lit lamp box gives. */
  public static final int LAMP_LIGHT = 10;

  private final String registryName;
  private final String tile;
  final float depth;
  final float border;
  final int edgeRgb;
  private final PlaqueFont font;
  private final PlaqueInk ink;
  final Treatment treatment;

  PlaqueStyle(String registryName, String tile, float depth, float border, int edgeRgb,
      PlaqueFont font, PlaqueInk ink, Treatment treatment) {
    this.registryName = registryName;
    this.tile = tile;
    this.depth = depth;
    this.border = border;
    this.edgeRgb = edgeRgb;
    this.font = font;
    this.ink = ink;
    this.treatment = treatment;
  }

  public String getRegistryName() {
    return registryName;
  }

  /** The plate's tile on the body sheet, or null for a style with no plate. */
  public String getTile() {
    return tile;
  }

  public boolean hasPlate() {
    return tile != null;
  }

  /** Whether the block is the lamp box, which has a lens and gives light. */
  public boolean isLamp() {
    return this == LAMP_BOX;
  }

  /** Whether the numbers are read from both sides (the decal, on glass). */
  public boolean isTwoSided() {
    return treatment == Treatment.OUTLINED;
  }

  public PlaqueFont getDefaultFont() {
    return font;
  }

  public PlaqueInk getDefaultInk() {
    return ink;
  }

  /** The style a block of this registry name is, or null for none. */
  public static PlaqueStyle byRegistryName(String name) {
    for (PlaqueStyle s : values()) {
      if (s.registryName.equals(name)) {
        return s;
      }
    }
    return null;
  }
}
