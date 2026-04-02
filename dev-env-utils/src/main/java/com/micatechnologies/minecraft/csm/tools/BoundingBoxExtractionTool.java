package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

public class BoundingBoxExtractionTool {

  // ==================== Bounding Box Variant Selection ====================

  /**
   * Which variant of bounding box to generate for a given block/model.
   * <ul>
   *   <li>REGULAR — exact values from model geometry, no rounding</li>
   *   <li>ROUNDED — snapped to significant values (-16, 0, 16, 32) in model space</li>
   *   <li>REASONABLE — min size enforced, optional model + bbox rounding</li>
   * </ul>
   */
  enum BBoxVariant {
    REGULAR,
    ROUNDED,
    REASONABLE
  }

  // ==================== Configuration Constants ====================

  /**
   * When true, the tool writes generated bounding box methods directly into block Java source
   * files, replacing existing getBlockBoundingBox() implementations. When false (default), the
   * tool only writes snippet files to the output directory (original behavior).
   */
  private static final boolean WRITE_BACK_LIVE = true;

  /**
   * When true (and WRITE_BACK_LIVE is also true), the tool will add a new getBlockBoundingBox()
   * method to block classes that don't already have one. When false, blocks without an existing
   * method are skipped. This allows covering all calculable bounding boxes in the mod.
   */
  private static final boolean ADD_NEW_METHODS = false;

  /**
   * The default bounding box variant to use when no override is specified.
   */
  private static final BBoxVariant DEFAULT_VARIANT = BBoxVariant.REASONABLE;

  /**
   * Per-block or per-model override map. Keys can be either:
   * <ul>
   *   <li>A block registry name (e.g., "postlight1") — takes precedence</li>
   *   <li>A shared model name without extension (e.g., "ae115_cutoff")</li>
   * </ul>
   * When a key matches, the specified variant is used regardless of global rounding settings.
   */
  private static final Map<String, BBoxVariant> OVERRIDE_MAP = Map.ofEntries(
      // Example overrides (uncomment and modify as needed):
      // Map.entry("postlight1", BBoxVariant.REGULAR),
      // Map.entry("ae115_cutoff", BBoxVariant.ROUNDED)
  );

  // ==================== Rounding Configuration ====================

  // Constants for 'reasonable' variant
  private static final double minBoxSideSize = 1.6;  // Minimum size for clickable space (model units)

  // Toggles for enabling/disabling rounding (apply to REASONABLE variant only)
  private static final boolean ENABLE_MODEL_ROUNDING = false;  // Toggle model rounding (0-16 scale)
  private static final boolean ENABLE_BBOX_ROUNDING = true;    // Toggle bounding box rounding (0.0-1.0 scale)

  // Thresholds for each rounding type
  private static final double modelRoundingThreshold = 0.099999;  // Threshold for model scale rounding
  private static final double bboxRoundingThreshold = 0.05;       // Threshold for bbox scale rounding

  // Constants for 'rounded' variant
  private static final double[] significantValuesRounded = {-16, 0, 16, 32};

  // ==================== Path Constants ====================

  private static final String BLOCK_MODELS_PATH = "src/main/resources/assets/csm/models/block";
  private static final String BLOCKSTATES_PATH = "src/main/resources/assets/csm/blockstates";
  private static final String SOURCE_PATH = "src/main/java/com/micatechnologies/minecraft/csm";
  private static final String OUTPUT_PATH = "dev-env-utils/boundingBoxExtractorToolOutput";

  // ==================== Statistics ====================

  private static int modelsScanned = 0;
  private static int blocksMatched = 0;
  private static int filesUpdated = 0;
  private static int blocksSkipped = 0;

