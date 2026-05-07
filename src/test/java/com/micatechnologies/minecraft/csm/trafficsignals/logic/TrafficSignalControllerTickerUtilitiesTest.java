package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
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

    @Test
    @DisplayName("Facings variants: out-of-range circuit returns empty set")
    void facingsVariants_outOfRange() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      assertTrue(TrafficSignalControllerTickerUtilities.computeGreenLeftTurnFacings(
          circuits, -1, true, null).isEmpty());
      assertTrue(TrafficSignalControllerTickerUtilities.computeGreenRightTurnFacings(
          circuits, 0, true, null).isEmpty());
      assertTrue(TrafficSignalControllerTickerUtilities.computeGreenLeftTurnFacings(
          circuits, 5, true, null).isEmpty());
      assertTrue(TrafficSignalControllerTickerUtilities.computeGreenRightTurnFacings(
          circuits, 5, true, null).isEmpty());
    }

    @Test
    @DisplayName("Facings variants: overlap=false returns empty set (no arbitration applies)")
    void facingsVariants_overlapDisabled() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(emptyCircuit());

      assertTrue(TrafficSignalControllerTickerUtilities.computeGreenLeftTurnFacings(
          circuits, 1, false, null).isEmpty());
      assertTrue(TrafficSignalControllerTickerUtilities.computeGreenRightTurnFacings(
          circuits, 1, false, null).isEmpty());
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

    @Test
    @DisplayName("YELLOW→YELLOW is safe (mid-clearance, holding yellow)")
    void yellowToYellow_isSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      a.addYellowSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      b.addYellowSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("YELLOW→RED is safe (natural clearance progression)")
    void yellowToRed_isSafe() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      a.addYellowSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.RED_TRANSITIONING);
      b.addRedSignal(pos);

      assertFalse(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("YELLOW→GREEN is a conflict (skips all-red clearance)")
    void yellowToGreen_isConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      a.addYellowSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addGreenSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("YELLOW→FYA is a conflict (skips all-red clearance)")
    void yellowToFya_isConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      a.addYellowSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addFyaSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
    }

    @Test
    @DisplayName("YELLOW→OFF is a conflict (skips all-red, jumps to compound-hybrid dark)")
    void yellowToOff_isConflict() {
      BlockPos pos = new BlockPos(10, 64, 20);

      TrafficSignalPhase a = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.YELLOW_TRANSITIONING);
      a.addYellowSignal(pos);

      TrafficSignalPhase b = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      b.addOffSignal(pos);

      assertTrue(TrafficSignalControllerTickerUtilities.hasVehicleSignalConflict(a, b));
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
  // REGRESSION: FYA-only left signal (no regular left signal on circuit)
  // ========================================================================
  @Nested
  @DisplayName("Regression: FYA-only left signal (permissive-only)")
  class FyaOnlyLeftSignalTest {

    @Test
    @DisplayName("getEffectiveLeftDemand returns 0 when leftSignals is empty")
    void effectiveLeftDemand_noLeftSignals_returnsZero() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(new BlockPos(10, 64, 20));

      TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
          0, 0, 0, 0, 0,
          5, 5, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0);

      assertEquals(0,
          TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
              (net.minecraft.world.World) null, circuit, summary),
          "Should return 0 when circuit has no regular left signals");
    }

    @Test
    @DisplayName("getEffectiveLeftDemand returns nonzero when leftSignals exists")
    void effectiveLeftDemand_withLeftSignals_returnsNonzero() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(new BlockPos(10, 64, 20));
      circuit.getLeftSignals().add(new BlockPos(11, 64, 21));

      TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
          0, 0, 0, 0, 0,
          3, 3, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0);

      assertTrue(
          TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
              (net.minecraft.world.World) null, circuit, summary) > 0,
          "Should return nonzero when circuit has regular left signals and demand >= 2");
    }

    @Test
    @DisplayName("getEffectiveLeftDemand still returns 0 when no sensor demand")
    void effectiveLeftDemand_noDemand_returnsZero() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(new BlockPos(11, 64, 21));

      TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0);

      assertEquals(0,
          TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
              (net.minecraft.world.World) null, circuit, summary));
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

  // ========================================================================
  // Regression: ALL_THROUGHS_PROTECTEDS protected-vs-right conflict
  // ========================================================================
  @Nested
  @DisplayName("applyAllThroughsProtectedsSignalStates: per-direction protected-vs-right")
  class ApplyAllThroughsProtectedsSignalStatesTest {

    private static final BlockPos LEFT = new BlockPos(1, 0, 0);
    private static final BlockPos FLASHING_LEFT = new BlockPos(2, 0, 0);
    private static final BlockPos RIGHT = new BlockPos(3, 0, 0);
    private static final BlockPos FLASHING_RIGHT = new BlockPos(4, 0, 0);
    private static final BlockPos THROUGH = new BlockPos(5, 0, 0);
    private static final BlockPos PROTECTED = new BlockPos(6, 0, 0);

    private TrafficSignalControllerCircuit circuitWithThrough() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getThroughSignals().add(THROUGH);
      return circuit;
    }

    private TrafficSignalPhase newPhase() {
      return new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);
    }

    private Tuple<List<BlockPos>, List<BlockPos>> matching(BlockPos pos) {
      return new Tuple<>(new java.util.ArrayList<>(Collections.singletonList(pos)),
          new java.util.ArrayList<>());
    }

    private Tuple<List<BlockPos>, List<BlockPos>> nonMatching(BlockPos pos) {
      return new Tuple<>(new java.util.ArrayList<>(),
          new java.util.ArrayList<>(Collections.singletonList(pos)));
    }

    private Tuple<List<BlockPos>, List<BlockPos>> emptyTuple() {
      return new Tuple<>(new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    @Test
    @DisplayName("right at protected-green facing forces same-direction protected to RED")
    void greenRight_protectedRed() {
      TrafficSignalControllerCircuit circuit = circuitWithThrough();
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          /* flashingLeft */ matching(FLASHING_LEFT),
          /* left */ matching(LEFT),
          /* flashingRight */ matching(FLASHING_RIGHT),
          /* right */ matching(RIGHT),
          /* protected */ matching(PROTECTED));

      assertTrue(phase.getRedSignals().contains(PROTECTED),
          "Protected signal at solid-right facing should go RED");
      assertFalse(phase.getGreenSignals().contains(PROTECTED),
          "Protected must not be GREEN concurrent with solid green right at same facing");
      assertTrue(phase.getGreenSignals().contains(RIGHT));
    }

    @Test
    @DisplayName("FYA-permissive right (non-matching) keeps protected GREEN")
    void fyaRight_protectedGreen() {
      TrafficSignalControllerCircuit circuit = circuitWithThrough();
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          /* flashingLeft */ matching(FLASHING_LEFT),
          /* left */ matching(LEFT),
          /* flashingRight */ nonMatching(FLASHING_RIGHT),
          /* right */ nonMatching(RIGHT),
          /* protected */ nonMatching(PROTECTED));

      assertTrue(phase.getGreenSignals().contains(PROTECTED),
          "Protected at non-matching facing should be GREEN (no conflict with FYA right)");
      assertTrue(phase.getFyaSignals().contains(FLASHING_RIGHT));
      assertTrue(phase.getRedSignals().contains(RIGHT));
    }

    @Test
    @DisplayName("matching-facing left = solid green, FYA-left companion = OFF")
    void greenLeft_leftProtectedGreen() {
      TrafficSignalControllerCircuit circuit = circuitWithThrough();
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          /* flashingLeft */ matching(FLASHING_LEFT),
          /* left */ matching(LEFT),
          /* flashingRight */ nonMatching(FLASHING_RIGHT),
          /* right */ nonMatching(RIGHT),
          /* protected */ nonMatching(PROTECTED));

      assertTrue(phase.getGreenSignals().contains(LEFT));
      assertTrue(phase.getOffSignals().contains(FLASHING_LEFT));
    }

    @Test
    @DisplayName("non-matching left = FYA permissive, leftSignals = RED")
    void fyaLeft_leftFyaPermissive() {
      TrafficSignalControllerCircuit circuit = circuitWithThrough();
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          /* flashingLeft */ nonMatching(FLASHING_LEFT),
          /* left */ nonMatching(LEFT),
          /* flashingRight */ nonMatching(FLASHING_RIGHT),
          /* right */ nonMatching(RIGHT),
          /* protected */ nonMatching(PROTECTED));

      assertTrue(phase.getFyaSignals().contains(FLASHING_LEFT));
      assertTrue(phase.getRedSignals().contains(LEFT));
    }

    @Test
    @DisplayName("through signal is GREEN regardless of turn arbitration")
    void through_alwaysGreen() {
      TrafficSignalControllerCircuit circuit = circuitWithThrough();

      // Iterate every left/right combination of matching vs non-matching
      Tuple<List<BlockPos>, List<BlockPos>>[][] cases = new Tuple[][] {
          { matching(FLASHING_LEFT), matching(LEFT) },
          { nonMatching(FLASHING_LEFT), nonMatching(LEFT) }
      };
      Tuple<List<BlockPos>, List<BlockPos>>[][] rightCases = new Tuple[][] {
          { matching(FLASHING_RIGHT), matching(RIGHT), matching(PROTECTED) },
          { nonMatching(FLASHING_RIGHT), nonMatching(RIGHT), nonMatching(PROTECTED) }
      };
      for (Tuple<List<BlockPos>, List<BlockPos>>[] leftPair : cases) {
        for (Tuple<List<BlockPos>, List<BlockPos>>[] rightTriple : rightCases) {
          TrafficSignalPhase phase = newPhase();
          TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
              circuit, phase, leftPair[0], leftPair[1],
              rightTriple[0], rightTriple[1], rightTriple[2]);
          assertTrue(phase.getGreenSignals().contains(THROUGH),
              "Through should be GREEN regardless of turn partitioning");
        }
      }
    }

    @Test
    @DisplayName("multi-direction circuit: only matching facings get solid green right")
    void multiDirection_perDirectionRight() {
      // Simulate two directions: NB-right at MATCHING, SB-right at NON-MATCHING
      BlockPos nbRight = new BlockPos(10, 0, 0);
      BlockPos sbRight = new BlockPos(11, 0, 0);
      BlockPos nbFlashRight = new BlockPos(12, 0, 0);
      BlockPos sbFlashRight = new BlockPos(13, 0, 0);
      BlockPos nbProtected = new BlockPos(14, 0, 0);
      BlockPos sbProtected = new BlockPos(15, 0, 0);

      TrafficSignalControllerCircuit circuit = circuitWithThrough();
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          emptyTuple(),
          emptyTuple(),
          new Tuple<>(java.util.Arrays.asList(nbFlashRight),
              java.util.Arrays.asList(sbFlashRight)),
          new Tuple<>(java.util.Arrays.asList(nbRight), java.util.Arrays.asList(sbRight)),
          new Tuple<>(java.util.Arrays.asList(nbProtected),
              java.util.Arrays.asList(sbProtected)));

      assertTrue(phase.getGreenSignals().contains(nbRight),
          "NB right (matching) should be solid green");
      assertTrue(phase.getRedSignals().contains(sbRight),
          "SB right (non-matching) should be red companion of FYA");
      assertTrue(phase.getOffSignals().contains(nbFlashRight),
          "NB flashing-right (matching) should be OFF (protected showing)");
      assertTrue(phase.getFyaSignals().contains(sbFlashRight),
          "SB flashing-right (non-matching) should flash yellow permissively");
      assertTrue(phase.getRedSignals().contains(nbProtected),
          "NB protected (matching) should be RED — conflicts with NB solid green right");
      assertTrue(phase.getGreenSignals().contains(sbProtected),
          "SB protected (non-matching) should be GREEN — no conflict with SB FYA right");
    }
  }

  // ========================================================================
  // Regression: directional green phase protected handling
  // ========================================================================
  @Nested
  @DisplayName("applyDirectionalGreenSignalAssignments: matching-direction protected")
  class ApplyDirectionalGreenSignalAssignmentsTest {

    private static final BlockPos MATCHING_LEFT = new BlockPos(1, 0, 0);
    private static final BlockPos OPPOSITE_LEFT = new BlockPos(2, 0, 0);
    private static final BlockPos MATCHING_FLASHING_LEFT = new BlockPos(3, 0, 0);
    private static final BlockPos OPPOSITE_FLASHING_LEFT = new BlockPos(4, 0, 0);
    private static final BlockPos MATCHING_RIGHT = new BlockPos(5, 0, 0);
    private static final BlockPos OPPOSITE_RIGHT = new BlockPos(6, 0, 0);
    private static final BlockPos MATCHING_FLASHING_RIGHT = new BlockPos(7, 0, 0);
    private static final BlockPos OPPOSITE_FLASHING_RIGHT = new BlockPos(8, 0, 0);
    private static final BlockPos MATCHING_THROUGH = new BlockPos(9, 0, 0);
    private static final BlockPos OPPOSITE_THROUGH = new BlockPos(10, 0, 0);
    private static final BlockPos MATCHING_PROTECTED = new BlockPos(11, 0, 0);
    private static final BlockPos OPPOSITE_PROTECTED = new BlockPos(12, 0, 0);

    private Tuple<List<BlockPos>, List<BlockPos>> tuple(BlockPos matching, BlockPos opposite) {
      List<BlockPos> first = matching == null ? new java.util.ArrayList<>()
          : new java.util.ArrayList<>(Collections.singletonList(matching));
      List<BlockPos> second = opposite == null ? new java.util.ArrayList<>()
          : new java.util.ArrayList<>(Collections.singletonList(opposite));
      return new Tuple<>(first, second);
    }

    private Tuple<List<BlockPos>, List<BlockPos>> emptyTuple() {
      return new Tuple<>(new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    @Test
    @DisplayName("matching protected exists: matching right goes FYA, protected stays GREEN")
    void matchingProtected_rightFya_protectedGreen() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          tuple(MATCHING_FLASHING_LEFT, OPPOSITE_FLASHING_LEFT),
          tuple(MATCHING_FLASHING_RIGHT, OPPOSITE_FLASHING_RIGHT),
          tuple(MATCHING_LEFT, OPPOSITE_LEFT),
          tuple(MATCHING_RIGHT, OPPOSITE_RIGHT),
          tuple(MATCHING_THROUGH, OPPOSITE_THROUGH),
          emptyTuple(),
          tuple(MATCHING_PROTECTED, OPPOSITE_PROTECTED));

      assertTrue(phase.getGreenSignals().contains(MATCHING_PROTECTED),
          "Matching-direction protected should be GREEN");
      assertTrue(phase.getRedSignals().contains(OPPOSITE_PROTECTED),
          "Opposite-direction protected should be RED");
      assertTrue(phase.getFyaSignals().contains(MATCHING_FLASHING_RIGHT),
          "Matching-direction right FYA should flash to avoid protected conflict");
      assertTrue(phase.getRedSignals().contains(MATCHING_RIGHT),
          "Matching-direction solid-right should be RED when running FYA permissive");
    }

    @Test
    @DisplayName("no matching protected: matching right stays solid GREEN (regression)")
    void noMatchingProtected_rightSolidGreen() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          tuple(MATCHING_FLASHING_LEFT, OPPOSITE_FLASHING_LEFT),
          tuple(MATCHING_FLASHING_RIGHT, OPPOSITE_FLASHING_RIGHT),
          tuple(MATCHING_LEFT, OPPOSITE_LEFT),
          tuple(MATCHING_RIGHT, OPPOSITE_RIGHT),
          tuple(MATCHING_THROUGH, OPPOSITE_THROUGH),
          emptyTuple(),
          tuple(null, OPPOSITE_PROTECTED));

      assertTrue(phase.getGreenSignals().contains(MATCHING_RIGHT),
          "Without matching protected, matching-direction right is solid GREEN");
      assertTrue(phase.getOffSignals().contains(MATCHING_FLASHING_RIGHT),
          "Without matching protected, matching FYA-right is OFF");
      assertTrue(phase.getRedSignals().contains(OPPOSITE_PROTECTED),
          "Opposite-direction protected is still RED");
    }

    @Test
    @DisplayName("through and left assignments are direction-filtered")
    void throughAndLeft_directionFiltered() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          tuple(MATCHING_FLASHING_LEFT, OPPOSITE_FLASHING_LEFT),
          tuple(MATCHING_FLASHING_RIGHT, OPPOSITE_FLASHING_RIGHT),
          tuple(MATCHING_LEFT, OPPOSITE_LEFT),
          tuple(MATCHING_RIGHT, OPPOSITE_RIGHT),
          tuple(MATCHING_THROUGH, OPPOSITE_THROUGH),
          emptyTuple(),
          emptyTuple());

      assertTrue(phase.getGreenSignals().contains(MATCHING_THROUGH));
      assertTrue(phase.getRedSignals().contains(OPPOSITE_THROUGH));
      assertTrue(phase.getGreenSignals().contains(MATCHING_LEFT));
      assertTrue(phase.getRedSignals().contains(OPPOSITE_LEFT));
      assertTrue(phase.getOffSignals().contains(MATCHING_FLASHING_LEFT));
      assertTrue(phase.getRedSignals().contains(OPPOSITE_FLASHING_LEFT));
    }

    @Test
    @DisplayName("matching FYA-only left (no add-on) becomes FYA permissive")
    void matchingFyaOnlyLeft_becomesFya() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          tuple(MATCHING_FLASHING_LEFT, null),
          emptyTuple(),
          tuple(null, null),
          emptyTuple(),
          emptyTuple(),
          emptyTuple(),
          emptyTuple());

      assertTrue(phase.getFyaSignals().contains(MATCHING_FLASHING_LEFT),
          "Matching FYA-left with no protected add-on should flash yellow");
    }
  }

  // ========================================================================
  // findCircuitSensorFacingMismatch (sensor-facing validator pure helper)
  // ========================================================================
  @Nested
  @DisplayName("findCircuitSensorFacingMismatch")
  class FindCircuitSensorFacingMismatchTest {

    private static final BlockPos SENSOR_A = new BlockPos(1, 0, 0);
    private static final BlockPos SENSOR_B = new BlockPos(2, 0, 0);

    private List<Tuple<BlockPos, net.minecraft.util.EnumFacing>> sensors(
        Tuple<BlockPos, net.minecraft.util.EnumFacing>... entries) {
      return Arrays.asList(entries);
    }

    private Tuple<BlockPos, net.minecraft.util.EnumFacing> sensor(BlockPos pos,
        net.minecraft.util.EnumFacing facing) {
      return new Tuple<>(pos, facing);
    }

    @Test
    @DisplayName("empty signal facings = no constraint, returns null")
    void emptySignalFacings_returnsNull() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1,
          java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.NORTH)));
      assertNull(result, "No signal facings means no constraint to enforce");
    }

    @Test
    @DisplayName("sensor facing matches a signal facing → null (no fault)")
    void matchingFacing_returnsNull() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH,
              net.minecraft.util.EnumFacing.SOUTH),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.NORTH)));
      assertNull(result);
    }

    @Test
    @DisplayName("sensor facing not in signal set → fault message references circuit number")
    void mismatch_returnsMessage() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          3,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH,
              net.minecraft.util.EnumFacing.SOUTH),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.EAST)));
      assertNotNull(result);
      assertTrue(result.contains("circuit 3"), "Message should reference circuit number 3");
      assertTrue(result.contains("east"), "Message should reference sensor's facing direction");
    }

    @Test
    @DisplayName("sensor with null facing is skipped (unloaded chunk / malformed state)")
    void nullSensorFacing_skipped() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH),
          sensors(sensor(SENSOR_A, null)));
      assertNull(result, "Null sensor facing should not trigger a mismatch");
    }

    @Test
    @DisplayName("multiple sensors: first mismatch reported, subsequent ignored")
    void multipleSensors_firstMismatchWins() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          2,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.EAST),
              sensor(SENSOR_B, net.minecraft.util.EnumFacing.WEST)));
      assertNotNull(result);
      assertTrue(result.contains(SENSOR_A.toString()),
          "Should report the first mismatch, sensor A");
    }

    @Test
    @DisplayName("all sensors aligned → null (no fault)")
    void allAligned_returnsNull() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH,
              net.minecraft.util.EnumFacing.EAST),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.NORTH),
              sensor(SENSOR_B, net.minecraft.util.EnumFacing.EAST)));
      assertNull(result);
    }

    @Test
    @DisplayName("null inputs are handled defensively → null (no fault)")
    void nullInputs_returnNull() {
      assertNull(TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1, null, sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.NORTH))));
      assertNull(TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1, java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH), null));
    }

    @Test
    @DisplayName("empty sensor list returns null")
    void emptySensorList_returnsNull() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH),
          sensors());
      assertNull(result, "No sensors means nothing to validate");
    }

    @Test
    @DisplayName("all four cardinal facings present accept any sensor facing")
    void allFourCardinals_accept() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          1,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH,
              net.minecraft.util.EnumFacing.SOUTH,
              net.minecraft.util.EnumFacing.EAST,
              net.minecraft.util.EnumFacing.WEST),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.NORTH),
              sensor(SENSOR_B, net.minecraft.util.EnumFacing.SOUTH)));
      assertNull(result);
    }

    @Test
    @DisplayName("fault message includes 'no matching signal facing' on the same circuit")
    void faultMessage_includesPhrase() {
      String result = TrafficSignalControllerTickerUtilities.findCircuitSensorFacingMismatch(
          7,
          java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH),
          sensors(sensor(SENSOR_A, net.minecraft.util.EnumFacing.SOUTH)));
      assertNotNull(result);
      assertTrue(result.contains("no matching signal facing"),
          "Fault message should explain the mismatch");
    }
  }

  // ========================================================================
  // partitionSignalsByFacingSet (per-direction phase building primitive)
  // ========================================================================
  @Nested
  @DisplayName("partitionSignalsByFacingSet")
  class PartitionSignalsByFacingSetTest {

    private static final BlockPos POS_A = new BlockPos(1, 0, 0);
    private static final BlockPos POS_B = new BlockPos(2, 0, 0);
    private static final BlockPos POS_C = new BlockPos(3, 0, 0);

    @Test
    @DisplayName("empty signal list → both partitions empty")
    void emptySignals_emptyPartitions() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (net.minecraft.world.World) null, java.util.Collections.emptyList(),
              java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH));
      assertNotNull(result);
      assertTrue(result.getFirst().isEmpty());
      assertTrue(result.getSecond().isEmpty());
    }

    @Test
    @DisplayName("null signal list → both partitions empty (defensive)")
    void nullSignals_emptyPartitions() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (net.minecraft.world.World) null, null,
              java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH));
      assertNotNull(result);
      assertTrue(result.getFirst().isEmpty());
      assertTrue(result.getSecond().isEmpty());
    }

    @Test
    @DisplayName("null world → all signals partitioned to non-matching")
    void nullWorld_allNonMatching() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (net.minecraft.world.World) null, Arrays.asList(POS_A, POS_B, POS_C),
              java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH));
      assertTrue(result.getFirst().isEmpty(),
          "Without a world, we can't resolve facings — all go non-matching");
      assertEquals(3, result.getSecond().size());
      assertTrue(result.getSecond().contains(POS_A));
      assertTrue(result.getSecond().contains(POS_B));
      assertTrue(result.getSecond().contains(POS_C));
    }

    @Test
    @DisplayName("null world + empty matching set → all non-matching (no facings to match against)")
    void nullWorld_emptyMatching() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (net.minecraft.world.World) null, Arrays.asList(POS_A, POS_B),
              java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class));
      assertTrue(result.getFirst().isEmpty());
      assertEquals(2, result.getSecond().size());
    }

    @Test
    @DisplayName("partitions are independent ArrayLists, safe to mutate")
    void partitionsAreMutable() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (net.minecraft.world.World) null, Arrays.asList(POS_A),
              java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class));
      // Both lists should be mutable so callers can pass them to phase methods that may
      // append to / iterate over them.
      assertDoesNotThrow(() -> result.getFirst().add(POS_B));
      assertDoesNotThrow(() -> result.getSecond().add(POS_C));
    }

    @Test
    @DisplayName("preserves input order in non-matching partition")
    void preservesOrder() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (net.minecraft.world.World) null, Arrays.asList(POS_C, POS_A, POS_B),
              java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class));
      assertEquals(POS_C, result.getSecond().get(0));
      assertEquals(POS_A, result.getSecond().get(1));
      assertEquals(POS_B, result.getSecond().get(2));
    }
  }

  // ========================================================================
  // Per-direction phase building: extra multi-direction scenarios
  // ========================================================================
  @Nested
  @DisplayName("applyAllThroughsProtectedsSignalStates: multi-direction edge cases")
  class ApplyAllThroughsProtectedsSignalStatesEdgeCasesTest {

    private TrafficSignalControllerCircuit circuitWithThrough(BlockPos throughPos) {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getThroughSignals().add(throughPos);
      return circuit;
    }

    private TrafficSignalPhase newPhase() {
      return new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);
    }

    private Tuple<List<BlockPos>, List<BlockPos>> empty() {
      return new Tuple<>(new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    @Test
    @DisplayName("all-empty partitions: through-only phase (no left/right/protected work)")
    void allEmpty_throughOnly() {
      BlockPos through = new BlockPos(99, 0, 0);
      TrafficSignalControllerCircuit circuit = circuitWithThrough(through);
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase, empty(), empty(), empty(), empty(), empty());

      assertTrue(phase.getGreenSignals().contains(through),
          "Through is GREEN even when no turn signals exist");
      assertTrue(phase.getRedSignals().isEmpty(), "No reds when no left/right/protected");
      assertTrue(phase.getFyaSignals().isEmpty());
      assertTrue(phase.getOffSignals().isEmpty());
    }

    @Test
    @DisplayName("4-direction circuit: 2 directions matching, 2 non-matching")
    void fourDirection_partialMatching() {
      // Setup 4 directions, NB+EB matching (solid green right), SB+WB FYA permissive
      BlockPos through = new BlockPos(99, 0, 0);
      BlockPos nbRight = new BlockPos(1, 0, 0);
      BlockPos ebRight = new BlockPos(2, 0, 0);
      BlockPos sbRight = new BlockPos(3, 0, 0);
      BlockPos wbRight = new BlockPos(4, 0, 0);
      BlockPos nbProt = new BlockPos(11, 0, 0);
      BlockPos ebProt = new BlockPos(12, 0, 0);
      BlockPos sbProt = new BlockPos(13, 0, 0);
      BlockPos wbProt = new BlockPos(14, 0, 0);

      TrafficSignalControllerCircuit circuit = circuitWithThrough(through);
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          empty(), empty(),
          empty(),  // no flashing rights
          new Tuple<>(Arrays.asList(nbRight, ebRight),
              Arrays.asList(sbRight, wbRight)),
          new Tuple<>(Arrays.asList(nbProt, ebProt),
              Arrays.asList(sbProt, wbProt)));

      // Matching direction rights → green; non-matching → red companion
      assertTrue(phase.getGreenSignals().contains(nbRight));
      assertTrue(phase.getGreenSignals().contains(ebRight));
      assertTrue(phase.getRedSignals().contains(sbRight));
      assertTrue(phase.getRedSignals().contains(wbRight));
      // Matching protected → red (conflict with solid green right at same direction)
      assertTrue(phase.getRedSignals().contains(nbProt));
      assertTrue(phase.getRedSignals().contains(ebProt));
      // Non-matching protected → green (no conflict with FYA right)
      assertTrue(phase.getGreenSignals().contains(sbProt));
      assertTrue(phase.getGreenSignals().contains(wbProt));
    }

    @Test
    @DisplayName("multiple signals at same direction all share the same state")
    void multipleSignalsPerDirection() {
      // 3 NB right signals, all matching → all green
      BlockPos through = new BlockPos(99, 0, 0);
      BlockPos r1 = new BlockPos(1, 0, 0);
      BlockPos r2 = new BlockPos(2, 0, 0);
      BlockPos r3 = new BlockPos(3, 0, 0);

      TrafficSignalControllerCircuit circuit = circuitWithThrough(through);
      TrafficSignalPhase phase = newPhase();

      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase,
          empty(), empty(), empty(),
          new Tuple<>(Arrays.asList(r1, r2, r3), java.util.Collections.emptyList()),
          empty());

      assertTrue(phase.getGreenSignals().contains(r1));
      assertTrue(phase.getGreenSignals().contains(r2));
      assertTrue(phase.getGreenSignals().contains(r3));
    }

    @Test
    @DisplayName("pedestrian signals always go DONT_WALK regardless of turn state")
    void pedestrianSignals_alwaysDontWalk() {
      BlockPos pedSignal = new BlockPos(50, 0, 0);
      BlockPos pedAccessory = new BlockPos(51, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getPedestrianSignals().add(pedSignal);
      circuit.getPedestrianAccessorySignals().add(pedAccessory);

      TrafficSignalPhase phase = newPhase();
      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase, empty(), empty(), empty(), empty(), empty());

      assertTrue(phase.getDontWalkSignals().contains(pedSignal));
      assertTrue(phase.getDontWalkSignals().contains(pedAccessory));
      assertFalse(phase.getWalkSignals().contains(pedSignal));
    }

    @Test
    @DisplayName("beacon signals go OFF (not yellow as in all-red phase)")
    void beaconSignals_off() {
      BlockPos beacon = new BlockPos(60, 0, 0);
      BlockPos pedBeacon = new BlockPos(61, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getBeaconSignals().add(beacon);
      circuit.getPedestrianBeaconSignals().add(pedBeacon);

      TrafficSignalPhase phase = newPhase();
      TrafficSignalControllerTickerUtilities.applyAllThroughsProtectedsSignalStates(
          circuit, phase, empty(), empty(), empty(), empty(), empty());

      assertTrue(phase.getOffSignals().contains(beacon),
          "During through-protecteds active phase, beacons go OFF (not flashing)");
      assertTrue(phase.getOffSignals().contains(pedBeacon),
          "Ped beacons also go OFF");
    }
  }

  // ========================================================================
  // Per-direction directional phase: extra multi-direction scenarios
  // ========================================================================
  @Nested
  @DisplayName("applyDirectionalGreenSignalAssignments: edge cases")
  class ApplyDirectionalGreenSignalAssignmentsEdgeCasesTest {

    private Tuple<List<BlockPos>, List<BlockPos>> empty() {
      return new Tuple<>(new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    @Test
    @DisplayName("all-empty partitions: phase has no vehicle assignments at all")
    void allEmpty_emptyPhase() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          empty(), empty(), empty(), empty(), empty(), empty(), empty());

      assertTrue(phase.getGreenSignals().isEmpty());
      assertTrue(phase.getRedSignals().isEmpty());
      assertTrue(phase.getFyaSignals().isEmpty());
      assertTrue(phase.getOffSignals().isEmpty());
    }

    @Test
    @DisplayName("only FYA-only left exists in matching direction → flashes yellow")
    void onlyFyaLeft_flashes() {
      BlockPos fyaLeft = new BlockPos(10, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          new Tuple<>(Arrays.asList(fyaLeft), java.util.Collections.emptyList()),
          empty(),
          empty(),  // no protected-green left arrow
          empty(), empty(), empty(), empty());

      assertTrue(phase.getFyaSignals().contains(fyaLeft),
          "FYA-only left in matching direction permits permissive turn");
      assertFalse(phase.getOffSignals().contains(fyaLeft),
          "FYA-only left without paired left arrow stays as FYA, not OFF");
    }

    @Test
    @DisplayName("matching protected with no matching right → matching protected GREEN, no right action")
    void matchingProtected_noRight_protectedGreen() {
      BlockPos prot = new BlockPos(20, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          empty(), empty(), empty(), empty(), empty(), empty(),
          new Tuple<>(Arrays.asList(prot), java.util.Collections.emptyList()));

      assertTrue(phase.getGreenSignals().contains(prot),
          "Matching-direction protected runs green when no matching right exists");
    }

    @Test
    @DisplayName("non-matching protected always red regardless of right state")
    void nonMatchingProtected_alwaysRed() {
      BlockPos prot = new BlockPos(30, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          empty(), empty(), empty(), empty(), empty(), empty(),
          new Tuple<>(java.util.Collections.emptyList(), Arrays.asList(prot)));

      assertTrue(phase.getRedSignals().contains(prot),
          "Opposite-direction protected always goes red in directional phase");
    }

    @Test
    @DisplayName("multiple positions per partition all share the same state")
    void multiplePositionsPerPartition() {
      BlockPos t1 = new BlockPos(1, 0, 0);
      BlockPos t2 = new BlockPos(2, 0, 0);
      BlockPos t3 = new BlockPos(3, 0, 0);
      BlockPos t4 = new BlockPos(4, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);

      TrafficSignalControllerTickerUtilities.applyDirectionalGreenSignalAssignments(
          circuit, phase,
          empty(), empty(), empty(), empty(),
          new Tuple<>(Arrays.asList(t1, t2), Arrays.asList(t3, t4)),
          empty(), empty());

      assertTrue(phase.getGreenSignals().contains(t1));
      assertTrue(phase.getGreenSignals().contains(t2));
      assertTrue(phase.getRedSignals().contains(t3));
      assertTrue(phase.getRedSignals().contains(t4));
    }
  }

  // ========================================================================
  // buildAllThroughsProtectedsActivePhase integration (null-world fallback)
  // ========================================================================
  @Nested
  @DisplayName("buildAllThroughsProtectedsActivePhase integration")
  class BuildAllThroughsProtectedsActivePhaseIntegrationTest {

    @Test
    @DisplayName("null world: blankouts stay on walk list (visible) since world lookups skip")
    void nullWorld_blankoutsVisible() {
      BlockPos blankout1 = new BlockPos(100, 0, 0);
      BlockPos blankout2 = new BlockPos(101, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getThroughSignals().add(new BlockPos(1, 0, 0));
      circuit.getNoTurnBlankoutSignals().add(blankout1);
      circuit.getNoTurnBlankoutSignals().add(blankout2);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);

      TrafficSignalControllerTickerUtilities.buildAllThroughsProtectedsActivePhase(
          /* world */ null, circuit, phase,
          /* greenLeftFacings */ java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class),
          /* greenRightFacings */ java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class));

      // Without a world, the blankout TE/facing lookups are skipped, so every blankout
      // ends up on the walk list as a defensive default (sign visible) — better than
      // hiding signs based on incomplete information.
      assertTrue(phase.getWalkSignals().contains(blankout1));
      assertTrue(phase.getWalkSignals().contains(blankout2));
    }

    @Test
    @DisplayName("null world + empty facings: all left/right/protected go to FYA / red / green")
    void nullWorld_emptyFacings_fyaPath() {
      BlockPos flashLeft = new BlockPos(1, 0, 0);
      BlockPos left = new BlockPos(2, 0, 0);
      BlockPos flashRight = new BlockPos(3, 0, 0);
      BlockPos right = new BlockPos(4, 0, 0);
      BlockPos prot = new BlockPos(5, 0, 0);
      BlockPos through = new BlockPos(6, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeft);
      circuit.getLeftSignals().add(left);
      circuit.getFlashingRightSignals().add(flashRight);
      circuit.getRightSignals().add(right);
      circuit.getProtectedSignals().add(prot);
      circuit.getThroughSignals().add(through);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);

      TrafficSignalControllerTickerUtilities.buildAllThroughsProtectedsActivePhase(
          null, circuit, phase,
          java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class),
          java.util.EnumSet.noneOf(net.minecraft.util.EnumFacing.class));

      // All signals go to non-matching partition → FYA permissive path
      assertTrue(phase.getFyaSignals().contains(flashLeft));
      assertTrue(phase.getRedSignals().contains(left));
      assertTrue(phase.getFyaSignals().contains(flashRight));
      assertTrue(phase.getRedSignals().contains(right));
      assertTrue(phase.getGreenSignals().contains(prot),
          "Protected stays GREEN when right is FYA permissive");
      assertTrue(phase.getGreenSignals().contains(through));
    }
  }

  // ========================================================================
  // TrafficSignalControllerCircuits + TrafficSignalControllerCircuit interactions
  // ========================================================================
  @Nested
  @DisplayName("Circuit + Circuits interactions")
  class CircuitInteractionsTest {

    @Test
    @DisplayName("a single position appearing in multiple signal lists is queried by both")
    void positionInMultipleLists() {
      BlockPos shared = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(shared);
      circuit.getRightSignals().add(shared);

      assertTrue(circuit.getLeftSignals().contains(shared));
      assertTrue(circuit.getRightSignals().contains(shared));
      assertTrue(circuit.isDeviceLinked(shared));
    }

    @Test
    @DisplayName("getSize counts each list independently (duplicate positions count twice)")
    void getSize_doubleCountsDuplicates() {
      BlockPos shared = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(shared);
      circuit.getRightSignals().add(shared);

      assertEquals(2, circuit.getSize(),
          "getSize sums all lists; the same position can be linked twice");
    }

    @Test
    @DisplayName("unlinkDevice removes a position from every list it appears in")
    void unlinkDevice_removesFromAllLists() {
      BlockPos shared = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(shared);
      circuit.getRightSignals().add(shared);
      circuit.getThroughSignals().add(shared);

      assertTrue(circuit.unlinkDevice(shared));

      assertFalse(circuit.getLeftSignals().contains(shared));
      assertFalse(circuit.getRightSignals().contains(shared));
      assertFalse(circuit.getThroughSignals().contains(shared));
    }

    @Test
    @DisplayName("forAllSignals visits each list, including duplicates")
    void forAllSignals_visitsDuplicates() {
      BlockPos shared = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(shared);
      circuit.getRightSignals().add(shared);

      java.util.concurrent.atomic.AtomicInteger count =
          new java.util.concurrent.atomic.AtomicInteger();
      circuit.forAllSignals(pos -> count.incrementAndGet());

      assertEquals(2, count.get(),
          "forAllSignals visits each list even when positions repeat");
    }

    @Test
    @DisplayName("empty circuit has size 0 and isDeviceLinked returns false")
    void emptyCircuit_invariants() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      assertEquals(0, circuit.getSize());
      assertFalse(circuit.isDeviceLinked(new BlockPos(0, 0, 0)));
    }

    @Test
    @DisplayName("noTurnBlankoutSignals participates in linking and lookups")
    void noTurnBlankoutSignals_linking() {
      BlockPos blankout = new BlockPos(50, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      assertTrue(circuit.linkNoTurnBlankoutSignal(blankout));
      assertTrue(circuit.isDeviceLinked(blankout));
      assertEquals(1, circuit.getSize());

      assertTrue(circuit.unlinkDevice(blankout));
      assertFalse(circuit.isDeviceLinked(blankout));
      assertEquals(0, circuit.getSize());
    }
  }

  // ========================================================================
  // getEffectiveLeftDemand / getEffectiveRightDemand: phase-priority demand
  // ========================================================================
  @Nested
  @DisplayName("getEffectiveLeftDemand / getEffectiveRightDemand")
  class GetEffectiveTurnDemandTest {

    private TrafficSignalSensorSummary summary(int leftE, int leftW, int leftN, int leftS,
        int rightE, int rightW, int rightN, int rightS) {
      int leftTotal = leftE + leftW + leftN + leftS;
      int rightTotal = rightE + rightW + rightN + rightS;
      return new TrafficSignalSensorSummary(
          /* standard total/E/W/N/S */ 0, 0, 0, 0, 0,
          leftTotal, leftE, leftW, leftN, leftS,
          /* protected */ 0, 0, 0, 0, 0,
          rightTotal, rightE, rightW, rightN, rightS);
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: leftTotal=0 → 0 (early-out)")
    void leftDemand_zeroTotal() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(new BlockPos(1, 0, 0));
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (net.minecraft.world.World) null, circuit, summary(0, 0, 0, 0, 0, 0, 0, 0));
      assertEquals(0, demand);
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: no leftSignals → 0 (no green arrow to display)")
    void leftDemand_noLeftSignals() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      // Circuit has no leftSignals → ALL_LEFTS can't be served
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (net.minecraft.world.World) null, circuit, summary(5, 5, 5, 5, 0, 0, 0, 0));
      assertEquals(0, demand);
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: null world + no FYA → directional sum unchanged")
    void leftDemand_nullWorld_noFya() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(new BlockPos(1, 0, 0));
      // No flashingLeft signals → no FYA-direction lookups → no adjustment
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (net.minecraft.world.World) null, circuit, summary(2, 1, 0, 3, 0, 0, 0, 0));
      assertEquals(6, demand, "Sum of directional left counts when no FYA adjustment");
    }

    @Test
    @DisplayName("getEffectiveRightDemand: rightTotal=0 → 0 (early-out)")
    void rightDemand_zeroTotal() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveRightDemand(
          (net.minecraft.world.World) null, circuit, summary(0, 0, 0, 0, 0, 0, 0, 0));
      assertEquals(0, demand);
    }

    @Test
    @DisplayName("getEffectiveRightDemand: null world + no FYA → directional sum unchanged")
    void rightDemand_nullWorld_noFya() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      // No flashingRight signals → no FYA-direction lookups → no adjustment
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveRightDemand(
          (net.minecraft.world.World) null, circuit, summary(0, 0, 0, 0, 4, 0, 2, 0));
      assertEquals(6, demand);
    }
  }

  // ========================================================================
  // collectFacingsFromSignals (helper for non-overlap fallback paths)
  // ========================================================================
  // collectFacingsFromSignals is private; we exercise it indirectly through the
  // buildAllThroughsProtectedsActivePhase null-world path above (returns empty set
  // since null world produces no facings, and the partitioning logic falls back to
  // "all signals non-matching" which feeds through to the per-direction state assigner).

  // ========================================================================
  // validateSensorFacings (function-based overload, Map-backed lookups)
  // ========================================================================
  @Nested
  @DisplayName("validateSensorFacings (function-based)")
  class ValidateSensorFacingsFunctionBasedTest {

    private static final BlockPos SIG_NB = new BlockPos(10, 0, 0);
    private static final BlockPos SIG_SB = new BlockPos(11, 0, 0);
    private static final BlockPos SENSOR_NB = new BlockPos(20, 0, 0);
    private static final BlockPos SENSOR_BAD = new BlockPos(21, 0, 0);

    private TrafficSignalControllerCircuits circuits(TrafficSignalControllerCircuit... cs) {
      TrafficSignalControllerCircuits result = new TrafficSignalControllerCircuits();
      for (TrafficSignalControllerCircuit c : cs) {
        result.addCircuit(c);
      }
      return result;
    }

    @Test
    @DisplayName("aligned single-direction circuit → no fault")
    void singleDirection_aligned_noFault() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(SIG_NB, net.minecraft.util.EnumFacing.NORTH);
      facings.put(SENSOR_NB, net.minecraft.util.EnumFacing.NORTH);

      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getSensors().add(SENSOR_NB);

      String result = TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c));

      assertNull(result, "Sensor facing matches signal facing — no fault");
    }

    @Test
    @DisplayName("misaligned sensor → fault message references the sensor and circuit")
    void misalignedSensor_returnsFault() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(SIG_NB, net.minecraft.util.EnumFacing.NORTH);
      facings.put(SENSOR_BAD, net.minecraft.util.EnumFacing.EAST);

      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getSensors().add(SENSOR_BAD);

      String result = TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c));

      assertNotNull(result);
      assertTrue(result.contains("circuit 1"));
      assertTrue(result.contains(SENSOR_BAD.toString()));
      assertTrue(result.contains("east"));
    }

    @Test
    @DisplayName("multi-direction circuit aligned → no fault")
    void multiDirection_aligned_noFault() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(SIG_NB, net.minecraft.util.EnumFacing.NORTH);
      facings.put(SIG_SB, net.minecraft.util.EnumFacing.SOUTH);
      BlockPos sensorN = new BlockPos(30, 0, 0);
      BlockPos sensorS = new BlockPos(31, 0, 0);
      facings.put(sensorN, net.minecraft.util.EnumFacing.NORTH);
      facings.put(sensorS, net.minecraft.util.EnumFacing.SOUTH);

      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getThroughSignals().add(SIG_SB);
      c.getSensors().add(sensorN);
      c.getSensors().add(sensorS);

      String result = TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c));

      assertNull(result);
    }

    @Test
    @DisplayName("multi-circuit: only the misaligned circuit reports a fault")
    void multiCircuit_isolatesFault() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      // Circuit 1 aligned
      BlockPos sigC1 = new BlockPos(40, 0, 0);
      BlockPos sensorC1 = new BlockPos(41, 0, 0);
      facings.put(sigC1, net.minecraft.util.EnumFacing.NORTH);
      facings.put(sensorC1, net.minecraft.util.EnumFacing.NORTH);

      // Circuit 2 misaligned
      BlockPos sigC2 = new BlockPos(50, 0, 0);
      BlockPos sensorC2 = new BlockPos(51, 0, 0);
      facings.put(sigC2, net.minecraft.util.EnumFacing.EAST);
      facings.put(sensorC2, net.minecraft.util.EnumFacing.WEST);

      TrafficSignalControllerCircuit c1 = emptyCircuit();
      c1.getThroughSignals().add(sigC1);
      c1.getSensors().add(sensorC1);
      TrafficSignalControllerCircuit c2 = emptyCircuit();
      c2.getThroughSignals().add(sigC2);
      c2.getSensors().add(sensorC2);

      String result = TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c1, c2));

      assertNotNull(result);
      assertTrue(result.contains("circuit 2"), "Should pinpoint the offending circuit");
      assertFalse(result.contains("circuit 1"));
    }

    @Test
    @DisplayName("circuit with no sensors → no fault (nothing to validate)")
    void noSensors_skipped() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(SIG_NB, net.minecraft.util.EnumFacing.NORTH);

      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      // no sensors

      assertNull(TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c)));
    }

    @Test
    @DisplayName("circuit with sensors but no signal facings (resolver returns null) → no fault")
    void allFacingsNull_skipped() {
      // Resolver returns null for everything (e.g. signals removed but sensors still linked)
      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getSensors().add(SENSOR_NB);

      assertNull(TrafficSignalControllerTickerUtilities.validateSensorFacings(
          pos -> null, circuits(c)),
          "When no signal has a resolvable facing, there's no constraint to validate against");
    }

    @Test
    @DisplayName("sensor with null facing is skipped, others still validated")
    void sensorNullFacing_skipped() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(SIG_NB, net.minecraft.util.EnumFacing.NORTH);
      facings.put(SENSOR_NB, net.minecraft.util.EnumFacing.NORTH);
      // SENSOR_BAD intentionally not in map → resolver returns null
      BlockPos sensorUnloaded = SENSOR_BAD;

      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getSensors().add(sensorUnloaded);  // first — null facing, skipped
      c.getSensors().add(SENSOR_NB);       // second — aligned

      String result = TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c));

      assertNull(result, "Null-facing sensor is skipped (unloaded chunk); other sensors validate");
    }

    @Test
    @DisplayName("flashing-left signal facing counts toward the circuit's allowed set")
    void flashingLeftFacing_counts() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      BlockPos flashLeft = new BlockPos(60, 0, 0);
      facings.put(flashLeft, net.minecraft.util.EnumFacing.NORTH);
      facings.put(SENSOR_NB, net.minecraft.util.EnumFacing.NORTH);

      TrafficSignalControllerCircuit c = emptyCircuit();
      // Only a flashing-left signal — no through. Sensor still validates against its facing.
      c.getFlashingLeftSignals().add(flashLeft);
      c.getSensors().add(SENSOR_NB);

      assertNull(TrafficSignalControllerTickerUtilities.validateSensorFacings(
          facings::get, circuits(c)));
    }

    @Test
    @DisplayName("null facingResolver → null result (defensive)")
    void nullResolver_nullResult() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getSensors().add(SENSOR_NB);
      assertNull(TrafficSignalControllerTickerUtilities.validateSensorFacings(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) null, circuits(c)));
    }

    @Test
    @DisplayName("null circuits → null result (defensive)")
    void nullCircuits_nullResult() {
      assertNull(TrafficSignalControllerTickerUtilities.validateSensorFacings(
          pos -> net.minecraft.util.EnumFacing.NORTH, null));
    }

    @Test
    @DisplayName("World overload with null world → null result (delegates safely)")
    void worldOverload_nullWorld() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      c.getThroughSignals().add(SIG_NB);
      c.getSensors().add(SENSOR_NB);
      assertNull(TrafficSignalControllerTickerUtilities.validateSensorFacings(
          (net.minecraft.world.World) null, circuits(c)));
    }
  }

  // ========================================================================
  // getEffective{Left,Right}Demand: function-based overloads (FYA adjustment)
  // ========================================================================
  @Nested
  @DisplayName("getEffective{Left,Right}Demand (function-based, with FYA adjustment)")
  class GetEffectiveDemandFunctionBasedTest {

    private TrafficSignalSensorSummary mkSummary(int leftE, int leftW, int leftN, int leftS,
        int rightE, int rightW, int rightN, int rightS) {
      int leftTotal = leftE + leftW + leftN + leftS;
      int rightTotal = rightE + rightW + rightN + rightS;
      return new TrafficSignalSensorSummary(
          0, 0, 0, 0, 0,
          leftTotal, leftE, leftW, leftN, leftS,
          0, 0, 0, 0, 0,
          rightTotal, rightE, rightW, rightN, rightS);
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: single car on FYA-equipped direction is suppressed")
    void leftDemand_fya_singleCarSuppressed() {
      // Circuit has a flashing-left signal facing NORTH and a paired left arrow.
      // 1 car detected on the NB left lane → permissive turn assumed → effective demand 0.
      BlockPos flashLeft = new BlockPos(1, 0, 0);
      BlockPos leftArrow = new BlockPos(2, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeft);
      circuit.getLeftSignals().add(leftArrow);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashLeft, net.minecraft.util.EnumFacing.NORTH);
      facings.put(leftArrow, net.minecraft.util.EnumFacing.NORTH);

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 1, 0, 0, 0, 0, 0));
      assertEquals(0, demand, "Single car on FYA-equipped direction → assumed clears permissively");
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: 2+ cars on FYA-equipped direction count fully")
    void leftDemand_fya_multipleCarsCount() {
      BlockPos flashLeft = new BlockPos(1, 0, 0);
      BlockPos leftArrow = new BlockPos(2, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeft);
      circuit.getLeftSignals().add(leftArrow);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashLeft, net.minecraft.util.EnumFacing.NORTH);
      facings.put(leftArrow, net.minecraft.util.EnumFacing.NORTH);

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 3, 0, 0, 0, 0, 0));
      assertEquals(3, demand,
          "3 cars on a FYA-equipped direction count as 3 (permissive can't clear queue)");
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: direction without FYA counts all cars")
    void leftDemand_noFya_countsAll() {
      BlockPos leftArrow = new BlockPos(2, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      // No flashingLeft signal → no FYA on any direction
      circuit.getLeftSignals().add(leftArrow);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftArrow, net.minecraft.util.EnumFacing.NORTH);

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 1, 0, 0, 0, 0, 0));
      assertEquals(1, demand, "Single car without FYA fully counts as protected demand");
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: mixed installation — FYA on one direction only")
    void leftDemand_mixedInstallation() {
      // NB approach has FYA, SB approach has protected-only left.
      // 1 car NB (FYA — suppressed) + 1 car SB (protected-only — counts) = 1 effective.
      BlockPos flashLeftNb = new BlockPos(1, 0, 0);
      BlockPos leftArrowNb = new BlockPos(2, 0, 0);
      BlockPos leftArrowSb = new BlockPos(3, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeftNb);
      circuit.getLeftSignals().add(leftArrowNb);
      circuit.getLeftSignals().add(leftArrowSb);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashLeftNb, net.minecraft.util.EnumFacing.NORTH);
      facings.put(leftArrowNb, net.minecraft.util.EnumFacing.NORTH);
      facings.put(leftArrowSb, net.minecraft.util.EnumFacing.SOUTH);

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 1, 1, 0, 0, 0, 0));
      // Multi-direction circuit with signals on N+S → areSignalsFacingSameDirection=false → returns 0
      assertEquals(0, demand, "Multi-direction circuit gets 0 (must serve via FYA permissive)");
    }

    @Test
    @DisplayName("getEffectiveLeftDemand: same-direction guard — multi-direction → 0")
    void leftDemand_multiDirectionGuard() {
      BlockPos leftN = new BlockPos(1, 0, 0);
      BlockPos leftS = new BlockPos(2, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftN);
      circuit.getLeftSignals().add(leftS);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftN, net.minecraft.util.EnumFacing.NORTH);
      facings.put(leftS, net.minecraft.util.EnumFacing.SOUTH);

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveLeftDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 5, 5, 0, 0, 0, 0));
      assertEquals(0, demand, "Multi-direction circuit can't safely serve protected lefts");
    }

    @Test
    @DisplayName("getEffectiveRightDemand: single car on FYA direction suppressed")
    void rightDemand_fya_singleCarSuppressed() {
      BlockPos flashRight = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingRightSignals().add(flashRight);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashRight, net.minecraft.util.EnumFacing.EAST);

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveRightDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 0, 0, 1, 0, 0, 0));
      assertEquals(0, demand);
    }

    @Test
    @DisplayName("getEffectiveRightDemand: cars on different directions count independently")
    void rightDemand_multiDirection_independentCounts() {
      BlockPos flashRightE = new BlockPos(1, 0, 0);
      BlockPos flashRightW = new BlockPos(2, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingRightSignals().add(flashRightE);
      circuit.getFlashingRightSignals().add(flashRightW);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashRightE, net.minecraft.util.EnumFacing.EAST);
      facings.put(flashRightW, net.minecraft.util.EnumFacing.WEST);

      // 1 car EB (FYA → suppressed) + 3 cars WB (FYA but 2+) = 0 + 3 = 3.
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveRightDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(0, 0, 0, 0, 1, 3, 0, 0));
      assertEquals(3, demand);
    }
  }

  // ========================================================================
  // getEffectiveDirectionalDemand: function-based overload
  // ========================================================================
  @Nested
  @DisplayName("getEffectiveDirectionalDemand (function-based)")
  class GetEffectiveDirectionalDemandFunctionBasedTest {

    private TrafficSignalSensorSummary mkSummary(int stdE, int leftE, int rightE) {
      return new TrafficSignalSensorSummary(
          stdE, stdE, 0, 0, 0,
          leftE, leftE, 0, 0, 0,
          0, 0, 0, 0, 0,
          rightE, rightE, 0, 0, 0);
    }

    @Test
    @DisplayName("east direction: standard + left + right (no FYA, no adjustment)")
    void eastDirection_noFya() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();

      int demand = TrafficSignalControllerTickerUtilities.getEffectiveDirectionalDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) (pos -> null),
          circuit, mkSummary(2, 1, 3), net.minecraft.util.EnumFacing.EAST);
      assertEquals(6, demand, "Sum: standard 2 + left 1 + right 3");
    }

    @Test
    @DisplayName("east with east-facing FYA-left: single left car suppressed")
    void eastDirection_eastFyaLeft_suppresses() {
      BlockPos flashLeftE = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeftE);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashLeftE, net.minecraft.util.EnumFacing.EAST);

      // Standard 2 + left 1 (suppressed by FYA) + right 3 = 5
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveDirectionalDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(2, 1, 3), net.minecraft.util.EnumFacing.EAST);
      assertEquals(5, demand);
    }

    @Test
    @DisplayName("east with FYA-left at NORTH (different direction): no adjustment")
    void eastDirection_northFyaLeft_noAdjust() {
      BlockPos flashLeftN = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeftN);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashLeftN, net.minecraft.util.EnumFacing.NORTH);

      // FYA on NB doesn't affect EB demand — east left counts fully.
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveDirectionalDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(2, 1, 3), net.minecraft.util.EnumFacing.EAST);
      assertEquals(6, demand);
    }

    @Test
    @DisplayName("east with east-facing FYA-right: single right car suppressed")
    void eastDirection_eastFyaRight_suppresses() {
      BlockPos flashRightE = new BlockPos(1, 0, 0);
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingRightSignals().add(flashRightE);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashRightE, net.minecraft.util.EnumFacing.EAST);

      // Standard 2 + left 1 + right 1 (suppressed) = 3
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveDirectionalDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, mkSummary(2, 1, 1), net.minecraft.util.EnumFacing.EAST);
      assertEquals(3, demand);
    }

    @Test
    @DisplayName("invalid direction (UP/DOWN) returns 0")
    void invalidDirection_zero() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      int demand = TrafficSignalControllerTickerUtilities.getEffectiveDirectionalDemand(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) (pos -> null),
          circuit, mkSummary(5, 5, 5), net.minecraft.util.EnumFacing.UP);
      assertEquals(0, demand);
    }
  }

  // ========================================================================
  // partitionSignalsByFacingSet (function-based with non-null resolver)
  // ========================================================================
  @Nested
  @DisplayName("partitionSignalsByFacingSet (function-based)")
  class PartitionSignalsByFacingSetFunctionBasedTest {

    private static final BlockPos POS_N = new BlockPos(1, 0, 0);
    private static final BlockPos POS_E = new BlockPos(2, 0, 0);
    private static final BlockPos POS_S = new BlockPos(3, 0, 0);

    @Test
    @DisplayName("matching facing → first list; non-matching → second list")
    void mixedMatching() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(POS_N, net.minecraft.util.EnumFacing.NORTH);
      facings.put(POS_E, net.minecraft.util.EnumFacing.EAST);
      facings.put(POS_S, net.minecraft.util.EnumFacing.SOUTH);

      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
              Arrays.asList(POS_N, POS_E, POS_S),
              java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH,
                  net.minecraft.util.EnumFacing.SOUTH));

      assertEquals(2, result.getFirst().size());
      assertTrue(result.getFirst().contains(POS_N));
      assertTrue(result.getFirst().contains(POS_S));
      assertEquals(1, result.getSecond().size());
      assertTrue(result.getSecond().contains(POS_E));
    }

    @Test
    @DisplayName("position with null facing → non-matching")
    void nullFacingPos_nonMatching() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (Function<BlockPos, net.minecraft.util.EnumFacing>) (pos -> null),
              Arrays.asList(POS_N),
              java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH));
      assertTrue(result.getFirst().isEmpty());
      assertEquals(1, result.getSecond().size());
    }

    @Test
    @DisplayName("all matching → first list contains everything in input order")
    void allMatching_orderPreserved() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(POS_S, net.minecraft.util.EnumFacing.NORTH);
      facings.put(POS_E, net.minecraft.util.EnumFacing.NORTH);
      facings.put(POS_N, net.minecraft.util.EnumFacing.NORTH);

      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.partitionSignalsByFacingSet(
              (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
              Arrays.asList(POS_S, POS_E, POS_N),
              java.util.EnumSet.of(net.minecraft.util.EnumFacing.NORTH));

      assertEquals(POS_S, result.getFirst().get(0));
      assertEquals(POS_E, result.getFirst().get(1));
      assertEquals(POS_N, result.getFirst().get(2));
      assertTrue(result.getSecond().isEmpty());
    }
  }

  // ========================================================================
  // areSignalsFacingSameDirection (function-based) on TrafficSignalControllerCircuit
  // ========================================================================
  @Nested
  @DisplayName("TrafficSignalControllerCircuit.areSignalsFacingSameDirection (function-based)")
  class AreSignalsFacingSameDirectionFunctionBasedTest {

    @Test
    @DisplayName("empty circuit → true (no constraint to violate)")
    void emptyCircuit_true() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      assertTrue(c.areSignalsFacingSameDirection(pos -> null));
    }

    @Test
    @DisplayName("all signals facing NORTH → true")
    void allFacingNorth_true() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      BlockPos a = new BlockPos(1, 0, 0);
      BlockPos b = new BlockPos(2, 0, 0);
      c.getThroughSignals().add(a);
      c.getLeftSignals().add(b);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(a, net.minecraft.util.EnumFacing.NORTH);
      facings.put(b, net.minecraft.util.EnumFacing.NORTH);

      assertTrue(c.areSignalsFacingSameDirection(facings::get));
    }

    @Test
    @DisplayName("signals facing different directions → false")
    void mixedDirections_false() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      BlockPos a = new BlockPos(1, 0, 0);
      BlockPos b = new BlockPos(2, 0, 0);
      c.getThroughSignals().add(a);
      c.getLeftSignals().add(b);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(a, net.minecraft.util.EnumFacing.NORTH);
      facings.put(b, net.minecraft.util.EnumFacing.SOUTH);

      assertFalse(c.areSignalsFacingSameDirection(facings::get));
    }

    @Test
    @DisplayName("null-facing positions are skipped (don't count as mismatch)")
    void nullFacings_skipped() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      BlockPos a = new BlockPos(1, 0, 0);
      BlockPos b = new BlockPos(2, 0, 0);  // not in map → null
      c.getThroughSignals().add(a);
      c.getLeftSignals().add(b);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(a, net.minecraft.util.EnumFacing.NORTH);

      assertTrue(c.areSignalsFacingSameDirection(facings::get),
          "Null-facing position is skipped (unloaded chunk); not a mismatch");
    }

    @Test
    @DisplayName("iterates all 7 signal lists (regression — no list missed)")
    void allListsIterated() {
      TrafficSignalControllerCircuit c = emptyCircuit();
      BlockPos[] positions = new BlockPos[7];
      for (int i = 0; i < 7; i++) {
        positions[i] = new BlockPos(i + 1, 0, 0);
      }
      c.getFlashingLeftSignals().add(positions[0]);
      c.getFlashingRightSignals().add(positions[1]);
      c.getLeftSignals().add(positions[2]);
      c.getRightSignals().add(positions[3]);
      c.getThroughSignals().add(positions[4]);
      c.getProtectedSignals().add(positions[5]);
      c.getPedestrianBeaconSignals().add(positions[6]);

      // Make signal at index 6 face EAST while others face NORTH — last position checked.
      // If pedestrianBeaconSignals were skipped, this would incorrectly return true.
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      for (int i = 0; i < 6; i++) {
        facings.put(positions[i], net.minecraft.util.EnumFacing.NORTH);
      }
      facings.put(positions[6], net.minecraft.util.EnumFacing.EAST);

      assertFalse(c.areSignalsFacingSameDirection(facings::get),
          "pedestrianBeaconSignals must be checked too — mismatch should propagate");
    }
  }

  // ========================================================================
  // filterSignalsByFacingDirection (function-based overload)
  // ========================================================================
  @Nested
  @DisplayName("filterSignalsByFacingDirection (function-based)")
  class FilterSignalsByFacingDirectionFunctionBasedTest {

    private static final BlockPos POS_N = new BlockPos(1, 0, 0);
    private static final BlockPos POS_S = new BlockPos(2, 0, 0);
    private static final BlockPos POS_E = new BlockPos(3, 0, 0);

    @Test
    @DisplayName("matching direction in first list, others in second")
    void matchingDirection() {
      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(POS_N, net.minecraft.util.EnumFacing.NORTH);
      facings.put(POS_S, net.minecraft.util.EnumFacing.SOUTH);
      facings.put(POS_E, net.minecraft.util.EnumFacing.EAST);

      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.filterSignalsByFacingDirection(
              (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
              Arrays.asList(POS_N, POS_S, POS_E),
              net.minecraft.util.EnumFacing.NORTH);

      assertEquals(1, result.getFirst().size());
      assertTrue(result.getFirst().contains(POS_N));
      assertEquals(2, result.getSecond().size());
      assertTrue(result.getSecond().contains(POS_S));
      assertTrue(result.getSecond().contains(POS_E));
    }

    @Test
    @DisplayName("null facing → non-matching")
    void nullFacing_nonMatching() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.filterSignalsByFacingDirection(
              (Function<BlockPos, net.minecraft.util.EnumFacing>) (pos -> null),
              Arrays.asList(POS_N),
              net.minecraft.util.EnumFacing.NORTH);
      assertTrue(result.getFirst().isEmpty(),
          "Null facing must NOT be treated as matching even if enumFacing is also null");
      assertEquals(1, result.getSecond().size());
    }

    @Test
    @DisplayName("null resolver → all non-matching")
    void nullResolver_allNonMatching() {
      Tuple<List<BlockPos>, List<BlockPos>> result =
          TrafficSignalControllerTickerUtilities.filterSignalsByFacingDirection(
              (Function<BlockPos, net.minecraft.util.EnumFacing>) null,
              Arrays.asList(POS_N, POS_S),
              net.minecraft.util.EnumFacing.NORTH);
      assertTrue(result.getFirst().isEmpty());
      assertEquals(2, result.getSecond().size());
    }
  }

  // ========================================================================
  // addActiveCircuitToDirectionalGreenPhase (function-based overload)
  // ========================================================================
  @Nested
  @DisplayName("addActiveCircuitToDirectionalGreenPhase (function-based, end-to-end)")
  class AddActiveCircuitToDirectionalGreenPhaseFunctionBasedTest {

    @Test
    @DisplayName("ALL_EAST: east-facing through goes green, west-facing goes red")
    void eastDirection_partitionsByFacing() {
      BlockPos throughE = new BlockPos(1, 0, 0);
      BlockPos throughW = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getThroughSignals().add(throughE);
      circuit.getThroughSignals().add(throughW);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(throughE, net.minecraft.util.EnumFacing.EAST);
      facings.put(throughW, net.minecraft.util.EnumFacing.WEST);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      TrafficSignalControllerTickerUtilities.addActiveCircuitToDirectionalGreenPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, phase, net.minecraft.util.EnumFacing.EAST);

      assertTrue(phase.getGreenSignals().contains(throughE),
          "East-facing through gets green during ALL_EAST");
      assertTrue(phase.getRedSignals().contains(throughW),
          "West-facing through goes red during ALL_EAST");
    }

    @Test
    @DisplayName("ALL_NORTH with FYA-only left: matching FYA flashes, non-matching goes red")
    void northDirection_fyaOnlyLeft() {
      BlockPos fyaLeftN = new BlockPos(1, 0, 0);
      BlockPos fyaLeftS = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(fyaLeftN);
      circuit.getFlashingLeftSignals().add(fyaLeftS);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(fyaLeftN, net.minecraft.util.EnumFacing.NORTH);
      facings.put(fyaLeftS, net.minecraft.util.EnumFacing.SOUTH);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_NORTH);
      TrafficSignalControllerTickerUtilities.addActiveCircuitToDirectionalGreenPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, phase, net.minecraft.util.EnumFacing.NORTH);

      assertTrue(phase.getFyaSignals().contains(fyaLeftN),
          "Matching-direction FYA-only left flashes yellow permissively");
      assertTrue(phase.getRedSignals().contains(fyaLeftS),
          "Opposite-direction FYA-left companion goes red");
    }

    @Test
    @DisplayName("ALL_EAST with matching protected: right turn becomes FYA permissive")
    void eastDirection_matchingProtected_rightFya() {
      BlockPos rightE = new BlockPos(1, 0, 0);
      BlockPos flashRightE = new BlockPos(2, 0, 0);
      BlockPos protE = new BlockPos(3, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getRightSignals().add(rightE);
      circuit.getFlashingRightSignals().add(flashRightE);
      circuit.getProtectedSignals().add(protE);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(rightE, net.minecraft.util.EnumFacing.EAST);
      facings.put(flashRightE, net.minecraft.util.EnumFacing.EAST);
      facings.put(protE, net.minecraft.util.EnumFacing.EAST);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      TrafficSignalControllerTickerUtilities.addActiveCircuitToDirectionalGreenPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, phase, net.minecraft.util.EnumFacing.EAST);

      // Matching protected exists → right goes FYA, protected stays green
      assertTrue(phase.getFyaSignals().contains(flashRightE));
      assertTrue(phase.getRedSignals().contains(rightE));
      assertTrue(phase.getGreenSignals().contains(protE));
    }

    @Test
    @DisplayName("ALL_EAST with no matching protected: right stays solid green")
    void eastDirection_noMatchingProtected_rightSolidGreen() {
      BlockPos rightE = new BlockPos(1, 0, 0);
      BlockPos flashRightE = new BlockPos(2, 0, 0);
      BlockPos protW = new BlockPos(3, 0, 0);  // West-facing protected — not matching

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getRightSignals().add(rightE);
      circuit.getFlashingRightSignals().add(flashRightE);
      circuit.getProtectedSignals().add(protW);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(rightE, net.minecraft.util.EnumFacing.EAST);
      facings.put(flashRightE, net.minecraft.util.EnumFacing.EAST);
      facings.put(protW, net.minecraft.util.EnumFacing.WEST);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_EAST);
      TrafficSignalControllerTickerUtilities.addActiveCircuitToDirectionalGreenPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, phase, net.minecraft.util.EnumFacing.EAST);

      // No matching protected → right is solid green
      assertTrue(phase.getOffSignals().contains(flashRightE));
      assertTrue(phase.getGreenSignals().contains(rightE));
      // Opposite-direction protected → red
      assertTrue(phase.getRedSignals().contains(protW));
    }

    @Test
    @DisplayName("matching left arrow → solid green, FYA-left companion off")
    void matchingLeftArrow_solidGreen() {
      BlockPos leftN = new BlockPos(1, 0, 0);
      BlockPos flashLeftN = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftN);
      circuit.getFlashingLeftSignals().add(flashLeftN);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftN, net.minecraft.util.EnumFacing.NORTH);
      facings.put(flashLeftN, net.minecraft.util.EnumFacing.NORTH);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_NORTH);
      TrafficSignalControllerTickerUtilities.addActiveCircuitToDirectionalGreenPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          circuit, phase, net.minecraft.util.EnumFacing.NORTH);

      assertTrue(phase.getGreenSignals().contains(leftN));
      assertTrue(phase.getOffSignals().contains(flashLeftN));
    }
  }

  // ========================================================================
  // addBlankoutSignalsToPhase (function-based, full per-direction matching)
  // ========================================================================
  @Nested
  @DisplayName("addBlankoutSignalsToPhase (function-based)")
  class AddBlankoutSignalsToPhaseFunctionBasedTest {

    @Test
    @DisplayName("NO_LEFT_TURN at facing where left is GREEN → sign hidden (off)")
    void noLeftTurn_leftGreen_signHidden() {
      BlockPos leftSig = new BlockPos(1, 0, 0);
      BlockPos blankout = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftSig);
      circuit.getNoTurnBlankoutSignals().add(blankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);
      phase.addGreenSignal(leftSig);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftSig, net.minecraft.util.EnumFacing.NORTH);
      facings.put(blankout, net.minecraft.util.EnumFacing.NORTH);

      java.util.Map<BlockPos, BlankoutBoxType> types = new java.util.HashMap<>();
      types.put(blankout, BlankoutBoxType.NO_LEFT_TURN);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) types::get,
          circuit, phase);

      assertTrue(phase.getDontWalkSignals().contains(blankout),
          "Sign should be hidden (dontWalk) when same-facing left is solid green");
      assertFalse(phase.getWalkSignals().contains(blankout));
    }

    @Test
    @DisplayName("NO_LEFT_TURN at facing where left FYA flashes → sign hidden (FYA permits turn)")
    void noLeftTurn_fyaPermissive_signHidden() {
      BlockPos flashLeft = new BlockPos(1, 0, 0);
      BlockPos blankout = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getFlashingLeftSignals().add(flashLeft);
      circuit.getNoTurnBlankoutSignals().add(blankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);
      phase.addFyaSignal(flashLeft);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(flashLeft, net.minecraft.util.EnumFacing.NORTH);
      facings.put(blankout, net.minecraft.util.EnumFacing.NORTH);

      java.util.Map<BlockPos, BlankoutBoxType> types = new java.util.HashMap<>();
      types.put(blankout, BlankoutBoxType.NO_LEFT_TURN);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) types::get,
          circuit, phase);

      assertTrue(phase.getDontWalkSignals().contains(blankout),
          "Sign should be hidden when FYA permissively allows the turn");
    }

    @Test
    @DisplayName("NO_LEFT_TURN at facing where left is RED → sign visible (on)")
    void noLeftTurn_leftRed_signVisible() {
      BlockPos leftSig = new BlockPos(1, 0, 0);
      BlockPos blankout = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftSig);
      circuit.getNoTurnBlankoutSignals().add(blankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignal(leftSig);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftSig, net.minecraft.util.EnumFacing.NORTH);
      facings.put(blankout, net.minecraft.util.EnumFacing.NORTH);

      java.util.Map<BlockPos, BlankoutBoxType> types = new java.util.HashMap<>();
      types.put(blankout, BlankoutBoxType.NO_LEFT_TURN);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) types::get,
          circuit, phase);

      assertTrue(phase.getWalkSignals().contains(blankout),
          "Sign should be visible (walk) when left is red");
      assertFalse(phase.getDontWalkSignals().contains(blankout));
    }

    @Test
    @DisplayName("NO_RIGHT_TURN at facing where left is GREEN (but not right) → sign visible")
    void noRightTurn_leftGreen_rightRed_signVisible() {
      BlockPos leftSig = new BlockPos(1, 0, 0);
      BlockPos blankout = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftSig);
      circuit.getNoTurnBlankoutSignals().add(blankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_LEFTS);
      phase.addGreenSignal(leftSig);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftSig, net.minecraft.util.EnumFacing.NORTH);
      facings.put(blankout, net.minecraft.util.EnumFacing.NORTH);

      java.util.Map<BlockPos, BlankoutBoxType> types = new java.util.HashMap<>();
      types.put(blankout, BlankoutBoxType.NO_RIGHT_TURN);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) types::get,
          circuit, phase);

      assertTrue(phase.getWalkSignals().contains(blankout),
          "NO_RIGHT_TURN sign should remain visible during ALL_LEFTS — right is red");
    }

    @Test
    @DisplayName("blankout at non-matching facing keeps sign visible even when other-direction left is GREEN")
    void blankoutPerFacing_oppositeDirection() {
      BlockPos leftN = new BlockPos(1, 0, 0);
      BlockPos blankoutS = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftN);
      circuit.getNoTurnBlankoutSignals().add(blankoutS);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);
      phase.addGreenSignal(leftN);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftN, net.minecraft.util.EnumFacing.NORTH);
      facings.put(blankoutS, net.minecraft.util.EnumFacing.SOUTH);  // South-facing blankout

      java.util.Map<BlockPos, BlankoutBoxType> types = new java.util.HashMap<>();
      types.put(blankoutS, BlankoutBoxType.NO_LEFT_TURN);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) types::get,
          circuit, phase);

      assertTrue(phase.getWalkSignals().contains(blankoutS),
          "South-facing blankout should stay visible — only NB left is green, SB left is not");
    }

    @Test
    @DisplayName("null type resolver → all blankouts visible (defensive fallback)")
    void nullTypeResolver_allVisible() {
      BlockPos blankout = new BlockPos(1, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getNoTurnBlankoutSignals().add(blankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(blankout, net.minecraft.util.EnumFacing.NORTH);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) null,
          circuit, phase);

      assertTrue(phase.getWalkSignals().contains(blankout));
    }

    @Test
    @DisplayName("null facing resolver → all blankouts visible (defensive fallback)")
    void nullFacingResolver_allVisible() {
      BlockPos blankout = new BlockPos(1, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getNoTurnBlankoutSignals().add(blankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);

      java.util.Map<BlockPos, BlankoutBoxType> types = new java.util.HashMap<>();
      types.put(blankout, BlankoutBoxType.NO_LEFT_TURN);

      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) null,
          (Function<BlockPos, BlankoutBoxType>) types::get,
          circuit, phase);

      assertTrue(phase.getWalkSignals().contains(blankout));
    }

    @Test
    @DisplayName("position not a blankout box (typeResolver returns null) → sign visible")
    void notABlankoutBox_signVisible() {
      BlockPos notABlankout = new BlockPos(1, 0, 0);
      BlockPos leftSig = new BlockPos(2, 0, 0);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getLeftSignals().add(leftSig);
      circuit.getNoTurnBlankoutSignals().add(notABlankout);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS);
      phase.addGreenSignal(leftSig);

      java.util.Map<BlockPos, net.minecraft.util.EnumFacing> facings = new java.util.HashMap<>();
      facings.put(leftSig, net.minecraft.util.EnumFacing.NORTH);
      facings.put(notABlankout, net.minecraft.util.EnumFacing.NORTH);

      // typeResolver returns null for the position — not a blankout box
      TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase(
          (Function<BlockPos, net.minecraft.util.EnumFacing>) facings::get,
          (Function<BlockPos, BlankoutBoxType>) (pos -> null),
          circuit, phase);

      // Conservatively visible since type lookup failed
      assertTrue(phase.getWalkSignals().contains(notABlankout));
    }
  }
}
