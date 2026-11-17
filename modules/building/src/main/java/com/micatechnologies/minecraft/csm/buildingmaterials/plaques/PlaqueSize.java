package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

/**
 * How big a plaque is, saved by id: the plate (or, for the numerals and the decal, the box the
 * numbers fill) in px, across and up, laid flat and as a vertical stack. Medium is the issue's
 * "about 1 x 1/2 block".
 *
 * @since 2026.11
 */
public enum PlaqueSize {
  SMALL("s", 10f, 5f, 4f, 12f),
  MEDIUM("m", 14f, 7f, 5f, 14f),
  LARGE("l", 16f, 8f, 6f, 16f);

  private final String id;
  final float width;
  final float height;
  final float stackWidth;
  final float stackHeight;

  PlaqueSize(String id, float width, float height, float stackWidth, float stackHeight) {
    this.id = id;
    this.width = width;
    this.height = height;
    this.stackWidth = stackWidth;
    this.stackHeight = stackHeight;
  }

  public String getId() {
    return id;
  }

  public PlaqueSize next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /** The size with this id, or {@link #MEDIUM} for an unknown one. */
  public static PlaqueSize byId(String id) {
    for (PlaqueSize s : values()) {
      if (s.id.equals(id)) {
        return s;
      }
    }
    return MEDIUM;
  }
}
