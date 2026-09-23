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
 * <p>Leaves are <b>sheeted</b>: each <b>open</b> face (no leaves and no solid block beyond it) is a
 * sheet of leaves just inside the cell's face (one quad, facing out; the sprite's ragged alpha
 * keeps its outline soft), with a tuft card past each open side and the top breaking the
 * silhouette and one card inside (two in an upright needle column). That is a leafy block with a
 * fuzzy edge, at around a third of the quads of a crown drawn all in cards. Airy leaves (honey
 * locust, jacaranda, gum) are sheeted too; their sprite is mostly gaps, so light still comes
 * through the sheets. Clipped leaves are sheets a hair inside the face on a few interior cards,
 * with no tufts.</p>
 *
 * <ul>
 *   <li>Nothing is drawn between two leaves cells, and a cell with no open face draws only its
 *       interior card (enough that the crown is not see-through).</li>
 *   <li>Weeping leaves hang a curtain of long strands from the crown's underside, down each
 *       open side of a cell open below and under it, reaching well under the cell, so a crown of
 *       them weeps rather than billows.</li>
 * </ul>
 *
 * <p>The shape is seeded by the block's type, a per-position variant (0-3) and the open faces, so
 * the same block in the same place always looks the same, and neighbouring blocks differ. Pure
 * math; {@code TreeLeavesBakedModel} bakes it.</p>
 *
 * @since 2026.9
 */
public final class TreeLeavesGeometry {

  /** How far a tuft card's centre may sit past the face, in sixteenths. */
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

