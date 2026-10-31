package com.micatechnologies.minecraft.csm.venues.av;

/**
 * The sixteen team colours a scoreboard's team bars, a ribbon board's text and a banner's cloth,
 * trim and lettering are picked from. Stored by index (0-15) in NBT; the names are
 * {@code csm.venues.colour.<index>} in the lang files ({@code gen_venues_av.py} writes them in
 * this order).
 *
 * @since 2026.10
 */
public final class VenueColours {

  /** How many there are. */
  public static final int COUNT = 16;

  public static final int NAVY = 0;
  public static final int ROYAL_BLUE = 1;
  public static final int ICE_BLUE = 2;
  public static final int GOLD = 6;
  public static final int CRIMSON = 9;
  public static final int BLACK = 12;
  public static final int WHITE = 14;

  private static final int[] RGB = {
      0x1A2856, 0x2054C4, 0x78BEE8, 0x008080, 0x188038, 0x184828, 0xF0B41E, 0xF06E14,
      0xD21E24, 0x941E2C, 0x641424, 0x603096, 0x18181A, 0xB0B6BE, 0xF0F0EC, 0x6E4628};

  private VenueColours() {
  }

  /**
   * A colour's RGB.
   *
   * @param index its index; out of range is clamped
   *
   * @return {@code 0xRRGGBB}
   */
  public static int rgb(int index) {
    return RGB[clamp(index)];
  }

  /** An index brought into range. */
  public static int clamp(int index) {
    return Math.max(0, Math.min(COUNT - 1, index));
  }

  /** The lang key of a colour's name. */
  public static String key(int index) {
    return "csm.venues.colour." + clamp(index);
  }

  /** Whether a colour is light enough that white lettering on it would not read. */
  public static boolean isLight(int index) {
    int c = rgb(index);
    int r = (c >> 16) & 0xFF;
    int g = (c >> 8) & 0xFF;
    int b = c & 0xFF;
    return r * 299 + g * 587 + b * 114 > 160_000;
  }
}
