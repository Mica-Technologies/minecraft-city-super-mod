package com.micatechnologies.minecraft.csm.transit.arrival;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * How an arrival board is held up, chosen by the face its item is used on
 * ({@link BlockBusArrivalBoard}). Two bits of the master's metadata, beside its facing; the
 * ordinal is saved there, so the order is fixed.
 *
 * @since 2026.11
 */
public enum BoardMount implements IStringSerializable {
  /** Hung under a ceiling on two rods: the item used on a block's underside. */
  HUNG("hung"),
  /** Raised on a post (two for the 5 x 2), its bottom two blocks up: used on the ground's top. */
  POST("post"),
  /** Flat on a wall, one-sided: used on a wall's side. */
  WALL("wall"),
  /** Standing on the ground itself: the info totem. */
  FLOOR("floor");

  private static final BoardMount[] VALUES = values();

  private final String name;

  BoardMount(String name) {
    this.name = name;
  }

  /** The mount with an ordinal; HUNG for one out of range. */
  public static BoardMount byOrdinal(int ordinal) {
    return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : HUNG;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
