package com.micatechnologies.minecraft.csm.codeutils.light;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Lights and darkens the footprints of {@link AbstractLightFootprintTileEntity} lights (the
 * stadium light heads): a grid of columns per light, each given one light-up air on its surface.
 *
 * <p><b>Spread over ticks.</b> Every light-up air placed or removed is a block light update over
 * the few thousand cells round it, so a footprint is never done in one go. A light asks for its
 * footprint to be brought in line with its state ({@link #request}); the {@link Ticker} then works
 * through the lights round robin, one column or one placed cell at a time, at most
 * {@link #MAX_CHANGES_PER_TICK} light changes and {@link #BUDGET_NANOS} a server tick. Eight large
 * heads (648 cells) come up in about two seconds.</p>
 *
 * <p><b>Never loads a chunk.</b> A column in an unloaded chunk is skipped (the light's next heal
 * fills it); a placed cell in one is kept on the light's list and removed when the light next
 * asks.</p>
 *
 * <p><b>Ownership.</b> The light-up air stores no owner, so each light keeps the cells it placed.
 * A column that already holds a light-up air (a street light's, or another head's) is left as it
 * is and not taken. When a light gives a cell up it is removed only if nothing else wants it:
 * another loaded, lit footprint whose own column scan meets it takes it over onto its own list,
 * and a lit fixture above it ({@link ICsmLightupAirSource}) keeps it (it simply leaves the light's
 * list). A light broken while some of its cells are in unloaded chunks leaves those behind.</p>
 */
public final class CsmLightFootprints {

  /** Light changes (a light-up air placed or removed) a server tick may make, across all lights. */
  public static final int MAX_CHANGES_PER_TICK = 16;

  /** Time a server tick may spend here, in nanoseconds. */
  public static final long BUDGET_NANOS = 3_000_000L;

  /** Column scans and checks a server tick may make, light changes included. */
  public static final int MAX_STEPS_PER_TICK = 512;

  /** How far apart two footprints' lights may be for one to take over the other's cells. */
  private static final int TAKEOVER_RANGE = 96;

  private static final Map<World, Set<AbstractLightFootprintTileEntity>> LOADED =
      new WeakHashMap<>();
  private static final ArrayDeque<Job> JOBS = new ArrayDeque<>();
  private static final Map<AbstractLightFootprintTileEntity, Job> PENDING =
      new IdentityHashMap<>();

  private static long placedTotal;
  private static long removedTotal;
  private static long busyTicks;
  private static long worstTickNanos;

  private CsmLightFootprints() {
  }

  /**
   * Packs a column for {@link AbstractLightFootprintTileEntity#footprintColumns()}.
   *
   * @param x the column's x
   * @param z the column's z
   *
   * @return the packed column
   */
  public static long column(int x, int z) {
    return ((long) x << 32) | (z & 0xFFFFFFFFL);
  }

  private static int columnX(long c) {
    return (int) (c >> 32);
  }

  private static int columnZ(long c) {
    return (int) c;
  }

  /**
   * Asks for a light's footprint to be brought in line with its state: filled if it is lit,
   * cleared if not. Cheap to call often; a light already waiting is not queued twice.
   *
   * @param light the light
   */
  public static void request(AbstractLightFootprintTileEntity light) {
    if (light.getWorld() == null || light.getWorld().isRemote || light.isInvalid()) {
      return;
    }
    Job job = PENDING.get(light);
    if (job != null) {
      job.stale = true;
      return;
    }
    job = new Job(light.getWorld(), light);
    job.start(light.isFootprintLit());
    PENDING.put(light, job);
    JOBS.addLast(job);
  }

  /**
   * Gives up all of a light's cells because the light is going (call from the block's
   * {@code breakBlock}, before the tile entity is removed). The cells are removed, or taken over,
   * on the ticks that follow.
   *
   * @param light the light
   */
  public static void released(AbstractLightFootprintTileEntity light) {
    untrack(light);
    Job pending = PENDING.remove(light);
    if (pending != null) {
      pending.dead = true;
    }
    if (light.getWorld() == null || light.getWorld().isRemote || !light.hasPlacedLight()) {
      return;
    }
    Job job = new Job(light.getWorld(), null);
    job.items = light.placed().toLongArray();
    job.clearing = true;
    light.placed().clear();
    JOBS.addLast(job);
  }

  static void track(AbstractLightFootprintTileEntity light) {
    LOADED.computeIfAbsent(light.getWorld(),
        w -> Collections.newSetFromMap(new IdentityHashMap<>())).add(light);
  }

  static void untrack(AbstractLightFootprintTileEntity light) {
    if (light.getWorld() != null) {
      Set<AbstractLightFootprintTileEntity> set = LOADED.get(light.getWorld());
      if (set != null) {
        set.remove(light);
      }
    }
  }

  /**
   * How much work is waiting and what has been done, for {@code /csm} and measurements.
   *
   * @return a one-line summary
   */
  public static String stats() {
    int lights = 0;
    for (Set<AbstractLightFootprintTileEntity> set : LOADED.values()) {
      lights += set.size();
    }
    return String.format(
        "footprint lights loaded %d, jobs waiting %d, light-up air placed %d removed %d, "
            + "busy ticks %d, worst tick %.2f ms", lights, JOBS.size(), placedTotal, removedTotal,
        busyTicks, worstTickNanos / 1e6);
  }

  /** Clears the counters {@link #stats()} reports. */
  public static void resetStats() {
    placedTotal = 0;
    removedTotal = 0;
    busyTicks = 0;
    worstTickNanos = 0;
  }

  /** Works through the waiting footprints within the tick's budget. */
  static void work() {
    if (JOBS.isEmpty()) {
      return;
    }
    long start = System.nanoTime();
    long deadline = start + BUDGET_NANOS;
    int changes = 0;
    int steps = 0;
    while (!JOBS.isEmpty() && changes < MAX_CHANGES_PER_TICK && steps < MAX_STEPS_PER_TICK) {
      // A light change costs 1-3 ms of relighting in open air, so look at the clock every step
      if (steps > 0 && System.nanoTime() > deadline) {
        break;
      }
      Job job = JOBS.pollFirst();
      if (job.dead) {
        continue;
      }
      AbstractLightFootprintTileEntity light = job.light;
      if (light != null) {
        if (light.isInvalid() || light.getWorld() != job.world) {
          PENDING.remove(light);
          continue;
        }
        boolean lit = light.isFootprintLit();
        if (job.stale || lit == job.clearing) {
          job.start(lit);
        }
      }
      if (job.index >= job.items.length) {
        if (light != null) {
          PENDING.remove(light);
        }
        continue;
      }
      long item = job.items[job.index++];
      steps++;
      if (job.clearing ? clear(job, item) : fill(job, item)) {
        changes++;
      }
      JOBS.addLast(job);
    }
    long took = System.nanoTime() - start;
    busyTicks++;
    worstTickNanos = Math.max(worstTickNanos, took);
  }

  /** Lights one column if it has a surface and no light-up air yet. */
  private static boolean fill(Job job, long column) {
    AbstractLightFootprintTileEntity light = job.light;
    World world = job.world;
    Block lightupAir = CsmLightupAir.block();
    if (lightupAir == null) {
      return false;
    }
    int x = columnX(column);
    int z = columnZ(column);
    int top = Math.min(255, light.footprintTopY());
    int bottom = Math.max(0, top - light.footprintDepth() + 1);
    BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos(x, top, z);
    if (!world.isBlockLoaded(test)) {
      return false;
    }
    int candidate = -1;
    boolean surface = false;
    for (int y = top; y >= bottom; y--) {
      test.setY(y);
      IBlockState state = world.getBlockState(test);
      if (state.getBlock() == lightupAir) {
        return false;
      }
      if (state.getBlock().isAir(state, world, test)) {
        candidate = y;
      } else {
        surface = true;
        break;
      }
    }
    if (!surface || candidate < 0) {
      return false;
    }
    test.setY(candidate);
    BlockPos at = test.toImmutable();
    world.setBlockState(at, lightupAir.getDefaultState(), 2);
    light.placed().add(at.toLong());
    light.markDirty();
    placedTotal++;
    return true;
  }

  /** Gives up one placed cell: hands it on, leaves it to a fixture, or removes it. */
  private static boolean clear(Job job, long cell) {
    AbstractLightFootprintTileEntity light = job.light;
    World world = job.world;
    BlockPos at = BlockPos.fromLong(cell);
    if (!world.isBlockLoaded(at)) {
      // Kept on a live light's list for its next request; a broken light's is lost
      return false;
    }
    if (light != null) {
      light.placed().remove(cell);
      light.markDirty();
    }
    if (!CsmLightupAir.is(world.getBlockState(at))) {
      return false;
    }
    AbstractLightFootprintTileEntity heir = heir(world, light, at);
    if (heir != null) {
      heir.placed().add(cell);
      heir.markDirty();
      return false;
    }
    if (CsmLightupAir.claimedBySource(world, at)) {
      return false;
    }
    world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
    removedTotal++;
    return true;
  }

  /** Another loaded, lit footprint that would take this cell as its own, or {@code null}. */
  private static AbstractLightFootprintTileEntity heir(World world,
      AbstractLightFootprintTileEntity giver, BlockPos at) {
    Set<AbstractLightFootprintTileEntity> set = LOADED.get(world);
    if (set == null) {
      return null;
    }
    long range = (long) TAKEOVER_RANGE * TAKEOVER_RANGE;
    for (AbstractLightFootprintTileEntity other : set) {
      if (other == giver || other.isInvalid()) {
        continue;
      }
      BlockPos p = other.getPos();
      long dx = p.getX() - at.getX();
      long dz = p.getZ() - at.getZ();
      if (dx * dx + dz * dz <= range && other.wouldClaim(at)) {
        return other;
      }
    }
    return null;
  }

  /** One light's pending work: the columns to fill or the cells to clear. */
  private static final class Job {

    final World world;
    /** The light, or {@code null} for a broken light's leftover cells. */
    final AbstractLightFootprintTileEntity light;
    long[] items = new long[0];
    int index;
    boolean clearing;
    boolean stale;
    boolean dead;

    Job(World world, AbstractLightFootprintTileEntity light) {
      this.world = world;
      this.light = light;
    }

    void start(boolean lit) {
      stale = false;
      clearing = !lit;
      index = 0;
      if (lit) {
        items = light.footprintColumns();
      } else {
        LongSet placed = light.placed();
        items = placed.toLongArray();
      }
    }
  }

  /** Runs the work at the end of every server tick, and forgets a world as it unloads. */
  public static class Ticker {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase == TickEvent.Phase.END) {
        work();
      }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      World world = event.getWorld();
      if (world.isRemote) {
        return;
      }
      LOADED.remove(world);
      Iterator<Job> it = JOBS.iterator();
      while (it.hasNext()) {
        Job job = it.next();
        if (job.world == world) {
          it.remove();
          if (job.light != null) {
            PENDING.remove(job.light);
          }
        }
      }
    }
  }
}
