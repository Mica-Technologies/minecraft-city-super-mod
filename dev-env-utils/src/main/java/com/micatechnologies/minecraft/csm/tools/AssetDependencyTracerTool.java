package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Traces the full dependency chain: block class → tab/subsystem → blockstate → models → textures.
 * Outputs a JSON report mapping every block to its subsystem and all referenced asset files,
 * enabling automated reorganization into subsystem-based folder structures.
 *
 * Usage: pass the dev environment root path as the first argument.
 * Output: prints JSON report to stdout, summary to stderr.
 */
public class AssetDependencyTracerTool {

  private static final String ASSETS_PATH = "src/main/resources/assets/csm";
  private static final String SOURCE_PATH = "src/main/java/com/micatechnologies/minecraft/csm";
  private static final String TABS_PATH = SOURCE_PATH + "/tabs";

  // Subsystem packages that map to folder names
  private static final String[] SUBSYSTEMS = {
      "buildingmaterials", "hvac", "lifesafety", "lighting", "novelties",
      "powergrid", "technology", "trafficaccessories", "trafficsignals", "trafficsigns"
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("Asset Dependency Tracer", args, devEnvPath -> {
      File assetsDir = new File(devEnvPath, ASSETS_PATH);
      File sourceDir = new File(devEnvPath, SOURCE_PATH);
      File tabsDir = new File(devEnvPath, TABS_PATH);

      // Step 1: Map block classes to subsystems via tab files
      log("Step 1: Scanning tab files for block→subsystem mapping...");
      Map<String, String> classToSubsystem = mapClassesToSubsystems(tabsDir);
      log("  Found " + classToSubsystem.size() + " block/item classes mapped to subsystems");

      // Step 2: Map block classes to registry names
      log("Step 2: Scanning block classes for registry names...");
      Map<String, String> classToRegistryName = mapClassesToRegistryNames(sourceDir);
      log("  Found " + classToRegistryName.size() + " registry names");

      // Step 3: Build registry name → subsystem mapping
      Map<String, String> registryToSubsystem = new TreeMap<>();
      for (Map.Entry<String, String> entry : classToRegistryName.entrySet()) {
        String className = entry.getKey();
        String registryName = entry.getValue();
        String subsystem = classToSubsystem.get(className);
        if (subsystem != null) {
          registryToSubsystem.put(registryName, subsystem);
        }
      }
      log("  Mapped " + registryToSubsystem.size() + " registry names to subsystems");

      // Step 4: Parse blockstates and trace model/texture references
      log("Step 3: Parsing blockstates...");
      File blockstatesDir = new File(assetsDir, "blockstates");
      File modelsBlockDir = new File(assetsDir, "models/block");
      File texturesDir = new File(assetsDir, "textures/blocks");

      Map<String, BlockAssetInfo> assetMap = new TreeMap<>();
      int blockstateCount = 0;

      for (File bsFile : listJsonFiles(blockstatesDir)) {
        String registryName = bsFile.getName().replace(".json", "");
        String subsystem = registryToSubsystem.getOrDefault(registryName, "UNKNOWN");

        BlockAssetInfo info = new BlockAssetInfo();
        info.registryName = registryName;
        info.subsystem = subsystem;
        info.blockstateFile = relativePath(devEnvPath, bsFile);

        try {
          JsonObject bs = parseJson(bsFile);
          Set<String> modelRefs = new LinkedHashSet<>();
          Set<String> textureRefs = new LinkedHashSet<>();

          extractRefsFromBlockstate(bs, modelRefs, textureRefs);

          // Resolve model files and trace their textures
          for (String modelRef : modelRefs) {
            String modelPath = resolveModelPath(modelsBlockDir, modelRef);
            if (modelPath != null) {
              info.modelFiles.add(modelPath);
              // Parse model for parent chain and textures
              traceModelChain(modelsBlockDir, modelRef, info.modelFiles, textureRefs,
                  new HashSet<>());
            }
            // Check for OBJ companion MTL
            if (modelRef.endsWith(".obj")) {
              String mtlRef = modelRef.replace(".obj", ".mtl");
              File mtlFile = new File(modelsBlockDir,
                  mtlRef.replace("csm:", ""));
              if (mtlFile.exists()) {
                info.modelFiles.add(relativePath(devEnvPath, mtlFile));
                extractTexturesFromMtl(mtlFile, textureRefs);
              }
            }
          }

          // Resolve texture file paths
          for (String texRef : textureRefs) {
            String texPath = resolveTexturePath(texturesDir, texRef);
            if (texPath != null) {
              info.textureFiles.add(texPath);
            }
          }

          // Also store raw refs for debugging
          info.modelRefs.addAll(modelRefs);
          info.textureRefs.addAll(textureRefs);

        } catch (Exception e) {
          info.errors.add("Parse error: " + e.getMessage());
        }

        assetMap.put(registryName, info);
        blockstateCount++;
      }
      log("  Parsed " + blockstateCount + " blockstates");

      // Step 5: Generate report
      log("Step 4: Generating report...");
      generateReport(assetMap, devEnvPath);
    });
  }

