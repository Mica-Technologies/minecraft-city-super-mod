package com.micatechnologies.minecraft.csm.parks.trees;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What falls when a tree log is broken: every log no longer joined to the ground, and the leaves
 * left too far from any log. Cut a trunk and the tree above comes down; cut a limb and the limb
 * goes with its foliage, and the rest of the tree stays.
 *
 * <p>Logs are joined as the log kit draws them, through faces and edge diagonals. A log holds up
 * the logs joined to it when it stands on something solid that is not part of the tree, or is
 * joined to a vanilla log; nothing else of a tree touching a wall counts, since the generator
 * keeps logs a block off walls anyway. Leaves are kept within {@link #LEAF_REACH} steps, through
 * leaves, of a log, much as vanilla leaves decay, or when they stand straight on a log through
 * leaves, as a one-wide column of cypress does; every tree the planting tool grows keeps all of
 * its leaves that close, so cutting one limb never thins the rest of the crown.</p>
 *
 * <p>The search is bounded ({@link #MAX_LOGS}, {@link #MAX_LEAVES}): a log structure too big to
 * search is taken as held up, and leaves at the edge of the searched box as kept, so a felling
 * can only ever do too little.</p>
 *
 * <p>The searches are pure functions over {@link Cells}, so they are tested without a world.</p>
 *
 * @since 2026.9
 */
public final class TreeFelling {

  /** How many steps through leaves a leaves block may be from a log and still be kept. */
  public static final int LEAF_REACH = 8;
  /** The most logs one search follows before giving up and leaving them standing. */
  static final int MAX_LOGS = 2048;
  /** The tallest column of leaves standing on a log that is kept for that alone. */
  static final int MAX_COLUMN = 24;
  /** How far past the felled logs leaves are looked at: a whole column of them, at least. */
  static final int LEAF_MARGIN = MAX_COLUMN + 2;
  /** The most leaves one search follows. */
  static final int MAX_LEAVES = 16384;
  /** Of the logs felled at once, how many play their break effect (the rest go quietly). */
  private static final int EFFECTS = 24;

  /** What a cell holds, for the searches. */
  public enum Kind {
    /** One of this module's logs: felled with the tree. */
    LOG,
    /** A vanilla log: never felled, and holds up any tree log joined to it. */
    ANCHOR,
    /** One of this module's leaves blocks. */
    LEAVES,
    /** Something solid a trunk can stand on. */
    GROUND,
    /** Anything else: air, plants, a leaves block of another mod. */
    OTHER
  }

  /** A view of the world cell by cell. */
  @FunctionalInterface
  public interface Cells {

    Kind at(BlockPos pos);
  }

  private TreeFelling() {
  }

  /** Face and edge-diagonal offsets: the neighbours a log is drawn joined to. */
  private static final List<BlockPos> JOINS = new ArrayList<>();

  static {
    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          int moved = (x != 0 ? 1 : 0) + (y != 0 ? 1 : 0) + (z != 0 ? 1 : 0);
          if (moved == 1 || moved == 2) {
            JOINS.add(new BlockPos(x, y, z));
          }
        }
      }
    }
  }

  /**
   * The logs that fall once the log at {@code cut} is gone: every log joined to it (through a
   * neighbour) that no longer reaches the ground.
   *
   * @param cells the world, with {@code cut} already cleared
   * @param cut   where the broken log was
   *
   * @return the logs to fell, possibly none
   */
  public static Set<BlockPos> unsupportedLogs(Cells cells, BlockPos cut) {
    Set<BlockPos> falling = new HashSet<>();
    Set<BlockPos> checked = new HashSet<>();
    for (BlockPos d : JOINS) {
      BlockPos start = cut.add(d);
      if (checked.contains(start) || cells.at(start) != Kind.LOG) {
        continue;
      }
      Set<BlockPos> piece = new HashSet<>();
      boolean held = false;
      Deque<BlockPos> todo = new ArrayDeque<>();
      todo.add(start);
      piece.add(start);
      while (!todo.isEmpty()) {
        BlockPos at = todo.poll();
        Kind below = cells.at(at.down());
        if (below == Kind.GROUND || below == Kind.ANCHOR) {
          held = true;
        }
        for (BlockPos j : JOINS) {
          BlockPos next = at.add(j);
          if (piece.contains(next)) {
            continue;
          }
          Kind k = cells.at(next);
          if (k == Kind.ANCHOR) {
            held = true;
          } else if (k == Kind.LOG) {
            piece.add(next);
            todo.add(next);
          }
        }
        if (held || piece.size() > MAX_LOGS) {
          held = true;
          break;
        }
      }
      checked.addAll(piece);
      if (!held) {
        falling.addAll(piece);
      }
    }
    return falling;
  }

  /**
   * The leaves to take away once {@code gone} (logs and leaves already cleared) are gone: those
   * joined, through leaves, to where something went, that are now more than {@link #LEAF_REACH}
   * steps from any log.
   *
   * @param cells the world, with {@code gone} already cleared
   * @param gone  the cells cleared
   *
   * @return the leaves to remove
   */
  public static Set<BlockPos> orphanedLeaves(Cells cells, Set<BlockPos> gone) {
    if (gone.isEmpty()) {
      return new HashSet<>();
    }
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    int maxZ = Integer.MIN_VALUE;
    for (BlockPos p : gone) {
      minX = Math.min(minX, p.getX());
      minY = Math.min(minY, p.getY());
      minZ = Math.min(minZ, p.getZ());
      maxX = Math.max(maxX, p.getX());
      maxY = Math.max(maxY, p.getY());
      maxZ = Math.max(maxZ, p.getZ());
    }
    int[] box = {minX - LEAF_MARGIN, minY - LEAF_MARGIN, minZ - LEAF_MARGIN,
        maxX + LEAF_MARGIN, maxY + LEAF_MARGIN, maxZ + LEAF_MARGIN};

    // Every leaves block joined, through leaves, to where something went, within the box.
    Set<BlockPos> leaves = new HashSet<>();
    Deque<BlockPos> todo = new ArrayDeque<>();
    for (BlockPos p : gone) {
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = p.offset(f);
        if (inside(box, n) && cells.at(n) == Kind.LEAVES && leaves.add(n)) {
          todo.add(n);
        }
      }
    }
    // Held: touching a log, standing straight on one through leaves (a one-wide column, the
    // cypress), or at the edge of the box (what is beyond was not looked at).
    Map<BlockPos, Integer> reach = new HashMap<>();
    Deque<BlockPos> held = new ArrayDeque<>();
    while (!todo.isEmpty()) {
      BlockPos at = todo.poll();
      boolean touching = onEdge(box, at);
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = at.offset(f);
        Kind k = cells.at(n);
        if (k == Kind.LOG || k == Kind.ANCHOR) {
          touching = true;
        } else if (k == Kind.LEAVES && inside(box, n) && leaves.size() < MAX_LEAVES
            && leaves.add(n)) {
          todo.add(n);
        }
      }
      if (touching || standsOnLog(cells, at) || leaves.size() >= MAX_LEAVES) {
        reach.put(at, 1);
        held.add(at);
      }
    }
    // Distance through leaves from the held ones.
    while (!held.isEmpty()) {
      BlockPos at = held.poll();
      int d = reach.get(at);
      if (d >= LEAF_REACH) {
        continue;
      }
      for (EnumFacing f : EnumFacing.values()) {
        BlockPos n = at.offset(f);
        if (leaves.contains(n) && !reach.containsKey(n)) {
          reach.put(n, d + 1);
          held.add(n);
        }
      }
    }
    Set<BlockPos> orphans = new HashSet<>();
    for (BlockPos p : leaves) {
      if (!reach.containsKey(p)) {
        orphans.add(p);
      }
    }
    return orphans;
  }

  /** Whether straight down from {@code at}, through leaves only, there is a log. */
  private static boolean standsOnLog(Cells cells, BlockPos at) {
    BlockPos p = at.down();
    for (int k = 0; k < MAX_COLUMN; k++, p = p.down()) {
      Kind kind = cells.at(p);
      if (kind == Kind.LOG || kind == Kind.ANCHOR) {
        return true;
      }
      if (kind != Kind.LEAVES) {
        return false;
      }
    }
    return false;
  }

  private static boolean inside(int[] box, BlockPos p) {
    return p.getX() >= box[0] && p.getY() >= box[1] && p.getZ() >= box[2]
        && p.getX() <= box[3] && p.getY() <= box[4] && p.getZ() <= box[5];
  }

  private static boolean onEdge(int[] box, BlockPos p) {
    return p.getX() == box[0] || p.getY() == box[1] || p.getZ() == box[2]
        || p.getX() == box[3] || p.getY() == box[4] || p.getZ() == box[5];
  }

  // --- in the world ---

  /** How the world's blocks read to the searches. */
  public static Kind kind(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (block instanceof BlockTreeLog) {
      return Kind.LOG;
    }
    if (TreeLogConnections.logWidthIndex(state) > 0) {
      return Kind.ANCHOR;
    }
    if (block instanceof ICsmTreeLeaves) {
      return Kind.LEAVES;
    }
    if (TreeLogConnections.isLeaves(block) || block.isAir(state, world, pos)
        || !state.getMaterial().blocksMovement()) {
      return Kind.OTHER;
    }
    return Kind.GROUND;
  }

  /**
   * Fells what the log just broken at {@code cut} held up: the logs (dropped as items unless the
   * player is in creative) and then the leaves they leave stranded (dropping nothing, as decaying
   * leaves do not drop themselves). Server side only.
   *
   * @param world  the world
   * @param cut    where the log was
   * @param player who broke it
   *
   * @return the logs felled (not counting the one at {@code cut}), empty on the client
   */
  public static Set<BlockPos> fell(World world, BlockPos cut, EntityPlayer player) {
    if (world.isRemote) {
      return new HashSet<>();
    }
    Cells cells = pos -> world.isBlockLoaded(pos) ? kind(world, pos) : Kind.GROUND;
    Set<BlockPos> logs = unsupportedLogs(cells, cut);
    boolean drops = !player.capabilities.isCreativeMode;
    int effects = 0;
    for (BlockPos p : logs) {
      if (effects++ < EFFECTS) {
        world.destroyBlock(p, drops);
      } else {
        IBlockState state = world.getBlockState(p);
        if (drops) {
          state.getBlock().dropBlockAsItem(world, p, state, 0);
        }
        world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
      }
    }
    Set<BlockPos> gone = new HashSet<>(logs);
    gone.add(cut);
    Cells now = pos -> world.isBlockLoaded(pos) ? kind(world, pos) : Kind.LOG;
    for (BlockPos p : orphanedLeaves(now, gone)) {
      world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
    }
    return logs;
  }
}
