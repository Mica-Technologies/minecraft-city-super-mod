package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.AssetFolder;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Validates all Forge blockstate files for structural correctness and valid references.
 *
 * <p>Checks performed:
 * <ul>
 *   <li>Valid JSON structure</li>
 *   <li>forge_marker presence and value</li>
 *   <li>defaults.model references resolve to existing files</li>
 *   <li>Variant model references resolve to existing files</li>
 *   <li>Texture references in defaults and variants resolve to existing files</li>
 *   <li>inventory and normal variants present</li>
 *   <li>Facing variants have valid rotation values</li>
 * </ul>
 */
public class ForgeBlockstateValidator {

  // Relative to a tree's assets/csm. Core and every module contribute a copy of each of these
  // folders and the game merges them, so the validator resolves against all of them at once.
  private static final String BLOCKSTATE_DIR = "blockstates";
  private static final String BLOCK_MODELS_DIR = "models/block";
  private static final String SHARED_MODELS_DIR = "models/block/shared_models";
  private static final String BLOCK_TEXTURES_DIR = "textures/blocks";
  private static final String ITEM_TEXTURES_DIR = "textures/items";
  private static final String MOD_PREFIX = "csm:";

  private static final AtomicInteger totalFiles = new AtomicInteger(0);
  private static final AtomicInteger forgeFiles = new AtomicInteger(0);
  private static final AtomicInteger vanillaFiles = new AtomicInteger(0);
  private static final AtomicInteger errorCount = new AtomicInteger(0);
  private static final AtomicInteger warningCount = new AtomicInteger(0);
  private static final AtomicInteger validatedModels = new AtomicInteger(0);
  private static final AtomicInteger validatedTextures = new AtomicInteger(0);

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Forge Blockstate Validator Tool", args,
        (devEnvironmentPath) -> {
          CsmLayout layout = new CsmLayout(devEnvironmentPath);
          AssetFolder blockModelsDir = AssetFolder.ofAsset(layout, BLOCK_MODELS_DIR);
          AssetFolder sharedModelsDir = AssetFolder.ofAsset(layout, SHARED_MODELS_DIR);
          AssetFolder blockTexturesDir = AssetFolder.ofAsset(layout, BLOCK_TEXTURES_DIR);
          AssetFolder itemTexturesDir = AssetFolder.ofAsset(layout, ITEM_TEXTURES_DIR);

          // One blockstate folder per tree; every one of them ships blockstates for the mod.
          for (File blockstateDir : layout.assetDirs(BLOCKSTATE_DIR)) {
            try (Stream<Path> files = Files.walk(blockstateDir.toPath())) {
              files.filter(p -> p.toString().endsWith(".json"))
                  .forEach(p -> validateBlockstate(p.toFile(), blockModelsDir, sharedModelsDir,
                      blockTexturesDir, itemTexturesDir));
            }
          }

          // Print report
          System.out.println("\n========================================");
          System.out.println("Forge Blockstate Validator Report");
          System.out.println("========================================");
          System.out.println("Total blockstate files: " + totalFiles.get());
          System.out.println("  Forge format: " + forgeFiles.get());
          System.out.println("  Vanilla/multipart: " + vanillaFiles.get());
          System.out.println("Models validated: " + validatedModels.get());
          System.out.println("Textures validated: " + validatedTextures.get());
          System.out.println("Errors: " + errorCount.get());
          System.out.println("Warnings: " + warningCount.get());
          System.out.println("========================================\n");
        });
  }

  private static void validateBlockstate(File blockstateFile, AssetFolder blockModelsDir,
      AssetFolder sharedModelsDir, AssetFolder blockTexturesDir, AssetFolder itemTexturesDir) {
    totalFiles.incrementAndGet();
    String name = blockstateFile.getName().replace(".json", "");

    try {
      String content = Files.readString(blockstateFile.toPath());
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();

      // Check if Forge format
      if (!json.has("forge_marker")) {
        vanillaFiles.incrementAndGet();
        // Vanilla/multipart — check for basic structure
        if (!json.has("variants") && !json.has("multipart")) {
          logError(name, "Neither 'variants' nor 'multipart' found in vanilla blockstate");
        }
        return;
      }

      forgeFiles.incrementAndGet();
      JsonObject variants = json.has("variants") ? json.getAsJsonObject("variants") : null;

      // Check for inventory variant
      if (variants == null || !variants.has("inventory")) {
        logWarning(name, "Missing 'inventory' variant");
      }

      // Check for normal variant
      if (variants == null || !variants.has("normal")) {
        logWarning(name, "Missing 'normal' variant");
      }

      // Validate defaults.model
      if (json.has("defaults") && json.getAsJsonObject("defaults").has("model")) {
        String model = json.getAsJsonObject("defaults").get("model").getAsString();
        validateModelReference(name, model, blockModelsDir, sharedModelsDir, "defaults");
      }

      // Validate defaults.textures
      if (json.has("defaults") && json.getAsJsonObject("defaults").has("textures")) {
        JsonObject textures = json.getAsJsonObject("defaults").getAsJsonObject("textures");
        validateTextureReferences(name, textures, blockTexturesDir, itemTexturesDir, "defaults");
      }

      // Validate variant model and texture references
      if (variants != null) {
        validateVariants(name, variants, blockModelsDir, sharedModelsDir, blockTexturesDir,
            itemTexturesDir);
      }

      // Validate facing variants have valid rotations
      if (variants != null && variants.has("facing")) {
        validateFacingVariant(name, variants.get("facing"));
      }

    } catch (Exception e) {
      logError(name, "Failed to parse: " + e.getMessage());
    }
  }

  private static void validateVariants(String name, JsonObject variants, AssetFolder blockModelsDir,
      AssetFolder sharedModelsDir, AssetFolder blockTexturesDir, AssetFolder itemTexturesDir) {
    for (String key : variants.keySet()) {
      if (key.equals("inventory") || key.equals("normal")) {
        // Validate inventory/normal array entries
        JsonElement element = variants.get(key);
        if (element.isJsonArray()) {
          for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonObject()) {
              JsonObject obj = entry.getAsJsonObject();
              if (obj.has("model")) {
                validateModelReference(name, obj.get("model").getAsString(), blockModelsDir,
                    sharedModelsDir, key);
              }
              if (obj.has("textures")) {
                validateTextureReferences(name, obj.getAsJsonObject("textures"), blockTexturesDir,
                    itemTexturesDir, key);
              }
            }
          }
        }
        continue;
      }

      JsonElement element = variants.get(key);
      if (element.isJsonObject()) {
        JsonObject propertyVariants = element.getAsJsonObject();
        for (String value : propertyVariants.keySet()) {
          JsonElement variantElement = propertyVariants.get(value);
          if (variantElement.isJsonObject()) {
            JsonObject variant = variantElement.getAsJsonObject();
            if (variant.has("model")) {
              validateModelReference(name, variant.get("model").getAsString(), blockModelsDir,
                  sharedModelsDir, key + "=" + value);
            }
            if (variant.has("textures")) {
              validateTextureReferences(name, variant.getAsJsonObject("textures"), blockTexturesDir,
                  itemTexturesDir, key + "=" + value);
            }
          }
        }
      }
    }
  }

  private static void validateModelReference(String blockstateName, String modelRef,
      AssetFolder blockModelsDir, AssetFolder sharedModelsDir, String context) {
    validatedModels.incrementAndGet();
    if (!modelRef.startsWith(MOD_PREFIX)) {
      return; // Vanilla model reference — skip
    }

    String stripped = modelRef.substring(MOD_PREFIX.length());
    File modelFile;

    if (stripped.startsWith("shared_models/")) {
      // Blockstate model ref (auto-prepends block/) → resolves to shared_models/
      modelFile = sharedModelsDir.file(stripped.substring("shared_models/".length()) + ".json");
    } else if (stripped.startsWith("block/shared_models/")) {
      // Should not appear in blockstates (double block/ issue)
      logError(blockstateName, "Model ref has 'block/shared_models/' prefix — will cause double "
          + "block/ resolution. Use 'shared_models/' instead. Context: " + context);
      return;
    } else if (stripped.endsWith(".obj")) {
      modelFile = blockModelsDir.file(stripped);
    } else {
      modelFile = blockModelsDir.file(stripped + ".json");
    }

    if (!modelFile.exists()) {
      logError(blockstateName, "Model not found: " + modelRef + " (expected: "
          + modelFile.getPath() + ") Context: " + context);
    }
  }

  private static void validateTextureReferences(String blockstateName, JsonObject textures,
      AssetFolder blockTexturesDir, AssetFolder itemTexturesDir, String context) {
    for (String key : textures.keySet()) {
      validatedTextures.incrementAndGet();
      String value = textures.get(key).getAsString();
      if (value.isEmpty() || !value.startsWith(MOD_PREFIX)) {
        continue;
      }
      String stripped = value.substring(MOD_PREFIX.length());
      File textureFile = null;
      if (stripped.startsWith("blocks/")) {
        textureFile = blockTexturesDir.file(stripped.substring("blocks/".length()) + ".png");
      } else if (stripped.startsWith("items/")) {
        textureFile = itemTexturesDir.file(stripped.substring("items/".length()) + ".png");
      }

      if (textureFile != null && !textureFile.exists()) {
        logError(blockstateName, "Texture not found: " + value + " Context: " + context);
      }
    }
  }

  private static void validateFacingVariant(String name, JsonElement facingElement) {
    if (!facingElement.isJsonObject()) {
      return;
    }
    JsonObject facing = facingElement.getAsJsonObject();
    Set<String> validKeys = Set.of("north", "south", "east", "west", "up", "down",
        "n", "ne", "e", "se", "s", "sw", "w", "nw");
    for (String key : facing.keySet()) {
      if (!validKeys.contains(key)) {
        logWarning(name, "Unusual facing value: '" + key + "'");
      }
    }
  }

  private static void logError(String blockstate, String message) {
    int count = errorCount.incrementAndGet();
    System.err.printf("E%04d [%s]: %s%n", count, blockstate, message);
  }

  private static void logWarning(String blockstate, String message) {
    int count = warningCount.incrementAndGet();
    System.out.printf("W%04d [%s]: %s%n", count, blockstate, message);
  }
}
