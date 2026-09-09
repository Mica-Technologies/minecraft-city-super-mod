package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightLens;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightLinkMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightPattern;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class InRoadwayWarningLightTest {

  @Test
  void aBeaconFixtureIsLitOnEverythingButOff() {
    // Matching the RRFB exactly is the point: a fixture on the same circuit as one has to
    // flash whenever it does, and the RRFB is dark only on the controller's OFF state.
    for (int color : new int[] {AbstractBlockControllableSignal.SIGNAL_RED,
        AbstractBlockControllableSignal.SIGNAL_YELLOW,
        AbstractBlockControllableSignal.SIGNAL_GREEN}) {
      assertTrue(BlockInRoadwayWarningLight.isLit(color, InRoadwayLightLinkMode.BEACON),
          "colour " + color);
    }
    assertFalse(BlockInRoadwayWarningLight.isLit(
        AbstractBlockControllableSignal.SIGNAL_OFF, InRoadwayLightLinkMode.BEACON));
  }

  @Test
  void aCrosswalkFixtureIsDarkOnDontWalk() {
    // The reading that makes the two modes irreconcilable: RED is a called beacon but it is
    // also don't-walk, and a pavement light that stayed lit through don't-walk would be
    // telling drivers to stop for nobody.
    assertTrue(BlockInRoadwayWarningLight.isLit(
        AbstractBlockControllableSignal.SIGNAL_GREEN, InRoadwayLightLinkMode.CROSSWALK),
        "walk");
    assertTrue(BlockInRoadwayWarningLight.isLit(
        AbstractBlockControllableSignal.SIGNAL_YELLOW, InRoadwayLightLinkMode.CROSSWALK),
        "pedestrian clearance");
    assertFalse(BlockInRoadwayWarningLight.isLit(
        AbstractBlockControllableSignal.SIGNAL_RED, InRoadwayLightLinkMode.CROSSWALK),
        "don't walk");
    assertFalse(BlockInRoadwayWarningLight.isLit(
        AbstractBlockControllableSignal.SIGNAL_OFF, InRoadwayLightLinkMode.CROSSWALK));
  }

  @Test
  void everyPatternHasTwoDistinctPhases() {
    // The alternation across a row rests on these being different values; if any pattern
    // mapped both phases to one lens, that row would fire in unison without saying so.
    for (InRoadwayLightPattern pattern : InRoadwayLightPattern.values()) {
      assertNotEquals(InRoadwayLightLens.of(pattern, false),
          InRoadwayLightLens.of(pattern, true), pattern.name());
    }
  }

  @Test
  void everyLensValueIsReachableAndNamedForItsTexture() {
    // The blockstate keys on these names, so a value that no pattern produces is a texture
    // nothing ever shows, and a name that does not match its file is a missing model.
    for (InRoadwayLightPattern pattern : InRoadwayLightPattern.values()) {
      for (boolean offbeat : new boolean[] {false, true}) {
        InRoadwayLightLens lens = InRoadwayLightLens.of(pattern, offbeat);
        assertEquals(pattern.getName() + (offbeat ? "_b" : "_a"), lens.getName());
      }
    }
    assertEquals("off", InRoadwayLightLens.OFF.getName());
  }

  @Test
  void anUnknownPatternFallsBackRatherThanThrowing() {
    // Both reach this from NBT, so a hand-edited or corrupt value must not crash a chunk load.
    assertEquals(InRoadwayLightPattern.RRFB, InRoadwayLightPattern.fromNBT(-1));
    assertEquals(InRoadwayLightPattern.RRFB, InRoadwayLightPattern.fromNBT(99));
    assertEquals(InRoadwayLightLinkMode.BEACON, InRoadwayLightLinkMode.fromNBT(-1));
    assertEquals(InRoadwayLightLinkMode.BEACON, InRoadwayLightLinkMode.fromNBT(99));
    assertEquals(InRoadwayLightLens.RRFB_A, InRoadwayLightLens.of(null, false));
  }

  @Test
  void nbtRoundTripsBothSettingsAndDefaultsToABeaconOnTheRrfbSequence() {
    TileEntityInRoadwayWarningLight fresh = new TileEntityInRoadwayWarningLight();
    fresh.readNBT(new NBTTagCompound());
    assertEquals(InRoadwayLightLinkMode.BEACON, fresh.getLinkMode());
    assertEquals(InRoadwayLightPattern.RRFB, fresh.getPattern());

    TileEntityInRoadwayWarningLight configured = new TileEntityInRoadwayWarningLight();
    configured.setLinkMode(InRoadwayLightLinkMode.CROSSWALK);
    configured.setPattern(InRoadwayLightPattern.WIG_WAG);

    TileEntityInRoadwayWarningLight restored = new TileEntityInRoadwayWarningLight();
    restored.readNBT(configured.writeNBT(new NBTTagCompound()));
    assertEquals(InRoadwayLightLinkMode.CROSSWALK, restored.getLinkMode());
    assertEquals(InRoadwayLightPattern.WIG_WAG, restored.getPattern());
  }

  @Test
  void cyclingWrapsAllTheWayRoundBothSettings() {
    // The GUI only steps forward, so every value has to be reachable that way.
    InRoadwayLightPattern pattern = InRoadwayLightPattern.RRFB;
    for (int i = 0; i < InRoadwayLightPattern.values().length; i++) {
      pattern = pattern.getNext();
    }
    assertEquals(InRoadwayLightPattern.RRFB, pattern);

    InRoadwayLightLinkMode mode = InRoadwayLightLinkMode.BEACON;
    for (int i = 0; i < InRoadwayLightLinkMode.values().length; i++) {
      mode = mode.getNext();
    }
    assertEquals(InRoadwayLightLinkMode.BEACON, mode);
  }
}
