package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase exclusions (issue #283): a pair of phases the ring-and-barrier model would run together
 * (same barrier, different rings) that the plan bars from timing together.
 */
class PhaseExclusionTest {

  /** Lefts 1 (circuit 0) and 5 (circuit 1) with their throughs 2 and 6, all on barrier A. */
  private static TrafficSignalProgrammedPhasePlan boulevard() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 1, 0);
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 5, 1);
    RingBarrierStateTest.enable(plan, 6, 1);
    for (int n : new int[] {1, 2, 5, 6}) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      p.setMinGreen(100L);
      p.setPassage(40L);
      p.setMaxGreen(300L);
    }
    return plan;
  }

  private static boolean up(ServedMovement m, int phase) {
    return m != null && m.phaseNumber == phase && m.vehicle != VehInterval.RED;
  }

  /** Runs every movement called for {@code ticks}; returns {served 1, served 5, ran together}. */
  private static boolean[] run(TrafficSignalProgrammedPhasePlan plan, long ticks) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(2);
    Demand all = new Demand().veh(0, 3, 3, 0).veh(1, 3, 3, 0);
    boolean served1 = false;
    boolean served5 = false;
    boolean together = false;
    for (long t = 0; t < ticks; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, all);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      served1 |= up(m1, 1);
      served5 |= up(m2, 5);
      together |= up(m1, 1) && up(m2, 5);
    }
    return new boolean[] {served1, served5, together};
  }

  @Test
  @DisplayName("without an exclusion the two lefts run together, as the rings allow")
  void leftsRunTogetherWithoutExclusion() {
    boolean[] r = run(boulevard(), 4000L);
    assertTrue(r[2], "1 and 5 never ran together");
  }

  @Test
  @DisplayName("an exclusion keeps the two lefts apart and still serves both")
  void exclusionKeepsLeftsApart() {
    TrafficSignalProgrammedPhasePlan plan = boulevard();
    plan.getPhase(1).setExclusionMask(1 << 5);
    boolean[] r = run(plan, 4000L);
    assertTrue(r[0], "phase 1 never served");
    assertTrue(r[1], "phase 5 never served");
    assertFalse(r[2], "excluded phases 1 and 5 were up together");
  }

  @Test
  @DisplayName("an exclusion written on either phase bars the pair")
  void exclusionAppliesBothWays() {
    TrafficSignalProgrammedPhasePlan plan = boulevard();
    plan.getPhase(5).setExclusionMask(1 << 1);
    assertTrue(plan.excludes(1, 5));
    assertTrue(plan.excludes(5, 1));
    assertFalse(plan.excludes(1, 6));
    assertFalse(run(plan, 4000L)[2], "excluded phases 1 and 5 were up together");
  }

  @Test
  @DisplayName("a lead-lag ring order with an exclusion never overlaps the lefts")
  void leadLagWithExclusion() {
    TrafficSignalProgrammedPhasePlan plan = boulevard();
    plan.getPhase(1).setExclusionMask(1 << 5);
    plan.setRing2Sequence(new int[] {6, 5, 7, 8});
    boolean[] r = run(plan, 4000L);
    assertTrue(r[0] && r[1], "a left was never served");
    assertFalse(r[2], "excluded phases 1 and 5 were up together");
  }

  @Test
  @DisplayName("exclusions round-trip through NBT, and a hand-typed list of ints reads")
  void exclusionNbt() {
    TrafficSignalProgrammedPhase p = new TrafficSignalProgrammedPhase(1, 1, 0);
    p.setExclusionMask((1 << 5) | (1 << 7) | (1 << 1));   // its own bit is dropped
    assertEquals((1 << 5) | (1 << 7), p.getExclusionMask());
    TrafficSignalProgrammedPhase back = TrafficSignalProgrammedPhase.fromNBT(p.toNBT());
    assertEquals(p.getExclusionMask(), back.getExclusionMask());

    NBTTagCompound c = new TrafficSignalProgrammedPhase(3, 1, 1).toNBT();
    assertFalse(c.hasKey("ex"), "no exclusions should write nothing");
    NBTTagList list = new NBTTagList();
    list.appendTag(new NBTTagInt(7));
    list.appendTag(new NBTTagInt(42));   // out of range: ignored
    c.setTag("ex", list);
    assertEquals(1 << 7, TrafficSignalProgrammedPhase.fromNBT(c).getExclusionMask());
  }

  @Test
  @DisplayName("a preempt serving an excluded pair together fails validation")
  void preemptWithExcludedPairFailsValidation() {
    TrafficSignalProgrammedPhasePlan plan = boulevard();
    plan.getPhase(1).setExclusionMask(1 << 5);
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(2);
    for (int c = 0; c < 2; c++) {
      ckts.getCircuit(c).linkDevice(new BlockPos(c, 0, 0),
          AbstractBlockControllableSignal.SIGNAL_SIDE.THROUGH);
      ckts.getCircuit(c).linkDevice(new BlockPos(c, 1, 0),
          AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED);
    }
    assertEquals(null, plan.validate(ckts));
    TrafficSignalPreempt pe = new TrafficSignalPreempt();
    pe.setEnabled(true);
    pe.setTriggerCircuitIndex(0);
    pe.setDwellPhases(new int[] {1, 5});
    plan.getPreempts().add(pe);
    String error = plan.validate(ckts);
    assertTrue(error != null && error.contains("1 and 5"), String.valueOf(error));
  }
}
