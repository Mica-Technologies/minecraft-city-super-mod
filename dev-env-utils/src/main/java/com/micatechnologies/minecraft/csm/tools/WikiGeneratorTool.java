package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates a complete GitHub Wiki-compatible markdown documentation folder for the CSM mod. Reads
 * source files, JSON resources, textures, and lang files from disk (no running game needed).
 *
 * @version 1.0
 */
public class WikiGeneratorTool {

    // ──────────────────────────────────────────────────────────────────────────
    // Constants — relative paths
    // ──────────────────────────────────────────────────────────────────────────

    private static final String SOURCE_ROOT =
            "src/main/java/com/micatechnologies/minecraft/csm";
    private static final String TABS_FOLDER = SOURCE_ROOT + "/tabs";
    private static final String LANG_FILE =
            "src/main/resources/assets/csm/lang/en_us.lang";
    private static final String BLOCKSTATES_FOLDER =
            "src/main/resources/assets/csm/blockstates";
    private static final String TEXTURES_FOLDER =
            "src/main/resources/assets/csm/textures/blocks";
    private static final String MODELS_FOLDER =
            "src/main/resources/assets/csm/models/block";
    private static final String DEFAULT_OUTPUT_DIR = "dev-env-utils/wikiOutput";

    private static final String LINE_SEP = "\r\n";

    // ──────────────────────────────────────────────────────────────────────────
    // Regex patterns
    // ──────────────────────────────────────────────────────────────────────────

    private static final Pattern BLOCK_REGISTRY_NAME_PATTERN =
            Pattern.compile("getBlockRegistryName\\s*\\(\\s*\\)\\s*\\{[^}]*return\\s+\"([a-z0-9_]+)\"\\s*;",
                    Pattern.DOTALL);
    private static final Pattern ITEM_REGISTRY_NAME_PATTERN =
            Pattern.compile("getItemRegistryName\\s*\\(\\s*\\)\\s*\\{[^}]*return\\s+\"([a-z0-9_]+)\"\\s*;",
                    Pattern.DOTALL);
    private static final Pattern EXTENDS_PATTERN =
            Pattern.compile("extends\\s+(\\w+)");
    private static final Pattern SUPER_CALL_PATTERN =
            Pattern.compile("super\\s*\\(([^)]+)\\)", Pattern.DOTALL);
    private static final Pattern TAB_ID_PATTERN =
            Pattern.compile("getTabId\\s*\\(\\s*\\)\\s*\\{[^}]*return\\s+\"([^\"]+)\"\\s*;",
                    Pattern.DOTALL);
    private static final Pattern INIT_TAB_BLOCK_PATTERN =
            Pattern.compile("initTabBlock\\s*\\(\\s*(\\w+)\\.class");
    private static final Pattern INIT_TAB_ITEM_PATTERN =
            Pattern.compile("initTabItem\\s*\\(\\s*(\\w+)\\.class");
    private static final Pattern LIGHT_VALUE_PATTERN =
            Pattern.compile("getLightValue[^{]*\\{[^}]*return\\s+(\\d+)\\s*;", Pattern.DOTALL);
    private static final Pattern REDSTONE_PATTERN =
            Pattern.compile("getBlockConnectsRedstone[^{]*\\{[^}]*return\\s+true\\s*;",
                    Pattern.DOTALL);
    private static final Pattern MATERIAL_PATTERN =
            Pattern.compile("Material\\.(\\w+)");

    // ──────────────────────────────────────────────────────────────────────────
    // Data classes
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Holds all extracted metadata for a single block.
     */
    private static class BlockInfo {
        String registryName;
        String className;
        String parentClass;
        String rotationType;
        boolean hasRedstone;
        int lightLevel;
        boolean hasTileEntity;
        boolean isDeprecated;
        String displayName;
        String tabDisplayName;
        String tabKey;          // internal tab grouping key, e.g. "Traffic Signals"
        String material;
        float hardness = -1;
        float resistance = -1;
        List<String> specialInterfaces = new ArrayList<>();
    }

    /**
     * Holds all extracted metadata for a single item.
     */
    private static class ItemInfo {
        String registryName;
        String className;
        String displayName;
        String tabDisplayName;
        String tabKey;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tracking counters
    // ──────────────────────────────────────────────────────────────────────────

    private int totalBlocks;
    private int totalItems;
    private int totalDeprecated;
    private int totalUnlisted;
    private int totalImagesCopied;
    private int totalPagesGenerated;

    // ──────────────────────────────────────────────────────────────────────────
    // Entry point
    // ──────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        CsmToolUtility.doToolExecuteWrapped("CSM Wiki Generator Tool", args,
                devEnvironmentPath -> new WikiGeneratorTool().run(devEnvironmentPath));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Main orchestrator
    // ──────────────────────────────────────────────────────────────────────────

