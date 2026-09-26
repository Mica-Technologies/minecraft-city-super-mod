package com.micatechnologies.minecraft.csm.furniture.market;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * What hangs on a gondola's shelf edges besides its price strip: nothing, red sale tags, or the
 * sale tags and a shelf talker sticking out into the aisle. A click with an empty hand steps a
 * gondola through them ({@link BlockGondola#TAGS}).
 *
 * <p>The ordinal is stored in the gondola's metadata, so the order is fixed: append, never
 * re-order.</p>
 *
 * @since 2026.9
 */
public enum ShelfTags implements IStringSerializable {
  /** The price strip alone. */
  NONE("none"),
  /** A red sale tag under the price strip, two to a shelf. */
  SALE("sale"),
  /** The sale tags and a shelf talker. */
  TALKER("talker");

  private final String name;

  ShelfTags(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  /**
   * The next (or, backwards, the previous) of the three, round again.
   *
   * @param backwards whether to step back
   *
   * @return the neighbouring value
   */
  public ShelfTags step(boolean backwards) {
    ShelfTags[] all = values();
    return all[(ordinal() + (backwards ? all.length - 1 : 1)) % all.length];
  }

  /**
   * The value stored as {@code ordinal}, or {@link #NONE} if it is out of range.
   *
   * @param ordinal the stored ordinal
   *
   * @return the value
   */
  public static ShelfTags byOrdinal(int ordinal) {
    ShelfTags[] all = values();
    return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
  }
}
