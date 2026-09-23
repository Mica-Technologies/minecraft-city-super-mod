package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A palm crown's shape: fronds radiating from the top of the trunk, drawn as bent double-sided
 * strips reaching well past the cell, so one crown block on a palm log is the whole head of the
 * tree.
 *
 * <p>The crown's sprite is a two-by-two sheet, so one block needs one texture:</p>
 * <ul>
 *   <li>top left: a live frond, stalk at the bottom edge, tip at the top;</li>
 *   <li>top right: a dead frond, the same way up (the skirt);</li>
 *   <li>bottom left: the boot, the woven frond bases the crown grows from.</li>
 * </ul>
 *
 * <p>A frond is a strip of constant width whose outline the sprite's alpha cuts out, laid along a
 * spine that rises and then droops: a fan frond barely bends, a feather frond arches over. Fronds
 * sit in two tiers, young ones reaching up and old ones out and down. A skirted crown adds dead
 * fronds hanging down the trunk below the crown. Pure math, seeded by the leaf type and the
 * per-position variant.</p>
 *
 * @since 2026.9
 */
public final class TreePalmGeometry {

  /** Sprite regions, as {u0, v0, u1, v1} in sixteenths of the sprite. */
  static final double[] LIVE = {0, 0, 8, 8};
  static final double[] DEAD = {8, 0, 16, 8};
  static final double[] BOOT = {0, 8, 8, 16};

  /** Where the fronds leave the boot, in sixteenths above the crown block's floor. */
  static final double FROND_BASE_Y = 7;

  private TreePalmGeometry() {
  }

  /**
   * The quads for one crown.
   *
   * @param type    a palm leaf type
   * @param variant 0-3, turns and varies the crown per position
   * @param fancy   whether Fancy graphics is on; Fast draws about two thirds of the fronds
   *
   * @return double-sided quads, positions in sixteenths, UVs 0-16 across the sprite sheet
   */
  public static List<TreeLogGeometry.Quad> quads(TreeLeafType type, int variant, boolean fancy) {
    Random rng = new Random(type.ordinal() * 6151L + variant * 92821L);
    List<TreeLogGeometry.Quad> quads = new ArrayList<>();
    boot(quads);

    boolean feather = type == TreeLeafType.PALM_FEATHER;
    int count = fancy ? type.fronds : type.fronds * 2 / 3;
    double turn = variant * Math.PI / 2 / count + rng.nextDouble() * 0.3;
    int segments = feather ? 3 : 2;
    double droop = feather ? 1.25 : 0.35;
    for (int i = 0; i < count; i++) {
      double yaw = turn + i * 2 * Math.PI / count + (rng.nextDouble() - 0.5) * 0.35;
      boolean upper = i % 2 == 0;
      double elevation = upper
          ? Math.toRadians(feather ? 25 + rng.nextDouble() * 30 : 25 + rng.nextDouble() * 35)
          : Math.toRadians(feather ? -5 + rng.nextDouble() * 20 : -25 + rng.nextDouble() * 30);
      double length = type.frondLength * (0.8 + rng.nextDouble() * 0.3) * (upper ? 0.9 : 1.0);
      double roll = (rng.nextDouble() - 0.5) * 0.8;
      double[][] spine = new double[segments + 1][];
      double dx = Math.cos(yaw);
      double dz = Math.sin(yaw);
      spine[0] = new double[]{8 + dx * 1.5, FROND_BASE_Y + rng.nextDouble(), 8 + dz * 1.5};
      for (int k = 1; k <= segments; k++) {
        double e = elevation - droop * (k - 1) / Math.max(1, segments - 1);
        double step = length / segments;
        double[] p = spine[k - 1];
        spine[k] = new double[]{p[0] + dx * Math.cos(e) * step, p[1] + Math.sin(e) * step,
            p[2] + dz * Math.cos(e) * step};
      }
      strip(quads, spine, side(yaw, roll), type.frondWidth, LIVE);
    }

    if (type.skirt) {
      int dead = fancy ? type.fronds : type.fronds / 2;
      for (int i = 0; i < dead; i++) {
        double yaw = i * 2 * Math.PI / dead + (rng.nextDouble() - 0.5) * 0.4;
        double dx = Math.cos(yaw);
        double dz = Math.sin(yaw);
        double out = 5 + rng.nextDouble() * 1.5;
        double[][] spine = {
            {8 + dx * 2, 4 + rng.nextDouble(), 8 + dz * 2},
            {8 + dx * out, -3, 8 + dz * out},
            {8 + dx * (out + 0.5), -26 - rng.nextDouble() * 6, 8 + dz * (out + 0.5)},
        };
        strip(quads, spine, side(yaw, (rng.nextDouble() - 0.5) * 0.3), 7, DEAD);
      }
    }
    return quads;
  }

