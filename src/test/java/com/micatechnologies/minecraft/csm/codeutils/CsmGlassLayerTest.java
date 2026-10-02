package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.util.BlockRenderLayer;
import org.junit.jupiter.api.Test;

/**
 * The glass-fronted blocks' split between passes ({@link CsmGlassLayer}): which textures are
 * glass, and that every glass-fronted block's see-through textures are named as glass, since one
 * that is not would be drawn by alpha test in the cutout pass, opaque wherever it is more than a
 * tenth so.
 *
 * <p>The blocks are found from the sources: every class declared {@code implements
 * ICsmGlassFronted} and its subclasses, and every tab line constructing one, less those given a
 * render layer other than the translucent one. Their textures are read off their blockstates and
 * models.</p>
 */
class CsmGlassLayerTest {

  private static final Pattern CLASS_DECL =
      Pattern.compile("public\\s+(?:abstract\\s+)?class\\s+(\\w+)\\s+extends\\s+(\\w+)([^{]*)\\{");
  private static final Pattern NEW_BLOCK = Pattern.compile("new\\s+(\\w+)\\(\\s*\"([^\"]+)\"");

  @Test
  void glassIsNamedAsGlass() {
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/furniture/market/glass_clear"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/furniture/office/case_glass"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/transit/shelters/glass"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/glazing/glass_oneway_outside"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/x/door_glass_lite"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/constructionsite/trailer_window_top"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/interior/curtain_sheer"));
    assertTrue(CsmGlassLayer.isGlass("csm:blocks/transit/platforms/bag"));

    assertFalse(CsmGlassLayer.isGlass("csm:blocks/powergrid/fiberglass_pole_hv_sign"));
    assertFalse(CsmGlassLayer.isGlass("csm:blocks/furniture/market/stock_hot_food"));
    assertFalse(CsmGlassLayer.isGlass("csm:blocks/furniture/market/liner"));
    assertFalse(CsmGlassLayer.isGlass("csm:blocks/furniture/residential/stainless"));
    assertFalse(CsmGlassLayer.isGlass("csm:blocks/transit/platforms/bags_rack"));
    assertFalse(CsmGlassLayer.isGlass("csm:blocks/glassworks_sign"));
  }

  @Test
  void eachFaceDrawsInExactlyOnePass() {
    for (String name : new String[]{"csm:blocks/furniture/market/glass_clear",
        "csm:blocks/furniture/market/liner"}) {
      int passes = 0;
      for (BlockRenderLayer layer : BlockRenderLayer.values()) {
        if (CsmGlassLayer.drawsIn(layer) && CsmGlassLayer.belongsIn(name, layer)) {
          passes++;
        }
      }
      assertEquals(1, passes, name);
    }
    assertTrue(CsmGlassLayer.drawsIn(BlockRenderLayer.CUTOUT_MIPPED));
    assertTrue(CsmGlassLayer.drawsIn(BlockRenderLayer.TRANSLUCENT));
    assertFalse(CsmGlassLayer.drawsIn(BlockRenderLayer.SOLID));
    assertFalse(CsmGlassLayer.drawsIn(BlockRenderLayer.CUTOUT));
  }

