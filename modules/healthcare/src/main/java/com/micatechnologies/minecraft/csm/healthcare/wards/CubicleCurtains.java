package com.micatechnologies.minecraft.csm.healthcare.wards;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockCurtainPanel.Drawn;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockCurtainPanel.Pattern;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a cubicle curtain does as a whole: filling the columns under a track run, opening and
 * closing the whole curtain, changing its cloth. Every operation is a bounded walk done once, on
 * the event that asks for it; nothing ticks.
 *
 * @since 2026.10
 */
public final class CubicleCurtains {

  /** The longest drop a fill hangs. */
  public static final int MAX_DROP = 15;
  /** How many track cells one run walk visits at most. */
  private static final int RUN_LIMIT = 512;
  /** How many panels one curtain walk visits at most. */
  private static final int CURTAIN_LIMIT = 8192;

  /** A panel's state flags: send to clients, no neighbour updates (nothing reads them). */
  private static final int FLAGS = 2;

  private CubicleCurtains() {
  }

  /**
   * The registered track block.
   *
   * @return the track, or null before registration
   */
  @Nullable
  public static Block trackBlock() {
    return CsmRegistry.getBlock("cubicle_curtain_track");
  }

  private static boolean isTrack(World world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockCurtainTrack;
  }

  private static boolean free(World world, BlockPos pos) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockCurtainPanel || s.getBlock().isReplaceable(world, pos);
  }

  /**
   * The drop "auto" gives under a track: down to one block above the floor (the cell on the
   * floor stays clear), at least one panel.
   *
   * @param world the world
   * @param track the track
   *
   * @return the drop
   */
  public static int autoDrop(World world, BlockPos track) {
    int n = 0;
    BlockPos p = track.down();
    while (n < MAX_DROP + 1 && p.getY() >= 0 && free(world, p)) {
      n++;
      p = p.down();
    }
    return Math.max(1, Math.min(MAX_DROP, n - 1));
  }

  /**
   * Takes down the panels hanging under a track.
   *
   * @param world the world
   * @param track the track
   */
  public static void clearColumn(World world, BlockPos track) {
    BlockPos p = track.down();
    for (int i = 0; i < MAX_DROP + 1 && BlockCurtainPanel.isPanel(world, p); i++, p = p.down()) {
      world.setBlockState(p, net.minecraft.init.Blocks.AIR.getDefaultState(), FLAGS);
    }
  }

  /**
   * Hangs a column of {@code drop} panels under a track, replacing any there, stopping early at
   * anything in the way.
   *
   * @return how many panels were hung
   */
  private static int hangColumn(World world, BlockPos track, int drop, Pattern pattern,
      Drawn drawn) {
    clearColumn(world, track);
    BlockCurtainPanel panel = BlockCurtainPanel.get();
    IBlockState state = panel.getDefaultState().withProperty(BlockCurtainPanel.PATTERN, pattern)
        .withProperty(BlockCurtainPanel.DRAWN, drawn);
    int hung = 0;
    BlockPos p = track.down();
    for (int i = 0; i < drop && p.getY() >= 0 && free(world, p); i++, p = p.down()) {
      world.setBlockState(p, state, FLAGS);
      hung++;
    }
    return hung;
  }

  /**
   * Fills the column under a track placed by hand: like the column under a track beside it if
   * there is one (its drop, cloth and whether it is drawn), or down to one block above the floor
   * in mint, closed.
   *
   * @param world the world
   * @param track the track just placed
   */
  public static void fillColumnLikeNeighbours(World world, BlockPos track) {
    for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
      BlockPos n = track.offset(d);
      IBlockState top = world.getBlockState(n.down());
      if (isTrack(world, n) && top.getBlock() instanceof BlockCurtainPanel) {
        int drop = 0;
        while (drop < MAX_DROP && BlockCurtainPanel.isPanel(world, n.down(drop + 1))) {
          drop++;
        }
        Drawn drawn = top.getValue(BlockCurtainPanel.DRAWN) == Drawn.CLOSED ? Drawn.CLOSED
            : Drawn.OPEN;
        hangColumn(world, track, drop, top.getValue(BlockCurtainPanel.PATTERN), drawn);
        return;
      }
    }
    hangColumn(world, track, autoDrop(world, track), Pattern.MINT, Drawn.CLOSED);
  }

  /**
   * The track cells joined to {@code start}, itself included.
   *
   * @param world the world
   * @param start a track
   *
   * @return the run, in walk order
   */
  public static List<BlockPos> trackRun(World world, BlockPos start) {
    List<BlockPos> run = new ArrayList<>();
    Set<BlockPos> seen = new LinkedHashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(start);
    seen.add(start);
    while (!queue.isEmpty() && run.size() < RUN_LIMIT) {
      BlockPos p = queue.poll();
      run.add(p);
      for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
        BlockPos q = p.offset(d);
        if (!seen.contains(q) && world.isBlockLoaded(q) && isTrack(world, q)) {
          seen.add(q);
          queue.add(q);
        }
      }
    }
    return run;
  }

  /**
   * Fills every column of the track run at {@code track}, replacing the curtain there.
   *
   * @param world   the world
   * @param track   a track cell of the run
   * @param drop    the drop, or null for each column's own "auto" drop
   * @param pattern the cloth
   *
   * @return how many panels were hung
   */
  public static int fillRun(World world, BlockPos track, @Nullable Integer drop,
      Pattern pattern) {
    int hung = 0;
    for (BlockPos t : trackRun(world, track)) {
      int d = drop == null ? autoDrop(world, t) : Math.max(1, Math.min(MAX_DROP, drop));
      hung += hangColumn(world, t, d, pattern, Drawn.CLOSED);
    }
    return hung;
  }

  /**
   * Takes down the curtain under every track of a run.
   *
   * @param world the world
   * @param track a track cell of the run
   */
  public static void clearRun(World world, BlockPos track) {
    for (BlockPos t : trackRun(world, track)) {
      clearColumn(world, t);
    }
  }

  /** Every panel of the curtain {@code start} belongs to, grouped by column (x, z). */
  private static Map<Long, List<BlockPos>> curtain(World world, BlockPos start) {
    Map<Long, List<BlockPos>> columns = new LinkedHashMap<>();
    Set<BlockPos> seen = new LinkedHashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(start);
    seen.add(start);
    int n = 0;
    while (!queue.isEmpty() && n < CURTAIN_LIMIT) {
      BlockPos p = queue.poll();
      n++;
      columns.computeIfAbsent(column(p), k -> new ArrayList<>()).add(p);
      for (EnumFacing d : EnumFacing.values()) {
        BlockPos q = p.offset(d);
        if (!seen.contains(q) && world.isBlockLoaded(q) && BlockCurtainPanel.isPanel(world, q)) {
          seen.add(q);
          queue.add(q);
        }
      }
    }
    return columns;
  }

  private static long column(BlockPos p) {
    return ((long) p.getX() << 32) ^ (p.getZ() & 0xFFFFFFFFL);
  }

  private static long column(int x, int z) {
    return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
  }

  /**
   * Opens the curtain {@code panel} belongs to if it is closed there, or closes it. Opening
   * gathers the cloth at the end of the run nearest the click and empties the rest.
   *
   * @param world the world
   * @param panel a panel of the curtain
   */
  public static void toggle(World world, BlockPos panel) {
    IBlockState s = world.getBlockState(panel);
    if (!(s.getBlock() instanceof BlockCurtainPanel)) {
      return;
    }
    setOpen(world, panel, s.getValue(BlockCurtainPanel.DRAWN) == Drawn.CLOSED);
  }

  /**
   * Opens or closes the curtain {@code panel} belongs to.
   *
   * @param world the world
   * @param panel a panel of the curtain
   * @param open  true to open it (bunched at the end nearest {@code panel})
   */
  public static void setOpen(World world, BlockPos panel, boolean open) {
    Map<Long, List<BlockPos>> columns = curtain(world, panel);
    long end = open ? nearestEnd(columns, panel) : Long.MIN_VALUE;
    for (Map.Entry<Long, List<BlockPos>> c : columns.entrySet()) {
      Drawn drawn = !open ? Drawn.CLOSED : c.getKey() == end ? Drawn.BUNCHED : Drawn.OPEN;
      for (BlockPos p : c.getValue()) {
        IBlockState s = world.getBlockState(p);
        if (s.getValue(BlockCurtainPanel.DRAWN) != drawn) {
          world.setBlockState(p, s.withProperty(BlockCurtainPanel.DRAWN, drawn), FLAGS);
        }
      }
    }
  }

  /** The end of the curtain (a column with at most one beside it) nearest {@code from}. */
  private static long nearestEnd(Map<Long, List<BlockPos>> columns, BlockPos from) {
    Map<Long, Integer> dist = new HashMap<>();
    Deque<int[]> queue = new ArrayDeque<>();
    long start = column(from);
    dist.put(start, 0);
    queue.add(new int[]{from.getX(), from.getZ()});
    long best = start;
    int bestDist = Integer.MAX_VALUE;
    while (!queue.isEmpty()) {
      int[] c = queue.poll();
      long key = column(c[0], c[1]);
      int d = dist.get(key);
      int beside = 0;
      for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
        int nx = c[0] + f.getXOffset();
        int nz = c[1] + f.getZOffset();
        long nk = column(nx, nz);
        if (columns.containsKey(nk)) {
          beside++;
          if (!dist.containsKey(nk)) {
            dist.put(nk, d + 1);
            queue.add(new int[]{nx, nz});
          }
        }
      }
      if (beside <= 1 && d < bestDist) {
        best = key;
        bestDist = d;
      }
    }
    return best;
  }

  /**
   * Gives the whole curtain {@code panel} belongs to the next cloth.
   *
   * @param world the world
   * @param panel a panel of the curtain
   */
  public static void cyclePattern(World world, BlockPos panel) {
    IBlockState s = world.getBlockState(panel);
    if (!(s.getBlock() instanceof BlockCurtainPanel)) {
      return;
    }
    Pattern[] all = Pattern.values();
    setPattern(world, panel, all[(s.getValue(BlockCurtainPanel.PATTERN).ordinal() + 1)
        % all.length]);
  }

  /**
   * Gives the whole curtain {@code panel} belongs to a cloth.
   *
   * @param world   the world
   * @param panel   a panel of the curtain
   * @param pattern the cloth
   */
  public static void setPattern(World world, BlockPos panel, Pattern pattern) {
    for (List<BlockPos> c : curtain(world, panel).values()) {
      for (BlockPos p : c) {
        IBlockState s = world.getBlockState(p);
        if (s.getValue(BlockCurtainPanel.PATTERN) != pattern) {
          world.setBlockState(p, s.withProperty(BlockCurtainPanel.PATTERN, pattern), FLAGS);
        }
      }
    }
  }
}
