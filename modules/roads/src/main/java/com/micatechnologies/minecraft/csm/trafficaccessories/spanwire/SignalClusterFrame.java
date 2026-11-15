package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The footprint and frame-centre rules of a signal cluster hanger, kept free of any world so they
 * can be tested on their own.
 *
 * <p>A hanger sits in the block above the middle of its cluster, under the arm or the wire. It
 * claims the heads in the three-by-three columns directly beneath it, one row down, and centres its
 * frame on the points where their drops land -- which is what puts the tee of a pair across two
 * columns on their shared edge rather than over one of them.
 */
public final class SignalClusterFrame {

  /** How far the footprint reaches from the hanger's own column, either way along each axis. */
  public static final int REACH = 1;

  private SignalClusterFrame() {
  }

  /**
   * Adds the columns a hanger at {@code hanger} claims heads from: its own first, then its eight
   * neighbours, all at the hanger's own height.
   *
   * @param hanger the hanger's position.
   * @param out    the list to add to.
   */
  public static void footprint(BlockPos hanger, List<BlockPos> out) {
    out.add(hanger);
    for (int dx = -REACH; dx <= REACH; dx++) {
      for (int dz = -REACH; dz <= REACH; dz++) {
        if (dx != 0 || dz != 0) {
          out.add(hanger.add(dx, 0, dz));
        }
      }
    }
  }

  /**
   * Whether a head at {@code head} is one a hanger at {@code hanger} carries: in the footprint, and
   * in the row directly under the hanger. A head lower down in the same column is not carried, even
   * though it may find the hanger when it looks up for a mount; nothing reaches down to it.
   *
   * @param hanger the hanger's position.
   * @param head   the head's position.
   *
   * @return true when the hanger's frame drops to this head.
   */
  public static boolean carries(BlockPos hanger, BlockPos head) {
    return head.getY() == hanger.getY() - 1
        && Math.abs(head.getX() - hanger.getX()) <= REACH
        && Math.abs(head.getZ() - hanger.getZ()) <= REACH;
  }

  /**
   * The horizontal centre of a set of points, at the fallback's height.
   *
   * <p>The mean, not the middle of the bounding box: for the shapes a hanger carries (a pair, a
   * row, a T, a plus) the two only differ for the T, and there the mean sits nearer the stem, which
   * is where the real spider's hub is.
   *
   * @param points   the points to centre on.
   * @param fallback returned as is when there are no points; its Y is always kept.
   *
   * @return the centroid at the fallback's height.
   */
  public static Vec3d centroid(List<Vec3d> points, Vec3d fallback) {
    if (points.isEmpty()) {
      return fallback;
    }
    double x = 0.0;
    double z = 0.0;
    for (Vec3d point : points) {
      x += point.x;
      z += point.z;
    }
    return new Vec3d(x / points.size(), fallback.y, z / points.size());
  }
}
