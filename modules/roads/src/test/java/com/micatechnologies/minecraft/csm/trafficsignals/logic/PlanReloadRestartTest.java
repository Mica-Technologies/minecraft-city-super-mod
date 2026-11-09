package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController;
import java.lang.reflect.Field;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A plan written into a running controller's NBT ({@code /blockdata x y z {tcAdv:{...}}}, which
 * merges into the tile entity's NBT and re-reads it) restarts the ADVANCED engine, as a panel
 * edit does, instead of leaving the old program's engine running on the new one; and it does so
 * without taking a head from green or FYA straight to red. The plans are the Alto Springs Main St
 * corridor's own, from the live controllers ({@link AltoCorridorPlans}): the 2026-11-09
 * {@code tcAdv:{ph:[...]}} write that set ped recall and rest in walk on all eight, and Elm's
 * {@code tcAdv:{co:{...}}} merge.
 */
class PlanReloadRestartTest {

  private static NBTTagCompound controllerNbt(NBTTagCompound adv) {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setInteger(TrafficSignalControllerNBTKeys.MODE, TrafficSignalControllerMode.ADVANCED.toNBT());
    nbt.setInteger(TrafficSignalControllerNBTKeys.OPERATING_MODE,
        TrafficSignalControllerMode.ADVANCED.toNBT());
    nbt.setTag(TrafficSignalControllerNBTKeys.CIRCUITS, new NBTTagCompound());
    nbt.setTag(TrafficSignalControllerNBTKeys.OVERLAPS, new NBTTagCompound());
    nbt.setTag(TrafficSignalControllerNBTKeys.CACHED_PHASES, new NBTTagCompound());
    nbt.setTag(TrafficSignalControllerNBTKeys.ADVANCED_PLAN, adv);
    return nbt;
  }

  private static Field field(String name) throws Exception {
    Field f = TileEntityTrafficSignalController.class.getDeclaredField(name);
    f.setAccessible(true);
    return f;
  }

  /** A controller loaded from {@code nbt} whose ADVANCED engine is running, showing a green. */
  private static TileEntityTrafficSignalController running(NBTTagCompound nbt) throws Exception {
    TileEntityTrafficSignalController te = new TileEntityTrafficSignalController() {
      @Override
      public void onTick() {
      }
    };
    te.readNBT(nbt);
    assertNull(field("advancedRuntime").get(te), "a chunk load must not create an engine");
    field("advancedRuntime").set(te, new RingBarrierState());
    TrafficSignalPhase shown = new TrafficSignalPhase(TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
        TrafficSignalPhaseApplicability.NONE);
    shown.addGreenSignals(java.util.Collections.singletonList(new BlockPos(1, 64, 1)));
    field("currentPhase").set(te, shown);
    return te;
  }

  @Test
  @DisplayName("a ph-only and a co-only /blockdata write restart the running engine through an "
      + "entry clearance; an unchanged re-read leaves it alone")
  void changedPlanRestartsTheEngine() throws Exception {
    int restartedCount = 0;
    for (String j : AltoCorridorPlans.JUNCTIONS) {
      NBTTagCompound before = j.equals("elm") ? AltoCorridorPlans.elmBeforeCoordination()
          : AltoCorridorPlans.beforePedRecall(j);
      TileEntityTrafficSignalController te = running(controllerNbt(before));
      Object engine = field("advancedRuntime").get(te);

      te.readNBT(controllerNbt(before.copy()));
      assertSame(engine, field("advancedRuntime").get(te),
          j + ": re-reading the same program must not restart the engine");

      NBTTagCompound after = AltoCorridorPlans.after(j);
      te.readNBT(controllerNbt(after));
      RingBarrierState restarted = (RingBarrierState) field("advancedRuntime").get(te);
      if (AltoCorridorPlans.plan(before).toNBT().equals(AltoCorridorPlans.plan(after).toNBT())) {
        // This junction's write changed nothing (it already had the settings).
        assertSame(engine, restarted, j + ": an identical program must not restart the engine");
        continue;
      }
      assertNotNull(restarted, j);
      assertNotSame(engine, restarted, j + ": the rewritten program must get a fresh engine");
      assertTrue(restarted.isInEntryClearance(),
          j + ": the fresh engine must first clear what the old one was showing");
      restartedCount++;
    }
    // The dump's "before" phase lists already carry ped recall at six junctions, so only Elm (its
    // co merge) and the junctions whose ph write changed something restart.
    assertTrue(restartedCount >= 2, "the changed programs must restart; " + restartedCount
        + " restarted");
  }

  @Test
  @DisplayName("an engine taking over from a shown phase clears every green and FYA head through "
      + "yellow, holds all red, then starts the rings: the conflict monitor never trips")
  void entryClearanceNeverSkipsYellow() throws Exception {
    TrafficSignalProgrammedPhasePlan plan =
        AltoCorridorPlans.plan(AltoCorridorPlans.after("hammond"));
    int count = AltoCorridorPlans.circuitCount(plan);
    TrafficSignalControllerCircuits ckts = RingBarrierStateTest.circuits(count);
    for (int c = 0; c < count; c++) {
      ckts.getCircuit(c).getThroughSignals().add(new BlockPos(c, 64, 0));
      ckts.getCircuit(c).getFlashingLeftSignals().add(new BlockPos(c, 64, 1));
      ckts.getCircuit(c).getLeftSignals().add(new BlockPos(c, 64, 1));
    }
    RingBarrierStateTest.Demand busy = new RingBarrierStateTest.Demand();
    for (int c = 0; c < count; c++) {
      busy.veh(c, 1, 1, 0);
    }
    // The old engine runs the old program; the phase it shows is what the new one takes over.
    RingBarrierState old = new RingBarrierState();
    TrafficSignalProgrammedPhasePlan oldPlan =
        AltoCorridorPlans.plan(AltoCorridorPlans.beforePedRecall("hammond"));
    TrafficSignalPhase shown = null;
    for (long t = 0; t < 3000; t += 2) {
      TrafficSignalPhase p = old.tick(oldPlan, ckts, RingBarrierStateCoordinationTest.NO_OVERLAPS,
          t, busy);
      shown = p != null ? p : shown;
    }
    assertFalse(shown.getGreenSignals().isEmpty() && shown.getFyaSignals().isEmpty(),
        "sanity: something is green or flashing when the program is rewritten");

    RingBarrierState fresh = new RingBarrierState();
    fresh.beginAfter(shown);
    fresh.beginOnRestPhases();
    long clearance = RingBarrierState.outputClearanceYellow(plan)
        + RingBarrierState.entryClearanceRed(plan);
    TrafficSignalPhase prev = shown;
    boolean ringsStarted = false;
    for (long t = 3000; t < 3000 + 2400 * 2; t += 2) {
      TrafficSignalPhase p = fresh.tick(plan, ckts, RingBarrierStateCoordinationTest.NO_OVERLAPS,
          t, busy);
      if (p == null) {
        continue;
      }
      String skipped = TrafficSignalControllerTickerUtilities.findSkippedClearance(prev, p, ckts);
      assertNull(skipped, "t=" + t + ": " + skipped);
      if (t - 3000 < clearance) {
        assertTrue(p.getGreenSignals().isEmpty() && p.getFyaSignals().isEmpty(),
            "t=" + t + ": nothing may be green during the changeover clearance");
      } else {
        ringsStarted |= !p.getGreenSignals().isEmpty();
      }
      prev = p;
    }
    assertTrue(ringsStarted, "the new program's rings must start after the clearance");
  }
}
