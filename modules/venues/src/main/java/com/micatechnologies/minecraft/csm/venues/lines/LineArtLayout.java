package com.micatechnologies.minecraft.csm.venues.lines;

/**
 * One court or track: what a cell shows where nothing is painted, the paint operations in order,
 * the cells it may paint and its default colours. Built from the flat tables
 * {@code venues_lines.py} writes ({@link CourtLayouts}, {@link TrackLayouts}); painted by
 * {@link LinePainter}.
 *
 * <p>Coordinates are metres in the layout's own frame: x east, z south, cell (i, j) the square
 * [i, i + 1] x [j, j + 1].</p>
 *
 * @since 2026.10
 */
public final class LineArtLayout {

  /** Its name. */
  public final String name;
  /** The material of a cell nothing paints. */
  final int base;
  /** The cells it may paint, inclusive: i0, j0, i1, j1. */
  final int[] extent;
  /** Its default colours: cc, sc, lc, oc. */
  final int[] colours;
  /** Its operations, in order. */
  final Op[] ops;

  /**
   * Builds a layout from its table.
   *
   * @param name    its name
   * @param base    the base material's id
   * @param extent  the cells it may paint, inclusive: i0, j0, i1, j1
   * @param colours its default colours: cc, sc, lc, oc
   * @param flat    its operations: for each, the material, its bounding box (x0, z0, x1, z1),
   *                the counts of positive and negative sets, then each set: {@code 0, n, a, b,
   *                c, ...} (n half-planes a x + b z &lt;= c) or {@code 1, cx, cz, r, n} (a disc,
   *                its polygon's corners on a grid n lines a block)
   */
  public LineArtLayout(String name, int base, int[] extent, int[] colours, double[] flat) {
    this.name = name;
    this.base = base;
    this.extent = extent;
    this.colours = colours;
    java.util.List<Op> list = new java.util.ArrayList<>();
    int o = 0;
    while (o < flat.length) {
      Op op = new Op();
      op.mat = (int) flat[o++];
      op.x0 = flat[o++];
      op.z0 = flat[o++];
      op.x1 = flat[o++];
      op.z1 = flat[o++];
      int np = (int) flat[o++];
      int nn = (int) flat[o++];
      op.pos = new Region[np];
      op.neg = new Region[nn];
      for (int k = 0; k < np + nn; k++) {
        Region s;
        if (flat[o] == 0) {
          int n = (int) flat[o + 1];
          double[][] hps = new double[n][];
          for (int h = 0; h < n; h++) {
            hps[h] = new double[]{flat[o + 2 + h * 3], flat[o + 3 + h * 3], flat[o + 4 + h * 3]};
          }
          s = new Convex(hps);
          o += 2 + n * 3;
        } else {
          s = new Disc(flat[o + 1], flat[o + 2], flat[o + 3], (int) flat[o + 4]);
          o += 5;
        }
        if (k < np) {
          op.pos[k] = s;
        } else {
          op.neg[k - np] = s;
        }
      }
      list.add(op);
    }
    this.ops = list.toArray(new Op[0]);
  }

  /**
   * A layout's operations from the text its table is written in: numbers separated by single
   * spaces, cut into literals (a track's twenty thousand numbers would not fit a method as an
   * array initializer).
   *
   * @param parts the literals, in order
   *
   * @return the numbers
   */
  public static double[] table(String... parts) {
    StringBuilder b = new StringBuilder();
    for (String p : parts) {
      if (b.length() > 0) {
        b.append(' ');
      }
      b.append(p);
    }
    String[] tok = b.toString().trim().split(" ");
    double[] out = new double[tok.length];
    for (int k = 0; k < tok.length; k++) {
      out[k] = Double.parseDouble(tok[k]);
    }
    return out;
  }

  /** Whether layout cell (i, j) is one this layout may paint. */
  public boolean covers(int i, int j) {
    return i >= extent[0] && i <= extent[2] && j >= extent[1] && j <= extent[3];
  }

  /** Default colour {@code k} (0 cc, 1 sc, 2 lc, 3 oc). */
  public int defaultColour(int k) {
    return colours[k];
  }

  /** A paint operation. */
  static final class Op {

    int mat;
    double x0;
    double z0;
    double x1;
    double z1;
    Region[] pos;
    Region[] neg;
  }

  /** A set an operation paints inside or leaves out. */
  abstract static class Region {

    /**
     * The set within cell (i, j): {@link LinePainter#ALL}, {@link LinePainter#NONE}, or the
     * half-planes ({a, b, c}: a x + b z &lt;= c) that cut the cell.
     */
    abstract double[][] cell(int i, int j);
  }

