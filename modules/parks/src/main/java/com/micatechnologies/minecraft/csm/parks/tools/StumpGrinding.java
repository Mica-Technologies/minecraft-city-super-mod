package com.micatechnologies.minecraft.csm.parks.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.math.BlockPos;

/**
 * What the stump grinder takes: the stump a felling left and its roots, and nothing that still
 * stands.
 *
 * <ul>
 *   <li><b>A stump</b> is a log with no log over it: not straight above, and not on the eight
 *   diagonals above either, since logs join through edges and corners (as {@link AnyTreeFelling}
 *   and the tree kit join them) and a leaning trunk steps that way. A log with any of those is a
 *   standing tree, refused.</li>
 *   <li><b>The roots</b> are the logs joined to the stump through faces, edges and corners, at or
 *   below the stump's level, within {@link #REACH} blocks of it sideways and {@link #DEPTH}
 *   below. Nothing above the stump's level is ever taken.</li>
 *   <li><b>Nothing standing.</b> A log over which any other log stands, one that is not taken
 *   itself, stays, and with it whatever was only joined to the stump through it. So a
 *   neighbouring tree's trunk beside the stump is never undercut.</li>
 *   <li><b>In the ground.</b> A stump stands in natural ground: going down its column, within
 *   {@link #MAX_HEIGHT} logs, there is soil, or a log with soil beside it, and that is ground
 *   level. A log standing on anything else (a foundation, a floor, the air) is no stump. Above
 *   ground only the stump's own column is taken; roots spread sideways only at and below ground
 *   level. That is what keeps a log wall from being ground away: its top logs have nothing over
 *   them either, but at most the one column clicked goes, and only if it stands on soil.</li>
 * </ul>
 *
 * <p>Only logs: the {@link Cells} view decides what a log is ({@link AnyTrees#isLog}, which
 * counts this module's logs). Pure, so it is tested without a world ({@code StumpGrindingTest}).
 * </p>
 *
 * @since 2026.10
 */
public final class StumpGrinding {

  /** How far sideways from the stump a root may be. */
  public static final int REACH = 3;
  /** How far below the stump a root may be. */
  public static final int DEPTH = 4;
  /** How many logs a stump may stand above the ground. */
  public static final int MAX_HEIGHT = 3;
  /** {@link #groundLevel} when the stump does not stand in the ground. */
  public static final int NOT_GROUNDED = Integer.MIN_VALUE;

  /** The four cells beside a cell. */
  private static final BlockPos[] BESIDE = {new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
      new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};

  /** A view of the world cell by cell. */
  @FunctionalInterface
  public interface Cells {

    /** Whether the cell holds a log. */
    boolean isLog(BlockPos pos);
  }

  /** Every offset to a cell sharing a face, an edge or a corner. */
  private static final List<BlockPos> AROUND = new ArrayList<>();
  /** The nine cells over a cell. */
  private static final List<BlockPos> OVER = new ArrayList<>();

  static {
    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x != 0 || y != 0 || z != 0) {
            AROUND.add(new BlockPos(x, y, z));
          }
          if (y == 1) {
            OVER.add(new BlockPos(x, y, z));
          }
        }
      }
    }
  }

  private StumpGrinding() {
  }

  /** Whether the log at {@code pos} has a log over it: straight above or diagonally above. */
  public static boolean isStanding(Cells cells, BlockPos pos) {
    for (BlockPos d : OVER) {
      if (cells.isLog(pos.add(d))) {
        return true;
      }
    }
    return false;
  }

  /** Whether {@code pos} is a stump: a log with no log over it. */
  public static boolean isStump(Cells cells, BlockPos pos) {
    return cells.isLog(pos) && !isStanding(cells, pos);
  }

  /**
   * The ground level under a stump: going down its column from the stump, within
   * {@link #MAX_HEIGHT} logs, the first cell that is soil, or that is a log with soil beside it.
   *
   * @param cells the logs
   * @param soil  the natural ground (dirt, grass and the like)
   * @param stump the stump
   *
   * @return the ground level's y, or {@link #NOT_GROUNDED}
   */
  public static int groundLevel(Cells cells, Cells soil, BlockPos stump) {
    for (int k = 0; k <= MAX_HEIGHT; k++) {
      BlockPos at = stump.down(k);
      if (soil.isLog(at)) {
        return at.getY();
      }
      if (!cells.isLog(at)) {
        return NOT_GROUNDED;
      }
      for (BlockPos d : BESIDE) {
        if (soil.isLog(at.add(d))) {
          return at.getY();
        }
      }
    }
    return NOT_GROUNDED;
  }

  /**
   * The logs a grind takes: the stump at {@code stump} and its roots, the stump first.
   *
   * @param cells the logs
   * @param soil  the natural ground (dirt, grass and the like); {@link Cells#isLog} answers
   *              whether a cell is soil
   * @param stump the stump
   *
   * @return empty when {@code stump} is not a stump standing in the ground
   */
  public static Set<BlockPos> grind(Cells cells, Cells soil, BlockPos stump) {
    if (!isStump(cells, stump)) {
      return new LinkedHashSet<>();
    }
    int ground = groundLevel(cells, soil, stump);
    if (ground == NOT_GROUNDED) {
      return new LinkedHashSet<>();
    }
    // Every log joined to the stump within the box: its own column above ground, and anything
    // at or below ground level.
    Set<BlockPos> found = reach(cells, stump, ground, null);
    // Leave any log something else still stands on, until nothing changes.
    boolean changed = true;
    while (changed) {
      changed = false;
      for (BlockPos p : new ArrayList<>(found)) {
        if (p.equals(stump)) {
          continue;
        }
        for (BlockPos d : OVER) {
          BlockPos over = p.add(d);
          if (!found.contains(over) && cells.isLog(over)) {
            found.remove(p);
            changed = true;
            break;
          }
        }
      }
    }
    // What is still joined to the stump through what is left.
    return reach(cells, stump, ground, found);
  }

  /** The logs joined to the stump inside the box, through {@code allowed} only if given. */
  private static Set<BlockPos> reach(Cells cells, BlockPos stump, int ground,
      Set<BlockPos> allowed) {
    Set<BlockPos> found = new LinkedHashSet<>();
    found.add(stump);
    Deque<BlockPos> todo = new ArrayDeque<>();
    todo.add(stump);
    Set<BlockPos> seen = new HashSet<>(found);
    while (!todo.isEmpty()) {
      BlockPos at = todo.poll();
      for (BlockPos d : AROUND) {
        BlockPos n = at.add(d);
        if (!seen.add(n) || !inBox(stump, ground, n)) {
          continue;
        }
        if (allowed != null ? allowed.contains(n) : cells.isLog(n)) {
          found.add(n);
          todo.add(n);
        }
      }
    }
    return found;
  }

  private static boolean inBox(BlockPos stump, int ground, BlockPos p) {
    if (p.getY() > ground) {
      // above ground: the stump's own column only
      return p.getX() == stump.getX() && p.getZ() == stump.getZ() && p.getY() <= stump.getY();
    }
    return p.getY() >= stump.getY() - DEPTH
        && Math.abs(p.getX() - stump.getX()) <= REACH
        && Math.abs(p.getZ() - stump.getZ()) <= REACH;
  }
}
