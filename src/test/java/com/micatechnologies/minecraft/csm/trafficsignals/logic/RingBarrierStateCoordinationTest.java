package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Coordinated-operation regression tests for the ring-and-barrier engine, from an in-game report:
 * under COORDINATED operation with rest-in-walk coordinated phases, a locked (LOCK detector
 * memory) side-street call placed briefly and then dropped triggered the coordinated phases'
 * pedestrian clearance, but the side street was never served — the permissive-window gate was
 * re-checked every tick, so a window that closed during the mains' clearance erased the accepted
 * call and the controller recycled back to WALK every cycle. Also covers the force-off fixes that
 * fell out of the same investigation (cycle-wrap force-off; late-served accepted calls clipped to
 * min green).
 */
class RingBarrierStateCoordinationTest {

  static final TrafficSignalControllerOverlaps NO_OVERLAPS = new TrafficSignalControllerOverlaps();

  private TrafficSignalProgrammedPhasePlan coordinatedPlan() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 2, 0); // main street EW (coordinated)
    RingBarrierStateTest.enable(plan, 6, 1); // main street WE (coordinated)
    RingBarrierStateTest.enable(plan, 4, 2); // side street
    RingBarrierStateTest.enable(plan, 8, 3); // side street
    plan.getPhase(2).setRestInWalk(true);
    plan.getPhase(6).setRestInWalk(true);
    plan.getPhase(4).setLockCall(true);
    plan.getPhase(8).setLockCall(true);
    plan.getCoordination().setMode(TrafficSignalCoordinationMode.COORDINATED);
    // Defaults: cycle 1800, offset 0, coordinated phases {2, 6}, splits auto (even).
    return plan;
  }

  /** Runs the engine from t=0 to t=end, with a vehicle on circuit 2 during [from, to). */
  private List<String> run(TrafficSignalProgrammedPhasePlan plan, long callFrom, long callTo,
      long end) {
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    RingBarrierStateTest.Demand none = new RingBarrierStateTest.Demand();
    RingBarrierStateTest.Demand sideCall = new RingBarrierStateTest.Demand().veh(2, 1, 0, 0);
    List<String> timeline = new ArrayList<>();
    String last = "";
    boolean sideServed = false;
    for (long t = 0; t <= end; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, (t >= callFrom && t < callTo) ? sideCall : none);
      ServedMovement m1 = rb.getLastServed(1);
      ServedMovement m2 = rb.getLastServed(2);
      String s = "t=" + t + " r1=" + fmt(m1) + " r2=" + fmt(m2);
      String key = s.substring(s.indexOf("r1="));
      if (!key.equals(last)) {
        timeline.add(s);
        last = key;
      }
      if (m1 != null && m1.phaseNumber == 4 && m1.vehicle == VehInterval.GREEN) {
        sideServed = true;
      }
    }
    timeline.add(sideServed ? "SIDE STREET SERVED" : "SIDE STREET NEVER SERVED");
    return timeline;
  }

  private static String fmt(ServedMovement m) {
    return m == null ? "idle" : (m.phaseNumber + ":" + m.vehicle + "/" + m.pedestrian);
  }

  @Test
  @DisplayName("coordination: a locked side-street call placed INSIDE the permissive window "
      + "and then dropped must still get the side street served")
  void lockedCallInsideWindowIsServed() {
    // Side-street (phase 4) window with even splits: [900, 1800) of the 1800-tick cycle.
    // Vehicle present t=1000..1060 (3 s), then gone. LOCK must hold the call until served.
    List<String> timeline = run(coordinatedPlan(), 1000L, 1060L, 5400L);
    timeline.forEach(System.out::println);
    assertTrue(timeline.contains("SIDE STREET SERVED"),
        "locked call placed inside the permissive window must be served");
  }

  @Test
  @DisplayName("coordination: a locked side-street call placed OUTSIDE the permissive window "
      + "and then dropped must be served when the window opens")
  void lockedCallOutsideWindowIsServedWhenWindowOpens() {
    // Vehicle present t=100..160, during the coordinated phases' window ([0, 900)).
    List<String> timeline = run(coordinatedPlan(), 100L, 160L, 5400L);
    timeline.forEach(System.out::println);
    assertTrue(timeline.contains("SIDE STREET SERVED"),
        "locked call placed outside the permissive window must be served when it opens");
  }

  @Test
  @DisplayName("coordination: 30s cycle — the mains' ped clearance must not consume the side "
      + "street's entire window (locked call must still be served)")
  void shortCycleLockedCallIsServed() {
    // Cycle 600 (30 s), even splits: side-street window [300, 600) = 300 ticks. The coordinated
    // phases' rest-in-walk clearance alone is pedClear(200) + yellow(70) + redClear(40) = 310
    // ticks, so if the engine re-checks the window at service time the side street can never win.
    TrafficSignalProgrammedPhasePlan plan = coordinatedPlan();
    plan.getCoordination().setCycleLength(600L);
    List<String> timeline = run(plan, 350L, 410L, 4200L);
    timeline.forEach(System.out::println);
    assertTrue(timeline.contains("SIDE STREET SERVED"),
        "locked side-street call must eventually be served on a short coordinated cycle");
  }

  @Test
  @DisplayName("coordination: tight side-street split — locked call must still be served")
  void tightSplitLockedCallIsServed() {
    // Realistic splits on the 90 s cycle: mains 75 s, side street 15 s (300 ticks) — less than
    // the mains' clearance overhead (310 ticks).
    TrafficSignalProgrammedPhasePlan plan = coordinatedPlan();
    plan.getCoordination().setSplit(2, 1500L);
    plan.getCoordination().setSplit(6, 1500L);
    plan.getCoordination().setSplit(4, 300L);
    plan.getCoordination().setSplit(8, 300L);
    List<String> timeline = run(plan, 1550L, 1610L, 7200L);
    timeline.forEach(System.out::println);
    assertTrue(timeline.contains("SIDE STREET SERVED"),
        "locked side-street call must eventually be served with a tight split");
  }

  @Test
  @DisplayName("coordination: the last window in the ring force-offs at the cycle wrap "
      + "(continuous side-street demand must not overstay into the mains' time)")
  void forceOffFiresAtCycleWrapForLastWindowPhase() {
    // Phase 4's window is the last in ring 1 ([900, 1800) with even splits), so its end is the
    // cycle wrap. With continuous demand and a huge max green, only force-off can end it — a
    // windowEnd comparison (localCycle >= 1800) can never be true since localCycle wraps to 0.
    TrafficSignalProgrammedPhasePlan plan = coordinatedPlan();
    plan.getPhase(4).setMaxGreen(6000L);
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    RingBarrierStateTest.Demand none = new RingBarrierStateTest.Demand();
    RingBarrierStateTest.Demand sideCall = new RingBarrierStateTest.Demand().veh(2, 1, 0, 0);
    for (long t = 0; t <= 1900; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, t >= 1000 ? sideCall : none);
      if (t == 1400) {
        // Sanity: the side street is being served within its window.
        assertTrue(rb.getLastServed(1) != null && rb.getLastServed(1).phaseNumber == 4
                && rb.getLastServed(1).vehicle == VehInterval.GREEN,
            "phase 4 should be green inside its window");
      }
    }
    // At t=1900 (100 past the wrap) phase 4 must have been forced off despite standing demand.
    ServedMovement m1 = rb.getLastServed(1);
    assertTrue(m1 == null || m1.phaseNumber != 4 || m1.vehicle != VehInterval.GREEN,
        "phase 4 must be forced off at the cycle wrap, not run on toward max green");
  }

  @Test
  @DisplayName("coordination: under continuous demand a side street is never served outside its "
      + "permissive window (a force-off must consume the window acceptance)")
  void forcedOffPhaseDoesNotKeepCallingThroughTheMainsGreen() {
    // Continuous demand on BOTH side streets is the loaded-intersection case. The window
    // acceptance latch is only cleared when demand drops, so with demand that never drops a
    // force-off that left the latch set gave the side street a standing call straight through the
    // mains' green: phase 2 was terminated after bare min green and phase 4 was then served well
    // outside its own window, three serve-rounds per two cycles, permanently off offset.
    TrafficSignalProgrammedPhasePlan plan = coordinatedPlan();
    long cycle = plan.getCoordination().getCycleLength();
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    RingBarrierStateTest.Demand busy = new RingBarrierStateTest.Demand()
        .veh(2, 1, 0, 0).veh(3, 1, 0, 0);

    // Phase 4's permissive window with even splits is [900, 1800) of the 1800-tick cycle. Allow a
    // grace equal to its clearance for a green that legitimately began inside the window and is
    // still finishing; serving green well before the window opens is the defect.
    long windowStart = cycle / 2;
    long grace = plan.getPhase(4).getYellow() + plan.getPhase(4).getRedClear();
    for (long t = 0; t <= cycle * 6; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, busy);
      ServedMovement m1 = rb.getLastServed(1);
      if (m1 == null || m1.phaseNumber != 4 || m1.vehicle != VehInterval.GREEN) {
        continue;
      }
      long local = ((t - plan.getCoordination().getOffset()) % cycle + cycle) % cycle;
      assertTrue(local >= windowStart - grace,
          "phase 4 was green at localCycle=" + local + " (t=" + t + "), before its window opened "
              + "at " + windowStart + " — a stale window acceptance survived its force-off");
    }
  }

  @Test
  @DisplayName("coordination: the green band holds and the side street starts at its window "
      + "start every cycle (splits must budget each phase's own clearance)")
  void coordinationLocksToTheOffsetEveryCycle() {
    // The two properties that define a working coordinated controller:
    //   1. the coordinated phase is GREEN at the offset -- that IS the green band, and
    //   2. the side street's green begins at ITS window start, once per cycle.
    // Note it is the coordinated phase being green AT the offset that matters, not its green
    // STARTING there: when the controller is recovering, the coordinated phase dwells across the
    // top of the cycle, which is the add-only correction working as intended.
    //
    // Splits tile the cycle as if they were all green, but real service is green + yellow +
    // red-clear. Force-offing a phase at its window END rather than its yield point let every
    // phase overrun by its own clearance, so the side street started that much late every cycle
    // with nothing to ever take the lateness back -- the permanent offset error that looked like
    // "the signal never catches up".
    TrafficSignalProgrammedPhasePlan plan = coordinatedPlan();
    long cycle = plan.getCoordination().getCycleLength();
    long sideWindowStart = cycle / 2; // even splits: phase 4's window is [900, 1800)
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    RingBarrierStateTest.Demand busy = new RingBarrierStateTest.Demand()
        .veh(2, 1, 0, 0).veh(3, 1, 0, 0);

    int offsetsSeen = 0;
    int bandHeld = 0;
    int sideStarts = 0;
    boolean sideWasGreen = false;
    for (long t = 0; t <= cycle * 6; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, busy);
      long local = ((t - plan.getCoordination().getOffset()) % cycle + cycle) % cycle;
      ServedMovement m1 = rb.getLastServed(1);
      boolean coordGreen = m1 != null && m1.phaseNumber == 2 && m1.vehicle == VehInterval.GREEN;
      boolean sideGreen = m1 != null && m1.phaseNumber == 4 && m1.vehicle == VehInterval.GREEN;
      // Skip the first cycle: the engine cold-starts at t=0 and steady state is what matters.
      if (t > cycle) {
        if (local == 0) {
          offsetsSeen++;
          if (coordGreen) {
            bandHeld++;
          }
        }
        if (sideGreen && !sideWasGreen) {
          sideStarts++;
          assertEquals(sideWindowStart, local,
              "side street (phase 4) went green at localCycle=" + local + " (t=" + t
                  + ") instead of at its window start " + sideWindowStart);
        }
      }
      sideWasGreen = sideGreen;
    }
    assertEquals(offsetsSeen, bandHeld,
        "the coordinated phase must be green at the offset on every cycle (green band)");
    assertEquals(5, sideStarts,
        "expected exactly one side-street green per cycle over 5 steady-state cycles");
  }

  @Test
  @DisplayName("coordination: a phase's clearance fits inside its own split, so it never eats "
      + "into the next phase's window")
  void phaseClearanceFitsInsideItsOwnSplit() {
    // A split is the time the movement owns end to end -- green AND its clearance -- so a phase
    // must go yellow early enough that its red clearance has finished by its window end. Force-
    // offing at the window end instead pushed every phase's clearance into the next phase's
    // window. The dwell correction can absorb that, so the green band still holds and the
    // symptom hides; this test pins the split arithmetic itself.
    TrafficSignalProgrammedPhasePlan plan = coordinatedPlan();
    // Max green must not be what ends the phase, or force-off is never the binding constraint
    // and the split arithmetic is not exercised at all.
    plan.getPhase(4).setMaxGreen(6000L);
    plan.getPhase(8).setMaxGreen(6000L);
    long cycle = plan.getCoordination().getCycleLength();
    // Even splits: phase 4's window is [900, 1800). Measure the green-end WINDOW-RELATIVE, not by
    // raw localCycle -- phase 4's window ends at the cycle wrap, where a green ending late reads
    // as localCycle 0 and would look early. Same trap the engine's pastYieldPoint documents.
    long sideWindowStart = cycle / 2;
    long sideWindowLen = cycle - sideWindowStart;
    long clearance = plan.getPhase(4).getYellow() + plan.getPhase(4).getRedClear();
    RingBarrierState rb = new RingBarrierState();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    RingBarrierStateTest.Demand busy = new RingBarrierStateTest.Demand()
        .veh(2, 1, 0, 0).veh(3, 1, 0, 0);

    boolean checked = false;
    boolean wasGreen = false;
    for (long t = 0; t <= cycle * 4; t++) {
      rb.tick(plan, ckts, NO_OVERLAPS, t, busy);
      ServedMovement m1 = rb.getLastServed(1);
      boolean green = m1 != null && m1.phaseNumber == 4 && m1.vehicle == VehInterval.GREEN;
      if (!green && wasGreen && t > cycle) {
        long local = ((t - plan.getCoordination().getOffset()) % cycle + cycle) % cycle;
        long pos = ((local - sideWindowStart) % cycle + cycle) % cycle;
        // Green ended here; its clearance still has to run. It must finish by the window end.
        assertTrue(pos + clearance <= sideWindowLen,
            "phase 4 left green " + pos + " ticks into its " + sideWindowLen
                + "-tick window (localCycle=" + local + ", t=" + t + "); its " + clearance
                + "-tick clearance would run " + (pos + clearance - sideWindowLen)
                + " ticks past the window end — the split must budget the phase's own clearance");
        checked = true;
      }
      wasGreen = green;
    }
    assertTrue(checked, "expected to observe the side street leaving green at least once");
  }
}