  @Test
  void glassFrontedBlocksNameEverySeeThroughTextureAsGlass() throws IOException {
    List<Path> javaRoots = roots(Paths.get("src", "main", "java"));
    List<Path> assetRoots = roots(Paths.get("src", "main", "resources", "assets", "csm"));
    assertTrue(Files.isDirectory(javaRoots.get(0)), "run from the project root");

    Set<String> glassFronted = glassFrontedClasses(javaRoots);
    assertTrue(glassFronted.contains("BlockDisplayCase"), "found " + glassFronted);

    Map<String, String> blocks = new LinkedHashMap<>();   // registry name -> class
    for (Path root : javaRoots) {
      for (Path file : javaFiles(root)) {
        if (!file.getFileName().toString().startsWith("CsmTab")) {
          continue;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
          Matcher m = NEW_BLOCK.matcher(line);
          if (!m.find() || !glassFronted.contains(m.group(1))) {
            continue;
          }
          if (line.contains("BlockRenderLayer.") && !line.contains("BlockRenderLayer.TRANSLUCENT")) {
            continue;   // a class whose instances choose their layer, this one not translucent
          }
          blocks.put(m.group(2), m.group(1));
        }
      }
    }
    assertTrue(blocks.size() > 40, "found only " + blocks.keySet());

    Map<String, Boolean> partialCache = new HashMap<>();
    List<String> offenders = new ArrayList<>();
    int checked = 0;
    for (Map.Entry<String, String> block : blocks.entrySet()) {
      Path blockstate = find(assetRoots, "blockstates/" + block.getKey() + ".json");
      if (blockstate == null) {
        offenders.add(block.getKey() + ": no blockstate");
        continue;
      }
      Set<String> textures = new TreeSet<>();
      for (String[] model : models(blockstate)) {
        textures.addAll(faceTextures(assetRoots, model[0], model[1]));
      }
      boolean anyGlass = textures.stream().anyMatch(CsmGlassLayer::isGlass);
      List<String> seeThrough = textures.stream()
          .filter(t -> partialCache.computeIfAbsent(t, k -> partlyClear(assetRoots, k)))
          .collect(Collectors.toList());
      if (!anyGlass && seeThrough.isEmpty()) {
        continue;   // an instance with no glass at all (a blackout curtain, a solid shelter)
      }
      checked++;
      for (String t : seeThrough) {
        if (!CsmGlassLayer.isGlass(t)) {
          offenders.add(block.getKey() + " (" + block.getValue() + "): " + t);
        }
      }
    }
    assertTrue(checked > 40, "checked only " + checked);
    assertTrue(offenders.isEmpty(), "A glass-fronted block draws a part-clear texture that is not "
        + "named as glass; the cutout pass would draw it opaque. Name it as glass, add it to "
        + "CsmGlassLayer.OTHER_GLASS, or make its pixels fully opaque or fully clear:\n"
        + String.join("\n", offenders));
  }

  // ---------------------------------------------------------------------------------------------

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