    if (type != TreeLeafType.CLIPPED) {
      sheeted(quads, type, rng, open, fancy);
      return quads;
    }
    // Clipped flat: a few cards inside and a sheet of leaves a hair inside each open face, so a
    // run of them is a box hedge on stilts rather than a cloud. A cell closed on every side keeps
    // one card, so a gap in the crown is not sky.
    int interior = open == 0 ? 1 : Math.min(type.interior, 1 + Integer.bitCount(open));
    if (!fancy) {
      interior = Math.max(2, interior / 2);
    }
    for (int i = 0; i < interior; i++) {
      double[] c = {rng.nextDouble() * 10 + 3, rng.nextDouble() * 10 + 3,
          rng.nextDouble() * 10 + 3};
      card(quads, type, rng, c, size(type, rng));
    }
    for (EnumFacing f : EnumFacing.values()) {
      if ((open >> f.getIndex() & 1) != 0) {
        clippedFace(quads, f, 0.3);
      }
    }
    return quads;
  }

  /** How far inside the cell's face a sheet lies, in sixteenths. */
  static final double SHEET_INSET = 1.0;

  /**
   * A sheeted cell: a sheet on each open face, the type's cards inside (two for upright needles,
   * whose column is all inside), a tuft card past each open side and the top, and, under a
   * weeping crown, the curtain in place of the side tufts.
   */
  private static void sheeted(List<TreeLogGeometry.Quad> quads, TreeLeafType type, Random rng,
      int open, boolean fancy) {
    boolean curtained = fancy && type == TreeLeafType.WEEPING
        && (open >> EnumFacing.DOWN.getIndex() & 1) != 0;
    for (int i = 0; i < type.interior; i++) {
      double[] c = {rng.nextDouble() * 8 + 4, rng.nextDouble() * 8 + 4, rng.nextDouble() * 8 + 4};
      card(quads, type, rng, c, size(type, rng));
    }
    for (EnumFacing f : EnumFacing.values()) {
      if ((open >> f.getIndex() & 1) == 0) {
        continue;
      }
      clippedFace(quads, f, SHEET_INSET);
      if (!fancy || f == EnumFacing.DOWN) {
        continue;
      }
      if (curtained && f != EnumFacing.UP) {
        // The curtain hangs past this side and is its outline.
        continue;
      }
      // A tuft past the face, so the crown's outline is not the block grid.
      double[] n = {f.getXOffset(), f.getYOffset(), f.getZOffset()};
      double reach = 8 + rng.nextDouble() * FRINGE_REACH - 1;
      double[] c = {8 + n[0] * reach, 8 + n[1] * reach, 8 + n[2] * reach};
      for (int k = 0; k < 3; k++) {
        if (n[k] == 0) {
          c[k] += (rng.nextDouble() - 0.5) * 9;
        }
      }
      card(quads, type, rng, c, size(type, rng) * 1.05);
    }
    if (curtained) {
      curtain(quads, rng, open);
    }
  }

  /**
   * A weeping crown's curtain, for a cell open below: long, narrow upright strands just outside
   * each open side, from near its top to well below it, and more under it. Only the crown's
   * underside hangs one; higher up the crown's wall the sheets already read as leaves, and a
   * curtain there would hang behind the one below it.
   */
  private static void curtain(List<TreeLogGeometry.Quad> quads, Random rng, int open) {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      if ((open >> f.getIndex() & 1) == 0) {
        continue;
      }
      double nx = f.getXOffset();
      double nz = f.getZOffset();
      // Along the face: a strand's width runs this way, so the strands read as one curtain.
      double tx = -nz;
      double tz = nx;
      double yaw = Math.atan2(tz, tx);
      for (int i = 0; i < 2; i++) {
        double along = (i + 0.2 + rng.nextDouble() * 0.6) * 16.0 / 2 - 8;
        double out = 8.3 + rng.nextDouble() * 1.8;
        double top = 12 + rng.nextDouble() * 3.5;
        double bottom = -4 - rng.nextDouble() * 10;
        strand(quads, rng, 8 + nx * out + tx * along, 8 + nz * out + tz * along, top, bottom,
            6 + rng.nextDouble() * 2.5, yaw);
      }
    }
    for (int i = 0; i < 2; i++) {
      strand(quads, rng, 2 + rng.nextDouble() * 12, 2 + rng.nextDouble() * 12,
          4 + rng.nextDouble() * 3, -8 - rng.nextDouble() * 12, 4 + rng.nextDouble() * 3,
          rng.nextDouble() * Math.PI);
    }
  }

  /** One hanging strand: an upright double-sided card from {@code top} down to {@code bottom}. */
  private static void strand(List<TreeLogGeometry.Quad> quads, Random rng, double x, double z,
      double top, double bottom, double width, double yaw) {
    addCard(quads, new double[]{x, (top + bottom) / 2, z}, width / 2, (top - bottom) / 2,
        yaw + (rng.nextDouble() - 0.5) * 0.5, (rng.nextDouble() - 0.5) * 0.12);
  }

  /** A sheet of leaves over one face of the cell, {@code in} sixteenths inside it, facing out. */
  private static void clippedFace(List<TreeLogGeometry.Quad> quads, EnumFacing f, double in) {
    double[][] c;
    switch (f) {
      case UP:
        c = new double[][]{{0, 16 - in, 16}, {16, 16 - in, 16}, {16, 16 - in, 0},
            {0, 16 - in, 0}};
        break;
      case DOWN:
        c = new double[][]{{0, in, 0}, {16, in, 0}, {16, in, 16}, {0, in, 16}};
        break;
      case NORTH:
        c = new double[][]{{16, 0, in}, {0, 0, in}, {0, 16, in}, {16, 16, in}};
        break;
      case SOUTH:
        c = new double[][]{{0, 0, 16 - in}, {16, 0, 16 - in}, {16, 16, 16 - in},
            {0, 16, 16 - in}};
        break;
      case WEST:
        c = new double[][]{{in, 0, 0}, {in, 0, 16}, {in, 16, 16}, {in, 16, 0}};
        break;
      default:
        c = new double[][]{{16 - in, 0, 16}, {16 - in, 0, 0}, {16 - in, 16, 0},
            {16 - in, 16, 16}};
        break;
    }
    double[][] uv = {{0, 16}, {16, 16}, {16, 0}, {0, 0}};
    TreeLogGeometry.Quad q = new TreeLogGeometry.Quad();
    for (int i = 0; i < 4; i++) {
      q.pos[i] = c[i];
      q.uv[i] = uv[i];
    }
    double[] n = q.faceNormal();
    for (int i = 0; i < 4; i++) {
      q.normal[i] = n;
    }
    quads.add(q);
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
