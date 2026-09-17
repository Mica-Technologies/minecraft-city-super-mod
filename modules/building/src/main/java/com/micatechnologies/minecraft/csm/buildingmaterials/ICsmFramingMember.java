package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A block that is part of a framed wall, and which framing system it belongs to.
 *
 * <p>This exists so that a run joins on the FRAMING, not on block identity. A real wall changes
 * along its length without ever stopping being one wall: a plain bay becomes a bay with a door
 * opening, becomes a braced bay, becomes a bay again. All of those are the same studs in the same
 * track, so all of them join.</p>
 *
 * <p>What must match is the {@link #getFramingKind() kind}. Steel studs do not join wood studs:
 * the two are different sections at different depths, sitting in different track, and a real
 * building that mixes them does so in separate walls that meet at a corner — which is exactly
 * what happens here too, because a corner is what a non-matching neighbour produces.</p>
 *
 * <p>Deliberately a string rather than an enum, following {@code ICsmGuardrailRail}: the framing
 * families arrive one phase at a time, and a string keeps a new one from being a change to a
 * shared enum that every block already switches on.</p>
 *
 * @version 1.0
 * @see FramingJoins
 * @since 2026.9
 */
public interface ICsmFramingMember {

  /**
   * The framing system this block belongs to. Two framing members join only if these match.
   *
   * @return the framing kind, never null
   *
   * @since 1.0
   */
  String getFramingKind();

  /**
   * Whether this block will join a run of {@code kind}.
   *
   * <p>Asked instead of comparing kinds directly so that a piece belonging to two systems at once
   * can say so. Nothing needs that yet; the transition pieces that will — a wall that changes from
   * steel to wood part way along, if one is ever added — would override this rather than forcing
   * every caller to special-case them.</p>
   *
   * @param kind the framing the neighbour presents
   *
   * @return true if the two will join
   *
   * @since 1.0
   */
  default boolean acceptsFraming(String kind) {
    return getFramingKind().equals(kind);
  }
}
