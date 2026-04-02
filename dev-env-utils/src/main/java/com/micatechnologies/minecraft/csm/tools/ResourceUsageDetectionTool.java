package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Batch-mode CLI tool that finds ALL unused resources across the mod.
 *
 * <p>Checks for unused:
 * <ul>
 *   <li>Blockstate JSON files with no corresponding Java block class</li>
 *   <li>Block model JSON files not referenced by any blockstate or other model</li>
 *   <li>Item model JSON files not needed (block has inventory variant or no matching block/item)</li>
 *   <li>Block textures (PNG) not referenced by any blockstate, model, or MTL file</li>
 *   <li>Item textures (PNG) not referenced by any blockstate, model, or MTL file</li>
 *   <li>Sound files (.ogg) not referenced in sounds.json</li>
 *   <li>Lang entries with no corresponding block, item, or tab</li>
 * </ul>
 *
 * <p>Usage: pass the project root path as the single CLI argument.
 */
public class ResourceUsageDetectionTool {

  // --- Directory constants ---
  private static final String SOURCE_DIR =
      "src/main/java/com/micatechnologies/minecraft/csm";
  private static final String ASSETS_DIR = "src/main/resources/assets/csm";
  private static final String BLOCKSTATE_DIR = ASSETS_DIR + "/blockstates";
  private static final String BLOCK_MODELS_DIR = ASSETS_DIR + "/models/block";
  private static final String ITEM_MODELS_DIR = ASSETS_DIR + "/models/item";
  private static final String BLOCK_TEXTURES_DIR = ASSETS_DIR + "/textures/blocks";
  private static final String ITEM_TEXTURES_DIR = ASSETS_DIR + "/textures/items";
  private static final String SOUNDS_DIR = ASSETS_DIR + "/sounds";
  private static final String SOUNDS_JSON = ASSETS_DIR + "/sounds.json";
  private static final String LANG_FILE = ASSETS_DIR + "/lang/en_us.lang";

  private static final String MOD_PREFIX = "csm:";

