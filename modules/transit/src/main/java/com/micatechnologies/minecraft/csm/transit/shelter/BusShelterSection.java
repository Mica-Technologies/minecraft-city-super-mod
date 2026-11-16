package com.micatechnologies.minecraft.csm.transit.shelter;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * Which section of its column a bus shelter block is, from the ground up. Actual state, worked
 * out from the column ({@link BlockBusShelter#getActualState}); only {@link #TOP} is stored.
 *
 * @since 2026.11
 */
public enum BusShelterSection implements IStringSerializable {
  /** The section on the ground: the kick rails, the bench, the foot of the ad panel frame. */
  BOTTOM("bottom"),
  /** The one above it: the frit band at eye level, the head of the ad panel frame. */
  SECOND("second"),
  /** Any section between the second and the top: posts and plain glass, more headroom. */
  MIDDLE("middle"),
  /** The top section, which carries the roof, its fascia and its light. */
  TOP("top");

  private final String name;

  BusShelterSection(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
