package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * A lane control controller: groups of lane control signals, and the aspect each group shows in
 * each time-of-day slot.
 *
 * <p>This is deliberately not a mode on the signal controller. That block's whole model is
 * phases, rings, barriers, circuits and detection, and a reversible lane uses none of it — the
 * centre lanes run inbound in the morning and outbound in the evening because of the clock, not
 * because of a phase. Bolting an unrelated second machine onto the largest and most
 * safety-critical tile entity in the mod would have put this work inside the file the ASC-3
 * audit had to repair, for no shared machinery at all.</p>
 *
 * <p>The one piece of real timing here is the clearance. Closing a lane runs YELLOW X first, so
 * the drivers already in it are told to leave before the X goes red; opening one does not,
 * because there is nobody in it to warn. That asymmetry is the whole reason the transition is
 * modelled rather than snapped.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityLaneControlController extends AbstractTickableTileEntity {

  /**
   * Ticks between pushes. A lane aspect changes a handful of times a day, so there is nothing to
   * gain from looking more often, and a controller that walked its whole signal list every tick
   * would be the most expensive block in a city for no reason.
   */
  public static final int TICK_RATE = 10;

  /** Follow the clock rather than a hand-picked slot. */
  public static final int MANUAL_OFF = -1;

  private static final String K_GROUPS = "grp";
  private static final String K_SCHEDULE = "sch";
  private static final String K_MANUAL = "man";

  private final List<LaneControlGroup> groups = new ArrayList<>();
  private final TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();

  /**
   * A slot to hold regardless of the clock, or {@link #MANUAL_OFF}. Real systems can be driven
   * by hand for an incident or a closure, and without this the block would only ever be a clock.
   */
  private int manualSlot = MANUAL_OFF;

  // Transient: what each group is actually showing, and when its clearance ends. Not saved --
  // a controller that came back mid-clearance would be holding a YELLOW X nobody asked for, and
  // recomputing from the clock on the next tick is both simpler and correct.
  private transient List<LaneControlSignalType> shown = new ArrayList<>();
  private transient List<Long> clearanceUntil = new ArrayList<>();

  public List<LaneControlGroup> getGroups() {
    return groups;
  }

  public TrafficTimeOfDaySchedule getSchedule() {
    return schedule;
  }

  public int getManualSlot() {
    return manualSlot;
  }

  /**
   * Sets a slot to hold by hand, or {@link #MANUAL_OFF} to follow the clock again.
   *
   * @param manualSlot the slot to hold, or {@link #MANUAL_OFF}
   */
  public void setManualSlot(int manualSlot) {
    if (manualSlot < 0 || manualSlot >= TrafficTimeOfDaySchedule.SLOT_COUNT) {
      this.manualSlot = MANUAL_OFF;
    } else {
      this.manualSlot = manualSlot;
    }
    sync();
  }

  /** Which slot's aspects are in force: the hand-picked one, or whatever the clock says. */
  public int getActiveSlot() {
    return manualSlot == MANUAL_OFF ? schedule.getActiveSlot(world) : manualSlot;
  }

  public LaneControlGroup addGroup() {
    LaneControlGroup group = new LaneControlGroup();
    groups.add(group);
    sync();
    return group;
  }

  public void removeGroup(int index) {
    if (index >= 0 && index < groups.size()) {
      groups.remove(index);
      sync();
    }
  }

  /**
   * Links a lane control signal into a group, removing it from any other group first.
   *
   * <p>A signal in two groups would be written twice every tick by two different aspects, and
   * which one won would depend on group order — so membership is made exclusive here rather than
   * left as something a player could set up and then not understand.</p>
   *
   * @param groupIndex the group to add it to
   * @param pos        the signal's position
   *
   * @return {@code true} if the signal was added
   */
  public boolean linkSignal(int groupIndex, BlockPos pos) {
    if (groupIndex < 0 || groupIndex >= groups.size() || pos == null) {
      return false;
    }
    for (LaneControlGroup other : groups) {
      other.removeSignal(pos);
    }
    boolean added = groups.get(groupIndex).addSignal(pos);
    sync();
    return added;
  }

  /**
   * Removes a signal from every group.
   *
   * @param pos the signal's position
   *
   * @return {@code true} if any group held it
   */
  public boolean unlinkSignal(BlockPos pos) {
    boolean removed = false;
    for (LaneControlGroup group : groups) {
      removed |= group.removeSignal(pos);
    }
    if (removed) {
      sync();
    }
    return removed;
  }

  @Override
  public long getTickRate() {
    return TICK_RATE;
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }
    int slot = getActiveSlot();
    long now = world.getTotalWorldTime();
    ensureTransientSize();

    for (int i = 0; i < groups.size(); i++) {
      LaneControlGroup group = groups.get(i);
      LaneControlSignalType target = group.getAspect(slot);
      LaneControlSignalType current = shown.get(i);

      if (current == target) {
        continue;
      }
      if (now < clearanceUntil.get(i)) {
        continue; // still warning the lane; the YELLOW X is already pushed
      }
      if (LaneControlGroup.isOpen(current) && group.getClearanceTicks() > 0L
          && current != LaneControlSignalType.YELLOW_X) {
        // Closing or re-purposing an open lane: warn it first.
        push(group, LaneControlSignalType.YELLOW_X);
        shown.set(i, LaneControlSignalType.YELLOW_X);
        clearanceUntil.set(i, now + group.getClearanceTicks());
        continue;
      }
      push(group, target);
      shown.set(i, target);
      clearanceUntil.set(i, 0L);
    }
  }

  /**
   * Grows the transient per-group state to match the group list.
   *
   * <p>Starts every group on {@code null} rather than on OFF, so the first tick after a load
   * pushes the real aspect instead of deciding it has nothing to do.</p>
   */
  private void ensureTransientSize() {
    while (shown.size() < groups.size()) {
      shown.add(null);
      clearanceUntil.add(0L);
    }
    while (shown.size() > groups.size()) {
      shown.remove(shown.size() - 1);
      clearanceUntil.remove(clearanceUntil.size() - 1);
    }
  }

  private void push(LaneControlGroup group, LaneControlSignalType aspect) {
    for (BlockPos pos : group.getSignals()) {
      // Only touch loaded chunks: reaching into an unloaded one would force it to load, and a
      // controller with signals spread down a corridor would keep the whole corridor loaded.
      if (!world.isBlockLoaded(pos)) {
        continue;
      }
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityLaneControlSignal) {
        ((TileEntityLaneControlSignal) te).setSignalType(aspect);
      }
    }
  }

  /**
   * Marks a configuration change made through one of the group or schedule objects directly.
   *
   * <p>Those are handed out by reference so the packet handler can edit them in place, which
   * means nothing in this class sees the change happen — this is how the handler says it did.
   * </p>
   */
  public void markConfigChanged() {
    sync();
  }

  private void sync() {
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    groups.clear();
    if (compound.hasKey(K_GROUPS)) {
      NBTTagList list = compound.getTagList(K_GROUPS, 10); // 10 = TAG_COMPOUND
      for (int i = 0; i < list.tagCount(); i++) {
        groups.add(LaneControlGroup.fromNBT(list.getCompoundTagAt(i)));
      }
    }
    if (compound.hasKey(K_SCHEDULE)) {
      schedule.readNBT(compound.getCompoundTag(K_SCHEDULE));
    }
    manualSlot = compound.hasKey(K_MANUAL) ? compound.getInteger(K_MANUAL) : MANUAL_OFF;
    shown = new ArrayList<>();
    clearanceUntil = new ArrayList<>();
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    NBTTagList list = new NBTTagList();
    for (LaneControlGroup group : groups) {
      list.appendTag(group.toNBT());
    }
    compound.setTag(K_GROUPS, list);
    compound.setTag(K_SCHEDULE, schedule.writeNBT(new NBTTagCompound()));
    compound.setInteger(K_MANUAL, manualSlot);
    return compound;
  }
}