  /** The unit vector across a frond: horizontal and square to its yaw, rolled a little. */
  static double[] side(double yaw, double roll) {
    return new double[]{-Math.sin(yaw) * Math.cos(roll), Math.sin(roll),
        Math.cos(yaw) * Math.cos(roll)};
  }

  /** A strip of constant width along the spine, the region's stalk end at spine[0]. */
  static void strip(List<TreeLogGeometry.Quad> quads, double[][] spine, double[] side,
      double width, double[] region) {
    int n = spine.length - 1;
    double h = width / 2;
    for (int k = 0; k < n; k++) {
      double[] a = spine[k];
      double[] b = spine[k + 1];
      double va = region[3] - (region[3] - region[1]) * k / n;
      double vb = region[3] - (region[3] - region[1]) * (k + 1) / n;
      double[][] corners = {
          {a[0] - side[0] * h, a[1] - side[1] * h, a[2] - side[2] * h},
          {a[0] + side[0] * h, a[1] + side[1] * h, a[2] + side[2] * h},
          {b[0] + side[0] * h, b[1] + side[1] * h, b[2] + side[2] * h},
          {b[0] - side[0] * h, b[1] - side[1] * h, b[2] - side[2] * h},
      };
      double[][] uv = {{region[0], va}, {region[2], va}, {region[2], vb}, {region[0], vb}};
      twoSided(quads, corners, uv);
    }
  }

  /** The boot: a short square collar the fronds grow from, capped on top. */
  static void boot(List<TreeLogGeometry.Quad> quads) {
    double r0 = 2.4;
    double r1 = 3.4;
    double top = FROND_BASE_Y + 1;
    double[][] ring0 = {{8 - r0, 0, 8 - r0}, {8 + r0, 0, 8 - r0}, {8 + r0, 0, 8 + r0},
        {8 - r0, 0, 8 + r0}};
    double[][] ring1 = {{8 - r1, top, 8 - r1}, {8 + r1, top, 8 - r1}, {8 + r1, top, 8 + r1},
        {8 - r1, top, 8 + r1}};
    double[] b = BOOT;
    for (int i = 0; i < 4; i++) {
      int j = (i + 1) % 4;
      twoSided(quads, new double[][]{ring0[i], ring0[j], ring1[j], ring1[i]},
          new double[][]{{b[0], b[3]}, {b[2], b[3]}, {b[2], b[1]}, {b[0], b[1]}});
    }
    twoSided(quads, new double[][]{ring1[0], ring1[1], ring1[2], ring1[3]},
        new double[][]{{b[0], b[1]}, {b[2], b[1]}, {b[2], b[3]}, {b[0], b[3]}});
  }

  private static void twoSided(List<TreeLogGeometry.Quad> quads, double[][] corners,
      double[][] uv) {
    TreeLogGeometry.Quad front = new TreeLogGeometry.Quad();
    TreeLogGeometry.Quad back = new TreeLogGeometry.Quad();
    for (int i = 0; i < 4; i++) {
      front.pos[i] = corners[i];
      front.uv[i] = uv[i];
      back.pos[i] = corners[3 - i];
      back.uv[i] = uv[3 - i];
    }
    double[] nf = front.faceNormal();
    double[] nb = back.faceNormal();
    for (int i = 0; i < 4; i++) {
      front.normal[i] = nf;
      back.normal[i] = nb;
    }
    quads.add(front);
    quads.add(back);
  }
}
