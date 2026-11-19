package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #369, the Blossom Blvd x 90th program: a lead left first on barrier A (ring 1
 * [1, 2, 3, 4], coordinated 2 and 6) was skipped every cycle when the barrier B phase before it
 * maxed out early, so the rings reached barrier A 100-200 ticks before phase 1's window opened.
 * The call was refused there (its position wrapped round to the end of the cycle), coordinated 2
 * started early, and phase 1's window passed while 2 was green.
 */
class CoordinatedEarlyLeadTest {

  private static final long CYCLE = 1800L;

  /**
   * Phase 1 SBL (circuit 0, FYA left of 2), 2 and 6 coordinated throughs (circuits 1 and 2),
   * 3 a protected right on call (circuit 3, never called here), 4 EB on max recall (circuit 4).
   * Splits 1:250 2:700 6:950 3:220 4:630: windows 1 [0,250], 2 [250,950], 3 [950,1170],
   * 4 [1170,1800].
   */
  private static TrafficSignalProgrammedPhasePlan plan(long max4) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 1, 0);
    RingBarrierStateTest.enable(plan, 2, 1);
    RingBarrierStateTest.enable(plan, 6, 2);
    RingBarrierStateTest.enable(plan, 3, 3);
    RingBarrierStateTest.enable(plan, 4, 4);
    plan.setRing1Sequence(new int[] {1, 2, 3, 4});
    plan.setRing2Sequence(new int[] {5, 6, 8, 7});
    for (int n : new int[] {1, 2, 3, 4, 6}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setYellow(80L);
      p.setRedClear(40L);
      p.setPassage(40L);
    }
    plan.getPhase(1).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(1).setPermissivePhase(2);
    plan.getPhase(1).setMinGreen(100L);
    plan.getPhase(1).setMaxGreen(130L);
    plan.getPhase(2).setMinGreen(300L);
    plan.getPhase(2).setMaxGreen(700L);
    plan.getPhase(6).setMinGreen(300L);
    plan.getPhase(6).setMaxGreen(900L);
    plan.getPhase(3).setMinGreen(100L);
    plan.getPhase(3).setMaxGreen(200L);
    plan.getPhase(4).setMinGreen(200L);
    plan.getPhase(4).setMaxGreen(max4);
    plan.getPhase(4).setRecallMode(TrafficSignalRecallMode.MAXIMUM);
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    co.setMode(TrafficSignalCoordinationMode.COORDINATED);
    co.setCycleLength(CYCLE);
    co.setOffset(0L);
    co.setCoordinatedPhases(new int[] {2, 6});
    co.setSplit(1, 250L);
    co.setSplit(2, 700L);
    co.setSplit(6, 950L);
    co.setSplit(3, 220L);
    co.setSplit(4, 630L);
    return plan;
  }

  private static String desc(ServedMovement m) {
    return m == null ? "idle" : m.phaseNumber + ":" + m.vehicle;
  }

  /** Counts phase 1's protected greens in cycles 3..8 with a car standing in its bay. */
  private static void run(long max4) {
    TrafficSignalProgrammedPhasePlan plan = plan(max4);
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(5);
    Demand d = new Demand().veh(0, 0, 1, 0);
    int[] perCycle = new int[10];
    int[] twoStarts = new int[10];
    boolean was = false;
    boolean was2 = false;
    StringBuilder tl = new StringBuilder();
    String last = "";
    for (long t = 0; t < CYCLE * 9; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, d);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      String key = desc(m1) + " | " + desc(m2);
      if (!key.equals(last) && t >= CYCLE * 3 && t < CYCLE * 5) {
        tl.append("\n  t=").append(t).append(" local=").append(t % CYCLE).append("  ").append(key);
      }
      last = key;
      boolean is = m1 != null && m1.phaseNumber == 1 && m1.vehicle == VehInterval.GREEN;
      if (is && !was) {
        // attribute a start just before the window (an early arrival) to the cycle it serves
        perCycle[(int) ((t + 300L) / CYCLE)]++;
      }
      boolean is2 = m1 != null && m1.phaseNumber == 2 && m1.vehicle == VehInterval.GREEN;
      if (is2 && !was2) {
        twoStarts[(int) ((t + 300L) / CYCLE)]++;
      }
      was = is;
      was2 = is2;
    }
    for (int c = 3; c <= 8; c++) {
      assertEquals(1, perCycle[c], "phase 1 served " + perCycle[c] + " times in cycle " + c
          + " (max green of 4: " + max4 + ")" + tl);
      // the lead goes first: coordinated 2 is not started early and then cut back for it
      assertEquals(1, twoStarts[c], "phase 2 went green " + twoStarts[c] + " times in cycle "
          + c + " (max green of 4: " + max4 + ")" + tl);
    }
  }

  @Test
  @DisplayName("coordination: a lead left first on barrier A is served when the rings reach the "
      + "barrier early, once a cycle (issue #369)")
  void leadServedWhenBarrierBEndsEarly() {
    run(510L);
  }

  @Test
  @DisplayName("coordination: the lead left still runs once a cycle when barrier B runs to its "
      + "force-off (the workaround's timing)")
  void leadServedWhenBarrierBRunsToForceOff() {
    run(760L);
  }
}
