package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.loohp.blockmodelrenderer.blending.BlendingModes;
import com.loohp.blockmodelrenderer.render.Hexahedron;
import com.loohp.blockmodelrenderer.render.Model;
import com.loohp.blockmodelrenderer.render.Point3D;
import com.loohp.blockmodelrenderer.utils.TaskCompletion;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;

/**
 * Converts Minecraft JSON block models into rendered PNG images using the BlockModelRenderer library
 * (com.loohp.blockmodelrenderer).
 *
 * <p>This utility parses a standard Minecraft block model JSON (with an {@code elements} array),
 * resolves texture references against a caller-provided texture map, constructs 3D hexahedrons for
 * each element, and renders an isometric inventory-style view to a {@link BufferedImage}.
 */
public class MinecraftModelRenderer {

  /** Canonical face ordering expected by {@link Hexahedron#fromCorners}. */
  private static final String[] FACE_ORDER = {"up", "down", "north", "east", "south", "west"};

  /** A 1x1 fully transparent image used when a face is absent from the model JSON. */
  private static final BufferedImage TRANSPARENT_1X1 = createTransparent1x1();

  /** A 16x16 magenta/black checkerboard used when a texture cannot be resolved. */
  private static final BufferedImage MISSING_TEXTURE = createMissingTexture();

  private static final Gson GSON = new Gson();

  // ── public API ──────────────────────────────────────────────────────────────

