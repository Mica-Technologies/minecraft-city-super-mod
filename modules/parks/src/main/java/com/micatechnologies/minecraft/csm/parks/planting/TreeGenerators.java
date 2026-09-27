package com.micatechnologies.minecraft.csm.parks.planting;

import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Grows a {@link TreePreset} into a {@link TreePlan}, relative to the base of the trunk at the
 * origin, into the room a {@link TreeSpace} gives it. Six shapes cover the catalogue:
 *
 * <ul>
 *   <li><b>Profile</b>: a straight trunk and a crown whose radius at each height comes from a
 *       profile (a column, a cone). Cypress, ginkgo.</li>
 *   <li><b>Limb</b>: a trunk leaning by diagonal steps, sometimes forking into two leaders, then
 *       curved limbs out to foliage clusters, some with a side branch, tapering as they go. The
 *       log kit bridges each diagonal step, so a stepped line reads as a smooth arching limb.
 *       Live oak, elm, plane, honey locust.</li>
 *   <li><b>Palm</b>: a trunk whose sideways offset grows with the square of the height, so it
 *       curves rather than leans straight, and a crown on top.</li>
 *   <li><b>Head</b>: a clear trunk and a clipped round head.</li>
 *   <li><b>Box</b>: a clear trunk and a box-clipped crown wide across the facing, so a row
 *       joins into a pleached hedge on stilts.</li>
 *   <li><b>Pollard</b>: a stout trunk cut back to a head of knuckles, each with a tuft.</li>
 *   <li><b>Tiered</b>: a tall conifer with its limbs in whorls up the trunk, each tipped with a
 *       flat pad, the whorls shorter toward the top. Eastern white pine.</li>
 * </ul>
 *
 * <p><b>Growing into the room there is.</b> A street tree next to a building has grown away from
 * it and been pruned flat on that side for years. The generator does the same: it looks around
 * the planting spot first, leans the trunk away from walls (the planter's heading is only where
 * it would lean in the open), turns limbs that would run into something toward open sky, cuts a
 * limb back where it cannot turn, keeps logs a block off any wall, and fills each foliage cluster
 * outward from its limb only through open cells, so a crown meets a wall with a flat face and
 * never reaches through it. Another tree's leaves are no wall: a canopy grows up to them and
 * joins them.</p>
 *
 * <p>A line never steps on all three axes at once: the log kit bridges edge diagonals, not corner
 * ones, so such a step is split in two. A limbed tree has no leaves under the street clearance, so
 * its canopy stays above traffic and its trunk stays clear.</p>
 *
 * @since 2026.9
 */
public final class TreeGenerators {

  /** How far round from its first choice a limb may turn to find room, in radians. */
  private static final double[] TURNS = {0, 0.35, -0.35, 0.7, -0.7, 1.05, -1.05};
  /** How many blocks above a leaves cell must be clear of anything built for it to grow. */
  private static final int ROOF_CLEARANCE = 3;

  private TreeGenerators() {
  }

  /**
   * Grows a tree in the open.
   *
   * @param preset the species
   * @param facing the horizontal direction the tree leans and reaches toward
   * @param rng    the planting's randomness
   *
   * @return the plan, relative to the base of the trunk
   */
  public static TreePlan grow(TreePreset preset, EnumFacing facing, Random rng) {
    return grow(preset, heading(facing), rng, TreeSpace.OPEN);
  }

  /**
   * Grows a tree into the room it has.
   *
   * @param preset  the species
   * @param heading the direction the tree would lean in the open, in radians: 0 toward +X
   *                (east), {@code PI / 2} toward +Z (south)
   * @param rng     the planting's randomness
   * @param space   what is around the planting spot
   *
   * @return the plan, relative to the base of the trunk
   */
  public static TreePlan grow(TreePreset preset, double heading, Random rng, TreeSpace space) {
    Grower g = new Grower(preset, rng, space.cached());
    switch (preset.shape) {
      case PROFILE:
        g.profile();
        break;
      case LIMB:
        g.limb(heading);
        break;
      case PALM:
        g.palm(heading);
        break;
      case BOX:
        g.box(facing(heading));
        break;
      case POLLARD:
        g.pollard(heading);
        break;
      case TIERED:
        g.tiered(heading);
        break;
      default:
        g.head(heading);
        break;
    }
    if (!g.plan.hasNoRoom() && !g.plan.parts().values().stream()
        .anyMatch(part -> part.kind == TreePlan.Kind.LEAVES)) {
      g.plan.noRoom(); // a bare trunk is not a tree
    }
    return g.plan;
  }