  // ==================== Main Entry Point ====================

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Bounding Box Extractor Tool", args,
        (devEnvironmentPath) -> {
          File blockModelsDir = new File(devEnvironmentPath, BLOCK_MODELS_PATH);
          File outputDir = new File(devEnvironmentPath, OUTPUT_PATH);

          // Collect all shared_models directories across subsystems
          List<File> sharedModelDirs = findSharedModelDirs(blockModelsDir);
          System.out.println("Found " + sharedModelDirs.size() + " shared_models directories");

          // Build mappings for write-back (needed for variant selection even when not writing back)
          Map<String, String> blockstateToModel = buildBlockstateToModelMap(devEnvironmentPath);
          // Invert: model path -> list of registry names
          Map<String, List<String>> modelToRegistryNames = new HashMap<>();
          for (Map.Entry<String, String> entry : blockstateToModel.entrySet()) {
            modelToRegistryNames.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                .add(entry.getKey());
          }

          Map<String, File> registryToJavaFile = null;
          if (WRITE_BACK_LIVE) {
            registryToJavaFile = buildRegistryToJavaFileMap(devEnvironmentPath);
            System.out.println(
                "Mapped " + registryToJavaFile.size() + " block registry names to Java files");
          }

          // Process each shared_models directory
          for (File sharedModelDir : sharedModelDirs) {
            String subsystem = sharedModelDir.getParentFile().getName();
            File subsystemOutputDir = new File(outputDir, subsystem);
            processSharedModelsFolder(sharedModelDir, subsystemOutputDir, subsystem,
                modelToRegistryNames, registryToJavaFile);
          }

          // Print summary
          System.out.println("\n========================================");
          System.out.println("Bounding Box Extraction Tool Report");
          System.out.println("========================================");
          System.out.println("Models scanned: " + modelsScanned);
          System.out.println("Blocks matched: " + blocksMatched);
          if (WRITE_BACK_LIVE) {
            System.out.println("Java files updated: " + filesUpdated);
          }
          System.out.println("Blocks skipped: " + blocksSkipped);
          System.out.println("WRITE_BACK_LIVE: " + WRITE_BACK_LIVE);
          System.out.println("DEFAULT_VARIANT: " + DEFAULT_VARIANT);
          System.out.println("========================================\n");
        });
  }

  // ==================== Directory Discovery ====================

  /**
   * Finds all shared_models/ directories under models/block/\<subsystem\>/.
   */
  private static List<File> findSharedModelDirs(File blockModelsDir) {
    List<File> dirs = new ArrayList<>();
    File[] subsystemDirs = blockModelsDir.listFiles(File::isDirectory);
    if (subsystemDirs != null) {
      for (File subsystemDir : subsystemDirs) {
        File sharedModels = new File(subsystemDir, "shared_models");
        if (sharedModels.exists() && sharedModels.isDirectory()) {
          dirs.add(sharedModels);
        }
      }
    }
    return dirs;
  }

  // ==================== Mapping Chain ====================

  /**
   * Scans all blockstate JSON files to build a map of registryName -> model path fragment.
   * Only includes blockstates that reference shared_models.
   */
  private static Map<String, String> buildBlockstateToModelMap(File devEnvironmentPath) {
    Map<String, String> map = new HashMap<>();
    File blockstatesDir = new File(devEnvironmentPath, BLOCKSTATES_PATH);
    File[] blockstateFiles = blockstatesDir.listFiles(
        (dir, name) -> name.endsWith(".json"));
    if (blockstateFiles == null) {
      return map;
    }

    for (File bsFile : blockstateFiles) {
      try {
        String json = Files.readString(bsFile.toPath());
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("defaults") || !root.getAsJsonObject("defaults").has("model")) {
          continue;
        }
        String model = root.getAsJsonObject("defaults").get("model").getAsString();
        if (model.startsWith("csm:") && model.contains("/shared_models/")) {
          String modelPath = model.substring("csm:".length()); // e.g. "lighting/shared_models/ae115_cutoff"
          String registryName = bsFile.getName().replace(".json", "");
          map.put(registryName, modelPath);
        }
      } catch (Exception e) {
        // Skip malformed blockstates
      }
    }
    return map;
  }

  /**
   * Walks all Java source files and builds a map of registryName -> Java File.
   */
  private static Map<String, File> buildRegistryToJavaFileMap(File devEnvironmentPath) {
    Map<String, File> map = new HashMap<>();
    File sourceDir = new File(devEnvironmentPath, SOURCE_PATH);
    Pattern registryPattern = Pattern.compile(
        "public\\s+String\\s+getBlockRegistryName\\s*\\(\\s*\\)\\s*\\{\\s*return\\s+\"([a-z0-9_]+)\"\\s*;");

    try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
      paths.filter(p -> p.toString().endsWith(".java"))
          .forEach(p -> {
            try {
              String content = Files.readString(p);
              Matcher m = registryPattern.matcher(content);
              if (m.find()) {
                map.put(m.group(1), p.toFile());
              }
            } catch (IOException e) {
              // Skip unreadable files
            }
          });
    } catch (IOException e) {
      System.err.println("Failed to walk source directory: " + e.getMessage());
    }
    return map;
  }

  // ==================== Model Processing ====================

  /**
   * Processes all JSON model files in a shared_models folder.
   */
  private static void processSharedModelsFolder(File modelFolder, File outputFolder,
      String subsystem, Map<String, List<String>> modelToRegistryNames,
      Map<String, File> registryToJavaFile) {
    System.out.println("\nProcessing: " + subsystem + "/shared_models/");
    File[] files = modelFolder.listFiles();
    if (files == null) {
      return;
    }

    for (File modelFile : files) {
      if (modelFile.isDirectory()) {
        // Recurse into subdirectories (e.g., trafficsignals/shared_models/backplates/)
        processSharedModelsFolder(modelFile, new File(outputFolder, modelFile.getName()),
            subsystem, modelToRegistryNames, registryToJavaFile);
      } else if (modelFile.getName().endsWith(".json")) {
        try {
          String jsonContent = FileUtils.readFileToString(modelFile);
          JsonObject modelJson = new Gson().fromJson(jsonContent, JsonObject.class);

          if (!modelJson.has("elements")) {
            continue; // Skip models without geometry (parent-only wrappers)
          }

          modelsScanned++;
          String modelName = modelFile.getName().replace(".json", "");

          // Determine the model path fragment for mapping (e.g., "lighting/shared_models/ae115_cutoff")
          String modelPathFragment = subsystem + "/shared_models/" + modelName;

          // Extract raw bounding box
          double[][] rawBBox = extractRawBoundingBox(modelJson);
          double[] rawFrom = rawBBox[0];
          double[] rawTo = rawBBox[1];

          // Write all 3 variant files to output (existing behavior)
          writeOutputFiles(outputFolder, modelFile.getName(), rawFrom, rawTo);

          // Write back to Java files if enabled
          if (WRITE_BACK_LIVE && registryToJavaFile != null) {
            List<String> registryNames = modelToRegistryNames.getOrDefault(modelPathFragment,
                List.of());
            if (registryNames.isEmpty()) {
              // No blockstates reference this model — skip silently (many shared models are
              // parents of other models, not directly referenced by blockstates)
            } else {
              for (String registryName : registryNames) {
                blocksMatched++;
                File javaFile = registryToJavaFile.get(registryName);
                if (javaFile == null) {
                  System.out.println(
                      "  SKIP (no Java class): " + registryName + " -> " + modelPathFragment);
                  blocksSkipped++;
                  continue;
                }
                BBoxVariant variant = selectVariant(registryName, modelName);
                String bboxCode = generateBBoxCode(rawFrom, rawTo, variant);
                boolean updated = writeBackToJavaFile(javaFile, bboxCode);
                if (updated) {
                  filesUpdated++;
                  System.out.println("  UPDATED: " + registryName + " (" + variant + ") -> "
                      + javaFile.getName());
                } else if (ADD_NEW_METHODS) {
                  boolean added = addMethodToJavaFile(javaFile, bboxCode);
                  if (added) {
                    filesUpdated++;
                    System.out.println("  ADDED: " + registryName + " (" + variant + ") -> "
                        + javaFile.getName());
                  } else {
                    blocksSkipped++;
                    System.out.println(
                        "  SKIP (failed to add): " + registryName + " -> " + javaFile.getName());
                  }
                } else {
                  blocksSkipped++;
                  System.out.println(
                      "  SKIP (no existing method): " + registryName + " -> " + javaFile.getName());
                }
              }
            }
          }
        } catch (Exception e) {
          System.err.println("  Failed to process: " + modelFile.getName() + " — " + e.getMessage());
        }
      }
    }
  }

  // ==================== Bounding Box Extraction ====================

  /**
   * Extracts the raw bounding box from a model's elements array.
   *
   * @return double[2][3] where [0] = from (minX, minY, minZ), [1] = to (maxX, maxY, maxZ)
   */
  private static double[][] extractRawBoundingBox(JsonObject blockModelJson) {
    double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY,
        minZ = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY,
        maxZ = Double.NEGATIVE_INFINITY;

    JsonArray elements = blockModelJson.getAsJsonArray("elements");
    for (JsonElement element : elements) {
      JsonObject cuboid = element.getAsJsonObject();
      double[] from = toArray(cuboid.getAsJsonArray("from"));
      double[] to = toArray(cuboid.getAsJsonArray("to"));

      minX = Math.min(minX, from[0]);
      minY = Math.min(minY, from[1]);
      minZ = Math.min(minZ, from[2]);
      maxX = Math.max(maxX, to[0]);
      maxY = Math.max(maxY, to[1]);
      maxZ = Math.max(maxZ, to[2]);
    }

    return new double[][]{{minX, minY, minZ}, {maxX, maxY, maxZ}};
  }

  // ==================== Variant Selection ====================

  /**
   * Selects which bounding box variant to use for a given block/model.
   * Priority: registry name override > model name override > DEFAULT_VARIANT.
   */
  private static BBoxVariant selectVariant(String registryName, String modelName) {
    if (OVERRIDE_MAP.containsKey(registryName)) {
      return OVERRIDE_MAP.get(registryName);
    }
    if (OVERRIDE_MAP.containsKey(modelName)) {
      return OVERRIDE_MAP.get(modelName);
    }
    return DEFAULT_VARIANT;
  }

  // ==================== Code Generation ====================

  /**
   * Generates the Java code snippet for a specific bounding box variant.
   */
  private static String generateBBoxCode(double[] rawFrom, double[] rawTo, BBoxVariant variant) {
    double[] from;
    double[] to;

    switch (variant) {
      case REGULAR:
        from = rawFrom;
        to = rawTo;
        return fromJsonToJavaCode(from, to, false);

      case ROUNDED:
        from = new double[]{
            roundToSignificantValue(rawFrom[0]),
            roundToSignificantValue(rawFrom[1]),
            roundToSignificantValue(rawFrom[2])
        };
        to = new double[]{
            roundToSignificantValue(rawTo[0]),
            roundToSignificantValue(rawTo[1]),
            roundToSignificantValue(rawTo[2])
        };
        return fromJsonToJavaCode(from, to, false);

      case REASONABLE:
      default:
        from = adjustBoxSide(rawFrom[0], rawFrom[1], rawFrom[2],
            rawTo[0], rawTo[1], rawTo[2], true);
        to = adjustBoxSide(rawFrom[0], rawFrom[1], rawFrom[2],
            rawTo[0], rawTo[1], rawTo[2], false);
        return fromJsonToJavaCode(from, to, true);
    }
  }

  // ==================== Output File Writing (Original Behavior) ====================

  /**
   * Writes all 3 variant files to the output directory (existing behavior preserved).
   */
  private static void writeOutputFiles(File outputFolder, String modelFileName,
      double[] rawFrom, double[] rawTo) throws IOException {
    // Regular
    File regularFile = new File(outputFolder, modelFileName);
    String regularCode = generateBBoxCode(rawFrom, rawTo, BBoxVariant.REGULAR);
    FileUtils.writeStringToFile(regularFile, regularCode, false);

    // Rounded
    File roundedFile = new File(outputFolder,
        modelFileName.replace(".json", "_rounded.json"));
    String roundedCode = generateBBoxCode(rawFrom, rawTo, BBoxVariant.ROUNDED);
    FileUtils.writeStringToFile(roundedFile, roundedCode, false);

    // Reasonable
    File reasonableFile = new File(outputFolder,
        modelFileName.replace(".json", "_reasonable.json"));
    String reasonableCode = generateBBoxCode(rawFrom, rawTo, BBoxVariant.REASONABLE);
    FileUtils.writeStringToFile(reasonableFile, reasonableCode, false);
  }

  // ==================== Write-Back to Java Source ====================

  /**
   * Pattern to find the @Override annotation followed by the getBlockBoundingBox signature.
   * Used to locate the method start; brace counting handles finding the end.
   */
  private static final Pattern BBOX_SIGNATURE_PATTERN = Pattern.compile(
      "@Override\\s*\\r?\\n\\s*public\\s+AxisAlignedBB\\s+getBlockBoundingBox\\s*\\([^)]*\\)\\s*\\{");

  /**
   * Replaces the existing getBlockBoundingBox method in a Java source file.
   * Uses brace-counting to correctly handle methods with switch/if/nested blocks.
   *
   * @return true if the method was found and replaced, false if not found.
   */
  private static boolean writeBackToJavaFile(File javaFile, String newMethodCode) {
    try {
      String content = Files.readString(javaFile.toPath());
      Matcher matcher = BBOX_SIGNATURE_PATTERN.matcher(content);

      if (!matcher.find()) {
        return false; // No existing method to replace
      }

      // Walk backwards from @Override to find optional Javadoc
      int methodStart = matcher.start();
      int searchPos = methodStart - 1;
      // Skip whitespace before @Override
      while (searchPos >= 0 && (content.charAt(searchPos) == ' ' || content.charAt(searchPos) == '\t'
          || content.charAt(searchPos) == '\n' || content.charAt(searchPos) == '\r')) {
        searchPos--;
      }
      // Check if we're at the end of a Javadoc comment (*/)
      if (searchPos >= 1 && content.charAt(searchPos - 1) == '*'
          && content.charAt(searchPos) == '/') {
        // Find the start of this Javadoc (/**)
        int javadocEnd = searchPos;
        int javadocStart = content.lastIndexOf("/**", javadocEnd);
        if (javadocStart >= 0) {
          // Verify nothing but whitespace between /** ... */ and @Override
          String between = content.substring(javadocEnd + 1, methodStart);
          if (between.trim().isEmpty()) {
            // Include leading whitespace on the Javadoc line
            while (javadocStart > 0 && content.charAt(javadocStart - 1) == ' '
                || javadocStart > 0 && content.charAt(javadocStart - 1) == '\t') {
              javadocStart--;
            }
            methodStart = javadocStart;
          }
        }
      }

      // Find the method end by counting braces from the opening {
      int openBrace = matcher.end() - 1; // Position of the opening {
      int braceDepth = 1;
      int pos = openBrace + 1;
      boolean inString = false;
      boolean inLineComment = false;
      boolean inBlockComment = false;

      while (pos < content.length() && braceDepth > 0) {
        char c = content.charAt(pos);
        char next = pos + 1 < content.length() ? content.charAt(pos + 1) : 0;

        if (inLineComment) {
          if (c == '\n') {
            inLineComment = false;
          }
        } else if (inBlockComment) {
          if (c == '*' && next == '/') {
            inBlockComment = false;
            pos++;
          }
        } else if (inString) {
          if (c == '\\') {
            pos++; // Skip escaped character
          } else if (c == '"') {
            inString = false;
          }
        } else {
          if (c == '/' && next == '/') {
            inLineComment = true;
            pos++;
          } else if (c == '/' && next == '*') {
            inBlockComment = true;
            pos++;
          } else if (c == '"') {
            inString = true;
          } else if (c == '{') {
            braceDepth++;
          } else if (c == '}') {
            braceDepth--;
          }
        }
        pos++;
      }

      if (braceDepth != 0) {
        System.err.println("  ERROR: Unbalanced braces in " + javaFile.getName());
        return false;
      }

      int methodEnd = pos; // Position after the closing }

      String updated = content.substring(0, methodStart)
          + newMethodCode
          + content.substring(methodEnd);

      Files.writeString(javaFile.toPath(), updated);
      return true;
    } catch (IOException e) {
      System.err.println("  ERROR writing to " + javaFile.getName() + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Adds a new getBlockBoundingBox method to a Java source file that doesn't have one.
   * Inserts the method before the last closing brace of the class.
   *
   * @return true if the method was successfully added, false on failure.
   */
  private static boolean addMethodToJavaFile(File javaFile, String newMethodCode) {
    try {
      String content = Files.readString(javaFile.toPath());

      // Find the last } in the file (the class closing brace)
      int lastBrace = content.lastIndexOf('}');
      if (lastBrace < 0) {
        System.err.println("  ERROR: No closing brace found in " + javaFile.getName());
        return false;
      }

      // Insert the new method before the class closing brace, with a blank line separator
      String updated = content.substring(0, lastBrace)
          + "\r\n" + newMethodCode + "\r\n"
          + content.substring(lastBrace);

      Files.writeString(javaFile.toPath(), updated);
      return true;
    } catch (IOException e) {
      System.err.println("  ERROR writing to " + javaFile.getName() + ": " + e.getMessage());
      return false;
    }
  }

  // ==================== Rounding Methods (Preserved from Original) ====================

  private static double roundToSignificantValue(double value) {
    double closestValue = significantValuesRounded[0];
    double smallestDifference = Math.abs(value - closestValue);
    for (int i = 1; i < significantValuesRounded.length; i++) {
      double difference = Math.abs(value - significantValuesRounded[i]);
      if (difference < smallestDifference) {
        smallestDifference = difference;
        closestValue = significantValuesRounded[i];
      }
    }
    return closestValue;
  }

  private static double[] adjustBoxSide(double minX, double minY, double minZ, double maxX,
      double maxY, double maxZ, boolean isFrom) {
    double[] adjustedValues = new double[3];
    double[] mins = {minX, minY, minZ};
    double[] maxs = {maxX, maxY, maxZ};

    for (int i = 0; i < 3; i++) {
      double size = maxs[i] - mins[i];

      // Enforce minimum size before any rounding
      if (size < minBoxSideSize) {
        if (isFrom) {
          adjustedValues[i] = mins[i];
          maxs[i] = mins[i] + minBoxSideSize;
        } else {
          mins[i] = maxs[i] - minBoxSideSize;
          adjustedValues[i] = maxs[i];
        }
      } else if (ENABLE_MODEL_ROUNDING) {
        double originalValue = isFrom ? mins[i] : maxs[i];
        double roundedValue = roundToNearestModel(originalValue);

        if (isFrom && roundedValue >= maxs[i]) {
          adjustedValues[i] = maxs[i] - minBoxSideSize;
        } else if (!isFrom && roundedValue <= mins[i]) {
          adjustedValues[i] = mins[i] + minBoxSideSize;
        } else {
          adjustedValues[i] = roundedValue;
        }
      } else {
        adjustedValues[i] = isFrom ? mins[i] : maxs[i];
      }
    }
    return adjustedValues;
  }

  private static double roundToNearestModel(double value) {
    double[] roundingValuesModel = {0.0, 4.0, 8.0, 12.0, 16.0};
    double closestValue = roundingValuesModel[0];
    double smallestDifference = Math.abs(value - closestValue);
    for (double roundingValue : roundingValuesModel) {
      double difference = Math.abs(value - roundingValue);
      if (difference < smallestDifference && difference <= modelRoundingThreshold) {
        smallestDifference = difference;
        closestValue = roundingValue;
      }
    }
    return closestValue;
  }

  private static double roundToPrecision(double value, int decimalPlaces) {
    double scale = Math.pow(10, decimalPlaces);
    return Math.round(value * scale) / scale;
  }

  private static double roundToNearestBbox(double value, double min, double max, boolean isMin) {
    double[] roundingValuesBbox = {0.0, 0.25, 0.5, 0.75, 1.0};
    double closestValue = roundingValuesBbox[0];
    double smallestDifference = Math.abs(value - closestValue);
    for (double roundingValue : roundingValuesBbox) {
      double difference = Math.abs(value - roundingValue);
      if (difference < smallestDifference && difference <= bboxRoundingThreshold) {
        smallestDifference = difference;
        closestValue = roundingValue;
      }
    }
    if (smallestDifference <= bboxRoundingThreshold) {
      if (isMin && closestValue >= max) {
        return max - Math.max(minBoxSideSize / 16.0, Math.abs(max - min));
      } else if (!isMin && closestValue <= min) {
        return min + Math.max(minBoxSideSize / 16.0, Math.abs(max - min));
      }
      return closestValue;
    }
    return value;
  }

  // ==================== Utility Methods ====================

  private static double[] toArray(JsonArray jsonArray) {
    return new double[]{
        jsonArray.get(0).getAsDouble(),
        jsonArray.get(1).getAsDouble(),
        jsonArray.get(2).getAsDouble()
    };
  }

  private static String fromJsonToJavaCode(double[] from, double[] to,
      boolean isReasonableVariant) {
    double x1 = from[0] / 16.0;
    double y1 = from[1] / 16.0;
    double z1 = from[2] / 16.0;
    double x2 = to[0] / 16.0;
    double y2 = to[1] / 16.0;
    double z2 = to[2] / 16.0;

    if (isReasonableVariant && ENABLE_BBOX_ROUNDING) {
      x1 = roundToNearestBbox(x1, x1, x2, true);
      y1 = roundToNearestBbox(y1, y1, y2, true);
      z1 = roundToNearestBbox(z1, z1, z2, true);
      x2 = roundToNearestBbox(x2, x1, x2, false);
      y2 = roundToNearestBbox(y2, y1, y2, false);
      z2 = roundToNearestBbox(z2, z1, z2, false);
    }

    x1 = roundToPrecision(x1, 6);
    y1 = roundToPrecision(y1, 6);
    z1 = roundToPrecision(z1, 6);
    x2 = roundToPrecision(x2, 6);
    y2 = roundToPrecision(y2, 6);
    z2 = roundToPrecision(z2, 6);

    return String.format(
        "    /**\r\n"
            + "     * Retrieves the bounding box of the block.\r\n"
            + "     *\r\n"
            + "     * @param state  the block state\r\n"
            + "     * @param source the block access\r\n"
            + "     * @param pos    the block position\r\n"
            + "     *\r\n"
            + "     * @return The bounding box of the block.\r\n"
            + "     *\r\n"
            + "     * @since 1.0\r\n"
            + "     */\r\n"
            + "    @Override\r\n"
            + "    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {\r\n"
            + "        return new AxisAlignedBB(%f, %f, %f, %f, %f, %f);\r\n"
            + "    }", x1, y1, z1, x2, y2, z2);
  }
}
