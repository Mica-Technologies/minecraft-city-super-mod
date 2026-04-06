package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for pure (no-World) methods in {@link TrafficSignalControllerTicker}.
 */
class TrafficSignalControllerTickerTest {

  // -- Helpers --

  /**
   * Creates a {@link TrafficSignalPhases} populated with the given phases via reflection
   * (the array constructor is private).
   */
  private static TrafficSignalPhases buildPhases(TrafficSignalPhase... phasesArray) {
    try {
      // Pad to required size (PHASE_INDEX_COUNT = 22) with dummy phases
      TrafficSignalPhase[] padded = new TrafficSignalPhase[22];
      TrafficSignalPhase dummy = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);
      Arrays.fill(padded, dummy);
      for (int i = 0; i < phasesArray.length && i < padded.length; i++) {
        if (phasesArray[i] != null) {
          padded[i] = phasesArray[i];
        }
      }
      Constructor<TrafficSignalPhases> ctor =
          TrafficSignalPhases.class.getDeclaredConstructor(TrafficSignalPhase[].class);
      ctor.setAccessible(true);
      return ctor.newInstance((Object) padded);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to build TrafficSignalPhases via reflection", e);
    }
  }

  /**
   * Creates an empty {@link TrafficSignalControllerCircuit} via fromNBT with an empty tag.
   */
  private static TrafficSignalControllerCircuit emptyCircuit() {
    return TrafficSignalControllerCircuit.fromNBT(new NBTTagCompound());
  }

  /**
   * Creates a {@link TrafficSignalControllerCircuits} with the given circuits.
   */
  private static TrafficSignalControllerCircuits buildCircuits(
      TrafficSignalControllerCircuit... circuitArray) {
    TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
    for (TrafficSignalControllerCircuit c : circuitArray) {
      circuits.addCircuit(c);
    }
    return circuits;
  }

  // ========================================================================
  // faultModeTick
  // ========================================================================
  @Nested
  @DisplayName("faultModeTick")
  class FaultModeTickTest {

    @Test
    @DisplayName("alternatingFlash=true returns FAULT_1 phase (index 3)")
    void alternatingFlashTrue_returnsFault1() {
      TrafficSignalPhase fault1 = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);
      fault1.addRedSignal(new BlockPos(1, 0, 0));

      TrafficSignalPhase fault2 = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);
      fault2.addRedSignal(new BlockPos(2, 0, 0));

      TrafficSignalPhase[] arr = new TrafficSignalPhase[22];
      Arrays.fill(arr, fault2); // fill with dummy
      arr[TrafficSignalPhases.PHASE_INDEX_FAULT_1] = fault1;
      arr[TrafficSignalPhases.PHASE_INDEX_FAULT_2] = fault2;

      TrafficSignalPhases phases = buildPhases(arr);

      TrafficSignalPhase result = TrafficSignalControllerTicker.faultModeTick(phases, true);
      assertSame(fault1, result);
    }

    @Test
    @DisplayName("alternatingFlash=false returns FAULT_2 phase (index 4)")
    void alternatingFlashFalse_returnsFault2() {
      TrafficSignalPhase fault1 = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);

      TrafficSignalPhase fault2 = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);

      TrafficSignalPhase[] arr = new TrafficSignalPhase[22];
      Arrays.fill(arr, fault1);
      arr[TrafficSignalPhases.PHASE_INDEX_FAULT_1] = fault1;
      arr[TrafficSignalPhases.PHASE_INDEX_FAULT_2] = fault2;

      TrafficSignalPhases phases = buildPhases(arr);

      TrafficSignalPhase result = TrafficSignalControllerTicker.faultModeTick(phases, false);
      assertSame(fault2, result);
    }

    @Test
    @DisplayName("returned phases are never null")
    void returnedPhaseNeverNull() {
      TrafficSignalPhase fault1 = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);
      TrafficSignalPhase fault2 = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NONE);

      TrafficSignalPhase[] arr = new TrafficSignalPhase[22];
      Arrays.fill(arr, fault1);
      arr[TrafficSignalPhases.PHASE_INDEX_FAULT_1] = fault1;
      arr[TrafficSignalPhases.PHASE_INDEX_FAULT_2] = fault2;

      TrafficSignalPhases phases = buildPhases(arr);

      assertNotNull(TrafficSignalControllerTicker.faultModeTick(phases, true));
      assertNotNull(TrafficSignalControllerTicker.faultModeTick(phases, false));
    }
  }

  // ========================================================================
  // manualOffModeTick
  // ========================================================================
  @Nested
  @DisplayName("manualOffModeTick")
  class ManualOffModeTickTest {

    @Test
    @DisplayName("originalPhase=null returns the OFF phase")
    void nullOriginalPhase_returnsOffPhase() {
      TrafficSignalPhase offPhase = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NO_POWER);

      TrafficSignalPhase[] arr = new TrafficSignalPhase[22];
      Arrays.fill(arr, offPhase);
      arr[TrafficSignalPhases.PHASE_INDEX_OFF] = offPhase;
      TrafficSignalPhases phases = buildPhases(arr);

      TrafficSignalPhase result = TrafficSignalControllerTicker.manualOffModeTick(phases, null);
      assertSame(offPhase, result);
    }

    @Test
    @DisplayName("originalPhase=non-null returns null (no change needed)")
    void nonNullOriginalPhase_returnsNull() {
      TrafficSignalPhase offPhase = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NO_POWER);

      TrafficSignalPhase[] arr = new TrafficSignalPhase[22];
      Arrays.fill(arr, offPhase);
      TrafficSignalPhases phases = buildPhases(arr);

      TrafficSignalPhase existing = new TrafficSignalPhase(
          TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
          TrafficSignalPhaseApplicability.NO_POWER);

      TrafficSignalPhase result = TrafficSignalControllerTicker.manualOffModeTick(phases, existing);
      assertNull(result);
    }
  }

  // ========================================================================
  // flashModeTick
  // ========================================================================
  @Nested
  @DisplayName("flashModeTick")
  class FlashModeTickTest {

    private TrafficSignalPhases buildFullPhases() {
      TrafficSignalPhase[] arr = new TrafficSignalPhase[22];
      for (int i = 0; i < 22; i++) {
        arr[i] = new TrafficSignalPhase(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
            TrafficSignalPhaseApplicability.NONE);
        // Tag each phase with a unique signal so we can identify it
        arr[i].addRedSignal(new BlockPos(i, 0, 0));
      }
      return buildPhases(arr);
    }

    @Test
    @DisplayName("RAMP_METER_FULL_TIME + alternatingFlash=true returns ramp flash 1")
    void rampMeterFullTime_altTrue() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.RAMP_METER_FULL_TIME, phases, true, false);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_1), result);
    }

    @Test
    @DisplayName("RAMP_METER_FULL_TIME + alternatingFlash=false returns ramp flash 2")
    void rampMeterFullTime_altFalse() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.RAMP_METER_FULL_TIME, phases, false, false);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_2), result);
    }

    @Test
    @DisplayName("RAMP_METER_PART_TIME + alternatingFlash=true returns ramp flash 1")
    void rampMeterPartTime_altTrue() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.RAMP_METER_PART_TIME, phases, true, false);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_RAMP_METER_FLASH_1), result);
    }

    @Test
    @DisplayName("allRedFlash=true returns fault phase 1 when alternatingFlash=true")
    void allRedFlash_altTrue() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.NORMAL, phases, true, true);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_FAULT_1), result);
    }

    @Test
    @DisplayName("allRedFlash=true returns fault phase 2 when alternatingFlash=false")
    void allRedFlash_altFalse() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.NORMAL, phases, false, true);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_FAULT_2), result);
    }

    @Test
    @DisplayName("standard flash + alternatingFlash=true returns flash phase 1")
    void standardFlash_altTrue() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.NORMAL, phases, true, false);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_FLASH_1), result);
    }

    @Test
    @DisplayName("standard flash + alternatingFlash=false returns flash phase 2")
    void standardFlash_altFalse() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.NORMAL, phases, false, false);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_FLASH_2), result);
    }

    @Test
    @DisplayName("FLASH mode (non-ramp, non-allRed) returns standard flash")
    void flashMode_standardFlash() {
      TrafficSignalPhases phases = buildFullPhases();
      TrafficSignalPhase result = TrafficSignalControllerTicker.flashModeTick(
          TrafficSignalControllerMode.FLASH, phases, true, false);
      assertSame(phases.getPhase(TrafficSignalPhases.PHASE_INDEX_FLASH_1), result);
    }
  }

  // ========================================================================
  // overrideBeaconsToYellow (private -- tested via reflection)
  // ========================================================================
  @Nested
  @DisplayName("overrideBeaconsToYellow")
  class OverrideBeaconsToYellowTest {

    private TrafficSignalPhase invoke(TrafficSignalPhase phase,
        TrafficSignalControllerCircuits circuits) {
      try {
        Method m = TrafficSignalControllerTicker.class.getDeclaredMethod(
            "overrideBeaconsToYellow", TrafficSignalPhase.class,
            TrafficSignalControllerCircuits.class);
        m.setAccessible(true);
        return (TrafficSignalPhase) m.invoke(null, phase, circuits);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    @DisplayName("phase with beacon signals in RED - beacons move to YELLOW")
    void beaconsInRed_movedToYellow() {
      BlockPos beacon1 = new BlockPos(100, 64, 200);
      BlockPos through1 = new BlockPos(10, 64, 20);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getBeaconSignals().add(beacon1);
      circuit.getThroughSignals().add(through1);
      TrafficSignalControllerCircuits circuits = buildCircuits(circuit);

      // Phase with beacon in red, through in green
      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addRedSignal(beacon1);
      phase.addGreenSignal(through1);

      TrafficSignalPhase result = invoke(phase, circuits);
      assertTrue(result.getYellowSignals().contains(beacon1),
          "Beacon should be moved to yellow");
      assertFalse(result.getRedSignals().contains(beacon1),
          "Beacon should no longer be in red");
    }

    @Test
    @DisplayName("phase with no beacons - returns same phase instance")
    void noBeacons_returnsSameInstance() {
      TrafficSignalControllerCircuit circuit = emptyCircuit();
      TrafficSignalControllerCircuits circuits = buildCircuits(circuit);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addGreenSignal(new BlockPos(10, 64, 20));

      TrafficSignalPhase result = invoke(phase, circuits);
      assertSame(phase, result, "Should return same instance when no beacons");
    }

    @Test
    @DisplayName("phase with beacons already in YELLOW - returns same instance")
    void beaconsAlreadyYellow_returnsSameInstance() {
      BlockPos beacon1 = new BlockPos(100, 64, 200);

      TrafficSignalControllerCircuit circuit = emptyCircuit();
      circuit.getBeaconSignals().add(beacon1);
      TrafficSignalControllerCircuits circuits = buildCircuits(circuit);

      TrafficSignalPhase phase = new TrafficSignalPhase(1, null,
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS);
      phase.addYellowSignal(beacon1);

      TrafficSignalPhase result = invoke(phase, circuits);
      assertSame(phase, result, "Should return same instance when beacons already yellow");
    }
  }

  // ========================================================================
  // isThroughTypeApplicability (in TickerUtilities, but tested here per spec)
  // ========================================================================
  @Nested
  @DisplayName("isThroughTypeApplicability")
  class IsThroughTypeApplicabilityTest {

    @Test
    void allThroughsRights_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS));
    }

    @Test
    void allThroughsProtecteds_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTEDS));
    }

    @Test
    void allThroughsProtectedRights_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_PROTECTED_RIGHTS));
    }

    @Test
    void allLefts_isFalse() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }

    @Test
    void none_isFalse() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.NONE));
    }

    @Test
    void allEast_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_EAST));
    }

    @Test
    void allWest_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_WEST));
    }

    @Test
    void allNorth_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_NORTH));
    }

    @Test
    void allSouth_isTrue() {
      assertTrue(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_SOUTH));
    }

    @Test
    void pedestrian_isFalse() {
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.PEDESTRIAN));
    }

    @Test
    void allProtecteds_isFalse() {
      // ALL_PROTECTEDS is not an enum value; closest non-through is PEDESTRIAN or ALL_LEFTS
      // Test ALL_RED as another non-through
      assertFalse(TrafficSignalControllerTickerUtilities.isThroughTypeApplicability(
          TrafficSignalPhaseApplicability.ALL_RED));
    }
  }

  // ========================================================================
  // isSamePhaseCategory
  // ========================================================================
  @Nested
  @DisplayName("isSamePhaseCategory")
  class IsSamePhaseCategoryTest {

    @Test
    @DisplayName("two through-types are same category")
    void twoThroughTypes_sameCategory() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS,
          TrafficSignalPhaseApplicability.ALL_EAST));
    }

    @Test
    @DisplayName("same non-through type is same category")
    void sameNonThrough_sameCategory() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_LEFTS,
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }

    @Test
    @DisplayName("different non-through types are different category")
    void differentNonThroughs_differentCategory() {
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_LEFTS,
          TrafficSignalPhaseApplicability.PEDESTRIAN));
    }

    @Test
    @DisplayName("through + non-through are different category")
    void throughAndNonThrough_differentCategory() {
      assertFalse(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_THROUGHS_RIGHTS,
          TrafficSignalPhaseApplicability.ALL_LEFTS));
    }

    @Test
    @DisplayName("two directional throughs are same category")
    void twoDirectionals_sameCategory() {
      assertTrue(TrafficSignalControllerTickerUtilities.isSamePhaseCategory(
          TrafficSignalPhaseApplicability.ALL_NORTH,
          TrafficSignalPhaseApplicability.ALL_SOUTH));
    }
  }

  // ========================================================================
  // adjustForFya (private -- tested via reflection)
  // ========================================================================
  @Nested
  @DisplayName("adjustForFya")
  class AdjustForFyaTest {

    private int invoke(int leftCount, boolean hasFya) {
      try {
        Method m = TrafficSignalControllerTickerUtilities.class.getDeclaredMethod(
            "adjustForFya", int.class, boolean.class);
        m.setAccessible(true);
        return (int) m.invoke(null, leftCount, hasFya);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    @DisplayName("adjustForFya(0, true) returns 0")
    void zeroWithFya() {
      assertEquals(0, invoke(0, true));
    }

    @Test
    @DisplayName("adjustForFya(1, true) returns 0 (single vehicle cleared by permissive)")
    void oneWithFya() {
      assertEquals(0, invoke(1, true));
    }

    @Test
    @DisplayName("adjustForFya(2, true) returns 2 (multiple vehicles need protected)")
    void twoWithFya() {
      assertEquals(2, invoke(2, true));
    }

    @Test
    @DisplayName("adjustForFya(1, false) returns 1 (no FYA, count as-is)")
    void oneWithoutFya() {
      assertEquals(1, invoke(1, false));
    }

    @Test
    @DisplayName("adjustForFya(0, false) returns 0")
    void zeroWithoutFya() {
      assertEquals(0, invoke(0, false));
    }

    @Test
    @DisplayName("adjustForFya(5, true) returns 5 (large count unaffected)")
    void largeCountWithFya() {
      assertEquals(5, invoke(5, true));
    }
  }
}
