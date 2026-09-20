package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The decisions behind a controller's return from a power outage (issue #211): which modes flash,
 * for how long, how flash is left, and that the ADVANCED engine really does come up on the main
 * street when told to.
 */
class TrafficSignalStartupFlashTest {

  @Test
  @DisplayName("the modes that run an intersection flash on start-up; the rest do not")
  void whichModesFlash() {
    assertTrue(TrafficSignalStartupFlash.appliesTo(TrafficSignalControllerMode.NORMAL));
    assertTrue(TrafficSignalStartupFlash.appliesTo(TrafficSignalControllerMode.ADVANCED));
    assertTrue(TrafficSignalStartupFlash.appliesTo(TrafficSignalControllerMode.REQUESTABLE));
    for (TrafficSignalControllerMode mode : new TrafficSignalControllerMode[] {
        TrafficSignalControllerMode.FLASH, TrafficSignalControllerMode.MANUAL_OFF,
        TrafficSignalControllerMode.RAMP_METER_FULL_TIME,
        TrafficSignalControllerMode.RAMP_METER_PART_TIME,
        TrafficSignalControllerMode.WRONG_WAY_DETECTION,
        TrafficSignalControllerMode.OVERHEIGHT_DETECTION,
        TrafficSignalControllerMode.FORCED_FAULT}) {
      assertFalse(TrafficSignalStartupFlash.appliesTo(mode), mode.name());
      assertEquals(0L, TrafficSignalStartupFlash.armedUntil(mode, 5_000L), mode.name());
    }
  }

  @Test
  @DisplayName("the flash runs for its duration and then is over, in a world of any age")
  void howLongItRuns() {
    long now = 3_803_291_727L;
    long until = TrafficSignalStartupFlash.armedUntil(TrafficSignalControllerMode.NORMAL, now);
    assertEquals(now + TrafficSignalStartupFlash.DURATION_TICKS, until);
    assertTrue(TrafficSignalStartupFlash.isActive(until, now));
    assertTrue(TrafficSignalStartupFlash.isActive(until, until - 1));
    assertFalse(TrafficSignalStartupFlash.isActive(until, until));
    assertFalse(TrafficSignalStartupFlash.isActive(0L, now), "0 is no flash, not a flash until 0");
  }

  @Test
  @DisplayName("MUTCD 4D.31: yellow-red flash ends on the main green, all-red flash on steady red")
  void howFlashIsLeft() {
    TrafficSignalControllerMode flash = TrafficSignalControllerMode.FLASH;
    assertTrue(TrafficSignalStartupFlash.resumesOnPrimaryGreen(flash,
        TrafficSignalControllerMode.NORMAL, false));
    assertFalse(TrafficSignalStartupFlash.resumesOnPrimaryGreen(flash,
        TrafficSignalControllerMode.NORMAL, true),
        "all-red flash goes to NORMAL's steady all-red first");
    assertTrue(TrafficSignalStartupFlash.resumesOnPrimaryGreen(flash,
        TrafficSignalControllerMode.ADVANCED, false));
    assertTrue(TrafficSignalStartupFlash.resumesOnPrimaryGreen(flash,
        TrafficSignalControllerMode.ADVANCED, true));
    assertFalse(TrafficSignalStartupFlash.resumesOnPrimaryGreen(flash,
        TrafficSignalControllerMode.REQUESTABLE, false));
    assertFalse(TrafficSignalStartupFlash.resumesOnPrimaryGreen(
        TrafficSignalControllerMode.FORCED_FAULT, TrafficSignalControllerMode.NORMAL, false),
        "only leaving FLASH counts: a cleared fault restarts the ordinary way");
    assertFalse(TrafficSignalStartupFlash.resumesOnPrimaryGreen(
        TrafficSignalControllerMode.NORMAL, flash, false));
  }

  /** Main street 2 and 6, its left turn 1, the side street 4: a left turn and the side street calling. */
  private static TrafficSignalProgrammedPhasePlan planWithAWaitingLeftTurn() {
    TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
    RingBarrierStateTest.enable(plan, 2, 0);
    RingBarrierStateTest.enable(plan, 6, 1);
    RingBarrierStateTest.enable(plan, 1, 2);
    RingBarrierStateTest.enable(plan, 4, 3);
    return plan;
  }

  private static RingBarrierStateTest.Demand leftTurnAndSideStreetCalling() {
    return new RingBarrierStateTest.Demand().veh(2, 3, 3, 0).veh(3, 3, 0, 0);
  }

  @Test
  @DisplayName("an ordinary ADVANCED cold start hands the first green to whoever is calling")
  void ordinaryColdStartServesTheCall() {
    RingBarrierState rb = new RingBarrierState();
    rb.tick(planWithAWaitingLeftTurn(), RingBarrierStateTest.circuits(4),
        RingBarrierStateTest.NO_OVERLAPS, 0L, leftTurnAndSideStreetCalling());
    assertEquals(1, rb.getLastServed(1).phaseNumber,
        "the premise of the next test: without beginOnRestPhases the left turn goes first");
  }

  @Test
  @DisplayName("out of flash, ADVANCED comes up green on the main street whatever is calling")
  void outOfFlashTheMainStreetGoesFirst() {
    RingBarrierState rb = new RingBarrierState();
    rb.beginOnRestPhases();
    TrafficSignalProgrammedPhasePlan plan = planWithAWaitingLeftTurn();
    rb.tick(plan, RingBarrierStateTest.circuits(4), RingBarrierStateTest.NO_OVERLAPS, 0L,
        leftTurnAndSideStreetCalling());
    assertEquals(2, rb.getLastServed(1).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(1).vehicle);
    assertEquals(6, rb.getLastServed(2).phaseNumber);
    assertEquals(VehInterval.GREEN, rb.getLastServed(2).vehicle);
  }

  @Test
  @DisplayName("and the calls that were waiting are still served afterwards")
  void theWaitingCallsAreStillServed() {
    RingBarrierState rb = new RingBarrierState();
    rb.beginOnRestPhases();
    TrafficSignalProgrammedPhasePlan plan = planWithAWaitingLeftTurn();
    boolean sideStreetServed = false;
    for (long t = 0; t <= 4_000L && !sideStreetServed; t += 2) {
      rb.tick(plan, RingBarrierStateTest.circuits(4), RingBarrierStateTest.NO_OVERLAPS, t,
          leftTurnAndSideStreetCalling());
      sideStreetServed = rb.getLastServed(1) != null && rb.getLastServed(1).phaseNumber == 4
          && rb.getLastServed(1).vehicle == VehInterval.GREEN;
    }
    assertTrue(sideStreetServed, "the main street's start-up green never gave way");
  }
}
