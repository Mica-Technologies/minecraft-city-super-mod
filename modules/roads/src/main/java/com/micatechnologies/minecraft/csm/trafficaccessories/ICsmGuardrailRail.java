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
   * The rail section this block presents at its LEFT-hand end.
   *
   * <p>The same as {@link #getRailKind()} for everything except a transition piece, which is the
   * whole reason these exist: a W-to-thrie transition is W-beam at one end and thrie beam at the
   * other, and a run has to be able to carry straight through it. Without the distinction the
   * transition would join neither side, because it matches neither rail.</p>
   *
   * @return the rail kind at the left-hand end, never null
   *
   * @since 1.0
   */
  default String getRailKindOnLeft() {
    return getRailKind();
  }

  /**
   * The rail section this block presents at its RIGHT-hand end.
   *
   * @return the rail kind at the right-hand end, never null
   *
   * @see #getRailKindOnLeft()
   * @since 1.0
   */
  default String getRailKindOnRight() {
    return getRailKind();
  }

  /**
   * Whether this block will join a run carrying {@code kind} at one of its ends.
   *
   * <p>Asked instead of comparing rail kinds directly so a transition piece can answer yes to
   * BOTH of the rails it joins, at either end. A transition is chiral and rotating it does not
   * change which end is which — facing here means the way the rail looks, so a transition turned
   * round points away from the road. Letting it accept either rail at either end is what lets the
   * same block serve a run going W-to-thrie and one going thrie-to-W, with the model mirrored to
   * suit.</p>
   *
   * @param kind      the rail the neighbouring run presents
   * @param onLeftEnd true to ask about this block's left-hand end, false for its right
   *
   * @return true if the two will join
   *
   * @since 1.0
   */
  default boolean acceptsRail(String kind, boolean onLeftEnd) {
    return kind.equals(onLeftEnd ? getRailKindOnLeft() : getRailKindOnRight());
  }

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
