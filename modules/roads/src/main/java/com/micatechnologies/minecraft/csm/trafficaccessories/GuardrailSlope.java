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

  /**
   * How much of a block a rail has to be climbing before it ramps rather than staying level.
   *
   * <p>Half, because these three are the only shapes there are: a rail ramps by exactly one block
   * or not at all, so every rise is drawn as whichever of those two is nearer. A rise of a
   * quarter block left as a step is a smaller lie than one drawn as a whole block's ramp.</p>
   */
  private static final double RAMP_AT = 0.5;

  private final String name;

  GuardrailSlope(String name) {
    this.name = name;
  }

  /**
   * The slope that best draws a rail climbing {@code rise} blocks across its own cell.
   *
   * <p>Takes a real height rather than a difference of block positions, and that is the whole
   * point of it. A guardrail settles onto whatever it is standing on, so two cells a block apart
   * in Y can be a hair apart in the world — one on bare ground and its neighbour on a snow layer
   * is the everyday case — and a rail that read the block positions would answer that with a
   * full block's ramp and dive into the ground. What decides the shape is where the two rails
   * actually are.</p>
   *
   * @param rise how far the right-hand neighbour's rail sits above this one's, in blocks
   *
   * @return the slope to draw
   *
   * @see GuardrailJoins#resolve
   * @since 1.0
   */
  public static GuardrailSlope forRise(double rise) {
    if (rise >= RAMP_AT) {
      return UP;
    }
    if (rise <= -RAMP_AT) {
      return DOWN;
    }
    return FLAT;
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