  /** A convex polygon: half-planes. */
  static final class Convex extends Region {

    final double[][] hps;

    Convex(double[][] hps) {
      this.hps = hps;
    }

    @Override
    double[][] cell(int i, int j) {
      java.util.List<double[]> out = new java.util.ArrayList<>(hps.length);
      for (double[] h : hps) {
        double v0 = h[0] * i + h[1] * j - h[2];
        double v1 = h[0] * (i + 1) + h[1] * j - h[2];
        double v2 = h[0] * (i + 1) + h[1] * (j + 1) - h[2];
        double v3 = h[0] * i + h[1] * (j + 1) - h[2];
        double lo = Math.min(Math.min(v0, v1), Math.min(v2, v3));
        double hi = Math.max(Math.max(v0, v1), Math.max(v2, v3));
        if (lo >= -LinePainter.EPS) {
          return LinePainter.NONE;
        }
        if (hi <= LinePainter.EPS) {
          continue;
        }
        out.add(h);
      }
      return out.isEmpty() ? LinePainter.ALL : out.toArray(new double[0][]);
    }
  }

  /** A disc, drawn as the polygon through its crossings of a grid {@code n} lines a block. */
  static final class Disc extends Region {

    final double cx;
    final double cz;
    final double r;
    final int n;

    Disc(double cx, double cz, double r, int n) {
      this.cx = cx;
      this.cz = cz;
      this.r = r;
      this.n = Math.max(1, n);
    }

    @Override
    double[][] cell(int i, int j) {
      double nx = Math.min(Math.max(cx, i), i + 1);
      double nz = Math.min(Math.max(cz, j), j + 1);
      if ((nx - cx) * (nx - cx) + (nz - cz) * (nz - cz) >= r * r) {
        return LinePainter.NONE;
      }
      double fx = Math.max(Math.abs(i - cx), Math.abs(i + 1 - cx));
      double fz = Math.max(Math.abs(j - cz), Math.abs(j + 1 - cz));
      if (fx * fx + fz * fz <= r * r) {
        return LinePainter.ALL;
      }
      double eps = LinePainter.EPS;
      java.util.List<double[]> pts = new java.util.ArrayList<>();
      for (int k = 0; k <= n; k++) {
        double x = i + k / (double) n;
        double d = r * r - (x - cx) * (x - cx);
        if (d >= 0) {
          double s = Math.sqrt(d);
          for (double z : new double[]{cz - s, cz + s}) {
            if (z >= j - eps && z <= j + 1 + eps) {
              add(pts, x, Math.min(Math.max(z, j), j + 1));
            }
          }
        }
      }
      for (int k = 0; k <= n; k++) {
        double z = j + k / (double) n;
        double d = r * r - (z - cz) * (z - cz);
        if (d >= 0) {
          double s = Math.sqrt(d);
          for (double x : new double[]{cx - s, cx + s}) {
            if (x >= i - eps && x <= i + 1 + eps) {
              add(pts, Math.min(Math.max(x, i), i + 1), z);
            }
          }
        }
      }
      boolean centreIn = (i + 0.5 - cx) * (i + 0.5 - cx) + (j + 0.5 - cz) * (j + 0.5 - cz) < r * r;
      if (pts.size() < 2) {
        return centreIn ? LinePainter.ALL : LinePainter.NONE;
      }
      pts.sort((a, b) -> Double.compare(Math.atan2(a[1] - cz, a[0] - cx),
          Math.atan2(b[1] - cz, b[0] - cx)));
      java.util.List<double[]> out = new java.util.ArrayList<>();
      int m = pts.size();
      for (int k = 0; k < m; k++) {
        double[] p = pts.get(k);
        double[] q = pts.get((k + 1) % m);
        double ta = Math.atan2(p[1] - cz, p[0] - cx);
        double tb = Math.atan2(q[1] - cz, q[0] - cx);
        if (tb <= ta) {
          tb += 2 * Math.PI;
        }
        double tm = (ta + tb) / 2;
        double mx = cx + r * Math.cos(tm);
        double mz = cz + r * Math.sin(tm);
        if (mx > i && mx < i + 1 && mz > j && mz < j + 1) {
          out.add(LinePainter.through(p, q, cx, cz));
        }
      }
      if (out.isEmpty()) {
        return centreIn ? LinePainter.ALL : LinePainter.NONE;
      }
      return out.toArray(new double[0][]);
    }

    private static void add(java.util.List<double[]> pts, double x, double z) {
      for (double[] q : pts) {
        if (Math.abs(x - q[0]) <= 1e-9 && Math.abs(z - q[1]) <= 1e-9) {
          return;
        }
      }
      pts.add(new double[]{x, z});
    }
  }
}
