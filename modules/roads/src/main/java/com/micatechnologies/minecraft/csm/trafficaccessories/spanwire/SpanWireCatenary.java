package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.util.math.Vec3d;

/**
 * The shape of a span wire messenger cable: the curve a uniform cable hangs in between two
 * supports, solved once and then sampled.
 *
 * <p>A hanging cable is a catenary, {@code y = a*cosh((x - x0)/a) + y0}, not a parabola. The
 * difference is small at the sags used here, but the catenary is the one that falls out of the
 * cable's own length, which is the parameter a builder actually controls: a cable cut 0.66%
 * longer than the gap it crosses sags 5% of the span, and does so at <em>every</em> span length.
 * A parabola would need its curvature re-tuned per span to keep that looking right.
 *
 * <p>The whole span is one curve (see the plan's D2). Real span wire does not kink at each
 * signal — what looks like a kink in photographs is the lashed electrical conductors coiled up
 * at the clamp, and the anti-chafe loop routed over the connector. Those are hardware drawn at
 * the hanger, not a change in the messenger's shape.
 *
 * <p>Instances are immutable and hold no world reference, so one can be solved on either side
 * and handed around freely.
 *
 * <p>Prior art: Immersive Engineering solves the same equation in
 * {@code ApiUtils.getConnectionCatenary}. The equation is the standard one; the solver here is
 * independent (Newton's method rather than IE's fixed 0.01 stepping, which needs up to 300
 * iterations to do what this does in about five).
 */
public final class SpanWireCatenary {

  /**
   * Cable length as a multiple of the straight-line distance between the two ends. Anything at
   * or below this is treated as a taut cable and drawn straight -- below 1.0 the cable would be
   * shorter than the gap, which has no solution.
   */
  public static final double MIN_SLACK = 1.0 + 1.0e-6;

  /**
   * Default slack, chosen to give a 5% sag -- the real-world figure for signal span wire at
   * rest. Verified numerically: this produces 5.00% of the span at 4, 8, 16, 24, 40 and 64
   * blocks, so it does not need re-tuning per span length.
   *
   * <p>Worth knowing what that means in blocks before being surprised by it in game: a 20-block
   * span hangs a full block low at midspan, and a 40-block span two blocks. Wide real
   * intersections really do look like that.
   */
  public static final double DEFAULT_SLACK = 1.0066;

  /** Newton iteration caps. Convergence is typically five iterations; these are backstops. */
  private static final int SOLVE_MAX_ITERATIONS = 60;
  private static final double SOLVE_TOLERANCE = 1.0e-14;

  private final Vec3d start;
  /** Horizontal displacement from start to end. The cable's shape is a function of these. */
  private final double dx;
  private final double dz;
  /** Horizontal (plan-view) distance between the ends. Zero for a purely vertical span. */
  private final double horizontalLength;
  private final double dy;

  /** Catenary parameter; larger means flatter. Unused when {@link #taut} is set. */
  private final double a;
  private final double vertexX;
  private final double vertexY;
  private final boolean taut;

  private SpanWireCatenary(Vec3d start, double dx, double dy, double dz, double horizontalLength,
      double a, double vertexX, double vertexY, boolean taut) {
    this.start = start;
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.horizontalLength = horizontalLength;
    this.a = a;
    this.vertexX = vertexX;
    this.vertexY = vertexY;
    this.taut = taut;
  }