  // ---- Tab scanning ----

  private static Map<String, String> mapClassesToSubsystems(File tabsDir) {
    Map<String, String> result = new HashMap<>();
    Pattern initPattern = Pattern.compile(
        "initTab(?:Block|Item)\\(([A-Za-z0-9_]+)\\.class");

    for (File tabFile : Objects.requireNonNull(tabsDir.listFiles(
        f -> f.getName().startsWith("CsmTab") && f.getName().endsWith(".java")))) {
      // Derive subsystem name from tab file name
      String tabName = tabFile.getName().replace("CsmTab", "").replace(".java", "");
      String subsystem = mapTabNameToSubsystem(tabName);

      try (BufferedReader br = new BufferedReader(new FileReader(tabFile))) {
        String line;
        while ((line = br.readLine()) != null) {
          Matcher m = initPattern.matcher(line);
          while (m.find()) {
            result.put(m.group(1), subsystem);
          }
        }
      } catch (IOException e) {
        log("  WARNING: Could not read " + tabFile.getName() + ": " + e.getMessage());
      }
    }
    return result;
  }

  private static String mapTabNameToSubsystem(String tabName) {
    switch (tabName.toLowerCase()) {
      case "buildingmaterials": return "buildingmaterials";
      case "hvac": return "hvac";
      case "lifesafety": return "lifesafety";
      case "lighting": return "lighting";
      case "novelties": return "novelties";
      case "powergrid": return "powergrid";
      case "technology": return "technology";
      case "trafficaccessories": return "trafficaccessories";
      case "trafficsignals": return "trafficsignals";
      case "roadsigns": return "trafficsigns";
      case "none": return "none";
      default: return "unknown_" + tabName.toLowerCase();
    }
  }

  // ---- Registry name scanning ----

  private static Map<String, String> mapClassesToRegistryNames(File sourceDir) {
    Map<String, String> result = new HashMap<>();
    Pattern registryPattern = Pattern.compile(
        "return\\s+\"([a-z0-9_]+)\"\\s*;");
    Pattern classPattern = Pattern.compile(
        "public\\s+class\\s+([A-Za-z0-9_]+)");

    for (String subsystem : SUBSYSTEMS) {
      File subsystemDir = new File(sourceDir, subsystem);
      if (!subsystemDir.isDirectory()) continue;

      for (File javaFile : Objects.requireNonNull(subsystemDir.listFiles(
          f -> f.getName().endsWith(".java")))) {
        String className = null;
        String registryName = null;

        try (BufferedReader br = new BufferedReader(new FileReader(javaFile))) {
          String line;
          boolean inRegistryMethod = false;
          while ((line = br.readLine()) != null) {
            Matcher cm = classPattern.matcher(line);
            if (cm.find() && className == null) {
              className = cm.group(1);
            }
            if (line.contains("getBlockRegistryName") || line.contains("getItemRegistryName")) {
              inRegistryMethod = true;
            }
            if (inRegistryMethod) {
              Matcher rm = registryPattern.matcher(line);
              if (rm.find()) {
                registryName = rm.group(1);
                inRegistryMethod = false;
              }
            }
          }
        } catch (IOException e) {
          // skip
        }

        if (className != null && registryName != null) {
          result.put(className, registryName);
        }
      }
    }

    // Also scan trafficsignals/logic for base classes with registry names
    File logicDir = new File(sourceDir, "trafficsignals/logic");
    if (logicDir.isDirectory()) {
      for (File javaFile : Objects.requireNonNull(logicDir.listFiles(
          f -> f.getName().endsWith(".java")))) {
        // Skip abstract classes for registry names
      }
    }

    return result;
  }

  // ---- Blockstate parsing ----

