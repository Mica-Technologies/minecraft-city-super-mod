package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The multipart blockstates of OBJ models whose options ride in the model's location
 * ({@code csm:a/b.obj?flip-v&body=csm:blocks/x}, read by {@link CsmObjModelLoader}): every file
 * they name exists in some tree, every option is one the loader understands, and every texture an
 * option names is a sprite on disk. That each state draws what its Forge form drew was checked
 * when they were written, with {@code verify_obj_multipart.py}.
 */
class CsmObjMultipartTest {

  private static final Set<String> CUSTOM = new HashSet<>(Arrays.asList("flip-v", "ambient",
      "gui3d"));

  @Test
  void objLocationsResolve() throws IOException {
    List<Path> trees = roots(Paths.get("src", "main", "resources", "assets", "csm"));
    int blockstates = 0;
    Set<String> checked = new HashSet<>();
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
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        if (!text.contains(CsmObjModelLoader.OPTIONS)) {
          continue;
        }
        JsonObject state = read(file);
        List<String> locations = new ArrayList<>();
        collect(state, locations);
        for (String location : locations) {
          if (checked.add(location)) {
            check(trees, file.getFileName().toString(), location);
          }
        }
        assertTrue(state.has("multipart"), file + ": options are for multipart blockstates");
        blockstates++;
      }
    }
    // The light pole families of #99: 228 luminaire arms, 45 signal arms, 8 decorative poles.
    assertTrue(blockstates >= 281, "expected the converted OBJ blockstates, found " + blockstates);
  }

  private static void collect(JsonElement e, List<String> out) {
    if (e.isJsonObject()) {
      for (java.util.Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet()) {
        if ("model".equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
          out.add(entry.getValue().getAsString());
        } else {
          collect(entry.getValue(), out);
        }
        if ("apply".equals(entry.getKey()) && entry.getValue().isJsonObject()) {
          JsonObject apply = entry.getValue().getAsJsonObject();
          for (String axis : new String[]{"x", "y"}) {
            if (apply.has(axis)) {
              int deg = apply.get(axis).getAsInt();
              assertEquals(0, deg % 90, "rotation " + deg);
            }
          }
        }
      }
    } else if (e.isJsonArray()) {
      for (JsonElement x : e.getAsJsonArray()) {
        collect(x, out);
      }
    }
  }

  private static void check(List<Path> trees, String name, String location) {
    int at = location.indexOf(CsmObjModelLoader.OPTIONS);
    String file = at < 0 ? location : location.substring(0, at + ".obj".length());
    assertTrue(file.startsWith("csm:") && file.endsWith(".obj"), name + ": " + location);
    String path = file.substring("csm:".length());
    if (!path.startsWith("block/")) {
      path = "block/" + path;
    }
    assertTrue(exists(trees, "models/" + path), name + ": no file " + file);
    if (at < 0) {
      return;
    }
    for (String option : location.substring(at + CsmObjModelLoader.OPTIONS.length()).split("&")) {
      int eq = option.indexOf('=');
      String key = eq < 0 ? option : option.substring(0, eq);
      String value = eq < 0 ? "true" : option.substring(eq + 1);
      if (CUSTOM.contains(key)) {
        assertTrue("true".equals(value) || "false".equals(value), name + ": " + option);
      } else if ("transform".equals(key)) {
        assertEquals("forge:default-block", value, name + ": " + option);
      } else {
        assertTrue(value.startsWith("csm:"), name + ": " + option);
        assertTrue(exists(trees, "textures/" + value.substring("csm:".length()) + ".png"),
            name + ": no texture " + value);
      }
    }
  }

  private static boolean exists(List<Path> trees, String rel) {
    for (Path tree : trees) {
      if (Files.exists(tree.resolve(rel))) {
        return true;
      }
    }
    return false;
  }

  private static JsonObject read(Path p) throws IOException {
    try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
      return new JsonParser().parse(r).getAsJsonObject();
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
