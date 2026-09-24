package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalAPSSoundSchemes} and {@link TrafficSignalAPSSoundScheme}
 * constants, getters, and sound length validation.
 */
class TrafficSignalAPSSoundSchemesTest {

  // region: Campbell schemes

  @Test
  void campbellArrayNotEmpty() {
    assertNotNull(TrafficSignalAPSSoundSchemes.CAMPBELL);
    assertTrue(TrafficSignalAPSSoundSchemes.CAMPBELL.length > 0);
  }

  @Test
  void campbellSchemesHaveValidNames() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.CAMPBELL) {
      assertNotNull(scheme.getName(), "Each Campbell scheme must have a name");
      assertFalse(scheme.getName().isEmpty(), "Campbell scheme name must not be empty");
    }
  }

  @Test
  void campbellSchemesHavePositiveLengths() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.CAMPBELL) {
      assertTrue(scheme.getLenOfWaitSound() > 0,
          "Wait sound length must be positive for: " + scheme.getName());
      assertTrue(scheme.getLenOfPressSound() > 0,
          "Press sound length must be positive for: " + scheme.getName());
      assertTrue(scheme.getLenOfWalkSound() > 0,
          "Walk sound length must be positive for: " + scheme.getName());
      assertTrue(scheme.getLenOfLocateSound() > 0,
          "Locate sound length must be positive for: " + scheme.getName());
    }
  }

  @Test
  void campbellSchemesHaveDefaultVolumeAndPitch() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.CAMPBELL) {
      assertEquals(1.0f, scheme.getVolume(), 0.001f,
          "Volume should be 1.0 for: " + scheme.getName());
      assertEquals(1.0f, scheme.getPitch(), 0.001f,
          "Pitch should be 1.0 for: " + scheme.getName());
    }
  }

  @Test
  void campbellDefaultLocateSoundLengthIs20() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.CAMPBELL) {
      assertEquals(20, scheme.getLenOfLocateSound(),
          "Default locate sound length should be 20 for: " + scheme.getName());
    }
  }

  /**
   * A placed button saves its scheme as an index into this list, so every scheme that has ever
   * shipped must keep its index: new schemes go after "Audio Disabled", never before it.
   */
  @Test
  void campbellShippedSchemesKeepTheirIndexes() {
    String[] shipped = {
        "Campbell Standard Voice - Walk Sign is On",
        "Campbell Standard Voice - Warning Lights are Flashing",
        "Campbell Standard Voice - Yellow Lights are Flashing",
        "Campbell Standard Voice - Walk Sign is On for All Crossings",
        "Campbell Standard Percussive (East-West)",
        "Campbell Standard Percussive (North-South)",
        "Campbell Phil Voice - Walk Sign is On",
        "Campbell Phil Voice - Warning Lights Activated",
        "Campbell Phil Voice - Crossing Lights Activated",
        "Campbell Phil Voice - Walk Sign is On for All Crossings",
        "Audio Disabled",
        "Campbell Canadian Melody",
        "Campbell Automated Walk Signal",
        "Campbell Ancient"};
    for (int i = 0; i < shipped.length; i++) {
      assertEquals(shipped[i], TrafficSignalAPSSoundSchemes.CAMPBELL[i].getName(),
          "Campbell scheme " + i + " moved; saved buttons would change sound");
    }
  }

  @Test
  void campbellAudioDisabledIsSilent() {
    TrafficSignalAPSSoundScheme disabled = TrafficSignalAPSSoundSchemes.CAMPBELL[10];
    assertEquals("Audio Disabled", disabled.getName());
    assertNull(disabled.getLocateSound());
    assertNull(disabled.getWaitSound());
    assertNull(disabled.getPressSound());
    assertNull(disabled.getWalkSound());
    assertNull(disabled.getClearanceSound());
  }

  // endregion

  // region: Polara schemes

  @Test
  void polaraArrayNotEmpty() {
    assertNotNull(TrafficSignalAPSSoundSchemes.POLARA);
    assertTrue(TrafficSignalAPSSoundSchemes.POLARA.length > 0);
  }

  @Test
  void polaraSchemesHaveValidNames() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.POLARA) {
      assertNotNull(scheme.getName(), "Each Polara scheme must have a name");
      assertFalse(scheme.getName().isEmpty(), "Polara scheme name must not be empty");
    }
  }

  @Test
  void polaraSchemesHavePositiveLengths() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.POLARA) {
      assertTrue(scheme.getLenOfWaitSound() > 0,
          "Wait sound length must be positive for: " + scheme.getName());
      assertTrue(scheme.getLenOfPressSound() > 0,
          "Press sound length must be positive for: " + scheme.getName());
      assertTrue(scheme.getLenOfWalkSound() > 0,
          "Walk sound length must be positive for: " + scheme.getName());
      assertTrue(scheme.getLenOfLocateSound() > 0,
          "Locate sound length must be positive for: " + scheme.getName());
    }
  }

  @Test
  void polaraSchemesHaveDefaultVolumeAndPitch() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.POLARA) {
      assertEquals(1.0f, scheme.getVolume(), 0.001f,
          "Volume should be 1.0 for: " + scheme.getName());
      assertEquals(1.0f, scheme.getPitch(), 0.001f,
          "Pitch should be 1.0 for: " + scheme.getName());
    }
  }

  /** As {@link #campbellShippedSchemesKeepTheirIndexes()}, for the Polara list. */
  @Test
  void polaraShippedSchemesKeepTheirIndexes() {
    String[] shipped = {
        "Polara Standard Rapid Tick",
        "Polara Voice - Walk Sign is On",
        "Polara Voice - Walk Sign is on for All Crossings",
        "Polara Spanish Standard Rapid Tick",
        "Polara Spanish Voice - Walk Sign is On",
        "Polara Spanish Voice - Walk Sign is on for All Crossings",
        "Audio Disabled",
        "Polara Automated Walk Signal",
        "Polara Meme"};
    for (int i = 0; i < shipped.length; i++) {
      assertEquals(shipped[i], TrafficSignalAPSSoundSchemes.POLARA[i].getName(),
          "Polara scheme " + i + " moved; saved buttons would change sound");
    }
  }

  @Test
  void polaraAudioDisabledIsSilent() {
    TrafficSignalAPSSoundScheme disabled = TrafficSignalAPSSoundSchemes.POLARA[6];
    assertEquals("Audio Disabled", disabled.getName());
    assertNull(disabled.getLocateSound());
    assertNull(disabled.getWaitSound());
    assertNull(disabled.getPressSound());
    assertNull(disabled.getWalkSound());
    assertNull(disabled.getClearanceSound());
  }

  // endregion

  // region: Clearance sounds and brand locate tones

  @Test
  void clearanceSoundHasARepeatLength() {
    for (TrafficSignalAPSSoundScheme[] list : new TrafficSignalAPSSoundScheme[][]{
        TrafficSignalAPSSoundSchemes.CAMPBELL, TrafficSignalAPSSoundSchemes.POLARA}) {
      for (TrafficSignalAPSSoundScheme scheme : list) {
        if (scheme.getClearanceSound() != null) {
          assertTrue(scheme.getLenOfClearanceSound() > 0,
              "Clearance sound needs a repeat length for: " + scheme.getName());
        }
      }
    }
  }

  @Test
  void canadianMelodyHurriesThroughClearance() {
    TrafficSignalAPSSoundScheme canadian = TrafficSignalAPSSoundSchemes.CAMPBELL[11];
    assertEquals("Campbell Canadian Melody", canadian.getName());
    assertNotNull(canadian.getClearanceSound());
    assertNotEquals(canadian.getWalkSound(), canadian.getClearanceSound());
  }

  /** Each brand's buttons keep their own locate tone, whatever voice a scheme borrows. */
  @Test
  void eachListUsesItsOwnBrandsLocateTone() {
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.CAMPBELL) {
      if (scheme.getLocateSound() != null) {
        assertTrue(scheme.getLocateSound().getSoundName().startsWith("campbell_"),
            "Campbell scheme with a non-Campbell locate tone: " + scheme.getName());
      }
    }
    for (TrafficSignalAPSSoundScheme scheme : TrafficSignalAPSSoundSchemes.POLARA) {
      if (scheme.getLocateSound() != null) {
        assertTrue(scheme.getLocateSound().getSoundName().startsWith("polara_"),
            "Polara scheme with a non-Polara locate tone: " + scheme.getName());
      }
    }
  }

  // endregion

  // region: Sound scheme getters

  @Test
  void campbellFirstSchemeGetters() {
    TrafficSignalAPSSoundScheme scheme = TrafficSignalAPSSoundSchemes.CAMPBELL[0];
    assertEquals("Campbell Standard Voice - Walk Sign is On", scheme.getName());
    assertNotNull(scheme.getLocateSound());
    assertNotNull(scheme.getWaitSound());
    assertNotNull(scheme.getPressSound());
    assertNotNull(scheme.getWalkSound());
    assertEquals(20, scheme.getLenOfWaitSound());
    assertEquals(20, scheme.getLenOfPressSound());
    assertEquals(80, scheme.getLenOfWalkSound());
  }

  @Test
  void polaraFirstSchemeGetters() {
    TrafficSignalAPSSoundScheme scheme = TrafficSignalAPSSoundSchemes.POLARA[0];
    assertEquals("Polara Standard Rapid Tick", scheme.getName());
    assertNotNull(scheme.getLocateSound());
    assertNotNull(scheme.getWaitSound());
    assertNotNull(scheme.getPressSound());
    assertNotNull(scheme.getWalkSound());
    assertEquals(20, scheme.getLenOfWaitSound());
    assertEquals(20, scheme.getLenOfPressSound());
    assertEquals(60, scheme.getLenOfWalkSound());
  }

  // endregion

  // region: Array sizes

  @Test
  void campbellHas14Schemes() {
    assertEquals(14, TrafficSignalAPSSoundSchemes.CAMPBELL.length);
  }

  @Test
  void polaraHas9Schemes() {
    assertEquals(9, TrafficSignalAPSSoundSchemes.POLARA.length);
  }

  // endregion

  // region: Unique names within each array

  @Test
  void campbellSchemesHaveUniqueNames() {
    TrafficSignalAPSSoundScheme[] schemes = TrafficSignalAPSSoundSchemes.CAMPBELL;
    for (int i = 0; i < schemes.length; i++) {
      for (int j = i + 1; j < schemes.length; j++) {
        assertNotEquals(schemes[i].getName(), schemes[j].getName(),
            "Campbell scheme names must be unique");
      }
    }
  }

  @Test
  void polaraSchemesHaveUniqueNames() {
    TrafficSignalAPSSoundScheme[] schemes = TrafficSignalAPSSoundSchemes.POLARA;
    for (int i = 0; i < schemes.length; i++) {
      for (int j = i + 1; j < schemes.length; j++) {
        assertNotEquals(schemes[i].getName(), schemes[j].getName(),
            "Polara scheme names must be unique");
      }
    }
  }

  // endregion
}
