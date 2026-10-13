package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Repro for two live reports on coordinated T junctions with every phase on MAX recall.
 *
 * <p>Z01 (freeze): after a cold start off the offset, ring 2 runs its lag phase 7 and ring 1
 * dual-enters the coordinated phase 4 beside it. When 7 clears, 4 is a dual-entry companion of a
 * coordinated phase on MAX recall: MAX recall re-arms lastActuation every tick (no gap-out), a
 * coordinated phase has no max-out, and a dual-entry companion is exempt from force-off and the
 * coordinated yield, so nothing can terminate it and phase 4 stays green forever.</p>
 *
 * <p>T03 (one through ends early): after a cold start, the coordinated phase 4 yields for the
 * lead left 3 whose window is opening, but 3 sits behind 4 in ring 1's sequence and the
 * within-barrier wrap is blocked by phase 6's call on the other barrier, so 3 is not served; ring
 * 1 then stays dark beside 8 (dual entry will not re-enter beside the green it cleared beside)
 * until 8's yield point. Transient: the controller is in step from the next cycle.</p>
 *
 * <p>Both are fixed in {@code RingBarrierState} (issue #275): a dual-entry companion ends without
 * gapping out, and a ring may wrap to the one phase its coordinated green yielded for while the
 * other ring is still working the barrier.</p>
 */
class CoordinatedDualEntryFreezeReproTest {

  static final TrafficSignalControllerOverlaps NO_OVERLAPS = new TrafficSignalControllerOverlaps();

  static void phase(TrafficSignalProgrammedPhasePlan plan, int n, int ring, int barrier, int ci,
      TrafficSignalPhaseMovement mv, boolean de, long mg, boolean pr) {
    TrafficSignalProgrammedPhase p = plan.getPhase(n);
    p.setRing(ring);
    p.setBarrier(barrier);
    p.setCircuitIndex(ci);
    p.setMovement(mv);
    p.setEnabled(true);
    p.setDualEntry(de);
    p.setRecallMode(TrafficSignalRecallMode.MAXIMUM);
    p.setMinGreen(mg);
    p.setMaxGreen(1200L);
    p.setYellow(80L);
    p.setRedClear(40L);
    p.setDelayedGreen(60L);
    p.setPedRecall(pr);
  }

  static void disable(TrafficSignalProgrammedPhasePlan plan, int n) {
    plan.getPhase(n).setEnabled(false);
    plan.getPhase(n).setCircuitIndex(-1);
  }

  /** The Z01 plan as read from the live controller's tcAdv NBT. */
  static TrafficSignalProgrammedPhasePlan z01() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    disable(plan, 1);
    disable(plan, 2);
    disable(plan, 5);
    plan.setRing1Sequence(new int[] {1, 2, 3, 4});
    plan.setRing2Sequence(new int[] {6, 5, 8, 7});
    phase(plan, 3, 1, 1, 3, TrafficSignalPhaseMovement.PROTECTED_LEFT, false, 100L, false);
    plan.getPhase(3).setWalk(140L);
    plan.getPhase(3).setPedClear(220L);
    plan.getPhase(3).setPassage(40L);
    phase(plan, 4, 1, 1, 1, TrafficSignalPhaseMovement.THROUGH, true, 140L, true);
    phase(plan, 6, 2, 0, 0, TrafficSignalPhaseMovement.THROUGH, true, 200L, true);
    phase(plan, 7, 2, 1, 4, TrafficSignalPhaseMovement.PROTECTED_LEFT, false, 100L, false);
    phase(plan, 8, 2, 1, 2, TrafficSignalPhaseMovement.THROUGH, true, 140L, true);
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    co.setMode(TrafficSignalCoordinationMode.COORDINATED);
    co.setCycleLength(1800L);
    co.setOffset(457L);
    co.setCoordinatedPhases(new int[] {4, 8});
    co.setSplit(2, 800L);
    co.setSplit(3, 240L);
    co.setSplit(4, 760L);
    co.setSplit(6, 800L);
    co.setSplit(7, 240L);
    co.setSplit(8, 760L);
    return plan;
  }

  /** The T03 plan (approximate: ring 2 order assumed). */
  static TrafficSignalProgrammedPhasePlan t03() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    disable(plan, 1);
    disable(plan, 2);
    disable(plan, 5);
    disable(plan, 7);
    plan.setRing1Sequence(new int[] {1, 2, 3, 4});
    plan.setRing2Sequence(new int[] {6, 5, 8, 7});
    phase(plan, 3, 1, 1, 3, TrafficSignalPhaseMovement.PROTECTED_LEFT, false, 100L, false);
    phase(plan, 4, 1, 1, 1, TrafficSignalPhaseMovement.THROUGH, true, 140L, true);
    phase(plan, 6, 2, 0, 0, TrafficSignalPhaseMovement.THROUGH, true, 200L, true);
    phase(plan, 8, 2, 1, 2, TrafficSignalPhaseMovement.THROUGH, true, 140L, true);
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    co.setMode(TrafficSignalCoordinationMode.COORDINATED);
    co.setCycleLength(1800L);
    co.setOffset(457L);
    co.setCoordinatedPhases(new int[] {4, 8});
    co.setSplit(3, 240L);
    co.setSplit(4, 760L);
    co.setSplit(6, 800L);
    co.setSplit(8, 1000L);
    return plan;
  }

  static String fmt(ServedMovement m) {
    return m == null ? "idle" : (m.phaseNumber + ":" + m.vehicle);
  }

  static boolean green(ServedMovement m) {
    return m != null && m.vehicle == VehInterval.GREEN;
  }

  /** Compressed timeline of both rings from a cold start at world tick t0. */
  static List<String> timeline(TrafficSignalProgrammedPhasePlan plan, long t0, long len) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(5);
    RingBarrierStateTest.Demand none = new RingBarrierStateTest.Demand();
    List<String> out = new ArrayList<>();
    String last = "";
    long cycle = plan.getCoordination().getCycleLength();
    for (long t = t0; t <= t0 + len; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, none);
      String key = "r1=" + fmt(rb.getLastServed(1)) + " r2=" + fmt(rb.getLastServed(2));
      if (!key.equals(last)) {
        long local = ((t - plan.getCoordination().getOffset()) % cycle + cycle) % cycle;
        out.add("t=" + (t - t0) + " (lc=" + local + ") " + key);
        last = key;
      }
    }
    return out;
  }

  /**
   * Cold-starts the plan at t0 = 0, 23, 46 ... across one cycle (both with and without
   * beginOnRestPhases), runs six cycles of no detector demand, and returns {longest single green,
   * longest time one ring sat idle while the other showed a barrier-1 green, t0 of the worst
   * green}.
   */
  static long[] sweep(Supplier<TrafficSignalProgrammedPhasePlan> mk) {
    long worstGreen = 0L;
    long worstDark = 0L;
    long worstT0 = -1L;
    long worstDarkT0 = -1L;
    boolean worstDarkRest = false;
    for (long t0 = 0; t0 < 1800; t0 += 23) {
      for (boolean rest : new boolean[] {false, true}) {
        TrafficSignalProgrammedPhasePlan plan = mk.get();
        RingBarrierState rb = new RingBarrierState();
        if (rest) {
          rb.beginOnRestPhases();
        }
        TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(5);
        RingBarrierStateTest.Demand none = new RingBarrierStateTest.Demand();
        long[] gs = {-1L, -1L};
        long darkStart = -1L;
        for (long t = t0; t <= t0 + 1800L * 6; t++) {
          rb.tick(plan, ckts, NO_OVERLAPS, t, none);
          ServedMovement[] m = {rb.getLastServed(1), rb.getLastServed(2)};
          for (int r = 0; r < 2; r++) {
            if (green(m[r])) {
              if (gs[r] < 0L) {
                gs[r] = t;
              }
              if (t - gs[r] > worstGreen) {
                worstGreen = t - gs[r];
                worstT0 = t0;
              }
            } else {
              gs[r] = -1L;
            }
          }
          boolean dark = (m[0] == null && green(m[1]) && m[1].phaseNumber != 6)
              || (m[1] == null && green(m[0]));
          if (dark) {
            if (darkStart < 0L) {
              darkStart = t;
            }
            if (t - darkStart > worstDark) {
              worstDark = t - darkStart;
              worstDarkT0 = t0;
              worstDarkRest = rest;
            }
          } else {
            darkStart = -1L;
          }
        }
      }
    }
    return new long[] {worstGreen, worstDark, worstT0, worstDarkT0, worstDarkRest ? 1L : 0L};
  }

  @Test
  @DisplayName("Z01: a dual-entry coordinated phase on MAX recall must not hold green forever")
  void z01NeverFreezes() {
    timeline(z01(), 1234L, 1800L).forEach(System.out::println);
    long[] r = sweep(CoordinatedDualEntryFreezeReproTest::z01);
    System.out.println("Z01 worst green " + r[0] + " (t0=" + r[2] + "), worst dark " + r[1]);
    // The longest legitimate green is a cold-start dwell to the next yield point (< 1 cycle).
    assertTrue(r[0] < 1800L * 2, "a phase stayed green " + r[0] + " ticks (frozen)");
  }

  @Test
  @DisplayName("Z01 workarounds: each of these plan changes avoids the freeze today")
  void z01Workarounds() {
    List<Supplier<TrafficSignalProgrammedPhasePlan>> fixes = new ArrayList<>();
    fixes.add(() -> {
      TrafficSignalProgrammedPhasePlan p = z01();
      p.getPhase(4).setDualEntry(false);
      return p;
    });
    fixes.add(() -> {
      TrafficSignalProgrammedPhasePlan p = z01();
      p.getPhase(4).setRecallMode(TrafficSignalRecallMode.NONE);
      p.getPhase(8).setRecallMode(TrafficSignalRecallMode.NONE);
      return p;
    });
    fixes.add(() -> {
      TrafficSignalProgrammedPhasePlan p = z01();
      p.setRing2Sequence(new int[] {5, 6, 7, 8});
      return p;
    });
    for (Supplier<TrafficSignalProgrammedPhasePlan> f : fixes) {
      long[] r = sweep(f);
      System.out.println("workaround: worst green " + r[0] + ", worst dark " + r[1]);
      assertTrue(r[0] < 1800L * 2, "a phase stayed green " + r[0] + " ticks (frozen)");
    }
  }

  @Test
  @DisplayName("T03: cold-start timeline (phase 4 yields for an unreachable phase 3)")
  void t03ColdStartTimeline() {
    timeline(t03(), 828L, 1800L * 2).forEach(System.out::println);
  }

  @Test
  @DisplayName("T03: a ring whose coordinated phase yielded for its lead left serves that left")
  void t03YieldTargetIsServed() {
    long[] r = sweep(CoordinatedDualEntryFreezeReproTest::t03);
    System.out.println("T03 worst green " + r[0] + ", worst dark " + r[1] + " (t0=" + r[3]
        + (r[4] != 0L ? ", rest start)" : ")"));
    // Before the fix ring 1 sat dark for 879 ticks beside phase 8 after phase 4 yielded for a
    // phase 3 it could not reach; wrapping to the yield target brought that to 419.
    assertTrue(r[1] < 600L, "a ring sat dark for " + r[1] + " ticks beside a barrier green");
  }

  @Test
  @DisplayName("Z01: wrapping to the yield target must not starve the next barrier")
  void z01NextBarrierStillServed() {
    // Cold start on the rest phases at t0=828: phase 4 ends at lc 1680 for phase 3 with ring 2
    // already idle. The barrier must cross to phase 6, not wrap ring 1 back to 3 and skip 6.
    TrafficSignalProgrammedPhasePlan plan = z01();
    RingBarrierState rb = new RingBarrierState();
    rb.beginOnRestPhases();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(5);
    RingBarrierStateTest.Demand none = new RingBarrierStateTest.Demand();
    boolean sixServed = false;
    boolean threeServed = false;
    for (long t = 828L; t <= 828L + 2000L && !threeServed; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, none);
      sixServed |= green(rb.getLastServed(2)) && rb.getLastServed(2).phaseNumber == 6;
      threeServed = green(rb.getLastServed(1)) && rb.getLastServed(1).phaseNumber == 3;
    }
    assertTrue(threeServed, "phase 3 was never served");
    assertTrue(sixServed, "phase 3 was served before phase 6 had its turn");
  }
}
