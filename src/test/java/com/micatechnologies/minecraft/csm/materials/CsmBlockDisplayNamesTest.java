package com.micatechnologies.minecraft.csm.materials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Fabricator prices blocks by the nouns in their display names, and those names now come
 * from one {@code en_us.lang} per module jar rather than a single file. The test class path
 * holds every module's resources, so a name from Core, from Roads and from Building Materials
 * must all resolve; if the loader ever reads only the first copy it finds, one of these goes
 * missing and the affected blocks silently fall back to their tab's generic price.
 */
class CsmBlockDisplayNamesTest {

  @Test
  @DisplayName("names from Core's own lang file resolve")
  void coreNamesResolve() {
    assertNotNull(CsmBlockDisplayNames.get("csm_fabricator"));
  }

  @Test
  @DisplayName("names shipped by module jars resolve alongside Core's")
  void moduleNamesResolve() {
    assertNotNull(CsmBlockDisplayNames.get("trafficpolevertical"),
        "Roads & Traffic name missing: only one lang copy was read");
    assertNotNull(CsmBlockDisplayNames.get("blackmetal"),
        "Building Materials name missing: only one lang copy was read");
    assertNotNull(CsmBlockDisplayNames.get("afei"),
        "Power Grid name missing: only one lang copy was read");
  }

  @Test
  @DisplayName("a pole is still recognised as a pole by its last word")
  void poleNounSurvivesTheSplit() {
    assertEquals("pole", CsmBlockDisplayNames.lastWord("trafficpolevertical"));
  }
}
