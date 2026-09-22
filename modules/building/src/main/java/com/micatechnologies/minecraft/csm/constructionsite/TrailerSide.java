package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * What is on one side of a job trailer block: more trailer, the outside, or the inside of the
 * trailer.
 *
 * <p>One three-valued property a side, as the scaffold's {@link ScaffoldSide}: six of them and the
 * window's two stacking flags come to 2,916 states, where a neighbour flag and an inside flag a
 * side would be 16,384.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum TrailerSide implements IStringSerializable {

  /** Another trailer block: nothing is drawn on this side. */
  JOINED("joined"),

  /** The outside: siding, the roof, or the underside. */
  OUT("out"),

  /** The inside of the trailer: panelling, the floor, or the ceiling. */
  IN("in");

  private final String name;

  TrailerSide(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
