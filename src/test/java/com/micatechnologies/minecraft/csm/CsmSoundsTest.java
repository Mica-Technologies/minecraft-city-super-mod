package com.micatechnologies.minecraft.csm;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.hvac.HvacSounds;
import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import com.micatechnologies.minecraft.csm.technology.TechnologySounds;
import com.micatechnologies.minecraft.csm.trafficsignals.RoadsSounds;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the per-module sound enums. Every sound in the mod belongs to exactly one module, and
 * the union of the module enums has to be exactly the set of keys in {@code sounds.json} — a
 * sound in an enum but not in {@code sounds.json} registers an event with nothing to play, and
 * a sound in {@code sounds.json} that no module claims ships an {@code .ogg} that no jar
 * registers.
 */
class CsmSoundsTest {

  /**
   * The sound enums, by the module that owns them. Add a module's enum here when it gains one.
   */
  private static final Map<String, ICsmSound[]> MODULE_SOUNDS = new LinkedHashMap<>();

  static {
    MODULE_SOUNDS.put("Roads & Traffic", RoadsSounds.values());
    MODULE_SOUNDS.put("Life Safety", LifeSafetySounds.values());
    MODULE_SOUNDS.put("Furniture & Novelties", FurnishingsSounds.values());
    MODULE_SOUNDS.put("Technology", TechnologySounds.values());
    MODULE_SOUNDS.put("HVAC", HvacSounds.values());
  }

  /**
   * Provides every sound of every module, for the per-sound parameterized tests.
   *
   * @return every sound in the mod
   */
  static Stream<ICsmSound> allSounds() {
    List<ICsmSound> sounds = new ArrayList<>();
    for (ICsmSound[] moduleSounds : MODULE_SOUNDS.values()) {
      for (ICsmSound sound : moduleSounds) {
        sounds.add(sound);
      }
    }
    return sounds.stream();
  }

  /**
   * Reads the keys of {@code sounds.json}, which is the list of sounds the mod actually ships.
   *
   * @return the sound names defined in {@code sounds.json}
   *
   * @throws IOException if the file cannot be read
   */
  private static Set<String> readSoundsJsonKeys() throws IOException {
    try (InputStream stream =
             CsmSoundsTest.class.getResourceAsStream("/assets/csm/sounds.json")) {
      assertNotNull(stream, "assets/csm/sounds.json should be on the test classpath");
      try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        // Minecraft 1.12.2 ships Gson 2.2.4, which has neither JsonParser.parseReader nor
        // JsonObject.keySet.
        JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
          keys.add(entry.getKey());
        }
        return keys;
      }
    }
  }

  @Test
  void everySoundIsOwnedByExactlyOneModule() {
    Map<String, String> ownerByName = new LinkedHashMap<>();
    for (Map.Entry<String, ICsmSound[]> module : MODULE_SOUNDS.entrySet()) {
      for (ICsmSound sound : module.getValue()) {
        String previousOwner = ownerByName.put(sound.getSoundName(), module.getKey());
        assertNull(previousOwner, "Sound " + sound.getSoundName() + " is claimed by both "
            + previousOwner + " and " + module.getKey());
      }
    }
  }

  @Test
  void moduleSoundsCoverEverySoundInSoundsJson() throws IOException {
    Set<String> declared = new LinkedHashSet<>();
    for (ICsmSound[] moduleSounds : MODULE_SOUNDS.values()) {
      for (ICsmSound sound : moduleSounds) {
        declared.add(sound.getSoundName());
      }
    }

    Set<String> defined = readSoundsJsonKeys();

    Set<String> unclaimed = new LinkedHashSet<>(defined);
    unclaimed.removeAll(declared);
    assertTrue(unclaimed.isEmpty(),
        "sounds.json defines sounds that no module claims: " + unclaimed);

    Set<String> undefined = new LinkedHashSet<>(declared);
    undefined.removeAll(defined);
    assertTrue(undefined.isEmpty(),
        "Modules claim sounds that sounds.json does not define: " + undefined);
  }

  @Test
  void allSoundValuesHaveUniqueSoundNames() {
    Set<String> names = new HashSet<>();
    allSounds().forEach(sound -> assertTrue(names.add(sound.getSoundName()),
        "Duplicate sound name: " + sound.getSoundName() + " for " + sound));
  }

  @ParameterizedTest
  @MethodSource("allSounds")
  void soundNameIsNonNullAndNonEmpty(ICsmSound sound) {
    assertNotNull(sound.getSoundName(), "soundName should not be null for " + sound);
    assertFalse(sound.getSoundName().isEmpty(), "soundName should not be empty for " + sound);
  }

  @ParameterizedTest
  @MethodSource("allSounds")
  void soundLocationIsNonNull(ICsmSound sound) {
    assertNotNull(sound.getSoundLocation(), "getSoundLocation() should not be null for " + sound);
  }

  @ParameterizedTest
  @MethodSource("allSounds")
  void soundLocationContainsModNamespace(ICsmSound sound) {
    var location = sound.getSoundLocation();
    assertEquals("csm", location.getNamespace(),
        "Sound location namespace should be 'csm' for " + sound);
  }

  @ParameterizedTest
  @MethodSource("allSounds")
  void soundLocationPathMatchesSoundName(ICsmSound sound) {
    var location = sound.getSoundLocation();
    // ResourceLocation lowercases the path, so compare case-insensitively
    assertEquals(sound.getSoundName().toLowerCase(), location.getPath(),
        "Sound location path should match soundName (lowercased) for " + sound);
  }

  @Test
  void atLeastOneSoundExists() {
    assertTrue(allSounds().findAny().isPresent(), "There should be at least one sound");
  }
}
