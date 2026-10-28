package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #318: a flashing-yellow-arrow left that runs with its opposing through must clear with it
 * (solid yellow with the through's yellow, red before the barrier crosses), never show a yellow
 * arrow, or keep flashing, while a phase across the barrier is green.
 */
class FyaBarrierClearanceTest {

  static final TrafficSignalControllerOverlaps NO_OVERLAPS = new TrafficSignalControllerOverlaps();

  static final BlockPos FYA_1 = new BlockPos(300, 0, 0);
  static final BlockPos ARROW_1 = new BlockPos(301, 0, 0);
  static final BlockPos FYA_5 = new BlockPos(302, 0, 0);
  static final BlockPos ARROW_5 = new BlockPos(303, 0, 0);

  /**
   * The reporter's arterial: FYA bay lefts 1 (permissive 2) and 5 (permissive 6) leading their
   * throughs 2 and 6 on barrier A, side streets 3 and 4 split on barrier B, coordinated on 2 + 6.
   * Circuits: 0 = phase 1, 1 = phase 2, 2 = phase 5, 3 = phase 6, 4 = phase 3, 5 = phase 4.
   */
  static TrafficSignalProgrammedPhasePlan arterialPlan(boolean coordinated) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 1, 0);
    RingBarrierStateTest.enable(plan, 2, 1);
    RingBarrierStateTest.enable(plan, 5, 2);
    RingBarrierStateTest.enable(plan, 6, 3);
    RingBarrierStateTest.enable(plan, 3, 4);
    RingBarrierStateTest.enable(plan, 4, 5);
    plan.getPhase(1).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(1).setPermissivePhase(2);
    plan.getPhase(5).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(5).setPermissivePhase(6);
    for (int n : new int[] {1, 2, 3, 4, 5, 6}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setMinGreen(100L);
      p.setPassage(40L);
      p.setMaxGreen(400L);
      p.setYellow(80L);
      p.setRedClear(40L);
    }
    if (coordinated) {
      plan.getCoordination().setMode(TrafficSignalCoordinationMode.COORDINATED);
      plan.getCoordination().setCycleLength(1600L);
    } else {
      plan.getCoordination().setCoordinatedPhases(new int[0]);
    }
    return plan;
  }

  static TrafficSignalControllerCircuits arterialCircuits() {
    TrafficSignalControllerCircuits ckts = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
    c0.getFlashingLeftSignals().add(FYA_1);
    c0.getLeftSignals().add(ARROW_1);
    ckts.addCircuit(c0);
    ckts.addCircuit(new TrafficSignalControllerCircuit());
    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    c2.getFlashingLeftSignals().add(FYA_5);
    c2.getLeftSignals().add(ARROW_5);
    ckts.addCircuit(c2);
    for (int i = 0; i < 3; i++) {
      ckts.addCircuit(new TrafficSignalControllerCircuit());
    }
    return ckts;
  }

  private static boolean crossGreen(ServedMovement m) {
    return m != null && (m.phaseNumber == 3 || m.phaseNumber == 4)
        && m.vehicle == VehInterval.GREEN;
  }

  /**
   * Runs a fresh engine (a plan just sent) from {@code start} for {@code length} ticks under
   * {@code demand} and returns every tick at which an FYA head was flashing or yellow while the
   * cross street was green.
   */
  static List<String> violations(TrafficSignalProgrammedPhasePlan plan, long start, long length,
      RingBarrierStateTest.Demand demand) {
    return violations(null, 0L, plan, start, length, demand);
  }

  /**
   * As {@link #violations(TrafficSignalProgrammedPhasePlan, long, long, RingBarrierStateTest.Demand)},
   * but the engine first runs {@code before} from tick 0 to {@code start} and only then gets
   * {@code plan}: a plan re-sent with /blockdata to a controller that is already cycling, whose
   * engine keeps running.
   */
  static List<String> violations(TrafficSignalProgrammedPhasePlan before, long beforeStart,
      TrafficSignalProgrammedPhasePlan plan, long start, long length,
      RingBarrierStateTest.Demand demand) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = arterialCircuits();
    List<String> bad = new ArrayList<>();
    if (before != null) {
      for (long t = beforeStart; t < start; t++) {
        rb.tick(before, ckts, NO_OVERLAPS, t, demand);
      }
    }
    for (long t = start; t < start + length; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, demand);
      if (!crossGreen(rb.getLastServed(1)) && !crossGreen(rb.getLastServed(2))) {
        continue;
      }
      TrafficSignalPhase applied = rb.getLastAppliedPhase();
      for (BlockPos lens : new BlockPos[] {FYA_1, FYA_5}) {
        if (applied.getFyaSignals().contains(lens) || applied.getYellowSignals().contains(lens)) {
          bad.add("t=" + t + " (start " + start + ") lens " + lens.getX()
              + (applied.getFyaSignals().contains(lens) ? " FYA" : " YELLOW")
              + " over the cross street's green");
        }
      }
    }
    return bad;
  }

  /** Every approach called: the bays, the throughs and both side streets. */
  static RingBarrierStateTest.Demand allCalled() {
    return new RingBarrierStateTest.Demand().veh(0, 0, 1, 0).veh(1, 1, 0, 0).veh(2, 0, 1, 0)
        .veh(3, 1, 0, 0).veh(4, 1, 0, 0).veh(5, 1, 0, 0);
  }

  /** The throughs and side streets called, the bays empty (the lefts run permissive only). */
  static RingBarrierStateTest.Demand baysEmpty() {
    return new RingBarrierStateTest.Demand().veh(1, 1, 0, 0).veh(3, 1, 0, 0).veh(4, 1, 0, 0)
        .veh(5, 1, 0, 0);
  }

  private static void assertNoneFromAnyStart(boolean coordinated,
      RingBarrierStateTest.Demand demand) {
    List<String> bad = new ArrayList<>();
    for (long start = 0L; start < 1600L; start += 10L) {
      bad.addAll(violations(arterialPlan(coordinated), start, 4800L, demand));
    }
    assertTrue(bad.isEmpty(), bad.size() + " ticks, first: "
        + (bad.isEmpty() ? "" : bad.subList(0, Math.min(10, bad.size()))));
  }

  @Test
  @DisplayName("#318: coordinated, all called: no FYA arrow over the cross street, from any start")
  void coordinatedAllCalled() {
    assertNoneFromAnyStart(true, allCalled());
  }

  @Test
  @DisplayName("#318: coordinated, bays empty: no FYA arrow over the cross street, from any start")
  void coordinatedBaysEmpty() {
    assertNoneFromAnyStart(true, baysEmpty());
  }

  /** The left's lens: FYA, Y, R or - (dark, under its protected green). */
  private static String lens(TrafficSignalPhase applied, BlockPos lens) {
    return applied.getFyaSignals().contains(lens) ? "FYA"
        : applied.getYellowSignals().contains(lens) ? "Y"
        : applied.getRedSignals().contains(lens) ? "R" : "-";
  }

  @Test
  @DisplayName("#318: a plan sent to a cycling controller clears the FYA lefts with the throughs")
  void resentPlanClearsLeftsWithThroughs() {
    // The reported timeline: a FREE controller is sent a coordinated plan while it runs. The
    // lead lefts, served in FREE just before the cycle starts, end inside their window with
    // their bays still called and are accepted again; through 2 + 6's green that call stood
    // while the side street's had not opened yet, so the flash was held for a wrap that never
    // came: throughs yellow and red with the arrows flashing, then the arrows' yellow over the
    // cross street's green.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = arterialCircuits();
    TrafficSignalProgrammedPhasePlan free = arterialPlan(false);
    TrafficSignalProgrammedPhasePlan coordinated = arterialPlan(true);
    boolean sawThroughYellow = false;
    boolean sawThroughRed = false;
    boolean sawCross = false;
    for (long t = 0; t < 4800L; t++) {
      rb.tick(t < 3200L ? free : coordinated, ckts, NO_OVERLAPS, t, allCalled());
      if (t < 3200L) {
        continue;
      }
      ServedMovement m1 = rb.getLastServed(1);
      TrafficSignalPhase applied = rb.getLastAppliedPhase();
      if (m1 != null && m1.phaseNumber == 2 && m1.vehicle == VehInterval.YELLOW) {
        sawThroughYellow = true;
        assertEquals("Y", lens(applied, FYA_1), "t=" + t + ": the arrow clears with 2's yellow");
        assertEquals("Y", lens(applied, FYA_5), "t=" + t + ": the arrow clears with 2's yellow");
      } else if (m1 != null && m1.phaseNumber == 2 && m1.vehicle == VehInterval.RED) {
        sawThroughRed = true;
        assertEquals("R", lens(applied, FYA_1), "t=" + t + ": red before the barrier crosses");
        assertEquals("R", lens(applied, FYA_5), "t=" + t + ": red before the barrier crosses");
      } else if (crossGreen(m1)) {
        sawCross = true;
        assertEquals("R", lens(applied, FYA_1), "t=" + t + ": no arrow over the cross street");
        assertEquals("R", lens(applied, FYA_5), "t=" + t + ": no arrow over the cross street");
      }
    }
    assertTrue(sawThroughYellow && sawThroughRed && sawCross, "the cycle reached the cross street");
  }

  @Test
  @DisplayName("#318: a re-sent plan never shows an FYA arrow over the cross street, at any time")
  void resentPlanFromAnyPoint() {
    List<String> bad = new ArrayList<>();
    for (long start = 0L; start < 1600L; start += 10L) {
      // from FREE into coordination, and a coordinated plan whose offset changed
      bad.addAll(violations(arterialPlan(false), 0L, arterialPlan(true), 3200L + start, 3200L,
          allCalled()));
      TrafficSignalProgrammedPhasePlan otherOffset = arterialPlan(true);
      otherOffset.getCoordination().setOffset(700L);
      bad.addAll(violations(otherOffset, 0L, arterialPlan(true), 3200L + start, 3200L,
          allCalled()));
    }
    assertTrue(bad.isEmpty(), bad.size() + " ticks, first: "
        + (bad.isEmpty() ? "" : bad.subList(0, Math.min(10, bad.size()))));
  }

  @Test
  @DisplayName("#318: free, all called: no FYA arrow over the cross street, from any start")
  void freeAllCalled() {
    assertNoneFromAnyStart(false, allCalled());
  }
}
