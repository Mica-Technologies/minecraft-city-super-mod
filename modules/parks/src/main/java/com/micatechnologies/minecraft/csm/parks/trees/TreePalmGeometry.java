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
  /** The fourth cell, where a sheet has one: the coconut palm's coconuts. */
  static final double[] NUT = {8, 8, 16, 16};
  /** The banana's fourth cell: the green hands above, the purple bell below. */
  static final double[] HANDS = {8, 8, 16, 12};
  static final double[] BELL = {8, 12, 16, 16};

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
    switch (type) {
      case PALM_CANARY:
        canary(quads, type, rng, variant, fancy);
        return quads;
      case PALM_COCONUT:
        coconut(quads, type, rng, variant, fancy);
        return quads;
      case PALM_KING:
        king(quads, type, rng, variant, fancy);
        return quads;
      case ROSETTE:
        rosette(quads, type, rng, variant, fancy);
        return quads;
      case PALM_BANANA:
      case PALM_BANANA_FRUIT:
        banana(quads, type, rng, variant, fancy, type == TreeLeafType.PALM_BANANA_FRUIT);
        return quads;
      default:
        break;
    }
    boot(quads, type.bootRadius);

    boolean feather = type == TreeLeafType.PALM_FEATHER;
    int count = fancy ? type.fronds : type.fronds * 2 / 3;
    double turn = variant * Math.PI / 2 / count + rng.nextDouble() * 0.3;
    if (type.tiers >= 3) {
      tieredFronds(quads, type, rng, count, turn);
      if (type.skirt) {
        skirt(quads, type, rng, fancy);
      }
      return quads;
    }
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
      skirt(quads, type, rng, fancy);
    }
    return quads;
  }

  /**
   * A full round head (the cabbage palm): fronds in three tiers, the young ones reaching up, a
   * level ring, and old ones bowed down, each bending further toward its tip, as a sabal's
   * costapalmate fans do. Every tier's fronds are spread round the whole crown.
   */
  private static void tieredFronds(List<TreeLogGeometry.Quad> quads, TreeLeafType type,
      Random rng, int count, double turn) {
    double[] lowDeg = {40, 6, -16};
    double[] spanDeg = {25, 20, 12};
    double[] droop = {0.35, 0.5, 0.5};
    double[] lengthScale = {0.82, 1.0, 1.0};
    int segments = 3;
    double start = type.bootRadius * 0.6;
    for (int i = 0; i < count; i++) {
      int tier = i % 3;
      // Stagger the tiers, so the fronds of one tier fill the gaps of the one above.
      double yaw = turn + i * 2 * Math.PI / count + tier * 0.7 + (rng.nextDouble() - 0.5) * 0.3;
      double elevation = Math.toRadians(lowDeg[tier] + rng.nextDouble() * spanDeg[tier]);
      double length = type.frondLength * (0.8 + rng.nextDouble() * 0.3) * lengthScale[tier];
      double roll = (rng.nextDouble() - 0.5) * 0.8;
      double dx = Math.cos(yaw);
      double dz = Math.sin(yaw);
      double[][] spine = new double[segments + 1][];
      spine[0] = new double[]{8 + dx * start, FROND_BASE_Y + (tier == 0 ? 1.5 : 0)
          + rng.nextDouble(), 8 + dz * start};
      for (int k = 1; k <= segments; k++) {
        double e = elevation - droop[tier] * (k - 1) / (segments - 1);
        double step = length / segments;
        double[] q = spine[k - 1];
        spine[k] = new double[]{q[0] + dx * Math.cos(e) * step, q[1] + Math.sin(e) * step,
            q[2] + dz * Math.cos(e) * step};
      }
      strip(quads, spine, side(yaw, roll), type.frondWidth, LIVE);
    }
  }

  /**
   * Fronds in tiers, each tier's fronds spread round the whole crown and staggered from the tier
   * above, each frond arching: it leaves the crown at its tier's elevation and bends down by its
   * tier's droop toward its tip.
   *
   * @param low   each tier's lowest elevation, in degrees
   * @param span  how much higher than that each tier's fronds may start
   * @param droop how far each tier's fronds bend down along their length, in radians
   * @param scale each tier's frond length, as a fraction of the type's
   * @param baseY where the lowest tier leaves the crown, in sixteenths
   * @param start how far out from the axis the fronds leave it
   */
  private static void tiers(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int count, double turn, double[] low, double[] span, double[] droop, double[] scale,
      double baseY, double start, int segments) {
    int tiers = low.length;
    for (int i = 0; i < count; i++) {
      int tier = i % tiers;
      double yaw = turn + i * 2 * Math.PI / count + tier * 0.7 + (rng.nextDouble() - 0.5) * 0.3;
      double elevation = Math.toRadians(low[tier] + rng.nextDouble() * span[tier]);
      double length = type.frondLength * (0.85 + rng.nextDouble() * 0.25) * scale[tier];
      double y = baseY + (tiers - 1 - tier) * 1.2 + rng.nextDouble();
      arch(quads, new double[]{8 + Math.cos(yaw) * start, y, 8 + Math.sin(yaw) * start}, yaw,
          elevation, droop[tier] * (0.85 + rng.nextDouble() * 0.3), length, segments,
          type.frondWidth, (rng.nextDouble() - 0.5) * 0.8, LIVE);
    }
  }

  /** One frond from {@code base}: out along {@code yaw}, bending down by {@code droop}. */
  private static void arch(List<TreeLogGeometry.Quad> quads, double[] base, double yaw,
      double elevation, double droop, double length, int segments, double width, double roll,
      double[] region) {
    double dx = Math.cos(yaw);
    double dz = Math.sin(yaw);
    double[][] spine = new double[segments + 1][];
    spine[0] = base;
    double step = length / segments;
    for (int k = 1; k <= segments; k++) {
      double e = elevation - droop * (k - 1) / Math.max(1, segments - 1);
      double[] q = spine[k - 1];
      spine[k] = new double[]{q[0] + dx * Math.cos(e) * step, q[1] + Math.sin(e) * step,
          q[2] + dz * Math.cos(e) * step};
    }
    strip(quads, spine, side(yaw, roll), width, region);
  }

  /**
   * The Canary Island date palm: on the "pineapple", a knob of trimmed leaf bases swelling from
   * the trunk, a huge round head of stiff fronds, the young reaching up, the old arching down
   * past the knob.
   */
  private static void canary(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int variant, boolean fancy) {
    double r = type.bootRadius;
    prism(quads, 8, r - 1.0, r + 1.2, 0, 7, BOOT);
    prism(quads, 8, r + 1.2, r - 0.6, 7, 12, BOOT);
    cap(quads, 8, r - 0.6, 12, BOOT);
    int count = fancy ? type.fronds : type.fronds * 2 / 3;
    double turn = variant * Math.PI / 2 / count + rng.nextDouble() * 0.3;
    tiers(quads, type, rng, count, turn, new double[]{52, 22, -4}, new double[]{28, 22, 14},
        new double[]{0.8, 1.0, 1.0}, new double[]{0.8, 1.0, 1.0}, 9, r * 0.7, 4);
  }

  /**
   * The coconut palm: long fronds drooping hard from a small boot, a dead frond or two hanging
   * under them, and a bunch of coconuts in the crown's armpit.
   */
  private static void coconut(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int variant, boolean fancy) {
    boot(quads, type.bootRadius);
    int count = fancy ? type.fronds : type.fronds * 2 / 3;
    double turn = variant * Math.PI / 2 / count + rng.nextDouble() * 0.3;
    tiers(quads, type, rng, count, turn, new double[]{34, 4, -18}, new double[]{24, 16, 12},
        new double[]{1.2, 1.45, 1.25}, new double[]{0.82, 1.0, 0.95}, FROND_BASE_Y,
        type.bootRadius * 0.6, 4);
    int dead = fancy ? 1 + rng.nextInt(2) : 1;
    for (int i = 0; i < dead; i++) {
      double yaw = rng.nextDouble() * 2 * Math.PI;
      arch(quads, new double[]{8 + Math.cos(yaw) * 2, 5, 8 + Math.sin(yaw) * 2}, yaw,
          Math.toRadians(-40), 0.9, type.frondLength * 0.8, 3, type.frondWidth * 0.8,
          (rng.nextDouble() - 0.5) * 0.4, DEAD);
    }
    int nuts = 7 + rng.nextInt(5);
    for (int i = 0; i < nuts; i++) {
      double yaw = i * 2.4 + rng.nextDouble() * 0.5;
      double out = type.bootRadius + 1.6 + rng.nextDouble() * 1.4;
      double half = 1.7 + rng.nextDouble() * 0.4;
      box(quads, new double[]{8 + Math.cos(yaw) * out, 3.5 + rng.nextDouble() * 3,
          8 + Math.sin(yaw) * out}, half, NUT);
    }
  }

  /**
   * The king palm: a smooth green crownshaft swelling a little from the slender trunk, the
   * unopened spear standing from its top, and a few long fronds arching out round it.
   */
  private static void king(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int variant, boolean fancy) {
    double r = type.bootRadius;
    double top = 20;
    prism(quads, 8, r - 0.5, r + 0.5, 0, 11, BOOT);
    prism(quads, 8, r + 0.5, r - 0.6, 11, top, BOOT);
    cap(quads, 8, r - 0.6, top, BOOT);
    int count = fancy ? type.fronds : type.fronds * 2 / 3;
    double turn = variant * Math.PI / 2 / count + rng.nextDouble() * 0.3;
    tiers(quads, type, rng, count, turn, new double[]{40, 12}, new double[]{30, 20},
        new double[]{1.3, 1.4}, new double[]{0.85, 1.0}, top - 1.5, r * 0.5, 4);
    // The spear: the next frond, still furled, straight up.
    strip(quads, new double[][]{{8, top - 1, 8}, {8.3, top + 9, 8.2}, {8.5, top + 18, 8.3}},
        side(rng.nextDouble() * Math.PI, 0), 2.5, LIVE);
  }

  /**
   * A Joshua tree's rosette, on the end of a branch: stiff dagger leaves pointing every way but
   * straight down, and the old ones turned down over the branch below, the shag a Joshua tree
   * wears. The sprite's live cell is three daggers, so a strip is three leaves.
   */
  private static void rosette(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int variant, boolean fancy) {
    boot(quads, type.bootRadius);
    double[] c = {8, 5.5, 8};
    int count = fancy ? type.fronds : type.fronds * 2 / 3;
    double turn = variant * 1.3 + rng.nextDouble();
    double lowSin = Math.sin(Math.toRadians(-35));
    double highSin = Math.sin(Math.toRadians(88));
    for (int i = 0; i < count; i++) {
      // Spread evenly over the part of the sphere they cover: a Fibonacci spiral.
      double sin = lowSin + (highSin - lowSin) * (i + 0.5) / count;
      double e = Math.asin(sin) + (rng.nextDouble() - 0.5) * 0.15;
      double yaw = turn + i * 2.39996 + (rng.nextDouble() - 0.5) * 0.3;
      double length = type.frondLength * (0.85 + rng.nextDouble() * 0.35);
      double dx = Math.cos(yaw) * Math.cos(e);
      double dy = Math.sin(e);
      double dz = Math.sin(yaw) * Math.cos(e);
      double[] base = {c[0] + dx * 1.2, c[1] + dy * 1.2, c[2] + dz * 1.2};
      double[] tip = {base[0] + dx * length, base[1] + dy * length, base[2] + dz * length};
      strip(quads, new double[][]{base, tip}, side(yaw, (rng.nextDouble() - 0.5) * 1.6),
          type.frondWidth, LIVE);
    }
    int dead = fancy ? 14 : 8;
    for (int i = 0; i < dead; i++) {
      double yaw = turn + i * 2 * Math.PI / dead + (rng.nextDouble() - 0.5) * 0.4;
      double dx = Math.cos(yaw);
      double dz = Math.sin(yaw);
      double out = 3.2 + rng.nextDouble() * 1.2;
      double[][] spine = {
          {8 + dx * 1.8, 4.5, 8 + dz * 1.8},
          {8 + dx * out, -2.5, 8 + dz * out},
          {8 + dx * (out + 0.5), -12 - rng.nextDouble() * 4, 8 + dz * (out + 0.5)},
      };
      strip(quads, spine, side(yaw, (rng.nextDouble() - 0.5) * 0.4), type.frondWidth, DEAD);
    }
  }

  /**
   * A banana plant's crown: the sheaths running up out of the pseudostem, huge paddle leaves
   * arching out and down from its top, the youngest still rolled and standing up, a dry one or
   * two hanging down the stem. A fruiting crown adds its bunch: the stalk arching out of the top
   * and down, the green hands hanging beside the stem, and the purple bell at the stalk's end.
   */
  private static void banana(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int variant, boolean fancy, boolean fruit) {
    double r = type.bootRadius;
    prism(quads, 8, r, r - 0.8, 0, 12, BOOT);
    cap(quads, 8, r - 0.8, 12, BOOT);
    int count = fancy ? type.fronds : type.fronds * 3 / 4;
    double turn = variant * 0.9 + rng.nextDouble();
    for (int i = 0; i < count; i++) {
      double yaw = turn + i * 2 * Math.PI / count + (rng.nextDouble() - 0.5) * 0.4;
      double elevation = Math.toRadians(42 + rng.nextDouble() * 32);
      double length = type.frondLength * (0.8 + rng.nextDouble() * 0.3);
      arch(quads, new double[]{8 + Math.cos(yaw) * 1.5, 9 + rng.nextDouble() * 4,
          8 + Math.sin(yaw) * 1.5}, yaw, elevation, 1.5 + rng.nextDouble() * 0.6, length, 4,
          type.frondWidth, (rng.nextDouble() - 0.5) * 0.6, LIVE);
    }
    // The youngest leaf, still rolled, standing straight up.
    strip(quads, new double[][]{{8, 11, 8}, {8.3, 22, 8.2}, {8.6, 30, 8.4}},
        side(rng.nextDouble() * Math.PI, 0), 3, LIVE);
    int dry = fancy ? 1 + rng.nextInt(2) : 1;
    for (int i = 0; i < dry; i++) {
      double yaw = rng.nextDouble() * 2 * Math.PI;
      double dx = Math.cos(yaw);
      double dz = Math.sin(yaw);
      strip(quads, new double[][]{{8 + dx * 2, 7, 8 + dz * 2}, {8 + dx * 5, -3, 8 + dz * 5},
          {8 + dx * 5.5, -24 - rng.nextDouble() * 6, 8 + dz * 5.5}},
          side(yaw, (rng.nextDouble() - 0.5) * 0.3), type.frondWidth * 0.5, DEAD);
    }
    if (!fruit) {
      return;
    }
    double yaw = turn + Math.PI / count;
    double dx = Math.cos(yaw);
    double dz = Math.sin(yaw);
    double out = r + 6;
    double cx = 8 + dx * out;
    double cz = 8 + dz * out;
    // The stalk, out of the top of the crown and over, down to the bunch.
    strip(quads, new double[][]{{8 + dx, 13, 8 + dz}, {8 + dx * (out - 2), 12, 8 + dz * (out - 2)},
        {cx, 4, cz}}, side(yaw, 0), 2.5, BOOT);
    prismAt(quads, 8, cx, cz, 2.8, 4.3, -14, 3, HANDS);
    capAt(quads, 8, cx, cz, 4.3, 3, true, HANDS);
    capAt(quads, 8, cx, cz, 2.8, -14, false, HANDS);
    prismAt(quads, 4, cx, cz, 0.6, 0.6, -18, -14, BOOT);
    prismAt(quads, 8, cx, cz, 0.3, 2.3, -26, -18, BELL);
    capAt(quads, 8, cx, cz, 2.3, -18, true, BELL);
  }

  /** A tapering n-sided tube from {@code y0} to {@code y1}, facing out, the region round it. */
  static void prism(List<TreeLogGeometry.Quad> quads, int sides, double r0, double r1,
      double y0, double y1, double[] region) {
    prismAt(quads, sides, 8, 8, r0, r1, y0, y1, region);
  }

  /** {@link #prism} round the vertical line through {@code (cx, cz)}. */
  static void prismAt(List<TreeLogGeometry.Quad> quads, int sides, double cx, double cz,
      double r0, double r1, double y0, double y1, double[] region) {
    for (int i = 0; i < sides; i++) {
      double a0 = 2 * Math.PI * i / sides;
      double a1 = 2 * Math.PI * (i + 1) / sides;
      double u0 = region[0] + (region[2] - region[0]) * i / sides;
      double u1 = region[0] + (region[2] - region[0]) * (i + 1) / sides;
      double[][] corners = {
          {cx + Math.cos(a0) * r0, y0, cz + Math.sin(a0) * r0},
          {cx + Math.cos(a0) * r1, y1, cz + Math.sin(a0) * r1},
          {cx + Math.cos(a1) * r1, y1, cz + Math.sin(a1) * r1},
          {cx + Math.cos(a1) * r0, y0, cz + Math.sin(a1) * r0},
      };
      double am = (a0 + a1) / 2;
      oneSided(quads, corners, new double[][]{{u0, region[3]}, {u0, region[1]}, {u1, region[1]},
          {u1, region[3]}}, new double[]{Math.cos(am), 0, Math.sin(am)});
    }
  }

  /** The flat top of a prism: a fan of quads from the centre. */
  static void cap(List<TreeLogGeometry.Quad> quads, int sides, double r, double y,
      double[] region) {
    capAt(quads, sides, 8, 8, r, y, true, region);
  }

  /** {@link #cap} round {@code (cx, cz)}, facing up or down. */
  static void capAt(List<TreeLogGeometry.Quad> quads, int sides, double cx, double cz, double r,
      double y, boolean up, double[] region) {
    double cu = (region[0] + region[2]) / 2;
    double cv = (region[1] + region[3]) / 2;
    double hu = (region[2] - region[0]) / 2;
    double hv = (region[3] - region[1]) / 2;
    for (int i = 0; i < sides; i += 2) {
      double[][] corners = new double[4][];
      double[][] uv = new double[4][];
      corners[0] = new double[]{cx, y, cz};
      uv[0] = new double[]{cu, cv};
      for (int k = 0; k < 3; k++) {
        double a = 2 * Math.PI * (i + k) / sides;
        corners[k + 1] = new double[]{cx + Math.cos(a) * r, y, cz + Math.sin(a) * r};
        uv[k + 1] = new double[]{cu + Math.cos(a) * hu, cv + Math.sin(a) * hv};
      }
      oneSided(quads, corners, uv, new double[]{0, up ? 1 : -1, 0});
    }
  }

  /** A small cube, faces out, the region on every face. */
  static void box(List<TreeLogGeometry.Quad> quads, double[] c, double h, double[] region) {
    int[][] steps = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
    for (int axis = 0; axis < 3; axis++) {
      for (int sign = -1; sign <= 1; sign += 2) {
        int u = (axis + 1) % 3;
        int v = (axis + 2) % 3;
        double[][] corners = new double[4][];
        for (int k = 0; k < 4; k++) {
          double[] p = c.clone();
          p[axis] += sign * h;
          p[u] += steps[k][0] * h;
          p[v] += steps[k][1] * h;
          corners[k] = p;
        }
        double[] n = new double[3];
        n[axis] = sign;
        oneSided(quads, corners, new double[][]{{region[0], region[3]}, {region[2], region[3]},
            {region[2], region[1]}, {region[0], region[1]}}, n);
      }
    }
  }

  /** One quad facing {@code outward}: its corners are reversed if they wind the other way. */
  private static void oneSided(List<TreeLogGeometry.Quad> quads, double[][] corners,
      double[][] uv, double[] outward) {
    TreeLogGeometry.Quad q = new TreeLogGeometry.Quad();
    for (int i = 0; i < 4; i++) {
      q.pos[i] = corners[i];
      q.uv[i] = uv[i];
    }
    double[] n = q.faceNormal();
    if (n[0] * outward[0] + n[1] * outward[1] + n[2] * outward[2] < 0) {
      for (int i = 0; i < 4; i++) {
        q.pos[i] = corners[3 - i];
        q.uv[i] = uv[3 - i];
      }
      n = q.faceNormal();
    }
    for (int i = 0; i < 4; i++) {
      q.normal[i] = n;
    }
    quads.add(q);
  }

  /** Dead fronds hanging down the trunk from under the crown, clear of the boot. */
  private static void skirt(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      boolean fancy) {
    int dead = fancy ? type.fronds : type.fronds / 2;
    double reach = type.bootRadius + 2.6;
    for (int i = 0; i < dead; i++) {
      double yaw = i * 2 * Math.PI / dead + (rng.nextDouble() - 0.5) * 0.4;
      double dx = Math.cos(yaw);
      double dz = Math.sin(yaw);
      double out = reach + rng.nextDouble() * 1.5;
      double[][] spine = {
          {8 + dx * 2, 4 + rng.nextDouble(), 8 + dz * 2},
          {8 + dx * out, -3, 8 + dz * out},
          {8 + dx * (out + 0.5), -26 - rng.nextDouble() * 6, 8 + dz * (out + 0.5)},
      };
      strip(quads, spine, side(yaw, (rng.nextDouble() - 0.5) * 0.3), 7, DEAD);
    }
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
  static void boot(List<TreeLogGeometry.Quad> quads, double r0) {
    double r1 = r0 + 1.0;
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
