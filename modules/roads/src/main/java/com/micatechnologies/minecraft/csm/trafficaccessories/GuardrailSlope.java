package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.IStringSerializable;

/**
 * How a guardrail's rail runs across its own cell: level, or ramping to meet a neighbour a block
 * higher.
 *
 * <p>Real guardrail follows the grade. A run built out of level cells staircases up a hill, which
 * is obvious on anything but the gentlest ground, so a cell next to a neighbour a block up ramps to
 * meet it instead.</p>
 *
 * <p><b>Both ramps are drawn in the LOWER of the two cells.</b> The higher cell is standing on
 * something solid, and a rail ramping down out of it spends the last part of its run inside that
 * block: the ramp appears to rise out of the top of the wall while the lower run butts into its
 * side. Which end of the lower cell the ramp climbs toward is what tells {@link #UP} from
 * {@link #DOWN}.</p>
 *
 * <p>Derived from the neighbours in {@code getActualState} rather than stored, exactly as the
 * connections are, so there is nothing for the player to set and nothing to keep in step when a run
 * is cut in half.</p>
 *
 * @version 1.1
 * @see GuardrailJoins#resolve
 * @since 2026.9
 */
public enum GuardrailSlope implements IStringSerializable {

  /** Level across the cell. */
  FLAT("flat"),

  /** Rises across the cell toward its RIGHT-hand end, to a neighbour a block up there. */
  UP("up"),

  /** Falls across the cell from a neighbour a block up at its LEFT-hand end, finishing level. */
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
   * The slope that best draws a rail whose neighbours sit {@code riseRight} and {@code riseLeft}
   * blocks above it.
   *
   * <p>Only a neighbour ABOVE this cell ramps it. One below is the higher cell of its own pair
   * with this one, so it is that neighbour that ramps up to meet this rail, not this rail that
   * ramps down to meet it — see the class comment for what goes wrong otherwise.</p>
   *
   * <p>A cell with a higher neighbour on both sides — a single cell in a dip — can ramp to only
   * one of them. The right-hand one wins and the left-hand joint stays a step, which is a smaller
   * lie than a ramp drawn inside a wall.</p>
   *
   * <p>Takes real heights rather than differences of block positions, and that is the whole point
   * of it. A guardrail settles onto whatever it is standing on, so two cells a block apart in Y can
   * be a hair apart in the world — one on bare ground and its neighbour on a snow layer is the
   * everyday case — and a rail that read the block positions would answer that with a full
   * block's ramp and dive into the ground. What decides the shape is where the rails actually
   * are.</p>
   *
   * @param riseRight how far the right-hand neighbour's rail sits above this one's, in blocks, or
   *                  {@link Double#NEGATIVE_INFINITY} if no rail joins on that side
   * @param riseLeft  the same for the left-hand neighbour
   *
   * @return the slope to draw
   *
   * @see GuardrailJoins#resolve
   * @since 1.1
   */
  public static GuardrailSlope forRises(double riseRight, double riseLeft) {
    if (riseRight >= RAMP_AT) {
      return UP;
    }
    if (riseLeft >= RAMP_AT) {
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
