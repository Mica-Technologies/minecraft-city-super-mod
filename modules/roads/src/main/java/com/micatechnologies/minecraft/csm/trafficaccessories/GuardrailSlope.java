package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.IStringSerializable;

/**
 * How a guardrail's rail runs across its own cell: level, or ramping to meet a neighbour a block
 * higher or lower.
 *
 * <p>Real guardrail follows the grade. A run built out of level cells staircases up a hill, which
 * is obvious on anything but the gentlest ground, so a cell whose right-hand neighbour sits a block
 * away in Y ramps to meet it instead.</p>
 *
 * <p>Derived from the neighbours in {@code getActualState} rather than stored, exactly as the
 * connections are, so there is nothing for the player to set and nothing to keep in step when a run
 * is cut in half.</p>
 *
 * @version 1.0
 * @see GuardrailJoins#resolve
 * @since 2026.9
 */
public enum GuardrailSlope implements IStringSerializable {

  /** Level across the cell. */
  FLAT("flat"),

  /** Rises across the cell toward its RIGHT-hand end. */
  UP("up"),

  /** Falls across the cell toward its RIGHT-hand end. */
  DOWN("down");

  private final String name;

  GuardrailSlope(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
