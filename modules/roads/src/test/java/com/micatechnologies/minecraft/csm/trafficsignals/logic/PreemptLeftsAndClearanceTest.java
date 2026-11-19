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
 * Preempt display (issues #370, #371 and #374), on the FYA arterial of {@link FyaBarrierClearanceTest}:
 * FYA lefts 1 (permissive 2) and 5 (permissive 6) leading throughs 2 and 6, side streets 3 and 4,
 * every phase yellow 80 and red clearance 60, and an emergency preempt on the phase 6 circuit
 * dwelling 6.
 */
class PreemptLeftsAndClearanceTest {

  private static final int PREEMPT_CIRCUIT = 3; // phase 6's circuit

  private static TrafficSignalProgrammedPhasePlan plan(boolean permissiveLefts, int[] exit) {
    return plan(permissiveLefts, exit, new int[] {6});
  }

  private static TrafficSignalProgrammedPhasePlan plan(boolean permissiveLefts, int[] exit,
      int[] dwell) {
    TrafficSignalProgrammedPhasePlan plan = FyaBarrierClearanceTest.arterialPlan(false);
    for (int n : new int[] {1, 2, 3, 4, 5, 6}) {
      plan.getPhase(n).setRedClear(60L);
    }
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    pe.setEnabled(true);
    pe.setType(TrafficSignalPreemptType.EMERGENCY);
    pe.setTriggerCircuitIndex(PREEMPT_CIRCUIT);
    pe.setTriggerOnDetectors(true);
    pe.setDwellPhases(dwell);
    pe.setExitPhases(exit);
    pe.setPermissiveLefts(permissiveLefts);
    plan.getPreempts().add(pe);
    return plan;
  }

  /** Every head's resolved indication per tick from {@code from} to {@code to}. */
  private static List<Map<BlockPos, Integer>> run(TrafficSignalProgrammedPhasePlan plan,
      long start, long release, long to, List<RingBarrierState> out) {
    return run(plan, FyaBarrierClearanceTest.arterialCircuits(), start, release, to, out);
  }

  private static List<Map<BlockPos, Integer>> run(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits ckts, long start, long release, long to,
      List<RingBarrierState> out) {
    RingBarrierState rb = new RingBarrierState();
    out.add(rb);
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

  /** Finds a tick in the first 3000 at which the FYA head 5 is red (not flashing, not served). */
  private static long redAt(TrafficSignalProgrammedPhasePlan plan) {
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, Long.MAX_VALUE, Long.MAX_VALUE, 3000L, rbs);
    for (int t = 600; t < s.size(); t++) {
      Integer i = s.get(t).get(FyaBarrierClearanceTest.FYA_5);
      if (i != null && TrafficSignalPhase.isRedIndication(i)) {
        return t;
      }
    }
    throw new AssertionError("the FYA left 5 was never red");
  }

  @Test
  @DisplayName("preempt: an FYA left that is red when the preempt begins stays red for the whole "
      + "preempt, though its permissive phase dwells (issue #370)")
  void fyaLeftRedAtEntryHeldRedThroughDwell() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0]);
    long start = redAt(plan) + 1L;
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, start, start + 600L, start + 1200L, rbs);
    BlockPos fya5 = FyaBarrierClearanceTest.FYA_5;
    assertTrue(TrafficSignalPhase.isRedIndication(s.get((int) start - 1).get(fya5)),
        "sanity: red at the call");
    for (long t = start; t < start + 600L; t++) {
      Integer now = s.get((int) t).get(fya5);
      assertTrue(!lit(now), "FYA left 5 lit (" + now + ") at t=" + (t - start)
          + " in a preempt it was red at the start of");
    }
  }

  @Test
  @DisplayName("preempt: an FYA left flashing when the preempt begins keeps flashing while its "
      + "opposing through dwells, rather than clearing beside it (issue #374)")
  void fyaLeftFlashingAtEntryKeepsFlashing() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0]);
    long start = flashingAt(plan) + 1L;
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, start, start + 600L, start + 1200L, rbs);
    BlockPos fya5 = FyaBarrierClearanceTest.FYA_5;
    assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) s.get((int) start - 1).get(fya5),
        "sanity: flashing at the call");
    for (long t = start; t < start + 600L; t++) {
      Integer now = s.get((int) t).get(fya5);
      assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) now, "FYA left 5 at t=" + (t - start)
          + " while its opposing through 6 stays green into the dwell");
    }
  }

  @Test
  @DisplayName("preempt: a flashing left whose opposing through clears at entry is held red, and "
      + "does not start flashing again when that through dwells after a track clear (#374)")
  void fyaLeftNeverRestartsAfterItsThroughClears() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0]);
    plan.getPreempts().get(0).setTrackClearPhases(new int[] {4});
    long start = flashingAt(plan) + 1L;
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, start, start + 900L, start + 1500L, rbs);
    BlockPos fya5 = FyaBarrierClearanceTest.FYA_5;
    boolean left = false;
    for (long t = start; t < start + 900L; t++) {
      Integer now = s.get((int) t).get(fya5);
      boolean flashing = now != null && now == TrafficSignalPhase.INDICATION_FYA;
      if (left) {
        assertTrue(!flashing, "FYA left 5 flashing again at t=" + (t - start)
            + " after it had cleared with its through");
      }
      left |= !flashing;
    }
    assertTrue(left, "the flashing left cleared when its opposing through cleared at entry");
  }

  /** A plain (non-compound) left head driven only by the FYA left overlap, on circuit 1. */
  private static final BlockPos OVERLAP_LEFT = new BlockPos(310, 0, 0);

  /** The arterial with an FYA left overlap: protected with 5, flashing while 6 is green. */
  private static TrafficSignalProgrammedPhasePlan overlapPlan() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0]);
    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(1);
    ov.setOutputMovement(TrafficSignalPhaseMovement.LEFT);
    ov.setIncludedPhases(new int[] {5});
    ov.setPermissivePhases(new int[] {6});
    plan.getVehicleOverlaps().add(ov);
    return plan;
  }

  private static TrafficSignalControllerCircuits overlapCircuits() {
    TrafficSignalControllerCircuits ckts = FyaBarrierClearanceTest.arterialCircuits();
    ckts.getCircuit(1).getLeftSignals().add(OVERLAP_LEFT);
    return ckts;
  }

  @Test
  @DisplayName("preempt: an FYA left overlap follows the same rule, kept flashing from a flashing "
      + "entry and held red from a red one (#370, #374)")
  void fyaLeftOverlapFollowsTheRule() {
    TrafficSignalProgrammedPhasePlan plan = overlapPlan();
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> normal =
        run(plan, overlapCircuits(), Long.MAX_VALUE, Long.MAX_VALUE, 3000L, rbs);
    long flashing = -1;
    long red = -1;
    for (int t = 600; t < normal.size() && (flashing < 0 || red < 0); t++) {
      Integer i = normal.get(t).get(OVERLAP_LEFT);
      if (flashing < 0 && i != null && i == TrafficSignalPhase.INDICATION_FYA) {
        flashing = t;
      }
      if (red < 0 && i != null && TrafficSignalPhase.isRedIndication(i)) {
        red = t;
      }
    }
    assertTrue(flashing >= 0 && red >= 0, "sanity: the overlap both flashed and was red");

    long start = flashing + 1L;
    List<Map<BlockPos, Integer>> s =
        run(plan, overlapCircuits(), start, start + 600L, start + 1200L, rbs);
    for (long t = start; t < start + 600L; t++) {
      assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) s.get((int) t).get(OVERLAP_LEFT),
          "overlap left at t=" + (t - start) + " after a flashing entry, 6 dwelling");
    }
    start = red + 1L;
    s = run(plan, overlapCircuits(), start, start + 600L, start + 1200L, rbs);
    for (long t = start; t < start + 600L; t++) {
      Integer now = s.get((int) t).get(OVERLAP_LEFT);
      assertTrue(!lit(now), "overlap left lit (" + now + ") at t=" + (t - start)
          + " after a red entry");
    }
  }

  @Test
  @DisplayName("preempt: dwelling both throughs, neither flashing left goes yellow or red while "
      + "its opposing through stays green (issue #374)")
  void noYellowTrapDwellingBothThroughs() {
    TrafficSignalProgrammedPhasePlan plan = plan(false, new int[0], new int[] {2, 6});
    long start = flashingAt(plan) + 1L;
    List<RingBarrierState> rbs = new ArrayList<>();
    List<Map<BlockPos, Integer>> s = run(plan, start, start + 600L, start + 1200L, rbs);
    for (BlockPos lens : new BlockPos[] {FyaBarrierClearanceTest.FYA_1,
        FyaBarrierClearanceTest.FYA_5}) {
      assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) s.get((int) start - 1).get(lens),
          "sanity: lens " + lens.getX() + " flashing at the call");
      for (long t = start; t < start + 600L; t++) {
        assertEquals(TrafficSignalPhase.INDICATION_FYA, (int) s.get((int) t).get(lens),
            "lens " + lens.getX() + " at t=" + (t - start) + " in a dwell of both throughs");
      }
    }
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
