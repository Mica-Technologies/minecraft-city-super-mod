package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The blockstates {@code forge_multipart.py} turned from Forge variants into multipart, because
 * a property of theirs only swapped textures (a TV's channel, a gate sign's letter and number):
 * each texture set is a child model under {@code models/block/.../states/<block>/}, and every
 * state must still find exactly one model.
 *
 * <p>For each such blockstate: every combination of the values its rules name matches exactly
 * one rule (a state matching none would draw nothing, two would draw both); every model a rule
 * or the item names is a child model (a parent and textures, no elements of its own) whose
 * parent exists; every texture the drawn elements use resolves; and every child has the same
 * particle texture, since a multipart model's particle is its first part's. That the conversion
 * drew every state as the Forge form did was checked when it was made, with
 * {@code verify_multipart_conversion.py}.</p>
 */
class CsmTextureSwapMultipartTest {

  private static final String STATES = "/states/";

  @Test
  void convertedBlockstatesDrawEveryStateOnce() throws IOException {
    List<Path> trees = roots(Paths.get("src", "main", "resources", "assets", "csm"));
    int checked = 0;
    for (Path tree : trees) {
      Path dir = tree.resolve("blockstates");
      if (!Files.isDirectory(dir)) {
        continue;
      }
      List<Path> files;
      try (Stream<Path> s = Files.list(dir)) {
        files = s.filter(p -> p.toString().endsWith(".json")).sorted()
            .collect(Collectors.toList());
      }
      for (Path file : files) {
        JsonObject state = read(file);
        if (state == null || !state.has("multipart") || !converted(state)) {
          continue;
        }
        check(trees, file, state);
        checked++;
      }
    }
    assertTrue(checked >= 70, "expected the converted blockstates, found " + checked);
  }

  private static boolean converted(JsonObject state) {
    for (JsonElement r : state.getAsJsonArray("multipart")) {
      JsonElement apply = r.getAsJsonObject().get("apply");
      if (apply.isJsonObject() && apply.getAsJsonObject().get("model").getAsString()
          .contains(STATES)) {
        return true;
      }
    }
    return false;
  }

