package com.micatechnologies.minecraft.csm.lifesafety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The interconnect of residential smoke and CO alarms (#335): when one trips, every alarm on the
 * same home system sounds, as 120 V hard-wired interconnect does in a real home.
 *
 * <h3>Systems</h3>
 * An alarm belongs to at most one system, resolved from its {@link TileEntityFireAlarmSensor}:
 * <ol>
 *   <li><b>A panel</b> (a home keypad, or any fire alarm panel): the alarm is linked to it as an
 *   initiating device. It reports to the panel, and the panel's alarm is what sounds it, together
 *   with every other residential alarm on that panel in a zone that is in alarm. The panel hands
 *   this class its members once a second ({@link #setPanel}).</li>
 *   <li><b>An interconnect group</b>, with no panel: the linker joined it to another alarm. The
 *   group's key is the position of the alarm it was started from; it is only a key, so breaking
 *   that alarm leaves the rest still joined. Members join here as their tile entities load.</li>
 *   <li><b>Neither</b>: the alarm is a placed, unlinked block and does nothing, exactly as the
 *   decorative blocks it replaced did.</li>
 * </ol>
 * An alarm linked to a panel whose panel cannot be reached sounds on its own, as a real alarm
 * always sounds where it detects: a solo group keyed by its own position.
 *
 * <h3>Sounding</h3>
 * A group sounds while a member keeps detecting: each detection holds it up for
 * {@link #CLEAR_TICKS}, a little over a detector's look period, so it falls quiet within about half
 * a minute of the fire going out (residential alarms are self-restoring; nothing latches). A smoke
 * trip outranks a CO trip, as a combination alarm's smoke signal does. Every member plays the
 * group's pattern from one start tick, so the alarms of a house beep together. The test button
 * sounds the group for two cycles; pressed while it sounds, it hushes the group for
 * {@link #HUSH_TICKS}.
 *
 * <p>The state here is the world's and is never saved: a fire still burning trips its alarms
 * again within one look.</p>
 *
 * <p>The instance methods hold the logic and take no world, so they are unit tested; the static
 * methods are the world-keyed facade the blocks and tile entities call, and {@link Events} plays
 * the sounds.</p>
 *
 * @since 2026.11
 */
public final class ResidentialInterconnect {

  /** The two signals a residential alarm sounds. */
  public enum Pattern {
    /** Smoke: the temporal-three evacuation signal (NFPA 72), one cycle every 4 s. */
    SMOKE(LifeSafetySounds.RESIDENTIAL_ALARM_T3, 80),
    /** Carbon monoxide: the temporal-four signal (NFPA 720), one cycle every 5.7 s. */
    CO(LifeSafetySounds.RESIDENTIAL_ALARM_T4, 114);

    private final LifeSafetySounds sound;
    private final int period;

    Pattern(LifeSafetySounds sound, int period) {
      this.sound = sound;
      this.period = period;
    }

    /** The sound one cycle plays. */
    public LifeSafetySounds getSound() {
      return sound;
    }

    /** Ticks from the start of one cycle to the start of the next. */
    public int getPeriod() {
      return period;
    }

    /** The stronger of two signals: smoke over CO, either over none. */
    static Pattern stronger(Pattern a, Pattern b) {
      if (a == null) {
        return b;
      }
      if (b == null) {
        return a;
      }
      return a == SMOKE || b == SMOKE ? SMOKE : CO;
    }
  }

  /** How long one detection keeps a group sounding: a detector's look period and a half. */
  public static final int CLEAR_TICKS = FireAlarmDeviceSampler.DETECTOR_PERIOD + 100;

  /** How long the test button's hush keeps a group quiet: ten minutes, as on a real alarm. */
  public static final int HUSH_TICKS = 12000;

  /** How many cycles a test sounds. */
  public static final int TEST_CYCLES = 2;

  /** One sounding group. */
  private static final class Active {

    Pattern pattern;
    long start;
    long until;
  }

  /** One panel's residential alarms, while the panel sounds them. */
  private static final class PanelGroup {

    Pattern pattern;
    long start;
    List<BlockPos> members;
  }

  private final Map<BlockPos, Set<BlockPos>> groups = new HashMap<>();
  private final Map<BlockPos, Active> active = new HashMap<>();
  private final Map<BlockPos, Long> hushedUntil = new HashMap<>();
  private final Map<BlockPos, PanelGroup> panels = new HashMap<>();

  // ------------------------------------------------------------------------------------------
  // The logic
  // ------------------------------------------------------------------------------------------

  /** Adds an alarm to a group's members. */
  void joinGroup(BlockPos group, BlockPos member) {
    groups.computeIfAbsent(group.toImmutable(), k -> new HashSet<>()).add(member.toImmutable());
  }

  /** Drops an alarm from a group's members; an empty group is forgotten. */
  void leaveGroup(BlockPos group, BlockPos member) {
    Set<BlockPos> set = groups.get(group);
    if (set != null) {
      set.remove(member);
      if (set.isEmpty()) {
        groups.remove(group);
      }
    }
  }

  /**
   * The alarms a group sounds through: its members, or, for a key nothing has joined (an alarm
   * sounding on its own), the key itself.
   */
  List<BlockPos> membersOf(BlockPos group) {
    Set<BlockPos> set = groups.get(group);
    if (set == null || set.isEmpty()) {
      return Collections.singletonList(group);
    }
    return new ArrayList<>(set);
  }

  /**
   * A member detected: the group sounds the pattern, or carries on sounding, for
   * {@link #CLEAR_TICKS} more. Ignored while the group is hushed.
   *
   * @return {@code true} if the group is sounding afterwards
   */
  boolean tripGroup(BlockPos group, Pattern pattern, long now) {
    Long hush = hushedUntil.get(group);
    if (hush != null) {
      if (now < hush) {
        return false;
      }
      hushedUntil.remove(group);
    }
    sound(group, pattern, now, now + CLEAR_TICKS);
    return true;
  }

  /** The test button: the group sounds its pattern for {@link #TEST_CYCLES} cycles. */
  void testGroup(BlockPos group, Pattern pattern, long now) {
    hushedUntil.remove(group);
    sound(group, pattern, now, now + (long) TEST_CYCLES * pattern.getPeriod());
  }

  private void sound(BlockPos group, Pattern pattern, long now, long until) {
    BlockPos key = group.toImmutable();
    Active a = active.get(key);
    if (a == null) {
      a = new Active();
      a.pattern = pattern;
      a.start = now;
      active.put(key, a);
    } else if (Pattern.stronger(a.pattern, pattern) != a.pattern) {
      // CO giving way to smoke: the new signal starts its cycle now
      a.pattern = pattern;
      a.start = now;
    }
    a.until = Math.max(a.until, until);
  }

  /**
   * The test button pressed while the group sounds: it falls quiet and ignores detections for
   * {@link #HUSH_TICKS}.
   *
   * @return {@code true} if it was sounding
   */
  boolean hushGroup(BlockPos group, long now) {
    if (active.remove(group) == null) {
      return false;
    }
    hushedUntil.put(group.toImmutable(), now + HUSH_TICKS);
    return true;
  }

  /** The pattern a group is sounding, or {@code null}. */
  Pattern soundingPattern(BlockPos group) {
    Active a = active.get(group);
    return a == null ? null : a.pattern;
  }

  /**
   * What a panel sounds through its residential alarms: a pattern and its members, or
   * {@code null} to stop. A panel that keeps its pattern keeps its cycle; the members may change
   * under it (a zone joining the alarm) without the beat restarting.
   */
  void setPanelGroup(BlockPos panel, Pattern pattern, List<BlockPos> members, long now) {
    if (pattern == null || members == null || members.isEmpty()) {
      panels.remove(panel);
      return;
    }
    PanelGroup g = panels.get(panel);
    if (g == null) {
      g = new PanelGroup();
      g.start = now;
      panels.put(panel.toImmutable(), g);
    } else if (g.pattern != pattern) {
      g.start = now;
    }
    g.pattern = pattern;
    g.members = new ArrayList<>(members);
  }

  /** Whether a panel is sounding residential alarms. */
  boolean panelSounding(BlockPos panel) {
    return panels.containsKey(panel);
  }

  /**
   * Hands every alarm due to start a cycle this tick to {@code play}, once each, and forgets the
   * groups whose time is up. An alarm on two systems at once (a stale solo group and its panel)
   * plays the stronger signal only.
   */
  void tick(long now, BiConsumer<BlockPos, Pattern> play) {
    Map<BlockPos, Pattern> due = new HashMap<>();
    Iterator<Map.Entry<BlockPos, Active>> it = active.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<BlockPos, Active> e = it.next();
      Active a = e.getValue();
      if (now >= a.until) {
        it.remove();
        continue;
      }
      if ((now - a.start) % a.pattern.getPeriod() == 0) {
        for (BlockPos member : membersOf(e.getKey())) {
          due.merge(member, a.pattern, Pattern::stronger);
        }
      }
    }
    for (PanelGroup g : panels.values()) {
      if ((now - g.start) % g.pattern.getPeriod() == 0) {
        for (BlockPos member : g.members) {
          due.merge(member, g.pattern, Pattern::stronger);
        }
      }
    }
    hushedUntil.values().removeIf(until -> now >= until);
    due.forEach(play);
  }

  /** Whether there is nothing to do: no group sounding and no panel. */
  boolean idle() {
    return active.isEmpty() && panels.isEmpty();
  }

  // ------------------------------------------------------------------------------------------
  // The world facade
  // ------------------------------------------------------------------------------------------

  private static final Map<World, ResidentialInterconnect> WORLDS = new WeakHashMap<>();

  private static ResidentialInterconnect of(World world, boolean create) {
    if (world == null || world.isRemote) {
      return null;
    }
    synchronized (WORLDS) {
      return create ? WORLDS.computeIfAbsent(world, w -> new ResidentialInterconnect())
          : WORLDS.get(world);
    }
  }

  /** An alarm's tile entity loaded, or was linked into a group. Server side only. */
  static void join(World world, BlockPos group, BlockPos member) {
    ResidentialInterconnect r = of(world, true);
    if (r != null) {
      r.joinGroup(group, member);
    }
  }

  /** An alarm's tile entity went away, or left its group. */
  static void leave(World world, BlockPos group, BlockPos member) {
    ResidentialInterconnect r = of(world, false);
    if (r != null) {
      r.leaveGroup(group, member);
    }
  }

  /**
   * An alarm detected: its group sounds.
   *
   * @param group the group's key, or the alarm's own position to sound it alone
   */
  static void trip(World world, BlockPos group, Pattern pattern) {
    ResidentialInterconnect r = of(world, true);
    if (r != null) {
      r.tripGroup(group, pattern, world.getTotalWorldTime());
    }
  }

  /**
   * The test button on an alarm in a group: hushes the group if it is sounding, tests it if not.
   *
   * @return {@code true} if it hushed, {@code false} if it started a test
   */
  static boolean testOrHush(World world, BlockPos group, Pattern pattern) {
    ResidentialInterconnect r = of(world, true);
    if (r == null) {
      return false;
    }
    long now = world.getTotalWorldTime();
    if (r.hushGroup(group, now)) {
      return true;
    }
    r.testGroup(group, pattern, now);
    return false;
  }

  /** How many loaded alarms are on a group, for the linker's report. */
  static int memberCount(World world, BlockPos group) {
    ResidentialInterconnect r = of(world, false);
    Set<BlockPos> set = r == null ? null : r.groups.get(group);
    return set == null ? 0 : set.size();
  }

  /** Whether a group is sounding. */
  static boolean isSounding(World world, BlockPos group) {
    ResidentialInterconnect r = of(world, false);
    return r != null && r.soundingPattern(group) != null;
  }

  /** A panel's residential alarms, once a second from the panel's tick; {@code null} stops. */
  static void setPanel(World world, BlockPos panel, Pattern pattern, List<BlockPos> members) {
    ResidentialInterconnect r = of(world, pattern != null);
    if (r != null) {
      r.setPanelGroup(panel, pattern, members, world.getTotalWorldTime());
    }
  }

  /** Plays the cycles due each server tick and drops a world's state with it. */
  public static final class Events {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
      if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
        return;
      }
      ResidentialInterconnect r = of(event.world, false);
      if (r == null || r.idle()) {
        return;
      }
      World world = event.world;
      r.tick(world.getTotalWorldTime(), (pos, pattern) -> {
        if (!world.isBlockLoaded(pos)) {
          return;
        }
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockResidentialAlarm)) {
          return;
        }
        SoundEvent event1 = pattern.getSound().getSoundEvent();
        if (event1 != null) {
          world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, event1,
              SoundCategory.BLOCKS, BlockResidentialAlarm.VOLUME, 1.0F);
        }
      });
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
      synchronized (WORLDS) {
        WORLDS.remove(event.getWorld());
      }
    }
  }
}
