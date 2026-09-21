package com.micatechnologies.minecraft.csm.buildingmaterials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Which way a door swings: the upper half's bits, the effective direction (the kind's default,
 * flipped if the door is reversed), and the blockstates {@code gen_doors.py} writes, which must
 * draw exactly one leaf for every state and hang it on the face of the cell the collision box
 * says, with the closer on the side it swings away from.
 */
class DoorSwingDirectionTest {

  /** SHARED with gen_doors.DOORS: every fixed door's registry name. */
  private static final String[] DOORS = {"door_wood_oak", "door_wood_oak_lite",
      "door_wood_white", "door_wood_white_lite", "door_metal_grey", "door_metal_fire",
      "door_metal_exit", "door_storefront_bronze", "door_front_white", "door_front_red",
      "door_front_black", "door_back_halfglass"};
  private static final String[] FACINGS = {"north", "east", "south", "west"};

  @Test
  void theUpperHalfsBitsGoRoundTrip() {
    for (int bits = 0; bits < 8; bits++) {
      boolean right = (bits & 1) != 0;
      boolean closer = (bits & 2) != 0;
      boolean reversed = (bits & 4) != 0;
      int meta = DoorSwingDirection.upperMeta(right, closer, reversed);
      assertTrue(meta >= 8 && meta < 16, "an upper half's meta is 8..15");
      assertTrue(DoorSwingDirection.isUpper(meta));
      assertEquals(right, DoorSwingDirection.hingeRight(meta));
      assertEquals(closer, DoorSwingDirection.closer(meta));
      assertEquals(reversed, DoorSwingDirection.reversed(meta));
    }
    // Reversed is the bit that said "swinging" before: an old door at rest reads unreversed.
    assertEquals(4, DoorSwingDirection.REVERSED);
    for (int lower = 0; lower < 8; lower++) {
      assertFalse(DoorSwingDirection.isUpper(lower));
    }
  }

  @Test
  void aReversedDoorSwingsTheOtherWayFromItsKind() {
    assertFalse(DoorSwingDirection.outward(false, false), "an interior door swings in");
    assertTrue(DoorSwingDirection.outward(false, true), "reversed, it swings out");
    assertTrue(DoorSwingDirection.outward(true, false), "an exit door swings out");
    assertFalse(DoorSwingDirection.outward(true, true), "reversed, it swings in");
    assertTrue(DoorSwingDirection.outswingKind("door_metal_exit"));
    assertTrue(DoorSwingDirection.outswingKind("door_metal_fire"));
    assertTrue(DoorSwingDirection.outswingKind("door_storefront_bronze"));
    assertFalse(DoorSwingDirection.outswingKind("door_wood_oak"));
    assertFalse(DoorSwingDirection.outswingKind("custom_door"));
  }

  @Test
  void everyStateDrawsOneLeafOnTheRightFaceAndTheCloserOnThePushSide() throws IOException {
    for (String door : DOORS) {
      JsonObject state = read("assets/csm/blockstates/" + door + ".json");
      List<JsonObject> parts = new ArrayList<>();
      for (JsonElement el : state.getAsJsonArray("multipart")) {
        parts.add(el.getAsJsonObject());
      }
      for (String facing : FACINGS) {
        for (String half : new String[]{"lower", "upper"}) {
          for (String hinge : new String[]{"left", "right"}) {
            for (boolean open : new boolean[]{false, true}) {
              for (boolean reversed : new boolean[]{false, true}) {
                boolean out = DoorSwingDirection.outward(DoorSwingDirection.outswingKind(door),
                    reversed);
                String what = door + " " + facing + " " + half + " " + hinge
                    + (open ? " open" : "") + (reversed ? " reversed" : "");
                List<String> leaf = models(parts, facing, half, hinge, open, reversed, false);
                assertEquals(1, leaf.size(), what + " draws one leaf");
                assertLeafPlane(leaf.get(0), open, out, what);
                List<String> withCloser = models(parts, facing, half, hinge, open, reversed,
                    true);
                withCloser.removeAll(leaf);
                if (half.equals("upper")) {
                  assertEquals(1, withCloser.size(), what + " draws one closer");
                  assertEquals(out, withCloser.get(0).startsWith("csm:doors/closer_out_"),
                      what + ": the closer is on the push side");
                  assertExists(withCloser.get(0));
                } else {
                  assertTrue(withCloser.isEmpty(), what + ": the closer is the upper half's");
                }
              }
            }
          }
        }
      }
    }
  }

