package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.PedInterval;
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
      return veh(circuit, through, left, right, 0);
    }

    Demand veh(int circuit, int through, int left, int right, int bike) {
      summaries.put(circuit, new TrafficSignalSensorSummary(
          through, 0, 0, 0, 0, left, 0, 0, 0, 0, bike, 0, 0, 0, 0, right, 0, 0, 0, 0));
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

  @Test
  @DisplayName("bike min green holds the phase green past its (short) minimum green")
  void bikeMinGreenHolds() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    enable(plan, 2, 0); // through, circuit 0 (barrier A)
    enable(plan, 4, 1); // through, circuit 1 (barrier B) — provides conflicting demand
    TrafficSignalProgrammedPhase p2 = plan.getPhase(2);
    p2.setMinGreen(20L);
    p2.setPassage(10L);
    p2.setBikeMinGreen(200L);
    TrafficSignalControllerCircuits ckts = circuits(2);
    // At green start, circuit 0 has both a through call (to serve phase 2) and a bike call;
    // circuit 1 has a conflicting through call. After it greens, the through call drops so the
    // phase gaps out on the vehicle side — only the bike minimum green holds it.
    Demand start = new Demand().veh(0, 1, 0, 0, 5).veh(1, 1, 0, 0);
    Demand gapped = new Demand().veh(0, 0, 0, 0, 5).veh(1, 1, 0, 0);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, start);   // phase 2 greens with a bike call captured
    rb.tick(plan, ckts, NO_OVERLAPS, 60L, gapped); // past min green (20) + gap (10), before bike (200)
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle,
        "bike minimum green should hold the phase green well past its 20-tick min green");

    rb.tick(plan, ckts, NO_OVERLAPS, 210L, gapped); // past the 200-tick bike min green
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle,
        "once bike min green is met the gapped-out phase terminates under conflict");
  }

  @Test
  @DisplayName("soft recall: controller rests on the soft-recall phase when nothing calls")
  void softRecallRest() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    // Clear the default coordinated phases (2 & 6) so the soft-recall preference is what selects
    // the rest phase rather than the coordinated-phase preference that runs first.
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0);
    enable(plan, 4, 1);
    plan.getPhase(4).setRecallMode(TrafficSignalRecallMode.SOFT);
    TrafficSignalControllerCircuits ckts = circuits(2);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, new Demand()); // no demand anywhere

    assertEquals(4, rb.getLastServed(1).phaseNumber,
        "with nothing calling, the ring rests on its soft-recall phase, not the first phase");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
  }

  @Test
  @DisplayName("rest in walk: WALK is held while resting on a rest-in-walk phase")
  void restInWalkHoldsWalk() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    enable(plan, 2, 0);
    plan.getPhase(2).setRestInWalk(true);
    TrafficSignalControllerCircuits ckts = circuits(1);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, new Demand());
    rb.tick(plan, ckts, NO_OVERLAPS, 500L, new Demand()); // well past a normal walk + ped clear

    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(PedInterval.WALK, rb.getLastServed(1).pedestrian,
        "rest in walk holds WALK indefinitely while resting");
  }

  @Test
  @DisplayName("dual entry: an uncalled ring companions the other ring's served barrier")
  void dualEntry() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // ring 1, barrier A, circuit 0
    enable(plan, 6, 1); // ring 2, barrier A, circuit 1
    plan.getPhase(6).setDualEntry(true);
    TrafficSignalControllerCircuits ckts = circuits(2);
    // Only circuit 0 calls (phase 2). Phase 6 has no call of its own, so it should dual-enter.
    Demand d = new Demand().veh(0, 3, 0, 0);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, d);

    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertNotNull(rb.getLastServed(2), "ring 2 should serve a dual-entry companion, not be dark");
    assertEquals(6, rb.getLastServed(2).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle);
  }

  @Test
  @DisplayName("conditional service: a lagging left re-serves once on a fresh call before crossing")
  void conditionalService() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 1, 0); // ring 1, barrier A, left, conditional-service
    enable(plan, 2, 0); // ring 1, barrier A, through
    enable(plan, 4, 1); // ring 1, barrier B — supplies conflicting demand so phases terminate
    for (int n : new int[] {1, 2}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setMinGreen(20L);
      p.setPassage(10L);
      p.setYellow(20L);
      p.setRedClear(20L);
    }
    plan.getPhase(1).setConditionalService(true);
    TrafficSignalControllerCircuits ckts = circuits(2);

    // Phase 1 (left) runs first, then gaps out; phase 2 runs; then the left re-acquires a call and
    // is conditionally re-served before the barrier crosses.
    Demand left = new Demand().veh(0, 1, 1, 0).veh(1, 1, 0, 0);   // left + through demand, conflict
    Demand noLeft = new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0); // left cleared, phase 2 demand
    Demand leftBack = new Demand().veh(0, 0, 1, 0).veh(1, 1, 0, 0); // through gone, left returns

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, left);   // serve phase 1
    assertEquals(1, rb.getLastServed(1).phaseNumber);
    rb.tick(plan, ckts, NO_OVERLAPS, 40L, noLeft); // phase 1 gaps out -> yellow
    rb.tick(plan, ckts, NO_OVERLAPS, 70L, noLeft); // yellow -> red
    rb.tick(plan, ckts, NO_OVERLAPS, 100L, noLeft); // red clears -> phase 2 served
    assertEquals(2, rb.getLastServed(1).phaseNumber, "phase 2 should run after phase 1 clears");
    rb.tick(plan, ckts, NO_OVERLAPS, 140L, leftBack); // phase 2 gaps out (left back) -> yellow
    rb.tick(plan, ckts, NO_OVERLAPS, 170L, leftBack); // yellow -> red
    rb.tick(plan, ckts, NO_OVERLAPS, 200L, leftBack); // red clears -> conditional re-service of φ1

    assertEquals(1, rb.getLastServed(1).phaseNumber,
        "the conditional-service left should be re-served once on its fresh call");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
  }

  @Test
  @DisplayName("overlap lag (trailing) green holds the head green after the included phase ends")
  void overlapTrailingGreen() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // through, circuit 0 — the overlap's included phase
    enable(plan, 4, 1); // barrier B — conflicting demand so phase 2 terminates
    TrafficSignalProgrammedPhase p2 = plan.getPhase(2);
    p2.setMinGreen(20L);
    p2.setPassage(10L);
    p2.setYellow(60L);
    p2.setRedClear(20L);

    net.minecraft.util.math.BlockPos rightHead = new net.minecraft.util.math.BlockPos(50, 0, 0);
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.getRightSignals().add(rightHead);
    TrafficSignalControllerCircuits ckts = new TrafficSignalControllerCircuits();
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 0
    ckts.addCircuit(c1);                                   // circuit 1 (overlap output)

    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(1);
    ov.setOutputMovement(TrafficSignalPhaseMovement.RIGHT);
    ov.setIncludedPhases(new int[] {2});
    ov.setTrailGreen(40L);
    plan.getVehicleOverlaps().add(ov);

    Demand go = new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0);   // phase 2 green, phase 4 conflict
    Demand stop = new Demand().veh(0, 0, 0, 0).veh(1, 1, 0, 0); // phase 2 gaps out

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, go);
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(rightHead),
        "overlap head is green while its included phase is green");

    rb.tick(plan, ckts, NO_OVERLAPS, 30L, stop); // phase 2 -> yellow; 30 ticks < 40 trail -> green
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(rightHead),
        "overlap head holds green (trailing) after the included phase leaves green");

    rb.tick(plan, ckts, NO_OVERLAPS, 50L, stop); // 50 ticks since last green > 40 trail -> not green
    assertFalse(rb.getLastAppliedPhase().getGreenSignals().contains(rightHead),
        "overlap head drops out of green once the trailing window expires");
  }

  @Test
  @DisplayName("-GRN/YEL overlap is forced red while a modifier phase is green")
  void minusGreenYellowOverlap() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // ring 1 — overlap's included phase
    enable(plan, 6, 1); // ring 2 — the modifier phase (runs concurrently on barrier A)
    net.minecraft.util.math.BlockPos rightHead = new net.minecraft.util.math.BlockPos(60, 0, 0);
    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    c2.getRightSignals().add(rightHead);
    TrafficSignalControllerCircuits ckts = circuits(2);
    ckts.addCircuit(c2); // circuit 2 — overlap output

    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(2);
    ov.setOutputMovement(TrafficSignalPhaseMovement.RIGHT);
    ov.setIncludedPhases(new int[] {2});
    ov.setType(TrafficSignalOverlapType.MINUS_GREEN_YELLOW);
    ov.setModifierPhases(new int[] {6});
    plan.getVehicleOverlaps().add(ov);

    // Phases 2 and 6 both run green concurrently; included phase 2 would green the overlap, but
    // modifier phase 6 forces it red.
    Demand d = new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0);
    rb.tick(plan, ckts, NO_OVERLAPS, 0L, d);

    assertFalse(rb.getLastAppliedPhase().getGreenSignals().contains(rightHead),
        "overlap is forced red while its modifier phase (6) is green");
    assertTrue(rb.getLastAppliedPhase().getRedSignals().contains(rightHead));
  }

  @Test
  @DisplayName("pedestrian overlap walks a ped head with its included phase")
  void pedestrianOverlap() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0);
    plan.getPhase(2).setPedRecall(true); // phase 2 serves a WALK each time it greens
    net.minecraft.util.math.BlockPos pedHead = new net.minecraft.util.math.BlockPos(70, 0, 0);
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.getPedestrianSignals().add(pedHead);
    TrafficSignalControllerCircuits ckts = circuits(1);
    ckts.addCircuit(c1); // circuit 1 — overlap output

    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(1);
    ov.setOutputMovement(TrafficSignalPhaseMovement.PED);
    ov.setIncludedPhases(new int[] {2});
    plan.getVehicleOverlaps().add(ov);

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, new Demand().veh(0, 1, 0, 0));

    assertTrue(rb.getLastAppliedPhase().getWalkSignals().contains(pedHead),
        "ped overlap should WALK while its included phase is walking");
  }

  @Test
  @DisplayName("overlap lead (advance) green greens the head before its included phase, in clearance")
  void overlapLeadGreen() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 1, 0); // left, runs first
    enable(plan, 2, 0); // through (the overlap's included phase), runs right after phase 1
    TrafficSignalProgrammedPhase p1 = plan.getPhase(1);
    p1.setMinGreen(20L);
    p1.setPassage(10L);
    p1.setYellow(20L);
    p1.setRedClear(40L);

    net.minecraft.util.math.BlockPos rightHead = new net.minecraft.util.math.BlockPos(80, 0, 0);
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.getRightSignals().add(rightHead);
    TrafficSignalControllerCircuits ckts = circuits(1);
    ckts.addCircuit(c1); // circuit 1 — overlap output

    TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
    ov.setEnabled(true);
    ov.setOutputCircuitIndex(1);
    ov.setOutputMovement(TrafficSignalPhaseMovement.RIGHT);
    ov.setIncludedPhases(new int[] {2});
    ov.setLeadGreen(20L);
    plan.getVehicleOverlaps().add(ov);

    Demand start = new Demand().veh(0, 1, 1, 0); // phase 1 (left) served, phase 2 (through) called
    Demand gapped = new Demand().veh(0, 1, 0, 0); // left cleared; phase 2 still called

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, start);   // phase 1 green
    rb.tick(plan, ckts, NO_OVERLAPS, 30L, gapped); // phase 1 -> yellow
    rb.tick(plan, ckts, NO_OVERLAPS, 50L, gapped); // phase 1 -> red clearance (ends at 90)
    rb.tick(plan, ckts, NO_OVERLAPS, 60L, gapped); // 30 ticks before phase 2 green > 20 lead -> red
    assertFalse(rb.getLastAppliedPhase().getGreenSignals().contains(rightHead),
        "overlap is not yet leading green (outside the lead window)");

    rb.tick(plan, ckts, NO_OVERLAPS, 75L, gapped); // 15 ticks before phase 2 green <= 20 lead -> green
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(rightHead),
        "overlap leads green during the clearance before its included phase greens");
  }

  /** Sets short, uniform interval timing on a phase so crossings happen at predictable ticks. */
  static void quickTiming(TrafficSignalProgrammedPhasePlan plan, int phase) {
    TrafficSignalProgrammedPhase p = plan.getPhase(phase);
    p.setMinGreen(20L);
    p.setPassage(10L);
    p.setYellow(20L);
    p.setRedClear(20L);
  }

  @Test
  @DisplayName("barrier crossing: phase 4 (barrier B) is served after phase 2, then wraps back")
  void crossesBarrierAndWrapsAround() {
    // Direct regression for the bug where the controller sat on barrier A forever and never
    // served phase 4. Phases 2 (barrier A) and 4 (barrier B) are both on ring 1.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // barrier A, circuit 0
    enable(plan, 4, 1); // barrier B, circuit 1
    quickTiming(plan, 2);
    quickTiming(plan, 4);
    TrafficSignalControllerCircuits ckts = circuits(2);

    Demand ph2gap = new Demand().veh(0, 0, 0, 0).veh(1, 1, 0, 0); // phase 2 gapped, phase 4 calling
    Demand ph4gap = new Demand().veh(0, 1, 0, 0).veh(1, 0, 0, 0); // phase 4 gapped, phase 2 calling

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0));
    assertEquals(2, rb.getLastServed(1).phaseNumber, "phase 2 (barrier A) greens first");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 40L, ph2gap);  // min green + gap met, conflict -> yellow
    rb.tick(plan, ckts, NO_OVERLAPS, 60L, ph2gap);  // yellow -> red clearance
    rb.tick(plan, ckts, NO_OVERLAPS, 80L, ph2gap);  // red clears -> CROSS to barrier B
    assertEquals(4, rb.getLastServed(1).phaseNumber, "phase 4 (barrier B) must be served after crossing");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 120L, ph4gap); // phase 4 min green + gap, conflict -> yellow
    rb.tick(plan, ckts, NO_OVERLAPS, 140L, ph4gap); // yellow -> red
    rb.tick(plan, ckts, NO_OVERLAPS, 160L, ph4gap); // red clears -> WRAP back to barrier A
    assertEquals(2, rb.getLastServed(1).phaseNumber, "controller wraps back to phase 2 after phase 4");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
  }

  @Test
  @DisplayName("second-barrier-only demand is served (Bug 1 regression: no stuck all-red)")
  void servesSecondBarrierWhenOnlyDemand() {
    // Only phase 4 (barrier B) calls; phase 2 (barrier A) has no demand. The old fillIdleRing broke
    // at the first barrier-A phase in the sequence and never reached phase 4 -> stuck all-red.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // barrier A, circuit 0 — never called
    enable(plan, 4, 1); // barrier B, circuit 1 — sole demand
    quickTiming(plan, 2);
    quickTiming(plan, 4);
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand onlyPh4 = new Demand().veh(1, 1, 0, 0);

    for (long t = 0; t <= 30; t += 10) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, onlyPh4);
      assertEquals(4, rb.getLastServed(1).phaseNumber,
          "phase 4 must be served when it is the only demand (tick " + t + ")");
    }
  }

  @Test
  @DisplayName("rest-in-walk runs pedestrian clearance (FDW) before yielding, not WALK->DONT_WALK")
  void restInWalkRunsClearanceBeforeYielding() {
    // Regression: a phase resting on WALK (soft recall + rest-in-walk) used to snap its ped signal
    // straight from WALK to DON'T WALK when a conflicting call arrived, skipping FDW clearance.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // rests here: soft recall + rest-in-walk hold WALK
    enable(plan, 4, 1); // barrier B: supplies the conflicting call
    TrafficSignalProgrammedPhase p2 = plan.getPhase(2);
    p2.setRecallMode(TrafficSignalRecallMode.SOFT);
    p2.setPedRecall(true);
    p2.setRestInWalk(true);
    p2.setWalk(40L);
    p2.setPedClear(60L);
    p2.setMinGreen(20L);
    p2.setPassage(10L);
    p2.setYellow(20L);
    p2.setRedClear(20L);
    quickTiming(plan, 4);
    TrafficSignalControllerCircuits ckts = circuits(2);

    // Resting on phase 2 with no demand anywhere: WALK is held.
    rb.tick(plan, ckts, NO_OVERLAPS, 0L, new Demand());
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
    assertEquals(PedInterval.WALK, rb.getLastServed(1).pedestrian, "rest-in-walk holds WALK");

    Demand ph4 = new Demand().veh(1, 1, 0, 0); // conflicting call on phase 4 (barrier B)

    // As soon as the conflicting call appears the walk must begin clearance (FDW), NOT jump to
    // don't-walk, and the vehicle must stay green through the clearance.
    rb.tick(plan, ckts, NO_OVERLAPS, 10L, ph4);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle,
        "vehicle stays green while pedestrian clearance runs");
    assertEquals(PedInterval.FDW, rb.getLastServed(1).pedestrian,
        "WALK enters FDW clearance rather than snapping to don't-walk");

    // Clearance still running partway through (walk 40 + ped clear 60).
    rb.tick(plan, ckts, NO_OVERLAPS, 65L, ph4);
    assertEquals(PedInterval.FDW, rb.getLastServed(1).pedestrian, "FDW persists for the full clearance");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);

    // Only after clearance completes may the phase yield (vehicle leaves green).
    rb.tick(plan, ckts, NO_OVERLAPS, 75L, ph4);
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle,
        "vehicle yields only after the pedestrian clearance has finished");
  }
}
