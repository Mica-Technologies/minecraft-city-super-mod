package com.micatechnologies.minecraft.csm.tools;

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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Audits texture usage across the entire mod by tracing the full reference chain:
 * blockstate → block model → shared model → textures.
 *
 * <p>Unlike BlockItemIntegrityTool (which traces per-block), this tool builds a complete
 * set of all referenced textures and compares against all texture files on disk. It handles
 * Forge blockstate texture overrides (defaults.textures and variant textures) which other
 * tools miss.
 *
 * <p>Reports:
 * <ul>
 *   <li>Unused texture files (on disk but never referenced)</li>
 *   <li>Missing texture files (referenced but not on disk)</li>
 *   <li>Summary statistics</li>
 * </ul>
 */
public class TextureUsageAuditTool {

  private static final String BLOCKSTATE_DIR = "src/main/resources/assets/csm/blockstates";
  private static final String BLOCK_MODELS_DIR = "src/main/resources/assets/csm/models/block";
  private static final String SHARED_MODELS_DIR = "src/main/resources/assets/csm/models/block/shared_models";
  private static final String ITEM_MODELS_DIR = "src/main/resources/assets/csm/models/item";
  private static final String BLOCK_TEXTURES_DIR = "src/main/resources/assets/csm/textures/blocks";
  private static final String ITEM_TEXTURES_DIR = "src/main/resources/assets/csm/textures/items";
  private static final String MOD_PREFIX = "csm:";