  /**
   * Renders a Minecraft JSON block model to a {@link BufferedImage}.
   *
   * @param modelJsonFile the Minecraft JSON model file (must contain an {@code elements} array)
   * @param textureMap    maps texture variable names (e.g. {@code "0"}, {@code "all"}) to PNG files
   *                      on disk. Keys should <em>not</em> include the {@code #} prefix.
   * @param imageSize     width and height of the output image in pixels (e.g. 128 for 128x128)
   *
   * @return the rendered image, or {@code null} if rendering fails
   */
  public static BufferedImage renderModel(File modelJsonFile, Map<String, File> textureMap,
      int imageSize) {
    try {
      // 1. Parse model JSON
      String jsonText = FileUtils.readFileToString(modelJsonFile, StandardCharsets.UTF_8);
      JsonObject root = GSON.fromJson(jsonText, JsonObject.class);

      if (!root.has("elements") || !root.get("elements").isJsonArray()) {
        System.err.println(
            "MinecraftModelRenderer: model has no 'elements' array: "
                + modelJsonFile.getAbsolutePath());
        return null;
      }

      JsonArray elements = root.getAsJsonArray("elements");

      // 2. Build hexahedrons
      List<Hexahedron> hexahedrons = new ArrayList<>();
      for (JsonElement elem : elements) {
        Hexahedron hex = buildHexahedron(elem.getAsJsonObject(), textureMap);
        if (hex != null) {
          hexahedrons.add(hex);
        }
      }

      if (hexahedrons.isEmpty()) {
        System.err.println(
            "MinecraftModelRenderer: no renderable elements in: "
                + modelJsonFile.getAbsolutePath());
        return null;
      }

      // 3. Assemble model
      Model model = new Model(hexahedrons);

      // 4. Apply display transform (gui rotation/translation/scale from model or parent)
      //    Center the model at the origin first (model space is 0-16, center at 8,8,8)
      model.translate(-8.0, -8.0, -8.0);

      // Look for display.gui transform in the model JSON or follow parent chain
      double[] guiRotation = {30, 135, 0};   // Default isometric: tilt + show front
      double[] guiTranslation = {0, 0, 0};
      double[] guiScale = {1, 1, 1};
      JsonObject displayJson = findDisplayGui(modelJsonFile, textureMap);
      if (displayJson != null) {
        if (displayJson.has("rotation")) {
          JsonArray r = displayJson.getAsJsonArray("rotation");
          guiRotation = new double[]{r.get(0).getAsDouble(), r.get(1).getAsDouble(),
              r.get(2).getAsDouble()};
        }
        if (displayJson.has("translation")) {
          JsonArray t = displayJson.getAsJsonArray("translation");
          guiTranslation = new double[]{t.get(0).getAsDouble(), t.get(1).getAsDouble(),
              t.get(2).getAsDouble()};
        }
        if (displayJson.has("scale")) {
          JsonArray s = displayJson.getAsJsonArray("scale");
          guiScale = new double[]{s.get(0).getAsDouble(), s.get(1).getAsDouble(),
              s.get(2).getAsDouble()};
        }
      }

      // Apply gui transform: Minecraft applies rotation, then translation, then scale
      model.rotate(Math.toRadians(guiRotation[0]), Math.toRadians(guiRotation[1]),
          Math.toRadians(guiRotation[2]), true);
      model.translate(guiTranslation[0], guiTranslation[1], guiTranslation[2]);
      model.scale(guiScale[0], guiScale[1], guiScale[2]);

      // 5. Sort faces for correct painter's-algorithm ordering
      model.sortFaces();

      // 6. Set up output image and transform to center/scale the model
      BufferedImage output = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);

      AffineTransform transform = new AffineTransform();
      // Scale model-space units to pixels. A full 16-unit cube spans roughly
      // 16 * sqrt(2) ~ 22.6 units diagonally after rotation; leave a small margin.
      double scaleFactor = imageSize / 26.0;
      transform.translate(imageSize / 2.0, imageSize / 2.0);
      transform.scale(scaleFactor, scaleFactor);

      // 7. Render using a single-thread executor (library requires non-null ExecutorService)
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newSingleThreadExecutor();
      try {
        TaskCompletion task = model.render(output, true, transform, BlendingModes.NORMAL, executor);
        if (task != null) {
          task.join();
        }
      } finally {
        executor.shutdown();
      }

      return output;

    } catch (Exception e) {
      System.err.println(
          "MinecraftModelRenderer: rendering failed for " + modelJsonFile.getAbsolutePath());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Finds the "display" -> "gui" JSON object from a model file or its parent chain.
   * Returns null if no gui display transform is found.
   */
  private static JsonObject findDisplayGui(File modelFile, Map<String, File> textureMap) {
    try {
      int depth = 0;
      File current = modelFile;
      while (current != null && current.exists() && depth < 10) {
        String content = FileUtils.readFileToString(current, StandardCharsets.UTF_8);
        JsonObject json = GSON.fromJson(content, JsonObject.class);

        if (json.has("display")) {
          JsonObject display = json.getAsJsonObject("display");
          if (display.has("gui")) {
            return display.getAsJsonObject("gui");
          }
        }

        // Follow parent
        if (json.has("parent")) {
          String parentRef = json.get("parent").getAsString();
          String stripped = parentRef;
          if (stripped.startsWith("csm:")) stripped = stripped.substring("csm:".length());
          if (stripped.startsWith("block/")) stripped = stripped.substring("block/".length());
          File parentDir = modelFile.getParentFile();
          // Walk up to models/block/ base
          while (parentDir != null && !parentDir.getName().equals("block")) {
            parentDir = parentDir.getParentFile();
          }
          if (parentDir != null) {
            current = new File(parentDir, stripped + ".json");
          } else {
            break;
          }
        } else {
          break;
        }
        depth++;
      }
    } catch (Exception e) {
      // Non-fatal
    }
    return null;
  }

  // ── internals ───────────────────────────────────────────────────────────────

  /**
   * Builds a single {@link Hexahedron} from a JSON element object.
   */
  private static Hexahedron buildHexahedron(JsonObject element, Map<String, File> textureMap) {
    try {
      // Parse from/to
      JsonArray fromArr = element.getAsJsonArray("from");
      JsonArray toArr = element.getAsJsonArray("to");
      Point3D from = new Point3D(
          fromArr.get(0).getAsDouble(),
          fromArr.get(1).getAsDouble(),
          fromArr.get(2).getAsDouble());
      Point3D to = new Point3D(
          toArr.get(0).getAsDouble(),
          toArr.get(1).getAsDouble(),
          toArr.get(2).getAsDouble());

      // Parse faces — build the 6-image array in FACE_ORDER
      JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : null;
      BufferedImage[] faceImages = new BufferedImage[6];
      for (int i = 0; i < FACE_ORDER.length; i++) {
        faceImages[i] = resolveFaceImage(FACE_ORDER[i], faces, textureMap, from, to);
      }

      Hexahedron hex = Hexahedron.fromCorners(from, to, faceImages);

      // Apply element-level rotation if present
      if (element.has("rotation")) {
        JsonObject rot = element.getAsJsonObject("rotation");
        double angle = rot.get("angle").getAsDouble();
        String axis = rot.get("axis").getAsString();

        // Origin defaults to [8, 8, 8] if not specified
        double ox = 8, oy = 8, oz = 8;
        if (rot.has("origin")) {
          JsonArray origin = rot.getAsJsonArray("origin");
          ox = origin.get(0).getAsDouble();
          oy = origin.get(1).getAsDouble();
          oz = origin.get(2).getAsDouble();
        }

        // Translate so origin is at world origin, rotate, translate back
        hex.translate(-ox, -oy, -oz);
        double radians = Math.toRadians(angle);
        switch (axis) {
          case "x":
            hex.rotate(radians, 0, 0, true);
            break;
          case "y":
            hex.rotate(0, radians, 0, true);
            break;
          case "z":
            hex.rotate(0, 0, radians, true);
            break;
          default:
            System.err.println("MinecraftModelRenderer: unknown rotation axis: " + axis);
            break;
        }
        hex.translate(ox, oy, oz);
      }

      return hex;

    } catch (Exception e) {
      System.err.println("MinecraftModelRenderer: failed to parse element");
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Resolves the texture image for a single face of an element.
   *
   * <p>If the face is not present in the JSON, returns a 1x1 transparent image. If the texture
   * cannot be resolved, returns the missing-texture checkerboard. UV coordinates are used to crop
   * the appropriate region from the full texture.
   */
  private static BufferedImage resolveFaceImage(String faceName, JsonObject faces,
      Map<String, File> textureMap, Point3D from, Point3D to) {
    if (faces == null || !faces.has(faceName)) {
      return TRANSPARENT_1X1;
    }

    JsonObject face = faces.getAsJsonObject(faceName);

    // Resolve texture reference
    String textureRef = face.has("texture") ? face.get("texture").getAsString() : null;
    if (textureRef == null || textureRef.isEmpty()) {
      return TRANSPARENT_1X1;
    }

    // Strip leading '#'
    String textureKey = textureRef.startsWith("#") ? textureRef.substring(1) : textureRef;

    BufferedImage fullTexture = loadTexture(textureKey, textureMap);

    // Determine UV coordinates
    double u0, v0, u1, v1;
    if (face.has("uv")) {
      JsonArray uv = face.getAsJsonArray("uv");
      u0 = uv.get(0).getAsDouble();
      v0 = uv.get(1).getAsDouble();
      u1 = uv.get(2).getAsDouble();
      v1 = uv.get(3).getAsDouble();
    } else {
      // Default UV based on face direction and from/to coordinates
      double[] defaultUv = getDefaultUv(faceName, from, to);
      u0 = defaultUv[0];
      v0 = defaultUv[1];
      u1 = defaultUv[2];
      v1 = defaultUv[3];
    }

    // Convert UV (0-16 range) to pixel coordinates
    int texW = fullTexture.getWidth();
    int texH = fullTexture.getHeight();
    int px0 = clamp((int) Math.round(u0 / 16.0 * texW), 0, texW);
    int py0 = clamp((int) Math.round(v0 / 16.0 * texH), 0, texH);
    int px1 = clamp((int) Math.round(u1 / 16.0 * texW), 0, texW);
    int py1 = clamp((int) Math.round(v1 / 16.0 * texH), 0, texH);

    // Ensure non-zero dimensions
    int cropW = Math.max(Math.abs(px1 - px0), 1);
    int cropH = Math.max(Math.abs(py1 - py0), 1);
    int cropX = Math.min(px0, px1);
    int cropY = Math.min(py0, py1);

    // Clamp crop region to texture bounds
    if (cropX + cropW > texW) {
      cropW = texW - cropX;
    }
    if (cropY + cropH > texH) {
      cropH = texH - cropY;
    }
    if (cropW <= 0 || cropH <= 0) {
      return fullTexture;
    }

    return fullTexture.getSubimage(cropX, cropY, cropW, cropH);
  }

  /**
   * Loads a texture by key from the texture map. Falls back to the missing-texture checkerboard if
   * the texture file cannot be read.
   */
  private static BufferedImage loadTexture(String textureKey, Map<String, File> textureMap) {
    File texFile = textureMap.get(textureKey);
    if (texFile == null) {
      // Try "all" as a common fallback
      texFile = textureMap.get("all");
    }
    if (texFile == null || !texFile.exists()) {
      return MISSING_TEXTURE;
    }
    try {
      BufferedImage img = ImageIO.read(texFile);
      return img != null ? img : MISSING_TEXTURE;
    } catch (IOException e) {
      System.err.println("MinecraftModelRenderer: failed to load texture: " + texFile);
      return MISSING_TEXTURE;
    }
  }

  /**
   * Computes default UV coordinates for a face based on the element's from/to coordinates, matching
   * Minecraft's default UV derivation when the {@code uv} field is omitted.
   *
   * @return array of [u0, v0, u1, v1] in 0-16 range
   */
  private static double[] getDefaultUv(String faceName, Point3D from, Point3D to) {
    switch (faceName) {
      case "up":
        return new double[]{from.x, from.z, to.x, to.z};
      case "down":
        return new double[]{from.x, from.z, to.x, to.z};
      case "north":
        return new double[]{16 - to.x, 16 - to.y, 16 - from.x, 16 - from.y};
      case "south":
        return new double[]{from.x, 16 - to.y, to.x, 16 - from.y};
      case "east":
        return new double[]{16 - to.z, 16 - to.y, 16 - from.z, 16 - from.y};
      case "west":
        return new double[]{from.z, 16 - to.y, to.z, 16 - from.y};
      default:
        return new double[]{0, 0, 16, 16};
    }
  }

  /**
   * Clamps an integer value to the range [min, max].
   */
  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  /**
   * Creates a 1x1 fully transparent image for faces that are absent from the model.
   */
  private static BufferedImage createTransparent1x1() {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    img.setRGB(0, 0, 0x00000000);
    return img;
  }

  /**
   * Creates the classic 16x16 magenta/black checkerboard used for missing textures.
   */
  private static BufferedImage createMissingTexture() {
    int size = 16;
    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    Color magenta = new Color(0xFF, 0x00, 0xFF);
    Color black = Color.BLACK;
    int half = size / 2;
    // Top-left and bottom-right: magenta; top-right and bottom-left: black
    g.setColor(magenta);
    g.fillRect(0, 0, half, half);
    g.fillRect(half, half, half, half);
    g.setColor(black);
    g.fillRect(half, 0, half, half);
    g.fillRect(0, half, half, half);
    g.dispose();
    return img;
  }
}