  private static void extractRefsFromBlockstate(JsonObject bs,
      Set<String> modelRefs, Set<String> textureRefs) {
    // Forge format: defaults + variants
    if (bs.has("defaults")) {
      JsonObject defaults = bs.getAsJsonObject("defaults");
      extractModelAndTextures(defaults, modelRefs, textureRefs);
    }
    if (bs.has("variants")) {
      JsonElement variants = bs.get("variants");
      if (variants.isJsonObject()) {
        for (Map.Entry<String, JsonElement> prop : variants.getAsJsonObject().entrySet()) {
          JsonElement val = prop.getValue();
          if (val.isJsonObject()) {
            extractFromVariantValue(val.getAsJsonObject(), modelRefs, textureRefs);
          } else if (val.isJsonArray()) {
            for (JsonElement arrEl : val.getAsJsonArray()) {
              if (arrEl.isJsonObject()) {
                extractFromVariantValue(arrEl.getAsJsonObject(), modelRefs, textureRefs);
              }
            }
          }
        }
      }
    }
    // Multipart format
    if (bs.has("multipart")) {
      for (JsonElement part : bs.getAsJsonArray("multipart")) {
        if (part.isJsonObject() && part.getAsJsonObject().has("apply")) {
          JsonElement apply = part.getAsJsonObject().get("apply");
          if (apply.isJsonObject()) {
            extractModelAndTextures(apply.getAsJsonObject(), modelRefs, textureRefs);
          }
        }
      }
    }
  }

  private static void extractFromVariantValue(JsonObject obj,
      Set<String> modelRefs, Set<String> textureRefs) {
    extractModelAndTextures(obj, modelRefs, textureRefs);
    // Check for submodels
    if (obj.has("submodel")) {
      JsonObject submodel = obj.getAsJsonObject("submodel");
      for (Map.Entry<String, JsonElement> sub : submodel.entrySet()) {
        if (sub.getValue().isJsonObject()) {
          extractModelAndTextures(sub.getValue().getAsJsonObject(), modelRefs, textureRefs);
        }
      }
    }
  }

  private static void extractModelAndTextures(JsonObject obj,
      Set<String> modelRefs, Set<String> textureRefs) {
    if (obj.has("model")) {
      modelRefs.add(obj.get("model").getAsString());
    }
    if (obj.has("textures")) {
      JsonObject textures = obj.getAsJsonObject("textures");
      for (Map.Entry<String, JsonElement> tex : textures.entrySet()) {
        if (tex.getValue().isJsonPrimitive()) {
          textureRefs.add(tex.getValue().getAsString());
        }
      }
    }
  }

  // ---- Model chain tracing ----

  private static void traceModelChain(File modelsBlockDir, String modelRef,
      Set<String> modelFiles, Set<String> textureRefs, Set<String> visited) {
    if (visited.contains(modelRef)) return;
    visited.add(modelRef);

    String cleanRef = modelRef.replace("csm:", "");
    if (cleanRef.endsWith(".obj")) return; // OBJ handled separately

    File modelFile = new File(modelsBlockDir, cleanRef + ".json");
    if (!modelFile.exists()) {
      // Try without block/ prefix
      modelFile = new File(modelsBlockDir.getParentFile(), "block/" + cleanRef + ".json");
    }
    if (!modelFile.exists()) return;

    try {
      JsonObject model = parseJson(modelFile);

      // Extract textures
      if (model.has("textures")) {
        JsonObject textures = model.getAsJsonObject("textures");
        for (Map.Entry<String, JsonElement> tex : textures.entrySet()) {
          if (tex.getValue().isJsonPrimitive()) {
            textureRefs.add(tex.getValue().getAsString());
          }
        }
      }

      // Follow parent
      if (model.has("parent")) {
        String parent = model.get("parent").getAsString();
        if (parent.startsWith("csm:")) {
          String parentPath = parent.replace("csm:block/", "").replace("csm:", "");
          File parentFile = new File(modelsBlockDir, parentPath + ".json");
          if (parentFile.exists()) {
            modelFiles.add(relativePath(modelsBlockDir.getParentFile().getParentFile()
                .getParentFile().getParentFile().getParentFile(), parentFile));
            traceModelChain(modelsBlockDir, parent, modelFiles, textureRefs, visited);
          }
        }
      }
    } catch (Exception e) {
      // skip parse errors
    }
  }

  // ---- MTL parsing ----

  private static void extractTexturesFromMtl(File mtlFile, Set<String> textureRefs) {
    try (BufferedReader br = new BufferedReader(new FileReader(mtlFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("map_Kd ")) {
          textureRefs.add(line.substring(7).trim());
        }
      }
    } catch (IOException e) {
      // skip
    }
  }

  // ---- Path resolution ----

  private static String resolveModelPath(File modelsBlockDir, String modelRef) {
    String cleanRef = modelRef.replace("csm:", "");
    File modelFile;
    if (cleanRef.endsWith(".obj")) {
      modelFile = new File(modelsBlockDir, cleanRef);
    } else {
      modelFile = new File(modelsBlockDir, cleanRef + ".json");
    }
    if (modelFile.exists()) {
      return relativePath(modelsBlockDir.getParentFile().getParentFile()
          .getParentFile().getParentFile().getParentFile(), modelFile);
    }
    return null;
  }