  private static void check(List<Path> trees, Path file, JsonObject state) throws IOException {
    String name = file.getFileName().toString();
    JsonArray rules = state.getAsJsonArray("multipart");
    Map<String, Set<String>> values = new TreeMap<>();
    List<Map<String, Set<String>>> whens = new ArrayList<>();
    Set<String> models = new HashSet<>();
    for (JsonElement r : rules) {
      JsonObject rule = r.getAsJsonObject();
      Map<String, Set<String>> when = new HashMap<>();
      if (rule.has("when")) {
        for (Map.Entry<String, JsonElement> e : rule.getAsJsonObject("when").entrySet()) {
          assertFalse("OR".equals(e.getKey()), name + ": a converted rule has no OR");
          Set<String> vs = new HashSet<>();
          for (String v : e.getValue().getAsString().split("\\|")) {
            vs.add(v);
          }
          when.put(e.getKey(), vs);
          values.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(vs);
        }
      }
      whens.add(when);
      models.add(rule.getAsJsonObject("apply").get("model").getAsString());
    }
    // every combination matches exactly one rule
    List<String> props = new ArrayList<>(values.keySet());
    List<Map<String, String>> combos = new ArrayList<>();
    combos.add(new LinkedHashMap<>());
    for (String p : props) {
      List<Map<String, String>> next = new ArrayList<>();
      for (Map<String, String> c : combos) {
        for (String v : values.get(p)) {
          Map<String, String> n = new LinkedHashMap<>(c);
          n.put(p, v);
          next.add(n);
        }
      }
      combos = next;
    }
    for (Map<String, String> combo : combos) {
      int hits = 0;
      for (Map<String, Set<String>> when : whens) {
        boolean all = true;
        for (Map.Entry<String, Set<String>> e : when.entrySet()) {
          if (!e.getValue().contains(combo.get(e.getKey()))) {
            all = false;
            break;
          }
        }
        if (all) {
          hits++;
        }
      }
      assertEquals(1, hits, name + " " + combo + ": rules matched");
    }
    Set<String> blockModels = new HashSet<>(models);
    if (state.has("variants")) {
      JsonObject inv = state.getAsJsonObject("variants").getAsJsonObject("inventory");
      assertNotNull(inv, name + ": inventory variant");
      models.add(inv.get("model").getAsString());
    }
    Set<String> particles = new HashSet<>();
    for (String model : models) {
      JsonObject child = model(trees, model);
      assertNotNull(child, name + ": no model " + model);
      if (model.contains(STATES)) {
        assertTrue(child.has("parent") && child.has("textures") && !child.has("elements"),
            name + ": " + model + " is not a child model");
      }
      Map<String, String> tex = new HashMap<>();
      JsonArray elements = null;
      List<JsonObject> chain = new ArrayList<>();
      for (JsonObject m = child; m != null; ) {
        chain.add(m);
        if (!m.has("parent")) {
          break;
        }
        String parent = m.get("parent").getAsString();
        if (!parent.startsWith("csm:")) {
          break;
        }
        JsonObject next = model(trees, parent);
        assertNotNull(next, name + ": " + model + " has no parent " + parent);
        m = next;
      }
      for (int i = chain.size() - 1; i >= 0; i--) {
        JsonObject m = chain.get(i);
        if (m.has("textures")) {
          for (Map.Entry<String, JsonElement> e : m.getAsJsonObject("textures").entrySet()) {
            tex.put(e.getKey(), e.getValue().getAsString());
          }
        }
      }
      for (JsonObject m : chain) {
        if (m.has("elements")) {
          elements = m.getAsJsonArray("elements");
          break;
        }
      }
      assertNotNull(elements, name + ": " + model + " draws no elements");
      for (JsonElement el : elements) {
        for (Map.Entry<String, JsonElement> f : el.getAsJsonObject().getAsJsonObject("faces")
            .entrySet()) {
          String ref = f.getValue().getAsJsonObject().get("texture").getAsString();
          assertNotNull(resolve(tex, ref), name + ": " + model + " leaves " + ref + " unset");
        }
      }
      if (blockModels.contains(model)) {
        particles.add(resolve(tex, "#particle"));
      }
    }
    assertEquals(1, particles.size(), name + ": the states' particles differ: " + particles);
  }

  private static String resolve(Map<String, String> tex, String ref) {
    for (int i = 0; i < 16 && ref != null; i++) {
      if (!ref.startsWith("#")) {
        return ref;
      }
      ref = tex.get(ref.substring(1));
    }
    return null;
  }

  /** {@code csm:a/b} or {@code csm:block/a/b} in any tree. */
  private static JsonObject model(List<Path> trees, String name) throws IOException {
    String path = name.substring(name.indexOf(':') + 1);
    if (!path.startsWith("block/")) {
      path = "block/" + path;
    }
    for (Path tree : trees) {
      Path p = tree.resolve("models").resolve(path + ".json");
      if (Files.exists(p)) {
        return read(p);
      }
    }
    return null;
  }

  private static JsonObject read(Path p) throws IOException {
    try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
      JsonElement e = new JsonParser().parse(r);
      return e.isJsonObject() ? e.getAsJsonObject() : null;
    }
  }

  private static List<Path> roots(Path inTree) throws IOException {
    List<Path> roots = new ArrayList<>();
    roots.add(inTree);
    Path modules = Paths.get("modules");
    if (Files.isDirectory(modules)) {
      try (Stream<Path> dirs = Files.list(modules)) {
        dirs.sorted().map(d -> d.resolve(inTree)).filter(Files::isDirectory).forEach(roots::add);
      }
    }
    return roots;
  }
}
