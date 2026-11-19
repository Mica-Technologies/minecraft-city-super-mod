package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmServerThread;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Spreads a group light switch (every light under a station canopy, every roof light of a bus
 * shelter) over a few server ticks, {@link #LIGHTS_PER_TICK} lights a tick, instead of one.
 *
 * <p>Each light switched is a block light update (and, for a canopy light, its light-up air in
 * the floor too), and nearly all of a switch's cost is vanilla's relighting: switching the 15
 * lights of a demo canopy in one tick made one 86 ms server tick switching on and one 229 ms tick
 * switching off. Here the walk that finds the lights still runs at once, the light that was
 * clicked switches at once, and the rest are queued and switched in the order the walk found them,
 * so the canopy lights as a quick ripple outward from it.</p>
 *
 * <p><b>The queue holds where each light is to end up, not a change.</b> A later switch of the
 * same light replaces its target where it stands in the queue, so an opposite switch overrides a
 * pending one, and the same switch asked again (every light under a powered deck asks for the
 * whole canopy in the same tick) neither queues it twice nor starts it over. A light already as
 * asked costs nothing but the check. Nothing ticks when the queue is empty.</p>
 *
 * <p><b>Never left half switched.</b> A chunk that unloads with lights of it still queued has
 * them switched before it is saved ({@link ChunkEvent.Unload} comes before the save), and the
 * server switches everything left when it stops, before its last save. Only a crash mid-ripple
 * leaves a group half switched, and the next switch of the group sets every light again.</p>
 *
 * <p>Server thread only. {@code /csm renderpass skip lightSwitchAtOnce} switches every light at
 * once again, as before, for measuring the two in one session.</p>
 *
 * @since 2026.11
 */
public final class LightSwitchRipple {

  /** The most lights a server tick switches, per world. */
  public static final int LIGHTS_PER_TICK = 4;

  /** The most queued lights a tick looks at, per world, switched or already as asked. */
  private static final int CHECKS_PER_TICK = 256;

  private static final Map<World, LinkedHashMap<BlockPos, Pending>> QUEUES = new WeakHashMap<>();

  private LightSwitchRipple() {
  }

  /**
   * Switches one light: sets it as asked, with whatever it lights besides.
   */
  @FunctionalInterface
  public interface Setter {

    /**
     * @param world the world
     * @param at    the light's position
     * @param lit   whether it is to be lit
     *
     * @return {@code true} if the light changed, {@code false} if it was already as asked or is
     *     no longer there
     */
    boolean set(World world, BlockPos at, boolean lit);
  }

  /** Where one queued light is to end up, and how to get it there. */
  private static final class Pending {

    boolean lit;
    final Setter setter;

    Pending(boolean lit, Setter setter) {
      this.lit = lit;
      this.setter = setter;
    }
  }

  /**
   * Switches a light now: the one clicked, whose state the next click reads. A switch of it still
   * queued is dropped.
   *
   * @param world  the world
   * @param at     the light's position
   * @param lit    whether it is to be lit
   * @param setter how to switch it
   */
  public static void now(World world, BlockPos at, boolean lit, Setter setter) {
    LinkedHashMap<BlockPos, Pending> queue = QUEUES.get(world);
    if (queue != null) {
      queue.remove(at);
    }
    setter.set(world, at, lit);
  }

  /**
   * Queues a light to be switched on a coming tick, or replaces the target of its switch already
   * queued, keeping its place.
   *
   * @param world  the world
   * @param at     the light's position
   * @param lit    whether it is to be lit
   * @param setter how to switch it
   */
  public static void queue(World world, BlockPos at, boolean lit, Setter setter) {
    if (world.isRemote) {
      return;
    }
    if (CsmRenderToggles.lightSwitchAtOnce) {
      now(world, at, lit, setter);
      return;
    }
    LinkedHashMap<BlockPos, Pending> queue =
        QUEUES.computeIfAbsent(world, w -> new LinkedHashMap<>());
    Pending pending = queue.get(at);
    if (pending == null) {
      queue.put(at.toImmutable(), new Pending(lit, setter));
    } else {
      pending.lit = lit;
    }
  }

  /**
   * How many lights are waiting, in every world, for {@code /csm} and measurements.
   *
   * @return the lights queued
   */
  public static int waiting() {
    int n = 0;
    for (LinkedHashMap<BlockPos, Pending> queue : QUEUES.values()) {
      n += queue.size();
    }
    return n;
  }

  /** Switches up to {@link #LIGHTS_PER_TICK} queued lights in each world. */
  static void work() {
    if (QUEUES.isEmpty()) {
      return;
    }
    // A light switched notifies its neighbours, and a neighbour may queue a switch of its own, so
    // nothing here iterates a map while a light is set: each light is taken off first.
    for (World world : new ArrayList<>(QUEUES.keySet())) {
      int switched = 0;
      int checked = 0;
      while (switched < LIGHTS_PER_TICK && checked < CHECKS_PER_TICK) {
        LinkedHashMap<BlockPos, Pending> queue = QUEUES.get(world);
        if (queue == null || queue.isEmpty()) {
          QUEUES.remove(world);
          break;
        }
        Iterator<Map.Entry<BlockPos, Pending>> it = queue.entrySet().iterator();
        Map.Entry<BlockPos, Pending> next = it.next();
        it.remove();
        checked++;
        BlockPos at = next.getKey();
        // Never loads a chunk: a chunk that unloaded had its lights switched as it went
        if (world != null && world.isBlockLoaded(at)) {
          Pending pending = next.getValue();
          if (pending.setter.set(world, at, pending.lit)) {
            switched++;
          }
        }
      }
      LinkedHashMap<BlockPos, Pending> left = QUEUES.get(world);
      if (left != null && left.isEmpty()) {
        QUEUES.remove(world);
      }
    }
  }

  /** Switches every queued light in a world now, and any its switching queues in turn. */
  static void flush(World world) {
    for (int round = 0; round < 8; round++) {
      LinkedHashMap<BlockPos, Pending> queue = QUEUES.remove(world);
      if (queue == null) {
        return;
      }
      for (Map.Entry<BlockPos, Pending> e : queue.entrySet()) {
        if (world.isBlockLoaded(e.getKey())) {
          e.getValue().setter.set(world, e.getKey(), e.getValue().lit);
        }
      }
    }
  }

  /** Switches every queued light in every world now: the server is stopping. */
  public static void flushAll() {
    for (World world : new ArrayList<>(QUEUES.keySet())) {
      flush(world);
    }
  }

  /** Runs the queue at the end of every server tick, and keeps it whole through unloads. */
  public static class Events {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase == TickEvent.Phase.END) {
        work();
      }
    }

    /** A chunk going: its queued lights are switched now, before it is saved. */
    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
      World world = event.getWorld();
      if (world == null || world.isRemote) {
        return;
      }
      // An async world editor unloads chunks off the server thread, where the queue is not safe to
      // touch and no light may be set (#375). The entries stay for work(), which switches them if
      // the chunk is back and otherwise drops them without loading it.
      if (!CsmServerThread.isServerThread(world)) {
        return;
      }
      LinkedHashMap<BlockPos, Pending> queue = QUEUES.get(world);
      if (queue == null) {
        return;
      }
      Chunk chunk = event.getChunk();
      List<Map.Entry<BlockPos, Pending>> due = new ArrayList<>();
      Iterator<Map.Entry<BlockPos, Pending>> it = queue.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<BlockPos, Pending> e = it.next();
        BlockPos at = e.getKey();
        if ((at.getX() >> 4) == chunk.x && (at.getZ() >> 4) == chunk.z) {
          due.add(e);
          it.remove();
        }
      }
      for (Map.Entry<BlockPos, Pending> e : due) {
        e.getValue().setter.set(world, e.getKey(), e.getValue().lit);
      }
      if (queue.isEmpty()) {
        QUEUES.remove(world);
      }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      if (!event.getWorld().isRemote) {
        QUEUES.remove(event.getWorld());
      }
    }
  }
}
