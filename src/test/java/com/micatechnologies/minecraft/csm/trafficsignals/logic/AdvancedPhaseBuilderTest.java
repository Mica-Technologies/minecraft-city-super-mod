package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.RingBarrierState.VehInterval;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  private static final BlockPos FYA_B = new BlockPos(2, 0, 0);
  private static final BlockPos ARROW_A = new BlockPos(3, 0, 0);

  private TrafficSignalPhase emptyPhase() {
    return new TrafficSignalPhase(1, TrafficSignalPhaseApplicability.NONE);
  }

  private List<BlockPos> list(BlockPos... p) {
    return new ArrayList<>(Arrays.asList(p));
  }

  @Nested
  @DisplayName("applyFyaLensState (PPLT FYA lens decision)")
  class FyaLensTest {

    @Test
    @DisplayName("protected GREEN: FYA lens off (solid green arrow shows via served movement)")
    void protectedGreen_lensOff() {
      TrafficSignalPhase phase = emptyPhase();
      AdvancedPhaseBuilder.applyFyaLensState(phase, list(FYA_A, FYA_B), list(ARROW_A),
          VehInterval.GREEN, false);
      assertTrue(phase.getOffSignals().contains(FYA_A));
      assertTrue(phase.getOffSignals().contains(FYA_B));
      assertFalse(phase.getFyaSignals().contains(FYA_A));
    }

    @Test
    @DisplayName("protected YELLOW clearance: FYA lens off")
    void protectedYellow_lensOff() {
      TrafficSignalPhase phase = emptyPhase();
      AdvancedPhaseBuilder.applyFyaLensState(phase, list(FYA_A), list(ARROW_A),
          VehInterval.YELLOW, false);
      assertTrue(phase.getOffSignals().contains(FYA_A));
    }

    @Test
    @DisplayName("not served + opposing through GREEN: flashing yellow, solid arrow red")
    void permissive_flashesYellow() {
      TrafficSignalPhase phase = emptyPhase();
      AdvancedPhaseBuilder.applyFyaLensState(phase, list(FYA_A), list(ARROW_A),
          null, true);
      assertTrue(phase.getFyaSignals().contains(FYA_A),
          "FYA lens should flash yellow (permissive) while the opposing through is green");
      assertTrue(phase.getRedSignals().contains(ARROW_A),
          "Solid green-arrow lens should be red during the permissive interval");
      assertFalse(phase.getOffSignals().contains(FYA_A));
    }

    @Test
    @DisplayName("not served + opposing through not green: FYA lens red")
    void notPermitted_red() {
      TrafficSignalPhase phase = emptyPhase();
      AdvancedPhaseBuilder.applyFyaLensState(phase, list(FYA_A), list(ARROW_A),
          null, false);
      assertTrue(phase.getRedSignals().contains(FYA_A));
      assertFalse(phase.getFyaSignals().contains(FYA_A));
    }

    @Test
    @DisplayName("own red clearance (served RED) is not 'protected': lens follows permissive/red")
    void servedRed_notProtected() {
      TrafficSignalPhase phase = emptyPhase();
      AdvancedPhaseBuilder.applyFyaLensState(phase, list(FYA_A), list(ARROW_A),
          VehInterval.RED, false);
      assertTrue(phase.getRedSignals().contains(FYA_A));
      assertFalse(phase.getOffSignals().contains(FYA_A));
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
      TrafficSignalProgrammedPhase r = TrafficSignalProgrammedPhase.fromNBT(p.toNBT());
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
