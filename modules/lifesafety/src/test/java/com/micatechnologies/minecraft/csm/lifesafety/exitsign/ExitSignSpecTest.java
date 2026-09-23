package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Faces;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExitSignSpecTest {

  /**
   * The most states an exit sign may have: the traditional signs' own count since they gained
   * single-faced hung mounts, a little past the frame scaffold's 5,184.
   */
  private static final int STATE_BUDGET = 5376;

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
    assertEquals(5376, BlockExitSignTraditionalFlat.SPEC.stateCount());
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
    assertEquals(1344, BlockExitSignDieCast.SPEC.stateCount());
    assertEquals(192, BlockExitSignExplosionProof.SPEC.stateCount());
  }

  @Test
  void aHungSignCanShowOneFaceWhereverItHangs() {
    for (ExitSignSpec spec : ALL) {
      assertTrue(spec.offersSingleFaced(), "every sign can hang single-faced from the ceiling");
      for (Mount place : spec.getMountPlaces()) {
        assertFalse(place.isSingleFaced());
        Mount single = spec.mountAt(place, Faces.SINGLE);
        assertEquals(place, single.getPlace(), "faces never move a sign");
        assertEquals(place.isHung(), single.isSingleFaced(),
            place + ": single-faced only where it hangs");
        assertEquals(place, spec.mountAt(single.getPlace(), Faces.DOUBLE));
      }
    }
    assertEquals(Arrays.asList(Mount.WALL, Mount.CEILING),
        BlockExitSignVandalResistant.SPEC.getMountPlaces());
  }

  @Test
  void facesAreReadFromTheMount() {
    assertEquals(Faces.SINGLE, Mount.END_RIGHT_SINGLE.getFaces());
    assertEquals(Mount.END_RIGHT, Mount.END_RIGHT_SINGLE.getPlace());
    assertEquals(Mount.CEILING_SINGLE, Mount.CEILING.withFaces(Faces.SINGLE));
    assertEquals(Mount.WALL, Mount.WALL.withFaces(Faces.SINGLE), "a wall already has one face");
    // Appended, never reordered: these ordinals are what every placed sign saved.
    assertEquals(3, Mount.END_RIGHT.ordinal());
    assertEquals(6, Mount.END_RIGHT_SINGLE.ordinal());
  }

  @Test
  void aSingleFacedSetupSurvivesThePacket() {
    ExitSignConfig config = BlockExitSignTraditionalFlat.SPEC.getDefaults()
        .withMount(Mount.END_LEFT_SINGLE);
    assertEquals(config, ExitSignConfig.unpack(config.pack()));
  }
}
