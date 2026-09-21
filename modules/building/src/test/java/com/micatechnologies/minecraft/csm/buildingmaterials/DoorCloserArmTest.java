package com.micatechnologies.minecraft.csm.buildingmaterials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * A door closer's arm is baked into the closer's models shut and open by {@code gen_doors.py}, and
 * drawn by the swing renderer at every angle between from {@link DoorCloserArm#solve}. These hold
 * the two to the same arm -- so a swing starts on the shut model and ends on the open one rather
 * than jumping -- and hold the arm clear of the leaf and the jambs all the way round, which is what
 * its lengths were chosen for.
 */
class DoorCloserArmTest {

  private static final String MODELS = "assets/csm/models/block/doors/";
  /** SHARED with gen_doors.TINT_ARM. */
  private static final int TINT_ARM = 2;

  @Test
  void theShutModelIsTheSolutionAtNoDegrees() throws IOException {
    assertArmMatches("closer_left", 0);
  }

  @Test
  void theOpenModelIsTheSolutionAtAQuarterTurn() throws IOException {
    assertArmMatches("closer_left_open", 90);
  }

  @Test
  void theArmReachesAndStaysClearThroughTheWholeSwing() {
    for (int tenths = 0; tenths <= 900; tenths++) {
      double degrees = tenths / 10.0;
      double[] j = DoorCloserArm.solve(degrees);
      double reach = Math.hypot(DoorCloserArm.SHOE_X - j[0], DoorCloserArm.SHOE_Z - j[1]);
      assertTrue(reach < DoorCloserArm.MAIN + DoorCloserArm.FORE - 0.3,
          "the arm is pulled straight at " + degrees);
      assertTrue(reach > Math.abs(DoorCloserArm.MAIN - DoorCloserArm.FORE) + 0.3,
          "the arm folds flat at " + degrees);
      assertEquals(DoorCloserArm.MAIN, Math.hypot(j[2] - j[0], j[3] - j[1]), 1e-9);
      assertEquals(DoorCloserArm.FORE,
          Math.hypot(DoorCloserArm.SHOE_X - j[2], DoorCloserArm.SHOE_Z - j[3]), 1e-9);
      for (int k = 1; k <= 40; k++) {
        double t = k / 40.0;
        assertClear(j[0] + (j[2] - j[0]) * t, j[1] + (j[3] - j[1]) * t, degrees);
        assertClear(j[2] + (DoorCloserArm.SHOE_X - j[2]) * t,
            j[3] + (DoorCloserArm.SHOE_Z - j[3]) * t, degrees);
      }
    }
  }

  /** A point of the arm's centre line is neither in the leaf (with the link's width) nor a jamb. */
  private static void assertClear(double x, double z, double degrees) {
    double half = DoorCloserArm.LINK_HALF + 0.05;
    // Into the closed leaf's frame: turn back about the hinge pivot.
    double a = Math.toRadians(-degrees);
    double rx = x - DoorCloserArm.PIVOT_X;
    double rz = z - DoorCloserArm.PIVOT_Z;
    double lx = DoorCloserArm.PIVOT_X + rx * Math.cos(a) + rz * Math.sin(a);
    double lz = DoorCloserArm.PIVOT_Z - rx * Math.sin(a) + rz * Math.cos(a);
    assertFalse(lx > -half && lx < 16 + half && lz > 14.25 - half && lz < 16 + half,
        "the arm passes through the leaf at " + degrees);
    // Inside the wall's thickness the opening is x 0..16; beyond it is open air.
    assertFalse(z < 16 && (x < half || x > 16 - half),
        "the arm passes through a jamb at " + degrees);
  }

  private static void assertArmMatches(String model, double degrees) throws IOException {
    double[] j = DoorCloserArm.solve(degrees);
    int found = 0;
    for (JsonElement el : read(model).getAsJsonArray("elements")) {
      JsonObject e = el.getAsJsonObject();
      JsonObject anyFace = e.getAsJsonObject("faces").entrySet().iterator().next().getValue()
          .getAsJsonObject();
      if (!anyFace.has("tintindex") || anyFace.get("tintindex").getAsInt() != TINT_ARM) {
        continue;
      }
      double[] from = vec(e.getAsJsonArray("from"));
      double[] to = vec(e.getAsJsonArray("to"));
      double cx = (from[0] + to[0]) / 2;
      double cz = (from[2] + to[2]) / 2;
      // The long axis: along x or z before the element is turned.
      double ux = to[0] - from[0] >= to[2] - from[2] ? 1 : 0;
      double uz = 1 - ux;
      if (e.has("rotation")) {
        JsonObject r = e.getAsJsonObject("rotation");
        assertEquals("y", r.get("axis").getAsString());
        double[] o = vec(r.getAsJsonArray("origin"));
        double a = Math.toRadians(r.get("angle").getAsDouble());
        double dx = cx - o[0];
        double dz = cz - o[2];
        cx = o[0] + dx * Math.cos(a) + dz * Math.sin(a);
        cz = o[2] - dx * Math.sin(a) + dz * Math.cos(a);
        double tx = ux * Math.cos(a) + uz * Math.sin(a);
        double tz = -ux * Math.sin(a) + uz * Math.cos(a);
        ux = tx;
        uz = tz;
      }
      double y0 = from[1];
      if (y0 == DoorCloserArm.MAIN_Y0) {
        assertLink(model + " main arm", cx, cz, ux, uz, j[0], j[1], j[2], j[3]);
      } else if (y0 == DoorCloserArm.FORE_Y0) {
        assertLink(model + " forearm", cx, cz, ux, uz, j[2], j[3], DoorCloserArm.SHOE_X,
            DoorCloserArm.SHOE_Z);
      } else {
        assertEquals(DoorCloserArm.PIN_Y0, y0, 1e-9, model + ": an arm part at no known height");
        assertEquals(j[2], cx, 2e-3, model + " elbow pin x");
        assertEquals(j[3], cz, 2e-3, model + " elbow pin z");
      }
      found++;
    }
    assertEquals(3, found, model + " should bake two links and a pin");
  }

  private static void assertLink(String what, double cx, double cz, double ux, double uz,
      double ax, double az, double bx, double bz) {
    assertEquals((ax + bx) / 2, cx, 2e-3, what + " centre x");
    assertEquals((az + bz) / 2, cz, 2e-3, what + " centre z");
    double len = Math.hypot(bx - ax, bz - az);
    double cross = ux * (bz - az) / len - uz * (bx - ax) / len;
    assertEquals(0.0, cross, 1e-3, what + " direction");
  }

  private static double[] vec(JsonArray a) {
    return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
  }

  private static JsonObject read(String model) throws IOException {
    InputStream in = DoorCloserArmTest.class.getClassLoader()
        .getResourceAsStream(MODELS + model + ".json");
    assertNotNull(in, "no model " + model);
    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      // Minecraft 1.12.2 ships Gson 2.2.4: no JsonParser.parseReader here.
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }
}