  private static List<Path> javaFiles(Path root) throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      return files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
    }
  }

  /** Classes declared to implement the interface, and their subclasses. */
  private static Set<String> glassFrontedClasses(List<Path> javaRoots) throws IOException {
    Map<String, String> parent = new HashMap<>();
    Set<String> found = new HashSet<>();
    for (Path root : javaRoots) {
      for (Path file : javaFiles(root)) {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Matcher m = CLASS_DECL.matcher(text);
        if (m.find()) {
          parent.put(m.group(1), m.group(2));
          if (m.group(3).contains("ICsmGlassFronted")) {
            found.add(m.group(1));
          }
        }
      }
    }
    boolean grew = true;
    while (grew) {
      grew = false;
      for (Map.Entry<String, String> e : parent.entrySet()) {
        if (found.contains(e.getValue()) && found.add(e.getKey())) {
          grew = true;
        }
      }
    }
    return found;
  }

  private static Path find(List<Path> assetRoots, String relative) {
    for (Path root : assetRoots) {
      Path p = root.resolve(relative);
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    return null;
  }

  private static JsonObject read(Path p) throws IOException {
    try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
      return new JsonParser().parse(r).getAsJsonObject();
    }
  }

  /** Every (model, textures-override JSON or null) a blockstate names. */
  private static List<String[]> models(Path blockstate) throws IOException {
    JsonObject json = read(blockstate);
    List<String[]> out = new ArrayList<>();
    if (json.has("forge_marker")) {
      JsonObject defaults = json.has("defaults") ? json.getAsJsonObject("defaults")
          : new JsonObject();
      String model = defaults.has("model") ? defaults.get("model").getAsString() : null;
      JsonObject textures = defaults.has("textures") ? defaults.getAsJsonObject("textures")
          : new JsonObject();
      if (model != null) {
        out.add(new String[]{model, textures.toString()});
      }
      if (json.has("variants")) {
        forgeVariants(json.get("variants"), model, textures, out);
      }
    } else if (json.has("multipart")) {
      for (JsonElement part : json.getAsJsonArray("multipart")) {
        addApplied(part.getAsJsonObject().get("apply"), out);
      }
    } else if (json.has("variants")) {
      for (Map.Entry<String, JsonElement> v : json.getAsJsonObject("variants").entrySet()) {
        addApplied(v.getValue(), out);
      }
    }
    return out;
  }

  private static void addApplied(JsonElement apply, List<String[]> out) {
    if (apply.isJsonArray()) {
      for (JsonElement e : apply.getAsJsonArray()) {
        out.add(new String[]{e.getAsJsonObject().get("model").getAsString(), null});
      }
    } else {
      out.add(new String[]{apply.getAsJsonObject().get("model").getAsString(), null});
    }
  }

  private static void forgeVariants(JsonElement node, String model, JsonObject textures,
      List<String[]> out) {
    if (node.isJsonArray()) {
      for (JsonElement e : node.getAsJsonArray()) {
        forgeVariants(e, model, textures, out);
      }
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    JsonObject obj = node.getAsJsonObject();
    if (obj.has("model") || obj.has("textures")) {
      JsonObject merged = new JsonObject();
      for (Map.Entry<String, JsonElement> t : textures.entrySet()) {
        merged.add(t.getKey(), t.getValue());
      }
      if (obj.has("textures")) {
        for (Map.Entry<String, JsonElement> t : obj.getAsJsonObject("textures").entrySet()) {
          merged.add(t.getKey(), t.getValue());
        }
      }
      String m = obj.has("model") ? obj.get("model").getAsString() : model;
      if (m != null) {
        out.add(new String[]{m, merged.toString()});
      }
    }
    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
      String k = e.getKey();
      if (!k.equals("textures") && !k.equals("transform") && !k.equals("custom")
          && (e.getValue().isJsonObject() || e.getValue().isJsonArray())) {
        forgeVariants(e.getValue(), model, textures, out);
      }
    }
  }

  private static Path modelFile(List<Path> assetRoots, String name) {
    if (name.endsWith(".obj")) {
      return null;
    }
    String path = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
    Path p = find(assetRoots, "models/block/" + path + ".json");
    return p != null ? p : find(assetRoots, "models/" + path + ".json");
  }

  /** The textures a model's faces wear, resolved through its parents and the overrides. */
  private static Set<String> faceTextures(List<Path> assetRoots, String model, String overrides)
      throws IOException {
    List<JsonObject> chain = new ArrayList<>();
    String name = model;
    while (name != null && chain.size() < 16) {
      Path p = modelFile(assetRoots, name);
      if (p == null) {
        break;
      }
      JsonObject json = read(p);
      chain.add(json);
      name = json.has("parent") ? json.get("parent").getAsString() : null;
    }
    Map<String, String> textures = new HashMap<>();
    Set<String> refs = new HashSet<>();
    for (int i = chain.size() - 1; i >= 0; i--) {
      JsonObject json = chain.get(i);
      if (json.has("textures")) {
        for (Map.Entry<String, JsonElement> t : json.getAsJsonObject("textures").entrySet()) {
          textures.put(t.getKey(), t.getValue().getAsString());
        }
      }
      if (json.has("elements")) {
        refs.clear();
        for (JsonElement el : json.getAsJsonArray("elements")) {
          JsonObject faces = el.getAsJsonObject().getAsJsonObject("faces");
          for (Map.Entry<String, JsonElement> f : faces.entrySet()) {
            refs.add(f.getValue().getAsJsonObject().get("texture").getAsString());
          }
        }
      }
    }
    if (overrides != null) {
      JsonObject o = new JsonParser().parse(overrides).getAsJsonObject();
      for (Map.Entry<String, JsonElement> t : o.entrySet()) {
        textures.put(t.getKey(), t.getValue().getAsString());
      }
    }
    Set<String> out = new HashSet<>();
    for (String ref : refs) {
      String t = ref;
      for (int i = 0; i < 10 && t.startsWith("#"); i++) {
        t = textures.getOrDefault(t.substring(1), t);
      }
      if (!t.startsWith("#")) {
        out.add(t.contains(":") ? t : "minecraft:" + t);
      }
    }
    return out;
  }

  /** Whether a csm texture has a pixel neither fully opaque nor fully clear. */
  private static boolean partlyClear(List<Path> assetRoots, String texture) {
    if (!texture.startsWith("csm:")) {
      return false;
    }
    Path p = find(assetRoots, "textures/" + texture.substring(4) + ".png");
    if (p == null) {
      return false;
    }
    try {
      BufferedImage img = ImageIO.read(p.toFile());
      if (img == null || !img.getColorModel().hasAlpha()) {
        return false;
      }
      for (int y = 0; y < img.getHeight(); y++) {
        for (int x = 0; x < img.getWidth(); x++) {
          int a = img.getRGB(x, y) >>> 24;
          if (a != 0 && a != 255) {
            return true;
          }
        }
      }
      return false;
    } catch (IOException e) {
      throw new RuntimeException(p.toString(), e);
    }
  }
}
