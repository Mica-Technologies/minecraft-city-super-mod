package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * Transit signal priority: green extension and early return, and the guards that keep them from
 * doing what a preempt does.
 */
class TrafficSignalPriorityPlanTest {

  private static TrafficSignalPriorityPlan configured() {
    TrafficSignalPriorityPlan plan = new TrafficSignalPriorityPlan();
    plan.setEnabled(true);
    plan.setTriggerCircuitIndex(3);
    plan.setTransitPhase(2);
    plan.setMaxExtension(200L);
    plan.setMaxEarlyReturn(200L);
    return plan;
  }

  @Test
  void extensionRaisesTheTransitPhasesCeilingByWhatIsLeftOfTheBudget() {
    TrafficSignalPriorityPlan plan = configured();
    assertEquals(1000L + 200L, plan.adjustMaxGreen(1000L, 100L, true, false, 200L));
    // Half the budget spent, half the extension left.
    assertEquals(1000L + 100L, plan.adjustMaxGreen(1000L, 100L, true, false, 100L));
    // Budget exhausted: the phase is back on its own maximum, which is what stops a standing
    // call holding a green forever.
    assertEquals(1000L, plan.adjustMaxGreen(1000L, 100L, true, false, 0L));
  }

  @Test
  void theExtensionIsCappedEvenIfTheBudgetSaysOtherwise() {
    // The budget is transient state and the cap is configuration; the cap has to win, or a bug
    // in the budget accounting becomes an unbounded green.
    TrafficSignalPriorityPlan plan = configured();
    plan.setMaxExtension(60L);
    assertEquals(1000L + 60L, plan.adjustMaxGreen(1000L, 100L, true, false, 9999L));
  }

  @Test
  void earlyReturnLowersOnlyTheConflictingPhases() {
    TrafficSignalPriorityPlan plan = configured();
    assertEquals(1000L - 200L, plan.adjustMaxGreen(1000L, 100L, false, true, 200L));
    // A phase that does not conflict with the transit phase is none of priority's business:
    // shortening it would rob a movement that was never in the way.
    assertEquals(1000L, plan.adjustMaxGreen(1000L, 100L, false, false, 200L));
  }

  @Test
  void earlyReturnCanNeverPushAPhaseBelowItsMinimumGreen() {
    // The safety property this whole feature rests on. The engine will not terminate before
    // minMet regardless, but a maximum under the minimum is a nonsense the plan should not be
    // able to produce in the first place.
    TrafficSignalPriorityPlan plan = configured();
    plan.setMaxEarlyReturn(100000L);
    assertEquals(150L, plan.adjustMaxGreen(1000L, 150L, false, true, 0L));
    assertEquals(150L, plan.adjustMaxGreen(160L, 150L, false, true, 0L));
  }

  @Test
  void theRateLimitCountsWholeCycles() {
    TrafficSignalPriorityPlan plan = configured();
    plan.setMinCyclesBetweenGrants(2);
    assertFalse(plan.mayGrant(10L, 9L), "one cycle later is too soon");
    assertTrue(plan.mayGrant(11L, 9L), "two cycles later is allowed");
    assertTrue(plan.mayGrant(50L, 9L));
  }

  @Test
  void theRateLimitCannotApplyWithoutCyclesToCount() {
    // Free operation has no cycle number, so there is nothing to count -- the extension cap is
    // what bounds priority there. Refusing every grant instead would silently disable the
    // feature for every uncoordinated controller.
    TrafficSignalPriorityPlan plan = configured();
    plan.setMinCyclesBetweenGrants(4);
    assertTrue(plan.mayGrant(-1L, -1L));
    assertTrue(plan.mayGrant(-1L, 5L));
    assertTrue(plan.mayGrant(5L, -1L), "the first grant is never rate limited");
  }

  @Test
  void aLimitOfZeroMeansNoLimit() {
    TrafficSignalPriorityPlan plan = configured();
    plan.setMinCyclesBetweenGrants(0);
    assertTrue(plan.mayGrant(10L, 10L));
  }

