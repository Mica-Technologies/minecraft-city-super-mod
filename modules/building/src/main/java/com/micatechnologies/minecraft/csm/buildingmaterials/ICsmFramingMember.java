package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A block that is part of a framed wall.
 *
 * <p>Framing joins framing. A run may change along its length without ever stopping being one
 * wall: a plain bay becomes a bay with a door opening, becomes a braced bay, becomes a bay again,
 * and steel becomes wood. All of those are studs standing in a plate, and all of them join.</p>
 *
 * <p>The mixed steel-and-wood wall is not an edge case. It is the first thing in the photograph
 * this family was built from: tan posts standing in the same plane as the galvanised studs, in one
 * wall, not in two walls meeting at a corner. An earlier version of this interface carried a
 * framing KIND and refused to join across it, which was wrong about exactly the case that prompted
 * the work.</p>
 *
 * <p>{@link #joinsFraming} survives as the place to refuse a join, for a member that one day needs
 * to — something that is framing but is not a wall, or a wall system that genuinely butts rather
 * than continues. Nothing needs it yet, so the default is yes.</p>
 *
 * @version 2.0
 * @see FramingJoins
 * @since 2026.9
 */
public interface ICsmFramingMember {

  /**
   * Whether this member continues into the given neighbour.
   *
   * <p>Asked of the block doing the drawing, about the block beside it. A member that refuses here
   * shows its own end rather than running on, which is what a wall does where it dies into
   * something it is not part of.</p>
   *
   * @param neighbour the framing member alongside
   *
   * @return {@code true} if the two are one run
   *
   * @since 2.0
   */
  default boolean joinsFraming(ICsmFramingMember neighbour) {
    return true;
  }
}