  private static final Pattern REGISTRY_NAME_PATTERN =
      Pattern.compile("return\\s*\"([a-z0-9_]+)\"\\s*;");

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Resource Usage Detection Tool", args,
        (devEnvironmentPath) -> {

          System.out.println("\n=== CSM Resource Usage Detection Tool ===\n");

          // ---------------------------------------------------------------
          // Phase 1: Build all reference sets from source and asset files
          // ---------------------------------------------------------------

          // 1a. Scan Java source for block/item registry names
          System.out.println("Scanning Java source for registry names...");
          Map<String, String> blockRegistry = scanJavaRegistryNames(devEnvironmentPath, true);
          Map<String, String> itemRegistry = scanJavaRegistryNames(devEnvironmentPath, false);
          Set<String> blockSetBaseNames = scanBlockSetBaseNames(devEnvironmentPath);

          // Build expanded block names (include _fence/_slab/_slab_double/_stairs for BlockSets)
          Set<String> allKnownBlockNames = new HashSet<>(blockRegistry.keySet());
          for (String baseName : blockSetBaseNames) {
            allKnownBlockNames.add(baseName + "_fence");
            allKnownBlockNames.add(baseName + "_slab");
            allKnownBlockNames.add(baseName + "_slab_double");
            allKnownBlockNames.add(baseName + "_stairs");
          }

          // 1b. Collect all files on disk
          System.out.println("Collecting files on disk...");
          Set<String> diskBlockstates = collectFileNames(
              new File(devEnvironmentPath, BLOCKSTATE_DIR), ".json", false);
          Set<String> diskBlockModels = collectRelativePaths(
              new File(devEnvironmentPath, BLOCK_MODELS_DIR), ".json");
          Set<String> diskItemModels = collectFileNames(
              new File(devEnvironmentPath, ITEM_MODELS_DIR), ".json", false);
          Set<String> diskBlockTextures = collectRelativePaths(
              new File(devEnvironmentPath, BLOCK_TEXTURES_DIR), ".png");
          Set<String> diskItemTextures = collectRelativePaths(
              new File(devEnvironmentPath, ITEM_TEXTURES_DIR), ".png");
          Set<String> diskSounds = collectFileNames(
              new File(devEnvironmentPath, SOUNDS_DIR), ".ogg", false);

          // 1c. Parse all blockstates to collect model refs, texture refs, and inventory variants
          System.out.println("Parsing blockstates for model and texture references...");
          Set<String> referencedBlockModels = new HashSet<>();
          Set<String> referencedBlockTextures = new HashSet<>();
          Set<String> referencedItemTextures = new HashSet<>();
          Set<String> blockstatesWithInventory = new HashSet<>();

          File blockstateDir = new File(devEnvironmentPath, BLOCKSTATE_DIR);
          if (blockstateDir.isDirectory()) {
            File[] bsFiles = blockstateDir.listFiles(f -> f.getName().endsWith(".json"));
            if (bsFiles != null) {
              for (File bsFile : bsFiles) {
                parseBlockstateForRefs(bsFile, referencedBlockModels,
                    referencedBlockTextures, referencedItemTextures,
                    blockstatesWithInventory);
              }
            }
          }

          // 1d. Parse all block models to collect parent refs and texture refs
          System.out.println("Parsing block models for parent and texture references...");
          File blockModelsDir = new File(devEnvironmentPath, BLOCK_MODELS_DIR);
          Set<String> modelReferencedModels = new HashSet<>();
          parseAllModelsForRefs(blockModelsDir, blockModelsDir,
              modelReferencedModels, referencedBlockTextures, referencedItemTextures);

          // 1e. Parse all item models for texture refs
          System.out.println("Parsing item models for texture references...");
          File itemModelsDir = new File(devEnvironmentPath, ITEM_MODELS_DIR);
          Set<String> itemModelParentRefs = new HashSet<>();
          parseAllModelsForRefs(itemModelsDir, blockModelsDir,
              itemModelParentRefs, referencedBlockTextures, referencedItemTextures);
          // Item model parents that reference block models are also valid references
          modelReferencedModels.addAll(itemModelParentRefs);

          // 1f. Parse MTL files for texture refs
          System.out.println("Parsing OBJ/MTL files for texture references...");
          parseMtlFiles(blockModelsDir, referencedBlockTextures, referencedItemTextures);

          // 1g. Parse sounds.json for sound references
          System.out.println("Parsing sounds.json...");
          Set<String> referencedSounds = parseSoundsJson(
              new File(devEnvironmentPath, SOUNDS_JSON));

          // 1h. Parse lang file
          System.out.println("Parsing lang file...");
          Map<String, String> langBlocks = new TreeMap<>();
          Map<String, String> langItems = new TreeMap<>();
          Set<String> langTabEntries = new TreeSet<>();
          parseLangFile(new File(devEnvironmentPath, LANG_FILE),
              langBlocks, langItems, langTabEntries);

          // 1i. Scan tab files for tab IDs
          Set<String> knownTabIds = scanTabIds(devEnvironmentPath);

          // ---------------------------------------------------------------
          // Phase 2: Combine all model references (blockstate + parent refs)
          // ---------------------------------------------------------------
          Set<String> allReferencedBlockModels = new HashSet<>(referencedBlockModels);
          allReferencedBlockModels.addAll(modelReferencedModels);

          // ---------------------------------------------------------------
          // Phase 3: Compute unused sets
          // ---------------------------------------------------------------

          // 3a. Unused blockstates: on disk but no corresponding Java class
          Set<String> unusedBlockstates = new TreeSet<>();
          for (String bs : diskBlockstates) {
            if (!allKnownBlockNames.contains(bs)) {
              unusedBlockstates.add(bs);
            }
          }

          // 3b. Unused block models: on disk but not referenced by any blockstate or model parent
          Set<String> unusedBlockModels = new TreeSet<>();
          for (String modelPath : diskBlockModels) {
            // Skip OBJ/MTL files - they are handled differently
            if (modelPath.endsWith(".obj") || modelPath.endsWith(".mtl")) {
              continue;
            }
            // The model path on disk is relative like "trafficsigns/foo" (no .json)
            // Blockstate references look like "csm:trafficsigns/foo"
            // Parent references look like "csm:block/trafficsigns/foo"
            // We normalize both to the disk-relative form for comparison
            if (!allReferencedBlockModels.contains(modelPath)) {
              unusedBlockModels.add(modelPath);
            }
          }

          // 3c. Unused item models: on disk but block has inventory variant or no matching
          //     block/item exists
          Set<String> unusedItemModels = new TreeSet<>();
          for (String itemModel : diskItemModels) {
            // An item model is needed if:
            //   - A block exists with this name AND the blockstate has NO inventory variant, OR
            //   - An item exists with this name
            boolean isBlock = allKnownBlockNames.contains(itemModel);
            boolean isItem = itemRegistry.containsKey(itemModel);
            boolean hasInventory = blockstatesWithInventory.contains(itemModel);

            if (isBlock && hasInventory) {
              // Block has inventory variant in blockstate, item model is unnecessary
              unusedItemModels.add(itemModel);
            } else if (!isBlock && !isItem) {
              // No corresponding block or item at all
              unusedItemModels.add(itemModel);
            }
            // If isBlock && !hasInventory -> item model IS needed (keep it)
            // If isItem -> item model IS needed (keep it)
          }

          // 3d. Unused block textures
          Set<String> unusedBlockTextures = new TreeSet<>(diskBlockTextures);
          unusedBlockTextures.removeAll(referencedBlockTextures);

          // 3e. Unused item textures
          Set<String> unusedItemTextures = new TreeSet<>(diskItemTextures);
          unusedItemTextures.removeAll(referencedItemTextures);

          // 3f. Unused sounds
          Set<String> unusedSounds = new TreeSet<>();
          for (String soundFile : diskSounds) {
            if (!referencedSounds.contains(soundFile)) {
              unusedSounds.add(soundFile);
            }
          }

          // 3g. Unused lang entries
          Set<String> unusedLangEntries = new TreeSet<>();
          for (Map.Entry<String, String> entry : langBlocks.entrySet()) {
            String blockId = entry.getKey();
            if (!allKnownBlockNames.contains(blockId)) {
              unusedLangEntries.add("tile." + blockId + ".name=" + entry.getValue());
            }
          }
          for (Map.Entry<String, String> entry : langItems.entrySet()) {
            String itemId = entry.getKey();
            if (!itemRegistry.containsKey(itemId)) {
              unusedLangEntries.add("item." + itemId + ".name=" + entry.getValue());
            }
          }
          for (String tabEntry : langTabEntries) {
            // tabEntry is the full line like "itemGroup.tabfoo=Display Name"
            String tabId = tabEntry.substring("itemGroup.".length(),
                tabEntry.indexOf('='));
            if (!knownTabIds.contains(tabId)) {
              unusedLangEntries.add(tabEntry);
            }
          }

          // ---------------------------------------------------------------
          // Phase 4: Output report
          // ---------------------------------------------------------------

          printSection("Unused Blockstates", unusedBlockstates,
              s -> s + ".json");
          printSection("Unused Block Models", unusedBlockModels,
              s -> s + ".json");
          printSection("Unused Item Models", unusedItemModels,
              s -> s + ".json");
          printSection("Unused Block Textures", unusedBlockTextures,
              s -> s + ".png");
          printSection("Unused Item Textures", unusedItemTextures,
              s -> s + ".png");
          printSection("Unused Sounds", unusedSounds,
              s -> s + ".ogg");
          printSection("Unused Lang Entries", unusedLangEntries,
              s -> s);

          // Summary
          System.out.println("\n========================================");
          System.out.println("Summary:");
          printSummaryLine("Blockstates", diskBlockstates.size(),
              diskBlockstates.size() - unusedBlockstates.size(), unusedBlockstates.size());
          printSummaryLine("Block Models", diskBlockModels.size(),
              diskBlockModels.size() - unusedBlockModels.size(), unusedBlockModels.size());
          printSummaryLine("Item Models", diskItemModels.size(),
              diskItemModels.size() - unusedItemModels.size(), unusedItemModels.size());
          printSummaryLine("Block Textures", diskBlockTextures.size(),
              diskBlockTextures.size() - unusedBlockTextures.size(),
              unusedBlockTextures.size());
          printSummaryLine("Item Textures", diskItemTextures.size(),
              diskItemTextures.size() - unusedItemTextures.size(),
              unusedItemTextures.size());
          printSummaryLine("Sounds", diskSounds.size(),
              diskSounds.size() - unusedSounds.size(), unusedSounds.size());
          int totalLang = langBlocks.size() + langItems.size() + langTabEntries.size();
          printSummaryLine("Lang Entries", totalLang,
              totalLang - unusedLangEntries.size(), unusedLangEntries.size());
          System.out.println("========================================\n");
        });
  }

  // =======================================================================
  // Java source scanning
  // =======================================================================

  /**
   * Scans Java files for getBlockRegistryName() or getItemRegistryName() return values.
   * Returns a map of registry name to class name.
   */
  private static Map<String, String> scanJavaRegistryNames(File devEnvironmentPath,
      boolean blocks) {
    Map<String, String> registry = new TreeMap<>();
    File sourceDir = new File(devEnvironmentPath, SOURCE_DIR);
    String methodName = blocks ? "getBlockRegistryName" : "getItemRegistryName";

    try (Stream<Path> files = Files.walk(sourceDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".java"))
          .forEach(p -> {
            try {
              String content = Files.readString(p);
              if (!content.contains(methodName)) {
                return;
              }
              int methodIdx = content.indexOf(methodName);
              while (methodIdx >= 0) {
                int returnIdx = content.indexOf("return", methodIdx);
                if (returnIdx >= 0 && returnIdx < methodIdx + 200) {
                  Matcher m = REGISTRY_NAME_PATTERN.matcher(
                      content.substring(returnIdx,
                          Math.min(returnIdx + 100, content.length())));
                  if (m.find()) {
                    String className = p.getFileName().toString().replace(".java", "");
                    registry.put(m.group(1), className);
                  }
                }
                methodIdx = content.indexOf(methodName, methodIdx + 1);
              }
            } catch (IOException e) {
              // Skip unreadable files
            }
          });
    } catch (IOException e) {
      System.err.println("Error scanning Java files: " + e.getMessage());
    }
    return registry;
  }

  /**
   * Scans Java files to find block classes extending AbstractBlockSetBasic and returns
   * their base registry names (used to generate _fence/_slab/_slab_double/_stairs variants).
   */
  private static Set<String> scanBlockSetBaseNames(File devEnvironmentPath) {
    Set<String> baseNames = new HashSet<>();
    File sourceDir = new File(devEnvironmentPath, SOURCE_DIR);

    try (Stream<Path> files = Files.walk(sourceDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".java"))
          .forEach(p -> {
            try {
              String content = Files.readString(p);
              if (!content.contains("AbstractBlockSetBasic")) {
                return;
              }
              if (!content.contains("getBlockRegistryName")) {
                return;
              }
              int methodIdx = content.indexOf("getBlockRegistryName");
              while (methodIdx >= 0) {
                int returnIdx = content.indexOf("return", methodIdx);
                if (returnIdx >= 0 && returnIdx < methodIdx + 200) {
                  Matcher m = REGISTRY_NAME_PATTERN.matcher(
                      content.substring(returnIdx,
                          Math.min(returnIdx + 100, content.length())));
                  if (m.find()) {
                    baseNames.add(m.group(1));
                  }
                }
                methodIdx = content.indexOf("getBlockRegistryName", methodIdx + 1);
              }
            } catch (IOException e) {
              // Skip
            }
          });
    } catch (IOException e) {
      System.err.println("Error scanning BlockSet classes: " + e.getMessage());
    }
    return baseNames;
  }

  /**
   * Scans tab Java files to extract known tab IDs (the itemGroup.<id> identifiers).
   */
  private static Set<String> scanTabIds(File devEnvironmentPath) {
    Set<String> tabIds = new HashSet<>();
    File tabsDir = new File(devEnvironmentPath, SOURCE_DIR + "/tabs");
    if (!tabsDir.isDirectory()) {
      return tabIds;
    }

    // Tab IDs are returned by getTabId() or similar. We look for the tab label pattern
    // which maps to itemGroup.<id>. The tab classes typically have a string like "tab..."
    // that matches the lang entry.
    Pattern tabLabelPattern = Pattern.compile("return\\s+\"(tab[a-z]+)\"\\s*;");

    try (Stream<Path> files = Files.walk(tabsDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".java"))
          .forEach(p -> {
            try {
              String content = Files.readString(p);
              Matcher m = tabLabelPattern.matcher(content);
              while (m.find()) {
                tabIds.add(m.group(1));
              }
            } catch (IOException e) {
              // Skip
            }
          });
    } catch (IOException e) {
      System.err.println("Error scanning tab files: " + e.getMessage());
    }
    return tabIds;
  }

  // =======================================================================
  // File collection
  // =======================================================================

  /**
   * Collects file names (without extension) from a directory, optionally recursing.
   */
  private static Set<String> collectFileNames(File dir, String extension,
      boolean recursive) {
    Set<String> names = new TreeSet<>();
    if (!dir.exists() || !dir.isDirectory()) {
      return names;
    }
    if (recursive) {
      try (Stream<Path> files = Files.walk(dir.toPath())) {
        files.filter(p -> p.toString().endsWith(extension))
            .forEach(p -> names.add(
                p.getFileName().toString()
                    .substring(0, p.getFileName().toString().length() - extension.length())));
      } catch (IOException e) {
        System.err.println("Error scanning directory " + dir + ": " + e.getMessage());
      }
    } else {
      File[] files = dir.listFiles(f -> f.getName().endsWith(extension));
      if (files != null) {
        for (File f : files) {
          names.add(f.getName()
              .substring(0, f.getName().length() - extension.length()));
        }
      }
    }
    return names;
  }

  /**
   * Collects relative paths (without extension) from a directory tree, using forward slashes.
   * E.g., for {@code models/block/trafficsigns/foo.json} relative to {@code models/block/},
   * returns {@code "trafficsigns/foo"}.
   */
  private static Set<String> collectRelativePaths(File baseDir, String extension) {
    Set<String> paths = new TreeSet<>();
    if (!baseDir.exists() || !baseDir.isDirectory()) {
      return paths;
    }
    try (Stream<Path> files = Files.walk(baseDir.toPath())) {
      files.filter(p -> p.toString().endsWith(extension))
          .forEach(p -> {
            String relative = baseDir.toPath().relativize(p).toString()
                .replace("\\", "/");
            // Strip extension
            relative = relative.substring(0, relative.length() - extension.length());
            paths.add(relative);
          });
    } catch (IOException e) {
      System.err.println("Error scanning directory " + baseDir + ": " + e.getMessage());
    }
    return paths;
  }

  // =======================================================================
  // Blockstate parsing
  // =======================================================================

  /**
   * Parses a blockstate JSON to extract model references, texture references, and
   * whether it has an inventory variant.
   */
  private static void parseBlockstateForRefs(File bsFile,
      Set<String> modelRefs, Set<String> blockTexRefs, Set<String> itemTexRefs,
      Set<String> inventoryBlocks) {
    try {
      String content = Files.readString(bsFile.toPath());
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      String blockName = bsFile.getName().replace(".json", "");

      // Check for inventory variant
      if (hasInventoryVariant(json)) {
        inventoryBlocks.add(blockName);
      }

      // Recursively extract all model and texture references
      extractRefsFromJson(json, modelRefs, blockTexRefs, itemTexRefs);

    } catch (Exception e) {
      // Skip malformed files
    }
  }

  /**
   * Checks if the blockstate JSON contains an "inventory" variant key.
   */
  private static boolean hasInventoryVariant(JsonObject json) {
    if (json.has("variants")) {
      JsonElement variants = json.get("variants");
      if (variants.isJsonObject()) {
        return variants.getAsJsonObject().has("inventory");
      }
    }
    return false;
  }

  /**
   * Recursively extracts model and texture references from a JSON structure.
   * Handles both Forge blockstate format and vanilla format.
   */
  private static void extractRefsFromJson(JsonObject json,
      Set<String> modelRefs, Set<String> blockTexRefs, Set<String> itemTexRefs) {

    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
      String key = entry.getKey();
      JsonElement element = entry.getValue();

      if (key.equals("model") && element.isJsonPrimitive()) {
        String modelRef = element.getAsString();
        normalizeAndAddModelRef(modelRef, modelRefs);
      } else if (key.equals("textures") && element.isJsonObject()) {
        extractTextureRefs(element.getAsJsonObject(), blockTexRefs, itemTexRefs);
      } else if (element.isJsonObject()) {
        extractRefsFromJson(element.getAsJsonObject(), modelRefs, blockTexRefs, itemTexRefs);
      } else if (element.isJsonArray()) {
        for (JsonElement arrayEl : element.getAsJsonArray()) {
          if (arrayEl.isJsonObject()) {
            extractRefsFromJson(arrayEl.getAsJsonObject(), modelRefs, blockTexRefs, itemTexRefs);
          }
        }
      }
    }
  }

  /**
   * Normalizes a model reference to the disk-relative path form used by
   * {@link #collectRelativePaths}.
   *
   * <p>Blockstate model references use formats like:
   * <ul>
   *   <li>{@code "csm:trafficsigns/foo"} -> disk path {@code models/block/trafficsigns/foo.json}</li>
   *   <li>{@code "csm:block/trafficsigns/foo"} -> same</li>
   *   <li>{@code "cube_all"} -> vanilla model, skip</li>
   * </ul>
   */
  private static void normalizeAndAddModelRef(String modelRef, Set<String> modelRefs) {
    if (modelRef == null || modelRef.isEmpty()) {
      return;
    }
    // Skip vanilla/Minecraft model references
    if (!modelRef.startsWith(MOD_PREFIX)) {
      return;
    }
    String stripped = modelRef.substring(MOD_PREFIX.length());
    // Remove "block/" prefix if present (parent refs use "csm:block/...")
    if (stripped.startsWith("block/")) {
      stripped = stripped.substring("block/".length());
    }
    // Handle OBJ files
    if (stripped.endsWith(".obj")) {
      // OBJ references are special, don't add to JSON model tracking
      return;
    }
    modelRefs.add(stripped);
  }

  /**
   * Extracts texture references from a "textures" JSON object block.
   */
  private static void extractTextureRefs(JsonObject textures,
      Set<String> blockTexRefs, Set<String> itemTexRefs) {
    for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
      JsonElement element = entry.getValue();
      if (!element.isJsonPrimitive()) {
        continue;
      }
      String value = element.getAsString();
      if (value.isEmpty() || !value.startsWith(MOD_PREFIX)) {
        continue;
      }
      String stripped = value.substring(MOD_PREFIX.length());
      if (stripped.startsWith("blocks/")) {
        blockTexRefs.add(stripped.substring("blocks/".length()));
      } else if (stripped.startsWith("items/")) {
        itemTexRefs.add(stripped.substring("items/".length()));
      }
    }
  }

  // =======================================================================
  // Model parsing
  // =======================================================================

  /**
   * Recursively walks a models directory, parsing each JSON model for parent refs
   * and texture refs.
   */
  private static void parseAllModelsForRefs(File modelsDir, File blockModelsBaseDir,
      Set<String> parentModelRefs, Set<String> blockTexRefs, Set<String> itemTexRefs) {
    if (!modelsDir.exists() || !modelsDir.isDirectory()) {
      return;
    }
    try (Stream<Path> files = Files.walk(modelsDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".json"))
          .forEach(p -> {
            try {
              String content = Files.readString(p);
              JsonObject json = JsonParser.parseString(content).getAsJsonObject();

              // Extract parent reference
              if (json.has("parent")) {
                String parent = json.get("parent").getAsString();
                normalizeAndAddModelRef(parent, parentModelRefs);
              }

              // Extract textures
              if (json.has("textures")) {
                extractTextureRefs(json.getAsJsonObject("textures"),
                    blockTexRefs, itemTexRefs);
              }
            } catch (Exception e) {
              // Skip malformed model files
            }
          });
    } catch (IOException e) {
      System.err.println("Error scanning models in " + modelsDir + ": " + e.getMessage());
    }
  }

  // =======================================================================
  // MTL parsing
  // =======================================================================

  /**
   * Parses .mtl files for texture references (map_Kd lines).
   */
  private static void parseMtlFiles(File blockModelsDir,
      Set<String> blockTexRefs, Set<String> itemTexRefs) {
    if (!blockModelsDir.exists()) {
      return;
    }
    try (Stream<Path> files = Files.walk(blockModelsDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".mtl"))
          .forEach(p -> {
            try {
              List<String> lines = Files.readAllLines(p);
              for (String line : lines) {
                line = line.trim();
                if (line.startsWith("map_Kd") && line.contains(MOD_PREFIX)) {
                  String ref = line.substring(line.indexOf(MOD_PREFIX) + MOD_PREFIX.length());
                  if (ref.startsWith("blocks/")) {
                    blockTexRefs.add(ref.substring("blocks/".length()));
                  } else if (ref.startsWith("items/")) {
                    itemTexRefs.add(ref.substring("items/".length()));
                  }
                }
              }
            } catch (IOException e) {
              // Skip
            }
          });
    } catch (IOException e) {
      System.err.println("Error scanning MTL files: " + e.getMessage());
    }
  }

  // =======================================================================
  // Sounds parsing
  // =======================================================================

  /**
   * Parses sounds.json to find all referenced sound file names (without extension).
   * The format is: { "event_id": { "sounds": [ { "name": "csm:filename" } ] } }
   */
  private static Set<String> parseSoundsJson(File soundsJsonFile) {
    Set<String> referenced = new HashSet<>();
    if (!soundsJsonFile.exists()) {
      return referenced;
    }
    try {
      String content = Files.readString(soundsJsonFile.toPath());
      JsonObject root = JsonParser.parseString(content).getAsJsonObject();

      for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
        JsonElement eventEl = entry.getValue();
        if (!eventEl.isJsonObject()) {
          continue;
        }
        JsonObject event = eventEl.getAsJsonObject();
        if (!event.has("sounds")) {
          continue;
        }
        JsonElement soundsEl = event.get("sounds");
        if (!soundsEl.isJsonArray()) {
          continue;
        }
        JsonArray sounds = soundsEl.getAsJsonArray();
        for (JsonElement soundEl : sounds) {
          if (soundEl.isJsonObject()) {
            JsonObject soundObj = soundEl.getAsJsonObject();
            if (soundObj.has("name")) {
              String name = soundObj.get("name").getAsString();
              if (name.startsWith(MOD_PREFIX)) {
                referenced.add(name.substring(MOD_PREFIX.length()));
              }
            }
          } else if (soundEl.isJsonPrimitive()) {
            // Simple string format: "csm:filename"
            String name = soundEl.getAsString();
            if (name.startsWith(MOD_PREFIX)) {
              referenced.add(name.substring(MOD_PREFIX.length()));
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error parsing sounds.json: " + e.getMessage());
    }
    return referenced;
  }

  // =======================================================================
  // Lang file parsing
  // =======================================================================

  /**
   * Parses the lang file to extract block entries, item entries, and tab entries.
   */
  private static void parseLangFile(File langFile, Map<String, String> blocks,
      Map<String, String> items, Set<String> tabEntries) {
    if (!langFile.exists()) {
      return;
    }
    try {
      List<String> lines = Files.readAllLines(langFile.toPath());
      for (String line : lines) {
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        if (line.startsWith("tile.") && line.contains(".name=")) {
          int nameEnd = line.indexOf(".name=");
          String blockId = line.substring("tile.".length(), nameEnd);
          String displayName = line.substring(nameEnd + ".name=".length());
          blocks.put(blockId, displayName);
        } else if (line.startsWith("item.") && line.contains(".name=")) {
          int nameEnd = line.indexOf(".name=");
          String itemId = line.substring("item.".length(), nameEnd);
          String displayName = line.substring(nameEnd + ".name=".length());
          items.put(itemId, displayName);
        } else if (line.startsWith("itemGroup.") && line.contains("=")) {
          tabEntries.add(line);
        }
      }
    } catch (IOException e) {
      System.err.println("Error reading lang file: " + e.getMessage());
    }
  }

  // =======================================================================
  // Output helpers
  // =======================================================================

  @FunctionalInterface
  private interface DisplayFormatter {
    String format(String entry);
  }

  private static void printSection(String title, Set<String> entries,
      DisplayFormatter formatter) {
    System.out.println("\n--- " + title + " (" + entries.size() + ") ---");
    if (entries.isEmpty()) {
      System.out.println("  (none)");
    } else {
      for (String entry : entries) {
        System.out.println("  " + formatter.format(entry));
      }
    }
  }

  private static void printSummaryLine(String category, int onDisk, int referenced,
      int unused) {
    System.out.printf("  %-16s %4d on disk, %4d referenced, %4d unused%n",
        category + ":", onDisk, referenced, unused);
  }
}
