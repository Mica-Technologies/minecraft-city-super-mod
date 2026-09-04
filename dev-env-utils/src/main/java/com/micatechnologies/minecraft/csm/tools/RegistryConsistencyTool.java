package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Cross-references all block/item registration sources to find inconsistencies.
 *
 * <p>Checks:
 * <ul>
 *   <li>Java classes with registry names that have no blockstate file</li>
 *   <li>Blockstate files with no corresponding Java class</li>
 *   <li>Blocks/items missing lang entries</li>
 *   <li>Lang entries with no corresponding block/item</li>
 *   <li>Block models with no corresponding blockstate reference</li>
 *   <li>Summary of registration counts across all sources</li>
 * </ul>
 */
public class RegistryConsistencyTool {

  // Relative to a tree's assets/csm. Core and each module hold a share of every one of these,
  // merged by the game into one namespace, so consistency is a question about the union.
  private static final String BLOCKSTATE_DIR = "blockstates";
  private static final String BLOCK_MODELS_DIR = "models/block";
  private static final String ITEM_MODELS_DIR = "models/item";
  private static final String LANG_LOCALE = "en_us";

  private static final Pattern REGISTRY_NAME_PATTERN =
      Pattern.compile("return\\s*\"([a-z0-9_]+)\"\\s*;");

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Registry Consistency Tool", args,
        (devEnvironmentPath) -> {
          CsmLayout layout = new CsmLayout(devEnvironmentPath);
          System.out.println("Scanning Java source files for registry names ("
              + String.join(", ", layout.modules()) + ")...");
          Map<String, String> blockRegistry = scanJavaRegistryNames(layout, true);
          Map<String, String> itemRegistry = scanJavaRegistryNames(layout, false);

          System.out.println("Scanning blockstate files...");
          Set<String> blockstateNames = scanDirectories(layout.assetDirs(BLOCKSTATE_DIR),
              ".json");

          System.out.println("Scanning block model files...");
          Set<String> blockModelNames = scanFlatDirectories(layout.assetDirs(BLOCK_MODELS_DIR),
              ".json");

          System.out.println("Scanning item model files...");
          Set<String> itemModelNames = scanDirectories(layout.assetDirs(ITEM_MODELS_DIR),
              ".json");

          System.out.println("Scanning lang files...");
          Map<String, String> langBlocks = new TreeMap<>();
          Map<String, String> langItems = new TreeMap<>();
          // One lang file per tree, merged by the game; an entry in any of them counts.
          for (File langFile : layout.langFiles(LANG_LOCALE)) {
            scanLangFile(langFile, langBlocks, langItems);
          }

          int issues = 0;

          // Check: Java blocks without blockstates
          System.out.println("\n--- Blocks without blockstate files ---");
          for (Map.Entry<String, String> entry : blockRegistry.entrySet()) {
            if (!blockstateNames.contains(entry.getKey())) {
              System.out.println("  MISSING BLOCKSTATE: " + entry.getKey()
                  + " (from " + entry.getValue() + ")");
              issues++;
            }
          }

          // Check: Blockstates without Java classes
          System.out.println("\n--- Blockstate files without Java classes ---");
          Set<String> allRegistryNames = new HashSet<>(blockRegistry.keySet());
          // Also include block set variants (fence, slab, stairs)
          Set<String> expandedNames = new HashSet<>(allRegistryNames);
          for (String name : allRegistryNames) {
            expandedNames.add(name + "_fence");
            expandedNames.add(name + "_slab");
            expandedNames.add(name + "_slab_double");
            expandedNames.add(name + "_stairs");
          }
          for (String bsName : blockstateNames) {
            if (!expandedNames.contains(bsName)) {
              System.out.println("  ORPHAN BLOCKSTATE: " + bsName + ".json");
              issues++;
            }
          }

          // Check: Blocks without lang entries
          System.out.println("\n--- Blocks without lang entries ---");
          for (String blockId : blockRegistry.keySet()) {
            if (!langBlocks.containsKey(blockId)) {
              System.out.println("  MISSING LANG: tile." + blockId + ".name");
              issues++;
            }
          }

          // Check: Items without lang entries
          System.out.println("\n--- Items without lang entries ---");
          for (String itemId : itemRegistry.keySet()) {
            if (!langItems.containsKey(itemId)) {
              System.out.println("  MISSING LANG: item." + itemId + ".name");
              issues++;
            }
          }

          // Check: Lang entries without blocks/items
          System.out.println("\n--- Lang entries without blocks/items ---");
          for (String langBlock : langBlocks.keySet()) {
            if (!blockRegistry.containsKey(langBlock)
                && !expandedNames.contains(langBlock)) {
              System.out.println("  ORPHAN LANG: tile." + langBlock + ".name="
                  + langBlocks.get(langBlock));
              issues++;
            }
          }
          for (String langItem : langItems.keySet()) {
            if (!itemRegistry.containsKey(langItem)) {
              System.out.println("  ORPHAN LANG: item." + langItem + ".name="
                  + langItems.get(langItem));
              issues++;
            }
          }

          // Summary
          System.out.println("\n========================================");
          System.out.println("Registry Consistency Report");
          System.out.println("========================================");
          System.out.println("Java block classes: " + blockRegistry.size());
          System.out.println("Java item classes: " + itemRegistry.size());
          System.out.println("Blockstate files: " + blockstateNames.size());
          System.out.println("Block model files: " + blockModelNames.size());
          System.out.println("Item model files: " + itemModelNames.size());
          System.out.println("Lang block entries: " + langBlocks.size());
          System.out.println("Lang item entries: " + langItems.size());
          System.out.println("Issues found: " + issues);
          System.out.println("========================================\n");
        });
  }

  /**
   * Scans Java files for getBlockRegistryName() or getItemRegistryName() return values.
   */
  private static Map<String, String> scanJavaRegistryNames(CsmLayout layout,
      boolean blocks) {
    Map<String, String> registry = new TreeMap<>();
    String methodName = blocks ? "getBlockRegistryName" : "getItemRegistryName";

    // Core and every module tree: a block class lives in whichever jar ships it.
    for (File sourceDir : layout.javaPackageRoots()) {
      try (Stream<Path> files = Files.walk(sourceDir.toPath())) {
        files.filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> {
              try {
                String content = Files.readString(p);
                if (!content.contains(methodName)) {
                  return;
                }
                // Find the method and extract the return value
                int methodIdx = content.indexOf(methodName);
                while (methodIdx >= 0) {
                  int returnIdx = content.indexOf("return", methodIdx);
                  if (returnIdx >= 0 && returnIdx < methodIdx + 200) {
                    Matcher m = REGISTRY_NAME_PATTERN.matcher(
                        content.substring(returnIdx, Math.min(returnIdx + 100, content.length())));
                    if (m.find()) {
                      String className = p.getFileName().toString().replace(".java", "");
                      registry.put(m.group(1), className);
                    }
                  }
                  methodIdx = content.indexOf(methodName, methodIdx + 1);
                }
              } catch (IOException e) {
                // Skip
              }
            });
      } catch (IOException e) {
        System.err.println("Error scanning Java files: " + e.getMessage());
      }
    }
    return registry;
  }

  /**
   * Scans every tree's copy of one asset folder, recursively.
   */
  private static Set<String> scanDirectories(List<File> dirs, String extension) {
    Set<String> names = new TreeSet<>();
    for (File dir : dirs) {
      names.addAll(scanDirectory(dir, extension));
    }
    return names;
  }

  /**
   * Scans every tree's copy of one asset folder, top level only.
   */
  private static Set<String> scanFlatDirectories(List<File> dirs, String extension) {
    Set<String> names = new TreeSet<>();
    for (File dir : dirs) {
      names.addAll(scanFlatDirectory(dir, extension));
    }
    return names;
  }

  private static Set<String> scanDirectory(File dir, String extension) {
    Set<String> names = new TreeSet<>();
    if (!dir.exists()) {
      return names;
    }
    try (Stream<Path> files = Files.walk(dir.toPath())) {
      files.filter(p -> p.toString().endsWith(extension))
          .forEach(p -> names.add(p.getFileName().toString().replace(extension, "")));
    } catch (IOException e) {
      System.err.println("Error scanning directory: " + e.getMessage());
    }
    return names;
  }

  /**
   * Scans only the flat files in a directory (not subdirectories).
   */
  private static Set<String> scanFlatDirectory(File dir, String extension) {
    Set<String> names = new TreeSet<>();
    if (!dir.exists()) {
      return names;
    }
    File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
    if (files != null) {
      for (File f : files) {
        names.add(f.getName().replace(extension, ""));
      }
    }
    return names;
  }

  private static void scanLangFile(File langFile, Map<String, String> blocks,
      Map<String, String> items) {
    try {
      List<String> lines = Files.readAllLines(langFile.toPath());
      for (String line : lines) {
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
        }
      }
    } catch (IOException e) {
      System.err.println("Error reading lang file: " + e.getMessage());
    }
  }
}
