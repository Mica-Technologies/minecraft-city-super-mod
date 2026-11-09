package com.micatechnologies.minecraft.csm.trafficaccessories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The pole-side preemption devices (#338): placement takes the clicked face of the pole, and the
 * confirmation lights' glow boxes ({@code *_POLE_LENS_*}) sit on the lenses
 * {@code gen_preempt_detector.py} draws, so the beacon renderer's glow lands on the lamp.
 */
class PreemptPoleMountTest {

  @Test
  @DisplayName("a pole-side device faces the way the clicked face of the pole points")
  void placementTakesTheClickedFace() {
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      assertEquals(f, IPreemptPoleMounted.placementFacing(f, null));
    }
  }

  private static JsonObject model(String name) throws Exception {
    File f = new File("modules/roads/src/main/resources/assets/csm/models/block/trafficsignals/"
        + "shared_models/" + name + ".json");
    return new JsonParser().parse(new String(Files.readAllBytes(f.toPath()),
        StandardCharsets.UTF_8)).getAsJsonObject();
  }

  /** The bounds of the model's lens-textured elements: {minX, minY, minZ, maxX, maxY, maxZ}. */
  private static float[] lensBounds(JsonObject model) {
    float[] b = {99, 99, 99, -99, -99, -99};
    for (JsonElement e : model.getAsJsonArray("elements")) {
      JsonObject el = e.getAsJsonObject();
      boolean lens = false;
      for (java.util.Map.Entry<String, JsonElement> face
          : el.getAsJsonObject("faces").entrySet()) {
        lens |= "#lens".equals(face.getValue().getAsJsonObject().get("texture").getAsString());
      }
      if (!lens || el.has("rotation")) {
        continue; // the straight boxes bound the lens; the turned ones sit inside them
      }
      JsonArray from = el.getAsJsonArray("from");
      JsonArray to = el.getAsJsonArray("to");
      for (int i = 0; i < 3; i++) {
        b[i] = Math.min(b[i], from.get(i).getAsFloat());
        b[i + 3] = Math.max(b[i + 3], to.get(i).getAsFloat());
      }
    }
    return b;
  }

  private static void assertGlowOnLens(String name, float[] from, float[] to) throws Exception {
    float[] lens = lensBounds(model(name));
    for (int i = 0; i < 3; i++) {
      assertTrue(from[i] >= lens[i] - 0.2f && to[i] <= lens[i + 3] + 0.2f,
          name + ": glow box axis " + i + " [" + from[i] + ", " + to[i] + "] is off the lens ["
              + lens[i] + ", " + lens[i + 3] + "]");
    }
  }

  @Test
  @DisplayName("the pole-side lights' glow boxes sit on their drawn lenses")
  void glowBoxesMatchTheModels() throws Exception {
    assertGlowOnLens("preempt_confirm_par_white_pole",
        BlockPreemptConfirmationLight.PAR_POLE_LENS_FROM,
        BlockPreemptConfirmationLight.PAR_POLE_LENS_TO);
    assertGlowOnLens("preempt_confirm_par_blue_pole",
        BlockPreemptConfirmationLight.PAR_POLE_LENS_FROM,
        BlockPreemptConfirmationLight.PAR_POLE_LENS_TO);
    assertGlowOnLens("preempt_confirm_dome_red_pole",
        BlockPreemptConfirmationLight.DOME_POLE_LENS_FROM,
        BlockPreemptConfirmationLight.DOME_POLE_LENS_TO);
  }
}
