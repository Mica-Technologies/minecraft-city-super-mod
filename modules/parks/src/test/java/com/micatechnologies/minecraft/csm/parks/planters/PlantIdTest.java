package com.micatechnologies.minecraft.csm.parks.planters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlantIdTest {

  @Test
  void aBareNameIsCsms() {
    assertEquals(new PlantId("csm", "shrub_lavender", 0), PlantId.parse("shrub_lavender"));
    assertEquals(new PlantId("csm", "shrub_lavender", 0), PlantId.parse("csm:shrub_lavender"));
  }

  @Test
  void theVariantFollowsTheName() {
    assertEquals(new PlantId("minecraft", "red_flower", 3), PlantId.parse("minecraft:red_flower:3"));
    assertEquals(new PlantId("csm", "flower_tulips", 2), PlantId.parse("flower_tulips:2"));
    assertEquals("minecraft:red_flower:3", PlantId.parse("Minecraft:Red_Flower:3").toString());
    assertEquals("csm:shrub_rose", PlantId.parse("shrub_rose:0").toString());
  }

  @Test
  void nonsenseIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> PlantId.parse(""));
    assertThrows(IllegalArgumentException.class, () -> PlantId.parse("a:b:c"));
    assertThrows(IllegalArgumentException.class, () -> PlantId.parse("a:b:c:d"));
    assertThrows(IllegalArgumentException.class, () -> PlantId.parse("minecraft:red_flower:16"));
    assertThrows(IllegalArgumentException.class, () -> PlantId.parse("minecraft:red flower"));
    assertThrows(IllegalArgumentException.class, () -> PlantId.parse(":x"));
  }

  @Test
  void metaIsClamped() {
    assertEquals(15, new PlantId("csm", "x", 99).getMeta());
    assertEquals(0, new PlantId("csm", "x", -4).getMeta());
  }

  @Test
  void pottedPlantsNameThePlantInThem() {
    assertEquals("shrub_lavender", PlanterPlants.unpotted("potted_shrub_lavender"));
    assertEquals("cactus_saguaro", PlanterPlants.unpotted("potted_cactus_saguaro"));
    assertNull(PlanterPlants.unpotted("shrub_lavender"));
    assertNull(PlanterPlants.unpotted("potted_"));
  }
}
