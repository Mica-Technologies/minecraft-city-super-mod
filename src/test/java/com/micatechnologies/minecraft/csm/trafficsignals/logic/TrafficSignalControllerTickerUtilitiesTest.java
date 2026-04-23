package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    @DisplayName("signal moved from off to FYA is safe (turning on)")
    void offToFyaIsSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addOffSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addFyaSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("FYA to OFF is safe (permissive→protected compound hybrid)")
    void fyaToOffIsSafe() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addFyaSignal(fyaPos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addOffSignal(fyaPos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("FYA to RED is a conflict (permissive ending restrictively)")
    void fyaToRedIsConflict() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addFyaSignal(fyaPos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addRedSignal(fyaPos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("RED to GREEN is safe (signal was restrictive, now serving)")
    void redToGreenIsSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addRedSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("GREEN to RED is a conflict")
    void greenToRedIsConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addRedSignal(pos);

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
  // Red transition: FYA signal handling
  // ========================================================================
  @Nested
  @DisplayName("getRedTransitionPhaseForUpcoming - FYA handling")
  class GetRedTransitionFyaTest {

    @Test
    @DisplayName("FYA signals NOT in upcoming FYA become RED")
    void fyaNotInUpcoming_becomesRed() {
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
          "FYA signal not in upcoming should become red");
      assertFalse(result.getFyaSignals().contains(fyaPos),
          "FYA signal not in upcoming should not remain FYA");
    }

    @Test
    @DisplayName("FYA signals staying FYA in upcoming are preserved (no red blip)")
    void fyaStayingFya_preserved() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      upcoming.addFyaSignal(fyaPos);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getFyaSignals().contains(fyaPos),
          "FYA staying FYA in upcoming should be preserved");
      assertFalse(result.getRedSignals().contains(fyaPos),
          "FYA staying FYA should not become red");
    }

    @Test
    @DisplayName("mixed FYA: one stays, one goes to red")
    void mixedFya_oneStaysOneGoes() {
      BlockPos stays = new BlockPos(10, 64, 20);
      BlockPos goes = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      current.addFyaSignal(stays);
      current.addFyaSignal(goes);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      upcoming.addFyaSignal(stays);
      upcoming.addRedSignal(goes);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(result.getFyaSignals().contains(stays),
          "FYA staying FYA should be preserved");
      assertTrue(result.getRedSignals().contains(goes),
          "FYA going to red should become red");
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
  // REGRESSION: ALL_LEFTS → directional phase asymmetric clearance
  // ========================================================================
  @Nested
  @DisplayName("Regression: ALL_LEFTS → directional phase transitions")
  class AllLeftsToDirectionalTransitionTest {

    @Test
    @DisplayName("ALL_LEFTS→ALL_EAST: left signal going to RED gets YELLOW clearance")
    void leftGoingRed_getsYellow() {
      BlockPos eastLeft = new BlockPos(10, 64, 20);
      BlockPos westLeft = new BlockPos(20, 64, 30);

      TrafficSignalPhase allLefts = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      allLefts.addGreenSignal(eastLeft);
      allLefts.addGreenSignal(westLeft);

      TrafficSignalPhase allEast = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      allEast.addGreenSignal(eastLeft);
      allEast.addRedSignal(westLeft);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              allLefts, allEast);

      assertTrue(yellow.getYellowSignals().contains(westLeft),
          "West left going RED should get YELLOW clearance");
    }

    @Test
    @DisplayName("ALL_LEFTS→ALL_EAST: left staying green remains GREEN in yellow transition")
    void leftStayingGreen_staysGreen() {
      BlockPos eastLeft = new BlockPos(10, 64, 20);
      BlockPos westLeft = new BlockPos(20, 64, 30);

      TrafficSignalPhase allLefts = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      allLefts.addGreenSignal(eastLeft);
      allLefts.addGreenSignal(westLeft);

      TrafficSignalPhase allEast = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      allEast.addGreenSignal(eastLeft);
      allEast.addRedSignal(westLeft);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              allLefts, allEast);

      assertTrue(yellow.getGreenSignals().contains(eastLeft),
          "East left staying GREEN should remain GREEN");
    }

    @Test
    @DisplayName("ALL_LEFTS→ALL_EAST: red transition turns yellow to red and preserves green")
    void redTransition_yellowToRed_greenPreserved() {
      BlockPos eastLeft = new BlockPos(10, 64, 20);
      BlockPos westLeft = new BlockPos(20, 64, 30);

      TrafficSignalPhase allEast = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      allEast.addGreenSignal(eastLeft);
      allEast.addRedSignal(westLeft);

      TrafficSignalPhase yellowTransition = new TrafficSignalPhase(1, allEast,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      yellowTransition.addGreenSignal(eastLeft);
      yellowTransition.addYellowSignal(westLeft);

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellowTransition, allEast);

      assertTrue(red.getRedSignals().contains(westLeft),
          "Yellow signal should become RED in red transition");
      assertTrue(red.getGreenSignals().contains(eastLeft),
          "Green signal staying green in upcoming should remain GREEN");
    }

    @Test
    @DisplayName("ALL_LEFTS→ALL_THROUGHS: both lefts get symmetric yellow clearance")
    void bothLeftsGetSymmetricClearance() {
      BlockPos eastLeft = new BlockPos(10, 64, 20);
      BlockPos westLeft = new BlockPos(20, 64, 30);
      BlockPos eastThrough = new BlockPos(30, 64, 40);

      TrafficSignalPhase allLefts = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      allLefts.addGreenSignal(eastLeft);
      allLefts.addGreenSignal(westLeft);
      allLefts.addRedSignal(eastThrough);

      TrafficSignalPhase allThroughs = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      allThroughs.addRedSignal(eastLeft);
      allThroughs.addRedSignal(westLeft);
      allThroughs.addGreenSignal(eastThrough);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              allLefts, allThroughs);

      assertTrue(yellow.getYellowSignals().contains(eastLeft),
          "East left should get YELLOW clearance when upcoming has it RED");
      assertTrue(yellow.getYellowSignals().contains(westLeft),
          "West left should get YELLOW clearance when upcoming has it RED");
      assertFalse(yellow.getGreenSignals().contains(eastLeft),
          "East left should not remain GREEN");
      assertFalse(yellow.getGreenSignals().contains(westLeft),
          "West left should not remain GREEN");
    }
  }

  // ========================================================================
  // REGRESSION: Compound hybrid FYA→GREEN (no clearance needed)
  // ========================================================================
  @Nested
  @DisplayName("Regression: Compound hybrid FYA→GREEN transition")
  class CompoundHybridFyaToGreenTest {

    @Test
    @DisplayName("FYA→GREEN gets YELLOW clearance per MUTCD")
    void fyaToGreen_getsYellow() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(fyaPos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(yellow.getYellowSignals().contains(fyaPos),
          "FYA→GREEN should get YELLOW clearance");
      assertFalse(yellow.getFyaSignals().contains(fyaPos));
      assertFalse(yellow.getGreenSignals().contains(fyaPos));
    }

    @Test
    @DisplayName("OFF→OFF stays OFF (add-on block inactive in both phases)")
    void offToOff_staysOff() {
      BlockPos offPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addOffSignal(offPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addOffSignal(offPos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(yellow.getOffSignals().contains(offPos),
          "OFF→OFF should stay OFF");
      assertFalse(yellow.getYellowSignals().contains(offPos));
    }

    @Test
    @DisplayName("OFF→GREEN gets YELLOW clearance (3-section block waking up)")
    void offToGreen_getsYellow() {
      BlockPos offPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addOffSignal(offPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      upcoming.addGreenSignal(offPos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(yellow.getYellowSignals().contains(offPos),
          "OFF→GREEN should get YELLOW clearance");
    }

    @Test
    @DisplayName("OFF→FYA gets YELLOW clearance (turning on to permissive)")
    void offToFya_getsYellow() {
      BlockPos offPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      current.addOffSignal(offPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addFyaSignal(offPos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);

      assertTrue(yellow.getYellowSignals().contains(offPos),
          "OFF→FYA should get YELLOW clearance");
    }
  }

  // ========================================================================
  // Full yellow→red→upcoming transition chain tests
  // ========================================================================
  @Nested
  @DisplayName("Full phase transition chain (yellow → red)")
  class FullTransitionChainTest {

    @Test
    @DisplayName("GREEN→RED: full chain gives yellow then red")
    void greenToRed_fullChain() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(pos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(pos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);
      assertTrue(yellow.getYellowSignals().contains(pos), "Step 1: should be YELLOW");

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellow, upcoming);
      assertTrue(red.getRedSignals().contains(pos), "Step 2: should be RED");
    }

    @Test
    @DisplayName("FYA→RED: full chain gives yellow then red")
    void fyaToRed_fullChain() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(pos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(pos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);
      assertTrue(yellow.getYellowSignals().contains(pos), "Step 1: FYA should become YELLOW");

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellow, upcoming);
      assertTrue(red.getRedSignals().contains(pos), "Step 2: YELLOW should become RED");
    }

    @Test
    @DisplayName("OFF→RED: full chain gives yellow then red")
    void offToRed_fullChain() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addOffSignal(pos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(pos);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);
      assertTrue(yellow.getYellowSignals().contains(pos), "Step 1: OFF should get YELLOW");

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellow, upcoming);
      assertTrue(red.getRedSignals().contains(pos), "Step 2: YELLOW should become RED");
    }

    @Test
    @DisplayName("multi-signal chain: some green→red, some stay green")
    void mixedChain() {
      BlockPos stays = new BlockPos(10, 64, 20);
      BlockPos goes = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(stays);
      current.addGreenSignal(goes);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(stays);
      upcoming.addRedSignal(goes);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              current, upcoming);
      assertTrue(yellow.getGreenSignals().contains(stays));
      assertTrue(yellow.getYellowSignals().contains(goes));

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellow, upcoming);
      assertTrue(red.getGreenSignals().contains(stays),
          "Signal staying green should remain green through red transition");
      assertTrue(red.getRedSignals().contains(goes),
          "Signal going away should be RED in red transition");
    }

    @Test
    @DisplayName("compound hybrid protected→permissive full chain")
    void compoundHybridProtectedToPermissive() {
      BlockPos addOn = new BlockPos(10, 64, 20);
      BlockPos threeSec = new BlockPos(20, 64, 30);

      TrafficSignalPhase protectedPhase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      protectedPhase.addGreenSignal(addOn);
      protectedPhase.addOffSignal(threeSec);

      TrafficSignalPhase permissivePhase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      permissivePhase.addOffSignal(addOn);
      permissivePhase.addFyaSignal(threeSec);

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              protectedPhase, permissivePhase);

      assertTrue(yellow.getYellowSignals().contains(addOn),
          "Add-on GREEN→OFF should get yellow clearance");
      assertTrue(yellow.getYellowSignals().contains(threeSec),
          "3-section OFF→FYA should get yellow clearance");
    }
  }

  // ========================================================================
  // REGRESSION: moveOverlapSignalToGreen ped signal hardening
  // ========================================================================
  @Nested
  @DisplayName("Regression: moveOverlapSignalToGreen ped signal rejection")
  class MoveOverlapPedHardeningTest {

    @Test
    @DisplayName("WALK signal is NOT moved to green by overlap")
    void walkNotMoved() {
      BlockPos walkPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addWalkSignal(walkPos);

      boolean moved = phase.moveOverlapSignalToGreen(walkPos);

      assertFalse(moved, "Walk signal should not be moved by overlap");
      assertTrue(phase.getWalkSignals().contains(walkPos),
          "Walk signal should remain in walk list");
      assertFalse(phase.getGreenSignals().contains(walkPos),
          "Walk signal should not appear in green list");
    }

    @Test
    @DisplayName("FLASH_DONT_WALK signal is NOT moved to green by overlap")
    void flashDontWalkNotMoved() {
      BlockPos fdwPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addFlashDontWalkSignal(fdwPos);

      boolean moved = phase.moveOverlapSignalToGreen(fdwPos);

      assertFalse(moved, "FDW signal should not be moved by overlap");
      assertTrue(phase.getFlashDontWalkSignals().contains(fdwPos));
    }

    @Test
    @DisplayName("DONT_WALK signal is NOT moved to green by overlap")
    void dontWalkNotMoved() {
      BlockPos dwPos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addDontWalkSignal(dwPos);

      boolean moved = phase.moveOverlapSignalToGreen(dwPos);

      assertFalse(moved, "DontWalk signal should not be moved by overlap");
      assertTrue(phase.getDontWalkSignals().contains(dwPos));
    }

    @Test
    @DisplayName("RED signal IS moved to green by overlap")
    void redMoved() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignal(pos);

      boolean moved = phase.moveOverlapSignalToGreen(pos);

      assertTrue(moved, "RED signal should be moved to green");
      assertTrue(phase.getGreenSignals().contains(pos));
      assertFalse(phase.getRedSignals().contains(pos));
    }

    @Test
    @DisplayName("YELLOW signal IS moved to green by overlap")
    void yellowMoved() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addYellowSignal(pos);

      boolean moved = phase.moveOverlapSignalToGreen(pos);

      assertTrue(moved, "YELLOW signal should be moved to green");
      assertTrue(phase.getGreenSignals().contains(pos));
      assertFalse(phase.getYellowSignals().contains(pos));
    }

    @Test
    @DisplayName("FYA signal IS moved to green by overlap")
    void fyaMoved() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addFyaSignal(pos);

      boolean moved = phase.moveOverlapSignalToGreen(pos);

      assertTrue(moved, "FYA signal should be moved to green");
      assertTrue(phase.getGreenSignals().contains(pos));
      assertFalse(phase.getFyaSignals().contains(pos));
    }

    @Test
    @DisplayName("OFF signal IS moved to green by overlap")
    void offMoved() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addOffSignal(pos);

      boolean moved = phase.moveOverlapSignalToGreen(pos);

      assertTrue(moved, "OFF signal should be moved to green");
      assertTrue(phase.getGreenSignals().contains(pos));
      assertFalse(phase.getOffSignals().contains(pos));
    }

    @Test
    @DisplayName("signal already GREEN returns false (no-op)")
    void alreadyGreen_returnsFalse() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(pos);

      boolean moved = phase.moveOverlapSignalToGreen(pos);

      assertFalse(moved, "Already-green signal should return false");
      assertTrue(phase.getGreenSignals().contains(pos));
    }
  }

  // ========================================================================
  // hasVehicleSignalConflict - comprehensive matrix
  // ========================================================================
  @Nested
  @DisplayName("hasVehicleSignalConflict - extended scenarios")
  class HasVehicleSignalConflictExtendedTest {

    @Test
    @DisplayName("empty phases have no conflict")
    void emptyPhasesNoConflict() {
      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("GREEN→YELLOW is a conflict")
    void greenToYellow_isConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addYellowSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("GREEN→OFF is a conflict (signal going dark)")
    void greenToOff_isConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addOffSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("FYA→FYA is safe (no change)")
    void fyaToFya_isSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addFyaSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addFyaSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("FYA→GREEN is safe (permissive→protected, handled by OFF→FYA)")
    void fyaToGreen_isSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addFyaSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("RED→RED is safe (no change)")
    void redToRed_isSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addRedSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addRedSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("RED→FYA is safe (restrictive→permissive)")
    void redToFya_isSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addRedSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addFyaSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("multiple signals: one green→red, one green→green")
    void mixedGreenConflict() {
      BlockPos stays = new BlockPos(10, 64, 20);
      BlockPos goes = new BlockPos(20, 64, 30);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addGreenSignal(stays);
      a.addGreenSignal(goes);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(stays);
      b.addRedSignal(goes);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b),
          "At least one green signal leaving GREEN means conflict");
    }

    @Test
    @DisplayName("FYA→YELLOW is a conflict")
    void fyaToYellow_isConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      a.addFyaSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addYellowSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b),
          "FYA→YELLOW is not flagged because FYA→non-RED is safe");
    }
  }

  // ========================================================================
  // isThroughTypeApplicability / isOmnidirectionalThroughType / isDirectionalPhase
  // ========================================================================
  @Nested
  @DisplayName("isThroughTypeApplicability")
  class IsThroughTypeApplicabilityTest {

    @Test
    @DisplayName("ALL_THROUGHS_RIGHTS is through-type")
    void throughsRightsIsThrough() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
    }

    @Test
    @DisplayName("ALL_THROUGHS_PROTECTED_RIGHTS is through-type")
    void throughsProtectedRightsIsThrough() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS));
    }

    @Test
    @DisplayName("ALL_THROUGHS_PROTECTEDS is through-type")
    void throughsProtectedsIsThrough() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS));
    }

    @Test
    @DisplayName("directional phases are through-type")
    void directionalAreThrough() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_EAST));
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_WEST));
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_NORTH));
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_SOUTH));
    }

    @Test
    @DisplayName("ALL_LEFTS is NOT through-type")
    void leftsNotThrough() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }

    @Test
    @DisplayName("PEDESTRIAN is NOT through-type")
    void pedestrianNotThrough() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.PEDESTRIAN));
    }

    @Test
    @DisplayName("ALL_RED is NOT through-type")
    void allRedNotThrough() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_RED));
    }

    @Test
    @DisplayName("NONE is NOT through-type")
    void noneNotThrough() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.NONE));
    }
  }

  @Nested
  @DisplayName("isOmnidirectionalThroughType")
  class IsOmnidirectionalThroughTypeTest {

    @Test
    @DisplayName("ALL_THROUGHS variants are omnidirectional")
    void throughVariantsAreOmni() {
      assertTrue(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
      assertTrue(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS));
      assertTrue(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS));
    }

    @Test
    @DisplayName("directional phases are NOT omnidirectional")
    void directionalNotOmni() {
      assertFalse(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_EAST));
      assertFalse(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_WEST));
      assertFalse(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_NORTH));
      assertFalse(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_SOUTH));
    }

    @Test
    @DisplayName("ALL_LEFTS is NOT omnidirectional through")
    void leftsNotOmni() {
      assertFalse(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }
  }

  @Nested
  @DisplayName("isDirectionalPhase")
  class IsDirectionalPhaseTest {

    @Test
    @DisplayName("ALL_EAST/WEST/NORTH/SOUTH are directional")
    void directionalPhasesAreDirectional() {
      assertTrue(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_EAST));
      assertTrue(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_WEST));
      assertTrue(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_NORTH));
      assertTrue(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_SOUTH));
    }

    @Test
    @DisplayName("ALL_THROUGHS variants are NOT directional")
    void throughVariantsNotDirectional() {
      assertFalse(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
      assertFalse(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS));
      assertFalse(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS));
    }

    @Test
    @DisplayName("ALL_LEFTS is NOT directional")
    void leftsNotDirectional() {
      assertFalse(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }
  }

  // ========================================================================
  // isSamePhaseCategory
  // ========================================================================
  @Nested
  @DisplayName("isSamePhaseCategory")
  class IsSamePhaseCategoryTest {

    @Test
    @DisplayName("omnidirectional through-type variants are compatible with each other")
    void omniThroughTypesCompatible() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS));
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS));
    }

    @Test
    @DisplayName("directional phases are NOT compatible with omnidirectional through-types")
    void directionalNotCompatibleWithOmni() {
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_EAST,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS,
          TrafficSignalPhaseApplicability.ALL_WEST));
    }

    @Test
    @DisplayName("different directional phases are NOT compatible")
    void differentDirectionalNotCompatible() {
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_EAST,
          TrafficSignalPhaseApplicability.ALL_WEST));
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_NORTH,
          TrafficSignalPhaseApplicability.ALL_SOUTH));
    }

    @Test
    @DisplayName("same directional phase IS compatible (self-recall)")
    void sameDirectionalCompatible() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_EAST,
          TrafficSignalPhaseApplicability.ALL_EAST));
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_WEST,
          TrafficSignalPhaseApplicability.ALL_WEST));
    }

    @Test
    @DisplayName("ALL_LEFTS is only compatible with ALL_LEFTS")
    void leftsOnlyWithLefts() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_LEFTS,
          TrafficSignalPhaseApplicability.ALL_LEFTS));
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_LEFTS,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_LEFTS,
          TrafficSignalPhaseApplicability.ALL_EAST));
    }

    @Test
    @DisplayName("PEDESTRIAN is only compatible with PEDESTRIAN")
    void pedestrianOnlyWithPedestrian() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.PEDESTRIAN,
          TrafficSignalPhaseApplicability.PEDESTRIAN));
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.PEDESTRIAN,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
    }

    @Test
    @DisplayName("omnidirectional through-type is NOT compatible with ALL_LEFTS")
    void throughNotLefts() {
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS,
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }
  }

  // ========================================================================
  // Yellow transition: comprehensive signal state matrix
  // ========================================================================
  @Nested
  @DisplayName("Yellow transition: all signal state transitions")
  class YellowTransitionMatrixTest {

    @Test
    @DisplayName("GREEN→GREEN stays GREEN")
    void greenToGreen() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addGreenSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addGreenSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getGreenSignals().contains(p));
    }

    @Test
    @DisplayName("GREEN→RED becomes YELLOW")
    void greenToRed() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addGreenSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addRedSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getYellowSignals().contains(p));
    }

    @Test
    @DisplayName("GREEN→FYA becomes YELLOW (MUTCD clearance)")
    void greenToFya() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addGreenSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addFyaSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getYellowSignals().contains(p));
    }

    @Test
    @DisplayName("GREEN→OFF becomes YELLOW")
    void greenToOff() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addGreenSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addOffSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getYellowSignals().contains(p));
    }

    @Test
    @DisplayName("FYA→GREEN becomes YELLOW")
    void fyaToGreen() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addFyaSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addGreenSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getYellowSignals().contains(p));
    }

    @Test
    @DisplayName("OFF→RED becomes YELLOW")
    void offToRed() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addOffSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addRedSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getYellowSignals().contains(p));
    }

    @Test
    @DisplayName("RED→RED stays RED")
    void redToRed() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addRedSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addRedSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getRedSignals().contains(p));
    }

    @Test
    @DisplayName("YELLOW→YELLOW stays YELLOW (copied from current)")
    void yellowToYellow() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      curr.addYellowSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addYellowSignal(p);

      TrafficSignalPhase y = TrafficSignalControllerTickerUtilities
          .getYellowTransitionPhaseForUpcoming(curr, next);
      assertTrue(y.getYellowSignals().contains(p));
    }
  }

  // ========================================================================
  // Red transition: comprehensive signal state tests
  // ========================================================================
  @Nested
  @DisplayName("Red transition: comprehensive signal states")
  class RedTransitionComprehensiveTest {

    @Test
    @DisplayName("YELLOW becomes RED")
    void yellowBecomesRed() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addYellowSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getRedSignals().contains(p));
      assertFalse(r.getYellowSignals().contains(p));
    }

    @Test
    @DisplayName("FYA becomes RED during all-red")
    void fyaBecomesRed() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addFyaSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getRedSignals().contains(p));
      assertFalse(r.getFyaSignals().contains(p));
    }

    @Test
    @DisplayName("OFF is preserved")
    void offPreserved() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addOffSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getOffSignals().contains(p));
    }

    @Test
    @DisplayName("WALK is preserved")
    void walkPreserved() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addWalkSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getWalkSignals().contains(p));
    }

    @Test
    @DisplayName("FLASH_DONT_WALK is preserved")
    void flashDontWalkPreserved() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addFlashDontWalkSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getFlashDontWalkSignals().contains(p));
    }

    @Test
    @DisplayName("DONT_WALK is preserved")
    void dontWalkPreserved() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addDontWalkSignal(p);
      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getDontWalkSignals().contains(p));
    }

    @Test
    @DisplayName("GREEN not in upcoming becomes RED")
    void greenNotInUpcoming_becomesRed() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addGreenSignal(p);

      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addRedSignal(p);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getRedSignals().contains(p));
      assertFalse(r.getGreenSignals().contains(p));
    }

    @Test
    @DisplayName("GREEN in upcoming stays GREEN")
    void greenInUpcoming_staysGreen() {
      BlockPos p = new BlockPos(1, 0, 0);
      TrafficSignalPhase curr = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      curr.addGreenSignal(p);

      TrafficSignalPhase next = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      next.addGreenSignal(p);

      TrafficSignalPhase r = TrafficSignalControllerTickerUtilities
          .getRedTransitionPhaseForUpcoming(curr, next);
      assertTrue(r.getGreenSignals().contains(p));
      assertFalse(r.getRedSignals().contains(p));
    }
  }

  // ========================================================================
  // FDW transition: edge cases
  // ========================================================================
  @Nested
  @DisplayName("FDW transition: edge cases")
  class FdwTransitionEdgeCasesTest {

    @Test
    @DisplayName("FDW preserves GREEN signals from current phase")
    void fdwPreservesGreen() {
      BlockPos greenPos = new BlockPos(10, 64, 20);
      BlockPos walkPos = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(greenPos);
      current.addWalkSignal(walkPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(greenPos);
      upcoming.addDontWalkSignal(walkPos);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(fdw);
      assertTrue(fdw.getGreenSignals().contains(greenPos),
          "Green signal should stay green during FDW");
    }

    @Test
    @DisplayName("FDW preserves FYA signals from current phase")
    void fdwPreservesFya() {
      BlockPos fyaPos = new BlockPos(10, 64, 20);
      BlockPos walkPos = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addFyaSignal(fyaPos);
      current.addWalkSignal(walkPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addRedSignal(fyaPos);
      upcoming.addDontWalkSignal(walkPos);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(fdw);
      assertTrue(fdw.getFyaSignals().contains(fyaPos),
          "FYA signal should stay FYA during FDW");
    }

    @Test
    @DisplayName("FDW preserves RED signals from current phase")
    void fdwPreservesRed() {
      BlockPos redPos = new BlockPos(10, 64, 20);
      BlockPos walkPos = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addRedSignal(redPos);
      current.addWalkSignal(walkPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addDontWalkSignal(walkPos);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(fdw);
      assertTrue(fdw.getRedSignals().contains(redPos),
          "Red signal should stay red during FDW");
    }

    @Test
    @DisplayName("FDW preserves OFF signals from current phase")
    void fdwPreservesOff() {
      BlockPos offPos = new BlockPos(10, 64, 20);
      BlockPos walkPos = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addOffSignal(offPos);
      current.addWalkSignal(walkPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addDontWalkSignal(walkPos);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(fdw);
      assertTrue(fdw.getOffSignals().contains(offPos),
          "Off signal should stay off during FDW");
    }

    @Test
    @DisplayName("FDW with multiple walk signals, some stay WALK some become FDW")
    void fdwMixedWalkSignals() {
      BlockPos walkStays = new BlockPos(10, 64, 20);
      BlockPos walkGoes1 = new BlockPos(20, 64, 30);
      BlockPos walkGoes2 = new BlockPos(30, 64, 40);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addWalkSignal(walkStays);
      current.addWalkSignal(walkGoes1);
      current.addWalkSignal(walkGoes2);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addWalkSignal(walkStays);
      upcoming.addDontWalkSignal(walkGoes1);
      upcoming.addDontWalkSignal(walkGoes2);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);

      assertNotNull(fdw);
      assertTrue(fdw.getWalkSignals().contains(walkStays));
      assertTrue(fdw.getFlashDontWalkSignals().contains(walkGoes1));
      assertTrue(fdw.getFlashDontWalkSignals().contains(walkGoes2));
      assertEquals(1, fdw.getWalkSignals().size());
      assertEquals(2, fdw.getFlashDontWalkSignals().size());
    }
  }

  // ========================================================================
  // allCircuitsHaveSensors
  // ========================================================================
  @Nested
  @DisplayName("allCircuitsHaveSensors")
  class AllCircuitsHaveSensorsTest {

    @Test
    @DisplayName("empty circuits returns false")
    void emptyCircuits_returnsFalse() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

      assertFalse(TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits));
    }

    @Test
    @DisplayName("circuit with no sensors returns false")
    void noSensors_returnsFalse() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      assertFalse(TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits));
    }

    @Test
    @DisplayName("all circuits with sensors returns true")
    void allWithSensors_returnsTrue() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getSensors().add(new BlockPos(10, 64, 20));

      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(circuit);

      assertTrue(TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits));
    }

    @Test
    @DisplayName("mixed circuits (one with, one without sensors) returns false")
    void mixed_returnsFalse() {
      TrafficSignalControllerCircuit withSensors = emptyCircuit();
      withSensors.getSensors().add(new BlockPos(10, 64, 20));

      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(withSensors);
      circuits.addCircuit(emptyCircuit());

      assertFalse(TrafficSignalControllerTickerUtilities.allCircuitsHaveSensors(circuits));
    }
  }

  // ========================================================================
  // Overlap: source NOT green, target not moved
  // ========================================================================
  @Nested
  @DisplayName("Overlap application: non-green sources don't fire")
  class OverlapNonGreenSourceTest {

    @Test
    @DisplayName("RED source does not move overlap target to GREEN")
    void redSourceNoOverlap() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignal(source);
      phase.addRedSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getRedSignals().contains(target),
          "Target should remain RED when source is RED");
      assertFalse(result.getGreenSignals().contains(target));
    }

    @Test
    @DisplayName("FYA source does not fire overlap")
    void fyaSourceNoOverlap() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addFyaSignal(source);
      phase.addRedSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getRedSignals().contains(target),
          "Target should remain RED when source is FYA (not GREEN)");
    }

    @Test
    @DisplayName("YELLOW source does not fire overlap")
    void yellowSourceNoOverlap() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addYellowSignal(source);
      phase.addRedSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getRedSignals().contains(target),
          "Target should remain RED when source is YELLOW");
    }

    @Test
    @DisplayName("OFF source does not fire overlap")
    void offSourceNoOverlap() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addOffSignal(source);
      phase.addRedSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getRedSignals().contains(target),
          "Target should remain RED when source is OFF");
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

  // ========================================================================
  // TrafficSignalControllerOverlaps
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalControllerOverlaps")
  class OverlapsTest {

    @Test
    @DisplayName("addOverlap returns true on first add")
    void addOverlap_returnsTrue() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      assertTrue(overlaps.addOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
    }

    @Test
    @DisplayName("addOverlap returns false on duplicate")
    void addOverlap_duplicate_returnsFalse() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
      assertFalse(overlaps.addOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
    }

    @Test
    @DisplayName("getOverlapCount reflects added entries")
    void getOverlapCount() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      assertEquals(0, overlaps.getOverlapCount());
      overlaps.addOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
      assertEquals(1, overlaps.getOverlapCount());
      overlaps.addOverlap(new BlockPos(3, 0, 0), new BlockPos(4, 0, 0));
      assertEquals(2, overlaps.getOverlapCount());
    }

    @Test
    @DisplayName("multiple targets for same source")
    void multipleTargetsSameSource() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      BlockPos source = new BlockPos(1, 0, 0);
      overlaps.addOverlap(source, new BlockPos(2, 0, 0));
      overlaps.addOverlap(source, new BlockPos(3, 0, 0));
      assertEquals(1, overlaps.getOverlapCount());
      List<BlockPos> targets = overlaps.getOverlapsForSource(source);
      assertNotNull(targets);
      assertEquals(2, targets.size());
    }

    @Test
    @DisplayName("getOverlapsForSource returns null for unknown source")
    void getOverlapsForSource_unknown() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      assertNull(overlaps.getOverlapsForSource(new BlockPos(99, 0, 0)));
    }

    @Test
    @DisplayName("removeOverlap removes a specific target")
    void removeOverlap_specifictTarget() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      BlockPos source = new BlockPos(1, 0, 0);
      BlockPos target1 = new BlockPos(2, 0, 0);
      BlockPos target2 = new BlockPos(3, 0, 0);
      overlaps.addOverlap(source, target1);
      overlaps.addOverlap(source, target2);

      assertTrue(overlaps.removeOverlap(source, target1));
      List<BlockPos> remaining = overlaps.getOverlapsForSource(source);
      assertNotNull(remaining);
      assertEquals(1, remaining.size());
      assertTrue(remaining.contains(target2));
    }

    @Test
    @DisplayName("removeOverlap returns false for nonexistent")
    void removeOverlap_nonexistent() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      assertFalse(overlaps.removeOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
    }

    @Test
    @DisplayName("removeOverlap cleans up source key when last target removed")
    void removeOverlap_cleansUpSourceKey() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      BlockPos source = new BlockPos(1, 0, 0);
      BlockPos target = new BlockPos(2, 0, 0);
      overlaps.addOverlap(source, target);
      overlaps.removeOverlap(source, target);
      assertEquals(0, overlaps.getOverlapCount());
      assertNull(overlaps.getOverlapsForSource(source));
    }

    @Test
    @DisplayName("removeOverlaps removes all targets for a source")
    void removeOverlaps_allTargets() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      BlockPos source = new BlockPos(1, 0, 0);
      overlaps.addOverlap(source, new BlockPos(2, 0, 0));
      overlaps.addOverlap(source, new BlockPos(3, 0, 0));

      assertTrue(overlaps.removeOverlaps(source));
      assertEquals(0, overlaps.getOverlapCount());
    }

    @Test
    @DisplayName("removeOverlaps returns false for unknown source")
    void removeOverlaps_unknown() {
      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      assertFalse(overlaps.removeOverlaps(new BlockPos(99, 0, 0)));
    }

    @Test
    @DisplayName("NBT round-trip preserves data")
    void nbtRoundTrip() {
      TrafficSignalControllerOverlaps original = new TrafficSignalControllerOverlaps();
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target1 = new BlockPos(11, 64, 21);
      BlockPos target2 = new BlockPos(12, 64, 22);
      original.addOverlap(source, target1);
      original.addOverlap(source, target2);

      NBTTagCompound nbt = original.toNBT();
      TrafficSignalControllerOverlaps restored = TrafficSignalControllerOverlaps.fromNBT(nbt);

      assertEquals(original, restored);
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
      TrafficSignalControllerOverlaps a = new TrafficSignalControllerOverlaps();
      TrafficSignalControllerOverlaps b = new TrafficSignalControllerOverlaps();
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());

      a.addOverlap(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
      assertNotEquals(a, b);
    }
  }

  // ========================================================================
  // TrafficSignalSensorSummary computed totals
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalSensorSummary")
  class SensorSummaryTest {

    @Test
    @DisplayName("all-zero constructor produces zero totals")
    void allZero() {
      TrafficSignalSensorSummary s = new TrafficSignalSensorSummary(
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0);
      assertEquals(0, s.getTotalAll());
      assertEquals(0, s.getNonProtectedTotalAll());
    }

    @Test
    @DisplayName("totalAll sums standard + left + protected + right totals")
    void totalAll() {
      TrafficSignalSensorSummary s = new TrafficSignalSensorSummary(
          10, 0, 0, 0, 0,
          5, 0, 0, 0, 0,
          3, 0, 0, 0, 0,
          2, 0, 0, 0, 0);
      assertEquals(20, s.getTotalAll());
    }

    @Test
    @DisplayName("nonProtectedTotalAll excludes protected and right")
    void nonProtectedTotalAll() {
      TrafficSignalSensorSummary s = new TrafficSignalSensorSummary(
          10, 0, 0, 0, 0,
          5, 0, 0, 0, 0,
          3, 0, 0, 0, 0,
          2, 0, 0, 0, 0);
      assertEquals(15, s.getNonProtectedTotalAll());
    }

    @Test
    @DisplayName("directional totals sum all movement types per direction")
    void directionalTotals() {
      TrafficSignalSensorSummary s = new TrafficSignalSensorSummary(
          0, 2, 3, 4, 5,
          0, 1, 1, 1, 1,
          0, 10, 20, 30, 40,
          0, 100, 200, 300, 400);
      assertEquals(113, s.getTotalEast());
      assertEquals(224, s.getTotalWest());
      assertEquals(335, s.getTotalNorth());
      assertEquals(446, s.getTotalSouth());
    }

    @Test
    @DisplayName("nonProtected directional totals exclude protected and right")
    void nonProtectedDirectional() {
      TrafficSignalSensorSummary s = new TrafficSignalSensorSummary(
          0, 2, 3, 4, 5,
          0, 1, 1, 1, 1,
          0, 10, 20, 30, 40,
          0, 100, 200, 300, 400);
      assertEquals(3, s.getNonProtectedTotalEast());
      assertEquals(4, s.getNonProtectedTotalWest());
      assertEquals(5, s.getNonProtectedTotalNorth());
      assertEquals(6, s.getNonProtectedTotalSouth());
    }

    @Test
    @DisplayName("individual getters return constructor values")
    void individualGetters() {
      TrafficSignalSensorSummary s = new TrafficSignalSensorSummary(
          100, 10, 20, 30, 40,
          50, 5, 10, 15, 20,
          25, 3, 6, 9, 7,
          12, 1, 2, 4, 5);

      assertEquals(100, s.getStandardTotal());
      assertEquals(10, s.getStandardEast());
      assertEquals(20, s.getStandardWest());
      assertEquals(30, s.getStandardNorth());
      assertEquals(40, s.getStandardSouth());

      assertEquals(50, s.getLeftTotal());
      assertEquals(5, s.getLeftEast());
      assertEquals(10, s.getLeftWest());
      assertEquals(15, s.getLeftNorth());
      assertEquals(20, s.getLeftSouth());

      assertEquals(25, s.getProtectedTotal());
      assertEquals(3, s.getProtectedEast());
      assertEquals(6, s.getProtectedWest());
      assertEquals(9, s.getProtectedNorth());
      assertEquals(7, s.getProtectedSouth());

      assertEquals(12, s.getRightTotal());
      assertEquals(1, s.getRightEast());
      assertEquals(2, s.getRightWest());
      assertEquals(4, s.getRightNorth());
      assertEquals(5, s.getRightSouth());
    }
  }

  // ========================================================================
  // TrafficSignalPhase: moveOverlapSignalToYellow
  // ========================================================================
  @Nested
  @DisplayName("moveOverlapSignalToYellow")
  class MoveOverlapToYellowTest {

    @Test
    @DisplayName("GREEN moved to YELLOW")
    void greenToYellow() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(pos);

      assertTrue(phase.moveOverlapSignalToYellow(pos));
      assertTrue(phase.getYellowSignals().contains(pos));
      assertFalse(phase.getGreenSignals().contains(pos));
    }

    @Test
    @DisplayName("RED moved to YELLOW")
    void redToYellow() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignal(pos);

      assertTrue(phase.moveOverlapSignalToYellow(pos));
      assertTrue(phase.getYellowSignals().contains(pos));
      assertFalse(phase.getRedSignals().contains(pos));
    }

    @Test
    @DisplayName("FYA moved to YELLOW")
    void fyaToYellow() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addFyaSignal(pos);

      assertTrue(phase.moveOverlapSignalToYellow(pos));
      assertTrue(phase.getYellowSignals().contains(pos));
      assertFalse(phase.getFyaSignals().contains(pos));
    }

    @Test
    @DisplayName("OFF moved to YELLOW")
    void offToYellow() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addOffSignal(pos);

      assertTrue(phase.moveOverlapSignalToYellow(pos));
      assertTrue(phase.getYellowSignals().contains(pos));
      assertFalse(phase.getOffSignals().contains(pos));
    }

    @Test
    @DisplayName("already YELLOW returns false")
    void alreadyYellow() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addYellowSignal(pos);

      assertFalse(phase.moveOverlapSignalToYellow(pos));
      assertTrue(phase.getYellowSignals().contains(pos));
    }
  }

  // ========================================================================
  // TrafficSignalPhase: moveOverlapSignalToRed
  // ========================================================================
  @Nested
  @DisplayName("moveOverlapSignalToRed")
  class MoveOverlapToRedTest {

    @Test
    @DisplayName("GREEN moved to RED")
    void greenToRed() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(pos);

      assertTrue(phase.moveOverlapSignalToRed(pos));
      assertTrue(phase.getRedSignals().contains(pos));
      assertFalse(phase.getGreenSignals().contains(pos));
    }

    @Test
    @DisplayName("YELLOW moved to RED")
    void yellowToRed() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addYellowSignal(pos);

      assertTrue(phase.moveOverlapSignalToRed(pos));
      assertTrue(phase.getRedSignals().contains(pos));
      assertFalse(phase.getYellowSignals().contains(pos));
    }

    @Test
    @DisplayName("FYA moved to RED")
    void fyaToRed() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addFyaSignal(pos);

      assertTrue(phase.moveOverlapSignalToRed(pos));
      assertTrue(phase.getRedSignals().contains(pos));
      assertFalse(phase.getFyaSignals().contains(pos));
    }

    @Test
    @DisplayName("OFF moved to RED")
    void offToRed() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addOffSignal(pos);

      assertTrue(phase.moveOverlapSignalToRed(pos));
      assertTrue(phase.getRedSignals().contains(pos));
      assertFalse(phase.getOffSignals().contains(pos));
    }

    @Test
    @DisplayName("already RED returns false")
    void alreadyRed() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignal(pos);

      assertFalse(phase.moveOverlapSignalToRed(pos));
      assertTrue(phase.getRedSignals().contains(pos));
    }
  }

  // ========================================================================
  // TrafficSignalPhase: removeSignals
  // ========================================================================
  @Nested
  @DisplayName("removeSignals")
  class RemoveSignalsTest {

    @Test
    @DisplayName("removes from all signal lists")
    void removesFromAllLists() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(pos);
      phase.addRedSignal(pos);
      phase.addYellowSignal(pos);
      phase.addFyaSignal(pos);
      phase.addOffSignal(pos);
      phase.addWalkSignal(pos);
      phase.addDontWalkSignal(pos);
      phase.addFlashDontWalkSignal(pos);

      phase.removeSignals(Collections.singletonList(pos));

      assertFalse(phase.getGreenSignals().contains(pos));
      assertFalse(phase.getRedSignals().contains(pos));
      assertFalse(phase.getYellowSignals().contains(pos));
      assertFalse(phase.getFyaSignals().contains(pos));
      assertFalse(phase.getOffSignals().contains(pos));
      assertFalse(phase.getWalkSignals().contains(pos));
      assertFalse(phase.getDontWalkSignals().contains(pos));
      assertFalse(phase.getFlashDontWalkSignals().contains(pos));
    }

    @Test
    @DisplayName("removes multiple positions at once")
    void removesMultiple() {
      BlockPos a = new BlockPos(1, 0, 0);
      BlockPos b = new BlockPos(2, 0, 0);
      BlockPos c = new BlockPos(3, 0, 0);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(a);
      phase.addRedSignal(b);
      phase.addYellowSignal(c);

      phase.removeSignals(Arrays.asList(a, b));

      assertFalse(phase.getGreenSignals().contains(a));
      assertFalse(phase.getRedSignals().contains(b));
      assertTrue(phase.getYellowSignals().contains(c));
    }

    @Test
    @DisplayName("empty list is a no-op")
    void emptyListNoOp() {
      BlockPos pos = new BlockPos(1, 0, 0);
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(pos);

      phase.removeSignals(Collections.emptyList());

      assertTrue(phase.getGreenSignals().contains(pos));
    }
  }

  // ========================================================================
  // Multi-target overlap application
  // ========================================================================
  @Nested
  @DisplayName("Overlap application: multi-target scenarios")
  class OverlapMultiTargetTest {

    @Test
    @DisplayName("green source with two targets moves both to green")
    void greenSourceMultipleTargets() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target1 = new BlockPos(11, 64, 21);
      BlockPos target2 = new BlockPos(12, 64, 22);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(source);
      phase.addRedSignal(target1);
      phase.addRedSignal(target2);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target1);
      overlaps.addOverlap(source, target2);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getGreenSignals().contains(target1));
      assertTrue(result.getGreenSignals().contains(target2));
      assertFalse(result.getRedSignals().contains(target1));
      assertFalse(result.getRedSignals().contains(target2));
    }

    @Test
    @DisplayName("two different green sources each with one target")
    void twoGreenSources() {
      BlockPos source1 = new BlockPos(10, 64, 20);
      BlockPos source2 = new BlockPos(20, 64, 30);
      BlockPos target1 = new BlockPos(11, 64, 21);
      BlockPos target2 = new BlockPos(21, 64, 31);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(source1);
      phase.addGreenSignal(source2);
      phase.addRedSignal(target1);
      phase.addRedSignal(target2);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source1, target1);
      overlaps.addOverlap(source2, target2);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getGreenSignals().contains(target1));
      assertTrue(result.getGreenSignals().contains(target2));
    }

    @Test
    @DisplayName("mixed: one source green, one red — only green source fires overlap")
    void mixedSourceStates() {
      BlockPos greenSource = new BlockPos(10, 64, 20);
      BlockPos redSource = new BlockPos(20, 64, 30);
      BlockPos target1 = new BlockPos(11, 64, 21);
      BlockPos target2 = new BlockPos(21, 64, 31);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(greenSource);
      phase.addRedSignal(redSource);
      phase.addRedSignal(target1);
      phase.addRedSignal(target2);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(greenSource, target1);
      overlaps.addOverlap(redSource, target2);

      TrafficSignalPhase result =
          TrafficSignalControllerTickerUtilities.getPhaseWithOverlapsApplied(phase, overlaps);

      assertTrue(result.getGreenSignals().contains(target1),
          "Target of green source should be green");
      assertTrue(result.getRedSignals().contains(target2),
          "Target of red source should stay red");
    }
  }

  // ========================================================================
  // Full FDW → yellow → red chain with ped signals
  // ========================================================================
  @Nested
  @DisplayName("Full FDW → yellow → red chain with pedestrian signals")
  class FullFdwYellowRedChainTest {

    @Test
    @DisplayName("complete chain: walk→fdw, green→yellow→red")
    void fullChainWithPeds() {
      BlockPos greenPos = new BlockPos(10, 64, 20);
      BlockPos walkPos = new BlockPos(20, 64, 30);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(greenPos);
      current.addWalkSignal(walkPos);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(2, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      upcoming.addRedSignal(greenPos);
      upcoming.addDontWalkSignal(walkPos);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);
      assertNotNull(fdw);
      assertTrue(fdw.getFlashDontWalkSignals().contains(walkPos));
      assertTrue(fdw.getGreenSignals().contains(greenPos));

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              fdw, upcoming);
      assertTrue(yellow.getYellowSignals().contains(greenPos));
      assertTrue(yellow.getDontWalkSignals().contains(walkPos));

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellow, upcoming);
      assertTrue(red.getRedSignals().contains(greenPos));
      assertTrue(red.getDontWalkSignals().contains(walkPos));
    }

    @Test
    @DisplayName("chain preserves green+walk signals that carry into upcoming phase")
    void chainPreservesCarriedSignals() {
      BlockPos staysGreen = new BlockPos(10, 64, 20);
      BlockPos goesRed = new BlockPos(11, 64, 21);
      BlockPos walkStays = new BlockPos(20, 64, 30);
      BlockPos walkGoes = new BlockPos(21, 64, 31);

      TrafficSignalPhase current = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      current.addGreenSignal(staysGreen);
      current.addGreenSignal(goesRed);
      current.addWalkSignal(walkStays);
      current.addWalkSignal(walkGoes);

      TrafficSignalPhase upcoming = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      upcoming.addGreenSignal(staysGreen);
      upcoming.addRedSignal(goesRed);
      upcoming.addWalkSignal(walkStays);
      upcoming.addDontWalkSignal(walkGoes);

      TrafficSignalPhase fdw =
          TrafficSignalControllerTickerUtilities.getFlashDontWalkTransitionPhaseForUpcoming(
              current, upcoming);
      assertNotNull(fdw);
      assertTrue(fdw.getWalkSignals().contains(walkStays));
      assertTrue(fdw.getFlashDontWalkSignals().contains(walkGoes));
      assertTrue(fdw.getGreenSignals().contains(staysGreen));
      assertTrue(fdw.getGreenSignals().contains(goesRed));

      TrafficSignalPhase yellow =
          TrafficSignalControllerTickerUtilities.getYellowTransitionPhaseForUpcoming(
              fdw, upcoming);
      assertTrue(yellow.getGreenSignals().contains(staysGreen));
      assertTrue(yellow.getYellowSignals().contains(goesRed));

      TrafficSignalPhase red =
          TrafficSignalControllerTickerUtilities.getRedTransitionPhaseForUpcoming(
              yellow, upcoming);
      assertTrue(red.getGreenSignals().contains(staysGreen));
      assertTrue(red.getRedSignals().contains(goesRed));
    }
  }

  // ========================================================================
  // TrafficSignalPhase: bulk add methods
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalPhase bulk add methods")
  class BulkAddTest {

    @Test
    @DisplayName("addGreenSignals adds all positions")
    void addGreenSignals() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignals(Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
      assertEquals(2, phase.getGreenSignals().size());
    }

    @Test
    @DisplayName("addRedSignals adds all positions")
    void addRedSignals() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignals(Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
      assertEquals(2, phase.getRedSignals().size());
    }

    @Test
    @DisplayName("addFyaSignals adds all positions")
    void addFyaSignals() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addFyaSignals(Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
      assertEquals(2, phase.getFyaSignals().size());
    }

    @Test
    @DisplayName("addWalkSignals adds all positions")
    void addWalkSignals() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addWalkSignals(Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
      assertEquals(2, phase.getWalkSignals().size());
    }

    @Test
    @DisplayName("addDontWalkSignals adds all positions")
    void addDontWalkSignals() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addDontWalkSignals(Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
      assertEquals(2, phase.getDontWalkSignals().size());
    }
  }

  // ========================================================================
  // Transition overlap application (yellow/red with overlaps)
  // ========================================================================
  @Nested
  @DisplayName("getTransitionPhaseWithOverlapsApplied: edge cases")
  class TransitionOverlapEdgeCasesTest {

    @Test
    @DisplayName("no overlaps returns same phase (identity)")
    void noOverlaps_identity() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      phase.addYellowSignal(new BlockPos(1, 0, 0));

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
          phase, overlaps);

      assertTrue(phase.getYellowSignals().contains(new BlockPos(1, 0, 0)));
    }

    @Test
    @DisplayName("null overlaps does not throw")
    void nullOverlaps_noThrow() {
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      phase.addYellowSignal(new BlockPos(1, 0, 0));

      assertDoesNotThrow(() ->
          TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
              phase, null));
    }

    @Test
    @DisplayName("overlap target already in correct state is not duplicated")
    void targetAlreadyCorrectState() {
      BlockPos source = new BlockPos(10, 64, 20);
      BlockPos target = new BlockPos(11, 64, 21);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      phase.addYellowSignal(source);
      phase.addYellowSignal(target);

      TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();
      overlaps.addOverlap(source, target);

      TrafficSignalControllerTickerUtilities.getTransitionPhaseWithOverlapsApplied(
          phase, overlaps);

      long yellowCount = phase.getYellowSignals().stream()
          .filter(p -> p.equals(target)).count();
      assertEquals(1, yellowCount, "Target should not be duplicated");
    }
  }

  // ========================================================================
  // TrafficSignalPhaseApplicability enum coverage
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalPhaseApplicability enum")
  class PhaseApplicabilityEnumTest {

    @Test
    @DisplayName("NO_POWER is not a through type or directional phase")
    void noPower() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.NO_POWER));
      assertFalse(TrafficSignalControllerTickerUtilities.isOmnidirectionalThroughType(
          TrafficSignalPhaseApplicability.NO_POWER));
      assertFalse(TrafficSignalControllerTickerUtilities.isDirectionalPhase(
          TrafficSignalPhaseApplicability.NO_POWER));
    }

    @Test
    @DisplayName("FLASH_DONT_WALK_TRANSITIONING is not a through type")
    void fdwTransitioning() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.FLASH_DONT_WALK_TRANSITIONING));
    }

    @Test
    @DisplayName("YELLOW_TRANSITIONING is not a through type")
    void yellowTransitioning() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING));
    }

    @Test
    @DisplayName("RED_TRANSITIONING is not a through type")
    void redTransitioning() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.RED_TRANSITIONING));
    }

    @Test
    @DisplayName("LEAD_PEDESTRIAN_INTERVAL is not a through type")
    void lpi() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.LEAD_PEDESTRIAN_INTERVAL));
    }
  }
}
