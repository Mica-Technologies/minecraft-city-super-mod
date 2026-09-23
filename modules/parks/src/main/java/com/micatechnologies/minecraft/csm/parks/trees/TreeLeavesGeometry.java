package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.EnumFacing;

/**
 * A leaves block's shape: leaf <b>cards</b>, double-sided cutout quads each showing the species'
 * leaf-cluster sprite, arranged so a group of leaves blocks reads as a crown rather than a stack
 * of green cubes.
 *
 * <ul>
 *   <li>A few cards inside the cell, at seeded positions and angles.</li>
 *   <li>A fringe of smaller cards reaching up to three sixteenths past each <b>open</b> face -- one
 *       with no leaves and no solid block beyond it -- so the crown's outline is not the block
 *       grid. A one-wide column of leaves becomes a narrow columnar crown.</li>
 *   <li>Nothing is drawn between two leaves cells, and a cell with no open face draws only a
 *       reduced interior (enough that the crown is not see-through).</li>
 *   <li>Weeping leaves hang their bottom fringe well below the cell.</li>
 * </ul>
 *
 * <p>The shape is seeded by the block's type, a per-position variant (0-3) and the open faces, so
 * the same block in the same place always looks the same, and neighbouring blocks differ. Pure
 * math; {@code TreeLeavesBakedModel} bakes it.</p>
 *
 * @since 2026.9
 */
public final class TreeLeavesGeometry {

  /** How far a fringe card's centre may sit past the face, in sixteenths. */
  static final double FRINGE_REACH = 3.0;
  /** Variants per position. */
  public static final int VARIANTS = 4;

  private TreeLeavesGeometry() {
  }

  /**
   * Packs the open faces and the variant into the value the extended state carries.
   *
   * @param openFaces bit i set when face i (EnumFacing index) is open
   * @param variant   0-3
   *
   * @return the shape key
   */
  public static int key(int openFaces, int variant) {
    return (openFaces & 63) | ((variant & 3) << 6);
  }

  /**
   * The quads for one shape.
   *
   * @param type  the leaf type
   * @param key   {@link #key}
   * @param fancy whether Fancy graphics is on; Fast draws the interior only, fewer cards
   *
   * @return double-sided cards, positions in sixteenths, UVs 0-16
   */
  public static List<TreeLogGeometry.Quad> quads(TreeLeafType type, int key, boolean fancy) {
    int open = key & 63;
    int variant = (key >> 6) & 3;
    if (type.isPalm()) {
      return TreePalmGeometry.quads(type, variant, fancy);
    }
    Random rng = new Random(type.ordinal() * 7919L + variant * 104729L + open * 31L);
    List<TreeLogGeometry.Quad> quads = new ArrayList<>();

    int interior = open == 0 ? 2 : type.interior;
    if (!fancy) {
      interior = Math.max(2, interior / 2);
    }
    for (int i = 0; i < interior; i++) {
      double[] c = {rng.nextDouble() * 10 + 3, rng.nextDouble() * 10 + 3,
          rng.nextDouble() * 10 + 3};
      card(quads, type, rng, c, size(type, rng));
    }
    if (!fancy) {
      return quads;
    }
    for (EnumFacing f : EnumFacing.values()) {
      if ((open >> f.getIndex() & 1) == 0) {
        continue;
      }
      double[] n = {f.getXOffset(), f.getYOffset(), f.getZOffset()};
      boolean hang = type == TreeLeafType.WEEPING && f == EnumFacing.DOWN;
      if (f.getAxis() == EnumFacing.Axis.Y && !type.upright && !hang) {
        // A near-flat card across an open top or bottom, so a canopy seen from below (or from a
        // window above) is a mass of leaves rather than sky between scattered cards.
        double y = f == EnumFacing.DOWN ? 1.5 + rng.nextDouble() : 14.5 - rng.nextDouble();
        addCard(quads, new double[]{8, y, 8}, 9.5, 9.5, rng.nextDouble() * Math.PI,
            Math.PI / 2 + (rng.nextDouble() * 2 - 1) * 0.2);
      }
      int count = hang ? type.fringe + 2 : type.fringe;
      for (int i = 0; i < count; i++) {
        double reach = 8 + rng.nextDouble() * FRINGE_REACH - (hang ? 6 : 1);
        double[] c = {8 + n[0] * reach, 8 + n[1] * reach, 8 + n[2] * reach};
        for (int k = 0; k < 3; k++) {
          if (n[k] == 0) {
            c[k] += (rng.nextDouble() - 0.5) * 11;
          }
        }
        double s = size(type, rng) * 0.8;
        if (hang) {
          hangingCard(quads, rng, c, s);
        } else {
          card(quads, type, rng, c, s);
        }
      }
    }
    return quads;
  }

  private static double size(TreeLeafType type, Random rng) {
    return type.minSize + rng.nextDouble() * (type.maxSize - type.minSize);
  }

  /** One double-sided card centred at {@code c}, turned about Y and tilted. */
  private static void card(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      double[] c, double size) {
    double yaw = rng.nextDouble() * Math.PI;
    double tilt = (rng.nextDouble() * 2 - 1) * type.tilt;
    double hw = size / 2;
    double hh = type.upright ? size * 0.9 : size / 2;
    if (type.upright) {
      hw = size * 0.35;
    }
    addCard(quads, c, hw, hh, yaw, tilt);
  }

  /** A weeping curtain card: tall, upright, hanging from above its centre. */
  private static void hangingCard(List<TreeLogGeometry.Quad> quads, Random rng, double[] c,
      double size) {
    double yaw = rng.nextDouble() * Math.PI;
    addCard(quads, new double[]{c[0], c[1] - size * 0.4, c[2]}, size * 0.35, size * 0.9, yaw,
        (rng.nextDouble() * 2 - 1) * 0.15);
  }

  private static void addCard(List<TreeLogGeometry.Quad> quads, double[] c, double hw, double hh,
      double yaw, double tilt) {
    double ca = Math.cos(yaw);
    double sa = Math.sin(yaw);
    double ct = Math.cos(tilt);
    double st = Math.sin(tilt);
    double[][] corners = new double[4][];
    double[][] local = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
    for (int i = 0; i < 4; i++) {
      double u = local[i][0];
      double w = local[i][1];
      // Upright card in the plane of its yaw, then leaned back by the tilt.
      double x = u;
      double y = w * ct;
      double z = w * st;
      corners[i] = new double[]{c[0] + x * ca - z * sa, c[1] + y, c[2] + x * sa + z * ca};
    }
    double[][] uv = {{0, 16}, {16, 16}, {16, 0}, {0, 0}};
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
