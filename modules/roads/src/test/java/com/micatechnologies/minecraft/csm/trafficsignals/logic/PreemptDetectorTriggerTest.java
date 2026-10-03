package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A preempt triggered by its circuit's preempt detectors (DET) rather than a sensor zone: it fires
 * on a detector call, ignores vehicles in the zone, keeps the entry clearance, holds the dwell
 * while the call stands, and round-trips through NBT.
 */
class PreemptDetectorTriggerTest {

  private final BlockPos head2 = new BlockPos(400, 0, 0);
  private final BlockPos head4 = new BlockPos(401, 0, 0);

  /** Through phases 2 (circuit 0) and 4 (circuit 1); an emergency preempt dwelling on 4. */
  private TrafficSignalProgrammedPhasePlan plan(boolean onDetectors) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.getCoordination().setCoordinatedPhases(new int[0]);
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 4, 1);
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    pe.setEnabled(true);
    pe.setType(TrafficSignalPreemptType.EMERGENCY);
    pe.setTriggerCircuitIndex(1);
    pe.setTriggerMovement(TrafficSignalPhaseMovement.THROUGH);
    pe.setTriggerOnDetectors(onDetectors);
    pe.setDwellPhases(new int[] {4});
    pe.setMinDwell(0L);
    plan.getPreempts().add(pe);
    return plan;
  }

  private TrafficSignalControllerCircuits circuits() {
    TrafficSignalControllerCircuits ckts = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
    c0.getThroughSignals().add(head2);
    ckts.addCircuit(c0);
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.getThroughSignals().add(head4);
    ckts.addCircuit(c1);
    return ckts;
  }

  /** Main street (circuit 0) waiting; circuit 1 with the given vehicles and detector calls. */
  private static Demand demand(int circuit1Vehicles, int detectorCalls) {
    Demand d = new Demand().veh(0, 1, 0, 0).veh(1, circuit1Vehicles, 0, 0);
    d.summaries.get(1).withPreemptDetectorCalls(detectorCalls);
    return d;
  }

  @Test
  @DisplayName("a detector call preempts: entry yellow, red, then the emergency approach green")
  void detectorCallPreempts() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = plan(true);
    TrafficSignalControllerCircuits ckts = circuits();
    Demand quiet = demand(0, 0);
    Demand truck = demand(0, 1);

    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 0L, quiet);
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(head2), "main street green");

    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 10L, truck);
    assertTrue(rb.getLastAppliedPhase().getYellowSignals().contains(head2),
        "the main street clears with a yellow, never straight to red");
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 85L, truck);
    assertTrue(rb.getLastAppliedPhase().getRedSignals().contains(head2), "then red");
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 125L, truck);
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(head4),
        "the emergency approach dwells green");

    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 400L, truck);
    assertTrue(rb.getLastAppliedPhase().getGreenSignals().contains(head4),
        "the dwell holds while the detector still has a call");

    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 410L, quiet);
    assertTrue(rb.getLastAppliedPhase().getYellowSignals().contains(head4),
        "the call drops: the dwell clears yellow on the way out");
  }

  @Test
  @DisplayName("a DET preempt ignores ordinary vehicles in the trigger circuit's sensor zone")
  void detectorPreemptIgnoresZoneVehicles() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = plan(true);
    TrafficSignalControllerCircuits ckts = circuits();
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 0L, demand(0, 0));
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 10L, demand(5, 0));
    assertFalse(rb.getLastAppliedPhase().getYellowSignals().contains(head2),
        "cars waiting on the side street do not preempt the main street");
  }

  @Test
  @DisplayName("a zone-triggered preempt ignores detector calls")
  void zonePreemptIgnoresDetectors() {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = plan(false);
    TrafficSignalControllerCircuits ckts = circuits();
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 0L, demand(0, 0));
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, 10L, demand(0, 1));
    assertFalse(rb.getLastAppliedPhase().getYellowSignals().contains(head2),
        "a detector does not call a preempt that watches a sensor zone");
  }

  @Test
  @DisplayName("the detector trigger round-trips through NBT; older plans load as zone triggers")
  void triggerPersists() {
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    pe.setTriggerOnDetectors(true);
    assertTrue(TrafficSignalPreempt.fromNBT(pe.toNBT()).isTriggerOnDetectors());
    assertFalse(TrafficSignalPreempt.fromNBT(new NBTTagCompound()).isTriggerOnDetectors());
  }
}
