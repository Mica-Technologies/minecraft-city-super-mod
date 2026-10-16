package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * More than one transit priority plan per controller (issue #287): one per bus movement, each
 * with its own trigger and transit phase, so two bus phases that do not run together are both
 * served.
 */
class MultiplePriorityPlansTest {

  /**
   * Phase 2 (circuit 0) and phase 4 (circuit 1), on different barriers, neither with a car:
   * only buses call them, each seen by its own circuit's detectors.
   */
  private static TrafficSignalProgrammedPhasePlan plan(boolean secondPlan) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 4, 1);
    priority(plan.getPriority(0), 0, 2);
    if (secondPlan) {
      priority(plan.getPriority(1), 1, 4);
    }
    return plan;
  }

  private static void priority(TrafficSignalPriorityPlan p, int circuit, int phase) {
    p.setEnabled(true);
    p.setTriggerCircuitIndex(circuit);
    p.setTriggerOnDetectors(true);
    p.setTransitPhase(phase);
  }

  /** Runs with a bus in view on both circuits; returns {phase 2 green seen, phase 4 green seen}. */
  private static boolean[] run(TrafficSignalProgrammedPhasePlan plan) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(2);
    Demand d = new Demand().veh(0, 0, 0, 0).veh(1, 0, 0, 0);
    d.summaries.get(0).withTransitDetectorCalls(1);
    d.summaries.get(1).withTransitDetectorCalls(1);
    boolean two = false;
    boolean four = false;
    for (long t = 0; t < 6000L; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, d);
      ServedMovement m = rb.getLastServed(1);
      if (m != null && m.vehicle == VehInterval.GREEN) {
        two |= m.phaseNumber == 2;
        four |= m.phaseNumber == 4;
      }
    }
    return new boolean[] {two, four};
  }

  @Test
  @DisplayName("with one plan, a bus on the other phase is never served")
  void onePlanServesOneBus() {
    boolean[] r = run(plan(false));
    assertTrue(r[0], "phase 2's bus was not served");
    assertFalse(r[1], "phase 4 has no plan, so its bus calls nothing");
  }

  @Test
  @DisplayName("a second plan serves the second bus, on a phase that does not run with the first")
  void twoPlansServeBothBuses() {
    boolean[] r = run(plan(true));
    assertTrue(r[0], "phase 2's bus was not served");
    assertTrue(r[1], "phase 4's bus was not served");
  }

  @Test
  @DisplayName("one plan saves as the old compound; several as a list; both read back")
  void savesPlans() {
    TrafficSignalProgrammedPhasePlan one = plan(false);
    NBTTagCompound c1 = one.toNBT();
    assertEquals(10, c1.getTagId("pri"), "a single plan is written as a compound");
    TrafficSignalProgrammedPhasePlan back1 = TrafficSignalProgrammedPhasePlan.fromNBT(c1);
    assertEquals(1, back1.getPriorities().size());
    assertEquals(2, back1.getPriority().getTransitPhase());

    // A plan the panel created by paging to it but nobody set up is not saved.
    one.getPriority(3);
    assertEquals(10, one.toNBT().getTagId("pri"));

    TrafficSignalProgrammedPhasePlan two = plan(true);
    NBTTagCompound c2 = two.toNBT();
    assertEquals(9, c2.getTagId("pri"), "several plans are written as a list");
    TrafficSignalProgrammedPhasePlan back2 = TrafficSignalProgrammedPhasePlan.fromNBT(c2);
    assertEquals(2, back2.getPriorities().size());
    assertEquals(4, back2.getPriority(1).getTransitPhase());
    assertEquals(1, back2.getPriority(1).getTriggerCircuitIndex());
  }

  @Test
  @DisplayName("plan indices from a packet are clamped")
  void clampsIndex() {
    TrafficSignalProgrammedPhasePlan plan = plan(false);
    assertEquals(plan.getPriority(0), plan.getPriority(-3));
    plan.getPriority(99).setTransitPhase(6);
    assertEquals(TrafficSignalProgrammedPhasePlan.MAX_PRIORITY_PLANS,
        plan.getPriorities().size());
    assertEquals(6, plan.getPriority(TrafficSignalProgrammedPhasePlan.MAX_PRIORITY_PLANS - 1)
        .getTransitPhase());
  }
}
