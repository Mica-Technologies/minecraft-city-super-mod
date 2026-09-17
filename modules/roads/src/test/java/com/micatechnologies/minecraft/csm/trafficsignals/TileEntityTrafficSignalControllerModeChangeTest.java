package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerCircuits;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerNBTKeys;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalProgrammedPhasePlan;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * Manual mode changes on a faulted controller: cycling past an unprogrammed {@code ADVANCED} mode
 * must not get stuck on its plan-validation fault (issue #204), while every other fault — the
 * conflict monitor's skipped clearance in particular — still blocks a mode change.
 * <p>
 * The controller cannot tick without a world, so {@link #controller} stubs the tick out and the
 * fault is set up the way it is persisted (configured mode, fault message, circuits, plan).
 * </p>
 */
class TileEntityTrafficSignalControllerModeChangeTest {

  private static final String MMU_FAULT = "Skipped yellow clearance at BlockPos{x=1, y=64, z=2}: "
      + "GREEN -> RED (GREEN -> RED_TRANSITIONING)";

  /** The fault an unprogrammed controller raises on its first ADVANCED tick. */
  private static String unprogrammedPlanError() {
    String error = TrafficSignalProgrammedPhasePlan.createDefault()
        .validate(TrafficSignalControllerCircuits.fromNBT(new NBTTagCompound()));
    assertNotNull(error, "the default plan must fail validation with no circuits");
    return error;
  }

  private static NBTTagCompound state(TrafficSignalControllerMode mode, String faultMessage,
      boolean withPlan) {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setInteger(TrafficSignalControllerNBTKeys.MODE, mode.toNBT());
    nbt.setInteger(TrafficSignalControllerNBTKeys.OPERATING_MODE, faultMessage.isEmpty()
        ? mode.toNBT() : TrafficSignalControllerMode.FORCED_FAULT.toNBT());
    nbt.setString(TrafficSignalControllerNBTKeys.CURRENT_FAULT_MESSAGE, faultMessage);
    nbt.setTag(TrafficSignalControllerNBTKeys.CIRCUITS, new NBTTagCompound());
    nbt.setTag(TrafficSignalControllerNBTKeys.OVERLAPS, new NBTTagCompound());
    nbt.setTag(TrafficSignalControllerNBTKeys.CACHED_PHASES, new NBTTagCompound());
    if (withPlan) {
      nbt.setTag(TrafficSignalControllerNBTKeys.ADVANCED_PLAN,
          TrafficSignalProgrammedPhasePlan.createDefault().toNBT());
    }
    return nbt;
  }

  /** A controller whose tick does nothing, loaded from the given state. */
  private static TileEntityTrafficSignalController controller(NBTTagCompound state) {
    TileEntityTrafficSignalController te = new TileEntityTrafficSignalController() {
      @Override
      public void onTick() {
      }
    };
    te.readNBT(state);
    return te;
  }

  @Test
  void advancedIsLastSoCyclingReachesItBeforeWrappingToFlash() {
    TrafficSignalControllerMode[] modes = TrafficSignalControllerMode.values();
    assertEquals(TrafficSignalControllerMode.ADVANCED, modes[modes.length - 1]);
    assertEquals(TrafficSignalControllerMode.FLASH,
        TrafficSignalControllerMode.ADVANCED.getNextMode());
  }

  @Test
  void cyclingPastAFaultedUnprogrammedAdvancedMovesOn() {
    for (boolean withPlan : new boolean[]{true, false}) {
      TileEntityTrafficSignalController te = controller(
          state(TrafficSignalControllerMode.ADVANCED, unprogrammedPlanError(), withPlan));
      assertTrue(te.isInFaultState());
      assertTrue(te.isInAdvancedPlanFault());

      assertEquals("Flash", te.switchMode());
      assertFalse(te.isInFaultState());
      assertEquals(TrafficSignalControllerMode.FLASH.toNBT(), te.getModeOrdinal());
      // Judging the fault must not have created a plan as a side effect
      if (!withPlan) {
        assertNull(te.getProgrammedPhasePlan());
      }
    }
  }

  @Test
  void aFullLapOfModeCyclingIsNeverStuck() {
    NBTTagCompound faultedAdvanced =
        state(TrafficSignalControllerMode.ADVANCED, unprogrammedPlanError(), true);
    // Stand-in for the tick: landing on ADVANCED with the unprogrammed plan faults at once
    TileEntityTrafficSignalController te = new TileEntityTrafficSignalController() {
      @Override
      public void onTick() {
        if (getModeOrdinal() == TrafficSignalControllerMode.ADVANCED.toNBT()
            && !isInFaultState()) {
          readNBT(faultedAdvanced);
        }
      }
    };
    te.readNBT(state(TrafficSignalControllerMode.FLASH, "", true));

    TrafficSignalControllerMode[] modes = TrafficSignalControllerMode.values();
    for (int lap = 0; lap < 2; lap++) {
      for (int i = 1; i <= modes.length; i++) {
        TrafficSignalControllerMode expected = modes[i % modes.length];
        assertEquals(expected.getName(), te.switchMode(), "lap " + lap + ", step " + i);
        assertEquals(expected == TrafficSignalControllerMode.ADVANCED, te.isInFaultState());
      }
    }
  }

  @Test
  void directSelectionLeavesAFaultedUnprogrammedAdvanced() {
    TileEntityTrafficSignalController te = controller(
        state(TrafficSignalControllerMode.ADVANCED, unprogrammedPlanError(), true));
    te.setModeByOrdinal(TrafficSignalControllerMode.NORMAL.toNBT());
    assertFalse(te.isInFaultState());
    assertEquals(TrafficSignalControllerMode.NORMAL.toNBT(), te.getModeOrdinal());
  }

  @Test
  void mmuFaultStillBlocksSwitching() {
    TileEntityTrafficSignalController te = controller(
        state(TrafficSignalControllerMode.ADVANCED, MMU_FAULT, true));
    assertFalse(te.isInAdvancedPlanFault());

    assertEquals("Advanced", te.switchMode());
    assertTrue(te.isInFaultState());
    assertEquals(MMU_FAULT, te.getCurrentFaultMessage());

    te.setModeByOrdinal(TrafficSignalControllerMode.FLASH.toNBT());
    assertTrue(te.isInFaultState());
    assertEquals(TrafficSignalControllerMode.ADVANCED.toNBT(), te.getModeOrdinal());
  }

  @Test
  void mmuFaultInNormalModeStillBlocksSwitching() {
    TileEntityTrafficSignalController te = controller(
        state(TrafficSignalControllerMode.NORMAL, MMU_FAULT, true));
    assertEquals("Normal", te.switchMode());
    assertTrue(te.isInFaultState());
    assertEquals(MMU_FAULT, te.getCurrentFaultMessage());
  }

  @Test
  void otherFaultsStillBlockSwitching() {
    String missing = "Linked signal missing at BlockPos{x=5, y=64, z=5}";
    TileEntityTrafficSignalController advancedMissing = controller(
        state(TrafficSignalControllerMode.ADVANCED, missing, true));
    assertEquals("Advanced", advancedMissing.switchMode());
    assertEquals(missing, advancedMissing.getCurrentFaultMessage());

    // The plan's error text as the fault of another mode is not ADVANCED's plan fault
    TileEntityTrafficSignalController normal = controller(
        state(TrafficSignalControllerMode.NORMAL, unprogrammedPlanError(), true));
    assertFalse(normal.isInAdvancedPlanFault());
    assertEquals("Normal", normal.switchMode());
    assertTrue(normal.isInFaultState());
  }

  @Test
  void unfaultedControllerStillCycles() {
    TileEntityTrafficSignalController te =
        controller(state(TrafficSignalControllerMode.FLASH, "", false));
    assertFalse(te.isInAdvancedPlanFault());
    assertEquals("Normal", te.switchMode());
  }

  @Test
  void planFaultDecisionMatchesOnlyTheCurrentValidationErrorInAdvanced() {
    String error = "Phase 2 is enabled but not assigned to a valid circuit.";
    TrafficSignalControllerMode advanced = TrafficSignalControllerMode.ADVANCED;
    assertTrue(TileEntityTrafficSignalController.isAdvancedPlanFault(advanced, error, error));
    assertFalse(TileEntityTrafficSignalController.isAdvancedPlanFault(advanced, MMU_FAULT, error));
    assertFalse(TileEntityTrafficSignalController.isAdvancedPlanFault(advanced, MMU_FAULT, null));
    assertFalse(TileEntityTrafficSignalController.isAdvancedPlanFault(advanced, "", null));
    assertFalse(TileEntityTrafficSignalController.isAdvancedPlanFault(advanced, null, null));
    assertFalse(TileEntityTrafficSignalController.isAdvancedPlanFault(
        TrafficSignalControllerMode.NORMAL, error, error));
  }
}
