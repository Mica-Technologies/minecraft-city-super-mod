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

  /**
   * Issue #359, as set up on the live server: lefts 1 and 5 with standing calls, throughs 2 and 6
   * on MIN recall, phase 4 alone on barrier B with no demand (so the barrier never needs to
   * cross), and phase 1 excluded beside 5 and 6. The within-barrier wrap used to skip the
   * excluded 1 and re-serve 2, and ring 2 re-served 5 and 6 around it, so 1 starved until a
   * barrier B call came. It must come back every cycle.
   */
  @Test
  @DisplayName("an excluded call waits its turn instead of starving while the barrier stays put")
  void excludedCallIsNotStarvedByTheWithinBarrierWrap() {
    TrafficSignalProgrammedPhasePlan plan = boulevard();
    RingBarrierStateTest.enable(plan, 4, 2);
    plan.getPhase(4).setMinGreen(100L);
    plan.getPhase(4).setPassage(40L);
    plan.getPhase(4).setMaxGreen(300L);
    plan.getPhase(2).setRecallMode(TrafficSignalRecallMode.MINIMUM);
    plan.getPhase(6).setRecallMode(TrafficSignalRecallMode.MINIMUM);
    plan.getPhase(1).setExclusionMask((1 << 5) | (1 << 6));
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(3);
    Demand lefts = new Demand().veh(0, 0, 3, 0).veh(1, 0, 3, 0);
    int starts1 = 0;
    long lastStart1 = 0;
    long longestWait = 0;
    boolean was1 = false;
    for (long t = 0; t < 12000L; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, lefts);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      boolean is1 = up(m1, 1);
      if (is1 && !was1) {
        starts1++;
        longestWait = Math.max(longestWait, t - lastStart1);
        lastStart1 = t;
      }
      was1 = is1;
      assertFalse(is1 && (up(m2, 5) || up(m2, 6)), "phase 1 up beside an excluded phase");
    }
    longestWait = Math.max(longestWait, 12000L - lastStart1);
    assertTrue(starts1 >= 5, "phase 1 served only " + starts1 + " times in 10 minutes");
    assertTrue(longestWait < 2400L, "phase 1 waited " + longestWait / 20 + " s for service");
  }

  /**
   * Issue #361, the live UWS program under coordination: ring 1 [1, 2, 4], ring 2 [5, 6], phase 4
   * alone on barrier B on MIN recall, phase 1 excluded beside 5 and 6, standing calls on both
   * lefts, 2 and 6 coordinated on a 90 s cycle. FREE serves 1, then 2+5, then 2+6. Coordinated,
   * phase 5's window was laid out from the barrier start, where phase 1 holds the exclusion, and
   * its permissive period had closed by the time 1 cleared, so 5 was never served. It must come up
   * after 1 every cycle while the cycle stays on its offset.
   *
   * <p>Phase 4 recalls on its own, so it gaps out and 2+6 come back early: this is the issue's
   * sequence, where ring 2 reaches 5 again from its coordinated phase's yield.</p>
   */
  @Test
  @DisplayName("coordination: a left excluded beside the other ring's lead left is served after "
      + "it every cycle, and the cycle holds (issue #361, early return)")
  void coordinatedExcludedLeftIsServedAfterItsExcluder() {
    runCoordinatedExclusion(new long[] {230L, 750L, 820L, 230L, 520L}, false, 980L, true);
  }

  /**
   * Issue #361 with a standing call on phase 4 as well, so it runs to its force-off and the rings
   * cross into barrier A at the cycle wrap: ring 2 then waits for 5 in the forward sequence.
   */
  @Test
  @DisplayName("coordination: the excluded left is served after its excluder when the rings "
      + "cross the barrier at the wrap (issue #361)")
  void coordinatedExcludedLeftIsServedAcrossTheBarrier() {
    runCoordinatedExclusion(new long[] {230L, 750L, 820L, 230L, 520L}, true, 980L, true);
  }

  /**
   * Issue #361 on splits that do not budget the wait (ring 2's 5 + 6 equal ring 1's 1 + 2, the
   * survey's first program): the barrier keeps its typed length, so phase 4 still opens at 1020,
   * and the wait comes out of the coordinated phase 6 behind 5, not out of 5.
   */
  @Test
  @DisplayName("coordination: an exclusion wait the splits did not budget is taken from the "
      + "coordinated phase, not the barrier (issue #361)")
  void coordinatedExclusionWaitComesOutOfTheCoordinatedPhase() {
    runCoordinatedExclusion(new long[] {300L, 720L, 780L, 300L, 720L}, true, 1020L, false);
  }

  /**
   * Runs the #361 junction coordinated for seven cycles with standing calls on both lefts (and on
   * phase 4 when {@code call4}); splits are for phases 1, 2, 4, 5 and 6. Asserts that 1, 5 and 4
   * are each served once per steady-state cycle, that 4 goes green at {@code start4} (barrier B's
   * window start, so 2 and 6 yielded on time), that 2 and 6 hold the green band late in barrier A,
   * and that 1 never shows beside 5 or 6. {@code aligned}: whether the barrier advisory, which
   * counts the wait, should find the rings meeting at barrier A.
   */
  private static void runCoordinatedExclusion(long[] splits, boolean call4, long start4,
      boolean aligned) {
    TrafficSignalProgrammedPhasePlan plan = boulevard();
    RingBarrierStateTest.enable(plan, 4, 2);
    plan.getPhase(4).setMinGreen(100L);
    plan.getPhase(4).setPassage(40L);
    plan.getPhase(4).setMaxGreen(800L);
    plan.getPhase(4).setRecallMode(TrafficSignalRecallMode.MINIMUM);
    plan.getPhase(1).setMaxGreen(110L);
    plan.getPhase(5).setMaxGreen(110L);
    plan.getPhase(2).setMaxGreen(630L);
    plan.getPhase(6).setMaxGreen(400L);
    plan.getPhase(1).setExclusionMask((1 << 5) | (1 << 6));
    TrafficSignalCoordinationPlan co = plan.getCoordination();
    co.setMode(TrafficSignalCoordinationMode.COORDINATED);
    co.setCycleLength(1800L);
    co.setOffset(0L);
    co.setCoordinatedPhases(new int[] {2, 6});
    int[] phases = {1, 2, 4, 5, 6};
    for (int i = 0; i < phases.length; i++) {
      co.setSplit(phases[i], splits[i]);
    }
    long cycle = 1800L;
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(3);
    Demand demand = new Demand().veh(0, 0, 3, 0).veh(1, 0, 3, 0);
    if (call4) {
      demand.veh(2, 3, 0, 0);
    }
    int starts1 = 0;
    int starts5 = 0;
    int starts4 = 0;
    boolean was1 = false;
    boolean was5 = false;
    boolean was4 = false;
    String last = "";
    StringBuilder timeline = new StringBuilder();
    for (long t = 0; t < cycle * 7; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      String key = (m1 == null ? "idle" : m1.phaseNumber + ":" + m1.vehicle) + " | "
          + (m2 == null ? "idle" : m2.phaseNumber + ":" + m2.vehicle);
      if (!key.equals(last) && t < cycle * 3) {
        timeline.append("\n  t=").append(t).append(" local=").append(t % cycle).append("  ")
            .append(key);
      }
      last = key;
      boolean is1 = up(m1, 1);
      boolean is5 = up(m2, 5);
      boolean is4 = m1 != null && m1.phaseNumber == 4 && m1.vehicle == VehInterval.GREEN;
      assertFalse(is1 && (up(m2, 5) || up(m2, 6)),
          "phase 1 up beside an excluded phase at t=" + t + timeline);
      long local = t % cycle;
      // Steady state only: the engine cold-starts at t=0.
      if (t >= cycle) {
        if (is1 && !was1) {
          starts1++;
        }
        if (is5 && !was5) {
          starts5++;
        }
        if (is4 && !was4) {
          starts4++;
          // Phase 4 starts at its window start (barrier B's), so 2 and 6 yielded at their yield
          // points and the cycle holds its offset.
          assertEquals(start4, local, "phase 4 went green at localCycle " + local + timeline);
        }
        if (local == start4 - 120L) {
          // Just before their yield points both coordinated phases hold the green band.
          assertTrue(m1 != null && m1.phaseNumber == 2 && m1.vehicle == VehInterval.GREEN,
              "phase 2 not green at localCycle " + local + " (t=" + t + ")" + timeline);
          assertTrue(m2 != null && m2.phaseNumber == 6 && m2.vehicle == VehInterval.GREEN,
              "phase 6 not green at localCycle " + local + " (t=" + t + ")" + timeline);
        }
      }
      was1 = is1;
      was5 = is5;
      was4 = is4;
    }
    assertEquals(6, starts4, "phase 4 once per cycle" + timeline);
    assertEquals(6, starts1, "phase 1 once per cycle" + timeline);
    assertEquals(6, starts5, "phase 5 once per cycle, after phase 1" + timeline);
    String advisory = rb.findBarrierMisalignment(plan);
    if (aligned) {
      assertEquals(null, advisory, "ring 2 totals 1 + 5 + 6 = ring 1's 1 + 2");
    } else {
      assertTrue(advisory != null && advisory.contains("waits"), String.valueOf(advisory));
    }
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
