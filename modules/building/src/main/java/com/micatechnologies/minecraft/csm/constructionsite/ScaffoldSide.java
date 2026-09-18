package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * What is on one side of a scaffold bay: more scaffold, nothing, or nothing with a guardrail.
 *
 * <p>One three-valued property per side rather than a neighbour flag and a rail flag. Block states
 * multiply as a product, and with the add-ons stored the two-flag form came to 16,384 states;
 * three values a side brings it to 5,184, the way vanilla's redstone wire keeps its count down.
 * A rail only ever stands on an open side, so the fourth combination never existed anyway.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum ScaffoldSide implements IStringSerializable {

  /** Another scaffold: the run carries on. */
  SCAFFOLD("scaffold"),

  /** Not a scaffold. */
  OPEN("open"),

  /** Not a scaffold, and the top deck's edge there is a drop, so it carries a guardrail. */
  RAIL("rail");

  private final String name;

  ScaffoldSide(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }
}