  @Test
  void priorityIsNotRunnableUntilItHasSomethingToActOn() {
    // Enabled alone would have the engine spend a tick every tick discovering it has no phase
    // to favour and no circuit to hear from.
    TrafficSignalPriorityPlan plan = new TrafficSignalPriorityPlan();
    assertFalse(plan.isRunnable(), "off by default");
    plan.setEnabled(true);
    assertFalse(plan.isRunnable(), "no transit phase, no circuit");
    plan.setTransitPhase(2);
    assertFalse(plan.isRunnable(), "still no circuit");
    plan.setTriggerCircuitIndex(0);
    assertTrue(plan.isRunnable());
  }

  @Test
  void valuesAreHeldInsideTheirRanges() {
    TrafficSignalPriorityPlan plan = new TrafficSignalPriorityPlan();
    plan.setMaxExtension(-50L);
    assertEquals(0L, plan.getMaxExtension());
    plan.setMaxEarlyReturn(-50L);
    assertEquals(0L, plan.getMaxEarlyReturn());
    plan.setMinCyclesBetweenGrants(-3);
    assertEquals(0, plan.getMinCyclesBetweenGrants());
    plan.setTransitPhase(99);
    assertEquals(TrafficSignalProgrammedPhasePlan.PHASE_COUNT, plan.getTransitPhase());
    plan.setTransitPhase(-4);
    assertEquals(0, plan.getTransitPhase());
    plan.setTriggerMovement(null);
    assertEquals(TrafficSignalPhaseMovement.THROUGH, plan.getTriggerMovement());
  }

  @Test
  void nbtRoundTripsAndAnAbsentTagKeepsTheDefaults() {
    TrafficSignalPriorityPlan plan = configured();
    plan.setTriggerMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.setMinCyclesBetweenGrants(5);

    TrafficSignalPriorityPlan restored = TrafficSignalPriorityPlan.fromNBT(plan.toNBT());
    assertTrue(restored.isEnabled());
    assertEquals(3, restored.getTriggerCircuitIndex());
    assertEquals(2, restored.getTransitPhase());
    assertEquals(TrafficSignalPhaseMovement.PROTECTED_LEFT, restored.getTriggerMovement());
    assertEquals(5, restored.getMinCyclesBetweenGrants());

    TrafficSignalPriorityPlan fresh = TrafficSignalPriorityPlan.fromNBT(new NBTTagCompound());
    assertFalse(fresh.isEnabled());
    assertEquals(-1, fresh.getTriggerCircuitIndex(),
        "unassigned, not circuit 0 -- otherwise enabling it would listen to a real circuit");
    assertEquals(TrafficSignalPriorityPlan.DEFAULT_MAX_EXTENSION, fresh.getMaxExtension());
  }

  @Test
  void aControllerSavedBeforePriorityExistedReadsBackWithItOff() {
    TrafficSignalProgrammedPhasePlan original = TrafficSignalProgrammedPhasePlan.createDefault();
    NBTTagCompound nbt = original.toNBT();
    nbt.removeTag("pri");

    TrafficSignalProgrammedPhasePlan restored = TrafficSignalProgrammedPhasePlan.fromNBT(nbt);
    assertFalse(restored.getPriority().isEnabled());
    assertFalse(restored.getPriority().isRunnable());
  }

  @Test
  void priorityRoundTripsAsPartOfThePlan() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getPriority().setEnabled(true);
    plan.getPriority().setTransitPhase(6);
    plan.getPriority().setTriggerCircuitIndex(1);
    plan.getPriority().setMaxExtension(140L);

    TrafficSignalProgrammedPhasePlan restored =
        TrafficSignalProgrammedPhasePlan.fromNBT(plan.toNBT());
    assertTrue(restored.getPriority().isRunnable());
    assertEquals(6, restored.getPriority().getTransitPhase());
    assertEquals(140L, restored.getPriority().getMaxExtension());
  }
}
