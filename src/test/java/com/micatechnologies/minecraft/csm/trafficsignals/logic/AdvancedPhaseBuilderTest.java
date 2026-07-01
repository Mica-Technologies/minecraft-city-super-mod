package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ADVANCED-mode ASC/3 features: the pure PPLT FYA lens decision in
 * {@link AdvancedPhaseBuilder} and the NBT persistence of the new {@link TrafficSignalProgrammedPhase}
 * fields (Delayed Green / DLY GRN and FYA permissive phase). See
 * {@code assets/docs/ADVANCED_MODE_ASC3.md}.
 */
class AdvancedPhaseBuilderTest {

  private static final BlockPos FYA_A = new BlockPos(1, 0, 0);
  private static final BlockPos ARROW_A = new BlockPos(3, 0, 0);

  @Nested
  @DisplayName("PPLT FYA compound-head lens mapping")
  class FyaLensTest {

    private TrafficSignalProgrammedPhasePlan fyaPlan() {
      TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
      TrafficSignalProgrammedPhase left = plan.getPhase(1);
      left.setCircuitIndex(0);
      left.setEnabled(true);
      left.setMovement(TrafficSignalPhaseMovement.PROTECTED_LEFT);
      left.setPermissivePhase(6); // opposing through
      return plan;
    }

    /** Circuit 0 is an FYA compound head: 3-section lens {@link #FYA_A}, add-on arrow {@link #ARROW_A}. */
    private TrafficSignalControllerCircuits fyaCircuits() {
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      TrafficSignalControllerCircuit c0 = new TrafficSignalControllerCircuit();
      c0.getFlashingLeftSignals().add(FYA_A);
      c0.getLeftSignals().add(ARROW_A);
      circuits.addCircuit(c0);
      circuits.addCircuit(new TrafficSignalControllerCircuit()); // circuit 1
      return circuits;
    }

    private TrafficSignalPhase build(int servedPhase, VehInterval iv) {
      return AdvancedPhaseBuilder.build(null, fyaPlan(), fyaCircuits(),
          new TrafficSignalControllerOverlaps(), new ServedMovementShortcut(servedPhase, iv).m, null);
    }

    @Test
    @DisplayName("baseFyaState: served + permissive combinations")
    void baseStates() {
      assertEquals(AdvancedPhaseBuilder.FyaLensState.PROTECTED_GREEN,
          AdvancedPhaseBuilder.baseFyaState(VehInterval.GREEN, null, true));
      assertEquals(AdvancedPhaseBuilder.FyaLensState.SOLID_YELLOW,
          AdvancedPhaseBuilder.baseFyaState(VehInterval.YELLOW, null, true));
      assertEquals(AdvancedPhaseBuilder.FyaLensState.RED,
          AdvancedPhaseBuilder.baseFyaState(VehInterval.RED, null, true));
      assertEquals(AdvancedPhaseBuilder.FyaLensState.FLASH,
          AdvancedPhaseBuilder.baseFyaState(null, VehInterval.GREEN, true));
      assertEquals(AdvancedPhaseBuilder.FyaLensState.SOLID_YELLOW,
          AdvancedPhaseBuilder.baseFyaState(null, VehInterval.YELLOW, true),
          "opposing through yellow -> FYA clears to solid yellow concurrently, not flashing");
      assertEquals(AdvancedPhaseBuilder.FyaLensState.RED,
          AdvancedPhaseBuilder.baseFyaState(null, VehInterval.RED, true));
      assertEquals(AdvancedPhaseBuilder.FyaLensState.RED,
          AdvancedPhaseBuilder.baseFyaState(null, VehInterval.GREEN, false),
          "no permissive phase configured -> never flashes");
    }

    @Test
    @DisplayName("protected green: 3-section OFF + green arrow, never red-with-green")
    void protectedGreen_noRedWithGreen() {
      TrafficSignalPhase phase = build(1, VehInterval.GREEN);
      assertTrue(phase.getOffSignals().contains(FYA_A), "3-section dark during protected green");
      assertTrue(phase.getGreenSignals().contains(ARROW_A), "green-arrow add-on shows green");
      assertFalse(phase.getRedSignals().contains(FYA_A),
          "3-section must NOT be red while the green arrow shows (the reported red+green bug)");
    }

