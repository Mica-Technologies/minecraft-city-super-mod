package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;

/**
 * Sweeps the messenger cable's tube geometry along a solved catenary.
 *
 * <p>The cable is drawn as a closed prism swept along the curve: a ring of points is built at
 * each sample and consecutive rings are stitched with quads. Six sides is enough — at a radius
 * this small the silhouette is a couple of pixels across at any distance a player looks at it
 * from, and the facet shading below does more for the impression of roundness than more sides
 * would.
 *
 * <p>Vertices come out in the {@code BLOCK} format with the lightmap baked per vertex, matching
 * every other CSM tile entity renderer, and textured against the shared white pixel so the
 * colour is carried entirely by vertex colour. That also satisfies the one-texture-per-display
 * list rule described in {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}.
 *
 * <p>Coordinates are emitted relative to a caller-supplied origin, so the tile entity that owns
 * a segment can translate to its own block and hand in its own position. A display list does not
 * capture the model-view matrix, but it does capture the vertices, so the origin has to be baked
 * in rather than applied afterwards.
 */
public final class SpanWireCableGeometry {

  /**
   * Cable radius in blocks. A real messenger cable is under half an inch, which at this scale
   * would be a fraction of a pixel and simply invisible, so this is exaggerated to the thinnest
   * thing that still reads as a cable across an intersection -- one texture pixel of a block.
   */
  public static final double CABLE_RADIUS = 1.0 / 32.0;

  /** Sides on the swept prism. See the class note on why this is not higher. */
  private static final int SIDES = 6;

  /** Samples per block of segment length, and the bounds that keeps a segment sane. */
  private static final double SAMPLES_PER_BLOCK = 2.0;
  private static final int MIN_SAMPLES = 2;
  private static final int MAX_SAMPLES = 32;

  /** UV of the shared white pixel; the whole tube samples this one texel. */
  private static final float WHITE_UV = 0.5f;

  /**
   * Direction the facet shading is lit from. Not a real light -- just a fixed bias so the top of
   * the cable reads brighter than its underside and the tube does not look like a flat ribbon.
   */
  private static final double LIGHT_X = 0.30;
  private static final double LIGHT_Y = 0.90;
  private static final double LIGHT_Z = 0.32;

  /** How far the facet shading swings either side of the base colour. */
  private static final float SHADE_RANGE = 0.35f;

  private SpanWireCableGeometry() {
  }

  /**
   * Emits the tube for the piece of a cable between two positions along it.
   *
   * @param buffer   the buffer, already begun in {@code GL_QUADS} with the {@code BLOCK} format
   * @param cable    the solved cable for the whole span
   * @param fromT    where the piece starts, as a fraction along the span
   * @param toT      where the piece ends
   * @param origin   world position that emitted coordinates are relative to
   * @param radius   cable radius in blocks
   * @param red      base colour
   * @param green    base colour
   * @param blue     base colour
   * @param skyLight lightmap sky component
   * @param blockLight lightmap block component
   */
  public static void emitSegment(BufferBuilder buffer, SpanWireCatenary cable, double fromT,
      double toT, Vec3d origin, double radius, float red, float green, float blue,
      int skyLight, int blockLight) {

    final double spanLength = cable.getHorizontalLength() * Math.abs(toT - fromT);
    final int samples = clamp((int) Math.round(spanLength * SAMPLES_PER_BLOCK),
        MIN_SAMPLES, MAX_SAMPLES);

    // Ring points for the current and previous sample, as flat xyz triples.
    double[] previous = null;
    double[] current = new double[SIDES * 3];

    for (int i = 0; i <= samples; i++) {
      final double t = fromT + (toT - fromT) * (i / (double) samples);
      buildRing(cable, t, origin, radius, current);

      if (previous != null) {
        stitch(buffer, previous, current, red, green, blue, skyLight, blockLight);
      }

      if (previous == null) {
        previous = new double[SIDES * 3];
      }
      System.arraycopy(current, 0, previous, 0, current.length);
    }
  }

  /**
   * Emits a straight tube between two world points -- the hanger hardware dropping from the cable
   * to the mount it carries.
   *
   * <p>Built from the same prism and shading as the cable so the two read as one assembly rather
   * than as a cable with a different-looking stick attached to it.
   */
  public static void emitStraightTube(BufferBuilder buffer, Vec3d from, Vec3d to, Vec3d origin,
      double radius, float red, float green, float blue, int skyLight, int blockLight) {
    final Vec3d direction = to.subtract(from);
    if (lengthSquared(direction) < 1.0e-12) {
      return;
    }
    final double[] startRing = new double[SIDES * 3];
    final double[] endRing = new double[SIDES * 3];
    buildRingAt(from, direction, origin, radius, startRing);
    buildRingAt(to, direction, origin, radius, endRing);
    stitch(buffer, startRing, endRing, red, green, blue, skyLight, blockLight);
  }

