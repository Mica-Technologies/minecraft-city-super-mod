package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.PedInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The {@link TrafficSignalPhaseMovement#PROTECTED} (bus/train/bike lane) movement: a phase of its
 * own that the protected zone calls and that lights only the protected heads, taking them and the
 * zone away from the left phase on the same circuit. Plans without one are pinned unchanged by
 * {@link ProtectedMovementRegressionTest}.
 */
class ProtectedPhaseTest {

  private static final BlockPos C0_THROUGH = new BlockPos(1, 64, 0);
  private static final BlockPos C1_THROUGH = new BlockPos(11, 64, 0);
  private static final BlockPos C1_LEFT = new BlockPos(12, 64, 0);
  private static final BlockPos C1_RIGHT = new BlockPos(13, 64, 0);
  private static final BlockPos C1_PROTECTED = new BlockPos(14, 64, 0);
  private static final BlockPos C2_LEFT = new BlockPos(22, 64, 0);
  private static final BlockPos C2_PROTECTED = new BlockPos(24, 64, 0);

  /**
   * Circuit 0 is the cross street (Ph4 THROUGH, barrier B). Circuit 1 is one approach: Ph2
   * THROUGH, Ph1 PROTECTED_LEFT and Ph6 PROTECTED, its heads a through, left, right and a bus
   * lane head. Circuit 2 is an output-only circuit for overlaps.
   */
  private static TrafficSignalControllerCircuits circuits() {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
    c0.getThroughSignals().add(C0_THROUGH);
    TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
    c1.getThroughSignals().add(C1_THROUGH);
    c1.getLeftSignals().add(C1_LEFT);
    c1.getRightSignals().add(C1_RIGHT);
    c1.getProtectedSignals().add(C1_PROTECTED);
    TrafficSignalControllerCircuit c2 = new TrafficSignalControllerCircuit();
    c2.getLeftSignals().add(C2_LEFT);
    c2.getProtectedSignals().add(C2_PROTECTED);
    circuits.addCircuit(c0);
    circuits.addCircuit(c1);
    circuits.addCircuit(c2);
    return circuits;
  }

  private static TrafficSignalProgrammedPhasePlan plan() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    enable(plan, 1, 1, TrafficSignalPhaseMovement.PROTECTED_LEFT);
    enable(plan, 2, 1, TrafficSignalPhaseMovement.THROUGH);
    enable(plan, 6, 1, TrafficSignalPhaseMovement.PROTECTED);
    enable(plan, 4, 0, TrafficSignalPhaseMovement.THROUGH);
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      p.setMinGreen(60L);
      p.setPassage(30L);
      p.setMaxGreen(300L);
      p.setYellow(60L);
      p.setRedClear(20L);
    }
    return plan;
  }

  private static void enable(TrafficSignalProgrammedPhasePlan plan, int n, int circuit,
      TrafficSignalPhaseMovement movement) {
    TrafficSignalProgrammedPhase p = plan.getPhase(n);
    p.setCircuitIndex(circuit);
    p.setMovement(movement);
    p.setEnabled(true);
  }

  private static TrafficSignalPhase build(TrafficSignalProgrammedPhasePlan plan, int phase,
      VehInterval iv) {
    return AdvancedPhaseBuilder.build(null, plan, circuits(), new TrafficSignalControllerOverlaps(),
        new ServedMovement(phase, iv, PedInterval.NONE), null);
  }

  /** Demand on circuit 1 only: through, left, right, protected-lane counts. */
  private static RingBarrierStateTest.Demand onApproach(int through, int left, int right,
      int bike) {
    return new RingBarrierStateTest.Demand().veh(0, 0, 0, 0).veh(1, through, left, right, bike)
        .veh(2, 0, 0, 0);
  }

  /** The phases served green, in order of first green, over {@code ticks}. */
  private static List<Integer> greens(TrafficSignalProgrammedPhasePlan plan,
      RingBarrierState.DemandSource demand, long ticks) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = circuits();
    List<Integer> served = new ArrayList<>();
    for (long t = 0; t < ticks; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      for (int ring = 1; ring <= 2; ring++) {
        ServedMovement m = rb.getLastServed(ring);
        if (m != null && m.vehicle == VehInterval.GREEN && !served.contains(m.phaseNumber)) {
          served.add(m.phaseNumber);
        }
      }
    }
    return served;
  }

  @Test
  @DisplayName("PROTECTED is ordinal 5, saved as mv:5 in a phase's NBT")
  void ordinalAndNbt() {
    assertEquals(5, TrafficSignalPhaseMovement.PROTECTED.toNBT());
    assertEquals(TrafficSignalPhaseMovement.PROTECTED, TrafficSignalPhaseMovement.fromNBT(5));
    assertEquals("Protected Lane", TrafficSignalPhaseMovement.PROTECTED.getName());

    TrafficSignalProgrammedPhasePlan plan = plan();
    NBTTagCompound phaseNbt = plan.getPhase(6).toNBT();
    assertEquals(5, phaseNbt.getInteger("mv"));
    TrafficSignalProgrammedPhasePlan read = TrafficSignalProgrammedPhasePlan.fromNBT(plan.toNBT());
    assertEquals(TrafficSignalPhaseMovement.PROTECTED, read.getPhase(6).getMovement());
    assertTrue(read.hasProtectedPhaseFor(1));
    assertFalse(read.hasProtectedPhaseFor(0));
  }

  @Test
  @DisplayName("the protected zone calls the PROTECTED phase and not the left")
  void protectedZoneCallsProtectedPhase() {
    List<Integer> served = greens(plan(), onApproach(0, 0, 0, 1), 1500L);
    assertTrue(served.contains(6), "a bus in the protected zone calls Ph6: " + served);
    assertFalse(served.contains(1), "it no longer calls the left: " + served);
  }

  @Test
  @DisplayName("the left zone calls the left and not the PROTECTED phase")
  void leftZoneCallsLeftOnly() {
    List<Integer> served = greens(plan(), onApproach(0, 1, 0, 0), 1500L);
    assertTrue(served.contains(1), "a car in the left zone calls Ph1: " + served);
    assertFalse(served.contains(6), served.toString());
  }

  @Test
  @DisplayName("without the PROTECTED phase, the protected zone calls the left again")
  void withoutProtectedPhaseTheLeftIsCalled() {
    TrafficSignalProgrammedPhasePlan plan = plan();
    plan.getPhase(6).setEnabled(false);
    List<Integer> served = greens(plan, onApproach(0, 0, 0, 1), 1500L);
    assertTrue(served.contains(1), served.toString());
  }

  @Test
  @DisplayName("Ph6 PROTECTED lights only the protected heads")
  void protectedPhaseLightsOnlyProtectedHeads() {
    TrafficSignalPhase phase = build(plan(), 6, VehInterval.GREEN);
    assertTrue(phase.getGreenSignals().contains(C1_PROTECTED));
    assertFalse(phase.getGreenSignals().contains(C1_LEFT));
    assertFalse(phase.getGreenSignals().contains(C1_THROUGH));
    assertFalse(phase.getGreenSignals().contains(C1_RIGHT));
    assertTrue(phase.getRedSignals().contains(C1_LEFT));

    assertTrue(build(plan(), 6, VehInterval.YELLOW).getYellowSignals().contains(C1_PROTECTED));
  }

  @Test
  @DisplayName("the left phase no longer lights the protected heads once a PROTECTED phase owns "
      + "them")
  void leftPhaseLeavesProtectedHeads() {
    TrafficSignalPhase phase = build(plan(), 1, VehInterval.GREEN);
    assertTrue(phase.getGreenSignals().contains(C1_LEFT));
    assertFalse(phase.getGreenSignals().contains(C1_PROTECTED));
    assertTrue(phase.getRedSignals().contains(C1_PROTECTED));

    // Disable the PROTECTED phase and the left carries them again, as before.
    TrafficSignalProgrammedPhasePlan plan = plan();
    plan.getPhase(6).setEnabled(false);
    assertTrue(build(plan, 1, VehInterval.GREEN).getGreenSignals().contains(C1_PROTECTED));
  }

  @Test
  @DisplayName("overlap outputs: PROTECTED drives the protected heads, LEFT leaves them to it")
  void overlapOutputs() {
    TrafficSignalProgrammedPhasePlan plan = plan();
    enable(plan, 5, 2, TrafficSignalPhaseMovement.PROTECTED); // circuit 2 has a PROTECTED phase
    TrafficSignalProgrammedOverlap left = new TrafficSignalProgrammedOverlap();
    left.setEnabled(true);
    left.setOutputCircuitIndex(2);
    left.setOutputMovement(TrafficSignalPhaseMovement.LEFT);
    left.setIncludedPhases(new int[] {2});
    plan.getVehicleOverlaps().add(left);
    TrafficSignalPhase phase = build(plan, 2, VehInterval.GREEN);
    assertTrue(phase.getGreenSignals().contains(C2_LEFT));
    assertFalse(phase.getGreenSignals().contains(C2_PROTECTED));

    TrafficSignalProgrammedPhasePlan plan2 = plan();
    TrafficSignalProgrammedOverlap prot = new TrafficSignalProgrammedOverlap();
    prot.setEnabled(true);
    prot.setOutputCircuitIndex(2);
    prot.setOutputMovement(TrafficSignalPhaseMovement.PROTECTED);
    prot.setIncludedPhases(new int[] {2});
    plan2.getVehicleOverlaps().add(prot);
    TrafficSignalPhase phase2 = build(plan2, 2, VehInterval.GREEN);
    assertTrue(phase2.getGreenSignals().contains(C2_PROTECTED));
    assertFalse(phase2.getGreenSignals().contains(C2_LEFT));
  }

  @Test
  @DisplayName("a PROTECTED phase is an ordinary phase: it runs beside the through on the other "
      + "ring, even with a permissive phase set")
  void protectedRunsConcurrentlyWithThrough() {
    TrafficSignalProgrammedPhasePlan plan = plan();
    // A permissive field on the PROTECTED phase must not make it an FYA pair with Ph2.
    plan.getPhase(6).setPermissivePhase(2);
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = circuits();
    RingBarrierStateTest.Demand demand = onApproach(1, 0, 0, 1);
    boolean together = false;
    for (long t = 0; t < 600L && !together; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      ServedMovement r1 = rb.getLastServed(1);
      ServedMovement r2 = rb.getLastServed(2);
      together = r1 != null && r2 != null && r1.phaseNumber == 2 && r2.phaseNumber == 6
          && r1.vehicle == VehInterval.GREEN && r2.vehicle == VehInterval.GREEN;
    }
    assertTrue(together, "Ph2 and Ph6 are green together");
    assertNull(plan.validate(ckts), "the plan validates");
  }

  @Test
  @DisplayName("no head ever goes from green straight to red in a plan with a PROTECTED phase")
  void clearanceInvariant() {
    TrafficSignalProgrammedPhasePlan plan = plan();
    TrafficSignalControllerCircuits ckts = circuits();
    RingBarrierState rb = new RingBarrierState();
    BlockPos[] heads = {C0_THROUGH, C1_THROUGH, C1_LEFT, C1_RIGHT, C1_PROTECTED};
    Map<BlockPos, Character> last = new HashMap<>();
    java.util.Random random = new java.util.Random(5L);
    RingBarrierStateTest.Demand demand = null;
    int protectedGreens = 0;
    for (long t = 0; t < 10_000L; t++) {
      if (t % 120L == 0L) {
        demand = new RingBarrierStateTest.Demand()
            .veh(0, random.nextInt(2), 0, 0)
            .veh(1, random.nextInt(2), random.nextInt(2), random.nextInt(2), random.nextInt(2))
            .veh(2, 0, 0, 0);
      }
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      TrafficSignalPhase phase = rb.getLastAppliedPhase();
      assertNotNull(phase);
      for (BlockPos pos : heads) {
        char now = phase.getGreenSignals().contains(pos) ? 'G'
            : phase.getYellowSignals().contains(pos) ? 'Y'
                : phase.getRedSignals().contains(pos) ? 'R' : '-';
        Character before = last.put(pos, now);
        assertFalse(before != null && before == 'G' && now == 'R',
            "head " + pos + " went green to red at tick " + t);
        if (pos.equals(C1_PROTECTED) && now == 'G' && (before == null || before != 'G')) {
          protectedGreens++;
        }
      }
    }
    assertTrue(protectedGreens > 3, "the protected head should cycle, got " + protectedGreens);
  }

  @Test
  @DisplayName("validation: a PROTECTED phase needs protected heads, and a left loses them to it")
  void validation() {
    TrafficSignalControllerCircuits ckts = circuits();
    TrafficSignalProgrammedPhasePlan plan = plan();
    enable(plan, 5, 0, TrafficSignalPhaseMovement.PROTECTED); // circuit 0 has no protected heads
    String error = plan.validate(ckts);
    assertNotNull(error);
    assertTrue(error.contains("Protected Lane"), error);

    // A left on circuit 2, whose only heads besides its left lens... remove the left lens: the
    // protected heads are the PROTECTED phase's, so the left has nothing to drive.
    TrafficSignalControllerCircuits noLeftLens = circuits();
    noLeftLens.getCircuit(2).getLeftSignals().clear();
    TrafficSignalProgrammedPhasePlan plan2 = plan();
    enable(plan2, 3, 2, TrafficSignalPhaseMovement.PROTECTED_LEFT);
    enable(plan2, 7, 2, TrafficSignalPhaseMovement.PROTECTED);
    String error2 = plan2.validate(noLeftLens);
    assertNotNull(error2);
    assertTrue(error2.startsWith("Phase 3"), error2);
  }
}