    @Test
    @DisplayName("protected clearance (served YELLOW): 3-section solid yellow, add-on red")
    void protectedYellow_solidYellow() {
      TrafficSignalPhase phase = build(1, VehInterval.YELLOW);
      assertTrue(phase.getYellowSignals().contains(FYA_A));
      assertTrue(phase.getRedSignals().contains(ARROW_A));
    }

    @Test
    @DisplayName("permissive: 3-section flashes yellow, green-arrow add-on red")
    void permissive_flash() {
      TrafficSignalPhase phase = build(6, VehInterval.GREEN);
      assertTrue(phase.getFyaSignals().contains(FYA_A));
      assertTrue(phase.getRedSignals().contains(ARROW_A));
      assertFalse(phase.getGreenSignals().contains(ARROW_A));
    }

    @Test
    @DisplayName("opposing through YELLOW: FYA clears to solid yellow concurrently, not flashing")
    void opposingYellow_solidYellowConcurrent() {
      TrafficSignalPhase phase = build(6, VehInterval.YELLOW);
      assertTrue(phase.getYellowSignals().contains(FYA_A),
          "FYA clears to a solid yellow arrow together with the opposing through's yellow");
      assertFalse(phase.getFyaSignals().contains(FYA_A),
          "FYA stops flashing once the opposing through begins clearing");
    }
  }

  @Nested
  @DisplayName("phase-based vehicle overlaps")
  class OverlapTest {

    private ServedMovementShortcut served(int phase, VehInterval iv) {
      return new ServedMovementShortcut(phase, iv);
    }

    @Test
    @DisplayName("overlapState: green dominates, then yellow, else red")
    void overlapState() {
      assertEquals(VehInterval.GREEN, AdvancedPhaseBuilder.overlapState(new int[] {2, 4},
          served(2, VehInterval.GREEN).m, served(4, VehInterval.YELLOW).m));
      assertEquals(VehInterval.YELLOW, AdvancedPhaseBuilder.overlapState(new int[] {2, 4},
          served(4, VehInterval.YELLOW).m, null));
      assertEquals(VehInterval.RED, AdvancedPhaseBuilder.overlapState(new int[] {2, 4},
          served(6, VehInterval.GREEN).m, null));
      assertEquals(VehInterval.RED, AdvancedPhaseBuilder.overlapState(new int[0],
          served(2, VehInterval.GREEN).m, null));
    }

    @Test
    @DisplayName("NBT round-trip for a programmed overlap")
    void nbtRoundTrip() {
      TrafficSignalProgrammedOverlap o = new TrafficSignalProgrammedOverlap();
      o.setEnabled(true);
      o.setOutputCircuitIndex(1);
      o.setOutputMovement(TrafficSignalPhaseMovement.RIGHT);
      o.setIncludedPhases(new int[] {2, 7});
      o.setTrailGreen(40L);
      o.setLeadGreen(25L);
      o.setType(TrafficSignalOverlapType.MINUS_GREEN_YELLOW);
      o.setModifierPhases(new int[] {6});
      TrafficSignalProgrammedOverlap r = TrafficSignalProgrammedOverlap.fromNBT(o.toNBT());
      assertTrue(r.isEnabled());
      assertEquals(1, r.getOutputCircuitIndex());
      assertEquals(TrafficSignalPhaseMovement.RIGHT, r.getOutputMovement());
      assertArrayEquals(new int[] {2, 7}, r.getIncludedPhases());
      assertEquals(40L, r.getTrailGreen());
      assertEquals(25L, r.getLeadGreen());
      assertEquals(TrafficSignalOverlapType.MINUS_GREEN_YELLOW, r.getType());
      assertArrayEquals(new int[] {6}, r.getModifierPhases());
    }

    @Test
    @DisplayName("anyServedActive: green or yellow modifier is active; red/absent is not")
    void anyServedActive() {
      assertTrue(AdvancedPhaseBuilder.anyServedActive(new int[] {6},
          served(6, VehInterval.GREEN).m, null));
      assertTrue(AdvancedPhaseBuilder.anyServedActive(new int[] {6},
          served(6, VehInterval.YELLOW).m, null));
      assertFalse(AdvancedPhaseBuilder.anyServedActive(new int[] {6},
          served(6, VehInterval.RED).m, null));
      assertFalse(AdvancedPhaseBuilder.anyServedActive(new int[] {6}, null, null));
      assertFalse(AdvancedPhaseBuilder.anyServedActive(new int[0],
          served(6, VehInterval.GREEN).m, null));
    }

