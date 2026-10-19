package com.micatechnologies.minecraft.csm.parks.planters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PlanterSoilTest {

  private static final Path PARKS = Paths.get("modules", "parks", "src", "main");

  @Test
  void aPlantIsDrawnAtThePlantersScale() {
    PlanterSoil soil = new PlanterSoil(14, 1.0F, null);
    assertEquals(1.0F, soil.scaleFor(12), 1e-6);
    assertEquals(1.0F, soil.scaleFor(16), 1e-6);
    PlanterSoil pot = new PlanterSoil(8, 0.45F, null);
    assertEquals(0.45F, pot.scaleFor(16), 1e-6);
  }

  @Test
  void aTallPlantIsCappedAtTheTopOfTheBlockAbove() {
    PlanterSoil soil = new PlanterSoil(14, 1.0F, null);
    float s = soil.scaleFor(28);    // the sunflower
    assertEquals(PlanterSoil.MAX_TOP, 14 + 28 * s, 1e-4);
    assertTrue(s < 1.0F);
  }

  @Test
  void onlyTheNamedTextureIsHidden() {
    PlanterSoil bowl = new PlanterSoil(8, 0.8F, "mixed_basket");
    assertTrue(bowl.hides("csm:blocks/parks/landscape/mixed_basket"));
    assertFalse(bowl.hides("csm:blocks/parks/landscape/planter_concrete"));
    assertFalse(bowl.hides("csm:blocks/parks/landscape/not_mixed_basket"));
    assertFalse(new PlanterSoil(14, 1, null).hides("csm:blocks/parks/landscape/mixed_basket"));
  }

  /**
   * Each planter's soil height, written into its tab line, is the top of an element of its own
   * model whose up face is drawn: the plant stands on the surface the player sees, neither
   * floating over it nor sunk into it.
   */
  @Test
  void eachPlantersSoilIsTheTopOfItsModel() throws IOException {
    String tab = new String(Files.readAllBytes(PARKS.resolve(Paths.get("java", "com",
        "micatechnologies", "minecraft", "csm", "tabs", "CsmTabTreesPlants.java"))),
        StandardCharsets.UTF_8);
    Matcher m = Pattern.compile(
        "new BlockPlanter\\(\"(\\w+)\", \\d+, \\d+, ([\\d.]+)F").matcher(tab);
    int found = 0;
    while (m.find()) {
      String name = m.group(1);
      float soil = Float.parseFloat(m.group(2));
      assertTrue(hasUpFaceAt(model(name), soil),
          name + ": no element of its model has a drawn top at its soil height " + soil);
      found++;
    }
    assertTrue(found >= 8, "found " + found + " planters");
    // The raised beds' soil is the post's top, the troughs' the middle's soil.
    for (String mat : new String[]{"concrete", "wood", "corten"}) {
      assertTrue(hasUpFaceAt(model("raised_bed_" + mat + "_post"), 10));
    }
    for (String mat : new String[]{"concrete", "corten"}) {
      assertTrue(hasUpFaceAt(model("planter_trough_" + mat + "_middle"), 9));
    }
  }

  private static JsonObject model(String name) throws IOException {
    Path path = PARKS.resolve(Paths.get("resources", "assets", "csm", "models", "block", "parks",
        "landscape", name + ".json"));
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }

  private static boolean hasUpFaceAt(JsonObject model, float y) {
    JsonArray elements = model.getAsJsonArray("elements");
    for (JsonElement e : elements) {
      JsonObject el = e.getAsJsonObject();
      float top = el.getAsJsonArray("to").get(1).getAsFloat();
      if (Math.abs(top - y) < 0.01F && el.getAsJsonObject("faces").has("up")) {
        return true;
      }
    }
    return false;
  }
}
