package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Transit signal priority called by preempt detectors that see a bus, and the queue jump it runs:
 * the transit phase's green held back with only its queue jump heads lit.
 */
class TransitQueueJumpTest {

  private static final BlockPos QUEUE_JUMP_HEAD = new BlockPos(10, 64, 10);
  private static final BlockPos THROUGH_HEAD = new BlockPos(12, 64, 10);

  /** Phase 2 on circuit 0 against phase 4 (the transit phase) on circuit 1, a bus calling 4. */
  private static TrafficSignalProgrammedPhasePlan plan(long queueJump) {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 4, 1);
    TrafficSignalPriorityPlan priority = plan.getPriority();
    priority.setEnabled(true);
    priority.setTriggerCircuitIndex(1);
    priority.setTriggerOnDetectors(true);
    priority.setTransitPhase(4);
    priority.setQueueJump(queueJump);
    return plan;
  }

  private static TrafficSignalControllerCircuits circuits(boolean queueJumpHeads) {
    TrafficSignalControllerCircuits c = RingBarrierStateTest.circuits(2);
    c.getCircuit(1).linkThroughSignal(THROUGH_HEAD);
    if (queueJumpHeads) {
      c.getCircuit(1).linkQueueJumpSignal(QUEUE_JUMP_HEAD);
    }
    return c;
  }

  private static Demand demand(int transitCalls) {
    return demand(transitCalls, 1);
  }

  private static Demand demand(int transitCalls, int cars) {
    Demand d = new Demand().veh(0, 1, 0, 0).veh(1, cars, 0, 0);
    d.summaries.get(1).withTransitDetectorCalls(transitCalls);
    return d;
  }

  /** What the run saw: when phase 4 was first served, when its green showed, and the heads. */
  private static final class Run {
    long served = -1L;
    long green = -1L;
    boolean jumpAtServe = false;
    boolean headLitAtServe = false;
    boolean throughRedAtServe = false;
    boolean headLitAtGreen = true;
  }

  private static Run run(long queueJump, boolean heads, int transitCalls) {
    return run(queueJump, heads, demand(transitCalls));
  }

  private static Run run(long queueJump, boolean heads, Demand d) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan plan = plan(queueJump);
    TrafficSignalControllerCircuits ckts = circuits(heads);
    Run r = new Run();
    TrafficSignalPhase shown = null;
    for (long t = 0; t < 6000L && r.green < 0L; t++) {
      TrafficSignalPhase changed = rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, d);
      if (changed != null) {
        shown = changed;
      }
      ServedMovement m = rb.getLastServed(1);
      if (m == null || m.phaseNumber != 4) {
        continue;
      }
      if (r.served < 0L) {
        r.served = t;
        r.jumpAtServe = m.queueJump;
        r.headLitAtServe = shown.getGreenSignals().contains(QUEUE_JUMP_HEAD);
        r.throughRedAtServe = shown.getRedSignals().contains(THROUGH_HEAD);
      }
      if (m.vehicle == VehInterval.GREEN) {
        r.green = t;
        r.headLitAtGreen = shown.getGreenSignals().contains(QUEUE_JUMP_HEAD);
      }
    }
    assertTrue(r.served >= 0L, "phase 4 was never served");
    assertTrue(r.green >= 0L, "phase 4 never showed green");
    return r;
  }

  @Test
  @DisplayName("a bus seen by the detectors gets a queue jump: its bar lit, the general heads red")
  void busGetsQueueJump() {
    Run r = run(80L, true, 1);
    assertTrue(r.jumpAtServe);
    assertTrue(r.headLitAtServe, "the queue jump head shows the bar");
    assertTrue(r.throughRedAtServe, "the general head is held red");
    assertEquals(80L, r.green - r.served, "the general green follows the jump");
    assertFalse(r.headLitAtGreen, "the bar goes dark when the general green comes up");
  }

  @Test
  @DisplayName("a bus no sensor counts still calls the transit phase, and gets its jump")
  void busAloneCallsPhase() {
    Run r = run(80L, true, demand(1, 0));
    assertTrue(r.jumpAtServe);
    assertEquals(80L, r.green - r.served);
  }

  @Test
  @DisplayName("no queue jump heads on the circuit: no jump, the green is not held")
  void noHeadsNoJump() {
    Run r = run(80L, false, 1);
    assertFalse(r.jumpAtServe);
    assertEquals(r.served, r.green);
  }

  @Test
  @DisplayName("no bus: no jump")
  void noBusNoJump() {
    Run r = run(80L, true, 0);
    assertFalse(r.jumpAtServe);
    assertFalse(r.headLitAtServe);
    assertEquals(r.served, r.green);
  }

  @Test
  @DisplayName("a queue jump of zero runs none")
  void zeroJumpRunsNone() {
    Run r = run(0L, true, 1);
    assertFalse(r.jumpAtServe);
    assertEquals(r.served, r.green);
  }

  @Test
  @DisplayName("the detector trigger and the queue jump survive a save")
  void savesSettings() {
    TrafficSignalPriorityPlan priority = plan(120L).getPriority();
    TrafficSignalPriorityPlan loaded = TrafficSignalPriorityPlan.fromNBT(priority.toNBT());
    assertTrue(loaded.isTriggerOnDetectors());
    assertEquals(120L, loaded.getQueueJump());
    TrafficSignalPriorityPlan old = TrafficSignalPriorityPlan.fromNBT(
        new TrafficSignalPriorityPlan().toNBT());
    assertFalse(old.isTriggerOnDetectors());
    assertEquals(TrafficSignalPriorityPlan.DEFAULT_QUEUE_JUMP, old.getQueueJump());
    assertNotNull(old);
  }
}