    private void run(File devRoot) throws Exception {
        Path root = devRoot.toPath();
        Path outputDir = root.resolve(DEFAULT_OUTPUT_DIR);

        // 1. Clean & recreate output directory
        if (Files.exists(outputDir)) {
            deleteRecursively(outputDir);
        }
        Files.createDirectories(outputDir);
        Files.createDirectories(outputDir.resolve("blocks"));
        Files.createDirectories(outputDir.resolve("deprecated"));
        Files.createDirectories(outputDir.resolve("unlisted"));
        Files.createDirectories(outputDir.resolve("items"));
        Files.createDirectories(outputDir.resolve("images/blocks"));

        // 2. Parse lang file
        Map<String, String> blockLang = new LinkedHashMap<>();
        Map<String, String> itemLang = new LinkedHashMap<>();
        Map<String, String> tabLang = new LinkedHashMap<>();
        parseLangFile(root.resolve(LANG_FILE), blockLang, itemLang, tabLang);
        System.out.println("  Lang entries loaded: " + blockLang.size() + " blocks, "
                + itemLang.size() + " items, " + tabLang.size() + " tabs");

        // 3. Parse tab files → class-name-to-tab mappings
        Map<String, String> blockClassToTabId = new LinkedHashMap<>();
        Map<String, String> itemClassToTabId = new LinkedHashMap<>();
        Map<String, String> tabIdToDisplayName = new LinkedHashMap<>();
        parseTabs(root.resolve(TABS_FOLDER), blockClassToTabId, itemClassToTabId,
                tabIdToDisplayName, tabLang);
        System.out.println("  Tab mappings loaded: " + blockClassToTabId.size() + " block entries, "
                + itemClassToTabId.size() + " item entries across "
                + tabIdToDisplayName.size() + " tabs");

        // 4. Discover blocks
        List<BlockInfo> blocks = discoverBlocks(root.resolve(SOURCE_ROOT));
        System.out.println("  Blocks discovered: " + blocks.size());

        // 5. Discover items
        List<ItemInfo> items = discoverItems(root.resolve(SOURCE_ROOT));
        System.out.println("  Items discovered: " + items.size());

        // 6. Enrich blocks with lang names, tab assignments, etc.
        enrichBlocks(blocks, blockLang, blockClassToTabId, tabIdToDisplayName);

        // 7. Enrich items with lang names and tab assignments
        enrichItems(items, itemLang, itemClassToTabId, tabIdToDisplayName);

        // 8. Resolve images for blocks
        resolveBlockImages(blocks, root, outputDir);

        // 9. Generate wiki pages
        generateWikiPages(blocks, items, outputDir);

        // 10. Print statistics
        printStatistics();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lang file parsing
    // ──────────────────────────────────────────────────────────────────────────

    private void parseLangFile(Path langPath,
                               Map<String, String> blockLang,
                               Map<String, String> itemLang,
                               Map<String, String> tabLang) throws IOException {
        if (!Files.exists(langPath)) {
            System.err.println("  WARNING: Lang file not found at " + langPath);
            return;
        }
        for (String line : Files.readAllLines(langPath)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (key.startsWith("tile.") && key.endsWith(".name")) {
                String id = key.substring(5, key.length() - 5);
                blockLang.put(id, value);
            } else if (key.startsWith("item.") && key.endsWith(".name")) {
                String id = key.substring(5, key.length() - 5);
                itemLang.put(id, value);
            } else if (key.startsWith("itemGroup.")) {
                String id = key.substring(10);
                tabLang.put(id, value);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tab parsing
    // ──────────────────────────────────────────────────────────────────────────

    private void parseTabs(Path tabsFolder,
                           Map<String, String> blockClassToTabId,
                           Map<String, String> itemClassToTabId,
                           Map<String, String> tabIdToDisplayName,
                           Map<String, String> tabLang) throws IOException {
        if (!Files.isDirectory(tabsFolder)) {
            System.err.println("  WARNING: Tabs folder not found at " + tabsFolder);
            return;
        }
        try (Stream<Path> files = Files.list(tabsFolder)) {
            for (Path tabFile : files.filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList())) {
                String src = new String(Files.readAllBytes(tabFile));
                String fileName = tabFile.getFileName().toString();

                // Extract tab ID
                Matcher tabIdMatcher = TAB_ID_PATTERN.matcher(src);
                if (!tabIdMatcher.find()) continue;
                String tabId = tabIdMatcher.group(1);

                // Resolve display name from lang, or derive from class name
                String displayName = tabLang.getOrDefault(tabId,
                        deriveDisplayNameFromTabClass(fileName));
                tabIdToDisplayName.put(tabId, displayName);

                // Extract block class references
                Matcher blockMatcher = INIT_TAB_BLOCK_PATTERN.matcher(src);
                while (blockMatcher.find()) {
                    blockClassToTabId.put(blockMatcher.group(1), tabId);
                }

                // Extract item class references
                Matcher itemMatcher = INIT_TAB_ITEM_PATTERN.matcher(src);
                while (itemMatcher.find()) {
                    itemClassToTabId.put(itemMatcher.group(1), tabId);
                }
            }
        }
    }

    /**
     * Derives a human-readable display name from a tab class filename like
     * "CsmTabTrafficSignals.java" -> "Traffic Signals".
     */
    private String deriveDisplayNameFromTabClass(String fileName) {
        String name = fileName.replace(".java", "").replace("CsmTab", "");
        // Insert spaces before capital letters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block discovery
    // ──────────────────────────────────────────────────────────────────────────

    private List<BlockInfo> discoverBlocks(Path sourceRoot) throws IOException {
        List<BlockInfo> blocks = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList())) {
                String src = new String(Files.readAllBytes(javaFile));

                // Must have getBlockRegistryName
                Matcher regNameMatcher = BLOCK_REGISTRY_NAME_PATTERN.matcher(src);
                if (!regNameMatcher.find()) continue;

                // Must extend an Abstract* class
                Matcher extendsMatcher = EXTENDS_PATTERN.matcher(src);
                if (!extendsMatcher.find()) continue;
                String parentClass = extendsMatcher.group(1);
                if (!parentClass.startsWith("Abstract") &&
                    !parentClass.startsWith("CsmTab") &&
                    !src.contains("extends Abstract")) {
                    // Not a block class — do a broader check
                    if (!isBlockParentClass(parentClass)) continue;
                }

                BlockInfo info = new BlockInfo();
                info.registryName = regNameMatcher.group(1);
                info.className = javaFile.getFileName().toString().replace(".java", "");
                info.parentClass = parentClass;
                info.rotationType = deriveRotationType(parentClass, src);

                // Light level
                Matcher lightMatcher = LIGHT_VALUE_PATTERN.matcher(src);
                if (lightMatcher.find()) {
                    try {
                        info.lightLevel = Integer.parseInt(lightMatcher.group(1));
                    } catch (NumberFormatException e) {
                        info.lightLevel = 0;
                    }
                } else {
                    // Check constructor for light level param
                    info.lightLevel = extractLightLevelFromSuper(src);
                }

                // Redstone
                info.hasRedstone = REDSTONE_PATTERN.matcher(src).find()
                        || parentClass.contains("Powered")
                        || parentClass.contains("ControllableSignal")
                        || parentClass.contains("ControllableCrosswalk");

                // Tile entity
                info.hasTileEntity = src.contains("ICsmTileEntityProvider")
                        || src.contains("createNewTileEntity")
                        || src.contains("AbstractTickableTileEntity")
                        || parentClass.contains("ControllableSignalHead")
                        || parentClass.contains("FireAlarm");

                // Deprecated
                info.isDeprecated = src.contains("ICsmRetiringBlock");

                // Special interfaces
                checkSpecialInterface(src, info, "ICsmTileEntityProvider");
                checkSpecialInterface(src, info, "ICsmRetiringBlock");
                if (src.contains("AbstractBlockFireAlarm")) {
                    info.specialInterfaces.add("Fire Alarm");
                }
                if (parentClass.contains("ControllableSignal")) {
                    info.specialInterfaces.add("Controllable Signal");
                }
                if (parentClass.contains("AbstractBlockSign")
                        || src.contains("extends AbstractBlockSign")) {
                    info.specialInterfaces.add("Sign");
                }

                // Material from super() call
                Matcher superMatcher = SUPER_CALL_PATTERN.matcher(src);
                if (superMatcher.find()) {
                    String superArgs = superMatcher.group(1);
                    Matcher matMatcher = MATERIAL_PATTERN.matcher(superArgs);
                    if (matMatcher.find()) {
                        info.material = matMatcher.group(1);
                    }
                    // Try to extract hardness/resistance from positional args
                    extractConstructorParams(superArgs, info);
                }

                blocks.add(info);
            }
        }
        return blocks;
    }

    /**
     * Checks whether the given parent class name is a known block parent (including non-Abstract
     * intermediate classes used in the codebase).
     */
    private boolean isBlockParentClass(String parentClass) {
        // Known intermediate parent classes that are themselves blocks
        return parentClass.startsWith("Abstract")
                || parentClass.equals("BlockControllableCrosswalkSignal")
                || parentClass.contains("BrightLight")
                || parentClass.contains("SignalBackplate");
    }

    /**
     * Derives rotation type string from the parent class name and source contents.
     */
    private String deriveRotationType(String parentClass, String src) {
        if (parentClass.contains("HZSixteen") || src.contains("AbstractBlockRotatableHZSixteen")) {
            return "16 directions (horizontal)";
        }
        if (parentClass.contains("HZEight") || src.contains("AbstractBlockRotatableHZEight")) {
            return "8 directions (horizontal)";
        }
        if (parentClass.contains("NSEWUD") || src.contains("AbstractBlockRotatableNSEWUD")
                || parentClass.contains("Powered")) {
            return "6 directions (NSEWUD)";
        }
        if (parentClass.contains("NSEW") || src.contains("AbstractBlockRotatableNSEW")) {
            return "4 directions (NSEW)";
        }
        if (parentClass.contains("ControllableSignal") || parentClass.contains("Crosswalk")
                || parentClass.contains("Sensor") || parentClass.contains("FireAlarm")) {
            // Most of these extend NSEWUD-based classes internally
            return "6 directions (NSEWUD)";
        }
        if (parentClass.contains("Sign")) {
            return "16 directions (horizontal)";
        }
        if (parentClass.contains("TrafficPole")) {
            return "6 directions (NSEWUD)";
        }
        return "None";
    }

    /**
     * Extracts light level from super(...) constructor args when the last numeric param is the
     * light level (0-15 range, as in AbstractBlock constructors that take light as the last float).
     */
    private int extractLightLevelFromSuper(String src) {
        Matcher superMatcher = SUPER_CALL_PATTERN.matcher(src);
        if (superMatcher.find()) {
            String args = superMatcher.group(1);
            // The AbstractBlockRotatableNSEWUD constructor signature is:
            // (Material, SoundType, harvestTool, harvestLevel, hardness, resistance, lightLevel, maxStack)
            // lightLevel is the second-to-last float, maxStack is the last int
            String[] parts = args.split(",");
            // Try to find a float between 0 and 1 (since setLightLevel takes 0.0-1.0 in some
            // constructors) or an int 0-15
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i].trim();
                // Skip maxStackSize (usually 64 or 255)
                try {
                    float val = Float.parseFloat(part.replace("F", "").replace("f", ""));
                    if (val >= 0 && val <= 1.0f && val != 0) {
                        // Likely light level as 0.0-1.0
                        return Math.round(val * 15);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    /**
     * Extracts hardness and resistance from super() constructor arguments.
     */
    private void extractConstructorParams(String superArgs, BlockInfo info) {
        String[] parts = superArgs.split(",");
        List<Float> floats = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim().replace("F", "").replace("f", "");
            try {
                floats.add(Float.parseFloat(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        // Common pattern: hardness, resistance are sequential floats
        // e.g., (Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255)
        //   → harvest=1, hardness=2, resistance=10, light=0, maxStack=255
        if (floats.size() >= 2) {
            info.hardness = floats.get(0);
            info.resistance = floats.get(1);
        }
    }

    private void checkSpecialInterface(String src, BlockInfo info, String interfaceName) {
        if (src.contains(interfaceName)) {
            info.specialInterfaces.add(interfaceName);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Item discovery
    // ──────────────────────────────────────────────────────────────────────────

    private List<ItemInfo> discoverItems(Path sourceRoot) throws IOException {
        List<ItemInfo> items = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList())) {
                String src = new String(Files.readAllBytes(javaFile));

                // Must have getItemRegistryName
                Matcher regNameMatcher = ITEM_REGISTRY_NAME_PATTERN.matcher(src);
                if (!regNameMatcher.find()) continue;

                // Must extend AbstractItem or AbstractItemSpade
                if (!src.contains("extends AbstractItem") &&
                    !src.contains("extends AbstractItemSpade")) continue;

                ItemInfo info = new ItemInfo();
                info.registryName = regNameMatcher.group(1);
                info.className = javaFile.getFileName().toString().replace(".java", "");
                items.add(info);
            }
        }
        return items;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Enrichment
    // ──────────────────────────────────────────────────────────────────────────

    private void enrichBlocks(List<BlockInfo> blocks,
                              Map<String, String> blockLang,
                              Map<String, String> blockClassToTabId,
                              Map<String, String> tabIdToDisplayName) {
        for (BlockInfo b : blocks) {
            // Display name from lang
            b.displayName = blockLang.getOrDefault(b.registryName,
                    humanize(b.registryName));

            // Tab assignment
            String tabId = blockClassToTabId.get(b.className);
            if (tabId != null) {
                b.tabDisplayName = tabIdToDisplayName.getOrDefault(tabId, tabId);
                b.tabKey = b.tabDisplayName;
            } else {
                if (b.isDeprecated) {
                    b.tabKey = null; // will go to deprecated
                } else {
                    b.tabKey = null; // will go to unlisted
                }
            }
        }
    }

    private void enrichItems(List<ItemInfo> items,
                             Map<String, String> itemLang,
                             Map<String, String> itemClassToTabId,
                             Map<String, String> tabIdToDisplayName) {
        for (ItemInfo item : items) {
            item.displayName = itemLang.getOrDefault(item.registryName,
                    humanize(item.registryName));
            String tabId = itemClassToTabId.get(item.className);
            if (tabId != null) {
                item.tabDisplayName = tabIdToDisplayName.getOrDefault(tabId, tabId);
                item.tabKey = item.tabDisplayName;
            } else {
                item.tabKey = "Tools and Items";
            }
        }
    }

    /**
     * Converts a registry name like "controllableverticalsolidsignal" into a rough human-readable
     * form as a fallback when no lang entry exists.
     */
    private String humanize(String registryName) {
        // Insert spaces before runs of uppercase if camelCase, else just capitalize
        String spaced = registryName.replace('_', ' ');
        if (spaced.length() > 0) {
            spaced = Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
        }
        return spaced;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Image resolution
    // ──────────────────────────────────────────────────────────────────────────

    private static final int RENDER_IMAGE_SIZE = 128;
    private int totalImagesRendered = 0;

    private void resolveBlockImages(List<BlockInfo> blocks, Path projectRoot, Path outputDir)
            throws IOException {
        Path blockstatesDir = projectRoot.resolve(BLOCKSTATES_FOLDER);
        Path modelsDir = projectRoot.resolve(MODELS_FOLDER);
        Path texturesDir = projectRoot.resolve(TEXTURES_FOLDER);
        Path imagesOut = outputDir.resolve("images/blocks");

        for (BlockInfo block : blocks) {
            Path bsFile = blockstatesDir.resolve(block.registryName + ".json");
            if (!Files.exists(bsFile)) continue;

            try {
                // Try 3D rendered image first via inventory model
                if (tryRenderInventoryModel(bsFile, modelsDir, texturesDir, imagesOut, block)) {
                    totalImagesRendered++;
                    totalImagesCopied++;
                    continue;
                }

                // Fall back to texture copy
                if (tryCopyTexture(bsFile, texturesDir, imagesOut, block)) {
                    totalImagesCopied++;
                }
            } catch (Exception e) {
                System.err.println("  WARNING: Could not resolve image for "
                        + block.registryName + ": " + e.getMessage());
            }
        }
        System.out.println("  Images resolved: " + totalImagesCopied
                + " (" + totalImagesRendered + " rendered, "
                + (totalImagesCopied - totalImagesRendered) + " texture copies)");
    }

    /**
     * Attempts to find the inventory variant's model, resolve it to a JSON file with elements,
     * and render it using MinecraftModelRenderer. Returns true if successful.
     */
    private boolean tryRenderInventoryModel(Path bsFile, Path modelsDir, Path texturesDir,
            Path imagesOut, BlockInfo block) {
        try {
            String content = new String(Files.readAllBytes(bsFile), java.nio.charset.StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonObject defaults = root.has("defaults") ? root.getAsJsonObject("defaults") : null;
            JsonObject variants = root.has("variants") ? root.getAsJsonObject("variants") : null;
            if (variants == null || !variants.has("inventory")) return false;

            // Get inventory variant
            JsonElement invElement = variants.get("inventory");
            JsonObject invObj = null;
            if (invElement.isJsonArray()) {
                JsonArray invArr = invElement.getAsJsonArray();
                if (invArr.size() > 0) invObj = invArr.get(0).getAsJsonObject();
            } else if (invElement.isJsonObject()) {
                invObj = invElement.getAsJsonObject();
            }
            if (invObj == null) return false;

            // Get model reference — inventory variant may specify its own, or inherit from defaults
            String modelRef = null;
            if (invObj.has("model")) {
                modelRef = invObj.get("model").getAsString();
            } else if (defaults != null && defaults.has("model")) {
                modelRef = defaults.get("model").getAsString();
            }
            if (modelRef == null) return false;
            // Skip vanilla models (cube_all, etc.) — not renderable as custom geometry
            if (!modelRef.startsWith("csm:")) return false;
            File modelFile = resolveModelToFile(modelRef, modelsDir);
            if (modelFile == null || !modelFile.exists()) return false;

            // Check if the model has elements (renderable geometry)
            String modelContent = new String(Files.readAllBytes(modelFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            JsonObject modelJson = JsonParser.parseString(modelContent).getAsJsonObject();

            // Follow parent chain to find elements
            File fileWithElements = modelFile;
            JsonObject jsonWithElements = modelJson;
            int depth = 0;
            while (!jsonWithElements.has("elements") && jsonWithElements.has("parent") && depth < 10) {
                String parentRef = jsonWithElements.get("parent").getAsString();
                File parentFile = resolveModelToFile(parentRef, modelsDir);
                if (parentFile == null || !parentFile.exists()) break;
                String parentContent = new String(Files.readAllBytes(parentFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                jsonWithElements = JsonParser.parseString(parentContent).getAsJsonObject();
                fileWithElements = parentFile;
                depth++;
            }
            if (!jsonWithElements.has("elements")) return false;

            // Build texture map: merge inventory variant textures with model textures and defaults
            Map<String, File> textureMap = buildTextureMap(root, invObj, modelJson, texturesDir);

            // Render
            java.awt.image.BufferedImage rendered = MinecraftModelRenderer.renderModel(
                    fileWithElements, textureMap, RENDER_IMAGE_SIZE);
            if (rendered == null) return false;

            Path dest = imagesOut.resolve(block.registryName + ".png");
            javax.imageio.ImageIO.write(rendered, "PNG", dest.toFile());
            return true;
        } catch (Exception e) {
            // Render failed — fall through to texture copy
            // Uncomment for debugging: System.err.println("  RENDER FAIL [" + block.registryName + "]: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resolves a model reference (like "csm:trafficsignals/shared_models/foo" or
     * "csm:block/lighting/shared_models/bar") to a File on disk.
     */
    private File resolveModelToFile(String modelRef, Path modelsDir) {
        if (modelRef == null) return null;
        String stripped = modelRef;
        if (stripped.startsWith("csm:")) {
            stripped = stripped.substring("csm:".length());
        }
        if (stripped.startsWith("block/")) {
            stripped = stripped.substring("block/".length());
        }
        // Try as-is under models/block/
        File f = modelsDir.resolve(stripped + ".json").toFile();
        if (f.exists()) return f;
        // Try with .json already
        f = modelsDir.resolve(stripped).toFile();
        if (f.exists()) return f;
        return null;
    }

    /**
     * Builds a texture variable → File map by merging textures from:
     * 1. Blockstate defaults.textures (lowest priority)
     * 2. Model's own textures block
     * 3. Inventory variant textures (highest priority)
     */
    private Map<String, File> buildTextureMap(JsonObject blockstate, JsonObject invVariant,
            JsonObject modelJson, Path texturesDir) {
        Map<String, File> map = new java.util.HashMap<>();

        // Defaults textures
        if (blockstate.has("defaults")) {
            JsonObject defaults = blockstate.getAsJsonObject("defaults");
            if (defaults.has("textures")) {
                addTexturesToMap(defaults.getAsJsonObject("textures"), texturesDir, map);
            }
        }

        // Model textures
        if (modelJson.has("textures")) {
            addTexturesToMap(modelJson.getAsJsonObject("textures"), texturesDir, map);
        }

        // Inventory variant textures (highest priority)
        if (invVariant.has("textures")) {
            addTexturesToMap(invVariant.getAsJsonObject("textures"), texturesDir, map);
        }

        return map;
    }

    private void addTexturesToMap(JsonObject textures, Path texturesDir, Map<String, File> map) {
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            String texRef = entry.getValue().getAsString();
            File texFile = resolveTextureToFile(texRef, texturesDir);
            if (texFile != null && texFile.exists()) {
                map.put(entry.getKey(), texFile);
            }
        }
    }

    private File resolveTextureToFile(String texRef, Path texturesDir) {
        if (texRef == null || texRef.contains("transparent")) return null;
        String stripped = texRef;
        if (stripped.startsWith("csm:blocks/")) {
            stripped = stripped.substring("csm:blocks/".length());
        } else if (stripped.startsWith("csm:")) {
            stripped = stripped.substring("csm:".length());
            if (stripped.startsWith("blocks/")) {
                stripped = stripped.substring("blocks/".length());
            }
        }
        File f = texturesDir.resolve(stripped + ".png").toFile();
        return f.exists() ? f : null;
    }

    /**
     * Falls back to copying a single texture file as the block image.
     */
    private boolean tryCopyTexture(Path bsFile, Path texturesDir, Path imagesOut, BlockInfo block)
            throws IOException {
        String texturePath = resolveTexturePath(bsFile);
        if (texturePath == null) return false;

        String fileRelative = texturePath;
        if (fileRelative.startsWith("csm:blocks/")) {
            fileRelative = fileRelative.substring("csm:blocks/".length());
        } else if (fileRelative.startsWith("csm:")) {
            fileRelative = fileRelative.substring("csm:".length());
            if (fileRelative.startsWith("blocks/")) {
                fileRelative = fileRelative.substring("blocks/".length());
            }
        }

        Path srcTexture = texturesDir.resolve(fileRelative + ".png");
        if (!Files.exists(srcTexture)) {
            srcTexture = texturesDir.resolve(
                    fileRelative.replace("/", File.separator) + ".png");
        }
        if (Files.exists(srcTexture)) {
            Path destTexture = imagesOut.resolve(block.registryName + ".png");
            Files.copy(srcTexture, destTexture, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        return false;
    }

    /**
     * Parses a blockstate JSON and resolves the best texture path for wiki display.
     */
    private String resolveTexturePath(Path blockstateFile) throws IOException {
        String content = new String(Files.readAllBytes(blockstateFile));
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        // Check if this is a TESR block (defaults model is cube_all with transparent texture)
        boolean isTesr = false;
        JsonObject defaults = root.has("defaults") ? root.getAsJsonObject("defaults") : null;
        if (defaults != null && defaults.has("textures")) {
            JsonObject defTextures = defaults.getAsJsonObject("textures");
            if (defTextures.has("all")) {
                String allTex = defTextures.get("all").getAsString();
                if (allTex.contains("transparent")) {
                    isTesr = true;
                }
            }
        }

        // Look for inventory variant first
        JsonObject variants = root.has("variants") ? root.getAsJsonObject("variants") : null;
        if (variants != null && variants.has("inventory")) {
            JsonElement invElement = variants.get("inventory");
            JsonObject invObj = null;
            if (invElement.isJsonArray()) {
                JsonArray invArr = invElement.getAsJsonArray();
                if (invArr.size() > 0) {
                    invObj = invArr.get(0).getAsJsonObject();
                }
            } else if (invElement.isJsonObject()) {
                invObj = invElement.getAsJsonObject();
            }

            if (invObj != null && invObj.has("textures")) {
                JsonObject textures = invObj.getAsJsonObject("textures");

                if (isTesr) {
                    // For TESR blocks, use texture "2" (green/bottom section)
                    if (textures.has("2")) {
                        return textures.get("2").getAsString();
                    }
                }

                // Try common texture keys in priority order
                for (String key : new String[]{"all", "0", "particle"}) {
                    if (textures.has(key)) {
                        String val = textures.get(key).getAsString();
                        if (!val.contains("transparent")) {
                            return val;
                        }
                    }
                }

                // Fall back to first texture found
                for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                    String val = entry.getValue().getAsString();
                    if (!val.contains("transparent")) {
                        return val;
                    }
                }
            }
        }

        // No inventory variant — check defaults textures
        if (defaults != null && defaults.has("textures")) {
            JsonObject textures = defaults.getAsJsonObject("textures");
            for (String key : new String[]{"all", "0", "particle"}) {
                if (textures.has(key)) {
                    String val = textures.get(key).getAsString();
                    if (!val.contains("transparent")) {
                        return val;
                    }
                }
            }
        }

        return null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Wiki page generation
    // ──────────────────────────────────────────────────────────────────────────

    private void generateWikiPages(List<BlockInfo> blocks, List<ItemInfo> items, Path outputDir)
            throws IOException {

        // Group blocks by tab
        Map<String, List<BlockInfo>> tabBlocks = new TreeMap<>();
        List<BlockInfo> deprecatedBlocks = new ArrayList<>();
        List<BlockInfo> unlistedBlocks = new ArrayList<>();

        for (BlockInfo b : blocks) {
            if (b.isDeprecated) {
                deprecatedBlocks.add(b);
                totalDeprecated++;
            } else if (b.tabKey != null) {
                tabBlocks.computeIfAbsent(b.tabKey, k -> new ArrayList<>()).add(b);
                totalBlocks++;
            } else {
                unlistedBlocks.add(b);
                totalUnlisted++;
                totalBlocks++;
            }
        }

        // Sort blocks within each group by display name
        for (List<BlockInfo> list : tabBlocks.values()) {
            list.sort(Comparator.comparing(b -> b.displayName.toLowerCase()));
        }
        deprecatedBlocks.sort(Comparator.comparing(b -> b.displayName.toLowerCase()));
        unlistedBlocks.sort(Comparator.comparing(b -> b.displayName.toLowerCase()));

        // Count deprecated in total blocks too
        totalBlocks += totalDeprecated;

        // Generate tab block pages
        for (Map.Entry<String, List<BlockInfo>> entry : tabBlocks.entrySet()) {
            String tabName = entry.getKey();
            String pageFileName = toPageFileName(tabName);
            Path pagePath = outputDir.resolve("blocks/" + pageFileName + ".md");
            writeBlockPage(pagePath, tabName, entry.getValue(), "../images/blocks/", false);
        }

        // Generate deprecated pages grouped by original tab
        Map<String, List<BlockInfo>> deprecatedByTab = new TreeMap<>();
        for (BlockInfo b : deprecatedBlocks) {
            String groupKey = b.tabDisplayName != null ? b.tabDisplayName : "Other";
            deprecatedByTab.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(b);
        }
        for (Map.Entry<String, List<BlockInfo>> entry : deprecatedByTab.entrySet()) {
            String tabName = entry.getKey();
            String pageFileName = toPageFileName(tabName);
            Path pagePath = outputDir.resolve("deprecated/" + pageFileName + ".md");
            writeBlockPage(pagePath, "Deprecated: " + tabName, entry.getValue(),
                    "../images/blocks/", true);
        }

        // Generate unlisted page
        if (!unlistedBlocks.isEmpty()) {
            Path pagePath = outputDir.resolve("unlisted/Unlisted.md");
            writeBlockPage(pagePath, "Unlisted Blocks", unlistedBlocks,
                    "../images/blocks/", false);
        }

        // Generate items page
        items.sort(Comparator.comparing(i -> i.displayName.toLowerCase()));
        totalItems = items.size();
        generateItemsPage(items, outputDir.resolve("items/Tools-and-Items.md"));

        // Generate Home.md
        generateHomePage(tabBlocks, deprecatedByTab, unlistedBlocks, items, outputDir);

        // Generate _Sidebar.md
        generateSidebarPage(tabBlocks, deprecatedByTab, items, outputDir);
    }

    /**
     * Writes a markdown page containing block entries for one tab/group.
     */
    private void writeBlockPage(Path pagePath, String title, List<BlockInfo> blocks,
                                String imageRelPath, boolean deprecated) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append(LINE_SEP);
        sb.append(LINE_SEP);
        sb.append("*").append(blocks.size()).append(" blocks*").append(LINE_SEP);

        for (BlockInfo b : blocks) {
            sb.append(LINE_SEP);
            sb.append("---").append(LINE_SEP);
            sb.append(LINE_SEP);
            sb.append("### ").append(b.displayName).append(LINE_SEP);
            sb.append(LINE_SEP);

            if (deprecated || b.isDeprecated) {
                sb.append("> **\u26A0 DEPRECATED** \u2014 This block is being retired and will auto-convert to its replacement.")
                        .append(LINE_SEP);
                sb.append(LINE_SEP);
            }

            // Image
            Path imagePath = pagePath.getParent().getParent()
                    .resolve("images/blocks/" + b.registryName + ".png");
            if (Files.exists(imagePath)) {
                sb.append("<img src=\"").append(imageRelPath).append(b.registryName)
                        .append(".png\" width=\"64\" height=\"64\" />").append(LINE_SEP);
            } else {
                sb.append("*(No image available)*").append(LINE_SEP);
            }
            sb.append(LINE_SEP);

            // Properties table
            sb.append("| Property | Value |").append(LINE_SEP);
            sb.append("|---|---|").append(LINE_SEP);
            sb.append("| **ID** | `csm:").append(b.registryName).append("` |").append(LINE_SEP);
            if (b.tabDisplayName != null) {
                sb.append("| **Category** | ").append(b.tabDisplayName)
                        .append(" |").append(LINE_SEP);
            }
            sb.append("| **Rotation** | ").append(b.rotationType).append(" |").append(LINE_SEP);
            sb.append("| **Redstone** | ").append(b.hasRedstone ? "Yes" : "No")
                    .append(" |").append(LINE_SEP);
            sb.append("| **Light Level** | ").append(b.lightLevel).append(" |").append(LINE_SEP);
            sb.append("| **Has Tile Entity** | ").append(b.hasTileEntity ? "Yes" : "No")
                    .append(" |").append(LINE_SEP);
            if (b.material != null) {
                sb.append("| **Material** | ").append(b.material).append(" |").append(LINE_SEP);
            }
            if (!b.specialInterfaces.isEmpty()) {
                sb.append("| **Special** | ").append(String.join(", ", b.specialInterfaces))
                        .append(" |").append(LINE_SEP);
            }
            sb.append(LINE_SEP);
        }

        Files.write(pagePath, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        totalPagesGenerated++;
    }

    /**
     * Generates the items markdown page.
     */
    private void generateItemsPage(List<ItemInfo> items, Path pagePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tools and Items").append(LINE_SEP);
        sb.append(LINE_SEP);
        sb.append("*").append(items.size()).append(" items*").append(LINE_SEP);

        for (ItemInfo item : items) {
            sb.append(LINE_SEP);
            sb.append("---").append(LINE_SEP);
            sb.append(LINE_SEP);
            sb.append("### ").append(item.displayName).append(LINE_SEP);
            sb.append(LINE_SEP);

            sb.append("| Property | Value |").append(LINE_SEP);
            sb.append("|---|---|").append(LINE_SEP);
            sb.append("| **ID** | `csm:").append(item.registryName).append("` |").append(LINE_SEP);
            if (item.tabDisplayName != null) {
                sb.append("| **Category** | ").append(item.tabDisplayName)
                        .append(" |").append(LINE_SEP);
            }
            sb.append(LINE_SEP);
        }

        Files.write(pagePath, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        totalPagesGenerated++;
    }

    /**
     * Generates the Home.md index page.
     */
    private void generateHomePage(Map<String, List<BlockInfo>> tabBlocks,
                                  Map<String, List<BlockInfo>> deprecatedByTab,
                                  List<BlockInfo> unlistedBlocks,
                                  List<ItemInfo> items,
                                  Path outputDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Minecraft City Super Mod \u2014 Wiki").append(LINE_SEP);
        sb.append(LINE_SEP);
        sb.append("Welcome to the CSM wiki. This mod adds 1,300+ city-themed blocks and items.")
                .append(LINE_SEP);
        sb.append(LINE_SEP);

        // Block categories
        sb.append("## Block Categories").append(LINE_SEP);
        sb.append(LINE_SEP);
        for (Map.Entry<String, List<BlockInfo>> entry : tabBlocks.entrySet()) {
            String tabName = entry.getKey();
            String pageName = toPageFileName(tabName);
            sb.append("- [").append(tabName).append("](blocks/").append(pageName)
                    .append(") (").append(entry.getValue().size()).append(" blocks)")
                    .append(LINE_SEP);
        }
        sb.append(LINE_SEP);

        // Items
        sb.append("## Items").append(LINE_SEP);
        sb.append(LINE_SEP);
        sb.append("- [Tools and Items](items/Tools-and-Items) (")
                .append(items.size()).append(" items)").append(LINE_SEP);
        sb.append(LINE_SEP);

        // Unlisted
        if (!unlistedBlocks.isEmpty()) {
            sb.append("## Unlisted Blocks").append(LINE_SEP);
            sb.append(LINE_SEP);
            sb.append("- [Unlisted](unlisted/Unlisted) (")
                    .append(unlistedBlocks.size()).append(" blocks)").append(LINE_SEP);
            sb.append(LINE_SEP);
        }

        // Deprecated
        if (!deprecatedByTab.isEmpty()) {
            sb.append("## Deprecated Blocks").append(LINE_SEP);
            sb.append(LINE_SEP);
            for (Map.Entry<String, List<BlockInfo>> entry : deprecatedByTab.entrySet()) {
                String tabName = entry.getKey();
                String pageName = toPageFileName(tabName);
                sb.append("- [Deprecated ").append(tabName).append("](deprecated/")
                        .append(pageName).append(") (").append(entry.getValue().size())
                        .append(" blocks)").append(LINE_SEP);
            }
            sb.append(LINE_SEP);
        }

        Files.write(outputDir.resolve("Home.md"), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        totalPagesGenerated++;
    }

    /**
     * Generates the _Sidebar.md navigation page.
     */
    private void generateSidebarPage(Map<String, List<BlockInfo>> tabBlocks,
                                     Map<String, List<BlockInfo>> deprecatedByTab,
                                     List<ItemInfo> items,
                                     Path outputDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("**[Home](Home)**").append(LINE_SEP);
        sb.append(LINE_SEP);

        sb.append("**Blocks**").append(LINE_SEP);
        for (String tabName : tabBlocks.keySet()) {
            String pageName = toPageFileName(tabName);
            sb.append("- [").append(tabName).append("](blocks/").append(pageName).append(")")
                    .append(LINE_SEP);
        }
        sb.append(LINE_SEP);

        sb.append("**Items**").append(LINE_SEP);
        sb.append("- [Tools and Items](items/Tools-and-Items)").append(LINE_SEP);
        sb.append(LINE_SEP);

        if (!deprecatedByTab.isEmpty()) {
            sb.append("**Deprecated**").append(LINE_SEP);
            for (String tabName : deprecatedByTab.keySet()) {
                String pageName = toPageFileName(tabName);
                sb.append("- [").append(tabName).append("](deprecated/").append(pageName)
                        .append(")").append(LINE_SEP);
            }
            sb.append(LINE_SEP);
        }

        Files.write(outputDir.resolve("_Sidebar.md"), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        totalPagesGenerated++;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts a tab display name like "Traffic Signals" to a wiki page filename like
     * "Traffic-Signals" (no .md extension — that's added by callers).
     */
    private String toPageFileName(String displayName) {
        // Strip "CSM: " or similar mod prefix, then sanitize for filesystem
        String name = displayName;
        if (name.startsWith("CSM: ")) {
            name = name.substring("CSM: ".length());
        }
        return name.replaceAll("[^a-zA-Z0-9 \\-]", "").trim().replace(' ', '-');
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private void deleteRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Prints final summary statistics.
     */
    private void printStatistics() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("Wiki Generation Summary");
        System.out.println("========================================");
        System.out.printf("  Total blocks documented:  %6d%n", totalBlocks);
        System.out.printf("  Total items documented:   %6d%n", totalItems);
        System.out.printf("  Total deprecated blocks:  %6d%n", totalDeprecated);
        System.out.printf("  Total unlisted blocks:    %6d%n", totalUnlisted);
        System.out.printf("  Total images:             %6d%n", totalImagesCopied);
        System.out.printf("    Rendered (3D):          %6d%n", totalImagesRendered);
        System.out.printf("    Texture copies:         %6d%n", totalImagesCopied - totalImagesRendered);
        System.out.printf("  Total wiki pages:         %6d%n", totalPagesGenerated);
        System.out.println("========================================");
    }
}
