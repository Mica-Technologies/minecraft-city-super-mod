package com.micatechnologies.minecraft.csm.venues.seating;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * Which part of an aisle's slope a rail or guard cell draws, read from the aisle under it: the
 * pieces follow the aisle's nosing line, so cells side by side make one straight rail.
 *
 * <p>An aisle beside 2-deep rows climbs 1 in 2: a tread level with the legroom, then a stair (a
 * half step, then the next row's tread a block up). Its rail cells are the air over the tread
 * ({@link #SHALLOW_LOW}) and the air over the stair, a block higher ({@link #SHALLOW_HIGH}),
 * repeating. An aisle beside 1-deep rows is a stair a row, 1 in 1, every cell over a stair
 * ({@link #STEEP}). A landing or cross aisle is {@link #LEVEL}. Ordinals are saved in metadata:
 * append only.</p>
 *
 * @since 2026.10
 */
public enum StadiumSlope implements IStringSerializable {
  /** Flat: a landing, a cross aisle, a tier front. */
  LEVEL("level"),
  /** 1 in 1, over a stair of an aisle beside 1-deep rows. */
  STEEP("steep"),
  /** 1 in 2, over the tread level with a 2-deep row's legroom. */
  SHALLOW_LOW("shallow_low"),
  /** 1 in 2, over the stair beside a 2-deep row's seat, a block above the tread before it. */
  SHALLOW_HIGH("shallow_high");

  private final String name;

  StadiumSlope(String name) {
    this.name = name;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  /** The next piece, for a sneak-click. */
  public StadiumSlope next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
