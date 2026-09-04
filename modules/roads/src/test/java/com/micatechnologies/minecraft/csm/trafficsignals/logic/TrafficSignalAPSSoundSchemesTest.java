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

  @Test
  void campbellLastSchemeIsAudioDisabled() {
    TrafficSignalAPSSoundScheme last =
        TrafficSignalAPSSoundSchemes.CAMPBELL[TrafficSignalAPSSoundSchemes.CAMPBELL.length - 1];
    assertEquals("Audio Disabled", last.getName());
    assertNull(last.getLocateSound());
    assertNull(last.getWaitSound());
    assertNull(last.getPressSound());
    assertNull(last.getWalkSound());
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

  @Test
  void polaraLastSchemeIsAudioDisabled() {
    TrafficSignalAPSSoundScheme last =
        TrafficSignalAPSSoundSchemes.POLARA[TrafficSignalAPSSoundSchemes.POLARA.length - 1];
    assertEquals("Audio Disabled", last.getName());
    assertNull(last.getLocateSound());
    assertNull(last.getWaitSound());
    assertNull(last.getPressSound());
    assertNull(last.getWalkSound());
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
  void campbellHas11Schemes() {
    assertEquals(11, TrafficSignalAPSSoundSchemes.CAMPBELL.length);
  }

  @Test
  void polaraHas7Schemes() {
    assertEquals(7, TrafficSignalAPSSoundSchemes.POLARA.length);
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
