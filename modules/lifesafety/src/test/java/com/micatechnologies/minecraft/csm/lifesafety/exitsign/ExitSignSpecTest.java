package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExitSignSpecTest {

  /** The most states any block in the mod has (the frame scaffold). */
  private static final int STATE_BUDGET = 5184;

  private static final List<ExitSignSpec> ALL = Arrays.asList(
      BlockExitSignTraditionalFlat.SPEC, BlockExitSignTraditionalRounded.SPEC,
      BlockExitSignCombo.SPEC, BlockExitSignDieCast.SPEC, BlockExitSignVandalResistant.SPEC,
      BlockExitSignPhotoluminescent.SPEC, BlockExitSignExplosionProof.SPEC);

  @Test
  void everySignFitsTheStateBudget() {
    for (ExitSignSpec spec : ALL) {
      assertTrue(spec.stateCount() <= STATE_BUDGET,
          "an exit sign has " + spec.stateCount() + " states");
    }
    assertEquals(3072, BlockExitSignTraditionalFlat.SPEC.stateCount());
  }

  @Test
  void presetsAreOnlyWhatTheSignOffers() {
    for (ExitSignSpec spec : ALL) {
      assertFalse(spec.getPresets().isEmpty());
      for (ExitSignConfig preset : spec.getPresets()) {
        assertEquals(preset, spec.clamp(preset));
      }
    }
  }

  @Test
  void clampReplacesWhatTheSignDoesNotOffer() {
    ExitSignSpec combo = BlockExitSignCombo.SPEC;
    ExitSignConfig clamped = combo.clamp(combo.getDefaults().withHeads(Heads.NONE));
    assertEquals(Heads.SQUARE, clamped.getHeads());

    ExitSignSpec vandal = BlockExitSignVandalResistant.SPEC;
    assertEquals(Mount.WALL, vandal.clamp(vandal.getDefaults().withMount(Mount.END_LEFT))
        .getMount());

    ExitSignSpec explosionProof = BlockExitSignExplosionProof.SPEC;
    assertEquals(Housing.BRUSHED, explosionProof.clamp(
        explosionProof.getDefaults().withHousing(Housing.WHITE)).getHousing());
  }

  @Test
  void singleValuedOptionsHaveNoProperty() {
    // explosion-proof: one housing, two mounts, one heads -> arrow, letters, mount, legend
    assertEquals(4, BlockExitSignExplosionProof.SPEC.properties().size());
    assertFalse(BlockExitSignPhotoluminescent.SPEC.isMainsPowered());
  }

  @Test
  void onlySignsThatCanHaveHeadsTakeMainsPower() {
    assertTrue(BlockExitSignTraditionalFlat.SPEC.isMainsPowered());
    assertTrue(BlockExitSignCombo.SPEC.isMainsPowered());
    assertFalse(BlockExitSignDieCast.SPEC.isMainsPowered());
    assertFalse(BlockExitSignVandalResistant.SPEC.isMainsPowered());
    assertFalse(BlockExitSignExplosionProof.SPEC.isMainsPowered());
    assertEquals(768, BlockExitSignDieCast.SPEC.stateCount());
    assertEquals(128, BlockExitSignExplosionProof.SPEC.stateCount());
  }
}
