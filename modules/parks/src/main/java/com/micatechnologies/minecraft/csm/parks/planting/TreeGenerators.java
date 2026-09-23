package com.micatechnologies.minecraft.csm.parks.planting;

import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Grows a {@link TreePreset} into a {@link TreePlan}, relative to the base of the trunk at the
 * origin, leaning toward {@code facing}. Four shapes cover the catalogue:
 *
 * <ul>
 *   <li><b>Profile</b>: a straight trunk and a crown whose radius at each height comes from a
 *       profile (a column, a cone). Cypress, ginkgo.</li>
 *   <li><b>Limb</b>: a trunk leaning by diagonal steps, then limbs drawn as voxel lines out to
 *       foliage clusters, tapering as they go. The log kit bridges each diagonal step, so a
 *       stepped line reads as a smooth arching limb. Live oak, elm, plane, honey locust.</li>
 *   <li><b>Palm</b>: a trunk whose sideways offset grows with the square of the height, so it
 *       curves rather than leans straight, and a crown on top.</li>
 *   <li><b>Head</b>: a clear trunk and a clipped round head.</li>
 * </ul>
 *
 * <p>A line never steps on all three axes at once: the log kit bridges edge diagonals, not corner
 * ones, so such a step is split in two. A limbed tree has no leaves under the street clearance, so
 * its canopy stays above traffic and its trunk stays clear.</p>
 *
 * @since 2026.9
 */
public final class TreeGenerators {

  private TreeGenerators() {
  }

  /**
   * Grows a tree.
   *
   * @param preset the species
   * @param facing the horizontal direction the tree leans and reaches toward
   * @param rng    the planting's randomness
   *
   * @return the plan, relative to the base of the trunk
   */
  public static TreePlan grow(TreePreset preset, EnumFacing facing, Random rng) {
    TreePlan plan = new TreePlan();
    switch (preset.shape) {
      case PROFILE:
        profile(plan, preset, rng);
        break;
      case LIMB:
        limb(plan, preset, facing, rng);
        break;
      case PALM:
        palm(plan, preset, facing, rng);
        break;
      default:
        head(plan, preset, rng);
        break;
    }
    return plan;
  }

  private static int range(Random rng, int min, int max) {
    return min + rng.nextInt(Math.max(1, max - min + 1));
  }

  // --- Profile ---

  private static void profile(TreePlan plan, TreePreset p, Random rng) {
    int trunk = range(rng, p.trunkMin, p.trunkMax);
    int height = range(rng, p.heightMin, p.heightMax);
    int crownTop = height - 1;
    // The trunk runs up through the crown to two below its top, thinning as it goes.
    int logTop = p.clusterRx < 1 ? trunk - 1 : crownTop - 2;
    for (int y = 0; y <= logTop; y++) {
      TreeLogWidth w = y < trunk ? p.trunkWidth : p.limbWidth;
      plan.log(new BlockPos(0, y, 0), p.wood, w, EnumFacing.Axis.Y);
    }
    int r = (int) Math.ceil(p.clusterRx);
    for (int y = trunk; y <= crownTop; y++) {
      double t = (y - trunk) / (double) Math.max(1, crownTop - trunk);
      // Rounded at the bottom, widest a quarter of the way up, pointed at the top.
      double radius = p.clusterRx * Math.min(1, (t + 0.12) * 3.5) * Math.pow(1 - t, 0.75);
      for (int x = -r; x <= r; x++) {
        for (int z = -r; z <= r; z++) {
          double d = Math.sqrt(x * x + z * z);
          if (d <= radius + 0.35 + (rng.nextDouble() - 0.5) * 0.3 || (x == 0 && z == 0)) {
            plan.leaves(new BlockPos(x, y, z), p.leaves);
          }
        }
      }
    }
  }

  // --- Limb ---