  /**
   * Samples the cable's centreline over part of a span, optionally displaced.
   *
   * <p>Used for anything that runs <em>alongside</em> the messenger rather than being it -- the
   * lashed conductors, and the lower tether of a box span. Sampling at the same density as the
   * cable keeps the two curves parallel; deriving the conductor run from its own solve would let
   * the two drift apart by a pixel or two mid-span, which reads as a rendering fault.
   *
   * @param verticalOffset how far below the cable to run, in blocks
   */
  public static List<Vec3d> sampleSegment(SpanWireCatenary cable, double fromT, double toT,
      double verticalOffset) {
    final double spanLength = cable.getHorizontalLength() * Math.abs(toT - fromT);
    final int samples = clamp((int) Math.round(spanLength * SAMPLES_PER_BLOCK),
        MIN_SAMPLES, MAX_SAMPLES);

    final List<Vec3d> points = new ArrayList<>(samples + 1);
    for (int i = 0; i <= samples; i++) {
      final double t = fromT + (toT - fromT) * (i / (double) samples);
      final Vec3d point = cable.pointAt(t);
      points.add(new Vec3d(point.x, point.y - verticalOffset, point.z));
    }
    return points;
  }

  /**
   * Emits a tube following an arbitrary path of points -- the shared builder behind every piece
   * of round hardware on a span that is not the cable itself: the slack conductor loops at a
   * clamp, and anything else that wants a bent tube.
   *
   * <p>Each ring is squared to the path's local direction, taken as a central difference so a
   * curve does not visibly facet at its sample points. A closed path additionally joins its last
   * ring back to its first, which is what makes a loop a loop rather than a horseshoe.
   *
   * @param points at least two points along the centreline, in world coordinates
   * @param closed whether the path returns to its start
   */
  public static void emitTubePath(BufferBuilder buffer, List<Vec3d> points, boolean closed,
      Vec3d origin, double radius, float red, float green, float blue, int skyLight,
      int blockLight) {
    final int count = points.size();
    if (count < 2) {
      return;
    }

    final double[][] rings = new double[count][SIDES * 3];
    for (int i = 0; i < count; i++) {
      buildRingAt(points.get(i), pathDirection(points, i, closed), origin, radius, rings[i]);
    }

    for (int i = 0; i < count - 1; i++) {
      stitch(buffer, rings[i], rings[i + 1], red, green, blue, skyLight, blockLight);
    }
    if (closed) {
      stitch(buffer, rings[count - 1], rings[0], red, green, blue, skyLight, blockLight);
    }
  }

  /** The centreline direction at one point of a path, by central difference where possible. */
  private static Vec3d pathDirection(List<Vec3d> points, int index, boolean closed) {
    final int count = points.size();
    final Vec3d previous = index > 0 ? points.get(index - 1)
        : (closed ? points.get(count - 1) : null);
    final Vec3d next = index < count - 1 ? points.get(index + 1)
        : (closed ? points.get(0) : null);

    if (previous != null && next != null) {
      return next.subtract(previous);
    }
    if (next != null) {
      return next.subtract(points.get(index));
    }
    return points.get(index).subtract(previous);
  }

  /**
   * Builds the ring of points around the cable at one position along it.
   *
   * <p>The frame is built from the cable's own direction rather than carried along the curve.
   * That would twist on a curve that turned in plan view, but a span is a straight run seen from
   * above -- all of its curvature is vertical -- so the simple construction is exact here and
   * cheaper than parallel transport.
   */
  private static void buildRing(SpanWireCatenary cable, double t, Vec3d origin, double radius,
      double[] out) {
    // Tangent, by sampling a little either side. Cheaper and steadier near the ends than
    // differentiating the closed form and having to special-case the taut cable.
    final double step = 1.0e-3;
    final Vec3d ahead = cable.pointAt(Math.min(1.0, t + step));
    final Vec3d behind = cable.pointAt(Math.max(0.0, t - step));
    buildRingAt(cable.pointAt(t), ahead.subtract(behind), origin, radius, out);
  }

