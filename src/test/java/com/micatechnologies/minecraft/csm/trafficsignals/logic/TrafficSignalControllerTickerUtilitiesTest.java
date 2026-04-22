package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for pure (no-World) methods in {@link TrafficSignalControllerTickerUtilities}.
 */
class TrafficSignalControllerTickerUtilitiesTest {

  // -- Helpers --

  /**
   * Creates an empty {@link TrafficSignalControllerCircuit} via fromNBT with an empty tag.
   */
  private static TrafficSignalControllerCircuit emptyCircuit() {
    return TrafficSignalControllerCircuit.fromNBT(new NBTTagCompound());
  }

  // ========================================================================
  // getFlashDontWalkTransitionPhaseForUpcoming
  // ========================================================================
  @Nested
  @DisplayName("getFlashDontWalkTransitionPhaseForUpcoming")
  class GetFlashDontWalkTransitionPhaseForUpcomingTest {

    @Test
    @DisplayName("current phase with WALK signals returns FDW transition phase")
    void withWalkSignals_returnsFdwPhase() {
      BlockPos walkPos = new BlockPos(10, 64, 20);
      BlockPos greenPos = new BlockPos(11, 64, 21);

      // Current phase has a walk signal and a green signal
      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addWalkSignal(walkPos);
      current.addGreenSignal(greenPos);

      // Upcoming phase does NOT have the walk signal as walk (it goes to dont walk)
      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addDontWalkSignal(walkPos);
      upcoming.addRedSignal(greenPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(result, "Should return a FDW transition phase");
      assertEquals(TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING,
          result.getApplicability());
      assertTrue(result.getFlashDontWalkSignals().contains(walkPos),
          "Walk signal should be in flash-dont-walk list");
      assertFalse(result.getWalkSignals().contains(walkPos),
          "Walk signal should not remain in walk list");
    }

    @Test
    @DisplayName("current phase with no WALK signals returns null")
    void noWalkSignals_returnsNull() {
      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNull(result, "Should return null when no walk signals");
    }

    @Test
    @DisplayName("WALK signals staying WALK in upcoming are preserved (not converted to FDW)")
    void walkStaysWalk_preserved() {
      BlockPos walkStays = new BlockPos(10, 64, 20);
      BlockPos walkGoes = new BlockPos(11, 64, 21);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addWalkSignal(walkStays);
      current.addWalkSignal(walkGoes);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(walkStays); // stays as walk
      upcoming.addDontWalkSignal(walkGoes); // transitions away

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(result);
      assertTrue(result.getWalkSignals().contains(walkStays),
          "Walk signal staying walk should remain in walk list");
      assertTrue(result.getFlashDontWalkSignals().contains(walkGoes),
          "Walk signal going away should be in FDW list");
    }

    @Test
    @DisplayName("all walk signals stay walk - returns null (no FDW signals added)")
    void allWalkStaysWalk_returnsNull() {
      BlockPos walkPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addWalkSignal(walkPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(walkPos); // stays as walk

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNull(result, "Should return null when all walk signals stay as walk");
    }
  }

  // ========================================================================
  // getYellowTransitionPhaseForUpcoming
  // ========================================================================
  @Nested
  @DisplayName("getYellowTransitionPhaseForUpcoming")
  class GetYellowTransitionPhaseForUpcomingTest {

    @Test
    @DisplayName("GREEN signals going to RED become YELLOW in transition")
    void greenToRed_becomesYellow() {
      BlockPos greenPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(greenPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(greenPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertEquals(TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING,
          result.getApplicability());
      assertTrue(result.getYellowSignals().contains(greenPos),
          "Green-to-red signal should be yellow in transition");
      assertFalse(result.getGreenSignals().contains(greenPos));
    }

    @Test
    @DisplayName("GREEN signals staying GREEN remain GREEN (no transition)")
    void greenStaysGreen_remainsGreen() {
      BlockPos greenPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(greenPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(greenPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getGreenSignals().contains(greenPos),
          "Signal staying green should remain green");
      assertFalse(result.getYellowSignals().contains(greenPos));
    }

    @Test
    @DisplayName("FYA signals going to RED become YELLOW")
    void fyaToRed_becomesYellow() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(fyaPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getYellowSignals().contains(fyaPos),
          "FYA going to RED should become yellow in transition");
    }

    @Test
    @DisplayName("FYA signals staying FYA remain FYA")
    void fyaStaysFya_remainsFya() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addFyaSignal(fyaPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getFyaSignals().contains(fyaPos),
          "FYA staying FYA should remain as FYA");
    }

    @Test
    @DisplayName("RED signals staying RED remain RED")
    void redStaysRed_remainsRed() {
      BlockPos redPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addRedSignal(redPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(redPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getRedSignals().contains(redPos),
          "Red signal should remain red in transition");
    }

    @Test
    @DisplayName("OFF signals going to non-OFF get YELLOW (hybrid left/right 3-section clearance)")
    void offToNonOff_becomesYellow() {
      BlockPos offPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addOffSignal(offPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(offPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getYellowSignals().contains(offPos),
          "Off signal going to non-off should get yellow clearance");
    }

    @Test
    @DisplayName("OFF signals staying OFF remain OFF")
    void offStayingOff_staysOff() {
      BlockPos offPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addOffSignal(offPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addOffSignal(offPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getOffSignals().contains(offPos),
          "Off signal staying off should remain off");
    }

    @Test
    @DisplayName("returned phase has YELLOW_TRANSITIONING applicability")
    void returnedPhaseHasCorrectApplicability() {
      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertEquals(TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING,
          result.getApplicability());
    }
  }

  // ========================================================================
  // getRedTransitionPhaseForUpcoming
  // ========================================================================
  @Nested
  @DisplayName("getRedTransitionPhaseForUpcoming")
  class GetRedTransitionPhaseForUpcomingTest {

    @Test
    @DisplayName("YELLOW signals become RED")
    void yellowBecomesRed() {
      BlockPos yellowPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addYellowSignal(yellowPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getRedSignals().contains(yellowPos),
          "Yellow signal should become red");
      assertFalse(result.getYellowSignals().contains(yellowPos),
          "Yellow signal should not remain yellow");
    }

    @Test
    @DisplayName("GREEN signals staying GREEN in upcoming are preserved")
    void greenPreservedWhenStayingGreen() {
      BlockPos greenPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(greenPos);

      TrafficSignalPhase current = new TrafficSignalPhase(1, upcoming,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addGreenSignal(greenPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getGreenSignals().contains(greenPos),
          "Green signal staying green in upcoming should be preserved");
    }

    @Test
    @DisplayName("GREEN signals NOT in upcoming become RED")
    void greenBecomesRedWhenNotInUpcoming() {
      BlockPos greenPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase current = new TrafficSignalPhase(1, upcoming,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addGreenSignal(greenPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getRedSignals().contains(greenPos),
          "Green signal not in upcoming should become red");
      assertFalse(result.getGreenSignals().contains(greenPos),
          "Green signal not in upcoming should not stay green");
    }

    @Test
    @DisplayName("RED signals are preserved")
    void redPreserved() {
      BlockPos redPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addRedSignal(redPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getRedSignals().contains(redPos),
          "Red signal should be preserved");
    }

    @Test
    @DisplayName("returned phase has RED_TRANSITIONING applicability")
    void returnedPhaseHasCorrectApplicability() {
      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addYellowSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertEquals(TrafficSignalPhaseApplicability.RED_TRANSITIONING,
          result.getApplicability());
    }
  }

  // ========================================================================
  // maybeWrapWithLpi
  // ========================================================================
  @Nested
  @DisplayName("maybeWrapWithLpi")
  class MaybeWrapWithLpiTest {

    @Test
    @DisplayName("overlapPedestrianSignals=false returns original phase unchanged")
    void overlapDisabled_returnsOriginal() {
      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.maybeWrapWithLpi(
              originating, upcoming, false, 100);

      assertSame(upcoming, result, "Should return original when overlap disabled");
    }

    @Test
    @DisplayName("leadPedestrianIntervalTime=0 returns original unchanged")
    void lpiTimeZero_returnsOriginal() {
      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.maybeWrapWithLpi(
              originating, upcoming, true, 0);

      assertSame(upcoming, result, "Should return original when LPI time is 0");
    }

    @Test
    @DisplayName("no WALK signals in upcoming returns original unchanged")
    void noWalkInUpcoming_returnsOriginal() {
      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(new BlockPos(10, 64, 20));
      // No walk signals

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.maybeWrapWithLpi(
              originating, upcoming, true, 100);

      assertSame(upcoming, result, "Should return original when no walk signals");
    }

    @Test
    @DisplayName("all conditions met returns LPI-wrapped phase")
    void allConditionsMet_returnsLpiPhase() {
      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      originating.addRedSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(new BlockPos(20, 64, 30));
      upcoming.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.maybeWrapWithLpi(
              originating, upcoming, true, 100);

      assertNotSame(upcoming, result, "Should return LPI phase, not original");
      assertEquals(TrafficSignalPhaseApplicability.LEAD_PEDESTRIAN_INTERVAL,
          result.getApplicability());
    }
  }

  // ========================================================================
  // getLpiPhaseForUpcoming
  // ========================================================================
  @Nested
  @DisplayName("getLpiPhaseForUpcoming")
  class GetLpiPhaseForUpcomingTest {

    @Test
    @DisplayName("creates LEAD_PEDESTRIAN_INTERVAL phase")
    void createsLpiPhase() {
      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(new BlockPos(20, 64, 30));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getLpiPhaseForUpcoming(originating, upcoming);

      assertEquals(TrafficSignalPhaseApplicability.LEAD_PEDESTRIAN_INTERVAL,
          result.getApplicability());
    }

    @Test
    @DisplayName("WALK signals from upcoming are included")
    void walkSignalsIncluded() {
      BlockPos walkPos = new BlockPos(20, 64, 30);

      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(walkPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getLpiPhaseForUpcoming(originating, upcoming);

      assertTrue(result.getWalkSignals().contains(walkPos),
          "Walk signals should be included in LPI phase");
    }

    @Test
    @DisplayName("vehicle signals already green in originating stay green")
    void alreadyGreen_staysGreen() {
      BlockPos greenPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      originating.addGreenSignal(greenPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(greenPos);
      upcoming.addWalkSignal(new BlockPos(20, 64, 30));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getLpiPhaseForUpcoming(originating, upcoming);

      assertTrue(result.getGreenSignals().contains(greenPos),
          "Signal already green in originating should stay green during LPI");
    }

    @Test
    @DisplayName("NEW vehicle greens (from red/off) are held RED during LPI")
    void newGreens_heldRed() {
      BlockPos newGreenPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase originating = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);
      originating.addRedSignal(newGreenPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(newGreenPos);
      upcoming.addWalkSignal(new BlockPos(20, 64, 30));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getLpiPhaseForUpcoming(originating, upcoming);

      assertTrue(result.getRedSignals().contains(newGreenPos),
          "Newly-green signal should be held RED during LPI");
      assertFalse(result.getGreenSignals().contains(newGreenPos),
          "Newly-green signal should NOT be green during LPI");
    }
  }

  // ========================================================================
  // addCircuitToPhaseAllRed
  // ========================================================================
  @Nested
  @DisplayName("addCircuitToPhaseAllRed")
  class AddCircuitToPhaseAllRedTest {

    @Test
    @DisplayName("all through/left/right/protected signals become RED")
    void allVehicleSignalsBecomeRed() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getThroughSignals().add(new BlockPos(1, 0, 0));
      circuit.getLeftSignals().add(new BlockPos(2, 0, 0));
      circuit.getRightSignals().add(new BlockPos(3, 0, 0));
      circuit.getProtectedSignals().add(new BlockPos(4, 0, 0));
      circuit.getFlashingLeftSignals().add(new BlockPos(5, 0, 0));
      circuit.getFlashingRightSignals().add(new BlockPos(6, 0, 0));

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);

      TrafficSignalControllerTickerUtilities.addCircuitToPhaseAllRed(circuit, phase, false);

      assertTrue(phase.getRedSignals().contains(new BlockPos(1, 0, 0)));
      assertTrue(phase.getRedSignals().contains(new BlockPos(2, 0, 0)));
      assertTrue(phase.getRedSignals().contains(new BlockPos(3, 0, 0)));
      assertTrue(phase.getRedSignals().contains(new BlockPos(4, 0, 0)));
      assertTrue(phase.getRedSignals().contains(new BlockPos(5, 0, 0)));
      assertTrue(phase.getRedSignals().contains(new BlockPos(6, 0, 0)));
    }

    @Test
    @DisplayName("pedestrianSignalsWalk=true sets peds to WALK")
    void pedsWalk() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getPedestrianSignals().add(new BlockPos(10, 0, 0));
      circuit.getPedestrianAccessorySignals().add(new BlockPos(11, 0, 0));

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);

      TrafficSignalControllerTickerUtilities.addCircuitToPhaseAllRed(circuit, phase, true);

      assertTrue(phase.getWalkSignals().contains(new BlockPos(10, 0, 0)));
      assertTrue(phase.getWalkSignals().contains(new BlockPos(11, 0, 0)));
      assertFalse(phase.getDontWalkSignals().contains(new BlockPos(10, 0, 0)));
    }

    @Test
    @DisplayName("pedestrianSignalsWalk=false sets peds to DONT_WALK")
    void pedsDontWalk() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getPedestrianSignals().add(new BlockPos(10, 0, 0));
      circuit.getPedestrianAccessorySignals().add(new BlockPos(11, 0, 0));

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);

      TrafficSignalControllerTickerUtilities.addCircuitToPhaseAllRed(circuit, phase, false);

      assertTrue(phase.getDontWalkSignals().contains(new BlockPos(10, 0, 0)));
      assertTrue(phase.getDontWalkSignals().contains(new BlockPos(11, 0, 0)));
      assertFalse(phase.getWalkSignals().contains(new BlockPos(10, 0, 0)));
    }

    @Test
    @DisplayName("beacon signals become YELLOW (not RED)")
    void beaconsBeforeYellow() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getBeaconSignals().add(new BlockPos(20, 0, 0));

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);

      TrafficSignalControllerTickerUtilities.addCircuitToPhaseAllRed(circuit, phase, false);

      assertTrue(phase.getYellowSignals().contains(new BlockPos(20, 0, 0)),
          "Beacon signals should be yellow in all-red");
      assertFalse(phase.getRedSignals().contains(new BlockPos(20, 0, 0)),
          "Beacon signals should not be red");
    }

    @Test
    @DisplayName("pedestrian beacon signals become RED")
    void pedestrianBeaconsRed() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getPedestrianBeaconSignals().add(new BlockPos(30, 0, 0));

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_RED);

      TrafficSignalControllerTickerUtilities.addCircuitToPhaseAllRed(circuit, phase, false);

      assertTrue(phase.getRedSignals().contains(new BlockPos(30, 0, 0)),
          "Pedestrian beacon signals should be red in all-red");
    }
  }

