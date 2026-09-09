package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * One group of lane control signals and the aspect each of them shows in each time-of-day slot.
 *
 * <p>A reversible lane is a group: the centre lanes of an arterial run inbound in the AM pattern
 * and outbound in the PM, and every signal over those lanes changes together. That is why the
 * unit of configuration is a group rather than a signal — half a lane changing direction is not
 * a state anyone wants to be able to express.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class LaneControlGroup {

  /**
   * Default clearance: three seconds of YELLOW X before a lane closes. Short by real standards,
   * where a reversal is announced minutes ahead, but this is the interval a driver in the lane
   * actually sees, and three seconds reads as a warning rather than a glitch.
   */
  public static final long DEFAULT_CLEARANCE = 60L;

  private static final String K_SIGNALS = "sg";
  private static final String K_ASPECTS = "as";
  private static final String K_CLEARANCE = "cl";

  private final List<BlockPos> signals = new ArrayList<>();
  private final LaneControlSignalType[] aspects =
      new LaneControlSignalType[TrafficTimeOfDaySchedule.SLOT_COUNT];
  private long clearanceTicks = DEFAULT_CLEARANCE;

  public LaneControlGroup() {
    for (int slot = 0; slot < aspects.length; slot++) {
      aspects[slot] = LaneControlSignalType.OFF;
    }
  }

  public List<BlockPos> getSignals() {
    return signals;
  }

  /**
   * Adds a signal to this group.
   *
   * @param pos the signal's position
   *
   * @return {@code true} if it was added, {@code false} if the group already had it
   */
  public boolean addSignal(BlockPos pos) {
    if (pos == null || signals.contains(pos)) {
      return false;
    }
    return signals.add(pos);
  }

  public boolean removeSignal(BlockPos pos) {
    return signals.remove(pos);
  }

  /**
   * The aspect this group shows in one time-of-day slot.
   *
   * @param slot the slot index; clamped, because it arrives from NBT and from packets
   *
   * @return the aspect
   */
  public LaneControlSignalType getAspect(int slot) {
    return aspects[clampSlot(slot)];
  }

  public void setAspect(int slot, LaneControlSignalType aspect) {
    aspects[clampSlot(slot)] = aspect == null ? LaneControlSignalType.OFF : aspect;
  }

  public void cycleAspect(int slot) {
    int index = clampSlot(slot);
    aspects[index] = aspects[index].getNextType();
  }

  public long getClearanceTicks() {
    return clearanceTicks;
  }

  public void setClearanceTicks(long clearanceTicks) {
    this.clearanceTicks = Math.max(0L, clearanceTicks);
  }

  private static int clampSlot(int slot) {
    if (slot < 0) {
      return 0;
    }
    return slot >= TrafficTimeOfDaySchedule.SLOT_COUNT
        ? TrafficTimeOfDaySchedule.SLOT_COUNT - 1
        : slot;
  }

  /**
   * Whether an aspect leaves the lane open to traffic.
   *
   * <p>This is what decides whether a change needs a clearance interval: closing an open lane
   * has to warn the drivers already in it, while opening a closed one has nobody to warn.</p>
   *
   * @param aspect the aspect to test
   *
   * @return {@code true} if traffic may use the lane
   */
  public static boolean isOpen(LaneControlSignalType aspect) {
    return aspect == LaneControlSignalType.GREEN_ARROW
        || aspect == LaneControlSignalType.SHARED_TURN
        || aspect == LaneControlSignalType.TURN;
  }

  public NBTTagCompound toNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setTag(K_SIGNALS, SerializationUtils.getBlockPosNBTArrayFromBlockPosList(signals));
    int[] ordinals = new int[aspects.length];
    for (int i = 0; i < aspects.length; i++) {
      ordinals[i] = aspects[i].toNBT();
    }
    c.setIntArray(K_ASPECTS, ordinals);
    c.setLong(K_CLEARANCE, clearanceTicks);
    return c;
  }

  public static LaneControlGroup fromNBT(NBTTagCompound c) {
    LaneControlGroup group = new LaneControlGroup();
    if (c.hasKey(K_SIGNALS)) {
      group.signals.addAll(
          SerializationUtils.getBlockPosListFromBlockPosNBTArray(c.getTag(K_SIGNALS)));
    }
    if (c.hasKey(K_ASPECTS)) {
      int[] ordinals = c.getIntArray(K_ASPECTS);
      for (int i = 0; i < ordinals.length && i < group.aspects.length; i++) {
        group.aspects[i] = LaneControlSignalType.fromNBT(ordinals[i]);
      }
    }
    group.clearanceTicks = c.hasKey(K_CLEARANCE) ? c.getLong(K_CLEARANCE) : DEFAULT_CLEARANCE;
    return group;
  }
}