  private static String resolveTexturePath(File texturesDir, String texRef) {
    if (!texRef.startsWith("csm:blocks/")) return null;
    String cleanRef = texRef.replace("csm:blocks/", "");
    File texFile = new File(texturesDir, cleanRef + ".png");
    if (texFile.exists()) {
      return relativePath(texturesDir.getParentFile().getParentFile()
          .getParentFile().getParentFile(), texFile);
    }
    return null;
  }

  // ---- Report generation ----

  private static void generateReport(Map<String, BlockAssetInfo> assetMap, File devEnvPath) {
    // Summary by subsystem
    Map<String, List<BlockAssetInfo>> bySubsystem = new TreeMap<>();
    Set<String> allModels = new TreeSet<>();
    Set<String> allTextures = new TreeSet<>();
    int unknownCount = 0;

    for (BlockAssetInfo info : assetMap.values()) {
      bySubsystem.computeIfAbsent(info.subsystem, k -> new ArrayList<>()).add(info);
      allModels.addAll(info.modelFiles);
      allTextures.addAll(info.textureFiles);
      if ("UNKNOWN".equals(info.subsystem)) unknownCount++;
    }

    log("\n===== ASSET DEPENDENCY REPORT =====\n");
    log("Total blockstates: " + assetMap.size());
    log("Total unique model files referenced: " + allModels.size());
    log("Total unique texture files referenced: " + allTextures.size());
    log("Blocks with unknown subsystem: " + unknownCount);
    log("");

    for (Map.Entry<String, List<BlockAssetInfo>> entry : bySubsystem.entrySet()) {
      String subsystem = entry.getKey();
      List<BlockAssetInfo> blocks = entry.getValue();

      Set<String> subsystemModels = new TreeSet<>();
      Set<String> subsystemTextures = new TreeSet<>();
      for (BlockAssetInfo info : blocks) {
        subsystemModels.addAll(info.modelFiles);
        subsystemTextures.addAll(info.textureFiles);
      }

      log(String.format("%-25s  blocks: %4d  models: %4d  textures: %4d",
          subsystem, blocks.size(), subsystemModels.size(), subsystemTextures.size()));
    }

    // Output full JSON report to stdout
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    boolean first = true;
    for (Map.Entry<String, BlockAssetInfo> entry : assetMap.entrySet()) {
      if (!first) json.append(",\n");
      first = false;
      BlockAssetInfo info = entry.getValue();
      json.append("  \"").append(info.registryName).append("\": {\n");
      json.append("    \"subsystem\": \"").append(info.subsystem).append("\",\n");
      json.append("    \"blockstate\": \"").append(info.blockstateFile).append("\",\n");
      json.append("    \"modelFiles\": ").append(toJsonArray(info.modelFiles)).append(",\n");
      json.append("    \"textureFiles\": ").append(toJsonArray(info.textureFiles)).append(",\n");
      json.append("    \"modelRefs\": ").append(toJsonArray(info.modelRefs)).append(",\n");
      json.append("    \"textureRefs\": ").append(toJsonArray(info.textureRefs));
      if (!info.errors.isEmpty()) {
        json.append(",\n    \"errors\": ").append(toJsonArray(info.errors));
      }
      json.append("\n  }");
    }
    json.append("\n}\n");
    System.out.println(json);
  }

  private static String toJsonArray(Collection<String> items) {
    if (items.isEmpty()) return "[]";
    return "[" + items.stream()
        .map(s -> "\"" + s.replace("\\", "/") + "\"")
        .collect(Collectors.joining(", ")) + "]";
  }

  // ---- Utility ----

  private static JsonObject parseJson(File file) throws IOException {
    try (FileReader reader = new FileReader(file)) {
      return new JsonParser().parse(reader).getAsJsonObject();
    }
  }

  private static File[] listJsonFiles(File dir) {
    File[] files = dir.listFiles(f -> f.getName().endsWith(".json"));
    return files != null ? files : new File[0];
  }

  private static String relativePath(File base, File file) {
    return base.toPath().relativize(file.toPath()).toString().replace("\\", "/");
  }

  private static void log(String msg) {
    System.err.println(msg);
  }

  // ---- Data class ----

  private static class BlockAssetInfo {
    String registryName;
    String subsystem;
    String blockstateFile;
    Set<String> modelFiles = new LinkedHashSet<>();
    Set<String> textureFiles = new LinkedHashSet<>();
    Set<String> modelRefs = new LinkedHashSet<>();
    Set<String> textureRefs = new LinkedHashSet<>();
    List<String> errors = new ArrayList<>();
  }
}
