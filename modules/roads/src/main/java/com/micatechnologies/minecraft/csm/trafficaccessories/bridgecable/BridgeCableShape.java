package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.SpanWireCatenary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

/**
 * The shapes a bridge cable takes between its two ends, and where a main cable's hangers drop.
 * Pure geometry: no world, no rendering, so the tests can hold it to its numbers.
 */
public final class BridgeCableShape {

  /** Samples a hanging cable takes per block of its plan length, and the bounds on the count. */
  static final double HANGING_SAMPLES_PER_BLOCK = 1.0;
  static final int HANGING_MIN_SEGMENTS = 8;
  static final int HANGING_MAX_SEGMENTS = 256;

  /**
   * Samples a striped straight cable takes per block. A plain straight cable is one segment: it
   * has nothing along its length to show.
   */
  static final double STRIPE_SAMPLES_PER_BLOCK = 2.0;
  static final int STRIPE_MAX_SEGMENTS = 512;

  /** A hanger is not dropped closer than this to the far end of its span. */
  static final double HANGER_END_CLEARANCE = 0.5;

  private BridgeCableShape() {
  }

  /** A cable's centreline, from its owner's end (t = 0) to its target's end (t = 1). */
  public interface Curve {

    Vec3d pointAt(double t);
  }

  /** A taut cable: a stay, or a hanger. */
  public static Curve straight(Vec3d from, Vec3d to) {
    return t -> new Vec3d(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t,
        from.z + (to.z - from.z) * t);
  }

  /**
   * A main cable: the parabola that falls {@code sag} blocks below the straight line between its
   * ends at the middle of the span. Exact for any sag and any difference in height between the
   * ends, which is what a builder setting "sag 6" expects to see.
   */
  public static Curve parabola(Vec3d from, Vec3d to, double sag) {
    if (sag <= 0.0) {
      return straight(from, to);
    }
    final Curve chord = straight(from, to);
    return t -> {
      final Vec3d onChord = chord.pointAt(t);
      return new Vec3d(onChord.x, onChord.y - 4.0 * sag * t * (1.0 - t), onChord.z);
    };
  }

  /**
   * A rope: the catenary that falls {@code sag} blocks below its chord at its lowest, from the span
   * wire's solver. The solver takes slack (cable length over chord) rather than sag, so the slack
   * that gives this sag is found first.
   */
  public static Curve catenary(Vec3d from, Vec3d to, double sag) {
    if (sag <= 0.0 || horizontalLength(from, to) < 1.0e-6) {
      return straight(from, to);
    }
    final SpanWireCatenary cable = SpanWireCatenary.between(from, to, slackForSag(from, to, sag));
    return cable::pointAt;
  }

  /**
   * The slack that makes the span wire's catenary hang {@code sag} blocks below its chord, by
   * bisection. Sag rises with slack without a turning point, so bisection cannot pick a wrong root.
   */
  static double slackForSag(Vec3d from, Vec3d to, double sag) {
    double low = SpanWireCatenary.MIN_SLACK + 1.0e-9;
    double high = 1.01;
    for (int i = 0; i < 40 && SpanWireCatenary.between(from, to, high).sag() < sag; i++) {
      high = 1.0 + (high - 1.0) * 2.0;
    }
    for (int i = 0; i < 50; i++) {
      final double middle = 0.5 * (low + high);
      if (SpanWireCatenary.between(from, to, middle).sag() < sag) {
        low = middle;
      } else {
        high = middle;
      }
    }
    return 0.5 * (low + high);
  }

  /** The curve a cable of this style follows. */
  public static Curve curveFor(BridgeCableStyle style, Vec3d from, Vec3d to, double sag) {
    switch (style) {
      case MAIN:
        return parabola(from, to, sag);
      case ROPE:
        return catenary(from, to, sag);
      default:
        return straight(from, to);
    }
  }

  /** How many segments a cable is drawn in. */
  public static int segmentsFor(BridgeCableStyle style, Vec3d from, Vec3d to, boolean striped) {
    final double length = from.distanceTo(to);
    if (style.hangs()) {
      int segments = clamp((int) Math.ceil(horizontalLength(from, to)
          * HANGING_SAMPLES_PER_BLOCK), HANGING_MIN_SEGMENTS, HANGING_MAX_SEGMENTS);
      if (striped) {
        segments = Math.min(STRIPE_MAX_SEGMENTS, segments * 2);
      }
      return segments;
    }
    if (striped) {
      return clamp((int) Math.ceil(length * STRIPE_SAMPLES_PER_BLOCK), 2, STRIPE_MAX_SEGMENTS);
    }
    return 1;
  }

  /** Samples a curve at {@code segments + 1} evenly spaced points, both ends included. */
  public static List<Vec3d> sample(Curve curve, int segments) {
    final int count = Math.max(1, segments);
    final List<Vec3d> points = new ArrayList<>(count + 1);
    for (int i = 0; i <= count; i++) {
      points.add(curve.pointAt(i / (double) count));
    }
    return points;
  }

  /**
   * Where along a span its hangers drop, as fractions from the owner's end: every {@code
   * spacing} blocks of plan length from that end, and none within half a block of the far end.
   * Measured from the anchor's own point, which is a block centre for every anchor, so on a
   * cable running along x or z every hanger lands on a block centre too.
   */
  public static List<Double> hangerStations(Vec3d from, Vec3d to, double spacing) {
    final List<Double> stations = new ArrayList<>();
    final double length = horizontalLength(from, to);
    if (spacing <= 0.0 || length < 1.0e-6) {
      return stations;
    }
    for (int k = 1; k * spacing <= length - HANGER_END_CLEARANCE
        && stations.size() < BridgeCableLink.MAX_HANGERS; k++) {
      stations.add(k * spacing / length);
    }
    return stations;
  }

  /** Plan-view length between two points. */
  public static double horizontalLength(Vec3d from, Vec3d to) {
    final double dx = to.x - from.x;
    final double dz = to.z - from.z;
    return Math.sqrt(dx * dx + dz * dz);
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
