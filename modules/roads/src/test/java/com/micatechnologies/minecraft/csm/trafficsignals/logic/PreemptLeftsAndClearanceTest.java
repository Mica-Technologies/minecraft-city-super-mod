package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Preempt display (issues #370 and #371), on the FYA arterial of {@link FyaBarrierClearanceTest}:
 * FYA lefts 1 (permissive 2) and 5 (permissive 6) leading throughs 2 and 6, side streets 3 and 4,
 * every phase yellow 80 and red clearance 60, and an emergency preempt on the phase 6 circuit
 * dwelling 6.
 */
class PreemptLeftsAndClearanceTest {

  private static final int PREEMPT_CIRCUIT = 3; // phase 6's circuit

  private static TrafficSignalProgrammedPhasePlan plan(boolean permissiveLefts, int[] exit) {
    TrafficSignalProgrammedPhasePlan plan = FyaBarrierClearanceTest.arterialPlan(false);
    for (int n : new int[] {1, 2, 3, 4, 5, 6}) {
      plan.getPhase(n).setRedClear(60L);
    }
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    pe.setEnabled(true);
    pe.setType(TrafficSignalPreemptType.EMERGENCY);
    pe.setTriggerCircuitIndex(PREEMPT_CIRCUIT);
    pe.setTriggerOnDetectors(true);
    pe.setDwellPhases(new int[] {6});
    pe.setExitPhases(exit);
    pe.setPermissiveLefts(permissiveLefts);
    plan.getPreempts().add(pe);
    return plan;
  }

  /** Every head's resolved indication per tick from {@code from} to {@code to}. */
  private static List<Map<BlockPos, Integer>> run(TrafficSignalProgrammedPhasePlan plan,
      long start, long release, long to, List<RingBarrierState> out) {
    RingBarrierState rb = new RingBarrierState();
    out.add(rb);
    TrafficSignalControllerCircuits ckts = FyaBarrierClearanceTest.arterialCircuits();
    RingBarrierStateTest.Demand normal = FyaBarrierClearanceTest.baysEmpty();
    RingBarrierStateTest.Demand pre = FyaBarrierClearanceTest.baysEmpty();
    pre.summaries.get(PREEMPT_CIRCUIT).withPreemptDetectorCalls(1);
    List<Map<BlockPos, Integer>> states = new ArrayList<>();
    for (long t = 0; t < to; t++) {
      rb.tick(plan, ckts, FyaBarrierClearanceTest.NO_OVERLAPS, t,
          t >= start && t < release ? pre : normal);
      states.add(new HashMap<>(rb.getLastAppliedPhase().resolveVehicleSignalStates()));
    }
    return states;
  }

  private static boolean lit(Integer i) {
    return i != null && !TrafficSignalPhase.isRedIndication(i);
  }

  /** Finds a tick in the first 3000 at which the FYA head 5 is flashing (6 green, 5 not served). */
  private static long flashingAt(TrafficSignalProgrammedPhasePlan plan) {
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, Long.MAX_VALUE, Long.MAX_VALUE, 3000L, rbs);
    for (int t = 600; t < s.size(); t++) {
      Integer i = s.get(t).get(FyaBarrierClearanceTest.FYA_5);
      if (i != null && i == TrafficSignalPhase.INDICATION_FYA) {
        return t;
      }
    }
    throw new AssertionError("the FYA left 5 never flashed");
  }

  @Test
  @DisplayName("preempt: an FYA left whose permissive phase dwells is held red for the whole "
      + "preempt, and clears through yellow when it was flashing at entry (issue #370)")
  void fyaLeftHeldRedThroughEmergencyDwell() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0]);
    long start = flashingAt(plan) + 1L;
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, start, start + 600L, start + 1200L, rbs);
    BlockPos fya5 = FyaBarrierClearanceTest.FYA_5;
    Integer prev = s.get((int) start - 1).get(fya5);
    assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) prev, "sanity: flashing at the call");
    boolean sawYellow = false;
    for (long t = start; t < start + 600L; t++) {
      Integer now = s.get((int) t).get(fya5);
      if (now != null && now == AbstractBlockControllableSignal.SIGNAL_YELLOW) {
        sawYellow = true;
      }
      if (t > start + 300L) {
        assertTrue(!lit(now), "FYA left 5 lit (" + now + ") at t=" + (t - start)
            + " in the dwell on its permissive phase 6");
      }
      if (prev != null && prev == TrafficSignalPhase.INDICATION_FYA && now != null
          && TrafficSignalPhase.isRedIndication(now)) {
        throw new AssertionError("FYA left 5 went from flashing to red without a yellow at t="
            + (t - start));
      }
      prev = now;
    }
    assertTrue(sawYellow, "the flashing left cleared through a yellow at entry");
  }

  @Test
  @DisplayName("preempt: with permissive lefts allowed, the FYA left flashes beside its dwelling "
      + "permissive phase (issue #370)")
  void fyaLeftFlashesWhenAllowed() {
    TrafficSignalProgrammedPhasePlan plan = plan(true, new int[0]);
    long start = flashingAt(plan) + 1L;
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, start, start + 600L, start + 1200L, rbs);
    Integer mid = s.get((int) (start + 450L)).get(FyaBarrierClearanceTest.FYA_5);
    assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) mid,
        "FYA left 5 flashes in the dwell when the preempt allows permissive lefts");
  }

  /**
   * For each head: every time it goes from red to lit after the preempt released, the last head
   * to go red must have done so at least the plan's red clearance (60) before.
   */
  @Test
  @DisplayName("preempt: entry, exit and every resumed green are timed from the plan's yellow and "
      + "red clearance, not the fixed 70 / 40 (issue #371)")
  void preemptClearancesFollowThePlan() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0]);
    for (long start = 700L; start < 2300L; start += 100L) {
      List<RingBarrierState> rbs = new ArrayList<>();
      List<Map<BlockPos, Integer>> s = run(plan, start, start + 500L, start + 1400L, rbs);
      long lastRed = Long.MIN_VALUE;
      for (long t = start; t < start + 1400L; t++) {
        Map<BlockPos, Integer> before = s.get((int) t - 1);
        Map<BlockPos, Integer> now = s.get((int) t);
        boolean wentGreen = false;
        for (Map.Entry<BlockPos, Integer> e : now.entrySet()) {
          Integer was = before.get(e.getKey());
          boolean litBefore = lit(was);
          boolean litNow = lit(e.getValue());
          if (litBefore && !litNow) {
            lastRed = t;
            // the yellow ahead of this red ran the plan's 80
            long y = 0;
            for (long k = t - 1; k >= start && s.get((int) k).get(e.getKey()) != null
                && s.get((int) k).get(e.getKey()) == AbstractBlockControllableSignal.SIGNAL_YELLOW;
                k--) {
              y++;
            }
            assertTrue(y == 0 || y >= 80L, "a yellow of " + y + " ticks before t=" + (t - start)
                + " (entry at " + start + ")");
          }
          if (!litBefore && litNow && e.getValue() != AbstractBlockControllableSignal.SIGNAL_YELLOW) {
            wentGreen = true;
          }
        }
        if (wentGreen && lastRed != Long.MIN_VALUE) {
          assertTrue(t - lastRed >= 60L, "a head lit " + (t - lastRed)
              + " ticks after another went red, at t=" + (t - start) + " (entry at " + start
              + "); the plan's red clearance is 60");
        }
      }
    }
  }

  @Test
  @DisplayName("preempt: service resumes on the preempt's exit phases (issue #371)")
  void exitPhasesStartService() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[] {3});
    long start = 1000L;
    List<RingBarrierState> rbs = new ArrayList<>();
    run(plan, start, start + 500L, start + 1400L, rbs);
    // replay and catch the first green after the exit clearance
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = FyaBarrierClearanceTest.arterialCircuits();
    RingBarrierStateTest.Demand normal = FyaBarrierClearanceTest.baysEmpty();
    RingBarrierStateTest.Demand pre = FyaBarrierClearanceTest.baysEmpty();
    pre.summaries.get(PREEMPT_CIRCUIT).withPreemptDetectorCalls(1);
    int first = 0;
    for (long t = 0; t < start + 1400L && first == 0; t++) {
      rb.tick(plan, ckts, FyaBarrierClearanceTest.NO_OVERLAPS, t,
          t >= start && t < start + 500L ? pre : normal);
      if (t > start + 500L) {
        for (int ring = 1; ring <= 2; ring++) {
          RingBarrierState.ServedMovement m = rb.getLastServed(ring);
          if (m != null && m.vehicle == RingBarrierState.VehInterval.GREEN) {
            first = m.phaseNumber;
          }
        }
      }
    }
    assertEquals(3, first, "the first green after the preempt is its exit phase 3");
  }
}
