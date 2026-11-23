package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.ServedMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierStateTest.Demand;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPatternSelect.Source;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * Manual and switched pattern selection (#378): an event mode switch that moves an ADVANCED
 * controller onto another coordination pattern and back.
 *
 * <p>What is covered: who wins between a switch, a manual hold and the clock; that the settings
 * survive a save and that an old save reads as "clock, switch selects pattern 2"; that the engine
 * takes a selected pattern up only at a cycle boundary, exactly as it does a clock change; that
 * every phase is still served on either pattern; and that a preempt wins over both.</p>
 */
class PatternSelectTest {

  private static final int NONE = TrafficSignalPatternSelect.NONE;

  // region: who chooses

  @Test
  void theClockChoosesWhenNothingElseDoes() {
    assertEquals(Source.CLOCK, TrafficSignalPatternSelect.source(NONE, 1, false));
    assertEquals(2, TrafficSignalPatternSelect.slot(2, NONE, 1, false));
    assertEquals(0, TrafficSignalPatternSelect.slot(0, NONE, NONE, true));
  }

  @Test
  void aManualHoldOutranksTheClock() {
    assertEquals(Source.MANUAL, TrafficSignalPatternSelect.source(3, 1, false));
    assertEquals(3, TrafficSignalPatternSelect.slot(0, 3, 1, false));
    // Holding pattern 1 is a hold, not "none": it keeps the clock's afternoon pattern out.
    assertEquals(Source.MANUAL, TrafficSignalPatternSelect.source(0, NONE, false));
    assertEquals(0, TrafficSignalPatternSelect.slot(2, 0, NONE, false));
  }

  @Test
  void aSwitchThatIsOnOutranksBoth() {
    assertEquals(Source.SWITCH, TrafficSignalPatternSelect.source(3, 1, true));
    assertEquals(1, TrafficSignalPatternSelect.slot(0, 3, 1, true));
    assertEquals(1, TrafficSignalPatternSelect.slot(2, NONE, 1, true));
  }

  @Test
  void aSwitchTheControllerIgnoresChoosesNothing() {
    assertEquals(Source.MANUAL, TrafficSignalPatternSelect.source(2, NONE, true));
    assertEquals(Source.CLOCK, TrafficSignalPatternSelect.source(NONE, NONE, true));
    assertEquals(2, TrafficSignalPatternSelect.slot(0, 2, NONE, true));
  }

  @Test
  void aSettingThatIsNotASlotIsNone() {
    for (int bad : new int[]{-2, 4, 99, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      assertEquals(NONE, TrafficSignalPatternSelect.normalize(bad));
      assertEquals(Source.CLOCK, TrafficSignalPatternSelect.source(bad, bad, true));
      assertEquals(1, TrafficSignalPatternSelect.slot(1, bad, bad, true));
    }
    // and a clock that says something impossible still yields a pattern that exists
    assertEquals(0, TrafficSignalPatternSelect.slot(-7, NONE, NONE, false));
  }

  // endregion

  // region: the plan

  @Test
  void aPlanSavedBeforePatternSelectReadsAsClockWithTheSwitchOnPatternTwo() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    NBTTagCompound saved = plan.toNBT();
    assertFalse(saved.hasKey("cpm"), "a default plan adds no pattern select keys");
    assertFalse(saved.hasKey("cps"));
    TrafficSignalProgrammedPhasePlan loaded = TrafficSignalProgrammedPhasePlan.fromNBT(saved);
    assertEquals(NONE, loaded.getManualPatternSlot());
    assertEquals(1, loaded.getSwitchPatternSlot());
    assertEquals(0, loaded.getWantedCoordinationSlot(null, false));
    assertEquals(1, loaded.getWantedCoordinationSlot(null, true));
    assertEquals(Source.SWITCH, loaded.getPatternSource(true));
  }

  @Test
  void patternSelectSettingsSurviveASave() {
    for (int manual = NONE; manual < TrafficTimeOfDaySchedule.SLOT_COUNT; manual++) {
      for (int sw = NONE; sw < TrafficTimeOfDaySchedule.SLOT_COUNT; sw++) {
        TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
        plan.setManualPatternSlot(manual);
        plan.setSwitchPatternSlot(sw);
        TrafficSignalProgrammedPhasePlan loaded =
            TrafficSignalProgrammedPhasePlan.fromNBT(plan.toNBT());
        assertEquals(manual, loaded.getManualPatternSlot());
        assertEquals(sw, loaded.getSwitchPatternSlot());
      }
    }
  }

  @Test
  void storedSettingsOutOfRangeAreDropped() {
    NBTTagCompound saved = TrafficSignalProgrammedPhasePlan.createDefault().toNBT();
    saved.setInteger("cpm", 9);
    saved.setInteger("cps", -4);
    TrafficSignalProgrammedPhasePlan loaded = TrafficSignalProgrammedPhasePlan.fromNBT(saved);
    assertEquals(NONE, loaded.getManualPatternSlot());
    assertEquals(NONE, loaded.getSwitchPatternSlot());
  }

  @Test
  void theSettingsWorkWithTheTimeOfDayTableOff() {
    // The stadium case: no clock table at all, pattern 1 normally and pattern 2 on the switch.
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    assertFalse(plan.isTimeOfDayPatterns());
    assertEquals(0, plan.getWantedCoordinationSlot(null, false));
    assertEquals(1, plan.getWantedCoordinationSlot(null, true));
    plan.setManualPatternSlot(3);
    assertEquals(3, plan.getWantedCoordinationSlot(null, false));
    assertEquals(1, plan.getWantedCoordinationSlot(null, true));
  }

  @Test
  void thePlanJsonCarriesTheSettingsAndAPlanWithoutThemKeepsTheControllers() throws Exception {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    plan.setManualPatternSlot(2);
    plan.setSwitchPatternSlot(3);
    String json = AdvancedPlanJson.export(plan, null, 20L, 60L, "t");
    TrafficSignalProgrammedPhasePlan back =
        AdvancedPlanJson.apply(json, TrafficSignalProgrammedPhasePlan.createDefault()).plan;
    assertEquals(2, back.getManualPatternSlot());
    assertEquals(3, back.getSwitchPatternSlot());

    // SPATT writes neither key: importing its plan must not let go of a hold or forget the switch.
    String without = json.replaceAll("\"manualSlot\"\\s*:\\s*-?\\d+\\s*,?", "")
        .replaceAll("\"switchSlot\"\\s*:\\s*-?\\d+\\s*,?", "")
        .replaceAll(",(\\s*})", "$1");
    assertFalse(without.contains("manualSlot") || without.contains("switchSlot"));
    TrafficSignalProgrammedPhasePlan kept = AdvancedPlanJson.apply(without, back).plan;
    assertEquals(2, kept.getManualPatternSlot());
    assertEquals(3, kept.getSwitchPatternSlot());
  }

  // endregion

  // region: the engine

  private static final long NORMAL_CYCLE = 1800L;
  private static final long EVENT_CYCLE = 2400L;

  /** Mains 2 and 6 coordinated, side street 4 and 8; pattern 2 the same on a longer cycle. */
  private static TrafficSignalProgrammedPhasePlan twoPatternPlan() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 6, 1);
    RingBarrierStateTest.enable(plan, 4, 2);
    RingBarrierStateTest.enable(plan, 8, 3);
    plan.getCoordination(0).setMode(TrafficSignalCoordinationMode.COORDINATED);
    plan.getCoordination(0).setCycleLength(NORMAL_CYCLE);
    plan.getCoordination(1).setMode(TrafficSignalCoordinationMode.COORDINATED);
    plan.getCoordination(1).setCycleLength(EVENT_CYCLE);
    return plan;
  }

  private static Demand everyApproachCalling(int circuits) {
    Demand d = new Demand();
    for (int i = 0; i < circuits; i++) {
      d.veh(i, 1, 0, 0);
    }
    return d;
  }

  @Test
  void aSwitchedPatternIsTakenUpAtTheCycleBoundaryAndGivenBackAtTheNext() {
    TrafficSignalProgrammedPhasePlan plan = twoPatternPlan();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    Demand demand = everyApproachCalling(4);
    RingBarrierState rb = new RingBarrierState();

    long t = 0L;
    for (; t < NORMAL_CYCLE + 600L; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      assertEquals(0, rb.getActiveCoordinationSlot(), "pattern 1 with the switch off, t=" + t);
    }

    // The lever is thrown a third of the way into a cycle: nothing changes until it ends.
    rb.setPatternSwitchOn(true);
    long boundary = 2 * NORMAL_CYCLE;
    for (; t < boundary; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      assertEquals(0, rb.getActiveCoordinationSlot(), "still pattern 1 mid-cycle, t=" + t);
    }
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t++, demand);
    assertEquals(1, rb.getActiveCoordinationSlot(), "pattern 2 from the boundary (within a cycle)");

    // On the event pattern every phase is still served.
    Set<Integer> served = new HashSet<>();
    long eventEnd = t + 2 * EVENT_CYCLE;
    for (; t < eventEnd; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      assertEquals(1, rb.getActiveCoordinationSlot(), "pattern 2 while the switch is on, t=" + t);
      noteGreens(rb, served);
    }
    assertEquals(new HashSet<>(java.util.Arrays.asList(2, 4, 6, 8)), served,
        "every phase served on the switched pattern");

    // Lever off: back to pattern 1, again only when the running (event) cycle ends.
    rb.setPatternSwitchOn(false);
    long changedAt = -1L;
    long limit = t + EVENT_CYCLE + 1L;
    for (; t < limit; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      if (rb.getActiveCoordinationSlot() == 0) {
        changedAt = t;
        break;
      }
    }
    assertTrue(changedAt >= 0L, "back on pattern 1 within one cycle of the switch going off");
    assertEquals(0L, changedAt % EVENT_CYCLE, "and at the event cycle's boundary, not before");

    served.clear();
    long normalEnd = changedAt + 3 * NORMAL_CYCLE;
    for (t = changedAt + 1L; t < normalEnd; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      noteGreens(rb, served);
    }
    assertEquals(new HashSet<>(java.util.Arrays.asList(2, 4, 6, 8)), served,
        "every phase served again on the normal pattern");
  }

  @Test
  void aManualHoldIsTakenUpTheSameWay() {
    TrafficSignalProgrammedPhasePlan plan = twoPatternPlan();
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(4);
    Demand demand = everyApproachCalling(4);
    RingBarrierState rb = new RingBarrierState();
    long t = 0L;
    for (; t < 500L; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
    }
    plan.setManualPatternSlot(1);
    for (; t < NORMAL_CYCLE; t++) {
      rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
      assertEquals(0, rb.getActiveCoordinationSlot(), "held pattern waits for the boundary");
    }
    rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, demand);
    assertEquals(1, rb.getActiveCoordinationSlot());
  }

  @Test
  void aControllerThatStartsWithTheSwitchOnStartsOnItsPattern() {
    // A reload with the lever already thrown: the cold start takes the wanted pattern at once.
    TrafficSignalProgrammedPhasePlan plan = twoPatternPlan();
    RingBarrierState rb = new RingBarrierState();
    rb.setPatternSwitchOn(true);
    rb.tick(plan, RingBarrierStateTest.circuits(4), RingBarrierStateTest.NO_OVERLAPS, 700L,
        everyApproachCalling(4));
    assertEquals(1, rb.getActiveCoordinationSlot());
  }

  private static void noteGreens(RingBarrierState rb, Set<Integer> served) {
    for (int ring = 1; ring <= 2; ring++) {
      ServedMovement m = rb.getLastServed(ring);
      if (m != null && m.vehicle == VehInterval.GREEN) {
        served.add(m.phaseNumber);
      }
    }
  }

  // endregion

  // region: preemption

  /** The preempt's trigger circuit in the Alto fixture (dwelling phase 4). */
  private static final int PREEMPT_CIRCUIT = 2;

  @Test
  void aPreemptWinsWhicheverPatternIsSelected() throws Exception {
    File f = new File("modules/roads/src/test/resources/alto/"
        + "midtown_mad86_controller_20261118.snbt");
    if (!f.isFile()) {
      f = new File(System.getProperty("user.dir"), f.getPath());
    }
    NBTTagCompound c = JsonToNBT.getTagFromJson(
        new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
    TrafficSignalControllerCircuits ckts =
        TrafficSignalControllerCircuits.fromNBT(c.getCompoundTag("tcCrc"));

    for (boolean switchOn : new boolean[]{false, true}) {
      TrafficSignalProgrammedPhasePlan plan =
          TrafficSignalProgrammedPhasePlan.fromNBT(c.getCompoundTag("tcAdv"));
      // Pattern 2: the same junction on a longer cycle, as an event plan would be.
      TrafficSignalCoordinationPlan normal = plan.getCoordination(0);
      TrafficSignalCoordinationPlan event = plan.getCoordination(1);
      event.setMode(TrafficSignalCoordinationMode.COORDINATED);
      event.setCycleLength(normal.getCycleLength() + 600L);
      event.setOffset(normal.getOffset());
      long cycle = switchOn ? event.getCycleLength() : normal.getCycleLength();

      Demand normalDemand = new Demand();
      Demand preemptDemand = new Demand();
      for (int i = 0; i < ckts.getCircuitCount(); i++) {
        normalDemand.veh(i, 1, 1, 0);
        preemptDemand.veh(i, 1, 1, 0);
      }
      preemptDemand.summaries.get(PREEMPT_CIRCUIT).withPreemptDetectorCalls(1);

      RingBarrierState rb = new RingBarrierState();
      rb.setPatternSwitchOn(switchOn);
      long start = 3 * cycle + 450L;
      long release = start + 600L;
      boolean preempted = false;
      for (long t = 0; t < release; t++) {
        boolean calling = t >= start;
        rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t,
            calling ? preemptDemand : normalDemand);
        if (calling && rb.getActivePreemptTriggerCircuit(plan) == PREEMPT_CIRCUIT) {
          preempted = true;
        }
      }
      assertTrue(preempted, "the preempt took the junction with the switch "
          + (switchOn ? "on" : "off"));
      assertEquals(switchOn ? 1 : 0, rb.getActiveCoordinationSlot(),
          "and the selected pattern is still the one in force behind it");

      // Afterwards the selected pattern carries on and serves every phase the plan enables.
      Set<Integer> enabled = new HashSet<>();
      for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
        if (p.isEnabled() && p.getMovement() != TrafficSignalPhaseMovement.PED) {
          enabled.add(p.getPhaseNumber());
        }
      }
      Set<Integer> served = new HashSet<>();
      for (long t = release; t < release + 4 * cycle; t++) {
        rb.tick(plan, ckts, RingBarrierStateTest.NO_OVERLAPS, t, normalDemand);
        noteGreens(rb, served);
      }
      assertTrue(served.containsAll(enabled), "after the preempt every phase is served again ("
          + served + " of " + enabled + ", switch " + (switchOn ? "on" : "off") + ")");
      assertEquals(switchOn ? 1 : 0, rb.getActiveCoordinationSlot());
    }
  }

  // endregion
}
