package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * Color palettes for the dynamic traffic light mount kit. Each scheme bundles four RGB triples
 * so the renderer can draw the same geometry in a different finish without per-box logic:
 * the main aluminum body, a slightly darker recessed C-channel shade, the knuckle hardware
 * (a step darker than the body), and the pivot hubs / collar (darkest).
 *
 * <p>Cycled by sneak + right-clicking the block in-world; persisted on the tile entity.
 */
public enum MountKitColorScheme {

  /** Raw cast aluminum — matches a real Pelco Astro-brac. Default scheme. */
  DEFAULT("Aluminum",
      0.64f, 0.65f, 0.66f,
      0.55f, 0.56f, 0.57f,
      0.48f, 0.49f, 0.50f,
      0.44f, 0.45f, 0.46f),

  /** Powder-coated traffic signal white — common on modern installations. */
  WHITE("White",
      0.95f, 0.95f, 0.94f,
      0.85f, 0.85f, 0.84f,
      0.75f, 0.75f, 0.74f,
      0.65f, 0.65f, 0.64f),

  /** Charcoal / dark graphite finish. */
  DARK_GRAY("Dark Gray",
      0.28f, 0.29f, 0.30f,
      0.22f, 0.23f, 0.24f,
      0.18f, 0.19f, 0.20f,
      0.14f, 0.15f, 0.16f),

  /** Near-black — DOT flat black finish. */
  BLACK("Black",
      0.12f, 0.12f, 0.13f,
      0.08f, 0.08f, 0.09f,
      0.06f, 0.06f, 0.07f,
      0.04f, 0.04f, 0.05f);

  private final String friendlyName;
  public final float aluR, aluG, aluB;
  public final float aluDarkR, aluDarkG, aluDarkB;
  public final float knuckleR, knuckleG, knuckleB;
  public final float pivotR, pivotG, pivotB;

  MountKitColorScheme(String friendlyName,
      float aluR, float aluG, float aluB,
      float aluDarkR, float aluDarkG, float aluDarkB,
      float knuckleR, float knuckleG, float knuckleB,
      float pivotR, float pivotG, float pivotB) {
    this.friendlyName = friendlyName;
    this.aluR = aluR; this.aluG = aluG; this.aluB = aluB;
    this.aluDarkR = aluDarkR; this.aluDarkG = aluDarkG; this.aluDarkB = aluDarkB;
    this.knuckleR = knuckleR; this.knuckleG = knuckleG; this.knuckleB = knuckleB;
    this.pivotR = pivotR; this.pivotG = pivotG; this.pivotB = pivotB;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  /** Safely reads a scheme from its stored ordinal; unknown ordinals fall back to {@link #DEFAULT}. */
  public static MountKitColorScheme fromOrdinal(int ordinal) {
    MountKitColorScheme[] values = values();
    if (ordinal < 0 || ordinal >= values.length) {
      return DEFAULT;
    }
    return values[ordinal];
  }

  /** Returns the next scheme in declaration order, wrapping at the end. */
  public MountKitColorScheme next() {
    MountKitColorScheme[] values = values();
    return values[(this.ordinal() + 1) % values.length];
  }
}
