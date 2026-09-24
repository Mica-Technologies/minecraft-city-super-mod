package com.micatechnologies.minecraft.csm.furniture.residential;

import java.util.Locale;
import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * What a TV shows, in the order a right-click steps through them. Each is an animated texture
 * drawn by {@code gen_furniture_living.py} ({@code tv_<name>}), picked by the blockstate; the
 * ordinal is what a TV saves, so new channels go at the end.
 *
 * @since 2026.9
 */
public enum TvChannel implements IStringSerializable {
  /** Switched off: the dark glass. */
  OFF,
  /** A news desk: the anchor, the lower third and a ticker running along the bottom. */
  NEWS,
  /** A football match from the stand, the players running. */
  SPORTS,
  /** Mountains over a lake, clouds drifting. */
  NATURE,
  /** A colour-bar test card. */
  BARS,
  /** No signal: snow. */
  STATIC;

  private static final TvChannel[] VALUES = values();

  /**
   * The channel a right-click moves to.
   *
   * @return the next channel, round to off
   */
  public TvChannel next() {
    return VALUES[(ordinal() + 1) % VALUES.length];
  }

  /**
   * The channel saved as {@code ordinal}, or off for one that is not known.
   *
   * @param ordinal the saved ordinal
   *
   * @return the channel
   */
  public static TvChannel byOrdinal(int ordinal) {
    return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : OFF;
  }

  @Nonnull
  @Override
  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
