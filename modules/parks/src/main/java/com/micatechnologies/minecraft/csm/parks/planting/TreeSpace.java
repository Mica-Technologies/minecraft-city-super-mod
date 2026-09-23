package com.micatechnologies.minecraft.csm.parks.planting;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;

/**
 * What a tree finds around the spot it is planted, as its generator asks about it: cell by cell,
 * relative to the base of the trunk. A tree grows into the room it has, the way a street tree
 * grown against a building ends up leaning away from it and pruned flat on that side.
 *
 * <p>Three answers, not two. A cell of another tree's foliage cannot be planted into, but it is
 * not an obstacle either: a limb may grow up to it and a canopy may meet it, so a row planted a
 * few blocks apart joins into one canopy over the street rather than each tree shying away from
 * the next.</p>
 *
 * @since 2026.9
 */
@FunctionalInterface
public interface TreeSpace {

  /** What a cell holds, as far as growing into it goes. */
  enum Cell {
    /** Nothing a tree cannot grow through: air, plants, snow, a ground cover. */
    FREE,
    /** Another tree's leaves: not planted into, but not a wall to keep off either. */
    FOLIAGE,
    /** Anything else: a wall, a roof, a pole, a log. */
    BLOCKED
  }

  /** Open ground in every direction: how a tree grows with nothing near it. */
  TreeSpace OPEN = rel -> Cell.FREE;

  /**
   * What the cell holds.
   *
   * @param rel a position relative to the base of the trunk
   *
   * @return its cell
   */
  Cell at(BlockPos rel);

  default boolean free(BlockPos rel) {
    return at(rel) == Cell.FREE;
  }

  default boolean blocked(BlockPos rel) {
    return at(rel) == Cell.BLOCKED;
  }

  /**
   * This space, answering each cell once. A generator asks about the same cells many times, and
   * a world lookup is not free.
   *
   * @return a caching view of this space
   */
  default TreeSpace cached() {
    Map<BlockPos, Cell> seen = new HashMap<>();
    return rel -> seen.computeIfAbsent(rel, this::at);
  }
}
