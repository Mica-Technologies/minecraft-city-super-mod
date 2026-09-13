package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerTicker.REQUESTABLE_NO_CHANGE;
import static com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerTicker.requestableNextPhaseIndex;
import static com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPhaseApplicability.*;
import static com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPhases.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The requestable sequence as a function of what is linked: which steps a request waits on,
 * and which are skipped because nothing linked needs them.
 */
class RequestableSequencingTest {

  private static final long YELLOW = 60;
  private static final long FLASH_DW = 200;
  private static final long ALL_RED = 40;
  private static final long MIN_SERVICE = 500;
  private static final long MAX_SERVICE = 2400;
  private static final long MIN_GREEN = 100;

  /** The full signalised crossing: main street ped signals, a HAWK, vehicle heads both ways. */
  private static final RequestableCircuitNeeds EVERYTHING =
      new RequestableCircuitNeeds(true, true, true, true);

  /** An RRFB crossing: nothing on the main street but the beacons, nothing served but buttons. */
  private static final RequestableCircuitNeeds RRFB_ONLY =
      new RequestableCircuitNeeds(false, false, false, false);

  private static int next(TrafficSignalPhaseApplicability current, long time, boolean request,
      int demand, RequestableCircuitNeeds needs) {
    return requestableNextPhaseIndex(current, time, true, request, demand, needs, YELLOW,
        FLASH_DW, ALL_RED, MIN_SERVICE, MAX_SERVICE, MIN_GREEN);
  }

  @Nested
  @DisplayName("an RRFB crossing")
  class RrfbCrossing {

    @Test
    @DisplayName("serves the request at once: the beacons come on with the WALK")
    void requestGoesStraightToService() {
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN, true, 0, RRFB_ONLY));
    }

    @Test
    @DisplayName("returns to default green straight after the pedestrian clearance")
    void noServiceClearance() {
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN,
          next(REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK, YELLOW, false, 0, RRFB_ONLY));
    }

    @Test
    @DisplayName("still runs the whole pedestrian clearance before returning")
    void clearanceIsNotShortened() {
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW,
          next(REQUESTABLE_SERVICE_GREEN, MIN_SERVICE, false, 0, RRFB_ONLY));
      assertEquals(REQUESTABLE_NO_CHANGE,
          next(REQUESTABLE_SERVICE_GREEN_FLASH_DW, FLASH_DW - YELLOW - 1, false, 0, RRFB_ONLY));
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1,
          next(REQUESTABLE_SERVICE_GREEN_FLASH_DW, FLASH_DW - YELLOW, false, 0, RRFB_ONLY));
      // the wig-wag phase is re-applied every tick until its time is up, so the alternator
      // can switch frames; that is not an exit
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1,
          next(REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK, YELLOW - 1, false, 0, RRFB_ONLY));
    }
  }

  @Nested
  @DisplayName("a signalised crossing")
  class SignalisedCrossing {

    @Test
    @DisplayName("clears the main street's pedestrians first")
    void flashDontWalkFirst() {
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN, true, 0, EVERYTHING));
    }

    @Test
    @DisplayName("then pre-flashes the HAWK, goes yellow, all-red, and serves")
    void fullApproach() {
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1,
          next(REQUESTABLE_DEFAULT_GREEN_FLASH_DW, FLASH_DW - YELLOW, false, 0, EVERYTHING));
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW,
          next(REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK, YELLOW, false, 0, EVERYTHING));
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_RED,
          next(REQUESTABLE_DEFAULT_YELLOW, YELLOW, false, 0, EVERYTHING));
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN,
          next(REQUESTABLE_DEFAULT_RED, ALL_RED, false, 0, EVERYTHING));
    }

    @Test
    @DisplayName("clears the served street with yellow and all-red on the way back")
    void fullReturn() {
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_YELLOW,
          next(REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK, YELLOW, false, 0, EVERYTHING));
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_RED,
          next(REQUESTABLE_SERVICE_YELLOW, YELLOW, false, 0, EVERYTHING));
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN,
          next(REQUESTABLE_SERVICE_RED, ALL_RED, false, 0, EVERYTHING));
    }
  }

  @Nested
  @DisplayName("each step is taken only for a device that needs it")
  class PerDevice {

    @Test
    @DisplayName("a HAWK alone: pre-flash, yellow and all-red, but no flashing don't walk")
    void hawkOnly() {
      RequestableCircuitNeeds hawk = new RequestableCircuitNeeds(false, true, true, false);
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN, true, 0, hawk));
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW,
          next(REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK, YELLOW, false, 0, hawk));
      // ends its service with the wig-wag and goes dark: no steady red after
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN,
          next(REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK, YELLOW, false, 0, hawk));
    }

    @Test
    @DisplayName("vehicle heads alone: straight to yellow, no pre-flash")
    void vehicleHeadsOnly() {
      RequestableCircuitNeeds heads = new RequestableCircuitNeeds(false, false, true, true);
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN, true, 0, heads));
    }

    @Test
    @DisplayName("main street ped signals with only RRFBs: clearance, then straight to service")
    void pedSignalsThenService() {
      RequestableCircuitNeeds peds = new RequestableCircuitNeeds(true, false, false, false);
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN, true, 0, peds));
      // the HAWK-flash phase is still entered: it is the last yellowTime of the clearance
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1,
          next(REQUESTABLE_DEFAULT_GREEN_FLASH_DW, FLASH_DW - YELLOW, false, 0, peds));
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN,
          next(REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK, YELLOW, false, 0, peds));
    }
  }

  @Nested
  @DisplayName("timing that does not depend on what is linked")
  class Timing {

    @Test
    @DisplayName("a request waits out the minimum default green")
    void minGreenHonoured() {
      assertEquals(REQUESTABLE_NO_CHANGE,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN - 1, true, 0, RRFB_ONLY));
    }

    @Test
    @DisplayName("no request, no change")
    void noRequest() {
      assertEquals(REQUESTABLE_NO_CHANGE,
          next(REQUESTABLE_DEFAULT_GREEN, MIN_GREEN * 10, false, 0, null));
    }

    @Test
    @DisplayName("service holds while traffic waits, up to the maximum")
    void serviceHoldsForDemand() {
      assertEquals(REQUESTABLE_NO_CHANGE,
          next(REQUESTABLE_SERVICE_GREEN, MIN_SERVICE, false, 3, null));
      assertEquals(PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW,
          next(REQUESTABLE_SERVICE_GREEN, MAX_SERVICE, false, 3, null));
    }

    @Test
    @DisplayName("starts in default green")
    void startsInDefaultGreen() {
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN, next(null, 0, false, 0, null));
    }

    @Test
    @DisplayName("the flash alternator picks the wig-wag frame")
    void alternator() {
      assertEquals(PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2,
          requestableNextPhaseIndex(REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK, 0, false, false, 0,
              null, YELLOW, FLASH_DW, ALL_RED, MIN_SERVICE, MAX_SERVICE, MIN_GREEN));
    }
  }
}
