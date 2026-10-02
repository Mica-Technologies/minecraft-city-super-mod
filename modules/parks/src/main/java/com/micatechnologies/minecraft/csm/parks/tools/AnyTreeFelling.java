package com.micatechnologies.minecraft.csm.parks.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * What the chainsaw fells when it cuts a log that is not one of this module's: a vanilla tree or
 * another mod's. The module's own trees fall by {@code TreeFelling}, which knows how they are
 * built; this is the guess for everything else, so it is deliberately timid.
 *
 * <ul>
 *   <li><b>Logs</b> joined to the cut (through faces, edges and corners, as vanilla branches
 *   step), at or above the cut and within {@link #MAX_SPREAD} blocks of it sideways. Nothing
 *   below the cut is taken, so a stump stays.</li>
 *   <li><b>A tree, not a build.</b> The logs must touch at least {@link #MIN_LEAF_CONTACTS} natural
 *   leaves (a vanilla leaves block a player placed does not count). A log cabin has none, so
 *   cutting a wall log takes only that log. More than {@link #MAX_LOGS} logs is taken as a build
 *   too, and nothing beyond the cut falls.</li>
 *   <li><b>Leaves</b> within {@link #LEAF_REACH} steps, through leaves, of the felled logs, except
 *   those within {@link #KEEP_REACH} of a log that stays (a neighbouring tree's, as vanilla leaves
 *   live within four of a log) or of leaves past the search. At most {@link #MAX_LEAVES}.</li>
 * </ul>
 *
 * <p>Pure over {@link Cells}, so it is tested without a world ({@code AnyTreeFellingTest}).</p>
 *
 * @since 2026.10
 */
public final class AnyTreeFelling {

  /** The most logs felled at once; a bigger structure is left standing. */
  public static final int MAX_LOGS = 512;
  /** The most leaves taken with them. */
  public static final int MAX_LEAVES = 2048;
  /** How far sideways from the cut a felled log may be. */
  public static final int MAX_SPREAD = 12;
  /** How many steps through leaves from a felled log a leaves block is taken. */
  public static final int LEAF_REACH = 8;
  /** How many steps through leaves from a log that stays a leaves block is kept. */
  public static final int KEEP_REACH = 4;
  /** How many natural leaves the logs must touch to be a tree. */
  public static final int MIN_LEAF_CONTACTS = 2;

  /** What a cell holds, for the search. */
  public enum Kind {
    /** A log of any mod (not this module's own, which never reach this search). */
    LOG,
    /** A natural leaves block: one that decays. */
    LEAVES,
    /** Anything else, including leaves a player placed. */
    OTHER
  }

  /** A view of the world cell by cell. */
  @FunctionalInterface
  public interface Cells {

    Kind at(BlockPos pos);
  }

  /** What falls. */
  public static final class Result {

    public final Set<BlockPos> logs;
    public final Set<BlockPos> leaves;

    Result(Set<BlockPos> logs, Set<BlockPos> leaves) {
      this.logs = logs;
      this.leaves = leaves;
    }

    public boolean isEmpty() {
      return logs.isEmpty() && leaves.isEmpty();
    }
  }

  private static final Result NOTHING = new Result(Collections.emptySet(),
      Collections.emptySet());

  /** Every offset to a cell sharing a face, an edge or a corner. */
  private static final List<BlockPos> AROUND = new ArrayList<>();

  static {
    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x != 0 || y != 0 || z != 0) {
            AROUND.add(new BlockPos(x, y, z));
          }
        }
      }
    }
  }

  private AnyTreeFelling() {
  }

  /**
   * The logs and leaves that fall once the log at {@code cut} is gone.
   *
   * @param cells the world, with {@code cut} already cleared
   * @param cut   where the cut log was
   *
   * @return what falls; empty when it is not a tree, or too big to be sure
   */
  public static Result fell(Cells cells, BlockPos cut) {
    Set<BlockPos> logs = new HashSet<>();
    Deque<BlockPos> todo = new ArrayDeque<>();
    todo.add(cut);
    while (!todo.isEmpty()) {
      BlockPos at = todo.poll();
      for (BlockPos d : AROUND) {
        BlockPos n = at.add(d);
        if (n.getY() < cut.getY() || Math.abs(n.getX() - cut.getX()) > MAX_SPREAD
            || Math.abs(n.getZ() - cut.getZ()) > MAX_SPREAD || n.equals(cut)
            || logs.contains(n) || cells.at(n) != Kind.LOG) {
          continue;
        }
        logs.add(n);
        if (logs.size() > MAX_LOGS) {
          return NOTHING;
        }
        todo.add(n);
      }
    }

    // A tree: its logs (or the cut itself, a lone trunk block under a crown) touch leaves.
    Set<BlockPos> felled = new HashSet<>(logs);
    felled.add(cut);
    Map<BlockPos, Integer> depth = new HashMap<>();
    Deque<BlockPos> leafTodo = new ArrayDeque<>();
    for (BlockPos p : felled) {
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = p.offset(f);
        if (!depth.containsKey(n) && cells.at(n) == Kind.LEAVES) {
          depth.put(n, 1);
          leafTodo.add(n);
        }
      }
    }
    if (depth.size() < MIN_LEAF_CONTACTS) {
      return NOTHING;
    }
    // The leaves joined, through leaves, to the felled logs, within reach.
    while (!leafTodo.isEmpty() && depth.size() < MAX_LEAVES) {
      BlockPos at = leafTodo.poll();
      int d = depth.get(at);
      if (d >= LEAF_REACH) {
        continue;
      }
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = at.offset(f);
        if (!depth.containsKey(n) && cells.at(n) == Kind.LEAVES) {
          depth.put(n, d + 1);
          leafTodo.add(n);
        }
      }
    }
    // Kept: within KEEP_REACH, through leaves, of a log that stays (a neighbouring tree's, as
    // vanilla leaves stay within four of a log) or of leaves past the search (not looked at, so
    // taken as held: the felling can only do too little).
    Map<BlockPos, Integer> keep = new HashMap<>();
    Deque<BlockPos> held = new ArrayDeque<>();
    for (BlockPos p : depth.keySet()) {
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = p.offset(f);
        Kind k = cells.at(n);
        if (k == Kind.LOG && !felled.contains(n)
            || k == Kind.LEAVES && !depth.containsKey(n)) {
          keep.put(p, 1);
          held.add(p);
          break;
        }
      }
    }
    while (!held.isEmpty()) {
      BlockPos at = held.poll();
      int d = keep.get(at);
      if (d >= KEEP_REACH) {
        continue;
      }
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = at.offset(f);
        if (depth.containsKey(n) && !keep.containsKey(n)) {
          keep.put(n, d + 1);
          held.add(n);
        }
      }
    }
    Set<BlockPos> leaves = new HashSet<>();
    for (BlockPos p : depth.keySet()) {
      if (!keep.containsKey(p)) {
        leaves.add(p);
      }
    }
    return new Result(logs, leaves);
  }
}
