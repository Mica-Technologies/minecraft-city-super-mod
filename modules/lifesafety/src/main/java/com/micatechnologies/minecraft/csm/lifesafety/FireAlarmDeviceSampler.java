package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.AbstractBlockPanelFollower;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs the fire alarm devices that look at something on a schedule, the detectors (every 25
 * seconds, for fire) and the panel followers (every second, at their panel), from one world tick
 * rather than from a scheduled block update each (#308).
 *
 * <p><b>Why.</b> A device that reschedules its own update keeps one entry in the server's
 * pending-tick set for as long as it exists, and vanilla walks that whole set once for every chunk
 * it saves, so with a city's alarm devices every autosave, {@code /save-all} and backup cost
 * (chunks saved) x (devices). Here a device costs nothing between looks.</p>
 *
 * <p><b>How.</b> A device joins its world's roster when its tile entity loads (placed, pasted or
 * with its chunk) and leaves when the tile entity is invalidated or unloaded, so nothing is saved
 * and an edit that skips {@code onBlockAdded} still runs. Each roster is split into one bucket per
 * tick of its period, a device's bucket fixed by its position, so the looks are spread over the
 * period instead of all landing on one tick, and each device is looked at once a period as
 * before.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class FireAlarmDeviceSampler {

  private static final Logger LOGGER = LogManager.getLogger(FireAlarmDeviceSampler.class);

  /** Ticks between a detector's looks for fire. */
  public static final int DETECTOR_PERIOD = 500;

  /** Ticks between a follower's looks at its panel. */
  public static final int FOLLOWER_PERIOD = 20;

  private static final Map<World, FireAlarmDeviceSampler> WORLDS = new WeakHashMap<>();

  private final Roster detectors = new Roster(DETECTOR_PERIOD);
  private final Roster followers = new Roster(FOLLOWER_PERIOD);

  private FireAlarmDeviceSampler() {
  }

  /**
   * Adds the device at a position to its world's roster, if its block looks on a schedule. Called
   * on the server when its tile entity loads.
   */
  static void add(World world, BlockPos pos) {
    if (world == null || world.isRemote) {
      return;
    }
    Block block = world.getBlockState(pos).getBlock();
    boolean detector = block instanceof AbstractBlockFireAlarmDetector;
    boolean follower = block instanceof AbstractBlockPanelFollower;
    if (!detector && !follower) {
      return;
    }
    FireAlarmDeviceSampler s;
    synchronized (WORLDS) {
      s = WORLDS.computeIfAbsent(world, w -> new FireAlarmDeviceSampler());
    }
    (detector ? s.detectors : s.followers).add(pos.toImmutable());
  }

  /** Drops the device at a position from its world's roster, if it is on it. */
  static void remove(World world, BlockPos pos) {
    if (world == null || world.isRemote) {
      return;
    }
    FireAlarmDeviceSampler s;
    synchronized (WORLDS) {
      s = WORLDS.get(world);
    }
    if (s != null) {
      s.detectors.remove(pos);
      s.followers.remove(pos);
    }
  }

  private void tick(World world) {
    long time = world.getTotalWorldTime();
    for (BlockPos pos : detectors.due(time)) {
      look(world, pos, detectors);
    }
    for (BlockPos pos : followers.due(time)) {
      look(world, pos, followers);
    }
  }

  private static void look(World world, BlockPos pos, Roster roster) {
    if (!world.isBlockLoaded(pos)) {
      roster.remove(pos);
      return;
    }
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    try {
      if (block instanceof AbstractBlockFireAlarmDetector) {
        ((AbstractBlockFireAlarmDetector) block).onTick(world, pos, state);
      } else if (block instanceof AbstractBlockPanelFollower) {
        ((AbstractBlockPanelFollower) block).look(world, pos, state);
      } else {
        // The block changed under its tile entity without the tile entity being told
        roster.remove(pos);
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while a fire alarm device looked at {}", pos, e);
    }
  }

  /** One kind of device, in a bucket per tick of its period. */
  private static final class Roster {

    private final Set<BlockPos>[] buckets;

    @SuppressWarnings("unchecked")
    Roster(int period) {
      buckets = new Set[period];
    }

    private int bucket(BlockPos pos) {
      // The position mixed, so neighbours land in different buckets
      long h = pos.toLong() * 0x9E3779B97F4A7C15L;
      return (int) Math.floorMod(h ^ (h >>> 29), (long) buckets.length);
    }

    void add(BlockPos pos) {
      int b = bucket(pos);
      if (buckets[b] == null) {
        buckets[b] = new HashSet<>();
      }
      buckets[b].add(pos);
    }

    void remove(BlockPos pos) {
      Set<BlockPos> set = buckets[bucket(pos)];
      if (set != null) {
        set.remove(pos);
      }
    }

    /** A copy of this tick's bucket, so a look that adds or removes a device cannot upset it. */
    BlockPos[] due(long time) {
      Set<BlockPos> set = buckets[(int) Math.floorMod(time, (long) buckets.length)];
      return set == null || set.isEmpty() ? NONE : set.toArray(NONE);
    }
  }

  private static final BlockPos[] NONE = new BlockPos[0];

  /** Runs the rosters each server world tick and drops them with their world. */
  public static final class Events {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
      if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
        return;
      }
      FireAlarmDeviceSampler s;
      synchronized (WORLDS) {
        s = WORLDS.get(event.world);
      }
      if (s != null) {
        s.tick(event.world);
      }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      synchronized (WORLDS) {
        WORLDS.remove(event.getWorld());
      }
    }
  }
}