  /** The models a multipart blockstate draws for a door at rest. */
  private static List<String> models(List<JsonObject> parts, String facing, String half,
      String hinge, boolean open, boolean reversed, boolean closer) {
    List<String> out = new ArrayList<>();
    for (JsonObject part : parts) {
      boolean match = true;
      for (Map.Entry<String, JsonElement> c : part.getAsJsonObject("when").entrySet()) {
        String want = c.getValue().getAsString();
        String have;
        switch (c.getKey()) {
          case "facing":
            have = facing;
            break;
          case "half":
            have = half;
            break;
          case "hinge":
            have = hinge;
            break;
          case "open":
            have = String.valueOf(open);
            break;
          case "reversed":
            have = String.valueOf(reversed);
            break;
          case "closer":
            have = String.valueOf(closer);
            break;
          case "swing":
            have = "false";
            break;
          default:
            throw new AssertionError("a door state has no property " + c.getKey());
        }
        match &= want.equals(have);
      }
      if (match) {
        out.add(part.getAsJsonObject("apply").get("model").getAsString());
      }
    }
    return out;
  }

  /**
   * The leaf -- every element that is not hardware -- lies where the collision box puts it: shut,
   * along the outside face (z 14.25..16) of a door that swings in and the inside face (z 0..1.75)
   * of one that swings out; open, along the jamb, the whole depth of the cell.
   */
  private static void assertLeafPlane(String model, boolean open, boolean out, String what)
      throws IOException {
    JsonObject m = read(modelPath(model));
    double z0 = Double.MAX_VALUE;
    double z1 = -Double.MAX_VALUE;
    for (JsonElement el : m.getAsJsonArray("elements")) {
      JsonObject e = el.getAsJsonObject();
      String texture = e.getAsJsonObject("faces").entrySet().iterator().next().getValue()
          .getAsJsonObject().get("texture").getAsString();
      if (texture.equals("#hw")) {
        continue;
      }
      z0 = Math.min(z0, vec(e, "from")[2]);
      z1 = Math.max(z1, vec(e, "to")[2]);
    }
    assertTrue(z0 <= z1, what + ": no leaf");
    if (open) {
      assertEquals(0.0, z0, 1e-6, what + ": the open leaf runs the depth of the cell");
      assertEquals(16.0, z1, 1e-6, what + ": the open leaf runs the depth of the cell");
    } else if (out) {
      assertEquals(0.0, z0, 1e-6, what + ": a door that swings out hangs inside");
      assertEquals(1.75, z1, 1e-6, what + ": a door that swings out hangs inside");
    } else {
      assertEquals(14.25, z0, 1e-6, what + ": a door that swings in hangs outside");
      assertEquals(16.0, z1, 1e-6, what + ": a door that swings in hangs outside");
    }
  }

  private static void assertExists(String model) throws IOException {
    read(modelPath(model));
  }

  private static String modelPath(String model) {
    return "assets/csm/models/block/" + model.substring("csm:".length()) + ".json";
  }

  private static double[] vec(JsonObject e, String key) {
    return new double[]{e.getAsJsonArray(key).get(0).getAsDouble(),
        e.getAsJsonArray(key).get(1).getAsDouble(), e.getAsJsonArray(key).get(2).getAsDouble()};
  }

  private static JsonObject read(String path) throws IOException {
    InputStream in = DoorSwingDirectionTest.class.getClassLoader().getResourceAsStream(path);
    assertNotNull(in, "no resource " + path);
    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      // Minecraft 1.12.2 ships Gson 2.2.4: no JsonParser.parseReader here.
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }
}
