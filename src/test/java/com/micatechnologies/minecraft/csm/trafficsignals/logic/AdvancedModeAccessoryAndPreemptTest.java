package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.Arrays;
import java.util.Collections;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the advanced-mode plumbing fixes: FYA compound heads during preemption
 * phases, advance-warning beacon / NO-TURN blankout drive in ADVANCED mode, pedestrian-output
 * overlap call phases, and plan validation of preempt/overlap references.
 */
class AdvancedModeAccessoryAndPreemptTest {

  static final TrafficSignalControllerOverlaps NO_OVERLAPS = new TrafficSignalControllerOverlaps();

  @Nested
  @DisplayName("preemption phases drive FYA compound heads as one valid indication")
  class PreemptFyaTest {

    private final BlockPos fyaLens = new BlockPos(1, 0, 0);
    private final BlockPos arrow = new BlockPos(3, 0, 0);

    private TrafficSignalProgrammedPhasePlan fyaPlan() {
      TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
      TrafficSignalProgrammedPhase left = plan.getPhase(1);
      left.setCircuitIndex(0);
      left.setEnabled(true);
      left.setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
      left.setPermissivePhase(6); // opposing through
      RingBarrierStateTest.enable(plan, 6, 1);
      return plan;
    }

    private TrafficSignalControllerCircuits fyaCircuits() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
      c0.getFlashingLeftSignals().add(fyaLens);
      c0.getLeftSignals().add(arrow);
      circuits.addCircuit(c0);
      circuits.addCircuit(new TrafficSignalControllerCircuit());
      return circuits;
    }