  private static void limb(TreePlan plan, TreePreset p, EnumFacing facing, Random rng) {
    int fx = facing.getXOffset();
    int fz = facing.getZOffset();
    int trunk = range(rng, p.trunkMin, p.trunkMax);
    int lean = range(rng, p.leanMin, p.leanMax);
    BlockPos pos = BlockPos.ORIGIN;
    for (int y = 0; y < trunk; y++) {
      TreeLogWidth w = y < (trunk + 1) / 2 ? p.trunkWidth : thinner(p.trunkWidth);
      plan.log(pos, p.wood, w, EnumFacing.Axis.Y);
      boolean step = lean > 0 && y >= 1 && y < trunk - 1 && rng.nextDouble() < 0.7;
      if (step) {
        lean--;
        pos = pos.add(fx, 1, fz);
      } else {
        pos = pos.up();
      }
    }
    BlockPos top = pos.down();

    int limbs = range(rng, p.limbsMin, p.limbsMax);
    List<double[]> clusters = new ArrayList<>();
    for (int i = 0; i < limbs; i++) {
      boolean back = p.backLimb && i == limbs - 1;
      // Spread the limbs evenly across the fan, jittered, facing the lean.
      double a = limbs == 1 ? 0 : -p.spread + 2 * p.spread * i / (limbs - 1);
      a += (rng.nextDouble() - 0.5) * 0.4;
      if (back) {
        a = Math.PI + (rng.nextDouble() - 0.5) * 0.8;
      }
      double reach = range(rng, p.reachMin, p.reachMax) * (back ? 0.5 : 1.0);
      int rise = range(rng, p.riseMin, p.riseMax);
      // Along the lean (fx, fz) and across it (-fz, fx).
      double ox = Math.cos(a) * reach * fx - Math.sin(a) * reach * fz;
      double oz = Math.cos(a) * reach * fz + Math.sin(a) * reach * fx;
      int ty = Math.max(top.getY() + rise, p.clearance + (int) Math.ceil(p.clusterRy));
      BlockPos target = new BlockPos(top.getX() + Math.round(ox), ty,
          top.getZ() + Math.round(oz));
      line(plan, p, top, target);
      clusters.add(new double[]{target.getX(), target.getY(), target.getZ(), 1.0});
    }
    // A smaller cluster over the top of the trunk closes the crown.
    clusters.add(new double[]{top.getX(), top.getY() + p.clusterRy, top.getZ(), 0.8});
    for (double[] c : clusters) {
      cluster(plan, p, c, rng);
    }
    if (p.extra != null) {
      hangMoss(plan, p, rng);
    }
  }

  private static TreeLogWidth thinner(TreeLogWidth w) {
    return w.ordinal() == 0 ? w : TreeLogWidth.values()[w.ordinal() - 1];
  }

  /** A limb from {@code a} to {@code b}, tapering from the limb width toward a twig. */
  private static void line(TreePlan plan, TreePreset p, BlockPos a, BlockPos b) {
    int dx = b.getX() - a.getX();
    int dy = b.getY() - a.getY();
    int dz = b.getZ() - a.getZ();
    int n = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
    if (n == 0) {
      return;
    }
    EnumFacing.Axis axis = Math.abs(dy) >= Math.max(Math.abs(dx), Math.abs(dz))
        ? EnumFacing.Axis.Y : Math.abs(dx) >= Math.abs(dz) ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
    BlockPos prev = a;
    for (int i = 1; i <= n; i++) {
      double t = i / (double) n;
      BlockPos next = new BlockPos(a.getX() + Math.round(dx * t), a.getY() + Math.round(dy * t),
          a.getZ() + Math.round(dz * t));
      TreeLogWidth w = t < 0.4 ? p.limbWidth : t < 0.8 ? thinner(p.limbWidth)
          : thinner(thinner(p.limbWidth));
      if (next.getX() != prev.getX() && next.getY() != prev.getY()
          && next.getZ() != prev.getZ()) {
        // A corner diagonal: go the edge diagonal first, then the face.
        BlockPos mid = new BlockPos(next.getX(), next.getY(), prev.getZ());
        plan.log(mid, p.wood, w, axis);
      }
      plan.log(next, p.wood, w, axis);
      prev = next;
    }
  }

