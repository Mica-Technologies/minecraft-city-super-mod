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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Checks, against the shipped resources, that every road sign's {@code shift} variants name
 * models that actually do what the shift means.
 *
 * <p>A sign has three shift models and the geometry of each is a convention, not a free choice:
 *
 * <ul>
 *   <li>{@code none} -- the plate at the front of the block, the post behind it;</li>
 *   <li>{@code setback} -- the same model moved 12.5 units back, so the plate sits at the back
 *       of the block and the post reaches its rear face;</li>
 *   <li>{@code backtoback} -- the same model moved 28.5 units back, a whole block plus the
 *       setback, and WITHOUT the post: the plate ends up on the far side of the partner sign's
 *       post, in the partner's block, which is what puts two faces on one post.</li>
 * </ul>
 *
 * <p>Both ways of getting that wrong have shipped. A {@code shift} entry that names the model it
 * is shifting from renders the shift as nothing, so the sign simply never goes back to back; and
 * a model built by keeping the first element of the {@code none} model alone -- right for a sign,
 * whose first element is its plate, wrong for a mount, whose first element is one bar of a
 * bracket -- draws a fragment of the hardware floating in the block behind, which looks worse
 * than not shifting at all. Both are caught here, because neither is visible in the blockstate:
 * it takes measuring the models the blockstate points at.</p>
 */
class SignShiftModelTest {

  /** Where a sign's own shift geometry lives, in model units, relative to its {@code none}. */
  private static final double BACK_TO_BACK_SHIFT = 28.5;

  /** How far behind its own block a back-to-back plate must be, at the least. */
  private static final double ONE_BLOCK = 16.0;

  /**
   * Signs whose {@code shift} entries are allowed to name the same model three times, because
   * back-to-back genuinely has no meaning for them.
   */
  private static final Map<String, String> NOT_APPLICABLE = new LinkedHashMap<>();

  /**
   * Signs known to be wrong here, whose fixes belong to other work on issue #207. Delete an entry
   * when that fix lands -- the entries are skips, and a name left behind is a hole in the check.
   */
  private static final Map<String, String> PENDING_FIX = new LinkedHashMap<>();

  static {
    NOT_APPLICABLE.put("signstatelawstopforpeds",
        "the R1-6 in-street paddle: painted on both sides, standing in the roadway on a flexible "
            + "base rather than on a post, so there is no plate to move and no post to share");
    NOT_APPLICABLE.put("signstatelawstopforpedsflashingled", "as signstatelawstopforpeds");
    NOT_APPLICABLE.put("signdoubleoneway",
        "the double one-way blades already read from both sides, and they run 26 units along the "
            + "very axis the shift moves: a copy a block back would have to reach z 43.5, past "
            + "the -16..32 an element may occupy before the whole model silently fails to load");
    NOT_APPLICABLE.put("signdoubleonewayb", "as signdoubleoneway");

    PENDING_FIX.put("yieldsign", "yield_sign model family, issue #207");
    PENDING_FIX.put("signnopassingzone", "yield_sign model family, issue #207");
    PENDING_FIX.put("signrailroadcrossbuck", "yield_sign model family, issue #207");
    PENDING_FIX.put("signschoolbusstopahead", "yield_sign model family, issue #207");
    PENDING_FIX.put("signschoolcrossing", "yield_sign model family, issue #207");
    PENDING_FIX.put("signschoolcrossingflashingled", "yield_sign model family, issue #207");
    PENDING_FIX.put("signpost50min30", "ultratall plate family, issue #207");
    PENDING_FIX.put("signpost55min40", "ultratall plate family, issue #207");
    PENDING_FIX.put("signpost65min45", "ultratall plate family, issue #207");
    PENDING_FIX.put("thicklysettledspeedlimit25mphsign", "ultratall plate family, issue #207");
  }

  // region: the measurements a model is judged on

  /** What the test needs to know about one baked-model file. */
  private static final class Geometry {

    /** The model this came from, as the blockstate names it. */
    private final String ref;
    /** Front-most z any element reaches, in model units; {@code null} when there are none. */
    private final Double frontZ;
    /** How many elements the model draws. */
    private final int elementCount;
    /** x/y bounds of the largest face painted with a sign face texture, or {@code null}. */
    private final String plate;
    /** Every element's box and face textures, for telling two models apart. */
    private final String signature;

    private Geometry(String ref, Double frontZ, int elementCount, String plate,
        String signature) {
      this.ref = ref;
      this.frontZ = frontZ;
      this.elementCount = elementCount;
      this.plate = plate;
      this.signature = signature;
    }
  }

  // endregion

