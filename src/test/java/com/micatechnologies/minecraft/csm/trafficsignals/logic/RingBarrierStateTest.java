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

  @Test
  @DisplayName("PPLT FYA: permissive flash clears concurrently with the opposing through, not late")
  void fyaClearsConcurrentlyWithOpposingThrough() {
    // Phase 1 is a PPLT FYA left (permissive phase 6); it is never called, so its head is driven
    // only by the opposing through (phase 6). The flash must clear WITH phase 6 — flashing yellow
    // while it is green, solid yellow while it is yellow, red when it is red — so both reach red
    // together (rather than the flash lingering and clearing a whole yellow interval late).
    net.minecraft.util.math.BlockPos fyaLens = new net.minecraft.util.math.BlockPos(200, 0, 0);
    net.minecraft.util.math.BlockPos arrow = new net.minecraft.util.math.BlockPos(201, 0, 0);
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 1, 0); // FYA left head on circuit 0 (never called -> permissive only)
    plan.getPhase(1).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(1).setPermissivePhase(6);
    enable(plan, 6, 1); // opposing through (drives the permissive flash)
    enable(plan, 4, 2); // barrier B: conflict that ends phase 6
    for (int n : new int[] {6, 4}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setMinGreen(20L);
      p.setPassage(10L);
      p.setYellow(20L);
      p.setRedClear(20L);
    }
    TrafficSignalControllerCircuits ckts = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
    c0.getFlashingLeftSignals().add(fyaLens);
    c0.getLeftSignals().add(arrow);
    ckts.addCircuit(c0);
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 1 (phase 6)
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 2 (phase 4)

    Demand both = new Demand().veh(1, 1, 0, 0).veh(2, 1, 0, 0);  // phase 6 + phase 4 call
    Demand drop6 = new Demand().veh(2, 1, 0, 0);                 // phase 6 gapped, phase 4 waits

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, both);
    assertTrue(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens),
        "FYA flashes while the opposing through is green");

    rb.tick(plan, ckts, NO_OVERLAPS, 30L, drop6); // phase 6 -> yellow: FYA clears to solid yellow now
    assertTrue(rb.getLastAppliedPhase().getYellowSignals().contains(fyaLens),
        "FYA shows solid yellow WITH the opposing through's yellow, not after it");
    assertFalse(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens));
    assertFalse(rb.getLastAppliedPhase().getRedSignals().contains(fyaLens));

    rb.tick(plan, ckts, NO_OVERLAPS, 45L, drop6); // still within phase 6's yellow (30..50)
    assertTrue(rb.getLastAppliedPhase().getYellowSignals().contains(fyaLens));

    rb.tick(plan, ckts, NO_OVERLAPS, 55L, drop6); // phase 6 now red (50..70): FYA is red too
    assertTrue(rb.getLastAppliedPhase().getRedSignals().contains(fyaLens),
        "FYA is red once the opposing through is red — they cleared together");
    assertFalse(rb.getLastAppliedPhase().getYellowSignals().contains(fyaLens));
  }

  @Test
  @DisplayName("PPLT FYA: flash holds through the opposing clearance straight into protected green")
  void fyaHoldsFlashIntoProtectedGreen() {
    // Lag left: phase 6 (through) leads, phase 5 (FYA left, permissive phase 2) lags. While the
    // opposing through (2) and the lead through (6) clear, phase 5's protected green is imminent, so
    // its flash must HOLD (not run its own solid-yellow/red) and go flash -> protected green direct.
    net.minecraft.util.math.BlockPos fyaLens = new net.minecraft.util.math.BlockPos(210, 0, 0);
    net.minecraft.util.math.BlockPos arrow = new net.minecraft.util.math.BlockPos(211, 0, 0);
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    plan.setRing2Sequence(new int[] {6, 5, 7, 8}); // through 6 before left 5 (lag left)
    enable(plan, 5, 0); // FYA left head, circuit 0
    plan.getPhase(5).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(5).setPermissivePhase(2);
    enable(plan, 2, 1); // opposing/permissive through, circuit 1
    enable(plan, 6, 2); // lead through, circuit 2 (occupies ring 2 before phase 5)
    for (int n : new int[] {5, 2, 6}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setMinGreen(20L);
      p.setPassage(10L);
      p.setYellow(20L);
      p.setRedClear(20L);
    }
    TrafficSignalControllerCircuits ckts = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
    c0.getFlashingLeftSignals().add(fyaLens);
    c0.getLeftSignals().add(arrow);
    ckts.addCircuit(c0);
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 1 (phase 2)
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 2 (phase 6)

    Demand all = new Demand().veh(0, 0, 1, 0).veh(1, 1, 0, 0).veh(2, 1, 0, 0); // φ5 left + φ2 + φ6
    Demand dropThrus = new Demand().veh(0, 0, 1, 0);                           // φ5 left call only

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, all);
    assertTrue(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens),
        "phase 5 flashes permissively while phase 2 is green");

    rb.tick(plan, ckts, NO_OVERLAPS, 30L, dropThrus); // throughs -> yellow; φ5 protected imminent
    assertTrue(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens),
        "flash HOLDS through the opposing yellow (protected green imminent), no solid yellow");
    assertFalse(rb.getLastAppliedPhase().getYellowSignals().contains(fyaLens));

    rb.tick(plan, ckts, NO_OVERLAPS, 50L, dropThrus); // throughs -> red; flash still held
    assertTrue(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens),
        "flash holds through the opposing red clearance too");

    rb.tick(plan, ckts, NO_OVERLAPS, 70L, dropThrus); // throughs cleared -> phase 5 protected green
    assertEquals(5, rb.getLastServed(2).phaseNumber);
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(arrow),
        "phase 5 goes flash -> protected green arrow directly");
    assertTrue(rb.getLastAppliedPhase().getOffSignals().contains(fyaLens),
        "3-section lens is dark under the protected green");
  }

  /**
   * Shared skeleton for the compatible-green regressions: phases 1 (FYA left, circuit 0,
   * permissive 2), 2 (through, circuit 1, min recall) and 6 (through, circuit 2) on barrier A.
   * Asserts that a left-turn call clears ONLY the conflicting through (2), the compatible
   * adjacent through (6) holds green the whole way through the left's service and back, and the
   * left actually reaches its protected green (no dropped service).
   */
  private void assertCompanionHoldsThroughLeftService(TrafficSignalProgrammedPhasePlan plan,
      TrafficSignalControllerCircuits ckts,
      net.minecraft.util.math.BlockPos fyaLens, net.minecraft.util.math.BlockPos arrow) {
    RingBarrierState rb = new RingBarrierState();
    Demand noCall = new Demand();
    Demand leftCall = new Demand().veh(0, 0, 1, 0); // car in the left-turn lane (phase 1)

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, noCall); // recalls serve the through(s)
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(6, rb.getLastServed(2).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 20L, leftCall); // conflicting through 2 -> yellow
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle,
        "the opposing through (2) clears for the left-turn call");
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle,
        "the compatible adjacent through (6) must NOT clear for the left-turn call");
    assertTrue(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens),
        "FYA flash holds through the opposing through's clearance (protected green imminent)");

    rb.tick(plan, ckts, NO_OVERLAPS, 40L, leftCall); // through 2 -> red clearance
    assertEquals(VehInterval.RED, rb.getLastServed(1).vehicle);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle,
        "phase 6 holds green through the opposing through's red clearance");

    rb.tick(plan, ckts, NO_OVERLAPS, 60L, leftCall); // ring 1 wraps within the barrier -> left green
    assertEquals(1, rb.getLastServed(1).phaseNumber,
        "the called left must be served (not dropped at the barrier decision)");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle,
        "phase 6 holds green alongside the protected left (compatible pair)");
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(arrow),
        "protected green arrow is shown for the left");
    assertTrue(rb.getLastAppliedPhase().getOffSignals().contains(fyaLens));

    rb.tick(plan, ckts, NO_OVERLAPS, 80L, noCall); // left gapped out -> its own clearance
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 100L, noCall); // left red clearance
    rb.tick(plan, ckts, NO_OVERLAPS, 120L, noCall); // through 2 re-serves
    assertEquals(2, rb.getLastServed(1).phaseNumber, "through 2 resumes after the left");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
    assertEquals(6, rb.getLastServed(2).phaseNumber,
        "phase 6 was never cleared/re-served around the left-turn service");
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle);
    assertTrue(rb.getLastAppliedPhase().getFyaSignals().contains(fyaLens),
        "permissive flash resumes once the opposing through is green again");
  }

  /** Builds the circuits for the compatible-green regressions (FYA head on circuit 0). */
  private static TrafficSignalControllerCircuits fyaCircuits(
      net.minecraft.util.math.BlockPos fyaLens, net.minecraft.util.math.BlockPos arrow) {
    TrafficSignalControllerCircuits ckts = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
    c0.getFlashingLeftSignals().add(fyaLens);
    c0.getLeftSignals().add(arrow);
    ckts.addCircuit(c0);
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 1 (phase 2)
    ckts.addCircuit(new TrafficSignalControllerCircuit()); // circuit 2 (phase 6)
    return ckts;
  }

  @Test
  @DisplayName("a compatible through holds green while a left-turn call is served in the other ring")
  void companionThroughHoldsGreenThroughLeftService() {
    // Regression: a left call used to terminate EVERY green (global conflicting demand), forcing
    // the compatible adjacent through (6) to run green -> yellow -> red -> green around the left.
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 1, 0);
    plan.getPhase(1).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(1).setPermissivePhase(2);
    enable(plan, 2, 1);
    plan.getPhase(2).setRecallMode(TrafficSignalRecallMode.MINIMUM);
    enable(plan, 6, 2);
    plan.getPhase(6).setRecallMode(TrafficSignalRecallMode.MINIMUM);
    quickTiming(plan, 1);
    quickTiming(plan, 2);

    net.minecraft.util.math.BlockPos fyaLens = new net.minecraft.util.math.BlockPos(220, 0, 0);
    net.minecraft.util.math.BlockPos arrow = new net.minecraft.util.math.BlockPos(221, 0, 0);
    assertCompanionHoldsThroughLeftService(plan, fyaCircuits(fyaLens, arrow), fyaLens, arrow);
  }

  @Test
  @DisplayName("a dual-entry companion holds green while a left-turn call is served in the other ring")
  void dualEntryCompanionHoldsGreenThroughLeftService() {
    // Regression (in-game report): phase 6 was dual-entered alongside through 2; a call on the FYA
    // left (1) made 6 run green -> yellow -> red -> immediately green again. A dual-entry companion
    // now terminates by the same conflict rule as any phase, so it simply holds green.
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 1, 0);
    plan.getPhase(1).setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
    plan.getPhase(1).setPermissivePhase(2);
    enable(plan, 2, 1);
    plan.getPhase(2).setRecallMode(TrafficSignalRecallMode.MINIMUM);
    enable(plan, 6, 2);
    plan.getPhase(6).setDualEntry(true); // no recall/call of its own — companions the barrier
    quickTiming(plan, 1);
    quickTiming(plan, 2);

    net.minecraft.util.math.BlockPos fyaLens = new net.minecraft.util.math.BlockPos(230, 0, 0);
    net.minecraft.util.math.BlockPos arrow = new net.minecraft.util.math.BlockPos(231, 0, 0);
    assertCompanionHoldsThroughLeftService(plan, fyaCircuits(fyaLens, arrow), fyaLens, arrow);
  }

  @Test
  @DisplayName("coordination: a dual-entry companion serves the side street despite coord demand")
  void dualEntryCompanionServesUnderCoordination() {
    // In-game report: coordinated plan with coord throughs 2/6 (barrier A) and side street 4/8
    // (barrier B), DE on 4 and 8. A call on 4 opened its window and 4 went green, but 8 never
    // dual-entered — the coordinated phases are ALWAYS called, and that standing cross-barrier
    // demand blocked the companion from entering (and would have stripped it right back off).
    // A DE companion must ride with its companion through cross-barrier demand and clear WITH it.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    enable(plan, 2, 0); // coord through, barrier A, ring 1
    enable(plan, 6, 1); // coord through, barrier A, ring 2
    enable(plan, 4, 2); // side-street through, barrier B, ring 1
    enable(plan, 8, 3); // side-street through, barrier B, ring 2 — dual entry
    plan.getPhase(4).setDualEntry(true);
    plan.getPhase(8).setDualEntry(true);
    for (int n : new int[] {2, 4, 6, 8}) {
      quickTiming(plan, n);
    }
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    co.setMode(TrafficSignalCoordinationMode.COORDINATED); // coord phases default to {2, 6}
    co.setCycleLength(1800L);
    co.setSplit(2, 1000L);
    co.setSplit(6, 1000L);
    co.setSplit(4, 800L);
    co.setSplit(8, 800L);
    TrafficSignalControllerCircuits ckts = circuits(4);
    Demand carOn4 = new Demand().veh(2, 1, 0, 0); // one car waiting on the side street (phase 4)
    Demand none = new Demand();

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, carOn4); // window for 4 closed: coord phases serve
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(6, rb.getLastServed(2).phaseNumber);

    rb.tick(plan, ckts, NO_OVERLAPS, 1010L, carOn4); // 4's window opens -> coord phases clear
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle);
    rb.tick(plan, ckts, NO_OVERLAPS, 1030L, carOn4); // yellow -> red
    rb.tick(plan, ckts, NO_OVERLAPS, 1050L, carOn4); // red clears -> cross -> phase 4 green
    assertEquals(4, rb.getLastServed(1).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 1070L, carOn4); // companion 8 dual-enters alongside 4
    assertNotNull(rb.getLastServed(2), "phase 8 must dual-enter with 4 (was left dark)");
    assertEquals(8, rb.getLastServed(2).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 1090L, carOn4); // standing coord demand must NOT strip it
    assertEquals(8, rb.getLastServed(2).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle,
        "the DE companion rides through the coordinated phases' standing cross-barrier calls");

    rb.tick(plan, ckts, NO_OVERLAPS, 1120L, none); // side-street car gone: pair clears TOGETHER
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle, "phase 4 gaps out");
    assertEquals(VehInterval.YELLOW, rb.getLastServed(2).vehicle,
        "the DE companion clears concurrently with its companion");

    rb.tick(plan, ckts, NO_OVERLAPS, 1140L, none); // red clearance
    rb.tick(plan, ckts, NO_OVERLAPS, 1160L, none); // cross back -> coordinated phases resume
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(6, rb.getLastServed(2).phaseNumber);
  }

  @Test
  @DisplayName("max green times from the first conflicting call, not from green start")
  void maxGreenTimesFromConflictingCall() {
    // Regression: max-out used to time from green start even with zero conflicting demand, so a
    // recall/rest phase cycled green -> yellow -> red -> green every max-green interval for nobody.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // continuously actuated through
    enable(plan, 4, 1); // barrier B — supplies the conflicting call later
    TrafficSignalProgrammedPhase p2 = plan.getPhase(2);
    p2.setMinGreen(20L);
    p2.setPassage(10L);
    p2.setMaxGreen(50L);
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand unopposed = new Demand().veh(0, 1, 0, 0);               // phase 2 demand only
    Demand opposed = new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0); // + conflicting phase 4 call

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, unopposed);
    assertEquals(2, rb.getLastServed(1).phaseNumber);

    rb.tick(plan, ckts, NO_OVERLAPS, 60L, unopposed); // 60 > max 50, but nothing conflicts
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle,
        "an unopposed green must rest, not max out for nobody");

    rb.tick(plan, ckts, NO_OVERLAPS, 100L, opposed); // conflicting call registers -> max starts now
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);

    rb.tick(plan, ckts, NO_OVERLAPS, 149L, opposed); // 49 < 50 since the call — still green
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle,
        "max green is measured from the conflicting call's registration");

    rb.tick(plan, ckts, NO_OVERLAPS, 150L, opposed); // 50 ticks since the call -> max out
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle,
        "the phase maxes out one max-green after the conflicting call arrived");
  }

  @Test
  @DisplayName("LOCK: a latched call survives the vehicle leaving and is discharged by service")
  void lockedCallSurvivesDetectionDrop() {
    // Phase 2 (barrier A) has LOCK set. While phase 4 (barrier B) is being served, a car touches
    // phase 2's zone and leaves. The latched call must still terminate phase 4 and bring up
    // phase 2; serving phase 2 must discharge the latch (no phantom re-service afterward).
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0);
    plan.getPhase(2).setLockCall(true);
    enable(plan, 4, 1);
    quickTiming(plan, 2);
    quickTiming(plan, 4);
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand onlyPh4 = new Demand().veh(1, 1, 0, 0);
    Demand blip = new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0); // car touches phase 2's zone
    Demand nothing = new Demand();                              // ...and leaves again

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, onlyPh4);  // phase 4 (barrier B) served
    assertEquals(4, rb.getLastServed(1).phaseNumber);
    rb.tick(plan, ckts, NO_OVERLAPS, 10L, blip);    // momentary detection on phase 2 -> latched
    rb.tick(plan, ckts, NO_OVERLAPS, 30L, nothing); // car gone; latch must keep the call alive
    assertEquals(VehInterval.YELLOW, rb.getLastServed(1).vehicle,
        "phase 4 must terminate for the LATCHED phase-2 call even though the car left");
    rb.tick(plan, ckts, NO_OVERLAPS, 50L, nothing); // yellow -> red
    rb.tick(plan, ckts, NO_OVERLAPS, 70L, nothing); // red clears -> cross -> serve phase 2
    assertEquals(2, rb.getLastServed(1).phaseNumber,
        "the latched call is served with no live detection at all");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);

    // Service discharged the latch: a later phase-4 call must not find a phantom phase-2 call.
    rb.tick(plan, ckts, NO_OVERLAPS, 100L, onlyPh4); // phase 2 terminates for the real ph4 call
    rb.tick(plan, ckts, NO_OVERLAPS, 120L, onlyPh4);
    rb.tick(plan, ckts, NO_OVERLAPS, 140L, onlyPh4); // phase 4 green again
    assertEquals(4, rb.getLastServed(1).phaseNumber);
    rb.tick(plan, ckts, NO_OVERLAPS, 200L, onlyPh4); // no conflict: rests in green on 4
    assertEquals(4, rb.getLastServed(1).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle,
        "no phantom latched call remains after phase 2 was served");
  }

  @Test
  @DisplayName("non-lock (presence): a dropped call is forgotten and the phase is not served")
  void presenceCallDropsWhenVehicleLeaves() {
    // Identical to the LOCK test but without the flag: the blip on phase 2's zone leaves no
    // latch, so phase 4 simply rests in green and phase 2 is never served.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0); // no LOCK
    enable(plan, 4, 1);
    quickTiming(plan, 2);
    quickTiming(plan, 4);
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand onlyPh4 = new Demand().veh(1, 1, 0, 0);
    Demand blip = new Demand().veh(0, 1, 0, 0).veh(1, 1, 0, 0);
    Demand nothing = new Demand();

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, onlyPh4);
    rb.tick(plan, ckts, NO_OVERLAPS, 10L, blip);
    rb.tick(plan, ckts, NO_OVERLAPS, 30L, nothing);
    rb.tick(plan, ckts, NO_OVERLAPS, 70L, nothing);
    assertEquals(4, rb.getLastServed(1).phaseNumber,
        "a presence call that dropped is forgotten — phase 4 keeps the green");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
  }

  @Test
  @DisplayName("rest-in-walk recalls WALK while holding green after being called (not don't-walk)")
  void restInWalkRecyclesWalkWhenHoldingGreen() {
    // A rest-in-walk phase entered by a call (not the no-demand rest path) used to run WALK -> FDW
    // -> DON'T WALK once and then sit in don't-walk while the vehicle stayed green. It should recall
    // WALK while it holds green with nothing else calling.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0);
    enable(plan, 4, 1); // present but never called -> no conflict, phase 2 rests on green
    TrafficSignalProgrammedPhase p2 = plan.getPhase(2);
    p2.setRestInWalk(true);
    p2.setPedRecall(true);
    p2.setWalk(40L);
    p2.setPedClear(60L);
    p2.setMinGreen(20L);
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand ph2 = new Demand().veh(0, 1, 0, 0); // phase 2 called (served by a call, not the rest path)

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, ph2);
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    // Well past walk (40) + ped clear (60): without the rest-in-walk recall this would be DONT_WALK.
    rb.tick(plan, ckts, NO_OVERLAPS, 200L, ph2);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle, "phase 2 still holding green");
    assertEquals(PedInterval.WALK, rb.getLastServed(1).pedestrian,
        "rest-in-walk recalls WALK while the phase holds green, not resting in don't-walk");
  }

  @Test
  @DisplayName("rest-in-walk: an in-progress ped clearance is not cut off if the conflict drops")
  void restInWalkDoesNotCutOffClearance() {
    // Rest-in-walk phase 2 is holding WALK; a conflicting call starts its FDW clearance, then the
    // conflict drops mid-clearance. The FDW must complete (not snap back to WALK), then recycle.
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    enable(plan, 2, 0);
    enable(plan, 4, 1); // conflict source (appears, then drops)
    TrafficSignalProgrammedPhase p2 = plan.getPhase(2);
    p2.setRestInWalk(true);
    p2.setPedRecall(true);
    p2.setWalk(40L);
    p2.setPedClear(60L);
    p2.setMinGreen(20L);
    quickTiming(plan, 4);
    TrafficSignalControllerCircuits ckts = circuits(2);
    Demand rest = new Demand();                    // nothing calling -> phase 2 rests in WALK
    Demand conflict = new Demand().veh(1, 1, 0, 0); // phase 4 calls -> phase 2 starts FDW clearance

    rb.tick(plan, ckts, NO_OVERLAPS, 0L, rest);
    assertEquals(PedInterval.WALK, rb.getLastServed(1).pedestrian);

    rb.tick(plan, ckts, NO_OVERLAPS, 10L, conflict); // conflict -> FDW clearance begins
    assertEquals(PedInterval.FDW, rb.getLastServed(1).pedestrian, "clearance starts on the conflict");

    rb.tick(plan, ckts, NO_OVERLAPS, 30L, rest); // conflict drops mid-clearance
    assertEquals(PedInterval.FDW, rb.getLastServed(1).pedestrian,
        "an in-progress clearance is NOT cut off when the conflict drops");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle, "vehicle stays green through it");

    rb.tick(plan, ckts, NO_OVERLAPS, 75L, rest); // clearance done (walk 40 + ped clear 60) -> recycle
    assertEquals(PedInterval.WALK, rb.getLastServed(1).pedestrian,
        "once the clearance completes with no conflict, the walk recycles");
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
  }
}
