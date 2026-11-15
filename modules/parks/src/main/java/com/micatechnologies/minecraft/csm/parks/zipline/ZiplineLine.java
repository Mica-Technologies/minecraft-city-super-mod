package com.micatechnologies.minecraft.csm.parks.zipline;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The shape of a zipline: the cable from its launch anchor's sheave to its landing anchor's,
 * hanging in a shallow curve, and where along it a rider starts, stops and brakes.
 *
 * <p>A zipline cable is strung far tighter than a power line, so its sag is small and a parabola
 * is as good as the catenary it approximates: {@link #SAG_FRACTION} of the plan-view span at
 * midspan. Distances along the line are arc lengths, from a table of {@link #SAMPLES} chords, so
 * a rider's speed in blocks a second is a speed along the cable.</p>
 *
 * <p>Immutable, and built from the two anchors' positions alone, so the server and every client
 * work out the same line without sending it.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public final class ZiplineLine {

  /** Midspan sag as a fraction of the plan-view span: 2%, a tight recreational line. */
  static final double SAG_FRACTION = 0.02;
  /** Chords in the arc-length table. */
  static final int SAMPLES = 96;
  /** Where the cable leaves an anchor block, above its floor: the sheave's groove. */
  static final double CABLE_HEIGHT = 0.875;
  /** How far below the cable a rider's feet hang: hands on the bar, arms up, the body below. */
  public static final double RIDER_DROP = 2.45;
  /** Along the line from the launch sheave, where a rider clips on. */
  static final double START_DISTANCE = 1.0;
  /** Back along the line from the landing sheave, where a rider is set down. */
  static final double STOP_BACK = 1.5;
  /** How far before the stop point the brakes act, at most. */
  static final double BRAKE_LENGTH = 8.0;
  /** The shortest line that leaves room to clip on, ride and stop. */
  public static final double MIN_LENGTH = START_DISTANCE + STOP_BACK + 1.0;

  private final BlockPos launch;
  private final BlockPos landing;
  private final Vec3d start;
  private final Vec3d end;
  private final double sag;
  /** Arc length at each sample, {@code SAMPLES + 1} of them. */
  private final double[] arc = new double[SAMPLES + 1];

  private ZiplineLine(BlockPos launch, BlockPos landing) {
    this.launch = launch.toImmutable();
    this.landing = landing.toImmutable();
    this.start = cablePoint(launch);
    this.end = cablePoint(landing);
    double dx = end.x - start.x;
    double dz = end.z - start.z;
    this.sag = SAG_FRACTION * Math.sqrt(dx * dx + dz * dz);
    Vec3d prev = pointAtT(0);
    for (int i = 1; i <= SAMPLES; i++) {
      Vec3d p = pointAtT((double) i / SAMPLES);
      arc[i] = arc[i - 1] + p.distanceTo(prev);
      prev = p;
    }
  }

  /**
   * The line between two anchors.
   *
   * @param launch  the launch anchor's position
   * @param landing the landing anchor's position
   *
   * @return the line
   *
   * @since 1.0
   */
  public static ZiplineLine of(BlockPos launch, BlockPos landing) {
    return new ZiplineLine(launch, landing);
  }

  /** Where the cable meets an anchor block. */
  public static Vec3d cablePoint(BlockPos anchor) {
    return new Vec3d(anchor.getX() + 0.5, anchor.getY() + CABLE_HEIGHT, anchor.getZ() + 0.5);
  }

  public BlockPos launch() {
    return launch;
  }

  public BlockPos landing() {
    return landing;
  }

  /** The cable's length, sheave to sheave. */
  public double length() {
    return arc[SAMPLES];
  }

  /** The straight-line distance between the sheaves. */
  public double span() {
    return start.distanceTo(end);
  }

  /** How far the landing sheave is below the launch sheave; negative for a rising line. */
  public double drop() {
    return start.y - end.y;
  }

  /** Where a rider clips on. */
  public double startDistance() {
    return Math.min(START_DISTANCE, length() / 2);
  }

  /** Where a rider is set down. */
  public double stopDistance() {
    return Math.max(startDistance(), length() - STOP_BACK);
  }

  /** Where the brakes begin: the last stretch before the stop point, a quarter of the ride at most. */
  public double brakeDistance() {
    double ride = stopDistance() - startDistance();
    return stopDistance() - Math.min(BRAKE_LENGTH, ride / 4);
  }

  /** The point on the cable at a parameter from 0 (launch) to 1 (landing). */
  Vec3d pointAtT(double t) {
    double y = start.y + (end.y - start.y) * t - 4 * sag * t * (1 - t);
    return new Vec3d(start.x + (end.x - start.x) * t, y, start.z + (end.z - start.z) * t);
  }

  /** The parameter at a distance along the cable, clamped to the line. */
  double tAt(double s) {
    if (s <= 0) {
      return 0;
    }
    if (s >= arc[SAMPLES]) {
      return 1;
    }
    int lo = 0;
    int hi = SAMPLES;
    while (hi - lo > 1) {
      int mid = (lo + hi) >>> 1;
      if (arc[mid] <= s) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    double seg = arc[hi] - arc[lo];
    double f = seg <= 0 ? 0 : (s - arc[lo]) / seg;
    return (lo + f) / SAMPLES;
  }

  /**
   * The point on the cable a distance along it from the launch sheave.
   *
   * @param s the distance, clamped to the line
   *
   * @return the point
   *
   * @since 1.0
   */
  public Vec3d point(double s) {
    return pointAtT(tAt(s));
  }

  /**
   * The cable's rise per block along it at a distance: negative going downhill, the sine of its
   * angle to the horizontal.
   *
   * @param s the distance
   *
   * @return dy/ds
   *
   * @since 1.0
   */
  public double slope(double s) {
    double h = 0.25;
    double a = Math.max(0, s - h);
    double b = Math.min(length(), s + h);
    if (b - a <= 1.0e-9) {
      return 0;
    }
    return (point(b).y - point(a).y) / (b - a);
  }

  /** The way along the line in plan view, as a yaw in degrees (Minecraft's: 0 south, 90 west). */
  public float yaw() {
    double dx = end.x - start.x;
    double dz = end.z - start.z;
    return (float) (Math.toDegrees(Math.atan2(-dx, dz)));
  }

  /** Whether two anchors make the same line. */
  public boolean sameAs(BlockPos launchPos, BlockPos landingPos) {
    return launch.equals(launchPos) && landing.equals(landingPos);
  }
}
