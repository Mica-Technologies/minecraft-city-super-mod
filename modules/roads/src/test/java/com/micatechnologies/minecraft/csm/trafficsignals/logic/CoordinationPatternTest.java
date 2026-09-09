package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * Time-of-day coordination patterns: the four plans a controller can hold, and the rule that
 * decides when a new one is allowed to take over.
 */
class CoordinationPatternTest {

  // region: the cycle-boundary rule

  @Test
  void aColdStartTakesWhateverTheClockSaysImmediately() {
    // RingBarrierState is transient and rebuilds after every reload, so this is the path a
    // controller takes every time its chunk loads. Remembering the old pattern instead would
    // run the wrong one whenever a world was saved in one slot and loaded in another.
    assertEquals(2, RingBarrierState.nextCoordinationSlot(-1, 2, true, 500L, 400L));
    assertEquals(3, RingBarrierState.nextCoordinationSlot(-1, 3, false, 0L, -1L));
  }

  @Test
  void aScheduledChangeIsRefusedInTheMiddleOfACycle() {
    // The load-bearing rule. Adopting a pattern with a different cycle length here would move
    // every force-off point out from under the phases already timing against them.
    assertEquals(0, RingBarrierState.nextCoordinationSlot(0, 1, true, 500L, 400L));
    assertEquals(0, RingBarrierState.nextCoordinationSlot(0, 1, true, 1799L, 1798L));
  }

  @Test
  void aScheduledChangeIsTakenWhenTheCycleRollsOver() {
    // The local cycle position going backwards is the rollover: 1799 -> 0 on a 1800-tick cycle.
    assertEquals(1, RingBarrierState.nextCoordinationSlot(0, 1, true, 0L, 1799L));
    // And with no change pending, a rollover leaves the running pattern alone.
    assertEquals(2, RingBarrierState.nextCoordinationSlot(2, 2, true, 0L, 1799L));
  }

  @Test
  void thereIsNoBoundaryToWaitForWhenTheRunningPatternIsFree() {
    // A free pattern has no cycle, so a controller whose night pattern is FREE would otherwise
    // never be able to leave it in the morning.
    assertEquals(1, RingBarrierState.nextCoordinationSlot(3, 1, false, 0L, -1L));
  }

  @Test
  void theFirstTickOfACycleIsNotMistakenForARollover() {
    // previousLocalCycle is negative when there is no previous tick to compare against, and a
    // negative must not read as "larger than 0" and fake a boundary on the very first tick.
    assertEquals(0, RingBarrierState.nextCoordinationSlot(0, 1, true, 0L, -1L));
  }

  // endregion

  // region: the pattern table on the plan

  @Test
  void slotZeroIsThePlanAControllerAlwaysHad() {
    // Back-compat rests on this: the single coordination plan every existing controller holds
    // is slot 0, so with patterns off nothing about its behaviour changes.
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    assertSame(plan.getCoordination(), plan.getCoordination(0));
    assertNotSame(plan.getCoordination(), plan.getCoordination(1));
  }

  @Test
  void patternsAreOffUntilAskedForAndThenTheClockChoosesTheSlot() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    assertFalse(plan.isTimeOfDayPatterns());
    // With patterns off the answer is always slot 0, whatever the clock says -- and with a null
    // world, which is what the engine's unit-test entry point passes.
    assertEquals(0, plan.getScheduledCoordinationSlot(null));
    plan.setTimeOfDayPatterns(true);
    assertEquals(0, plan.getScheduledCoordinationSlot(null),
        "no world means no hour, which has to resolve to something rather than throw");
  }

  @Test
  void anOutOfRangeSlotIsClampedRatherThanThrowing() {
    // Slot indices arrive from NBT and from configuration packets.
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    assertSame(plan.getCoordination(0), plan.getCoordination(-4));
    assertSame(plan.getCoordination(TrafficTimeOfDaySchedule.SLOT_COUNT - 1),
        plan.getCoordination(99));
  }

  @Test
  void everyPatternRoundTripsThroughNbtIndependently() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.setTimeOfDayPatterns(true);
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      plan.getCoordination(slot).setMode(TrafficSignalCoordinationMode.COORDINATED);
      plan.getCoordination(slot).setCycleLength(1000L + slot * 200L);
      plan.getCoordination(slot).setOffset(slot * 40L);
    }
    plan.getCoordination(TrafficTimeOfDaySchedule.SLOT_NIGHT)
        .setMode(TrafficSignalCoordinationMode.FREE);
    plan.getCoordinationSchedule().setStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK, 5);

    TrafficSignalProgrammedPhasePlan restored =
        TrafficSignalProgrammedPhasePlan.fromNBT(plan.toNBT());

    assertTrue(restored.isTimeOfDayPatterns());
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      assertEquals(1000L + slot * 200L, restored.getCoordination(slot).getCycleLength(),
          "cycle length of slot " + slot);
      assertEquals(slot * 40L, restored.getCoordination(slot).getOffset(),
          "offset of slot " + slot);
    }
    assertEquals(TrafficSignalCoordinationMode.FREE,
        restored.getCoordination(TrafficTimeOfDaySchedule.SLOT_NIGHT).getMode(),
        "a night pattern set to free has to come back free");
    assertEquals(5, restored.getCoordinationSchedule()
        .getStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
  }

  @Test
  void aPlanSavedBeforePatternsExistedReadsBackUnchanged() {
    // The real back-compat case: NBT with a coordination tag but no pattern table. It must load
    // as one plan in slot 0 with patterns off, not as four empty ones.
    TrafficSignalProgrammedPhasePlan original = TrafficSignalProgrammedPhasePlan.createDefault();
    original.getCoordination().setMode(TrafficSignalCoordinationMode.COORDINATED);
    original.getCoordination().setCycleLength(2400L);
    original.getCoordination().setOffset(120L);

    NBTTagCompound nbt = original.toNBT();
    nbt.removeTag("cop");
    nbt.removeTag("cos");
    nbt.removeTag("coe");

    TrafficSignalProgrammedPhasePlan restored = TrafficSignalProgrammedPhasePlan.fromNBT(nbt);
    assertFalse(restored.isTimeOfDayPatterns());
    assertEquals(2400L, restored.getCoordination().getCycleLength());
    assertEquals(120L, restored.getCoordination().getOffset());
    assertEquals(TrafficSignalCoordinationMode.COORDINATED, restored.getCoordination().getMode());
    // And the schedule it never had comes back on its defaults.
    assertEquals(6, restored.getCoordinationSchedule()
        .getStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
  }

  // endregion
}