    @Test
    @DisplayName("builder: overlap greens a different circuit's right head while an included "
        + "phase is green")
    void overlapDrivesHeads() {
      BlockPos rightHead = new BlockPos(20, 0, 0);
      TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();
      circuits.addCircuit(new TrafficSignalControllerCircuit());       // circuit 0 (served)
      TrafficSignalControllerCircuit c1 = new TrafficSignalControllerCircuit();
      c1.getRightSignals().add(rightHead);                              // circuit 1 (overlap output)
      circuits.addCircuit(c1);

      TrafficSignalProgrammedPhasePlan plan = TrafficSignalProgrammedPhasePlan.createDefault();
      plan.getPhase(2).setCircuitIndex(0);
      plan.getPhase(2).setEnabled(true);
      TrafficSignalProgrammedOverlap ov = new TrafficSignalProgrammedOverlap();
      ov.setEnabled(true);
      ov.setOutputCircuitIndex(1);
      ov.setOutputMovement(TrafficSignalPhaseMovement.RIGHT);
      ov.setIncludedPhases(new int[] {2});
      plan.getVehicleOverlaps().add(ov);

      TrafficSignalPhase phase = AdvancedPhaseBuilder.build(null, plan, circuits,
          new TrafficSignalControllerOverlaps(), served(2, VehInterval.GREEN).m, null);

      assertTrue(phase.getGreenSignals().contains(rightHead),
          "the overlap drives its output right head green while phase 2 (its included phase) is green");
    }
  }

  /** Small holder so tests can build a ServedMovement without repeating the PedInterval. */
  private static final class ServedMovementShortcut {
    final RingBarrierState.ServedMovement m;

    ServedMovementShortcut(int phase, VehInterval iv) {
      this.m = new RingBarrierState.ServedMovement(phase, iv, RingBarrierState.PedInterval.NONE);
    }
  }

  @Nested
  @DisplayName("TrafficSignalProgrammedPhase NBT: Delayed Green + permissive phase")
  class PhaseNbtTest {

    @Test
    @DisplayName("all advanced phase fields round-trip through NBT")
    void roundTrip() {
      TrafficSignalProgrammedPhase p = new TrafficSignalProgrammedPhase(1, 1, 0);
      p.setDelayedGreen(60L);
      p.setPermissivePhase(6);
      p.setMax2(240L);
      p.setBikeMinGreen(180L);
      p.setAddedInitial(40L);
      p.setMaxInitial(200L);
      p.setMinGap(20L);
      p.setTimeBeforeReduce(100L);
      p.setTimeToReduce(300L);
      p.setRestInWalk(true);
      p.setDualEntry(true);
      p.setConditionalService(true);
      TrafficSignalProgrammedPhase r = TrafficSignalProgrammedPhase.fromNBT(p.toNBT());
      assertTrue(r.isRestInWalk());
      assertTrue(r.isDualEntry());
      assertTrue(r.isConditionalService());
      assertEquals(60L, r.getDelayedGreen());
      assertEquals(6, r.getPermissivePhase());
      assertEquals(240L, r.getMax2());
      assertEquals(180L, r.getBikeMinGreen());
      assertEquals(40L, r.getAddedInitial());
      assertEquals(200L, r.getMaxInitial());
      assertEquals(20L, r.getMinGap());
      assertEquals(100L, r.getTimeBeforeReduce());
      assertEquals(300L, r.getTimeToReduce());
    }

    @Test
    @DisplayName("absent keys default to off (0) for backward compatibility")
    void defaultsWhenAbsent() {
      // A legacy compound that only has the required structural keys, no DLY GRN / perm phase.
      NBTTagCompound legacy = new TrafficSignalProgrammedPhase(2, 1, 0).toNBT();
      legacy.removeTag("dg");
      legacy.removeTag("pp");
      TrafficSignalProgrammedPhase r = TrafficSignalProgrammedPhase.fromNBT(legacy);
      assertEquals(TrafficSignalProgrammedPhase.DEFAULT_DLY_GREEN, r.getDelayedGreen());
      assertEquals(0, r.getPermissivePhase());
    }
  }
}
