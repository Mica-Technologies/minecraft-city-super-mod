package com.micatechnologies.minecraft.csm.buildingmaterials.stairs;

import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A railing cell's collision, kept apart from the block so it can be checked without a game.
 *
 * <p>Each edge collides in a thin slab along it. The slab is thinner than the drawn railing on
 * purpose: a stair one block wide carries a railing on both sides of the same cell, and the lane
 * between the two slabs has to stay wider than a player (0.6) with room to spare, or the flight
 * cannot be climbed (#368). A slab as thin as a glass pane still stops a player walking through
 * it sideways.</p>
 */
final class RailingCollision {

  private static final double P = 1.0 / 16;
  /**
   * How far each edge's collision reaches into the cell. Two opposite edges leave {@code 1 - 2 *
   * DEPTH} between them: 0.75 at 2 px, so a player can walk in a little off centre.
   */
  static final double DEPTH = 2 * P;
  /** A guard's collision on the cell's north edge, as tall as a fence. */
  static final AxisAlignedBB GUARD_NORTH = new AxisAlignedBB(0, 0, 0, 1, 1.5, DEPTH);
  /** A wall handrail's collision on the north wall: where the rail runs, level or sloped. */
  static final AxisAlignedBB WALL_NORTH = new AxisAlignedBB(0, 7 * P, 0, 1, 24 * P, DEPTH);

  private RailingCollision() {
  }

  /**
   * The collision boxes of a cell with railings on the given edges, in block coordinates.
   *
   * @param kind  what the railing is
   * @param north whether it has a railing on its north edge
   * @param east  on its east edge
   * @param south on its south edge
   * @param west  on its west edge
   * @return one box per edge, rotated from the north edge's
   */
  static List<AxisAlignedBB> boxes(BlockStairRailing.Kind kind, boolean north, boolean east,
      boolean south, boolean west) {
    AxisAlignedBB base = kind == BlockStairRailing.Kind.WALL ? WALL_NORTH : GUARD_NORTH;
    List<AxisAlignedBB> out = new ArrayList<>(4);
    if (north) {
      out.add(base);
    }
    if (east) {
      out.add(RotationUtils.rotateBoundingBoxByFacing(base, EnumFacing.EAST));
    }
    if (south) {
      out.add(RotationUtils.rotateBoundingBoxByFacing(base, EnumFacing.SOUTH));
    }
    if (west) {
      out.add(RotationUtils.rotateBoundingBoxByFacing(base, EnumFacing.WEST));
    }
    return out;
  }
}