  /**
   * Solves the cable hanging between two points.
   *
   * @param start the cable's attachment point at one end, in world coordinates
   * @param end   the cable's attachment point at the other end, in world coordinates
   * @param slack cable length as a multiple of the straight-line distance between the ends;
   *              values at or below {@link #MIN_SLACK} give a straight cable
   *
   * @return the solved curve, never null; a degenerate span yields a straight one
   */
  public static SpanWireCatenary between(Vec3d start, Vec3d end, double slack) {
    final double dx = end.x - start.x;
    final double dy = end.y - start.y;
    final double dz = end.z - start.z;
    final double horizontalLength = Math.sqrt(dx * dx + dz * dz);

    // A purely vertical span has no horizontal extent to hang across, so there is no catenary
    // to solve -- x would not be a usable parameter. Drawn straight. This is the same case IE
    // special-cases before its solver, for the same reason.
    if (horizontalLength < 1.0e-6) {
      return straight(start, dx, dy, dz, horizontalLength);
    }

    final double chord = Math.sqrt(dx * dx + dy * dy + dz * dz);
    final double cableLength = chord * Math.max(slack, 1.0);
    if (slack <= MIN_SLACK || cableLength <= chord) {
      return straight(start, dx, dy, dz, horizontalLength);
    }

    // The cable's length, its horizontal span and its rise are related by
    //   sinh(l)/l = sqrt(length^2 - dy^2) / horizontalLength,  where l = horizontalLength/(2a).
    // sinh(l)/l is 1 at l=0 and increases without bound, so a solution exists exactly when the
    // right-hand side exceeds 1 -- that is, when the cable is longer than the gap it crosses.
    final double target =
        Math.sqrt(cableLength * cableLength - dy * dy) / horizontalLength;
    if (!(target > 1.0) || Double.isNaN(target)) {
      return straight(start, dx, dy, dz, horizontalLength);
    }

    double l = 1.0;
    for (int i = 0; i < SOLVE_MAX_ITERATIONS; i++) {
      final double sinh = Math.sinh(l);
      final double cosh = Math.cosh(l);
      final double f = sinh / l - target;
      final double fPrime = (l * cosh - sinh) / (l * l);
      if (fPrime == 0.0 || Double.isNaN(fPrime)) {
        break;
      }
      final double step = f / fPrime;
      l -= step;
      // Newton can overshoot past zero when the cable is very nearly taut; the root is strictly
      // positive, so fold back rather than diverging into the negative branch.
      if (l <= 1.0e-12) {
        l = 1.0e-12;
      }
      if (Math.abs(step) < SOLVE_TOLERANCE) {
        break;
      }
    }

    final double a = horizontalLength / (2.0 * l);
    final double vertexX =
        horizontalLength / 2.0 - (a / 2.0) * Math.log((cableLength + dy) / (cableLength - dy));
    // Pinning y0 this way makes the curve pass through the start point exactly rather than to
    // within solver tolerance. The far end then lands on the end point to the same accuracy the
    // solve achieved, which is the honest place for the error to show up.
    final double vertexY = -a * Math.cosh(vertexX / a);

    if (Double.isNaN(a) || Double.isNaN(vertexX) || Double.isNaN(vertexY)) {
      return straight(start, dx, dy, dz, horizontalLength);
    }
    return new SpanWireCatenary(start, dx, dy, dz, horizontalLength, a, vertexX, vertexY, false);
  }

  private static SpanWireCatenary straight(Vec3d start, double dx, double dy, double dz,
      double horizontalLength) {
    return new SpanWireCatenary(start, dx, dy, dz, horizontalLength, 0.0, 0.0, 0.0, true);
  }

  /** Whether the cable is drawn straight -- too little slack to hang, or a vertical span. */
  public boolean isTaut() {
    return taut;
  }

  /**
   * The cable's height, in world coordinates, at a fraction along the span.
   *
   * @param t fraction from the start end to the far end, clamped to {@code [0, 1]}
   */
  public double heightAt(double t) {
    final double clamped = clamp01(t);
    if (taut) {
      return start.y + dy * clamped;
    }
    final double x = horizontalLength * clamped;
    return start.y + a * Math.cosh((x - vertexX) / a) + vertexY;
  }

  /**
   * The cable's position, in world coordinates, at a fraction along the span.
   *
   * @param t fraction from the start end to the far end, clamped to {@code [0, 1]}
   */
  public Vec3d pointAt(double t) {
    final double clamped = clamp01(t);
    return new Vec3d(start.x + dx * clamped, heightAt(clamped), start.z + dz * clamped);
  }

  /**
   * The cable's slope at a fraction along the span, as rise over horizontal run. Hanger hardware
   * needs this to sit square on a cable that is not level where it clamps.
   *
   * @param t fraction from the start end to the far end, clamped to {@code [0, 1]}
   */
  public double slopeAt(double t) {
    final double clamped = clamp01(t);
    if (taut) {
      return horizontalLength < 1.0e-6 ? 0.0 : dy / horizontalLength;
    }
    return Math.sinh((horizontalLength * clamped - vertexX) / a);
  }

  /**
   * Where along the span a world position sits, as a fraction from the start end, by projecting
   * it onto the horizontal line between the two ends. Height is ignored: a hanger is identified
   * by where it stands in plan view, not by how high it was placed.
   *
   * @return the fraction, clamped to {@code [0, 1]}
   */
  public double parameterAt(double worldX, double worldZ) {
    if (horizontalLength < 1.0e-6) {
      return 0.0;
    }
    final double projected =
        ((worldX - start.x) * dx + (worldZ - start.z) * dz) / (horizontalLength * horizontalLength);
    return clamp01(projected);
  }

  /**
   * How far the cable's lowest point falls below the straight line joining its ends, in blocks.
   * This is the number that decides whether a builder has to step their hanger mounts down.
   */
  public double sag() {
    if (taut) {
      return 0.0;
    }
    double worst = 0.0;
    // The vertex is the low point when it falls inside the span; when it does not, the sag is
    // largest at whichever end the curve leans toward. Sampling covers both without branching
    // on a vertex position that may lie outside [0, horizontalLength].
    for (int i = 0; i <= 100; i++) {
      final double t = i / 100.0;
      final double chordHeight = start.y + dy * t;
      worst = Math.max(worst, chordHeight - heightAt(t));
    }
    return worst;
  }

  /** Horizontal (plan-view) distance between the cable's two ends, in blocks. */
  public double getHorizontalLength() {
    return horizontalLength;
  }

  private static double clamp01(double value) {
    if (value < 0.0) {
      return 0.0;
    }
    return Math.min(value, 1.0);
  }
}
