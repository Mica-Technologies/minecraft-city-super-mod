package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * World-free tests for the ADVANCED-mode ring-and-barrier engine, driven through the injectable
 * {@link RingBarrierState.DemandSource} test seam. See {@code assets/docs/ADVANCED_MODE_ASC3.md}.
 */
class RingBarrierStateTest {

  /** Canned detector/ped demand keyed by circuit index. */
  static final class Demand implements RingBarrierState.DemandSource {
    final Map<Integer, TrafficSignalSensorSummary> summaries = new HashMap<>();
    final Map<Integer, Boolean> peds = new HashMap<>();

    Demand veh(int circuit, int through, int left, int right) {
      summaries.put(circuit, new TrafficSignalSensorSummary(
          through, 0, 0, 0, 0, left, 0, 0, 0, 0, 0, 0, 0, 0, 0, right, 0, 0, 0, 0));
      return this;
    }

    Demand ped(int circuit) {
      peds.put(circuit, true);
      return this;
    }

    @Override
    public TrafficSignalSensorSummary summaryForCircuit(int ci) {
      return summaries.get(ci);
    }

    @Override
    public boolean pedestrianRequest(int ci) {
      return peds.getOrDefault(ci, false);
    }
  }

  static TrafficSignalControllerCircuits circuits(int n) {
    TrafficSignalControllerCircuits c = new TrafficSignalControllerCircuits();
    for (int i = 0; i < n; i++) {
      c.addCircuit(new TrafficSignalControllerCircuit());
    }
    return c;
  }

  /** Default 8-phase plan with through phases 2 (circuit 0) and 6 (circuit 1) enabled. */
  static TrafficSignalProgrammedPhasePlan twoThroughPlan() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    enable(plan, 2, 0);
    enable(plan, 6, 1);
    return plan;
  }

  static void enable(TrafficSignalProgrammedPhasePlan plan, int phase, int circuit) {
    TrafficSignalProgrammedPhase p = plan.getPhase(phase);
    p.setCircuitIndex(circuit);
    p.setEnabled(true);
  }

  static final TrafficSignalControllerOverlaps NO_OVERLAPS = new TrafficSignalControllerOverlaps();

  @Test
  @DisplayName("harness: a called through phase is served green")
  void servesCalledPhase() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = twoThroughPlan();
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand d = new Demand().veh(0, 3, 0, 0).veh(1, 3, 0, 0);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, d);

    ServedMovement m1 = rb.getLastServed(1);
    ServedMovement m2 = rb.getLastServed(2);
    assertNotNull(m1);
    assertNotNull(m2);
    assertEquals(2, m1.phaseNumber);
    assertEquals(VehInterval.GREEN, m1.vehicle);
    assertEquals(6, m2.phaseNumber);
    assertEquals(VehInterval.GREEN, m2.vehicle);
  }
}
