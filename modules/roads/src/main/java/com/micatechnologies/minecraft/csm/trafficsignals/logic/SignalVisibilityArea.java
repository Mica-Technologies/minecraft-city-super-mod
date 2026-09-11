package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ground area a signal head has been programmed to be seen from.
 *
 * <p>A polygon of up to {@link #MAX_POINTS} clicked ground blocks, in the order they were clicked.
 * Each vertex is the <em>centre</em> of its block in the horizontal plane; the surface the area
 * sits on is one above the clicked block's Y. Two points are expanded into an axis-aligned
 * rectangle at construction so the simple case stays two clicks, the way the sensor zone tool
 * works.</p>
 *
 * <p>One area per head serves two features. The programmable visibility visor projects the
 * viewer's eye through the lens onto this polygon and lights only when the projection lands
 * inside it. A horizontal louvered visor reads its visible elevation band from the polygon's
 * nearest and farthest vertices, which is how "visible from here to there" becomes a slat angle.
 * Vertical louvers do not use it: they are aimed by turning the head.</p>
 *
 * <p>Deliberately free of Minecraft types so it can be unit tested. The head's tile entity
 * serialises it as one int array under a short NBT key.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class SignalVisibilityArea {

  /** The most points a polygon may have. Eight covers a curved approach; more is a drawing. */
  public static final int MAX_POINTS = 8;

  /** Fewest points that make an area: two become a rectangle. */
  public static final int MIN_POINTS = 2;

  /** x, y, z triples, world block coordinates of the clicked blocks, in polygon order. */
  private final int[] points;

  private SignalVisibilityArea(int[] points) {
    this.points = points;
  }

  /**
   * Builds an area from clicked block positions.
   *
   * @param clicked block positions as {x, y, z} triples in click order; between
   *                {@link #MIN_POINTS} and {@link #MAX_POINTS} of them
   *
   * @return the area, or {@code null} if the count is out of range
   */
  public static SignalVisibilityArea of(List<int[]> clicked) {
    if (clicked == null || clicked.size() < MIN_POINTS || clicked.size() > MAX_POINTS) {
      return null;
    }
    if (clicked.size() == 2) {
      // Two clicks are opposite corners of a rectangle. Stored as its four corner blocks so the
      // renderer never has to special-case the shape. The rectangle sits at the height of the
      // first click; a slope between the two corners is not something two clicks can describe.
      int[] a = clicked.get(0);
      int[] b = clicked.get(1);
      int minX = Math.min(a[0], b[0]);
      int maxX = Math.max(a[0], b[0]);
      int minZ = Math.min(a[2], b[2]);
      int maxZ = Math.max(a[2], b[2]);
      int y = Math.min(a[1], b[1]);
      return new SignalVisibilityArea(new int[]{
          minX, y, minZ,
          maxX, y, minZ,
          maxX, y, maxZ,
          minX, y, maxZ});
    }
    int[] flat = new int[clicked.size() * 3];
    for (int i = 0; i < clicked.size(); i++) {
      int[] p = clicked.get(i);
      flat[i * 3] = p[0];
      flat[i * 3 + 1] = p[1];
      flat[i * 3 + 2] = p[2];
    }
    return new SignalVisibilityArea(flat);
  }

  /**
   * Restores an area from its serialised form.
   *
   * @param data the array from {@link #toIntArray()}
   *
   * @return the area, or {@code null} if the array is empty, malformed or has too many points
   */
  public static SignalVisibilityArea fromIntArray(int[] data) {
    if (data == null || data.length < MIN_POINTS * 3 || data.length % 3 != 0
        || data.length > MAX_POINTS * 3) {
      return null;
    }
    return new SignalVisibilityArea(Arrays.copyOf(data, data.length));
  }

  /**
   * The serialised form: x, y, z triples in polygon order.
   *
   * @return a fresh copy of the point array
   */
  public int[] toIntArray() {
    return Arrays.copyOf(points, points.length);
  }

  /** @return how many vertices the polygon has. */
  public int pointCount() {
    return points.length / 3;
  }

  /** @return the clicked block X of vertex {@code i}. */
  public int getBlockX(int i) {
    return points[i * 3];
  }

  /** @return the clicked block Y of vertex {@code i}. */
  public int getBlockY(int i) {
    return points[i * 3 + 1];
  }

  /** @return the clicked block Z of vertex {@code i}. */
  public int getBlockZ(int i) {
    return points[i * 3 + 2];
  }

  /** @return vertex {@code i}'s X in the horizontal plane: the centre of its block. */
  public double vertexX(int i) {
    return getBlockX(i) + 0.5;
  }

  /** @return vertex {@code i}'s Z in the horizontal plane: the centre of its block. */
  public double vertexZ(int i) {
    return getBlockZ(i) + 0.5;
  }

  /**
   * The Y of the ground the area lies on: one above the mean clicked block, since the clicked
   * block is the road surface and a viewer stands on top of it.
   *
   * @return the surface height in world coordinates
   */
  public double surfaceY() {
    double sum = 0.0;
    int n = pointCount();
    for (int i = 0; i < n; i++) {
      sum += getBlockY(i);
    }
    return sum / n + 1.0;
  }

  /**
   * Index of the vertex horizontally nearest to a point.
   *
   * @param fromX the point's X
   * @param fromZ the point's Z
   *
   * @return the vertex index
   */
  public int nearestVertex(double fromX, double fromZ) {
    int best = 0;
    double bestDistanceSquared = Double.MAX_VALUE;
    for (int i = 0; i < pointCount(); i++) {
      double dx = vertexX(i) - fromX;
      double dz = vertexZ(i) - fromZ;
      double d = dx * dx + dz * dz;
      if (d < bestDistanceSquared) {
        bestDistanceSquared = d;
        best = i;
      }
    }
    return best;
  }

  /**
   * Index of the vertex horizontally farthest from a point.
   *
   * @param fromX the point's X
   * @param fromZ the point's Z
   *
   * @return the vertex index
   */
  public int farthestVertex(double fromX, double fromZ) {
    int best = 0;
    double bestDistanceSquared = -1.0;
    for (int i = 0; i < pointCount(); i++) {
      double dx = vertexX(i) - fromX;
      double dz = vertexZ(i) - fromZ;
      double d = dx * dx + dz * dz;
      if (d > bestDistanceSquared) {
        bestDistanceSquared = d;
        best = i;
      }
    }
    return best;
  }

  /**
   * Whether a point in the horizontal plane lies inside the polygon. Ray casting; a point exactly
   * on an edge may fall either way, which the caller's fade hides.
   *
   * @param px the point's X
   * @param pz the point's Z
   *
   * @return true if inside
   */
  public boolean containsXZ(double px, double pz) {
    int n = pointCount();
    boolean inside = false;
    for (int i = 0, j = n - 1; i < n; j = i++) {
      double xi = vertexX(i);
      double zi = vertexZ(i);
      double xj = vertexX(j);
      double zj = vertexZ(j);
      boolean crosses = (zi > pz) != (zj > pz);
      if (crosses) {
        double xAtZ = xj + (pz - zj) * (xi - xj) / (zi - zj);
        if (px < xAtZ) {
          inside = !inside;
        }
      }
    }
    return inside;
  }

  /**
   * The point on the polygon's boundary nearest to a point in the horizontal plane.
   *
   * @param px the point's X
   * @param pz the point's Z
   *
   * @return {x, z} of the nearest boundary point
   */
  public double[] nearestBoundaryPoint(double px, double pz) {
    int n = pointCount();
    double bestX = vertexX(0);
    double bestZ = vertexZ(0);
    double bestDistanceSquared = Double.MAX_VALUE;
    for (int i = 0, j = n - 1; i < n; j = i++) {
      double ax = vertexX(j);
      double az = vertexZ(j);
      double bx = vertexX(i);
      double bz = vertexZ(i);
      double ex = bx - ax;
      double ez = bz - az;
      double lengthSquared = ex * ex + ez * ez;
      double t = lengthSquared <= 0.0 ? 0.0
          : ((px - ax) * ex + (pz - az) * ez) / lengthSquared;
      t = Math.max(0.0, Math.min(1.0, t));
      double qx = ax + ex * t;
      double qz = az + ez * t;
      double dx = qx - px;
      double dz = qz - pz;
      double d = dx * dx + dz * dz;
      if (d < bestDistanceSquared) {
        bestDistanceSquared = d;
        bestX = qx;
        bestZ = qz;
      }
    }
    return new double[]{bestX, bestZ};
  }

  /**
   * Whether every vertex lies within a horizontal distance of a point. Used to reject a polygon
   * programmed half a world away from its head.
   *
   * @param x        the point's X
   * @param z        the point's Z
   * @param maxRange the farthest a vertex may be, in blocks
   *
   * @return true if all vertices are within range
   */
  public boolean isWithinRange(double x, double z, double maxRange) {
    double limit = maxRange * maxRange;
    for (int i = 0; i < pointCount(); i++) {
      double dx = vertexX(i) - x;
      double dz = vertexZ(i) - z;
      if (dx * dx + dz * dz > limit) {
        return false;
      }
    }
    return true;
  }

  /** @return the vertices as a list of {x, y, z} block triples, in polygon order. */
  public List<int[]> toPointList() {
    List<int[]> out = new ArrayList<>(pointCount());
    for (int i = 0; i < pointCount(); i++) {
      out.add(new int[]{getBlockX(i), getBlockY(i), getBlockZ(i)});
    }
    return out;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SignalVisibilityArea
        && Arrays.equals(points, ((SignalVisibilityArea) other).points);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(points);
  }

  @Override
  public String toString() {
    return "SignalVisibilityArea" + Arrays.toString(points);
  }
}
