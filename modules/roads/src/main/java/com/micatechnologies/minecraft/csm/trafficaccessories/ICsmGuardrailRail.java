package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * A block that is part of a guardrail run, and which rail it carries.
 *
 * <p>This exists because a guardrail joins on a different rule from everything else in the work
 * zone tab. A barricade joins only to the identical block: its striping slopes toward the side
 * traffic should pass, so a keep-left meeting a keep-right contradicts itself mid-run, and
 * {@link WorkZoneJoins} is right to compare block identity.</p>
 *
 * <p>A guardrail is the opposite case. A real run changes post material part way along, and picks
 * up a second rail where it becomes a median barrier, without ever stopping being one run. What
 * must match is the RAIL — W-beam does not join thrie-beam, because those are different sections
 * and a real transition between them is its own piece of hardware.</p>
 *
 * @version 1.0
 * @see GuardrailJoins
 * @since 2026.9
 */
public interface ICsmGuardrailRail {

  /**
   * The rail section this block carries. Two guardrails join only if these match.
   *
   * <p>Deliberately a string rather than an enum: the rail types arrive one phase at a time, and a
   * string keeps a new one from being a change to a shared enum every block already switches on.</p>
   *
   * @return the rail kind, never null
   *
   * @since 1.0
   */
  String getRailKind();

  /**
   * Whether this block terminates a run rather than continuing it.
   *
   * <p>An end treatment carries the same rail and joins to the run, but nothing joins THROUGH it —
   * a run does not continue out the far side of a flared shoe.</p>
   *
   * @return true if this is an end treatment
   *
   * @since 1.0
   */
  default boolean isRunEnd() {
    return false;
  }
}
