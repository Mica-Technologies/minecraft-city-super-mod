package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * The kinds of glass the glazing blocks come in, each as a full block ({@link BlockGlazing}) and a
 * pane ({@link BlockGlazingPane}). The registry names are {@code glass_<kind>} and
 * {@code glass_pane_<kind>}.
 *
 * @version 1.0
 * @since 2026.9
 */
public enum GlassKind {
  CLEAR("clear", false, false),
  GREY("grey", false, false),
  BRONZE("bronze", false, false),
  BLUE("blue", false, false),
  /** Clear from the inside, dark and nearly opaque from the outside. */
  ONE_WAY("oneway", true, false),
  WIRED("wired", false, false),
  /** Laminated: hard to break and proof against blasts. */
  BULLET_RESISTANT("bullet", false, true),
  FROSTED("frosted", false, false);

  private final String name;
  private final boolean oneWay;
  private final boolean hardened;

  GlassKind(String name, boolean oneWay, boolean hardened) {
    this.name = name;
    this.oneWay = oneWay;
    this.hardened = hardened;
  }

  /**
   * The kind named in {@code registryName}, which ends in {@code _<kind>}.
   *
   * @param registryName a glazing registry name
   *
   * @return the kind
   *
   * @throws IllegalArgumentException if no kind matches
   * @since 1.0
   */
  public static GlassKind fromRegistryName(String registryName) {
    for (GlassKind k : values()) {
      if (registryName.endsWith("_" + k.name)) {
        return k;
      }
    }
    throw new IllegalArgumentException("No glass kind in " + registryName);
  }

  /**
   * Whether the glass is one-way: its outside, the side it faces, is drawn dark.
   *
   * @return whether it is one-way
   *
   * @since 1.0
   */
  public boolean isOneWay() {
    return oneWay;
  }

  /**
   * How hard the glass is to break.
   *
   * @return the hardness
   *
   * @since 1.0
   */
  public float hardness() {
    return hardened ? 25F : 0.5F;
  }

  /**
   * How well the glass stands up to explosions.
   *
   * @return the blast resistance
   *
   * @since 1.0
   */
  public float resistance() {
    return hardened ? 2000F : 1.5F;
  }
}
