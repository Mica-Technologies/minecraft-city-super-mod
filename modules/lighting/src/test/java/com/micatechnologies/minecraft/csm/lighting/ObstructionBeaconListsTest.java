package com.micatechnologies.minecraft.csm.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The beacon's glow is compiled once per style and part and replayed with the flash as the blend's
 * constant alpha. These hold the keys apart and the premultiplied colours to what the live draw
 * put on the screen.
 */
class ObstructionBeaconListsTest {

  private static final TileEntityObstructionBeaconRenderer.Style[] STYLES = {
      TileEntityObstructionBeaconRenderer.RED, TileEntityObstructionBeaconRenderer.WHITE_STROBE};

  @Test
  void everyStyleAndPartHasItsOwnKey() {
    Set<Long> keys = new HashSet<>();
    for (TileEntityObstructionBeaconRenderer.Style s : STYLES) {
      for (int part = 0; part < 4; part++) {
        assertTrue(keys.add(TileEntityObstructionBeaconRenderer.listKey(s.index, part)));
      }
    }
    assertEquals(8, keys.size());
  }

  /**
   * Live, a ring vertex was drawn at alpha {@code flash * ringAlpha} with {@code SRC_ALPHA, ONE},
   * adding {@code colour * flash * ringAlpha}. Shared, it is drawn premultiplied with
   * {@code CONSTANT_ALPHA, ONE} and the flash as the constant: the same sum.
   */
  @Test
  void premultipliedRingsAddWhatTheLiveDrawAdded() {
    float[] whites = {1f, 0.8f, 0.5f, 0.35f, 0f};
    float[] ringAlphas = {1f, 0.8f, 0.6f, 0.32f, 0.3f, 0.1f, 0f};
    float[] flashes = {0.05f, 0.4f, 0.73f, 1f};
    for (TileEntityObstructionBeaconRenderer.Style s : STYLES) {
      for (float white : whites) {
        float[] colour = TileEntityObstructionBeaconRenderer.ringColour(s, white);
        for (float ringAlpha : ringAlphas) {
          float[] shared = TileEntityObstructionBeaconRenderer.premultiplied(s, white, ringAlpha);
          for (float flash : flashes) {
            for (int c = 0; c < 3; c++) {
              float live = colour[c] * (flash * ringAlpha);
              assertEquals(live, shared[c] * flash, 1e-6f);
            }
          }
        }
      }
    }
  }

  @Test
  void theFlashCadenceIsUnchanged() {
    TileEntityObstructionBeaconRenderer.Style red = TileEntityObstructionBeaconRenderer.RED;
    assertEquals(0f, red.intensity(0), 0f);
    assertEquals(0.5f, red.intensity(50), 1e-6f);
    assertEquals(1f, red.intensity(400), 0f);
    assertEquals(0.5f, red.intensity(850), 1e-6f);
    assertEquals(0f, red.intensity(1500), 0f);
    TileEntityObstructionBeaconRenderer.Style white =
        TileEntityObstructionBeaconRenderer.WHITE_STROBE;
    assertEquals(1f, white.intensity(1500 * 7 + 50), 0f);
    assertEquals(0f, white.intensity(1500 * 7 + 200), 0f);
  }
}
