package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A tree log's shape, from its width and its connection mask ({@link TreeLogConnections}). Pure
 * math, no client classes: the baked model turns {@link #quads} into quads with the bark sprite,
 * and the block turns {@link #boxes} into its collision and selection boxes.
 *
 * <p>All coordinates are in sixteenths of a block, the cell centre at (8, 8, 8). The log is a set
 * of <b>arms</b>, each an eight-sided tube from the centre outward:</p>
 * <ul>
 *   <li>to each face with a log beyond it, tapering to that log's radius if it is thinner;</li>
 *   <li>to the shared edge of each edge-diagonal log (the bridge that joins a stepped trunk);</li>
 *   <li>into leaves the mask says it reaches, narrowing to a twig;</li>
 *   <li>down to solid ground, flaring out;</li>
 *   <li>and, where a log has one connection and nothing on the other side, straight on to the
 *       opposite block edge, so a limb or a trunk ends at the block edge rather than mid-block.</li>
 * </ul>
 * <p>Where arms meet at an angle a knuckle, a slightly wider short prism, covers the joint. A
 * full-width log is a plain cube, as a vanilla log is: full blocks already meet at their edges.</p>
 *
 * @since 2026.9
 */
public final class TreeLogGeometry {

  /** Sides of a log's cross-section, for the widths that get the most. */
  static final int SIDES = 8;

  /**
   * Sides of a log's cross-section at a width: a 2 px twig reads as round with four, a 4 px thin
   * log with six, and only the wider ones need eight. Every side is a quad per arm, and most of
   * a tree's logs are its thin limbs.
   */
  static int sides(TreeLogWidth width) {
    switch (width) {
      case TWIG:
        return 4;
      case THIN:
        return 6;
      default:
        return SIDES;
    }
  }
  /** How much wider than the log a flare is where it meets the ground. */
  static final double FLARE = 1.4;
  /** A limb's radius where it enters leaves, as a fraction of its own. */
  static final double INTO_LEAVES = 0.6;
  /** A free end's radius at the block edge, as a fraction of its own. */
  static final double FREE_END = 0.85;
  /** A knuckle's radius, as a fraction of the log's. */
  static final double KNUCKLE = 1.12;

  /** One quad: four corners, each with a position, a UV (0-16) and a normal. */
  public static final class Quad {

    public final double[][] pos = new double[4][];
    public final double[][] uv = new double[4][];
    public final double[][] normal = new double[4][];

    /** The face normal, from the corner order (outward by construction). */
    public double[] faceNormal() {
      double[] a = sub(pos[1], pos[0]);
      double[] b = sub(pos[2], pos[0]);
      return normalize(cross(a, b));
    }
  }

  /** One arm: a tube from {@code from} (radius r0) to {@code to} (radius r1). */
  static final class Arm {

    final double[] from;
    final double[] to;
    final double r0;
    final double r1;
    final boolean capEnd;

    Arm(double[] from, double[] to, double r0, double r1, boolean capEnd) {
      this.from = from;
      this.to = to;
      this.r0 = r0;
      this.r1 = r1;
      this.capEnd = capEnd;
    }

    double[] dir() {
      return normalize(sub(to, from));
    }
  }

  private static final double[] CENTRE = {8, 8, 8};

  private TreeLogGeometry() {
  }

  /**
   * The arms a log draws.
   *
   * @param width the log's width
   * @param mask  its connection mask
   *
   * @return the arms, in a fixed order
   */
  static List<Arm> arms(TreeLogWidth width, long mask) {
    double r = width.getPixels() / 2.0;
    List<Arm> arms = new ArrayList<>();
    int connections = 0;
    double[] onlyDir = null;

    for (EnumFacing f : EnumFacing.values()) {
      int i = f.getIndex();
      double[] d = {f.getXOffset(), f.getYOffset(), f.getZOffset()};
      if (TreeLogConnections.faceLog(mask, i)) {
        TreeLogWidth other = TreeLogWidth.fromMaskIndex(TreeLogConnections.faceWidthIndex(mask, i));
        double rEnd = other == null ? r : Math.min(r, other.getPixels() / 2.0);
        arms.add(new Arm(CENTRE, edge(d), r, rEnd, false));
        connections++;
        onlyDir = d;
      }
    }
    for (int i = 0; i < TreeLogConnections.DIAGONALS.length; i++) {
      if (TreeLogConnections.diagonal(mask, i)) {
        int[] g = TreeLogConnections.DIAGONALS[i];
        double[] d = {g[0], g[1], g[2]};
        arms.add(new Arm(CENTRE, edge(d), r, r, false));
        connections++;
        onlyDir = d;
      }
    }
    for (EnumFacing f : EnumFacing.values()) {
      if (TreeLogConnections.faceLeaves(mask, f.getIndex())) {
        double[] d = {f.getXOffset(), f.getYOffset(), f.getZOffset()};
        arms.add(new Arm(CENTRE, edge(d), r, r * INTO_LEAVES, true));
        connections++;
        onlyDir = d;
      }
    }
    boolean ground = TreeLogConnections.ground(mask);
    if (ground) {
      // Drawn from the ground up, so the wide end is at the bottom.
      arms.add(new Arm(new double[]{8, 0, 8}, CENTRE, r * FLARE, r, false));
      connections++;
      onlyDir = new double[]{0, -1, 0};
    }

    if (connections == 0) {
      // A lone log: a straight length along its placed axis, like a vanilla log.
      EnumFacing.Axis axis = TreeLogConnections.axis(mask);
      double[] d = {axis == EnumFacing.Axis.X ? 1 : 0, axis == EnumFacing.Axis.Y ? 1 : 0,
          axis == EnumFacing.Axis.Z ? 1 : 0};
      arms.add(new Arm(CENTRE, edge(d), r, r, true));
      arms.add(new Arm(CENTRE, edge(neg(d)), r, r, true));
    } else if (connections == 1) {
      // One connection: carry on straight to the opposite block edge, and close the end.
      arms.add(new Arm(CENTRE, edge(neg(onlyDir)), r, r * FREE_END, true));
    }
    return arms;
  }

  /**
   * The log's quads.
   *
   * @param width the log's width
   * @param mask  its connection mask
   *
   * @return the quads, positions in sixteenths, UVs 0-16
   */
  public static List<Quad> quads(TreeLogWidth width, long mask) {
    List<Quad> quads = new ArrayList<>();
    if (width == TreeLogWidth.FULL) {
      cube(quads);
      return quads;
    }
    int n = sides(width);
    List<Arm> arms = arms(width, mask);
    Arm straight = straightThrough(arms);
    if (straight != null) {
      // Two arms in one straight line with one radius: one tube through the cell, half the
      // quads of two, and nothing to see at the join.
      tube(quads, straight, n);
      return quads;
    }
    for (Arm arm : arms) {
      tube(quads, arm, n);
    }
    if (bent(arms)) {
      double r = width.getPixels() / 2.0 * KNUCKLE;
      tube(quads, new Arm(new double[]{8, 8 - r, 8}, new double[]{8, 8 + r, 8}, r, r, true), n);
      capStart(quads, new Arm(new double[]{8, 8 - r, 8}, new double[]{8, 8 + r, 8}, r, r, true),
          n);
    }
    return quads;
  }

  /**
   * The one tube two arms make when they run in a straight line through the centre at one radius
   * (a trunk between two logs of its width), or null.
   */
  static Arm straightThrough(List<Arm> arms) {
    if (arms.size() != 2) {
      return null;
    }
    Arm a = arms.get(0);
    Arm b = arms.get(1);
    if (a.from != CENTRE || b.from != CENTRE || a.capEnd || b.capEnd) {
      return null;
    }
    if (dot(a.dir(), b.dir()) > -0.999) {
      return null;
    }
    double r = a.r0;
    if (Math.abs(a.r1 - r) > 1e-6 || Math.abs(b.r0 - r) > 1e-6 || Math.abs(b.r1 - r) > 1e-6) {
      return null;
    }
    return new Arm(b.to, a.to, r, r, false);
  }

  /**
   * The log's collision and selection boxes, in block units (0-1).
   *
   * @param width the log's width
   * @param mask  its connection mask
   *
   * @return the boxes
   */
  public static List<AxisAlignedBB> boxes(TreeLogWidth width, long mask) {
    List<AxisAlignedBB> boxes = new ArrayList<>();
    if (width == TreeLogWidth.FULL) {
      boxes.add(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
      return boxes;
    }
    for (Arm arm : arms(width, mask)) {
      // A diagonal arm is boxed in steps, so a thin leaning trunk does not collide with a
      // whole quadrant of air beside it.
      int steps = isDiagonal(arm.dir()) ? 3 : 1;
      double r = Math.max(arm.r0, arm.r1);
      for (int s = 0; s < steps; s++) {
        double[] a = lerp(arm.from, arm.to, (double) s / steps);
        double[] b = lerp(arm.from, arm.to, (double) (s + 1) / steps);
        boxes.add(clamp(new AxisAlignedBB(
            Math.min(a[0], b[0]) - r, Math.min(a[1], b[1]) - r, Math.min(a[2], b[2]) - r,
            Math.max(a[0], b[0]) + r, Math.max(a[1], b[1]) + r, Math.max(a[2], b[2]) + r)));
      }
    }
    return boxes;
  }

  // --- shapes ---

  private static void tube(List<Quad> quads, Arm arm, int sides) {
    double[] axis = arm.dir();
    double[][] basis = basis(axis);
    double length = len(sub(arm.to, arm.from));
    double seg = 2 * Math.PI * Math.max(arm.r0, arm.r1) / sides;
    for (int i = 0; i < sides; i++) {
      double t0 = 2 * Math.PI * i / sides + Math.PI / sides;
      double t1 = 2 * Math.PI * (i + 1) / sides + Math.PI / sides;
      double[] n0 = radial(basis, t0);
      double[] n1 = radial(basis, t1);
      double u0 = (i * seg) % 16;
      double u1 = u0 + seg;
      if (u1 > 16) {
        u0 -= u1 - 16;
        u1 = 16;
      }
      double v1 = Math.min(16, length);
      Quad q = new Quad();
      q.pos[0] = add(arm.from, scale(n0, arm.r0));
      q.pos[1] = add(arm.from, scale(n1, arm.r0));
      q.pos[2] = add(arm.to, scale(n1, arm.r1));
      q.pos[3] = add(arm.to, scale(n0, arm.r1));
      q.uv[0] = new double[]{u0, v1};
      q.uv[1] = new double[]{u1, v1};
      q.uv[2] = new double[]{u1, 0};
      q.uv[3] = new double[]{u0, 0};
      q.normal[0] = n0;
      q.normal[1] = n1;
      q.normal[2] = n1;
      q.normal[3] = n0;
      orientOutward(q, arm.from, axis);
      quads.add(q);
    }
    if (arm.capEnd) {
      cap(quads, arm.to, axis, basis, arm.r1, false, sides);
    }
  }

  private static void capStart(List<Quad> quads, Arm arm, int sides) {
    double[] axis = arm.dir();
    cap(quads, arm.from, axis, basis(axis), arm.r0, true, sides);
  }

  /** An end cap with the tube's sides, as quads fanned from one corner ((sides - 2) / 2). */
  private static void cap(List<Quad> quads, double[] centre, double[] axis, double[][] basis,
      double r, boolean facingBack, int sides) {
    double[][] ring = new double[sides][];
    for (int i = 0; i < sides; i++) {
      ring[i] = add(centre, scale(radial(basis, 2 * Math.PI * i / sides + Math.PI / sides), r));
    }
    double[] outward = facingBack ? neg(axis) : axis;
    for (int k = 0; k < (sides - 2) / 2; k++) {
      Quad q = new Quad();
      int[] idx = {0, 1 + 2 * k, 2 + 2 * k, 3 + 2 * k};
      for (int c = 0; c < 4; c++) {
        double[] p = ring[idx[c]];
        q.pos[c] = p;
        double[] local = sub(p, centre);
        q.uv[c] = new double[]{8 + dot(local, basis[0]), 8 + dot(local, basis[1])};
        q.normal[c] = outward;
      }
      if (dot(q.faceNormal(), outward) < 0) {
        reverse(q);
      }
      quads.add(q);
    }
  }

  private static void cube(List<Quad> quads) {
    double[][][] faces = {
        {{0, 0, 16}, {0, 0, 0}, {16, 0, 0}, {16, 0, 16}},     // down
        {{0, 16, 0}, {0, 16, 16}, {16, 16, 16}, {16, 16, 0}}, // up
        {{16, 16, 0}, {16, 0, 0}, {0, 0, 0}, {0, 16, 0}},     // north
        {{0, 16, 16}, {0, 0, 16}, {16, 0, 16}, {16, 16, 16}}, // south
        {{0, 16, 0}, {0, 0, 0}, {0, 0, 16}, {0, 16, 16}},     // west
        {{16, 16, 16}, {16, 0, 16}, {16, 0, 0}, {16, 16, 0}}  // east
    };
    double[][] uvs = {{0, 0}, {0, 16}, {16, 16}, {16, 0}};
    for (double[][] face : faces) {
      Quad q = new Quad();
      for (int c = 0; c < 4; c++) {
        q.pos[c] = face[c];
        q.uv[c] = uvs[c];
      }
      double[] n = q.faceNormal();
      for (int c = 0; c < 4; c++) {
        q.normal[c] = n;
      }
      quads.add(q);
    }
  }

  // --- helpers ---

  /** Whether the arms meet at an angle anywhere (not one straight line through the centre). */
  static boolean bent(List<Arm> arms) {
    List<double[]> dirs = new ArrayList<>();
    for (Arm a : arms) {
      // An arm drawn toward the centre (the flare) points the other way from the centre.
      boolean inward = a.to == CENTRE;
      dirs.add(inward ? neg(a.dir()) : a.dir());
    }
    if (dirs.size() < 2) {
      return false;
    }
    if (dirs.size() == 2) {
      return dot(dirs.get(0), dirs.get(1)) > -0.999;
    }
    return true;
  }

  private static boolean isDiagonal(double[] d) {
    int nonZero = 0;
    for (double c : d) {
      if (Math.abs(c) > 1e-6) {
        nonZero++;
      }
    }
    return nonZero > 1;
  }

  /** The point where a direction from the centre meets the cell's boundary. */
  private static double[] edge(double[] d) {
    return new double[]{8 + 8 * d[0], 8 + 8 * d[1], 8 + 8 * d[2]};
  }

  private static double[][] basis(double[] axis) {
    double[] helper = Math.abs(axis[1]) > 0.9 ? new double[]{0, 0, 1} : new double[]{0, 1, 0};
    double[] u = normalize(cross(helper, axis));
    double[] v = cross(axis, u);
    return new double[][]{u, v};
  }

  private static double[] radial(double[][] basis, double t) {
    return add(scale(basis[0], Math.cos(t)), scale(basis[1], Math.sin(t)));
  }

  private static void orientOutward(Quad q, double[] onAxis, double[] axis) {
    double[] mid = scale(add(add(q.pos[0], q.pos[1]), add(q.pos[2], q.pos[3])), 0.25);
    double[] rel = sub(mid, onAxis);
    double[] outward = sub(rel, scale(axis, dot(rel, axis)));
    if (dot(q.faceNormal(), outward) < 0) {
      reverse(q);
    }
  }

  private static void reverse(Quad q) {
    swap(q.pos, 1, 3);
    swap(q.uv, 1, 3);
    swap(q.normal, 1, 3);
  }

  private static void swap(double[][] a, int i, int j) {
    double[] t = a[i];
    a[i] = a[j];
    a[j] = t;
  }

  private static AxisAlignedBB clamp(AxisAlignedBB b) {
    return new AxisAlignedBB(Math.max(0, b.minX) / 16, Math.max(0, b.minY) / 16,
        Math.max(0, b.minZ) / 16, Math.min(16, b.maxX) / 16, Math.min(16, b.maxY) / 16,
        Math.min(16, b.maxZ) / 16);
  }

  static double[] lerp(double[] a, double[] b, double t) {
    return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t,
        a[2] + (b[2] - a[2]) * t};
  }

  static double[] add(double[] a, double[] b) {
    return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
  }

  static double[] sub(double[] a, double[] b) {
    return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
  }

  static double[] scale(double[] a, double s) {
    return new double[]{a[0] * s, a[1] * s, a[2] * s};
  }

  static double[] neg(double[] a) {
    return scale(a, -1);
  }

  static double dot(double[] a, double[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  static double[] cross(double[] a, double[] b) {
    return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]};
  }

  static double len(double[] a) {
    return Math.sqrt(dot(a, a));
  }

  static double[] normalize(double[] a) {
    double l = len(a);
    return l == 0 ? new double[]{0, 0, 0} : scale(a, 1 / l);
  }
}
