package com.micatechnologies.minecraft.csm.technology.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The units' boxes: where they sit, which faces they keep, and that the pre-filled racks the
 * generator writes are drawn with the same boxes as a rack filled by hand.
 */
class RackUnitBoxesTest {

  private static final float EPS = 0.002f;

  @Test
  void aCabinetsCellLineFallsBetweenU21AndU22() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 21)
        .with(RackUnitType.SERVER_1U, 22);
    List<RackUnitBoxes.Box> lower = RackUnitBoxes.boxes(RackFrame.CABINET_42U, c, 0);
    List<RackUnitBoxes.Box> upper = RackUnitBoxes.boxes(RackFrame.CABINET_42U, c, 1);
    assertEquals(1, lower.size());
    assertEquals(1, upper.size());
    assertEquals(16.0f, lower.get(0).y1, EPS);
    assertEquals(0.0f, upper.get(0).y0, EPS);
  }

  @Test
  void aUnitAcrossTheCellLineIsCutWithItsFace() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_2U, 22);
    RackUnitBoxes.Box lo = RackUnitBoxes.boxes(RackFrame.CABINET_42U, c, 0).get(0);
    RackUnitBoxes.Box hi = RackUnitBoxes.boxes(RackFrame.CABINET_42U, c, 1).get(0);
    assertEquals(16.0f - 2.0f / 3.0f, lo.y0, EPS);
    assertEquals(16.0f, lo.y1, EPS);
    assertEquals(0.0f, hi.y0, EPS);
    assertEquals(2.0f / 3.0f, hi.y1, EPS);
    assertFalse(lo.has(RackUnitBoxes.UP), "no face at the cell line");
    assertFalse(hi.has(RackUnitBoxes.DOWN), "no face at the cell line");
    // the face's texture is split where the box is: the upper half of the band above
    float k = 16.0f / RackUnitType.SHEET_TEXELS;
    float v0 = RackUnitType.SERVER_2U.getBandRow() * k;
    float mid = v0 + RackUnitType.TEXELS_PER_U * k;
    assertEquals(v0, hi.frontUv[1], EPS);
    assertEquals(mid, hi.frontUv[3], EPS);
    assertEquals(mid, lo.frontUv[1], EPS);
  }

  @Test
  void touchingFacesOfStackedUnitsAreLeftOut() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 42)
        .with(RackUnitType.SERVER_1U, 41).with(RackUnitType.PATCH_PANEL, 40);
    List<RackUnitBoxes.Box> b = RackUnitBoxes.boxes(RackFrame.CABINET_42U, c, 1);
    assertTrue(b.get(0).has(RackUnitBoxes.UP));
    assertFalse(b.get(0).has(RackUnitBoxes.DOWN));
    assertFalse(b.get(1).has(RackUnitBoxes.UP));
    // the patch panel under it is shallower: the server's bottom shows behind it
    assertTrue(b.get(1).has(RackUnitBoxes.DOWN));
    assertFalse(b.get(2).has(RackUnitBoxes.UP), "a shallow unit's top is under the server");
    assertFalse(b.get(0).has(RackUnitBoxes.SOUTH), "no backs inside a cabinet");
    assertTrue(RackUnitBoxes.boxes(RackFrame.OPEN_42U, c, 1).get(0).has(RackUnitBoxes.SOUTH));
  }

  @Test
  void anOpenFrameHangsADeepUnitOnItsPosts() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 1)
        .with(RackUnitType.PATCH_PANEL, 2);
    List<RackUnitBoxes.Box> b = RackUnitBoxes.boxes(RackFrame.OPEN_42U, c, 0);
    assertEquals(15.5f - 11.0f, b.get(0).z0, EPS);
    assertEquals(15.5f, b.get(0).z1, EPS);
    assertEquals(5.75f, b.get(1).z0, EPS);
  }

  /** The compute farm preset, as gen_technology_rack.py lists it. */
  private static RackContents computeFarm() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SWITCH_48_PORT, 42)
        .with(RackUnitType.PATCH_PANEL, 41).with(RackUnitType.CABLE_MANAGER, 40);
    for (int top = 39; top >= 4; top--) {
      c = c.with(RackUnitType.SERVER_1U, top);
    }
    return c.with(RackUnitType.PDU, 2).with(RackUnitType.PDU, 1);
  }

  @Test
  void aPreFilledRackIsDrawnLikeAFilledOne() throws Exception {
    RackContents c = computeFarm();
    String[] cells = {"lower", "upper"};
    for (int cell = 0; cell < 2; cell++) {
      JsonArray elements = read("assets/csm/models/block/technology/rack/"
          + "server_rack_42u_filled_compute_units_" + cells[cell] + ".json")
          .getAsJsonArray("elements");
      List<RackUnitBoxes.Box> boxes = RackUnitBoxes.boxes(RackFrame.CABINET_42U, c, cell);
      assertEquals(boxes.size(), elements.size(), cells[cell]);
      for (int i = 0; i < boxes.size(); i++) {
        RackUnitBoxes.Box b = boxes.get(i);
        JsonObject e = elements.get(i).getAsJsonObject();
        float[] from = floats(e.getAsJsonArray("from"));
        float[] to = floats(e.getAsJsonArray("to"));
        String where = cells[cell] + " element " + i;
        assertEquals(b.x0, from[0], 0.01f, where);
        assertEquals(b.y0, from[1], 0.01f, where);
        assertEquals(b.z0, from[2], 0.01f, where);
        assertEquals(b.x1, to[0], 0.01f, where);
        assertEquals(b.y1, to[1], 0.01f, where);
        assertEquals(b.z1, to[2], 0.01f, where);
        JsonObject faces = e.getAsJsonObject("faces");
        String[] names = {"north", "south", "east", "west", "up", "down"};
        int[] bits = {RackUnitBoxes.NORTH, RackUnitBoxes.SOUTH, RackUnitBoxes.EAST,
            RackUnitBoxes.WEST, RackUnitBoxes.UP, RackUnitBoxes.DOWN};
        for (int f = 0; f < names.length; f++) {
          assertEquals(b.has(bits[f]), faces.has(names[f]), where + " " + names[f]);
        }
        float[] uv = floats(faces.getAsJsonObject("north").getAsJsonArray("uv"));
        for (int j = 0; j < 4; j++) {
          assertEquals(b.frontUv[j], uv[j], 0.001f, where + " uv");
        }
      }
    }
  }

  private static float[] floats(JsonArray a) {
    float[] out = new float[a.size()];
    int i = 0;
    for (JsonElement v : a) {
      out[i++] = v.getAsFloat();
    }
    return out;
  }

  private static JsonObject read(String path) throws Exception {
    InputStream in = RackUnitBoxesTest.class.getClassLoader().getResourceAsStream(path);
    assertNotNull(in, path + " is not on the test classpath");
    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }
}
