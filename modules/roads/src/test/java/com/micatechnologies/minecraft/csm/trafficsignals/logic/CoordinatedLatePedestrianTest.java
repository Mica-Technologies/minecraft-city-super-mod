package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.PedInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A pedestrian button pressed late in a coordinated phase's window, on the UWS junction of issue
 * #361 (ring 1 [1, 2, 4], ring 2 [5, 6], phase 4 alone on barrier B, 2 and 6 coordinated on a 90 s
 * cycle, splits 1:230 2:750 4:820 5:230 6:520) with no vehicle calls anywhere, so 2 and 6 rest.
 *
 * <p>On the live controller a W button (phase 2's walk) pressed about 60 ticks before phase 2's
 * yield re-ran the walk, because nothing conflicting was waiting at that moment. A phase 4 button
 * a second later then could not be served: phase 2 held its new walk and clearance past barrier
 * B's window, phase 6 yielded on time and ring 2 sat red beside it, and phase 4 (no slack: its
 * split is its pedestrian minimum) waited a whole cycle.</p>
 */
class CoordinatedLatePedestrianTest {

  private static final long CYCLE = 1800L;
  /** Barrier B (phase 4's window) opens after phases 1 and 2: 230 + 750. */
  private static final long BARRIER_B = 980L;

  private static TrafficSignalProgrammedPhasePlan junction(long walk2) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 1, 0);
    RingBarrierStateTest.enable(plan, 2, 3);
    RingBarrierStateTest.enable(plan, 5, 1);
    RingBarrierStateTest.enable(plan, 6, 4);
    RingBarrierStateTest.enable(plan, 4, 2);
    for (int n : new int[] {1, 2, 4, 5, 6}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setMinGreen(100L);
      p.setPassage(40L);
      p.setMaxGreen(600L);
      p.setYellow(80L);
      p.setRedClear(40L);
    }
    plan.getPhase(2).setWalk(walk2);
    plan.getPhase(2).setPedClear(340L);
    plan.getPhase(6).setWalk(140L);
    plan.getPhase(6).setPedClear(340L);
    // Phase 4's split is exactly its pedestrian minimum: walk + clearance + yellow + red = 820.
    plan.getPhase(4).setWalk(360L);
    plan.getPhase(4).setPedClear(340L);
    plan.getPhase(1).setExclusionMask((1 << 5) | (1 << 6));
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    co.setMode(TrafficSignalCoordinationMode.COORDINATED);
    co.setCycleLength(CYCLE);
    co.setOffset(0L);
    co.setCoordinatedPhases(new int[] {2, 6});
    co.setSplit(1, 230L);
    co.setSplit(2, 750L);
    co.setSplit(4, 820L);
    co.setSplit(5, 230L);
    co.setSplit(6, 520L);
    return plan;
  }

  /** A pedestrian button whose request stays lit until its phase shows WALK, like the block. */
  private static final class Button {
    final int circuit;
    final int phase;
    final long pressAt;
    boolean lit;
    long walkAt = -1L;

    Button(int circuit, int phase, long pressAt) {
      this.circuit = circuit;
      this.phase = phase;
      this.pressAt = pressAt;
    }

    void update(long t, ServedMovement m) {
      if (t == pressAt) {
        lit = true;
      }
      if (lit && m != null && m.phaseNumber == phase && m.pedestrian == PedInterval.WALK) {
        lit = false;
        walkAt = t;
      }
    }
  }

  private static final class Result {
    long greenAt4 = -1L;
    long longestRingTwoDark;
    long walk2At = -1L;
    final StringBuilder timeline = new StringBuilder();
  }

  /**
   * Runs from cold start through cycle 3, presses phase 2's button at {@code press2} and phase
   * 4's at {@code press4} (both in cycle 3's local time), and records when phase 4 went green and
   * the longest run of ring 2 showing nothing but red while ring 1 was green on phase 2.
   */
  private static Result run(long walk2, long press2, long press4, long press2Duration,
      boolean leftCalled) {
    TrafficSignalProgrammedPhasePlan plan = junction(walk2);
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(5);
    long base = CYCLE * 3;
    Button b2 = new Button(3, 2, base + press2);
    Button b4 = new Button(2, 4, base + press4);
    Result r = new Result();
    String last = "";
    long darkSince = -1L;
    for (long t = 0; t < base + CYCLE * 2; t++) {
      Demand d = new Demand();
      if (leftCalled) {
        d.veh(0, 0, 3, 0);
      }
      if (b2.lit || (t >= b2.pressAt && t < b2.pressAt + press2Duration)) {
        d.ped(3);
      }
      if (b4.lit) {
        d.ped(2);
      }
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, d);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      b2.update(t, m1);
      b4.update(t, m1);
      if (t >= base - 200L) {
        String key = desc(m1) + " | " + desc(m2);
        if (!key.equals(last)) {
          r.timeline.append("\n  t=").append(t).append(" local=").append(t % CYCLE).append("  ")
              .append(key);
        }
        last = key;
      }
      if (t >= b4.pressAt && r.greenAt4 < 0L && m1 != null && m1.phaseNumber == 4
          && m1.vehicle == VehInterval.GREEN) {
        r.greenAt4 = t;
      }
      boolean ringTwoDark = m2 == null || m2.vehicle == VehInterval.RED;
      boolean twoGreen = m1 != null && m1.phaseNumber == 2 && m1.vehicle == VehInterval.GREEN;
      if (t >= base && ringTwoDark && twoGreen) {
        if (darkSince < 0L) {
          darkSince = t;
        }
        r.longestRingTwoDark = Math.max(r.longestRingTwoDark, t - darkSince + 1);
      } else {
        darkSince = -1L;
      }
    }
    r.walk2At = b2.walkAt;
    return r;
  }

  private static String desc(ServedMovement m) {
    return m == null ? "idle" : m.phaseNumber + ":" + m.vehicle + "/" + m.pedestrian;
  }

  @Test
  @DisplayName("coordination: a walk pressed too late to clear by the yield point waits for the "
      + "next walk, so a conflicting call a moment later is served in its own window")
  void latePressDoesNotRecycleTheCoordinatedWalk() {
    // Phase 2's yield point with nothing to clear but yellow and red is 980 - 120 = 860; press
    // its button 60 ticks before it, and phase 4's 20 ticks after that.
    Result r = run(140L, 800L, 820L, 10L, false);
    assertEquals(CYCLE * 3 + BARRIER_B, r.greenAt4,
        "phase 4 did not go green at barrier B's window start in the same cycle" + r.timeline);
    assertTrue(r.longestRingTwoDark < 200L,
        "ring 2 sat red beside phase 2 for " + r.longestRingTwoDark + " ticks" + r.timeline);
  }

  @Test
  @DisplayName("coordination: a walk pressed early enough to clear by the yield point still "
      + "recycles at once")
  void earlyPressStillRecycles() {
    // 140 walk + 340 clearance + 120 yellow and red end by 980 when pressed at 300.
    Result r = run(140L, 300L, 820L, 10L, false);
    assertEquals(CYCLE * 3 + BARRIER_B, r.greenAt4, "phase 4 late" + r.timeline);
    assertTrue(r.walk2At >= CYCLE * 3 + 300L && r.walk2At <= CYCLE * 3 + 302L,
        "phase 2's walk did not recycle at the press: " + r.walk2At + r.timeline);
  }

  /**
   * When ring 1's coordinated phase overruns its yield point anyway (here a walk longer than its
   * split leaves room for, started with the green), the other ring's coordinated phase must not
   * yield to the cross-barrier call at its own yield point: the barrier cannot cross until both
   * rings clear, so yielding only left ring 2 red beside a green it does not conflict with. It
   * holds green and clears with ring 1.
   */
  @Test
  @DisplayName("coordination: a coordinated phase holds green until the other ring can clear "
      + "with it, rather than sitting red beside its overrun")
  void partnerOverrunDoesNotStrandTheOtherRing() {
    // Phase 1 called every cycle, and phase 2's button pressed during it, so phase 2's long walk
    // starts with its green at 230: 600 walk + 340 clearance + 120 ends at 1290, 310 ticks past
    // barrier B's window start.
    Result r = run(600L, 100L, 820L, 10L, true);
    assertTrue(r.longestRingTwoDark < 200L,
        "ring 2 sat red beside phase 2 for " + r.longestRingTwoDark + " ticks" + r.timeline);
  }
}