  /** The heading of a horizontal facing. */
  public static double heading(EnumFacing facing) {
    return Math.atan2(facing.getZOffset(), facing.getXOffset());
  }

  /**
   * The heading a player looks along, from their yaw (0 south, 90 west).
   *
   * @param yawDegrees the player's rotation yaw
   *
   * @return the heading
   */
  public static double headingOfYaw(float yawDegrees) {
    double yaw = Math.toRadians(yawDegrees);
    return Math.atan2(Math.cos(yaw), -Math.sin(yaw));
  }

  /** The horizontal facing nearest a heading. */
  static EnumFacing facing(double heading) {
    double x = Math.cos(heading);
    double z = Math.sin(heading);
    if (Math.abs(x) >= Math.abs(z)) {
      return x >= 0 ? EnumFacing.EAST : EnumFacing.WEST;
    }
    return z >= 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
  }

  private static TreeLogWidth thinner(TreeLogWidth w) {
    return w.ordinal() == 0 ? w : TreeLogWidth.values()[w.ordinal() - 1];
  }

  /**
   * Steps from {@code a} toward {@code b} one cell at a time, each step to a face or edge-diagonal
   * neighbour: never all three axes at once, since the log kit does not bridge a corner diagonal.
   *
   * @return the cells after {@code a}, ending at {@code b}
   */
  static List<BlockPos> walk(BlockPos a, BlockPos b) {
    List<BlockPos> cells = new ArrayList<>();
    int x = a.getX();
    int y = a.getY();
    int z = a.getZ();
    while (x != b.getX() || y != b.getY() || z != b.getZ()) {
      int dx = b.getX() - x;
      int dy = b.getY() - y;
      int dz = b.getZ() - z;
      int sx = Integer.signum(dx);
      int sy = Integer.signum(dy);
      int sz = Integer.signum(dz);
      if (sx != 0 && sy != 0 && sz != 0) {
        // A corner step: hold back the axis with the least left to go.
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);
        if (ax <= ay && ax <= az) {
          sx = 0;
        } else if (az <= ay) {
          sz = 0;
        } else {
          sy = 0;
        }
      }
      x += sx;
      y += sy;
      z += sz;
      cells.add(new BlockPos(x, y, z));
    }
    return cells;
  }

  /** One planting's growth: the plan, and the room it grows into. */
  private static final class Grower {

    private final TreePreset p;
    private final Random rng;
    private final TreeSpace space;
    final TreePlan plan = new TreePlan();

    Grower(TreePreset p, Random rng, TreeSpace space) {
      this.p = p;
      this.rng = rng;
      this.space = space;
    }

    private int range(int min, int max) {
      return min + rng.nextInt(Math.max(1, max - min + 1));
    }

    // --- what may grow where ---

    /** A log may stand in any open cell: a trunk may be planted hard against a wall. */
    boolean canLog(BlockPos at) {
      Part there = part(at);
      return there == Part.LOG || at.getY() >= 0 && space.free(at);
    }

    /** A limb keeps a block off anything built: no wall beside it and no roof on it. */
    boolean canLimb(BlockPos at) {
      if (!canLog(at)) {
        return false;
      }
      if (space.blocked(at.up())) {
        return false;
      }
      for (EnumFacing f : EnumFacing.HORIZONTALS) {
        if (space.blocked(at.offset(f))) {
          return false;
        }
      }
      return true;
    }

    /** Leaves grow in any open cell above the clearance that is not under a roof. */
    boolean roomForLeaves(BlockPos at) {
      if (!space.free(at)) {
        return false;
      }
      for (int k = 1; k <= ROOF_CLEARANCE; k++) {
        if (space.blocked(at.up(k))) {
          return false;
        }
      }
      return true;
    }

    private enum Part {
      NONE, LOG, OTHER
    }

    private Part part(BlockPos at) {
      TreePlan.Part part = plan.get(at);
      if (part == null) {
        return Part.NONE;
      }
      return part.kind == TreePlan.Kind.LOG ? Part.LOG : Part.OTHER;
    }

    // --- looking round ---

    /**
     * Which way is away from what is built around the spot: a horizontal vector pointing from the
     * walls toward the open, longer the nearer and wider the walls are, and zero in the open.
     * Measured at the heights the tree will fill, out to how far it will reach.
     *
     * @param heights the heights to look at
     * @param reach   how far to look
     *
     * @return {x, z}
     */
    double[] awayFromWalls(int[] heights, int reach) {
      double ax = 0;
      double az = 0;
      int dirs = 16;
      for (int k = 0; k < dirs; k++) {
        double a = 2 * Math.PI * k / dirs;
        double c = Math.cos(a);
        double s = Math.sin(a);
        int open = reach;
        search:
        for (int d = 1; d <= reach; d++) {
          for (int y : heights) {
            if (space.blocked(new BlockPos(Math.round(c * d), y, Math.round(s * d)))) {
              open = d - 1;
              break search;
            }
          }
        }
        double shut = (reach - open) / (double) reach;
        ax -= c * shut;
        az -= s * shut;
      }
      // A wall along one whole side shuts about four of the sixteen directions.
      return new double[]{ax / 4, az / 4};
    }

    /**
     * The heading the tree actually grows toward: the planter's, jittered so a row is not all
     * alike, turned away from walls, which win over the planter.
     *
     * @return {heading, how hard the walls pushed (0 in the open, about 1 against a wall)}
     */
    double[] settle(double heading, double[] away, double jitter) {
      double h = heading + (rng.nextDouble() - 0.5) * 2 * jitter;
      double push = Math.sqrt(away[0] * away[0] + away[1] * away[1]);
      double x = Math.cos(h) + 1.6 * away[0];
      double z = Math.sin(h) + 1.6 * away[1];
      if (x * x + z * z < 1e-4) {
        return new double[]{h, push};
      }
      return new double[]{Math.atan2(z, x), push};
    }

    // --- trunk ---

    /**
     * Grows a trunk from the origin, stepping sideways toward {@code heading} {@code lean} times
     * where {@code stepAt} says so, and going around an obstacle where it can.
     *
     * @return the trunk's cells, bottom up; shorter than {@code height} if something stopped it
     */
    List<BlockPos> trunk(int height, double heading, int lean, Predicate<Integer> stepAt,
        Width width) {
      List<BlockPos> cells = new ArrayList<>();
      BlockPos pos = BlockPos.ORIGIN;
      double c = Math.cos(heading);
      double s = Math.sin(heading);
      int ox = 0;
      int oz = 0;
      for (int y = 0; y < height; y++) {
        if (!canLog(pos)) {
          break;
        }
        plan.log(pos, p.wood, width.at(y), EnumFacing.Axis.Y);
        cells.add(pos);
        if (y == height - 1) {
          break;
        }
        BlockPos next = pos.up();
        if (lean > 0 && stepAt.test(y) || !canLog(next)) {
          // Step along whichever axis keeps the trunk nearer the heading's line.
          int sx = c >= 0 ? 1 : -1;
          int sz = s >= 0 ? 1 : -1;
          boolean xFirst = Math.abs((ox + sx) * s - oz * c) <= Math.abs(ox * s - (oz + sz) * c);
          BlockPos alongX = pos.add(sx, 1, 0);
          BlockPos alongZ = pos.add(0, 1, sz);
          BlockPos first = xFirst ? alongX : alongZ;
          BlockPos second = xFirst ? alongZ : alongX;
          if (Math.abs(c) < 0.2) {
            second = first = alongZ;
          } else if (Math.abs(s) < 0.2) {
            second = first = alongX;
          }
          BlockPos stepped = canLog(first) ? first : canLog(second) ? second : null;
          if (stepped != null && lean > 0) {
            lean--;
            ox += stepped.getX() - pos.getX();
            oz += stepped.getZ() - pos.getZ();
            next = stepped;
          } else if (!canLog(next) && stepped != null) {
            // Blocked straight up with no lean left: go round rather than stop.
            ox += stepped.getX() - pos.getX();
            oz += stepped.getZ() - pos.getZ();
            next = stepped;
          }
        }
        pos = next;
      }
      return cells;
    }

    /** The width of a trunk at a height. */
    private interface Width {

      TreeLogWidth at(int y);
    }

    // --- limbs ---

    /**
     * A limb's path: out {@code reach} along {@code angle} and up {@code rise}, rising early and
     * levelling off (an arch), as cells stepped from the origin.
     */
    List<BlockPos> limbPath(BlockPos origin, double angle, double reach, double rise,
        double curve) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      int samples = (int) Math.ceil(Math.max(reach, Math.abs(rise)) * 2) + 1;
      List<BlockPos> cells = new ArrayList<>();
      BlockPos prev = origin;
      for (int i = 1; i <= samples; i++) {
        double t = i / (double) samples;
        double up = rise * (1 - Math.pow(1 - t, curve));
        BlockPos next = new BlockPos(origin.getX() + Math.round(c * reach * t),
            origin.getY() + Math.round(up), origin.getZ() + Math.round(s * reach * t));
        if (!next.equals(prev)) {
          cells.addAll(walk(prev, next));
          prev = next;
        }
      }
      return cells;
    }

    /** How many of a path's cells a limb can grow into, from the start, before it is stopped. */
    int run(List<BlockPos> path) {
      for (int i = 0; i < path.size(); i++) {
        if (!canLimb(path.get(i))) {
          return i;
        }
      }
      return path.size();
    }

    /** Lays a limb's first {@code n} cells, tapering, and returns its tip (or null if none). */
    BlockPos layLimb(List<BlockPos> path, int n, TreeLogWidth base) {
      BlockPos tip = null;
      for (int i = 0; i < n; i++) {
        double t = (i + 1) / (double) path.size();
        TreeLogWidth w = t < 0.4 ? base : t < 0.8 ? thinner(base) : thinner(thinner(base));
        BlockPos at = path.get(i);
        plan.log(at, p.wood, w, axisOf(path, i));
        tip = at;
      }
      return tip;
    }

    private EnumFacing.Axis axisOf(List<BlockPos> path, int i) {
      BlockPos a = path.get(Math.max(0, i - 1));
      BlockPos b = path.get(Math.min(path.size() - 1, i + 1));
      int dx = Math.abs(b.getX() - a.getX());
      int dy = Math.abs(b.getY() - a.getY());
      int dz = Math.abs(b.getZ() - a.getZ());
      return dy >= Math.max(dx, dz) ? EnumFacing.Axis.Y
          : dx >= dz ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
    }

    /**
     * Grows one limb from {@code origin}, turned as far as it must be (up to about 60 degrees)
     * toward the most room, cut back where it cannot turn, and puts a foliage cluster on its tip.
     * The best direction is the one that runs furthest, less a little for each step turned and a
     * little for crowding a limb already grown.
     */
    void limbWithCluster(BlockPos origin, double angle, double reach, double rise,
        TreeLogWidth width, double clusterScale, List<Double> taken, boolean branch) {
      double curve = 1.4 + rng.nextDouble() * 1.4;
      double best = -1e9;
      List<BlockPos> bestPath = null;
      int bestRun = 0;
      double bestAngle = angle;
      for (double turn : TURNS) {
        double a = angle + turn;
        List<BlockPos> path = limbPath(origin, a, reach, rise, curve);
        if (path.isEmpty()) {
          continue;
        }
        int n = run(path);
        double score = n / (double) path.size() - 0.25 * Math.abs(turn);
        for (double t : taken) {
          double gap = Math.abs(Math.atan2(Math.sin(a - t), Math.cos(a - t)));
          if (gap < 0.3) {
            score -= 0.2;
          }
        }
        if (score > best) {
          best = score;
          bestPath = path;
          bestRun = n;
          bestAngle = a;
        }
        if (n == path.size() && turn == 0) {
          break; // room straight ahead: no need to look round
        }
      }
      if (bestPath == null) {
        return;
      }
      taken.add(bestAngle);
      if (bestRun < bestPath.size()) {
        // The rest of the limb, and roughly its cluster, is what was pruned off.
        plan.trimCount(bestPath.size() - bestRun);
      }
      BlockPos tip = layLimb(bestPath, bestRun, width);
      if (tip == null) {
        return;
      }
      // Cut back hard, the limb keeps a small tuft; otherwise a full cluster. A limb that puts
      // out a side branch shares its foliage with it rather than adding to it.
      boolean branches = branch && bestRun >= 4 && rng.nextDouble() < p.branchChance;
      double kept = bestRun / (double) bestPath.size();
      double scale = clusterScale * (0.55 + 0.45 * kept) * (branches ? 0.85 : 1.0);
      cluster(tip, scale);
      if (branches) {
        // A side branch from a little past halfway, off to one side.
        int from = Math.max(1, (int) Math.round(bestRun * (0.45 + rng.nextDouble() * 0.2)));
        double side = (rng.nextBoolean() ? 1 : -1) * (0.6 + rng.nextDouble() * 0.4);
        limbWithCluster(bestPath.get(from - 1), bestAngle + side, reach * 0.45,
            Math.max(1, rise * 0.4) + 1, thinner(thinner(width)), clusterScale * 0.6, taken,
            false);
      }
    }

    // --- foliage ---

    /** A flattened ellipsoid of foliage centred on {@code centre}. */
    void cluster(BlockPos centre, double scale) {
      double rx = p.clusterRx * scale;
      double ry = Math.max(0.8, p.clusterRy * scale);
      // A little lopsided, so no two clusters are the same ellipsoid.
      double sx = 1 + (rng.nextDouble() - 0.5) * 0.3;
      double sz = 1 + (rng.nextDouble() - 0.5) * 0.3;
      int cx = centre.getX();
      int cy = centre.getY();
      int cz = centre.getZ();
      fill(Collections.singletonList(centre), at -> {
        double x = (at.getX() - cx) / (rx * sx);
        double z = (at.getZ() - cz) / (rx * sz);
        double y = (at.getY() - cy) / ry;
        return x * x + y * y + z * z <= 1 + (rng.nextDouble() - 0.5) * 0.5;
      });
    }

    /**
     * Fills foliage outward from {@code seeds} through every cell {@code inside} accepts, but only
     * through open cells: leaves stop at a wall with a flat face rather than reaching through it
     * or round it into a room. The tree's own logs are passed through; another tree's leaves are
     * met, not entered. {@code inside} is asked once for each cell.
     */
    void fill(List<BlockPos> seeds, Predicate<BlockPos> inside) {
      Set<BlockPos> seen = new HashSet<>(seeds);
      Deque<BlockPos> todo = new ArrayDeque<>(seeds);
      while (!todo.isEmpty()) {
        BlockPos at = todo.poll();
        boolean seed = seeds.contains(at);
        if (!seed && !inside.test(at)) {
          continue;
        }
        if (part(at) != Part.LOG) {
          if (at.getY() < p.clearance || at.getY() < 0) {
            continue;
          }
          if (!roomForLeaves(at)) {
            if (space.at(at) != TreeSpace.Cell.FOLIAGE) {
              plan.trim();
            }
            continue;
          }
          plan.leaves(at, p.leaves);
        }
        for (EnumFacing f : EnumFacing.values()) {
          BlockPos next = at.offset(f);
          if (seen.add(next)) {
            todo.add(next);
          }
        }
      }
    }

    // --- shapes ---

    void limb(double heading) {
      int trunkHeight = range(p.trunkMin, p.trunkMax);
      int lookOut = p.reachMax + (int) Math.ceil(p.clusterRx);
      int[] heights = {Math.max(1, trunkHeight / 2), trunkHeight,
          trunkHeight + p.riseMin, Math.max(p.clearance, trunkHeight) + p.riseMax};
      double[] away = awayFromWalls(heights, Math.min(12, lookOut));
      double[] settled = settle(heading, away, 0.35);
      double h = settled[0];
      // Pushed off a wall, a tree leans a step or two further than it would in the open.
      int lean = Math.min(p.leanMax + 2, range(p.leanMin, p.leanMax)
          + (int) Math.round(Math.min(1, settled[1]) * 1.5));
      double stepChance = 0.5 + rng.nextDouble() * 0.4;
      int trunkHalf = (trunkHeight + 1) / 2;
      List<BlockPos> trunk = trunk(trunkHeight, h, lean,
          y -> y >= 1 && y < trunkHeight - 1 && rng.nextDouble() < stepChance,
          y -> y < trunkHalf ? p.trunkWidth : thinner(p.trunkWidth));
      if (trunk.size() < Math.min(p.trunkMin, Math.max(2, p.clearance - 1))) {
        plan.noRoom();
        return;
      }
      BlockPos top = trunk.get(trunk.size() - 1);

      // Leaders: the top of the trunk, or two where it forks.
      List<BlockPos> leaders = new ArrayList<>();
      List<Double> leaderHeadings = new ArrayList<>();
      if (rng.nextDouble() < p.forkChance) {
        double split = 0.55 + rng.nextDouble() * 0.35;
        for (int side = -1; side <= 1; side += 2) {
          double a = h + side * split;
          List<BlockPos> path = limbPath(top, a, 1 + rng.nextInt(2), 2 + rng.nextInt(2), 1.0);
          int n = run(path);
          BlockPos tip = layLimb(path, n, thinner(p.trunkWidth));
          if (tip != null) {
            leaders.add(tip);
            leaderHeadings.add(a);
          }
        }
      }
      if (leaders.isEmpty()) {
        leaders.add(top);
        leaderHeadings.add(h);
      }

      int limbs = range(p.limbsMin, p.limbsMax);
      List<Double> taken = new ArrayList<>();
      for (int i = 0; i < limbs; i++) {
        boolean back = p.backLimb && i == limbs - 1;
        int leader = i % leaders.size();
        BlockPos origin = leaders.get(leader);
        // Now and then a limb springs from a little lower on the trunk.
        if (leaders.size() == 1 && trunk.size() > 3 && rng.nextDouble() < 0.35) {
          origin = trunk.get(trunk.size() - 2 - rng.nextInt(Math.min(2, trunk.size() - 3)));
        }
        // Spread the limbs evenly across the fan, jittered, facing the lean.
        double a = limbs == 1 ? 0 : -p.spread + 2 * p.spread * i / (limbs - 1);
        a += (rng.nextDouble() - 0.5) * 0.5;
        if (back) {
          a = Math.PI + (rng.nextDouble() - 0.5) * 0.8;
        }
        a += leaderHeadings.size() > 1 ? (leaderHeadings.get(leader) - h) * 0.5 + h : h;
        double reach = range(p.reachMin, p.reachMax) * (back ? 0.5 : 1.0)
            * (0.85 + rng.nextDouble() * 0.3);
        int rise = range(p.riseMin, p.riseMax);
        // Up far enough that the cluster clears the street.
        double need = p.clearance + Math.ceil(p.clusterRy) - origin.getY();
        limbWithCluster(origin, a, reach, Math.max(rise, need), p.limbWidth, 1.0, taken, true);
      }
      // A smaller cluster over each leader closes the crown.
      double closing = leaders.size() > 1 ? 0.6 : 0.8;
      for (BlockPos leader : leaders) {
        cluster(leader.up((int) Math.round(p.clusterRy)), closing);
      }
      if (p.extra != null) {
        hangMoss(top);
      }
    }

    /**
     * Hanging blocks (Spanish moss, willow strands) under some of the crown's lowest leaves, away
     * from the trunk so the trunk stays in view, each down to the preset's longest drop and never
     * lower than two blocks off the ground.
     */
    void hangMoss(BlockPos top) {
      List<BlockPos> undersides = new ArrayList<>();
      for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
        if (e.getValue().kind == TreePlan.Kind.LEAVES && plan.isEmpty(e.getKey().down())) {
          undersides.add(e.getKey());
        }
      }
      for (BlockPos leaf : undersides) {
        boolean nearTrunk = Math.abs(leaf.getX() - top.getX()) <= 1
            && Math.abs(leaf.getZ() - top.getZ()) <= 1;
        if (nearTrunk || rng.nextDouble() > p.hangChance) {
          continue;
        }
        int length = 1 + rng.nextInt(p.hangMax);
        for (int k = 1; k <= length; k++) {
          BlockPos at = leaf.down(k);
          if (at.getY() < 2 || !plan.isEmpty(at) || !space.free(at)) {
            break;
          }
          plan.hanging(at, p.extra);
        }
      }
    }

    void profile() {
      int trunkHeight = range(p.trunkMin, p.trunkMax);
      int height = range(p.heightMin, p.heightMax);
      int crownTop = height - 1;
      // The trunk runs up through the crown to two below its top, thinning as it goes. A one-wide
      // column has no room for one: its leaves stand straight on the trunk.
      int logTop = p.clusterRx < 1 ? trunkHeight - 1 : crownTop - 2;
      List<BlockPos> trunk = trunk(logTop + 1, 0, 0, y -> false,
          y -> y < trunkHeight ? p.trunkWidth : p.limbWidth);
      if (trunk.size() < Math.min(p.trunkMin, logTop + 1)) {
        plan.noRoom();
        return;
      }
      // A column cut off by something overhead stops two above its last log.
      BlockPos last = trunk.get(trunk.size() - 1);
      if (trunk.size() < logTop + 1) {
        plan.trimCount(logTop + 1 - trunk.size());
        crownTop = Math.min(crownTop, last.getY() + 2);
      }
      int top = crownTop;
      int ox = last.getX();
      int oz = last.getZ();
      List<BlockPos> seeds = new ArrayList<>();
      for (BlockPos t : trunk) {
        if (t.getY() >= trunkHeight) {
          seeds.add(t);
        }
      }
      if (seeds.isEmpty()) {
        seeds.add(last);
      }
      fill(seeds, at -> {
        int y = at.getY();
        if (y < trunkHeight || y > top) {
          return false;
        }
        double t = (y - trunkHeight) / (double) Math.max(1, top - trunkHeight);
        // Rounded at the bottom, widest a quarter of the way up, pointed at the top.
        double radius = p.clusterRx * Math.min(1, (t + 0.12) * 3.5) * Math.pow(1 - t, 0.75);
        int x = at.getX() - ox;
        int z = at.getZ() - oz;
        double d = Math.sqrt(x * x + z * z);
        return d <= radius + 0.35 + (rng.nextDouble() - 0.5) * 0.3 || x == 0 && z == 0;
      });
    }

    void palm(double heading) {
      int height = range(p.heightMin, p.heightMax);
      double[] away = awayFromWalls(new int[]{height / 3, 2 * height / 3, height}, 6);
      double[] settled = settle(heading, away, 0.35);
      int leanFor = Math.min(p.leanMax + 2, range(p.leanMin, p.leanMax)
          + (int) Math.round(Math.min(1, settled[1]) * 1.5));
      // The offset grows with the square of the height: upright at the base, curving out.
      int[] offset = {0};
      List<BlockPos> trunk = trunk(height, settled[0], leanFor, y -> {
        double t = (y + 1) / (double) height;
        int want = (int) Math.round(leanFor * Math.min(1, t * t * 1.25));
        if (want > offset[0] && y < height - 1) { // the crown sits straight on the top log
          offset[0]++;
          return true;
        }
        return false;
      }, y -> p.trunkWidth);
      // The crown needs its own cell: shorten the trunk until it has one.
      while (!trunk.isEmpty() && !roomForLeaves(trunk.get(trunk.size() - 1).up())) {
        BlockPos gone = trunk.remove(trunk.size() - 1);
        plan.parts().remove(gone);
        plan.trim();
      }
      if (trunk.size() < Math.max(2, p.heightMin / 2)) {
        plan.noRoom();
        return;
      }
      String crown = p.extra != null && rng.nextBoolean() ? p.extra : p.leaves;
      plan.leaves(trunk.get(trunk.size() - 1).up(), crown);
    }

    void box(EnumFacing facing) {
      int trunkHeight = range(p.trunkMin, p.trunkMax);
      int half = (int) Math.floor(p.clusterRx);
      int height = (int) Math.round(p.clusterRy);
      // Across the facing: the row runs this way.
      int sx = -facing.getZOffset();
      int sz = facing.getXOffset();
      List<BlockPos> trunk = trunk(trunkHeight + height / 2 + 1, 0, 0, y -> false,
          y -> y < trunkHeight ? p.trunkWidth : p.limbWidth);
      if (trunk.size() < trunkHeight + 1) {
        plan.noRoom();
        return;
      }
      List<BlockPos> seeds = new ArrayList<>(trunk.subList(trunkHeight, trunk.size()));
      fill(seeds, at -> {
        int y = at.getY();
        // Two deep: the cell of the trunk and the one behind it, away from the facing.
        int a = at.getX() * sx + at.getZ() * sz;
        int d = -(at.getX() * facing.getXOffset() + at.getZ() * facing.getZOffset());
        return y >= trunkHeight && y < trunkHeight + height && Math.abs(a) <= half
            && d >= 0 && d <= 1;
      });
    }

    void pollard(double heading) {
      int trunkHeight = range(p.trunkMin, p.trunkMax);
      List<BlockPos> trunk = trunk(trunkHeight, 0, 0, y -> false,
          y -> y < trunkHeight - 1 ? p.trunkWidth : p.limbWidth);
      if (trunk.size() < trunkHeight) {
        plan.noRoom();
        return;
      }
      BlockPos top = trunk.get(trunk.size() - 1);
      int knuckles = range(p.limbsMin, p.limbsMax);
      double turn = heading + rng.nextDouble() * Math.PI * 2;
      List<Double> taken = new ArrayList<>();
      for (int i = 0; i < knuckles; i++) {
        double a = turn + i * 2 * Math.PI / knuckles;
        List<BlockPos> path = limbPath(top, a, 1.4, 1 + rng.nextInt(2), 1.0);
        int n = run(path);
        if (n < path.size()) {
          plan.trimCount(path.size() - n);
        }
        BlockPos knuckle = layLimb(path, n, p.limbWidth);
        if (knuckle != null) {
          taken.add(a);
          cluster(knuckle.up(), 1.0);
        }
      }
    }

    /**
     * A tall conifer with its limbs in whorls up the trunk: a clear trunk, then every three or
     * four blocks a whorl of level limbs, each tipped with a flat pad, the limbs shorter and
     * thinner toward the top, and a tuft on the leader. Each whorl is turned from the one below,
     * so the layers do not stack limb over limb.
     */
    void tiered(double heading) {
      int height = range(p.heightMin, p.heightMax);
      int clear = range(p.trunkMin, p.trunkMax);
      int lookOut = Math.min(12, p.reachMax + (int) Math.ceil(p.clusterRx));
      double[] away = awayFromWalls(new int[]{clear, (clear + height) / 2, height - 2}, lookOut);
      double[] settled = settle(heading, away, 0.35);
      double h = settled[0];
      int lean = Math.min(p.leanMax + 1, range(p.leanMin, p.leanMax)
          + (int) Math.round(Math.min(1, settled[1])));
      int half = height / 2;
      List<BlockPos> trunk = trunk(height, h, lean,
          y -> y >= clear && y < height - 2 && rng.nextDouble() < 0.25,
          y -> y < half ? p.trunkWidth : thinner(p.trunkWidth));
      if (trunk.size() < clear + 2) {
        plan.noRoom();
        return;
      }
      BlockPos top = trunk.get(trunk.size() - 1);
      double span = Math.max(1, trunk.size() - 1 - clear);
      int whorl = 0;
      for (int y = clear; y < trunk.size() - 2; y += 2 + rng.nextInt(2)) {
        BlockPos origin = trunk.get(y);
        double t = (y - clear) / span;
        double reach = (p.reachMax - (p.reachMax - p.reachMin) * t)
            * (0.8 + rng.nextDouble() * 0.35);
        int limbs = range(p.limbsMin, p.limbsMax);
        double base = h + whorl * 0.9 + rng.nextDouble() * 0.6;
        List<Double> taken = new ArrayList<>();
        for (int i = 0; i < limbs; i++) {
          double a = base + i * 2 * Math.PI / limbs + (rng.nextDouble() - 0.5) * 0.5;
          limbWithCluster(origin, a, Math.max(1.5, reach), range(p.riseMin, p.riseMax),
              t < 0.5 ? p.limbWidth : thinner(p.limbWidth), 1.0 - 0.35 * t, taken, false);
        }
        whorl++;
      }
      cluster(top.up(), 0.6);
    }

    void head(double heading) {
      int trunkHeight = range(p.trunkMin, p.trunkMax);
      double r = p.clusterRx;
      int centreY = trunkHeight + (int) Math.floor(r);
      // A clipped head is trained upright; only a wall close by moves it over a block.
      double[] away = awayFromWalls(new int[]{trunkHeight, centreY, centreY + 1},
          (int) Math.ceil(r) + 1);
      double[] settled = settle(heading, away, 0);
      int lean = settled[1] > 0.35 ? 1 : 0;
      List<BlockPos> trunk = trunk(centreY + 1, settled[0], lean, y -> y == trunkHeight - 1,
          y -> y < trunkHeight ? p.trunkWidth : p.limbWidth);
      if (trunk.size() < trunkHeight) {
        plan.noRoom();
        return;
      }
      BlockPos centre = trunk.get(trunk.size() - 1);
      if (centre.getY() < centreY) {
        plan.trimCount(centreY - centre.getY());
      }
      int cx = centre.getX();
      int cy = centreY;
      int cz = centre.getZ();
      fill(Collections.singletonList(centre), at -> {
        int x = at.getX() - cx;
        int y = at.getY() - cy;
        int z = at.getZ() - cz;
        return x * x + y * y + z * z <= r * r + 0.6 + (rng.nextDouble() - 0.5) * 0.6;
      });
    }
  }
}
