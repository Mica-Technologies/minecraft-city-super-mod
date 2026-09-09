package com.micatechnologies.minecraft.csm.trafficaccessories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class LaneControlGroupTest {

  @Test
  void anOpenLaneIsOneTrafficMayUse() {
    // This is what decides whether a change gets a clearance interval, so the two sets have to
    // be right: closing an open lane warns the drivers in it, opening a closed one does not.
    assertTrue(LaneControlGroup.isOpen(LaneControlSignalType.GREEN_ARROW));
    assertTrue(LaneControlGroup.isOpen(LaneControlSignalType.SHARED_TURN));
    assertTrue(LaneControlGroup.isOpen(LaneControlSignalType.TURN));

    assertFalse(LaneControlGroup.isOpen(LaneControlSignalType.RED_X));
    assertFalse(LaneControlGroup.isOpen(LaneControlSignalType.YELLOW_X));
    assertFalse(LaneControlGroup.isOpen(LaneControlSignalType.OFF));
    assertFalse(LaneControlGroup.isOpen(LaneControlSignalType.YELLOW_LEFT_ARROW));
    assertFalse(LaneControlGroup.isOpen(LaneControlSignalType.YELLOW_RIGHT_ARROW));
  }

  @Test
  void aGroupStartsDarkInEverySlot() {
    // A freshly added group must not open a lane it was never configured to open.
    LaneControlGroup group = new LaneControlGroup();
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      assertEquals(LaneControlSignalType.OFF, group.getAspect(slot), "slot " + slot);
    }
  }

  @Test
  void aSignalCannotBeAddedToTheSameGroupTwice() {
    // Every push walks the list, so a duplicate would be written twice for no reason.
    LaneControlGroup group = new LaneControlGroup();
    assertTrue(group.addSignal(new BlockPos(1, 2, 3)));
    assertFalse(group.addSignal(new BlockPos(1, 2, 3)));
    assertEquals(1, group.getSignals().size());
  }

  @Test
  void anOutOfRangeSlotIsClampedRatherThanThrowing() {
    // Slot indices arrive from NBT and from configuration packets.
    LaneControlGroup group = new LaneControlGroup();
    group.setAspect(-3, LaneControlSignalType.GREEN_ARROW);
    assertEquals(LaneControlSignalType.GREEN_ARROW, group.getAspect(0));
    group.setAspect(99, LaneControlSignalType.RED_X);
    assertEquals(LaneControlSignalType.RED_X,
        group.getAspect(TrafficTimeOfDaySchedule.SLOT_COUNT - 1));
  }

  @Test
  void clearanceCannotBeNegative() {
    // The GUI steps it down, and a negative would mean a clearance that had already expired
    // before it started -- a lane closing with no warning at all.
    LaneControlGroup group = new LaneControlGroup();
    group.setClearanceTicks(-100L);
    assertEquals(0L, group.getClearanceTicks());
  }

  @Test
  void cyclingAnAspectWrapsAllTheWayRound() {
    LaneControlGroup group = new LaneControlGroup();
    LaneControlSignalType first = group.getAspect(0);
    for (int i = 0; i < LaneControlSignalType.values().length; i++) {
      group.cycleAspect(0);
    }
    assertEquals(first, group.getAspect(0));
  }

  @Test
  void nbtRoundTripsSignalsAspectsAndClearance() {
    LaneControlGroup group = new LaneControlGroup();
    group.addSignal(new BlockPos(10, 64, -20));
    group.addSignal(new BlockPos(11, 64, -20));
    group.setAspect(TrafficTimeOfDaySchedule.SLOT_AM_PEAK, LaneControlSignalType.GREEN_ARROW);
    group.setAspect(TrafficTimeOfDaySchedule.SLOT_PM_PEAK, LaneControlSignalType.RED_X);
    group.setClearanceTicks(100L);

    LaneControlGroup restored = LaneControlGroup.fromNBT(group.toNBT());
    assertEquals(2, restored.getSignals().size());
    assertTrue(restored.getSignals().contains(new BlockPos(10, 64, -20)));
    assertTrue(restored.getSignals().contains(new BlockPos(11, 64, -20)));
    assertEquals(LaneControlSignalType.GREEN_ARROW,
        restored.getAspect(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
    assertEquals(LaneControlSignalType.RED_X,
        restored.getAspect(TrafficTimeOfDaySchedule.SLOT_PM_PEAK));
    assertEquals(100L, restored.getClearanceTicks());
  }

  @Test
  void anEmptyCompoundProducesAUsableGroup() {
    LaneControlGroup restored = LaneControlGroup.fromNBT(new NBTTagCompound());
    assertTrue(restored.getSignals().isEmpty());
    assertEquals(LaneControlGroup.DEFAULT_CLEARANCE, restored.getClearanceTicks());
    assertEquals(LaneControlSignalType.OFF, restored.getAspect(0));
  }
}
