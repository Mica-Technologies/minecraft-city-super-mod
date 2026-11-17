package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

/**
 * A typeface an address plaque can be set in, saved by id. The glyphs are on one sheet
 * ({@link PlaqueSheet}); the order here is the sheet's.
 *
 * <ul>
 *   <li>{@code classic}: a bold transitional serif (Libre Baskerville Bold), the brownstone look;</li>
 *   <li>{@code didone}: a high-contrast display serif (Abril Fatface), for enamel and gold leaf;</li>
 *   <li>{@code modern}: a geometric sans (Poppins SemiBold), for steel and the lamp box;</li>
 *   <li>{@code narrow}: the mod's own narrow highway alphabet (Mica Open Highway Series C), which
 *       fits five digits on the smallest plate.</li>
 * </ul>
 *
 * @since 2026.11
 */
public enum PlaqueFont {
  CLASSIC("classic"),
  DIDONE("didone"),
  MODERN("modern"),
  NARROW("narrow");

  private final String id;

  PlaqueFont(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  /** The font's table in {@link PlaqueSheet#GLYPHS}. */
  int sheetIndex() {
    return ordinal();
  }

  /**
   * The font with this id.
   *
   * @return it, or null for an unknown id (the style's default)
   */
  public static PlaqueFont byId(String id) {
    for (PlaqueFont f : values()) {
      if (f.id.equals(id)) {
        return f;
      }
    }
    return null;
  }
}