  @Test
  void everySignShiftNamesAModelThatActuallyShifts() throws Exception {
    Map<String, JsonObject> blockstates = readSignBlockstates();
    assertTrue(blockstates.size() > 500,
        "expected the whole road sign catalogue, found " + blockstates.size());

    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, JsonObject> entry : blockstates.entrySet()) {
      String name = entry.getKey();
      if (PENDING_FIX.containsKey(name)) {
        continue;
      }
      check(name, entry.getValue(), NOT_APPLICABLE.containsKey(name), problems);
    }
    if (!problems.isEmpty()) {
      fail("Sign shift models that do not follow the convention:\n  "
          + String.join("\n  ", problems));
    }
  }

  /**
   * Checks one sign's three shift models.
   *
   * @param name          the sign's registry name
   * @param blockstate    its blockstate JSON
   * @param notApplicable whether back-to-back is exempt for this sign
   * @param problems      collects one line per fault found
   *
   * @throws IOException if a model cannot be read
   */
  private void check(String name, JsonObject blockstate, boolean notApplicable,
      List<String> problems) throws IOException {
    JsonObject variants = blockstate.getAsJsonObject("variants");
    if (variants == null || !variants.has("shift")) {
      problems.add(name + ": a sign with no shift variants at all");
      return;
    }
    JsonObject shift = variants.getAsJsonObject("shift");
    for (String value : Arrays.asList("none", "setback", "backtoback")) {
      if (!shift.has(value)) {
        problems.add(name + ": shift has no " + value + " variant");
        return;
      }
    }
    String defaultModel = blockstate.getAsJsonObject("defaults").get("model").getAsString();
    Geometry none = read(modelOf(shift, "none", defaultModel), name, problems);
    Geometry setback = read(modelOf(shift, "setback", defaultModel), name, problems);
    Geometry backToBack = read(modelOf(shift, "backtoback", defaultModel), name, problems);
    if (none == null || setback == null || backToBack == null) {
      return;
    }
    if (notApplicable) {
      return;
    }

    if (setback.ref.equals(none.ref) || setback.signature.equals(none.signature)) {
      problems.add(name + ": setback names " + setback.ref
          + ", which is the same geometry as none, so the sign never sets back");
    }
    if (backToBack.ref.equals(none.ref) || backToBack.signature.equals(none.signature)) {
      problems.add(name + ": backtoback names " + backToBack.ref
          + ", which is the same geometry as none, so the sign never goes back to back");
      return;
    }
    if (backToBack.ref.equals(setback.ref)) {
      problems.add(name + ": backtoback names the setback model " + backToBack.ref
          + ", which leaves the plate in this sign's own block");
      return;
    }
    if (backToBack.elementCount == 0) {
      // Drawing nothing is the right answer for a piece that is all post: the partner's post
      // already stands through this block. It is only right when this sign has no face of its own.
      if (none.plate != null) {
        problems.add(name + ": backtoback draws nothing, but this sign has a face to show");
      }
      return;
    }
    if (none.frontZ != null && backToBack.frontZ != null) {
      double low = none.frontZ + ONE_BLOCK;
      double high = none.frontZ + BACK_TO_BACK_SHIFT + 0.5;
      if (backToBack.frontZ < low - 1.0e-6 || backToBack.frontZ > high + 1.0e-6) {
        problems.add(String.format(
            "%s: backtoback (%s) starts at z=%.3f, which is not the block behind "
                + "(expected %.3f..%.3f, the convention being none + %.1f)",
            name, backToBack.ref, backToBack.frontZ, low, high, BACK_TO_BACK_SHIFT));
      }
    }
    if (none.plate != null && backToBack.plate != null && !none.plate.equals(backToBack.plate)) {
      problems.add(name + ": backtoback (" + backToBack.ref + ") paints a " + backToBack.plate
          + " plate, but this sign's plate is " + none.plate);
    }
  }

  /**
   * Resolves the model a shift value uses, which is the blockstate default unless the variant
   * overrides it.
   *
   * @param shift        the {@code shift} variant block
   * @param value        the shift value
   * @param defaultModel the blockstate's default model
   *
   * @return the model reference
   */
  private static String modelOf(JsonObject shift, String value, String defaultModel) {
    JsonElement raw = shift.get(value);
    JsonObject variant;
    if (raw.isJsonArray()) {
      JsonArray array = raw.getAsJsonArray();
      variant = array.size() > 0 ? array.get(0).getAsJsonObject() : new JsonObject();
    } else {
      variant = raw.getAsJsonObject();
    }
    return variant.has("model") ? variant.get("model").getAsString() : defaultModel;
  }

  // region: reading the resources

  /**
   * Reads every blockstate in the mod whose default model is a road sign model.
   *
   * @return blockstate JSON by registry name
   *
   * @throws IOException        if a directory or file cannot be read
   * @throws URISyntaxException if a classpath URL is not a file
   */
  private static Map<String, JsonObject> readSignBlockstates()
      throws IOException, URISyntaxException {
    Map<String, JsonObject> signs = new TreeMap<>();
    Enumeration<URL> roots =
        SignShiftModelTest.class.getClassLoader().getResources("assets/csm/blockstates");
    Set<File> seen = new LinkedHashSet<>();
    while (roots.hasMoreElements()) {
      URL root = roots.nextElement();
      if (!"file".equals(root.getProtocol())) {
        continue;
      }
      File dir = new File(root.toURI());
      File[] files = dir.listFiles();
      if (files == null) {
        continue;
      }
      for (File file : files) {
        if (!file.getName().endsWith(".json") || !seen.add(file)) {
          continue;
        }
        JsonObject json = parse(file);
        JsonObject defaults = json.getAsJsonObject("defaults");
        if (defaults == null || !defaults.has("model")) {
          continue;
        }
        String model = defaults.get("model").getAsString();
        if (model.startsWith("csm:trafficsigns/")) {
          String name = file.getName().substring(0, file.getName().length() - ".json".length());
          signs.put(name, json);
        }
      }
    }
    return signs;
  }

  /**
   * Measures the model a shift value names.
   *
   * @param ref      the model reference, {@code csm:trafficsigns/<name>} or {@code ...obj}
   * @param sign     the sign being checked, for the message
   * @param problems collects a line if the model is missing
   *
   * @return the model's measurements, or {@code null} if it could not be read
   *
   * @throws IOException if the file cannot be read
   */
  private static Geometry read(String ref, String sign, List<String> problems) throws IOException {
    String path = ref.startsWith("csm:") ? ref.substring("csm:".length()) : ref;
    if (path.endsWith(".obj")) {
      URL url = resource("assets/csm/models/block/" + path);
      if (url == null) {
        problems.add(sign + ": model " + ref + " is not in any module's resources");
        return null;
      }
      return readObj(ref, url);
    }
    JsonObject model = null;
    String current = path;
    for (int depth = 0; depth < 8 && current != null; depth++) {
      URL url = resource("assets/csm/models/block/" + current + ".json");
      if (url == null) {
        problems.add(sign + ": model " + ref + " is not in any module's resources");
        return null;
      }
      model = parse(url);
      if (model.has("elements")) {
        break;
      }
      current = model.has("parent")
          ? model.get("parent").getAsString().replace("csm:", "") : null;
    }
    if (model == null || !model.has("elements")) {
      problems.add(sign + ": model " + ref + " has no elements and no parent that does");
      return null;
    }
    return readElements(ref, model.getAsJsonArray("elements"));
  }

  /**
   * Measures a Forge element model.
   *
   * @param ref      the model reference, for the message
   * @param elements the model's elements
   *
   * @return the model's measurements
   */
  private static Geometry readElements(String ref, JsonArray elements) {
    Double frontZ = null;
    String plate = null;
    double plateArea = -1.0;
    StringBuilder signature = new StringBuilder();
    for (JsonElement raw : elements) {
      JsonObject element = raw.getAsJsonObject();
      double[] from = triple(element.getAsJsonArray("from"));
      double[] to = triple(element.getAsJsonArray("to"));
      double z = Math.min(from[2], to[2]);
      frontZ = frontZ == null ? z : Math.min(frontZ, z);
      signature.append(Arrays.toString(from)).append(Arrays.toString(to));
      JsonObject faces = element.getAsJsonObject("faces");
      if (faces == null) {
        continue;
      }
      for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
        JsonObject definition = face.getValue().getAsJsonObject();
        String texture =
            definition.has("texture") ? definition.get("texture").getAsString() : "";
        signature.append(face.getKey()).append('=').append(texture).append(';');
        // A sign's face is painted from a numbered texture slot the blockstate fills in; the
        // blank metalwork all around it is slot 0. The biggest such face is the plate.
        if (!"#1".equals(texture) && !"#2".equals(texture) && !"#3".equals(texture)) {
          continue;
        }
        double area = Math.abs((to[0] - from[0]) * (to[1] - from[1]));
        if (area > plateArea) {
          plateArea = area;
          plate = String.format("%.3f,%.3f..%.3f,%.3f", from[0], from[1], to[0], to[1]);
        }
      }
    }
    return new Geometry(ref, frontZ, elements.size(), plate, signature.toString());
  }

  /**
   * Measures a Forge OBJ model, whose coordinates are in blocks rather than model units.
   *
   * @param ref the model reference, for the message
   * @param url the file
   *
   * @return the model's measurements
   *
   * @throws IOException if the file cannot be read
   */
  private static Geometry readObj(String ref, URL url) throws IOException {
    double frontZ = Double.MAX_VALUE;
    int vertices = 0;
    StringBuilder signature = new StringBuilder();
    try (InputStream stream = url.openStream();
         Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name())) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (!line.startsWith("v ")) {
          continue;
        }
        String[] parts = line.split("\\s+");
        double z = Double.parseDouble(parts[3]) * 16.0;
        frontZ = Math.min(frontZ, z);
        vertices++;
        signature.append(line).append(';');
      }
    }
    return new Geometry(ref, vertices == 0 ? null : frontZ, vertices, null,
        signature.toString());
  }

  /**
   * Finds a resource on the test classpath.
   *
   * @param path the resource path
   *
   * @return its URL, or {@code null}
   */
  private static URL resource(String path) {
    return SignShiftModelTest.class.getClassLoader().getResource(path);
  }

  private static double[] triple(JsonArray array) {
    return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(),
        array.get(2).getAsDouble()};
  }

  private static JsonObject parse(File file) throws IOException {
    return parse(file.toURI().toURL());
  }

  private static JsonObject parse(URL url) throws IOException {
    try (InputStream stream = url.openStream();
         Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      // Minecraft 1.12.2 ships Gson 2.2.4: no JsonParser.parseReader here.
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }

  // endregion
}
