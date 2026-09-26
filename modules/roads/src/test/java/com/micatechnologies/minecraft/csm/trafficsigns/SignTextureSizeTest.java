package com.micatechnologies.minecraft.csm.trafficsigns;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Holds every road sign face texture to the size its plate is given: the smallest power of two
 * that gives the largest plate drawing it at least {@link #DENSITY} texels a block, never below
 * {@link #FLOOR} px.
 *
 * <p>The block atlas is one texture the size of the next power of two that holds every sprite,
 * and the road signs were two thirds of CSM's share of it. At 256 px a one-block sign face was
 * 256 texels a block, which only shows within about six blocks, and it is what took the atlas
 * from 8192 x 4096 to 8192 x 8192: twice the launch-time stitching and GPU memory, for detail
 * nobody sees. At 85.3 texels a block a face matches the old one from about nine blocks out. A
 * texture drawn by several plates takes the largest, an animation strip counts one frame (its
 * width), and an OptiFine {@code _e} companion follows its base.</p>
 *
 * <p>The plate is measured the way {@code dev-env-utils/scripts/sign_texture_size.py} measures it
 * (the two must agree): every model a blockstate draws, JSON element faces through their UV
 * window and element rescale, OBJ polygons through their UV Jacobian, gives the number of blocks
 * one texture width or height covers. The fix for a failure is to re-run the generator that
 * writes the texture, each of which sizes its output through that script, or
 * {@code cap_sign_textures.py --apply} for a hand-made one. See "Texture resolution" in
 * {@code assets/docs/TRAFFIC_SIGNS.md}.</p>
 */
class SignTextureSizeTest {

  /** Texels a block of plate: 128 px up to 1.5 blocks, 256 up to 3, 512 up to 6. */
  static final double DENSITY = 256.0 / 3.0;

  /** No sign face is reduced below this, whatever its plate. */
  static final int FLOOR = 128;

  /** A plate within 2% of a threshold counts as on it (a 24-unit plate is 1.5 blocks). */
  static final double TOLERANCE = 1.02;

  static final String PREFIX = "csm:blocks/trafficsigns/";

  /** Not sign faces: the bare metal back sampled in slivers, and a particle-only texture. */
  static final Set<String> EXEMPT = new HashSet<>(Arrays.asList(
      PREFIX + "absolutely_nothing_sign", PREFIX + "pole_dead_end_large"));

  private final Map<String, JsonObject> jsonCache = new HashMap<>();

  @Test
  void everySignTextureIsWithinItsPlateSize() throws IOException, URISyntaxException {
    Map<String, Double> spans = spans();
    assertTrue(spans.size() > 300, "measured only " + spans.size()
        + " sign textures: the resources moved");
    Map<String, File> textures = signTextures();
    assertTrue(textures.size() > 300, "found only " + textures.size() + " sign textures");
    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, File> entry : textures.entrySet()) {
      String name = entry.getKey();
      if (EXEMPT.contains(name)) {
        continue;
      }
      int width = pngWidth(entry.getValue());
      if (width <= FLOOR) {
        continue;
      }
      Double span = spans.get(name);
      if (span == null && name.endsWith("_e")) {
        span = spans.get(name.substring(0, name.length() - 2));
      }
      if (span == null) {
        continue;   // drawn by no model: not in the atlas as a sign face
      }
      int target = Math.max(FLOOR, Math.min(width, ruleSize(span)));
      if (width > target) {
        problems.add(String.format("%s is %d px, its %.2f-block plate needs %d", name, width,
            span, target));
      }
    }
    if (!problems.isEmpty()) {
      fail(problems.size() + " road sign texture(s) above their plate size (re-run the "
          + "generator that writes each, or dev-env-utils/scripts/cap_sign_textures.py --apply):"
          + "\n  " + String.join("\n  ", problems));
    }
  }

  static int ruleSize(double span) {
    double need = DENSITY * span / TOLERANCE;
    int exp = (int) Math.ceil(Math.log(need) / Math.log(2.0) - 1e-12);
    return 1 << Math.max(0, exp);
  }

  // ------------------------------------------------------------------------ the measurement

  private Map<String, Double> spans() throws IOException, URISyntaxException {
    Map<String, Double> out = new TreeMap<>();
    Set<String> seen = new HashSet<>();
    for (File file : files("assets/csm/blockstates", ".json")) {
      String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      if (!text.contains("trafficsigns")) {
        continue;
      }
      JsonObject blockstate = new JsonParser().parse(text).getAsJsonObject();
      for (Map.Entry<String, Map<String, String>> drawn : variants(blockstate)) {
        String model = drawn.getKey();
        Map<String, String> textures = drawn.getValue();
        if (!seen.add(model + "|" + new TreeMap<>(textures))) {
          continue;
        }
        String path = modelPath(model);
        if (path == null) {
          continue;
        }
        List<Object[]> faces = path.endsWith(".obj") ? objFaces(path, textures)
            : jsonFaces(path, textures);
        for (Object[] face : faces) {
          String texture = (String) face[0];
          if (texture.startsWith(PREFIX)) {
            double span = Math.max((Double) face[1], (Double) face[2]);
            out.merge(texture, span, Math::max);
          }
        }
      }
    }
    return out;
  }

  /** (model, blockstate textures) for every model a blockstate draws in the world. */
  private static List<Map.Entry<String, Map<String, String>>> variants(JsonObject blockstate) {
    List<Map.Entry<String, Map<String, String>>> out = new ArrayList<>();
    JsonObject variants = blockstate.has("variants") ? blockstate.getAsJsonObject("variants")
        : new JsonObject();
    if (blockstate.has("forge_marker")) {
      JsonObject defaults = blockstate.has("defaults") ? blockstate.getAsJsonObject("defaults")
          : new JsonObject();
      emit(defaults, new JsonObject(), out);
      for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
        if ("inventory".equals(entry.getKey())) {
          continue;
        }
        JsonElement value = entry.getValue();
        if (value.isJsonArray()) {
          for (JsonElement v : value.getAsJsonArray()) {
            emit(defaults, v.getAsJsonObject(), out);
          }
        } else if (value.isJsonObject()) {
          JsonObject v = value.getAsJsonObject();
          if (v.has("model") || v.has("textures")) {
            emit(defaults, v, out);
          } else {
            for (Map.Entry<String, JsonElement> option : v.entrySet()) {
              if (option.getValue().isJsonObject()) {
                emit(defaults, option.getValue().getAsJsonObject(), out);
              }
            }
          }
        }
      }
    } else {
      for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
        if ("inventory".equals(entry.getKey())) {
          continue;
        }
        JsonElement value = entry.getValue();
        JsonArray list = value.isJsonArray() ? value.getAsJsonArray() : single(value);
        for (JsonElement v : list) {
          JsonObject o = v.getAsJsonObject();
          if (o.has("model")) {
            String m = o.get("model").getAsString();
            out.add(new java.util.AbstractMap.SimpleEntry<>(
                m.contains(":") ? "csm:" + m.substring(m.indexOf(':') + 1) : m, new HashMap<>()));
          }
        }
      }
      if (blockstate.has("multipart")) {
        for (JsonElement part : blockstate.getAsJsonArray("multipart")) {
          JsonElement apply = part.getAsJsonObject().get("apply");
          if (apply == null) {
            continue;
          }
          JsonArray list = apply.isJsonArray() ? apply.getAsJsonArray() : single(apply);
          for (JsonElement v : list) {
            JsonObject o = v.getAsJsonObject();
            if (o.has("model")) {
              String m = o.get("model").getAsString();
              out.add(new java.util.AbstractMap.SimpleEntry<>(
                  m.contains(":") ? m : "minecraft:" + m, new HashMap<>()));
            }
          }
        }
      }
    }
    return out;
  }

  private static JsonArray single(JsonElement value) {
    JsonArray a = new JsonArray();
    a.add(value);
    return a;
  }

  private static void emit(JsonObject defaults, JsonObject variant,
      List<Map.Entry<String, Map<String, String>>> out) {
    String model = variant.has("model") ? variant.get("model").getAsString()
        : defaults.has("model") ? defaults.get("model").getAsString() : null;
    Map<String, String> textures = new LinkedHashMap<>();
    putAll(textures, defaults.getAsJsonObject("textures"));
    putAll(textures, variant.getAsJsonObject("textures"));
    if (model != null) {
      out.add(new java.util.AbstractMap.SimpleEntry<>(model, textures));
    }
    for (JsonElement sub : new JsonElement[] {defaults.get("submodel"), variant.get("submodel")}) {
      if (sub == null) {
        continue;
      }
      if (sub.isJsonPrimitive()) {
        out.add(new java.util.AbstractMap.SimpleEntry<>(sub.getAsString(), textures));
      } else if (sub.isJsonObject()) {
        for (Map.Entry<String, JsonElement> s : sub.getAsJsonObject().entrySet()) {
          if (s.getValue().isJsonObject() && s.getValue().getAsJsonObject().has("model")) {
            JsonObject so = s.getValue().getAsJsonObject();
            Map<String, String> st = new LinkedHashMap<>(textures);
            putAll(st, so.getAsJsonObject("textures"));
            out.add(new java.util.AbstractMap.SimpleEntry<>(so.get("model").getAsString(), st));
          }
        }
      }
    }
  }

  private static void putAll(Map<String, String> into, JsonObject from) {
    if (from == null) {
      return;
    }
    for (Map.Entry<String, JsonElement> e : from.entrySet()) {
      if (e.getValue().isJsonPrimitive()) {
        into.put(e.getKey(), e.getValue().getAsString());
      }
    }
  }

  /** The resource path of a {@code csm:} model reference, or null. */
  private static String modelPath(String ref) {
    if (!ref.startsWith("csm:")) {
      return null;
    }
    String p = ref.substring(4);
    String[] candidates = p.endsWith(".obj")
        ? new String[] {"assets/csm/models/block/" + p, "assets/csm/models/" + p}
        : new String[] {"assets/csm/models/block/" + p + ".json", "assets/csm/models/" + p + ".json"};
    for (String c : candidates) {
      if (SignTextureSizeTest.class.getClassLoader().getResource(c) != null) {
        return c;
      }
    }
    return null;
  }

  private JsonObject json(String resource) throws IOException {
    JsonObject cached = jsonCache.get(resource);
    if (cached == null) {
      try (InputStream in = SignTextureSizeTest.class.getClassLoader().getResourceAsStream(resource);
           Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        cached = new JsonParser().parse(reader).getAsJsonObject();
      }
      jsonCache.put(resource, cached);
    }
    return cached;
  }

  /** A JSON model's elements and textures, its parent chain applied. */
  private Object[] resolve(String resource, int depth) throws IOException {
    JsonObject model = json(resource);
    Map<String, String> textures = new HashMap<>();
    JsonArray elements = null;
    if (model.has("parent") && depth < 20) {
      String parent = model.get("parent").getAsString();
      String pp = parent.contains(":") ? modelPath(parent) : null;
      if (pp != null && pp.endsWith(".json")) {
        Object[] p = resolve(pp, depth + 1);
        elements = (JsonArray) p[0];
        @SuppressWarnings("unchecked")
        Map<String, String> pt = (Map<String, String>) p[1];
        textures.putAll(pt);
      }
    }
    putAll(textures, model.getAsJsonObject("textures"));
    if (model.has("elements")) {
      elements = model.getAsJsonArray("elements");
    }
    return new Object[] {elements == null ? new JsonArray() : elements, textures};
  }

  private static String resolveTexture(String var, List<Map<String, String>> maps) {
    String v = var;
    for (int i = 0; i < 10 && v.startsWith("#"); i++) {
      String key = v.substring(1);
      String next = null;
      for (Map<String, String> m : maps) {
        if (m.containsKey(key)) {
          next = m.get(key);
          break;
        }
      }
      if (next == null) {
        return null;
      }
      v = next;
    }
    if (v.startsWith("#")) {
      return null;
    }
    return v.contains(":") ? v : "minecraft:" + v;
  }

  private List<Object[]> jsonFaces(String resource, Map<String, String> blockstateTextures)
      throws IOException {
    Object[] resolved = resolve(resource, 0);
    JsonArray elements = (JsonArray) resolved[0];
    @SuppressWarnings("unchecked")
    Map<String, String> modelTextures = (Map<String, String>) resolved[1];
    List<Map<String, String>> maps = Arrays.asList(blockstateTextures, modelTextures);
    List<Object[]> out = new ArrayList<>();
    for (JsonElement e : elements) {
      JsonObject element = e.getAsJsonObject();
      double[] from = triple(element.getAsJsonArray("from"));
      double[] to = triple(element.getAsJsonArray("to"));
      double[] d = {Math.abs(to[0] - from[0]), Math.abs(to[1] - from[1]),
          Math.abs(to[2] - from[2])};
      JsonObject rotation = element.getAsJsonObject("rotation");
      if (rotation != null && rotation.has("rescale") && rotation.get("rescale").getAsBoolean()
          && rotation.get("angle").getAsDouble() != 0.0) {
        double s = 1.0 / Math.cos(Math.toRadians(Math.abs(rotation.get("angle").getAsDouble())));
        int axis = "xyz".indexOf(rotation.get("axis").getAsString());
        for (int i = 0; i < 3; i++) {
          if (i != axis) {
            d[i] *= s;
          }
        }
      }
      JsonObject faces = element.getAsJsonObject("faces");
      if (faces == null) {
        continue;
      }
      for (Map.Entry<String, JsonElement> f : faces.entrySet()) {
        JsonObject face = f.getValue().getAsJsonObject();
        if (!face.has("texture")) {
          continue;
        }
        String texture = resolveTexture(face.get("texture").getAsString(), maps);
        if (texture == null) {
          continue;
        }
        String side = f.getKey();
        int a = "east".equals(side) || "west".equals(side) ? 2 : 0;
        int b = "up".equals(side) || "down".equals(side) ? 2 : 1;
        double w = d[a];
        double h = d[b];
        double du;
        double dv;
        if (face.has("uv")) {
          JsonArray uv = face.getAsJsonArray("uv");
          du = Math.abs(uv.get(2).getAsDouble() - uv.get(0).getAsDouble());
          dv = Math.abs(uv.get(3).getAsDouble() - uv.get(1).getAsDouble());
        } else {
          du = Math.min(w, 16);
          dv = Math.min(h, 16);
        }
        int turn = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
        if (turn == 90 || turn == 270) {
          double t = du;
          du = dv;
          dv = t;
        }
        if (du <= 0 || dv <= 0 || w <= 0 || h <= 0) {
          continue;
        }
        out.add(new Object[] {texture, w / du, h / dv});
      }
    }
    return out;
  }

  private static List<Object[]> objFaces(String resource, Map<String, String> blockstateTextures)
      throws IOException {
    Map<String, String> mtl = new HashMap<>();
    List<double[]> verts = new ArrayList<>();
    List<double[]> uvs = new ArrayList<>();
    List<Object[]> polys = new ArrayList<>();
    String folder = resource.substring(0, resource.lastIndexOf('/') + 1);
    String current = null;
    for (String line : lines(resource)) {
      String[] s = line.trim().split("\\s+");
      if (s.length == 0 || s[0].isEmpty()) {
        continue;
      }
      switch (s[0]) {
        case "mtllib":
          if (SignTextureSizeTest.class.getClassLoader().getResource(folder + s[1]) != null) {
            String name = null;
            for (String ml : lines(folder + s[1])) {
              String[] m = ml.trim().split("\\s+");
              if (m.length > 1 && "newmtl".equals(m[0])) {
                name = m[1];
              } else if (m.length > 1 && "map_Kd".equals(m[0]) && name != null) {
                mtl.put(name, m[1]);
              }
            }
          }
          break;
        case "v":
          verts.add(new double[] {Double.parseDouble(s[1]), Double.parseDouble(s[2]),
              Double.parseDouble(s[3])});
          break;
        case "vt":
          uvs.add(new double[] {Double.parseDouble(s[1]), Double.parseDouble(s[2])});
          break;
        case "usemtl":
          current = s[1];
          break;
        case "f":
          int[][] idx = new int[s.length - 1][];
          boolean textured = true;
          for (int i = 1; i < s.length; i++) {
            String[] parts = s[i].split("/");
            if (parts.length < 2 || parts[1].isEmpty()) {
              textured = false;
              break;
            }
            idx[i - 1] = new int[] {Integer.parseInt(parts[0]) - 1, Integer.parseInt(parts[1]) - 1};
          }
          if (textured && current != null && idx.length >= 3) {
            polys.add(new Object[] {current, idx});
          }
          break;
        default:
          break;
      }
    }
    List<Object[]> out = new ArrayList<>();
    for (Object[] poly : polys) {
      String material = (String) poly[0];
      int[][] idx = (int[][]) poly[1];
      String texture = blockstateTextures.get("#" + material);
      if (texture == null) {
        texture = blockstateTextures.get(material);
      }
      if (texture == null) {
        texture = mtl.get(material);
      }
      if (texture != null && texture.startsWith("#")) {
        texture = resolveTexture(texture, Arrays.asList(blockstateTextures));
      }
      if (texture == null) {
        continue;
      }
      double[] p0 = verts.get(idx[0][0]);
      double[] p1 = verts.get(idx[1][0]);
      double[] p2 = verts.get(idx[2][0]);
      double[] t0 = uvs.get(idx[0][1]);
      double[] t1 = uvs.get(idx[1][1]);
      double[] t2 = uvs.get(idx[2][1]);
      double du1 = t1[0] - t0[0];
      double dv1 = t1[1] - t0[1];
      double du2 = t2[0] - t0[0];
      double dv2 = t2[1] - t0[1];
      double det = du1 * dv2 - du2 * dv1;
      if (Math.abs(det) < 1e-9) {
        continue;
      }
      double bu = 0;
      double bv = 0;
      for (int i = 0; i < 3; i++) {
        double e1 = p1[i] - p0[i];
        double e2 = p2[i] - p0[i];
        double dpdu = (e1 * dv2 - e2 * dv1) / det;
        double dpdv = (e2 * du1 - e1 * du2) / det;
        bu += dpdu * dpdu;
        bv += dpdv * dpdv;
      }
      out.add(new Object[] {texture.contains(":") ? texture : "minecraft:" + texture,
          Math.sqrt(bu), Math.sqrt(bv)});
    }
    return out;
  }

  // ------------------------------------------------------------------------ files

  private static List<String> lines(String resource) throws IOException {
    List<String> out = new ArrayList<>();
    try (InputStream in = SignTextureSizeTest.class.getClassLoader().getResourceAsStream(resource);
         BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = r.readLine()) != null) {
        out.add(line);
      }
    }
    return out;
  }

  /** Every file with the suffix in the given resource folder, across every module's tree. */
  private static List<File> files(String folder, String suffix)
      throws IOException, URISyntaxException {
    List<File> out = new ArrayList<>();
    Enumeration<URL> roots = SignTextureSizeTest.class.getClassLoader().getResources(folder);
    Set<String> seen = new HashSet<>();
    while (roots.hasMoreElements()) {
      URL root = roots.nextElement();
      if (!"file".equals(root.getProtocol())) {
        continue;
      }
      File[] list = new File(root.toURI()).listFiles();
      if (list == null) {
        continue;
      }
      Arrays.sort(list);
      for (File f : list) {
        if (f.getName().endsWith(suffix) && seen.add(f.getAbsolutePath())) {
          out.add(f);
        }
      }
    }
    return out;
  }

  private static Map<String, File> signTextures() throws IOException, URISyntaxException {
    Map<String, File> out = new TreeMap<>();
    for (File f : files("assets/csm/textures/blocks/trafficsigns", ".png")) {
      String n = f.getName();
      out.put(PREFIX + n.substring(0, n.length() - 4), f);
    }
    return out;
  }

  /** A PNG's width, from its IHDR chunk. */
  private static int pngWidth(File file) throws IOException {
    try (DataInputStream in = new DataInputStream(Files.newInputStream(file.toPath()))) {
      in.skipBytes(16);
      return in.readInt();
    }
  }

  private static double[] triple(JsonArray array) {
    return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(),
        array.get(2).getAsDouble()};
  }
}
