package com.micatechnologies.minecraft.csm.trafficsigns;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Checks, against the shipped road sign models, that no sign draws two different surfaces so
 * close together that the depth buffer cannot tell them apart from across a street.
 *
 * <p>A sign's art sits on a sliver a hundredth of a model unit in front of its plate -- it has
 * to, the back-to-back convention puts it there (see {@link SignShiftModelTest}). The plates
 * also drew their own front face, bare metal, that same hundredth of a unit behind the art:
 * 0.000625 blocks. A 24-bit depth buffer behind Minecraft's 0.05 near plane resolves
 * {@code z * z / 838861} blocks at {@code z} blocks away, which passes that gap at 23 blocks and
 * is unreliable from half of it, so two thirds of the mod's signs flickered between their face
 * and bare metal from a dozen blocks out (issue #212). Nothing about it shows up close, which is
 * where a model gets looked at when it is made, so it takes measuring to catch.</p>
 *
 * <p>The rule: two faces looking the same way, wearing different textures, overlapping, must be
 * at least {@link #MIN_GAP} units apart. Faces wearing the same texture are let through -- the
 * four boxes of an octagonal plate overlap on one plane, and metal fighting the same metal
 * cannot be seen. The repair for a plate is {@code dev-env-utils/scripts/
 * fix_sign_plate_backing.py --apply}, which recesses the metal behind the art without changing
 * how the sign looks.</p>
 */
class SignFaceDepthTest {

  /**
   * The least distance, in model units, between two overlapping faces that look the same way:
   * 0.0125 blocks, which the depth buffer holds to about 100 blocks away -- past where a sign
   * is more than a few pixels.
   */
  private static final double MIN_GAP = 0.2;

  /** One face of one element: the plane it lies on and the rectangle it covers there. */
  private static final class Face {
    final int element;
    final String texture;
    final double plane;
    final double minU;
    final double maxU;
    final double minV;
    final double maxV;

    Face(int element, String texture, double plane, double minU, double maxU, double minV,
        double maxV) {
      this.element = element;
      this.texture = texture;
      this.plane = plane;
      this.minU = minU;
      this.maxU = maxU;
      this.minV = minV;
      this.maxV = maxV;
    }

    boolean overlaps(Face other) {
      return Math.min(maxU, other.maxU) - Math.max(minU, other.minU) > 1e-6
          && Math.min(maxV, other.maxV) - Math.max(minV, other.minV) > 1e-6;
    }
  }

  @Test
  void noSignDrawsTwoSurfacesTheDepthBufferCannotSeparate()
      throws IOException, URISyntaxException {
    List<String> problems = new ArrayList<>();
    int models = 0;
    for (File file : signModels()) {
      JsonObject json = parse(file);
      if (!json.has("elements")) {
        continue;
      }
      models++;
      check(file.getName(), json.getAsJsonArray("elements"), problems);
    }
    assertTrue(models > 50, "found only " + models + " sign models: the resources moved");
    if (!problems.isEmpty()) {
      fail(problems.size() + " pair(s) of sign faces too close to tell apart at a distance "
          + "(run dev-env-utils/scripts/fix_sign_plate_backing.py --apply):\n  "
          + String.join("\n  ", problems));
    }
  }

  private static void check(String model, JsonArray elements, List<String> problems) {
    // north, south: planes of constant z, looked at along it. The signs' faces are these two;
    // a plate's thin edges are not worth measuring.
    for (String side : new String[] {"north", "south"}) {
      List<Face> faces = new ArrayList<>();
      for (int i = 0; i < elements.size(); i++) {
        JsonObject element = elements.get(i).getAsJsonObject();
        JsonObject all = element.getAsJsonObject("faces");
        if (all == null || !all.has(side) || turnsOutOfPlane(element)) {
          continue;
        }
        double[] from = triple(element.getAsJsonArray("from"));
        double[] to = triple(element.getAsJsonArray("to"));
        double plane = "north".equals(side) ? Math.min(from[2], to[2]) : Math.max(from[2], to[2]);
        faces.add(new Face(i, all.getAsJsonObject(side).get("texture").getAsString(), plane,
            Math.min(from[0], to[0]), Math.max(from[0], to[0]),
            Math.min(from[1], to[1]), Math.max(from[1], to[1])));
      }
      for (int a = 0; a < faces.size(); a++) {
        for (int b = a + 1; b < faces.size(); b++) {
          Face one = faces.get(a);
          Face two = faces.get(b);
          double gap = Math.abs(one.plane - two.plane);
          if (gap < MIN_GAP && !one.texture.equals(two.texture) && one.overlaps(two)) {
            problems.add(String.format("%s: elements %d (%s) and %d (%s) both face %s, %.4f apart",
                model, one.element, one.texture, two.element, two.texture, side, gap));
          }
        }
      }
    }
  }

  /**
   * Whether an element is turned about x or y, which takes its north face off a plane of
   * constant z. A turn about z -- a diamond, an octagon's corners -- leaves it on one.
   */
  private static boolean turnsOutOfPlane(JsonObject element) {
    JsonObject rotation = element.getAsJsonObject("rotation");
    if (rotation == null || rotation.get("angle").getAsDouble() == 0.0) {
      return false;
    }
    return !"z".equals(rotation.get("axis").getAsString());
  }

  private static List<File> signModels() throws IOException, URISyntaxException {
    List<File> out = new ArrayList<>();
    Set<File> seen = new LinkedHashSet<>();
    Enumeration<URL> roots = SignFaceDepthTest.class.getClassLoader()
        .getResources("assets/csm/models/block/trafficsigns");
    while (roots.hasMoreElements()) {
      URL root = roots.nextElement();
      if (!"file".equals(root.getProtocol())) {
        continue;
      }
      File[] files = new File(root.toURI()).listFiles();
      if (files == null) {
        continue;
      }
      for (File file : files) {
        if (file.getName().endsWith(".json") && seen.add(file)) {
          out.add(file);
        }
      }
    }
    return out;
  }

  private static double[] triple(JsonArray array) {
    return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(),
        array.get(2).getAsDouble()};
  }

  private static JsonObject parse(File file) throws IOException {
    try (InputStream stream = file.toURI().toURL().openStream();
         Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      // Minecraft 1.12.2 ships Gson 2.2.4: no JsonParser.parseReader here.
      JsonElement root = new JsonParser().parse(reader);
      return root.getAsJsonObject();
    }
  }
}
