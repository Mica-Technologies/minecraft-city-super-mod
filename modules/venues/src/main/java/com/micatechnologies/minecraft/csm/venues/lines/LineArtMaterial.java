package com.micatechnologies.minecraft.csm.venues.lines;

/**
 * One material a court or track cell is painted in ({@link LineArtMaterials}): a sprite, a colour
 * and how far its face stands proud of the block's top.
 *
 * @since 2026.10
 */
public final class LineArtMaterial {

  /** Where a material's colour comes from. */
  public enum Colour {
    /** The anchor's main surface colour ({@link SurfaceColours}). */
    CC,
    /** The anchor's second surface colour ({@link SurfaceColours}). */
    SC,
    /** The anchor's line colour (a venue colour). */
    LC,
    /** The anchor's second line colour (a venue colour). */
    OC,
    /** The material's own. */
    FIXED
  }

  /** Its name, as {@code venues_lines.py} names it. */
  public final String name;
  /** Its sprite's key: {@code SURF} for the family's surface, else a texture under the
   * family's folder. */
  public final String sprite;
  /** Where its colour comes from. */
  public final Colour colour;
  /** Its own colour, for {@link Colour#FIXED}. */
  public final int rgb;
  /** How far its face stands proud of the top, in pixels (the kerb's). */
  public final float raise;

  LineArtMaterial(String name, String sprite, Colour colour, int rgb, float raise) {
    this.name = name;
    this.sprite = sprite;
    this.colour = colour;
    this.rgb = rgb;
    this.raise = raise;
  }
}
