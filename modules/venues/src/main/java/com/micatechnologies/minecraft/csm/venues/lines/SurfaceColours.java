package com.micatechnologies.minecraft.csm.venues.lines;

/**
 * The sixteen colours a court's acrylic and a track's polyurethane come in: a court and its
 * surround, a track and its apron. A plain {@code court_acrylic} or {@code track_surface} block
 * takes one by metadata (a tint over a light grey sprite); a painted cell takes its anchor's
 * {@code cc} and {@code sc}, written into the vertex colour, over the same sprite, so the two
 * match.
 *
 * @since 2026.10
 */
public final class SurfaceColours {

  /** How many. */
  public static final int COUNT = 16;

  private static final int[] RGB = {
      0x3C7448, 0x285438, 0x3060A0, 0x5C92C6, 0xAC4434, 0xBE6A44, 0x64468C, 0x7C8084,
      0xBEAA82, 0x22325C, 0xD4762C, 0x2C2E30, 0x227676, 0x6E2632, 0xD6B83C, 0xC4C6C8};

  private SurfaceColours() {
  }

  /** A colour's RGB, the index clamped. */
  public static int rgb(int index) {
    return RGB[clamp(index)];
  }

  /** An index brought into range. */
  public static int clamp(int index) {
    return Math.max(0, Math.min(COUNT - 1, index));
  }

  /** The lang key of a colour's name. */
  public static String key(int index) {
    return "csm.venues.surface." + clamp(index);
  }
}