  /**
   * Builds a ring of points around a position, square to a direction. Shared by the cable itself
   * and by the straight hardware hung off it, so both are the same prism built the same way.
   */
  private static void buildRingAt(Vec3d point, Vec3d direction, Vec3d origin, double radius,
      double[] out) {
    Vec3d tangent = direction;
    if (lengthSquared(tangent) < 1.0e-18) {
      tangent = new Vec3d(1.0, 0.0, 0.0);
    }
    tangent = tangent.normalize();

    // Two vectors across the tube. World up is parallel to the tangent for a vertical drop, which
    // is why the fallback below is a real case here and not just defensive.
    Vec3d across = tangent.crossProduct(new Vec3d(0.0, 1.0, 0.0));
    if (lengthSquared(across) < 1.0e-12) {
      across = tangent.crossProduct(new Vec3d(1.0, 0.0, 0.0));
    }
    across = across.normalize();
    final Vec3d upward = across.crossProduct(tangent).normalize();

    final double ox = point.x - origin.x;
    final double oy = point.y - origin.y;
    final double oz = point.z - origin.z;

    for (int k = 0; k < SIDES; k++) {
      final double angle = 2.0 * Math.PI * k / SIDES;
      final double ca = Math.cos(angle) * radius;
      final double sa = Math.sin(angle) * radius;
      out[k * 3] = ox + across.x * ca + upward.x * sa;
      out[k * 3 + 1] = oy + across.y * ca + upward.y * sa;
      out[k * 3 + 2] = oz + across.z * ca + upward.z * sa;
    }
  }

  /** Stitches two rings together with one quad per side. */
  private static void stitch(BufferBuilder buffer, double[] a, double[] b,
      float red, float green, float blue, int skyLight, int blockLight) {
    for (int k = 0; k < SIDES; k++) {
      final int k0 = k * 3;
      final int k1 = ((k + 1) % SIDES) * 3;

      final float shade = facetShade(a, b, k0, k1);
      final float r = red * shade;
      final float g = green * shade;
      final float bl = blue * shade;

      vertex(buffer, a[k0], a[k0 + 1], a[k0 + 2], r, g, bl, skyLight, blockLight);
      vertex(buffer, a[k1], a[k1 + 1], a[k1 + 2], r, g, bl, skyLight, blockLight);
      vertex(buffer, b[k1], b[k1 + 1], b[k1 + 2], r, g, bl, skyLight, blockLight);
      vertex(buffer, b[k0], b[k0 + 1], b[k0 + 2], r, g, bl, skyLight, blockLight);
    }
  }

  /**
   * Brightness for one facet, from how squarely it faces the fixed light direction. This is what
   * makes the prism read as a round cable rather than a flat strip.
   */
  private static float facetShade(double[] a, double[] b, int k0, int k1) {
    // Two edges of the quad, crossed for its normal.
    final double e1x = a[k1] - a[k0];
    final double e1y = a[k1 + 1] - a[k0 + 1];
    final double e1z = a[k1 + 2] - a[k0 + 2];
    final double e2x = b[k0] - a[k0];
    final double e2y = b[k0 + 1] - a[k0 + 1];
    final double e2z = b[k0 + 2] - a[k0 + 2];

    double nx = e1y * e2z - e1z * e2y;
    double ny = e1z * e2x - e1x * e2z;
    double nz = e1x * e2y - e1y * e2x;

    final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
    if (length < 1.0e-12) {
      return 1.0f;
    }
    nx /= length;
    ny /= length;
    nz /= length;

    // Culling is off for this pass, so a facet's normal may point either way; the magnitude of
    // the dot product is what matters, not its sign.
    final double alignment = Math.abs(nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z);
    return 1.0f - SHADE_RANGE + (float) (alignment * 2.0 * SHADE_RANGE);
  }

  private static void vertex(BufferBuilder buffer, double x, double y, double z,
      float red, float green, float blue, int skyLight, int blockLight) {
    buffer.pos(x, y, z)
        .color(red, green, blue, 1.0f)
        .tex(WHITE_UV, WHITE_UV)
        .lightmap(skyLight, blockLight)
        .endVertex();
  }

  /**
   * Squared length of a vector, from its components. Written out rather than calling Vec3d's own
   * accessor because that method's name differs between mappings, and comparing squares avoids a
   * square root that is only ever used against a threshold.
   */
  private static double lengthSquared(Vec3d vector) {
    return vector.x * vector.x + vector.y * vector.y + vector.z * vector.z;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
