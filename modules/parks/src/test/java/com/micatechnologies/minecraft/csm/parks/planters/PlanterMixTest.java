package com.micatechnologies.minecraft.csm.parks.planters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanterMixTest {

  @Test
  void theSameBoxAndSeedPlantTheSameBed() {
    for (int x = -20; x < 20; x++) {
      for (int z = -20; z < 20; z++) {
        assertEquals(PlanterMix.pick(x, 64, z, 7L), PlanterMix.pick(x, 64, z, 7L));
      }
    }
  }

  @Test
  void anotherSeedPlantsAnotherBed() {
    int differ = 0;
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        if (!PlanterMix.pick(x, 70, z, 1L).equals(PlanterMix.pick(x, 70, z, 2L))) {
          differ++;
        }
      }
    }
    assertTrue(differ > 200, "a different seed changes most of a bed, changed " + differ);
  }

  @Test
  void aBedUsesTheWholeListAndNoPlantDominates() {
    Map<String, Integer> counts = new HashMap<>();
    int n = 0;
    for (int x = 0; x < 40; x++) {
      for (int z = 0; z < 40; z++) {
        counts.merge(PlanterMix.pick(x, 64, z, 0L), 1, Integer::sum);
        n++;
      }
    }
    assertEquals(PlanterMix.PLANTS.size(), counts.size(), "every plant appears: " + counts);
    int fair = n / PlanterMix.PLANTS.size();
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      assertTrue(e.getValue() > fair / 2 && e.getValue() < fair * 2,
          e.getKey() + " appears " + e.getValue() + " times of " + n);
    }
  }

  @Test
  void neighboursAreNotAllTheSame() {
    Set<String> row = new HashSet<>();
    for (int x = 0; x < 8; x++) {
      row.add(PlanterMix.pick(x, 64, 0, 0L));
    }
    assertTrue(row.size() >= 4, "a row of eight planters has some variety: " + row);
  }

  @Test
  void everyPlantInTheMixIsRegistered() throws IOException {
    Path tab = Paths.get("modules", "parks", "src", "main", "java", "com", "micatechnologies",
        "minecraft", "csm", "tabs", "CsmTabTreesPlants.java");
    String source = new String(Files.readAllBytes(tab), StandardCharsets.UTF_8);
    for (String plant : PlanterMix.PLANTS) {
      assertTrue(source.contains("new BlockParkProp(\"" + plant + "\""),
          plant + " is not a planting registered in the Trees & Plants tab");
    }
  }
}