  private static final Set<String> referencedBlockTextures = new HashSet<>();
  private static final Set<String> referencedItemTextures = new HashSet<>();
  private static final Set<String> missingTextures = new TreeSet<>();
  private static final AtomicInteger blockstatesScanned = new AtomicInteger(0);
  private static final AtomicInteger modelsScanned = new AtomicInteger(0);

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Texture Usage Audit Tool", args,
        (devEnvironmentPath) -> {
          File blockstateDir = new File(devEnvironmentPath, BLOCKSTATE_DIR);
          File blockModelsDir = new File(devEnvironmentPath, BLOCK_MODELS_DIR);
          File sharedModelsDir = new File(devEnvironmentPath, SHARED_MODELS_DIR);
          File itemModelsDir = new File(devEnvironmentPath, ITEM_MODELS_DIR);
          File blockTexturesDir = new File(devEnvironmentPath, BLOCK_TEXTURES_DIR);
          File itemTexturesDir = new File(devEnvironmentPath, ITEM_TEXTURES_DIR);

          System.out.println("Phase 1: Scanning blockstates for texture references...");
          scanAllBlockstates(blockstateDir, blockModelsDir, sharedModelsDir);

          System.out.println("Phase 2: Scanning block models for texture references...");
          scanAllModels(blockModelsDir, sharedModelsDir);

          System.out.println("Phase 3: Scanning shared models for texture references...");
          scanAllModels(sharedModelsDir, sharedModelsDir);

          System.out.println("Phase 4: Scanning item models for texture references...");
          scanAllModels(itemModelsDir, sharedModelsDir);

          System.out.println("Phase 5: Scanning OBJ/MTL files for texture references...");
          scanMtlFiles(blockModelsDir);

          System.out.println("Phase 6: Comparing against texture files on disk...");

          // Find all texture files on disk
          Set<String> diskBlockTextures = new TreeSet<>();
          Set<String> diskItemTextures = new TreeSet<>();
          collectTextureFiles(blockTexturesDir, blockTexturesDir, diskBlockTextures);
          collectTextureFiles(itemTexturesDir, itemTexturesDir, diskItemTextures);

          // Find unused textures
          Set<String> unusedBlock = new TreeSet<>(diskBlockTextures);
          unusedBlock.removeAll(referencedBlockTextures);
          Set<String> unusedItem = new TreeSet<>(diskItemTextures);
          unusedItem.removeAll(referencedItemTextures);

          // Find missing textures (referenced but not on disk)
          Set<String> missingBlock = new TreeSet<>(referencedBlockTextures);
          missingBlock.removeAll(diskBlockTextures);
          Set<String> missingItem = new TreeSet<>(referencedItemTextures);
          missingItem.removeAll(diskItemTextures);

          // Report
          if (!unusedBlock.isEmpty()) {
            System.out.println("\n--- Unused Block Textures (" + unusedBlock.size() + ") ---");
            for (String tex : unusedBlock) {
              System.out.println("  UNUSED: blocks/" + tex);
            }
          }

          if (!unusedItem.isEmpty()) {
            System.out.println("\n--- Unused Item Textures (" + unusedItem.size() + ") ---");
            for (String tex : unusedItem) {
              System.out.println("  UNUSED: items/" + tex);
            }
          }

          if (!missingBlock.isEmpty() || !missingItem.isEmpty()) {
            System.out.println("\n--- Missing Textures ---");
            for (String tex : missingBlock) {
              System.err.println("  MISSING: blocks/" + tex);
            }
            for (String tex : missingItem) {
              System.err.println("  MISSING: items/" + tex);
            }
          }

          // Summary
          System.out.println("\n========================================");
          System.out.println("Texture Usage Audit Report");
          System.out.println("========================================");
          System.out.println("Blockstates scanned: " + blockstatesScanned.get());
          System.out.println("Models scanned: " + modelsScanned.get());
          System.out.println("Block textures on disk: " + diskBlockTextures.size());
          System.out.println("Block textures referenced: " + referencedBlockTextures.size());
          System.out.println("Item textures on disk: " + diskItemTextures.size());
          System.out.println("Item textures referenced: " + referencedItemTextures.size());
          System.out.println("Unused block textures: " + unusedBlock.size());
          System.out.println("Unused item textures: " + unusedItem.size());
          System.out.println("Missing textures: " + (missingBlock.size() + missingItem.size()));
          System.out.println("========================================\n");
        });
  }

  private static void scanMtlFiles(File blockModelsDir) {
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
                    referencedBlockTextures.add(ref.substring("blocks/".length()));
                  } else if (ref.startsWith("items/")) {
                    referencedItemTextures.add(ref.substring("items/".length()));
                  }
                  modelsScanned.incrementAndGet();
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

  private static void scanAllBlockstates(File blockstateDir, File blockModelsDir,
      File sharedModelsDir) {
    try (Stream<Path> files = Files.walk(blockstateDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".json"))
          .forEach(p -> scanBlockstate(p.toFile()));
    } catch (IOException e) {
      System.err.println("Error scanning blockstates: " + e.getMessage());
    }
  }

  private static void scanBlockstate(File blockstateFile) {
    blockstatesScanned.incrementAndGet();
    try {
      String content = Files.readString(blockstateFile.toPath());
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      // Recursively extract all texture references from the entire JSON tree
      extractTexturesRecursive(json);
    } catch (Exception e) {
      // Skip malformed files silently
    }
  }

  private static void scanAllModels(File modelsDir, File sharedModelsDir) {
    try (Stream<Path> files = Files.walk(modelsDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".json"))
          .filter(p -> !p.startsWith(sharedModelsDir.toPath()) || modelsDir.equals(sharedModelsDir))
          .forEach(p -> scanModel(p.toFile()));
    } catch (IOException e) {
      System.err.println("Error scanning models: " + e.getMessage());
    }
  }

  private static void scanModel(File modelFile) {
    modelsScanned.incrementAndGet();
    try {
      String content = Files.readString(modelFile.toPath());
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      // Extract textures from the model
      if (json.has("textures")) {
        extractTexturesFromBlock(json.getAsJsonObject("textures"));
      }
    } catch (Exception e) {
      // Skip malformed files silently
    }
  }

  private static void extractTexturesRecursive(JsonObject json) {
    for (String key : json.keySet()) {
      JsonElement element = json.get(key);
      if (key.equals("textures") && element.isJsonObject()) {
        extractTexturesFromBlock(element.getAsJsonObject());
      } else if (element.isJsonObject()) {
        extractTexturesRecursive(element.getAsJsonObject());
      } else if (element.isJsonArray()) {
        for (JsonElement arrayElement : element.getAsJsonArray()) {
          if (arrayElement.isJsonObject()) {
            extractTexturesRecursive(arrayElement.getAsJsonObject());
          }
        }
      }
    }
  }

  private static void extractTexturesFromBlock(JsonObject textures) {
    for (String key : textures.keySet()) {
      JsonElement element = textures.get(key);
      if (!element.isJsonPrimitive()) {
        continue;
      }
      String value = element.getAsString();
      if (value.isEmpty() || !value.startsWith(MOD_PREFIX)) {
        continue;
      }
      String stripped = value.substring(MOD_PREFIX.length());
      if (stripped.startsWith("blocks/")) {
        referencedBlockTextures.add(stripped.substring("blocks/".length()));
      } else if (stripped.startsWith("items/")) {
        referencedItemTextures.add(stripped.substring("items/".length()));
      }
    }
  }

  private static void collectTextureFiles(File texturesDir, File baseDir,
      Set<String> textureSet) {
    if (!texturesDir.exists()) {
      return;
    }
    try (Stream<Path> files = Files.walk(texturesDir.toPath())) {
      files.filter(p -> p.toString().endsWith(".png"))
          .forEach(p -> {
            String relative = baseDir.toPath().relativize(p).toString()
                .replace("\\", "/")
                .replace(".png", "");
            textureSet.add(relative);
          });
    } catch (IOException e) {
      System.err.println("Error collecting textures: " + e.getMessage());
    }
  }
}