  // ========================================================================
  // getPhaseWithOverlapsApplied
  // ========================================================================
  @Nested
  @DisplayName("getPhaseWithOverlapsApplied")
  class GetPhaseWithOverlapsAppliedTest {

    @Test
    @DisplayName("overlap signal moves from RED to GREEN")
    void overlapMovesRedToGreen() {
      BlockPos sourceGreen = new BlockPos(10, 64, 20);
      BlockPos overlapTarget = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(sourceGreen);
      phase.addRedSignal(overlapTarget);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(sourceGreen, overlapTarget);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getGreenSignals().contains(overlapTarget),
          "Overlap target should be moved to green");
      assertFalse(result.getRedSignals().contains(overlapTarget),
          "Overlap target should no longer be red");
    }

    @Test
    @DisplayName("non-overlap signals are unaffected")
    void nonOverlapUnaffected() {
      BlockPos sourceGreen = new BlockPos(10, 64, 20);
      BlockPos overlapTarget = new BlockPos(11, 64, 21);
      BlockPos unrelatedRed = new BlockPos(12, 64, 22);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(sourceGreen);
      phase.addRedSignal(overlapTarget);
      phase.addRedSignal(unrelatedRed);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(sourceGreen, overlapTarget);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getRedSignals().contains(unrelatedRed),
          "Unrelated red signal should remain red");
    }

    @Test
    @DisplayName("with no overlaps, same phase is returned")
    void noOverlaps_samePhaseReturned() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertSame(phase, result, "With no overlaps, should return same phase instance");
    }

    @Test
    @DisplayName("null overlaps returns same phase")
    void nullOverlaps_samePhaseReturned() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, null);

      assertSame(phase, result, "With null overlaps, should return same phase instance");
    }
  }

  // ========================================================================
  // getTransitionPhaseWithOverlapsApplied
  // ========================================================================
  @Nested
  @DisplayName("getTransitionPhaseWithOverlapsApplied")
  class GetTransitionPhaseWithOverlapsAppliedTest {

    @Test
    @DisplayName("yellow source propagates yellow to overlap targets")
    void yellowSourcePropagatesYellow() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      phase.addYellowSignal(source);
      phase.addRedSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
          phase, overlaps);

      assertTrue(phase.getYellowSignals().contains(target),
          "Overlap target should be yellow when source is yellow");
      assertFalse(phase.getRedSignals().contains(target),
          "Overlap target should no longer be red");
    }

    @Test
    @DisplayName("red source propagates red to overlap targets")
    void redSourcePropagatesRed() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.RED_TRANSITIONING);
      phase.addRedSignal(source);
      phase.addOffSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
          phase, overlaps);

      assertTrue(phase.getRedSignals().contains(target),
          "Overlap target should be red when source is red");
      assertFalse(phase.getOffSignals().contains(target),
          "Overlap target should no longer be off");
    }

    @Test
    @DisplayName("green source still propagates green in transition phases")
    void greenSourcePropagatesGreen() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      phase.addGreenSignal(source);
      phase.addRedSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
          phase, overlaps);

      assertTrue(phase.getGreenSignals().contains(target),
          "Overlap target should be green when source is green");
    }
  }

  // ========================================================================
  // hasVehicleSignalConflict
  // ========================================================================
  @Nested
  @DisplayName("hasVehicleSignalConflict")
  class HasVehicleSignalConflictTest {

    @Test
    @DisplayName("identical vehicle signals have no conflict")
    void identicalPhasesNoConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("signal moved from green to FYA is a conflict")
    void greenToFyaIsConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addFyaSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("signal moved from off to FYA is a conflict")
    void offToFyaIsConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addOffSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addFyaSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("ped signal differences are not vehicle conflicts")
    void pedSignalDifferencesNotConflict() {
      BlockPos walkPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addWalkSignal(walkPos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addDontWalkSignal(walkPos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }
  }

  // ========================================================================
  // Red transition: FYA signals become RED
  // ========================================================================
  @Nested
  @DisplayName("getRedTransitionPhaseForUpcoming - FYA handling")
  class GetRedTransitionFyaTest {

    @Test
    @DisplayName("FYA signals become RED during all-red clearance")
    void fyaBecomesRed() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getRedSignals().contains(fyaPos),
          "FYA signal should become red during all-red clearance");
      assertFalse(result.getFyaSignals().contains(fyaPos),
          "FYA signal should not remain FYA during all-red clearance");
    }
  }

  // ========================================================================
  // Yellow transition: FYA→OFF gets yellow clearance (hybrid left/right)
  // ========================================================================
  @Nested
  @DisplayName("getYellowTransitionPhaseForUpcoming - FYA handling")
  class GetYellowTransitionFyaTest {

    @Test
    @DisplayName("FYA→OFF gets solid yellow clearance (permissive→protected transition)")
    void fyaToOff_getsYellow() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addOffSignal(fyaPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getYellowSignals().contains(fyaPos),
          "FYA going to OFF should get solid yellow clearance");
      assertFalse(result.getFyaSignals().contains(fyaPos),
          "FYA going to OFF should not stay as FYA");
    }

    @Test
    @DisplayName("FYA→FYA stays FYA (no clearance needed)")
    void fyaToFya_staysFya() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addFyaSignal(fyaPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getFyaSignals().contains(fyaPos),
          "FYA staying FYA should remain as FYA");
    }

    @Test
    @DisplayName("FYA→RED gets solid yellow clearance")
    void fyaToRed_getsYellow() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(fyaPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getYellowSignals().contains(fyaPos),
          "FYA going to RED should get solid yellow clearance");
    }
  }

  // ========================================================================
  // computeGreenLeftTurn / computeGreenRightTurn bounds safety
  // ========================================================================
  @Nested
  @DisplayName("computeGreenLeftTurn / computeGreenRightTurn bounds")
  class ComputeGreenTurnBoundsTest {

    @Test
    @DisplayName("circuit number -1 returns false without crash")
    void negativeCircuitReturnsFalse() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      assertFalse(TrafficSignalControllerTickerUtilities.computeGreenLeftTurn(
          circuits, -1, true, null));
      assertFalse(TrafficSignalControllerTickerUtilities.computeGreenRightTurn(
          circuits, -1, true, null));
    }

    @Test
    @DisplayName("circuit number 0 returns false without crash")
    void zeroCircuitReturnsFalse() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      assertFalse(TrafficSignalControllerTickerUtilities.computeGreenLeftTurn(
          circuits, 0, true, null));
      assertFalse(TrafficSignalControllerTickerUtilities.computeGreenRightTurn(
          circuits, 0, true, null));
    }

    @Test
    @DisplayName("circuit number beyond count returns false without crash")
    void beyondCountReturnsFalse() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      assertFalse(TrafficSignalControllerTickerUtilities.computeGreenLeftTurn(
          circuits, 5, true, null));
      assertFalse(TrafficSignalControllerTickerUtilities.computeGreenRightTurn(
          circuits, 5, true, null));
    }
  }

  // ========================================================================
  // TrafficSignalPhase hashCode/equals consistency
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalPhase hashCode/equals")
  class PhaseHashCodeEqualsTest {

    @Test
    @DisplayName("phases differing only in greenSignals have different hashCodes")
    void greenSignalsDifferentHashCodes() {
      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(new BlockPos(99, 64, 99));

      assertNotEquals(a, b, "Phases with different green signals should not be equal");
      assertNotEquals(a.hashCode(), b.hashCode(),
          "Phases with different green signals should have different hash codes");
    }

    @Test
    @DisplayName("equal phases have equal hashCodes")
    void equalPhasesEqualHashCodes() {
      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(new BlockPos(10, 64, 20));
      a.addRedSignal(new BlockPos(11, 64, 21));

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(new BlockPos(10, 64, 20));
      b.addRedSignal(new BlockPos(11, 64, 21));

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }
  }

  // ========================================================================
  // TrafficSignalPhases ALL_RED applicability
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalPhases cached ALL_RED")
  class CachedAllRedTest {

    @Test
    @DisplayName("cached all-red phase has ALL_RED applicability")
    void allRedHasCorrectApplicability() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      TrafficSignalPhases phases = new TrafficSignalPhases(null, circuits);
      TrafficSignalPhase allRed = phases.getPhase(TrafficSignalPhases.PHASE_INDEX_ALL_RED);

      assertEquals(TrafficSignalPhaseApplicability.ALL_RED, allRed.getApplicability(),
          "Cached ALL_RED phase must have ALL_RED applicability");
    }
  }
}
