package com.micatechnologies.minecraft.csm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CsmSoundsTest {

  @Test
  void allSoundValuesHaveUniqueSoundNames() {
    Set<String> names = new HashSet<>();
    for (CsmSounds.SOUND sound : CsmSounds.SOUND.values()) {
      assertTrue(names.add(sound.soundName),
          "Duplicate sound name: " + sound.soundName + " for " + sound);
    }
  }

  @ParameterizedTest
  @EnumSource(CsmSounds.SOUND.class)
  void soundNameIsNonNullAndNonEmpty(CsmSounds.SOUND sound) {
    assertNotNull(sound.soundName, "soundName should not be null for " + sound);
    assertFalse(sound.soundName.isEmpty(), "soundName should not be empty for " + sound);
  }

  @ParameterizedTest
  @EnumSource(CsmSounds.SOUND.class)
  void soundLocationIsNonNull(CsmSounds.SOUND sound) {
    assertNotNull(sound.getSoundLocation(), "getSoundLocation() should not be null for " + sound);
  }

  @ParameterizedTest
  @EnumSource(CsmSounds.SOUND.class)
  void soundLocationContainsModNamespace(CsmSounds.SOUND sound) {
    var location = sound.getSoundLocation();
    assertEquals("csm", location.getNamespace(),
        "Sound location namespace should be 'csm' for " + sound);
  }

  @ParameterizedTest
  @EnumSource(CsmSounds.SOUND.class)
  void soundLocationPathMatchesSoundName(CsmSounds.SOUND sound) {
    var location = sound.getSoundLocation();
    // ResourceLocation lowercases the path, so compare case-insensitively
    assertEquals(sound.soundName.toLowerCase(), location.getPath(),
        "Sound location path should match soundName (lowercased) for " + sound);
  }

  @Test
  void atLeastOneSoundExists() {
    assertTrue(CsmSounds.SOUND.values().length > 0, "There should be at least one sound");
  }
}
