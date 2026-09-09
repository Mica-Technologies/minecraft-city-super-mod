package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * A four-slot time-of-day schedule, and the clock arithmetic the road system reads hours with.
 *
 * <p>Two different controllers need to agree about when the PM peak starts — the signal
 * controller picking a coordination pattern, and the lane control controller picking a lane
 * aspect — so the schedule lives here rather than in either of them. The school zone beacon's
 * window arithmetic moved here for the same reason: there should be one answer to "is this hour
 * inside that window", not one per block.</p>
 *
 * <p>The four slots partition the whole clock rather than each holding a window. Each slot has a
 * start hour, and a slot runs until the next one begins, wrapping past midnight — which is how a
 * real controller's time-of-day table reads, and it makes a gap between patterns impossible to
 * express by accident.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TrafficTimeOfDaySchedule {

  /** The four patterns a small-city controller runs. */
  public static final String[] SLOT_NAMES = {"AM Peak", "Midday", "PM Peak", "Night"};
  public static final int SLOT_COUNT = 4;

  public static final int SLOT_AM_PEAK = 0;
  public static final int SLOT_MIDDAY = 1;
  public static final int SLOT_PM_PEAK = 2;
  public static final int SLOT_NIGHT = 3;

  /** Default start hours: peaks around a working day, night from seven in the evening. */
  private static final int[] DEFAULT_START_HOURS = {6, 9, 15, 19};

  private static final String KEY_START_HOURS = "tods";

  private final int[] startHours = DEFAULT_START_HOURS.clone();

  /**
   * The world's hour of day, 0-23.
   *
   * <p>Minecraft's day starts at 06:00, so tick 0 is 6am and each 1000 ticks is an hour.</p>
   *
   * @param world the world to read
   *
   * @return the hour of day, or {@code -1} with no world
   */
  public static int hourOfDay(World world) {
    if (world == null) {
      return -1;
    }
    return (int) (((world.getWorldTime() / 1000L) + 6L) % 24L);
  }

  /**
   * Whether an hour falls inside a window.
   *
   * <p>Windows are read inclusive of the start hour and exclusive of the end. A window whose end
   * is at or before its start wraps past midnight rather than being empty, so a night-shift
   * window posted 22 to 02 behaves the way it reads. A zero-length window is off rather than all
   * day: both readings are defensible, and off is the safe one, because the alternative is
   * something running around the clock the moment someone equalises the two hours by accident.
   * </p>
   *
   * @param hour  the hour of day, 0-23
   * @param start the window's first hour, inclusive
   * @param end   the window's last hour, exclusive
   *
   * @return {@code true} if the hour is inside the window
   */
  public static boolean inWindow(int hour, int start, int end) {
    if (start == end) {
      return false;
    }
    if (start < end) {
      return hour >= start && hour < end;
    }
    return hour >= start || hour < end;
  }

  /**
   * The start hour of one slot.
   *
   * @param slot the slot index
   *
   * @return the hour that slot begins at, 0-23
   */
  public int getStartHour(int slot) {
    return startHours[clampSlot(slot)];
  }

  /**
   * Sets one slot's start hour, wrapping the value into range the way a stepping GUI needs.
   *
   * @param slot the slot index
   * @param hour the hour to start at; wrapped into 0-23
   */
  public void setStartHour(int slot, int hour) {
    startHours[clampSlot(slot)] = ((hour % 24) + 24) % 24;
  }

  /**
   * Which slot is in force at a given hour.
   *
   * <p>The slot whose start hour is the most recent one at or before this hour, measured the
   * short way around the clock. Two slots sharing a start hour resolve to the earlier index, so
   * the answer is always defined — a schedule with every slot on the same hour runs that one
   * slot all day rather than flickering.</p>
   *
   * @param hour the hour of day, 0-23
   *
   * @return the active slot index
   */
  public int getActiveSlot(int hour) {
    int normalised = ((hour % 24) + 24) % 24;
    int best = 0;
    int bestDistance = Integer.MAX_VALUE;
    for (int slot = 0; slot < SLOT_COUNT; slot++) {
      int distance = ((normalised - startHours[slot]) + 24) % 24;
      if (distance < bestDistance) {
        bestDistance = distance;
        best = slot;
      }
    }
    return best;
  }

  /**
   * Which slot is in force right now.
   *
   * @param world the world to read the clock from
   *
   * @return the active slot index, or {@link #SLOT_AM_PEAK} with no world
   */
  public int getActiveSlot(World world) {
    int hour = hourOfDay(world);
    return hour < 0 ? SLOT_AM_PEAK : getActiveSlot(hour);
  }

  private static int clampSlot(int slot) {
    if (slot < 0) {
      return 0;
    }
    return slot >= SLOT_COUNT ? SLOT_COUNT - 1 : slot;
  }

  /**
   * Reads the schedule from NBT, leaving the defaults in place when the tag is absent.
   *
   * @param compound the compound to read from
   */
  public void readNBT(NBTTagCompound compound) {
    if (compound == null || !compound.hasKey(KEY_START_HOURS)) {
      return;
    }
    int[] stored = compound.getIntArray(KEY_START_HOURS);
    for (int slot = 0; slot < SLOT_COUNT; slot++) {
      // A short or corrupt array keeps the default for the slots it does not cover, rather than
      // throwing during a chunk load.
      if (slot < stored.length) {
        startHours[slot] = ((stored[slot] % 24) + 24) % 24;
      }
    }
  }

  /**
   * Writes the schedule to NBT.
   *
   * @param compound the compound to write to
   *
   * @return the same compound
   */
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setIntArray(KEY_START_HOURS, startHours.clone());
    return compound;
  }

  /**
   * A human-readable summary of when this slot runs, for a configuration GUI.
   *
   * @param slot the slot index
   *
   * @return something like {@code "AM Peak: 6 AM - 9 AM"}
   */
  public String describeSlot(int slot) {
    int index = clampSlot(slot);
    int next = (index + 1) % SLOT_COUNT;
    return SLOT_NAMES[index] + ": " + formatHour(startHours[index])
        + " - " + formatHour(startHours[next]);
  }

  /**
   * Hours read as a 12-hour clock, which is how a time-of-day table is posted.
   *
   * @param hour the hour of day, 0-23
   *
   * @return the formatted hour
   */
  public static String formatHour(int hour) {
    int display = hour % 12;
    if (display == 0) {
      display = 12;
    }
    return display + (hour < 12 ? " AM" : " PM");
  }
}