  /** A flattened ellipsoid of leaves, ragged at the edge, kept above the street clearance. */
  private static void cluster(TreePlan plan, TreePreset p, double[] c, Random rng) {
    double rx = p.clusterRx * c[3];
    double ry = p.clusterRy * c[3];
    int ix = (int) Math.ceil(rx);
    int iy = (int) Math.ceil(ry);
    for (int x = -ix; x <= ix; x++) {
      for (int y = -iy; y <= iy; y++) {
        for (int z = -ix; z <= ix; z++) {
          double d = (x * x + z * z) / (rx * rx) + (y * y) / (ry * ry);
          if (d > 1 + (rng.nextDouble() - 0.5) * 0.5) {
            continue;
          }
          BlockPos at = new BlockPos(c[0] + x, c[1] + y, c[2] + z);
          if (at.getY() < p.clearance) {
            continue;
          }
          plan.leaves(at, p.leaves);
        }
      }
    }
  }

  /** Moss under some of the crown's lowest leaves, one to three blocks long. */
  private static void hangMoss(TreePlan plan, TreePreset p, Random rng) {
    List<BlockPos> undersides = new ArrayList<>();
    for (java.util.Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      if (e.getValue().kind == TreePlan.Kind.LEAVES && plan.isEmpty(e.getKey().down())) {
        undersides.add(e.getKey());
      }
    }
    for (BlockPos leaf : undersides) {
      if (rng.nextDouble() > 0.22) {
        continue;
      }
      int length = 1 + rng.nextInt(3);
      for (int k = 1; k <= length; k++) {
        BlockPos at = leaf.down(k);
        if (at.getY() < 2 || !plan.isEmpty(at)) {
          break;
        }
        plan.hanging(at, p.extra);
      }
    }
  }

  // --- Palm ---

  private static void palm(TreePlan plan, TreePreset p, EnumFacing facing, Random rng) {
    int height = range(rng, p.heightMin, p.heightMax);
    int lean = range(rng, p.leanMin, p.leanMax);
    int fx = facing.getXOffset();
    int fz = facing.getZOffset();
    int offset = 0;
    BlockPos pos = BlockPos.ORIGIN;
    for (int y = 0; y < height; y++) {
      plan.log(pos, p.wood, p.trunkWidth, EnumFacing.Axis.Y);
      // The offset grows with the square of the height: upright at the base, curving out.
      double t = (y + 1) / (double) height;
      int want = (int) Math.round(lean * Math.min(1, t * t * 1.25));
      if (want > offset && y < height - 1) { // the crown sits straight on the top log
        offset++;
        pos = pos.add(fx, 1, fz);
      } else {
        pos = pos.up();
      }
    }
    String crown = p.extra != null && rng.nextBoolean() ? p.extra : p.leaves;
    plan.leaves(pos, crown);
  }

  // --- Head ---

  private static void head(TreePlan plan, TreePreset p, Random rng) {
    int trunk = range(rng, p.trunkMin, p.trunkMax);
    double r = p.clusterRx;
    int centre = trunk + (int) Math.floor(r);
    for (int y = 0; y <= centre; y++) {
      plan.log(new BlockPos(0, y, 0), p.wood, y < trunk ? p.trunkWidth : p.limbWidth,
          EnumFacing.Axis.Y);
    }
    int ir = (int) Math.ceil(r);
    for (int x = -ir; x <= ir; x++) {
      for (int y = -ir; y <= ir; y++) {
        for (int z = -ir; z <= ir; z++) {
          if (x * x + y * y + z * z <= r * r + 0.6 + (rng.nextDouble() - 0.5) * 0.6) {
            plan.leaves(new BlockPos(x, centre + y, z), p.leaves);
          }
        }
      }
    }
  }
}
