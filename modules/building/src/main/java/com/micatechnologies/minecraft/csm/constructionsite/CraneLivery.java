package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * The colours a tower crane comes in: the three real cranes are actually seen in.
 *
 * <p>Stored in a mast section's metadata, so the ordinal is saved with the world: append only.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum CraneLivery implements IStringSerializable {

  /** Liebherr/Potain yellow. */
  YELLOW("yellow"),

  /** Red. */
  RED("red"),

  /** White. */
  WHITE("white");

  private final String name;

  CraneLivery(String name) {
    this.name = name;
  }

  /**
   * The livery for a stored ordinal, falling back to yellow rather than throwing on a value this
   * version does not know.
   *
   * @param ordinal the stored ordinal
   *
   * @return the livery
   *
   * @since 1.0
   */
  public static CraneLivery fromOrdinal(int ordinal) {
    CraneLivery[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : YELLOW;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