    @Test
    @DisplayName("dwell serving the left green: 3-section OFF + green arrow (no red-with-green)")
    void dwellGreen() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.buildForPhases(null, fyaPlan(),
          fyaCircuits(), NO_OVERLAPS, Collections.singletonList(1), VehInterval.GREEN);
      assertTrue(phase.getOffSignals().contains(fyaLens), "3-section dark during protected green");
      assertTrue(phase.getGreenSignals().contains(arrow), "green-arrow add-on shows green");
      assertFalse(phase.getRedSignals().contains(fyaLens),
          "3-section must NOT be red while the green arrow shows");
    }

    @Test
    @DisplayName("enter clearance serving the left yellow: 3-section solid yellow + red arrow")
    void enterYellow() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.buildForPhases(null, fyaPlan(),
          fyaCircuits(), NO_OVERLAPS, Collections.singletonList(1), VehInterval.YELLOW);
      assertTrue(phase.getYellowSignals().contains(fyaLens));
      assertTrue(phase.getRedSignals().contains(arrow));
      assertFalse(phase.getYellowSignals().contains(arrow),
          "the add-on arrow must not show yellow alongside the yellow 3-section");
    }

    @Test
    @DisplayName("dwell serving the opposing through: unserved left shows the permissive flash")
    void dwellOpposingThrough() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.buildForPhases(null, fyaPlan(),
          fyaCircuits(), NO_OVERLAPS, Collections.singletonList(6), VehInterval.GREEN);
      assertTrue(phase.getFyaSignals().contains(fyaLens),
          "left unserved with its opposing through green flashes permissive");
      assertTrue(phase.getRedSignals().contains(arrow));
    }
  }

  @Nested
  @DisplayName("beacons and NO-TURN blankouts are driven in ADVANCED mode")
  class AccessoryDriveTest {

    private final BlockPos through = new BlockPos(10, 0, 0);
    private final BlockPos beacon = new BlockPos(11, 0, 0);
    private final BlockPos blankout = new BlockPos(12, 0, 0);

    private TrafficSignalProgrammedPhasePlan plan() {
      TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
      RingBarrierStateTest.enable(plan, 2, 0);
      return plan;
    }

    private TrafficSignalControllerCircuits circuits() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
      c0.getThroughSignals().add(through);
      c0.getBeaconSignals().add(beacon);
      c0.getNoTurnBlankoutSignals().add(blankout);
      circuits.addCircuit(c0);
      return circuits;
    }

    @Test
    @DisplayName("beacon is OFF while its circuit's through is green")
    void beaconOffWhenThroughGreen() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.build(null, plan(), circuits(), NO_OVERLAPS,
          new RingBarrierState.ServedMovement(2, VehInterval.GREEN,
              RingBarrierState.PedInterval.NONE), null);
      assertTrue(phase.getOffSignals().contains(beacon));
      assertFalse(phase.getYellowSignals().contains(beacon));
    }

    @Test
    @DisplayName("beacon flashes yellow while its circuit's through is not green")
    void beaconYellowWhenThroughNotGreen() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.build(null, plan(), circuits(), NO_OVERLAPS,
          new RingBarrierState.ServedMovement(2, VehInterval.RED,
              RingBarrierState.PedInterval.NONE), null);
      assertTrue(phase.getYellowSignals().contains(beacon),
          "advance-warning beacon must flash while the approach is not green (was pinned OFF)");
    }

    @Test
    @DisplayName("beacon flashes yellow in preemption all-red phases too")
    void beaconYellowDuringPreemptAllRed() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.buildForPhases(null, plan(), circuits(),
          NO_OVERLAPS, Collections.emptyList(), VehInterval.RED);
      assertTrue(phase.getYellowSignals().contains(beacon));
    }

    @Test
    @DisplayName("NO-TURN blankouts are driven (not pinned OFF) in advanced phases")
    void blankoutsDriven() {
      TrafficSignalPhase phase = AdvancedPhaseBuilder.build(null, plan(), circuits(), NO_OVERLAPS,
          new RingBarrierState.ServedMovement(2, VehInterval.GREEN,
              RingBarrierState.PedInterval.NONE), null);
      // Null-world path: no facing lookup is possible, so the sign conservatively stays visible
      // (walk list) — the point is it is driven by the blankout logic, not left dark forever.
      assertTrue(phase.getWalkSignals().contains(blankout),
          "blankout must be handed to the blankout drive logic (was pinned OFF forever)");
      assertFalse(phase.getOffSignals().contains(blankout));
    }
  }

  @Test
  @DisplayName("a pedestrian-output overlap's call phase is called by button requests")
  void pedOverlapCallPhaseCalledByButtonRequest() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 4, 1);
    RingBarrierStateTest.quickTiming(plan, 2);
    RingBarrierStateTest.quickTiming(plan, 4);
    // Ped overlap on circuit 2 whose detection (button requests) calls phase 4.
    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(2);
    ov.setOutputMovement(TrafficSignalPhaseMovement.PED);
    ov.setCallPhase(4);
    ov.setIncludedPhases(new int[] {4});
    plan.getVehicleOverlaps().add(ov);
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(3);
    RingBarrierStateTest.Demand none = new RingBarrierStateTest.Demand();
    RingBarrierStateTest.Demand button = new RingBarrierStateTest.Demand().ped(2);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, none); // rests on phase 2
    assertEquals(2, rb.getLastServed(1).phaseNumber);

    rb.tick(plan, ckts, NO_OVERLAPS, 30L, button); // button press -> overlap calls phase 4
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle,
        "the ped-overlap call must register as conflicting demand (was silently zero)");
    rb.tick(plan, ckts, NO_OVERLAPS, 60L, button);  // yellow -> red
    rb.tick(plan, ckts, NO_OVERLAPS, 90L, button);  // red clears -> cross -> serve phase 4
    assertNotNull(rb.getLastServed(1));
    assertEquals(4, rb.getLastServed(1).phaseNumber,
        "phase 4 must be served on the pedestrian overlap call");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
  }

  @Nested
  @DisplayName("plan validation: preempt and overlap references fault loudly")
  class ValidationTest {

    private final BlockPos through = new BlockPos(20, 0, 0);

    private TrafficSignalControllerCircuits circuits() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
      c0.getThroughSignals().add(through);
      circuits.addCircuit(c0);
      circuits.addCircuit(new TrafficSignalControllerCircuit());
      return circuits;
    }

    private TrafficSignalProgrammedPhasePlan validPlan() {
      TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
      RingBarrierStateTest.enable(plan, 2, 0);
      return plan;
    }

    @Test
    @DisplayName("a dangling preempt trigger circuit (e.g. after pruning) is a validation error")
    void danglingPreemptCircuit() {
      TrafficSignalProgrammedPhasePlan plan = validPlan();
      assertNull(plan.validate(circuits()), "base plan must be valid");
      TrafficSignalPreempt pe = new TrafficSignalPreempt();
      pe.setEnabled(true);
      pe.setTriggerCircuitIndex(5); // beyond the 2 configured circuits
      plan.getPreempts().add(pe);
      String error = plan.validate(circuits());
      assertNotNull(error, "an active preempt with a dangling circuit must fault, not go dead");
      assertTrue(error.contains("Preempt 1"), "error should identify the preempt: " + error);
    }

    @Test
    @DisplayName("a preempt triggering on the pedestrian movement is a validation error")
    void pedPreemptTrigger() {
      TrafficSignalProgrammedPhasePlan plan = validPlan();
      TrafficSignalPreempt pe = new TrafficSignalPreempt();
      pe.setEnabled(true);
      pe.setTriggerCircuitIndex(0);
      pe.setTriggerMovement(TrafficSignalPhaseMovement.PED);
      plan.getPreempts().add(pe);
      String error = plan.validate(circuits());
      assertNotNull(error, "a PED preempt trigger must fault (it can never fire/exit)");
      assertTrue(error.contains("pedestrian"), "error should explain the restriction: " + error);
      // A vehicle trigger movement restores validity.
      pe.setTriggerMovement(TrafficSignalPhaseMovement.THROUGH);
      assertNull(plan.validate(circuits()));
    }

    @Test
    @DisplayName("a dangling overlap output circuit is a validation error")
    void danglingOverlapCircuit() {
      TrafficSignalProgrammedPhasePlan plan = validPlan();
      TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
      ov.setEnabled(true);
      ov.setOutputCircuitIndex(7);
      ov.setIncludedPhases(new int[] {2});
      plan.getVehicleOverlaps().add(ov);
      String error = plan.validate(circuits());
      assertNotNull(error, "an active overlap with a dangling circuit must fault, not go dead");
      assertTrue(error.contains("Overlap 1"), "error should identify the overlap: " + error);
      // Disabled (or unassigned) entries are config-in-progress and must not fault.
      ov.setEnabled(false);
      assertNull(plan.validate(circuits()));
    }
  }
}
