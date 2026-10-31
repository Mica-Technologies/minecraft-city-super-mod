package com.micatechnologies.minecraft.csm.venues.lines;

import java.util.ArrayList;
import java.util.List;

/**
 * Paints one cell of a court or track: the partition of its square into convex polygons, each
 * one material, that {@link LineArtLayout}'s operations make. The same algorithm, step for step,
 * as {@code venues_lines.py}'s {@code paint}, which is how the generator knows which cells to lay
 * as painted cells and which as plain blocks.
 *
 * <p>Each operation paints its material over (the intersection of its positive sets) less (the
 * union of its negative sets). Within one cell every set is, at most, a few half-planes (a disc
 * by the chords between its crossings of the cell's grid lines), so painting is cutting convex
 * polygons by half-planes. A disc's chords meet a neighbouring cell's at the same point on the
 * shared edge, which is what keeps a lane line continuous from cell to cell round a bend.</p>
 *
 * <p>Pure arithmetic, both sides: no Minecraft class.</p>
 *
 * @since 2026.10
 */
public final class LinePainter {

  static final double EPS = 1e-9;
  private static final double MIN_AREA = 1e-7;

  /** A set that holds the whole cell. */
  static final double[][] ALL = new double[0][];
  /** A set that holds none of the cell. */
  static final double[][] NONE = new double[0][];

  private LinePainter() {
  }

  /** A painted piece: a convex polygon (x, z pairs, layout metres) and its material. */
  public static final class Piece {

    /** Corners: x0, z0, x1, z1, ... */
    public final double[] pts;
    /** Its material's id ({@link LineArtMaterials}). */
    public final int mat;

    Piece(double[] pts, int mat) {
      this.pts = pts;
      this.mat = mat;
    }
  }

  /**
   * The pieces of layout cell (i, j).
   *
   * @param layout the layout
   * @param i      the cell's column
   * @param j      the cell's row
   *
   * @return polygons partitioning the square [i, i + 1] x [j, j + 1]
   */
  public static List<Piece> paint(LineArtLayout layout, int i, int j) {
    List<Piece> pieces = new ArrayList<>();
    pieces.add(new Piece(new double[]{i, j, i + 1, j, i + 1, j + 1, i, j + 1}, layout.base));
    for (LineArtLayout.Op op : layout.ops) {
      if (op.x1 <= i || op.x0 >= i + 1 || op.z1 <= j || op.z0 >= j + 1) {
        continue;
      }
      List<double[][]> cutsPos = new ArrayList<>();
      boolean dead = false;
      for (LineArtLayout.Region s : op.pos) {
        double[][] c = s.cell(i, j);
        if (c == NONE) {
          dead = true;
          break;
        }
        if (c != ALL) {
          cutsPos.add(c);
        }
      }
      if (dead) {
        continue;
      }
      List<double[][]> cutsNeg = new ArrayList<>();
      for (LineArtLayout.Region s : op.neg) {
        double[][] c = s.cell(i, j);
        if (c == ALL) {
          dead = true;
          break;
        }
        if (c != NONE) {
          cutsNeg.add(c);
        }
      }
      if (dead) {
        continue;
      }
      List<Piece> next = new ArrayList<>();
      for (Piece piece : pieces) {
        List<double[]> inside = new ArrayList<>();
        inside.add(piece.pts);
        List<double[]> kept = new ArrayList<>();
        for (double[][] hs : cutsPos) {
          for (double[] h : hs) {
            List<double[]> keepIn = new ArrayList<>();
            for (double[] p : inside) {
              double[][] ab = split(p, h);
              if (ab[1] != null) {
                kept.add(ab[1]);
              }
              if (ab[0] != null) {
                keepIn.add(ab[0]);
              }
            }
            inside = keepIn;
          }
        }
        for (double[][] hs : cutsNeg) {
          List<double[]> out = new ArrayList<>();
          for (double[] p : inside) {
            double[] rem = p;
            for (double[] h : hs) {
              double[][] ab = split(rem, h);
              if (ab[1] != null) {
                out.add(ab[1]);
              }
              rem = ab[0];
              if (rem == null) {
                break;
              }
            }
            if (rem != null) {
              kept.add(rem);
            }
          }
          inside = out;
        }
        for (double[] p : kept) {
          next.add(new Piece(p, piece.mat));
        }
        for (double[] p : inside) {
          next.add(new Piece(p, op.mat));
        }
      }
      pieces = next;
    }
    return pieces;
  }

  /** The half-plane bounded by the line through p and q that holds (ix, iz). */
  static double[] through(double[] p, double[] q, double ix, double iz) {
    double a = q[1] - p[1];
    double b = p[0] - q[0];
    double c = a * p[0] + b * p[1];
    if (a * ix + b * iz > c) {
      a = -a;
      b = -b;
      c = -c;
    }
    return new double[]{a, b, c};
  }

  /** Twice a polygon's signed area. */
  static double area2(double[] p) {
    double s = 0;
    int n = p.length / 2;
    for (int k = 0; k < n; k++) {
      int m = (k + 1) % n;
      s += p[2 * k] * p[2 * m + 1] - p[2 * m] * p[2 * k + 1];
    }
    return s;
  }

  /** {inside, outside} of a convex polygon cut by a half-plane; either may be null. */
  static double[][] split(double[] poly, double[] h) {
    int n = poly.length / 2;
    double[] vals = new double[n];
    double lo = Double.MAX_VALUE;
    double hi = -Double.MAX_VALUE;
    for (int k = 0; k < n; k++) {
      vals[k] = h[0] * poly[2 * k] + h[1] * poly[2 * k + 1] - h[2];
      lo = Math.min(lo, vals[k]);
      hi = Math.max(hi, vals[k]);
    }
    if (hi <= EPS) {
      return new double[][]{poly, null};
    }
    if (lo >= -EPS) {
      return new double[][]{null, poly};
    }
    double[] ins = new double[(n + 2) * 2];
    double[] outs = new double[(n + 2) * 2];
    int ni = 0;
    int no = 0;
    for (int k = 0; k < n; k++) {
      int m = (k + 1) % n;
      double vp = vals[k];
      double vq = vals[m];
      double px = poly[2 * k];
      double pz = poly[2 * k + 1];
      if (vp <= EPS) {
        ins[ni++] = px;
        ins[ni++] = pz;
      }
      if (vp >= -EPS) {
        outs[no++] = px;
        outs[no++] = pz;
      }
      if ((vp < -EPS && vq > EPS) || (vp > EPS && vq < -EPS)) {
        double t = vp / (vp - vq);
        double mx = px + (poly[2 * m] - px) * t;
        double mz = pz + (poly[2 * m + 1] - pz) * t;
        ins[ni++] = mx;
        ins[ni++] = mz;
        outs[no++] = mx;
        outs[no++] = mz;
      }
    }
    double[] a = ni >= 6 ? java.util.Arrays.copyOf(ins, ni) : null;
    double[] b = no >= 6 ? java.util.Arrays.copyOf(outs, no) : null;
    if (a != null && Math.abs(area2(a)) / 2 <= MIN_AREA) {
      a = null;
    }
    if (b != null && Math.abs(area2(b)) / 2 <= MIN_AREA) {
      b = null;
    }
    return new double[][]{a, b};
  }
}
