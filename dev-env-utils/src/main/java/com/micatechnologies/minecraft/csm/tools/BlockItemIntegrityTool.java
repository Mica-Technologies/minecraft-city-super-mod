package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockItemIntegrityTool {

  private static final String SOURCE_FILE_FOLDER_PATH_RELATIVE =
      "src/main/java/com/micatechnologies/minecraft/csm";
  private static final String SOURCE_FILE_EXTENSION = ".java";

  private static final String[] SOURCE_FILE_EXCLUDES =
      {"src/main/java/com/micatechnologies/minecraft/csm/codeutils/AbstractBlockRotatableHZEight"
          + ".java",
          "src/main/java/com/micatechnologies/minecraft/csm/codeutils/AbstractBlockRotatableNSEWUD"
              + ".java", "src/main/java/com/micatechnologies/minecraft/csm/codeutils"
          + "/AbstractBlockRotatableNSEW.java",
          "src/main/java/com/micatechnologies/minecraft/csm/codeutils/AbstractBlockSetBasic.java",
          "src/main/java/com/micatechnologies/minecraft/csm/codeutils/AbstractBlockSlab.java",
          "src/main/java/com/micatechnologies/minecraft/csm/lifesafety"
              + "/AbstractBlockFireAlarmActivator.java",
          "src/main/java/com/micatechnologies/minecraft/csm/lifesafety"
              + "/AbstractBlockFireAlarmDetector.java",
          "src/main/java/com/micatechnologies/minecraft/csm/lifesafety"
              + "/AbstractBlockFireAlarmSounder.java",
          "src/main/java/com/micatechnologies/minecraft/csm/lifesafety"
              + "/AbstractBlockFireAlarmSounderVoiceEvac.java",
          "src/main/java/com/micatechnologies/minecraft/csm/lighting/AbstractBrightLight.java",
          "src/main/java/com/micatechnologies/minecraft/csm/codeutils"
              + "/AbstractPoweredBlockRotatableNSEWUD.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic"
              + "/AbstractBlockControllableCrosswalkAccessory.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic"
              + "/AbstractBlockControllableSignal.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic"
              + "/AbstractBlockTrafficSignalRequester.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic"
              + "/AbstractBlockTrafficSignalSensor.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic"
              + "/AbstractBlockTrafficSignalTickableRequester.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/logic"
              + "/AbstractBlockTrafficSignalAPS.java",
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsigns/AbstractBlockSign.java",
          "src/main/java/com/micatechnologies/minecraft/csm/tabs/CsmTabNone.java"};
  private static final String[] SOURCE_FILE_ELIGIBLE_EXTENDS_BLOCKS =
      {"AbstractBlock", "AbstractBlockRotatableNSEW", "AbstractBlockRotatableNSEWUD",
          "AbstractPoweredBlockRotatableNSEWUD", "AbstractBrightLight", "AbstractBlockSetBasic",
          "AbstractBlockFireAlarmSounderVoiceEvac", "AbstractBlockFireAlarmSounder",
          "AbstractBlockFireAlarmDetector", "AbstractBlockFireAlarmActivator",
          "AbstractBlockControllableCrosswalkAccessory", "AbstractBlockControllableSignal",
          "AbstractBlockTrafficSignalRequester", "AbstractBlockTrafficSignalSensor",
          "AbstractBlockTrafficSignalTickableRequester", "AbstractBlockTrafficSignalAPS",
          "AbstractBlockSign"};
  private static final String[] SOURCE_FILE_ELIGIBLE_EXTENDS_ITEMS = {"AbstractItem"};

  private static final String[] SOURCE_FILE_ELIGIBLE_EXTENDS_TAB = {"CsmTab"};

  private static final String BLOCKSTATE_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/blockstates";
  private static final String BLOCKSTATE_FILE_EXTENSION = ".json";

  private static final String BLOCK_MODELS_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/models/block";

  private static final String ITEM_MODELS_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/models/item";
  private static final String CUSTOM_MODELS_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/models/custom";

  private static final String BLOCK_TEXTURES_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/textures/blocks";
  private static final String ITEM_TEXTURES_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/textures/items";
  private static final String SOUNDS_FILE_FOLDER_PATH_RELATIVE =
      "src/main/resources/assets/csm/sounds";

  private static final String SOUNDS_CLASS_FILE_PATH_RELATIVE =
      "src/main/java/com/micatechnologies/minecraft/csm/CsmSounds.java";

  private static final String SOUNDS_JSON_FILE_PATH_RELATIVE =
      "src/main/resources/assets/csm/sounds.json";

  private static final String TABS_FILE_FOLDER_PATH_RELATIVE =
      "src/main/java/com/micatechnologies/minecraft/csm/tabs";

  private static final String LANG_FILE_FOLDER_PATH_RELATIVE = "src/main/resources/assets/csm/lang";
  private static final String LANG_FILE_EXTENSION = ".lang";
  private static final String SOUND_FILE_EXTENSION = ".ogg";
  private static final String MOD_PREFIX = "csm";

  private static final boolean DEBUG = false;
  private static final boolean DISABLE_UNUSED_CHECK = false;
  private static final AtomicInteger validationsCount = new AtomicInteger(0);
  private static final AtomicInteger checkCount = new AtomicInteger(0);
  private static final AtomicInteger errorCount = new AtomicInteger(0);
  private static final AtomicInteger suppressedErrorCount = new AtomicInteger(0);
  private static final AtomicInteger unusedCount = new AtomicInteger(0);
  private static final AtomicInteger unusedLangCount = new AtomicInteger(0);

  private static final List<String> knownMetaLangs = new ArrayList<>();
  private static final List<String> knownBlockIds = new ArrayList<>();
  private static final List<String> knownTabIds = new ArrayList<>();
  private static final List<String> knownItemIds = new ArrayList<>();
  private static final List<String> blockSetBlockIds = new ArrayList<>();
  private static final List<File> usedBlockstateFiles = new ArrayList<>();
  private static final List<File> usedBlockModelFiles = new ArrayList<>();
  private static final List<File> usedItemModelFiles = new ArrayList<>();
  private static final List<File> usedCustomModelFiles = new ArrayList<>();
  private static final List<File> usedBlockTexturesFiles = new ArrayList<>();
  private static final List<File> usedItemTexturesFiles = new ArrayList<>();
  private static final List<File> usedSoundFiles = new ArrayList<>();

  private static final List<String> loggedErrorMessages = new ArrayList<String>();

  public static void main(String[] args) {

    CsmToolUtility.doToolExecuteWrapped("CSM Block/Item Integrity Verification Tool", args,
        (devEnvironmentPath) -> {
          // List eligible source files (blocks and items)
          List<File> blockSourceFiles = listEligibleBlockSourceFiles(devEnvironmentPath);
          List<File> itemSourceFiles = listEligibleItemSourceFiles(devEnvironmentPath);
          List<File> tabSourceFiles = listEligibleTabSourceFiles(devEnvironmentPath);
          List<String> soundFiles = listEligibleSoundFiles(devEnvironmentPath);

          // Build source file excludes
          List<File> sourceExcludes = buildSourceExcludes(devEnvironmentPath);

          // Verify block/item integrity
          verifyBlockItemIntegritys(devEnvironmentPath, sourceExcludes, blockSourceFiles,
              itemSourceFiles);

          // Verify tab integrity
          verifyTabIntegritys(devEnvironmentPath, sourceExcludes, tabSourceFiles);

          // Verify sound file integrity
          checkSoundFilesIntegrity(devEnvironmentPath, soundFiles);

          // Check for unused files
          if (!DISABLE_UNUSED_CHECK) {
            checkForUnusedFiles(devEnvironmentPath);
          }

          // Print report
          System.out.println("\n========================================");
          System.out.println("Block/Item Integrity Verification Tool Report");
          System.out.println("========================================");
          System.out.println("Total Checked: " + checkCount.get());
          System.out.println("Total Validations: " + validationsCount.get());
          System.out.println("Total Errors: " + errorCount.get());
          System.out.println("Total Suppressed Errors: " + suppressedErrorCount.get());
          System.out.println("Total Unused Files: " + unusedCount.get());
          System.out.println("Total Unused Lang Entries: " + unusedLangCount.get());
          System.out.println("========================================\n");
        });
  }

  private static void verifyTabIntegritys(File devEnvironmentPath, List<File> sourceExcludes,
      List<File> tabSourceFiles) {
    for (File tabSourceFile : tabSourceFiles) {
      checkCount.incrementAndGet(); // Increment check count
      if (sourceExcludes.contains(tabSourceFile)) {
        continue;
      }
      verifyTabIntegrity(devEnvironmentPath, tabSourceFile);
    }
  }

  private static void verifyTabIntegrity(File devEnvironmentPath, File tabSourceFile) {
    try {
      // Read file contents
      String fileContents = Files.readString(tabSourceFile.toPath());

      // Get tab ID
      String tabId = getTabIdFromSourceFileContents(tabSourceFile, fileContents);

      // Check for lang file entries
      File langFolder = new File(devEnvironmentPath, LANG_FILE_FOLDER_PATH_RELATIVE);
      for (File langFile : Objects.requireNonNull(langFolder.listFiles())) {
        if (langFile.getName().endsWith(LANG_FILE_EXTENSION)) {
          validationsCount.incrementAndGet(); // Increment validation count
          String fullyQualifiedNameTab = "itemGroup." + tabId;
          if (!Files.readString(langFile.toPath()).contains(fullyQualifiedNameTab)) {
            logError(
                "Lang file entry does not exist for tab: " + tabId + " in " + langFile.getPath());
          }
        }
      }

    } catch (Exception e) {
      logError("Failed to verify tab file integrity: " + tabSourceFile.getPath());
      e.printStackTrace();
    }
  }

  private static String getTabIdFromSourceFileContents(File tabSourceFile, String fileContents)
      throws Exception {

    String tabId = getIdFromSourceFileContents(tabSourceFile, fileContents, "getTabId");

    knownTabIds.add(tabId);
    if (DEBUG) {
      System.out.println("Found tab ID: " + tabId);
    }
    return tabId;
  }

  private static List<File> listEligibleTabSourceFiles(File devEnvironmentPath) throws Exception {
    File tabSourceFolder = new File(devEnvironmentPath, TABS_FILE_FOLDER_PATH_RELATIVE);
    List<File> tabSourceFiles = new ArrayList<>();
    if (tabSourceFolder.exists()) {
      for (File tabSourceFile : Objects.requireNonNull(tabSourceFolder.listFiles())) {
        if (tabSourceFile.getName().endsWith(SOURCE_FILE_EXTENSION)) {
          String tabSourceFileContents = Files.readString(tabSourceFile.toPath());
          for (String eligibleExtends : SOURCE_FILE_ELIGIBLE_EXTENDS_TAB) {
            if (tabSourceFileContents.contains("extends " + eligibleExtends)) {
              tabSourceFiles.add(tabSourceFile);
              break;
            }
          }
        }
      }
    }
    return tabSourceFiles;
  }

  public static void checkForUnusedFiles(File devEnvironmentPath) {
    try {
      // Create common File objects
      File blockstateFolder = new File(devEnvironmentPath, BLOCKSTATE_FILE_FOLDER_PATH_RELATIVE);
      File blockModelsFolder = new File(devEnvironmentPath, BLOCK_MODELS_FILE_FOLDER_PATH_RELATIVE);
      File itemModelsFolder = new File(devEnvironmentPath, ITEM_MODELS_FILE_FOLDER_PATH_RELATIVE);
      File customModelsFolder =
          new File(devEnvironmentPath, CUSTOM_MODELS_FILE_FOLDER_PATH_RELATIVE);
      File blockTexturesFolder =
          new File(devEnvironmentPath, BLOCK_TEXTURES_FILE_FOLDER_PATH_RELATIVE);
      File itemTexturesFolder =
          new File(devEnvironmentPath, ITEM_TEXTURES_FILE_FOLDER_PATH_RELATIVE);
      File soundsResourceFolder = new File(devEnvironmentPath, SOUNDS_FILE_FOLDER_PATH_RELATIVE);

      // Check for unused files
      checkUnusedFiles(blockstateFolder, usedBlockstateFiles);
      checkUnusedFiles(blockModelsFolder, usedBlockModelFiles);
      checkUnusedFiles(itemModelsFolder, usedItemModelFiles);
      checkUnusedFiles(customModelsFolder, usedCustomModelFiles);
      checkUnusedFiles(blockTexturesFolder, usedBlockTexturesFiles);
      checkUnusedFiles(itemTexturesFolder, usedItemTexturesFiles);
      checkUnusedFiles(soundsResourceFolder, usedSoundFiles);
      checkForUnusedLang(devEnvironmentPath);
    } catch (Exception e) {
      logError("Unable to check for unused files.");
      e.printStackTrace();
    }
  }

  public static void checkForUnusedLang(File devEnvironmentPath) {
    try {
      // Create common File objects
      File langFolder = new File(devEnvironmentPath, LANG_FILE_FOLDER_PATH_RELATIVE);

      // Go line by line in each lang file and check for unused entries
      for (File langFile : Objects.requireNonNull(langFolder.listFiles())) {
        if (langFile.getName().endsWith(LANG_FILE_EXTENSION)) {
          List<String> langFileContents = Files.readAllLines(langFile.toPath());

          int lineNum = 0;
          for (String line : langFileContents) {

            validationsCount.incrementAndGet(); // Increment validation count
            lineNum++;
            boolean unused = true;

            // Check if for block
            if (line.startsWith("tile.")) {
              String blockId = line.substring("tile.".length(), line.indexOf(".name"));
              if (knownBlockIds.contains(blockId)) {
                unused = false;
              }
            }

            // Check if for item
            if (line.startsWith("item.")) {
              String itemId = line.substring("item.".length(), line.indexOf(".name"));
              if (knownItemIds.contains(itemId)) {
                unused = false;
              }
            }

            // Check if for meta
            if (unused) {
              for (String metaLang : knownMetaLangs) {
                if (line.startsWith(metaLang + "=")) {
                  unused = false;
                  break;
                }
              }
            }

            // Check if for tab
            if (line.startsWith("itemGroup.")) {
              String tabId = line.substring("itemGroup.".length(), line.indexOf("="));
              if (knownTabIds.contains(tabId)) {
                unused = false;
              }
            }

            if (unused) {
              int currentUnusedLangCount = unusedLangCount.incrementAndGet();
              String unusedLangCountString = String.format("%04d", currentUnusedLangCount);
              System.err.println("UL" + unusedLangCountString + ": Unused lang file entry found in "
                  + langFile.getName() + " at line " + lineNum + ": " + line);
            }
          }
        }
      }
    } catch (Exception e) {
      logError("Unable to check for unused lang files entries.");
      e.printStackTrace();
    }
  }

  public static void checkUnusedFiles(File folder, List<File> usedFiles) throws Exception {
    try (Stream<Path> filesStream = Files.walk(folder.toPath()).parallel()) {
      List<File> allFiles =
          filesStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());

      // Increment validations count by the number of files checked
      validationsCount.addAndGet(allFiles.size());

      allFiles.parallelStream() // Use parallelStream for potential performance improvement
          .filter(file -> !usedFiles.contains(file))
          .forEach(BlockItemIntegrityTool::reportUnusedFile);
    } catch (IOException e) {
      throw new Exception("Error while checking for unused files", e);
    }
  }

  private static void reportUnusedFile(File unusedFile) {
    int currentUnusedCount = unusedCount.incrementAndGet();
    String unusedCountString = String.format("%04d", currentUnusedCount);
    System.err.println("U" + unusedCountString + ": Unused file found: " + unusedFile.getPath());
  }

  public static List<String> listEligibleSoundFiles(File devEnvironmentPath) {
    File soundsClassFile = new File(devEnvironmentPath, SOUNDS_CLASS_FILE_PATH_RELATIVE);
    return parseSoundNames(soundsClassFile);
  }

  public static void checkSoundFilesIntegrity(File devEnvironmentPath, List<String> soundFiles) {
    // Create common File objects
    File soundsResourceFolder = new File(devEnvironmentPath, SOUNDS_FILE_FOLDER_PATH_RELATIVE);
    File soundsClassFile = new File(devEnvironmentPath, SOUNDS_CLASS_FILE_PATH_RELATIVE);
    File soundsJsonFile = new File(devEnvironmentPath, SOUNDS_JSON_FILE_PATH_RELATIVE);

    if (!soundsClassFile.exists()) {
      logError("Sounds class file does not exist: " + soundsClassFile.getPath());
    }

    if (!soundsJsonFile.exists()) {
      logError("Sounds json file does not exist: " + soundsJsonFile.getPath());
    }

    for (String soundFile : soundFiles) {
      checkSoundFileIntegrity(soundsResourceFolder, soundsClassFile, soundsJsonFile, soundFile);
    }
  }

  public static void checkSoundFileIntegrity(File soundsResourceFolder, File soundsClassFile,
      File soundsJsonFile, String soundFile) {
    try {
      validationsCount.incrementAndGet(); // Increment validation count

      // Check for sound file in sounds class file
      if (!Files.readString(soundsClassFile.toPath()).contains("\"" + soundFile + "\"")) {
        logError("Sound file entry does not exist in sounds class file: " + soundFile);
      }

      // Check for sound file in sounds json file
      validationsCount.incrementAndGet(); // Increment validation count
      String soundsJsonContents = Files.readString(soundsJsonFile.toPath());
      JsonObject soundsJson = JsonParser.parseString(soundsJsonContents).getAsJsonObject();
      if (!soundsJson.has(soundFile)) {
        logError("Sound file entry does not exist in sounds json file: " + soundFile);
      } else {
        JsonObject soundJson = soundsJson.getAsJsonObject(soundFile);
        validationsCount.incrementAndGet(); // Increment validation count
        if (!soundJson.has("category")) {
          logError("Sound file entry does not have a category in sounds json file: " + soundFile);
        }

        validationsCount.incrementAndGet(); // Increment validation count
        if (!soundJson.has("sounds")) {
          logError("Sound file entry does not have sounds in sounds json file: " + soundFile);
        } else {
          JsonArray soundsArray = soundJson.getAsJsonArray("sounds");
          // Loop through sounds array
          for (JsonElement soundElement : soundsArray) {
            JsonObject soundObject = soundElement.getAsJsonObject();

            validationsCount.incrementAndGet(); // Increment validation count

            if (!soundObject.has("stream")) {
              logError(
                  "Sound file entry does not have a stream flag in sounds json file: " + soundFile);
            }
            if (!soundObject.has("name")) {
              logError("Sound file entry does not have a name in sounds json file: " + soundFile);
            } else {
              String soundName = soundObject.get("name").getAsString();
              String modPrefixCheck = MOD_PREFIX + ":";
              if (!soundName.startsWith(modPrefixCheck)) {
                logError("Sound file entry name does not have mod prefix in sounds json file: "
                    + soundFile);
              } else {
                soundName = soundName.substring(modPrefixCheck.length());
              }

              // Check for sound file in sounds resource folder
              File soundFileOgg = new File(soundsResourceFolder, soundName + SOUND_FILE_EXTENSION);
              if (!soundFileOgg.exists()) {
                logError(
                    "Sound file (ID: " + soundFile + ") does not exist: " + soundFileOgg.getPath());
              } else {
                usedSoundFiles.add(soundFileOgg);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logError("Failed to verify sound file integrity: " + soundFile);
      e.printStackTrace();
    }
  }

  public static List<String> parseSoundNames(File file) {
    List<String> soundNames = new ArrayList<>();
    Pattern pattern = Pattern.compile("\\w+\\(\"(.*?)\"\\)"); // Pattern to match the enum values.
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(line.trim());
        if (matcher.find()) {
          soundNames.add(matcher.group(1)); // Add the matched sound name to the list.
        }
      }
    } catch (IOException e) {
      logError("Failed to parse sound names from file: " + file.getPath());
      e.printStackTrace();
    }
    return soundNames;
  }

  public static void logError(String error) {
    if (loggedErrorMessages.contains(error) && !DEBUG) {
      suppressedErrorCount.incrementAndGet();
      return;
    }
    loggedErrorMessages.add(error);
    int currentErrorCount = errorCount.incrementAndGet();
    String errorCountString = String.format("%04d", currentErrorCount);
    System.err.println("E" + errorCountString + ": " + error);
  }

  public static void checkObjModelIntegrity(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, File blockTexturesFolder, File itemTexturesFolder, File modelFileObj)
      throws Exception {
    File modelFileMtl = new File(modelFileObj.getPath().replaceAll(".obj", ".mtl"));
    usedBlockModelFiles.add(modelFileObj);
    usedBlockModelFiles.add(modelFileMtl);
    validationsCount.incrementAndGet(); // Increment validation count
    if (!modelFileObj.exists()) {
      logError("Model file does not exist: " + modelFileObj.getPath());
    }
    if (!modelFileMtl.exists()) {

      logError("Model material file does not exist: " + modelFileMtl.getPath());
    }

    if (usedBlockModelFiles.contains(modelFileObj)) {
      usedBlockModelFiles.add(modelFileMtl);
    } else if (usedItemModelFiles.contains(modelFileObj)) {
      usedItemModelFiles.add(modelFileMtl);
    } else if (usedCustomModelFiles.contains(modelFileObj)) {
      usedCustomModelFiles.add(modelFileMtl);
    } else {
      logError("Model file use is unknown: " + modelFileObj.getPath());
    }

    // Read mtl file contents
    List<String> mtlFileContents = Files.readAllLines(modelFileMtl.toPath());
    for (String line : mtlFileContents) {
      if (line.startsWith("map_Kd")) {
        String textureFileName = line.substring("map_Kd".length()).trim();
        String modPrefixCheck = MOD_PREFIX + ":";
        textureFileName = textureFileName.substring(modPrefixCheck.length()) + ".png";
        boolean isItemTexture = textureFileName.startsWith("items/");
        textureFileName =
            textureFileName.substring(isItemTexture ? "items/".length() : "blocks/".length());
        File textureFile =
            new File(isItemTexture ? itemTexturesFolder : blockTexturesFolder, textureFileName);
        File textureFileMcMeta = new File(isItemTexture ? itemTexturesFolder : blockTexturesFolder,
            textureFileName + ".mcmeta");
        if (!textureFile.exists()) {
          logError("Texture file does not exist: " + textureFile.getPath());
        }

        usedBlockTexturesFiles.add(textureFile);
        usedBlockTexturesFiles.add(textureFileMcMeta);

      }
    }

  }

  public static void checkJsonModelIntegrity(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, File blockTexturesFolder, File itemTexturesFolder,
      File modelFileJson) throws Exception {

    validationsCount.incrementAndGet(); // Increment validation count
    if (!modelFileJson.exists()) {
      logError("Model file does not exist: " + modelFileJson.getPath());
    }

    String modelJsonRaw = Files.readString(modelFileJson.toPath());
    JsonObject modelJson = JsonParser.parseString(modelJsonRaw).getAsJsonObject();

    // Check for parent model file (if exists)
    String prefixCheck = MOD_PREFIX + ":";
    if (modelJson.has("parent")) {
      String parentValue = modelJson.get("parent").getAsString();
      if (parentValue.startsWith(prefixCheck)) {
        // Check parent model file (recursively)
        String strippedParentValue = parentValue.substring(prefixCheck.length());

        File parentModelFolder = null;
        String modelFileName = null;
        List<File> usedModelFiles = null;
        if (strippedParentValue.startsWith("custom/")) {
          parentModelFolder = customModelsFolder;
          modelFileName = strippedParentValue.substring("custom/".length());
          usedModelFiles = usedCustomModelFiles;
        } else if (strippedParentValue.startsWith("block/")) {
          parentModelFolder = blockModelsFolder;
          modelFileName = strippedParentValue.substring("block/".length());
          usedModelFiles = usedBlockModelFiles;
        } else if (strippedParentValue.startsWith("item/")) {
          parentModelFolder = itemModelsFolder;
          modelFileName = strippedParentValue.substring("item/".length());
          usedModelFiles = usedItemModelFiles;
        }

        if (parentModelFolder != null && modelFileName != null && usedModelFiles != null) {
          File parentModelFile = new File(parentModelFolder, modelFileName + ".json");
          checkJsonModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
              blockTexturesFolder, itemTexturesFolder, parentModelFile);
          usedModelFiles.add(parentModelFile);
        } else {
          logError("Unexpected or invalid parent model value: " + parentValue);
        }
      }
    }

    // Check textures block
    if (modelJson.has("textures")) {
      validateTexturesBlockJson(blockTexturesFolder, itemTexturesFolder, modelFileJson,
          modelJson.getAsJsonObject("textures"));
    }
  }

  public static void validateTexturesBlockJson(File blockTexturesFolder, File itemTexturesFolder,
      File validateFile, JsonObject texturesBlockToCheck) throws Exception {
    String prefixCheck = MOD_PREFIX + ":";
    for (String textureKey : texturesBlockToCheck.keySet()) {
      validationsCount.incrementAndGet(); // Increment validation count
      String textureValue = texturesBlockToCheck.get(textureKey).getAsString();
      if (textureValue.startsWith(prefixCheck)) {
        String strippedTextureValue = textureValue.substring(prefixCheck.length());
        String textureFileName = null;
        File textureFile = null;
        if (strippedTextureValue.startsWith("blocks/")) {
          textureFileName = strippedTextureValue.substring("blocks/".length()) + ".png";
          textureFile = new File(blockTexturesFolder, textureFileName);
          usedBlockTexturesFiles.add(textureFile);
          usedBlockTexturesFiles.add(new File(blockTexturesFolder, textureFileName + ".mcmeta"));
        } else if (strippedTextureValue.startsWith("items/")) {
          textureFileName = strippedTextureValue.substring("items/".length()) + ".png";
          textureFile = new File(itemTexturesFolder, textureFileName);
          usedItemTexturesFiles.add(textureFile);
          usedItemTexturesFiles.add(new File(itemTexturesFolder, textureFileName + ".mcmeta"));
        }
        if (textureFile == null) {
          logError(
              "Texture '" + textureValue + "' does not exist (--) in " + validateFile.getPath());
        } else if (!textureFile.exists()) {
          logError(
              "Texture '" + textureValue + "' does not exist (" + textureFile.getPath() + ") in "
                  + validateFile.getPath());
        }
      } else if (!textureValue.startsWith("minecraft:")) {
        logError("Texture '" + textureValue + "' is not a valid texture value in "
            + validateFile.getPath());
      }
    }

  }

  public static void validateTexturesBlocksJson(File blockTexturesFolder, File itemTexturesFolder,
      File validateFile, List<JsonObject> texturesBlocksToCheck) throws Exception {
    for (JsonObject texturesBlockToCheck : texturesBlocksToCheck) {
      validateTexturesBlockJson(blockTexturesFolder, itemTexturesFolder, validateFile,
          texturesBlockToCheck);
    }
  }


  public static void checkAndLogMetaText(String fileContents) {
    // Regular expression pattern to match I18n.format("...") calls.
    // It accounts for any characters (including new lines and spaces) between 'I18n.format(' and
    // the next ')'
    // and captures the string literal argument inside the parentheses.
    String regex = "I18n\\.format\\(\\s*\"([^\"]*)\"\\s*\\)";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(fileContents);

    while (matcher.find()) {
      // Add the captured string literal (the argument of I18n.format call) to the list
      knownMetaLangs.add(matcher.group(1));
    }
  }

  public static void verifyItemIntegrity(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, File blockstateFolder, File langFolder, File blockTexturesFolder,
      File itemTexturesFolder, File sourceFile) {
    try {
      // Read file contents
      String fileContents = Files.readString(sourceFile.toPath());

      // Get item ID from block code file
      String itemId = getBlockItemIdFromSourceFileContents(sourceFile, fileContents, true);

      // Check and log meta text
      checkAndLogMetaText(fileContents);

      // Check for lang file entries
      for (File langFile : Objects.requireNonNull(langFolder.listFiles())) {
        if (langFile.getName().endsWith(LANG_FILE_EXTENSION)) {
          validationsCount.incrementAndGet(); // Increment validation count
          String fullyQualifiedNameItem = "item." + itemId + ".name";
          if (!Files.readString(langFile.toPath()).contains(fullyQualifiedNameItem)) {
            logError(
                "Lang file entry does not exist for item: " + itemId + " in " + langFile.getPath());
          }
        }
      }

      // Check for item model file
      validationsCount.incrementAndGet(); // Increment validation count
      File itemModelFileJson = new File(itemModelsFolder, itemId + ".json");
      File itemModelFileObj = new File(itemModelsFolder, itemId + ".obj");
      if (!itemModelFileJson.exists() && !itemModelFileObj.exists()) {
        logError("Item model file does not exist: " + itemModelFileJson.getPath());
      }

      // If item model file exists and is JSON, check integrity
      if (itemModelFileJson.exists()) {
        // Validation count is incremented in checkJsonModelIntegrity
        usedItemModelFiles.add(itemModelFileJson);
        checkJsonModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
            blockTexturesFolder, itemTexturesFolder, itemModelFileJson);
      }

      if (itemModelFileObj.exists()) {
        usedItemModelFiles.add(itemModelFileObj);
        checkObjModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
            blockTexturesFolder, itemTexturesFolder, itemModelFileObj);
      }
    } catch (Exception e) {

      logError("Failed to verify item file integrity: " + sourceFile.getPath());
      e.printStackTrace();
    }
  }

  public static void verifyBlockStateIntegrity(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, File blockstateFolder, File langFolder, File blockTexturesFolder,
      File itemTexturesFolder, String checkBlockId) throws Exception {
    List<String> blockIds = new ArrayList<>();
    blockIds.add(checkBlockId);

    if (blockSetBlockIds.contains(checkBlockId)) {
      String[] variantFileNames = {"fence", "slab", "slab_double", "stairs"};
      for (String variantFileName : variantFileNames) {
        blockIds.add(checkBlockId + "_" + variantFileName);
        knownBlockIds.add(checkBlockId + "_" + variantFileName);
      }
      // blockIds.add(checkBlockId + "_fence_inventory");
      // blockIds.add(checkBlockId + "_fence_post");
      // blockIds.add(checkBlockId + "_slab_top");
      // blockIds.add(checkBlockId + "_stairs_inner");
      // blockIds.add(checkBlockId + "_stairs_outer");
    }

    for (String blockId : blockIds) {
      validationsCount.incrementAndGet(); // Increment validation count
      File blockstateFileJson = new File(blockstateFolder, blockId + BLOCKSTATE_FILE_EXTENSION);
      if (!blockstateFileJson.exists()) {
        logError("Blockstate file does not exist: " + blockstateFileJson.getPath());
      } else {
        usedBlockstateFiles.add(blockstateFileJson);
      }

      // Check for variants/inventory in blockstate file
      boolean hasInventoryVariant = false;
      boolean hasInventoryItemModel = false;
      File itemModelFileJson = new File(itemModelsFolder, blockId + ".json");
      String blockstateJsonRaw = Files.readString(blockstateFileJson.toPath());
      JsonObject blockstateJson = JsonParser.parseString(blockstateJsonRaw).getAsJsonObject();
      if (blockstateJson.has("variants")) {
        JsonObject variants = blockstateJson.getAsJsonObject("variants");
        if (variants.has("inventory")) {
          hasInventoryVariant = true;
        }
      }
      hasInventoryItemModel = itemModelFileJson.exists();
      if (!hasInventoryVariant && !hasInventoryItemModel) {
        logError("Block with ID '" + blockId
            + "' does not have an inventory variant in blockstate file or an item model file (for"
            + " inventory display)!");
      }
      if (hasInventoryItemModel) {
        checkJsonModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
            blockTexturesFolder, itemTexturesFolder, itemModelFileJson);
      }
      if (!hasInventoryVariant) {
        usedItemModelFiles.add(itemModelFileJson);
      }

      // heck for model files in blockstate file
      List<File> dugModels =
          digForModels(blockModelsFolder, itemModelsFolder, customModelsFolder, blockstateJson);
      for (File file : dugModels) {
        if (!file.exists()) {
          logError("Model file does not exist: " + file.getPath());
        }

        usedBlockModelFiles.add(file);
        if (file.getName().endsWith(".json")) {
          checkJsonModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
              blockTexturesFolder, itemTexturesFolder, file);
        }
        if (file.getName().endsWith(".obj")) {
          checkObjModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
              blockTexturesFolder, itemTexturesFolder, file);
        }
      }

      // Check for textures blocks in blockstate file and validate
      List<JsonObject> texturesBlocks = digForTexturesBlocks(blockstateJson);
      if (!texturesBlocks.isEmpty()) {
        validateTexturesBlocksJson(blockTexturesFolder, itemTexturesFolder, blockstateFileJson,
            texturesBlocks);
      }
    }
  }

  public static void verifyBlockIntegrity(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, File blockstateFolder, File langFolder, File blockTexturesFolder,
      File itemTexturesFolder, File sourceFile) {
    try {
      // Read file contents
      String fileContents = Files.readString(sourceFile.toPath());

      // Get block ID from block code file
      String blockId = getBlockItemIdFromSourceFileContents(sourceFile, fileContents, false);

      // Check and log meta text
      checkAndLogMetaText(fileContents);

      // Check for blockstate file
      verifyBlockStateIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
          blockstateFolder, langFolder, blockTexturesFolder, itemTexturesFolder, blockId);

      // Check if block model file exists
      boolean hasBlockModel = false;
      String blockModelFileName = blockId + ".json";
      File blockModelFile = new File(blockModelsFolder, blockModelFileName);
      hasBlockModel = blockModelFile.exists();
      if (hasBlockModel) {
        usedBlockModelFiles.add(blockModelFile);
        checkJsonModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
            blockTexturesFolder, itemTexturesFolder, blockModelFile);
        if (blockSetBlockIds.contains(blockId)) {
          List<File> variantFiles = new ArrayList<>();
          variantFiles.add(new File(blockModelsFolder, blockId + "_fence.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_fence_inventory.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_fence_post.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_slab.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_slab_double.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_slab_top.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_stairs.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_stairs_inner.json"));
          variantFiles.add(new File(blockModelsFolder, blockId + "_stairs_outer.json"));

          for (File variantFile : variantFiles) {
            usedBlockModelFiles.add(variantFile);
            checkJsonModelIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
                blockTexturesFolder, itemTexturesFolder, variantFile);
          }
        }
      }

      // Check for lang file entries
      for (File langFile : Objects.requireNonNull(langFolder.listFiles())) {
        if (langFile.getName().endsWith(LANG_FILE_EXTENSION)) {
          validationsCount.incrementAndGet(); // Increment validation count
          String fullyQualifiedNameBlock = "tile." + blockId + ".name";
          if (!Files.readString(langFile.toPath()).contains(fullyQualifiedNameBlock)) {
            logError("Lang file entry does not exist for block: " + blockId + " in "
                + langFile.getPath());
          }
        }
      }


    } catch (Exception e) {
      logError("Failed to verify block file integrity: " + sourceFile.getPath());
      e.printStackTrace();
    }
  }

  public static List<File> digForModels(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, JsonObject jsonToCheck) {
    // Recursively get all JsonObjects with key "textures"
    List<File> modelFiles = new ArrayList<>();
    digForModels(blockModelsFolder, itemModelsFolder, customModelsFolder, jsonToCheck, modelFiles);
    return modelFiles;
  }

  public static void digForModels(File blockModelsFolder, File itemModelsFolder,
      File customModelsFolder, JsonObject jsonToCheck, List<File> modelFiles) {
    if (jsonToCheck == null) {
      return;
    }

    String prefixCheck = MOD_PREFIX + ":";
    for (String key : jsonToCheck.keySet()) {
      JsonElement element = jsonToCheck.get(key);
      if (key.equals("model") && element.isJsonPrimitive()) {
        String modelValue = element.getAsString();
        if (modelValue.startsWith(prefixCheck)) {
          String strippedModelValue = modelValue.substring(prefixCheck.length());
          String modelFileName = strippedModelValue + ".json";
          File modelFolder = blockModelsFolder;
          if (strippedModelValue.startsWith("custom/")) {
            modelFolder = customModelsFolder;
            modelFileName = strippedModelValue.substring("custom/".length());
          } else if (strippedModelValue.startsWith("block/")) {
            modelFolder = blockModelsFolder;
            modelFileName = strippedModelValue.substring("block/".length());
          } else if (strippedModelValue.startsWith("item/")) {
            modelFolder = itemModelsFolder;
            modelFileName = strippedModelValue.substring("item/".length());
          }
          if (strippedModelValue.endsWith(".obj")) {
            modelFileName = strippedModelValue;
            File modelFileMtl = new File(modelFolder, modelFileName.replaceAll(".obj", ".mtl"));
            modelFiles.add(modelFileMtl);
          }
          File modelFile = new File(modelFolder, modelFileName);
          modelFiles.add(modelFile);
        }
      } else if (element.isJsonObject()) {
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("model")) {
          String modelValue = obj.get("model").getAsString();
          if (modelValue.startsWith(prefixCheck)) {
            String strippedModelValue = modelValue.substring(prefixCheck.length());
            String modelFileName = strippedModelValue + ".json";
            File modelFolder = blockModelsFolder;
            if (strippedModelValue.startsWith("custom/")) {
              modelFolder = customModelsFolder;
              modelFileName = strippedModelValue.substring("custom/".length());
            } else if (strippedModelValue.startsWith("block/")) {
              modelFolder = blockModelsFolder;
              modelFileName = strippedModelValue.substring("block/".length());
            } else if (strippedModelValue.startsWith("item/")) {
              modelFolder = itemModelsFolder;
              modelFileName = strippedModelValue.substring("item/".length());
            }
            if (strippedModelValue.endsWith(".obj")) {
              modelFileName = strippedModelValue;
              File modelFileMtl = new File(modelFolder, modelFileName.replaceAll(".obj", ".mtl"));
              modelFiles.add(modelFileMtl);
            }
            File modelFile = new File(modelFolder, modelFileName);
            modelFiles.add(modelFile);
          }
        }
        digForModels(blockModelsFolder, itemModelsFolder, customModelsFolder, obj, modelFiles);
      } else if (element.isJsonArray()) {
        for (JsonElement arrayElement : element.getAsJsonArray()) {
          if (arrayElement.isJsonObject()) {
            JsonObject obj = arrayElement.getAsJsonObject();
            if (obj.has("model")) {
              String modelValue = obj.get("model").getAsString();
              if (modelValue.startsWith(prefixCheck)) {
                String strippedModelValue = modelValue.substring(prefixCheck.length());
                String modelFileName = strippedModelValue + ".json";
                File modelFolder = blockModelsFolder;
                if (strippedModelValue.startsWith("custom/")) {
                  modelFolder = customModelsFolder;
                  modelFileName = strippedModelValue.substring("custom/".length());
                } else if (strippedModelValue.startsWith("block/")) {
                  modelFolder = blockModelsFolder;
                  modelFileName = strippedModelValue.substring("block/".length());
                } else if (strippedModelValue.startsWith("item/")) {
                  modelFolder = itemModelsFolder;
                  modelFileName = strippedModelValue.substring("item/".length());
                }
                if (strippedModelValue.endsWith(".obj")) {
                  modelFileName = strippedModelValue;
                  File modelFileMtl =
                      new File(modelFolder, modelFileName.replaceAll(".obj", ".mtl"));
                  modelFiles.add(modelFileMtl);
                }
                File modelFile = new File(modelFolder, modelFileName);
                modelFiles.add(modelFile);
              }
            }
            digForModels(blockModelsFolder, itemModelsFolder, customModelsFolder, obj, modelFiles);
          }
        }
      }
    }
  }

  public static List<JsonObject> digForTexturesBlocks(JsonObject jsonToCheck) {
    // Recursively get all JsonObjects with key "textures"
    List<JsonObject> texturesBlocks = new ArrayList<>();
    digForTexturesBlocks(jsonToCheck, texturesBlocks);
    return texturesBlocks;
  }

  public static void digForTexturesBlocks(JsonObject jsonToCheck, List<JsonObject> texturesBlocks) {
    if (jsonToCheck == null) {
      return;
    }

    if (jsonToCheck.has("textures")) {
      texturesBlocks.add(jsonToCheck.getAsJsonObject("textures"));
    }

    for (String key : jsonToCheck.keySet()) {
      JsonElement element = jsonToCheck.get(key);
      if (key.equals("textures") && element.isJsonObject()) {
        texturesBlocks.add(element.getAsJsonObject());
      } else if (element.isJsonObject()) {
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("textures")) {
          texturesBlocks.add(obj.getAsJsonObject("textures"));
        }
        digForTexturesBlocks(obj, texturesBlocks);
      } else if (element.isJsonArray()) {
        for (JsonElement arrayElement : element.getAsJsonArray()) {
          if (arrayElement.isJsonObject()) {
            JsonObject obj = arrayElement.getAsJsonObject();
            if (obj.has("textures")) {
              texturesBlocks.add(obj.getAsJsonObject("textures"));
            }
            digForTexturesBlocks(obj, texturesBlocks);
          }
        }
      }
    }
  }

  /**
   * Builds a list of source files to exclude from verification.
   *
   * @param devEnvironmentPath The dev environment path.
   *
   * @return The list of source files to exclude.
   */
  public static List<File> buildSourceExcludes(File devEnvironmentPath) {
    List<File> excludes = new ArrayList<>();
    for (String exclude : SOURCE_FILE_EXCLUDES) {
      excludes.add(new File(devEnvironmentPath, exclude));
    }
    return excludes;
  }

  /**
   * Verifies the integrity of all eligible block and item source files in the given dev environment
   * path.
   *
   * @param devEnvironmentPath The dev environment path.
   * @param sourceExcludes     The list of source files to exclude from verification.
   * @param blockSourceFiles   The list of eligible block source files.
   * @param itemSourceFiles    The list of eligible item source files.
   */
  public static void verifyBlockItemIntegritys(File devEnvironmentPath, List<File> sourceExcludes,
      List<File> blockSourceFiles, List<File> itemSourceFiles) {
    // Check if source files are empty
    if (blockSourceFiles == null) {
      System.err.println("No block source files found.");
      return;
    }
    if (itemSourceFiles == null) {
      System.err.println("No item source files found.");
      return;
    }

    // Check if source files are eligible
    if (blockSourceFiles.isEmpty()) {
      System.err.println("No eligible block source files found.");
      return;
    }
    if (itemSourceFiles.isEmpty()) {
      System.err.println("No eligible item source files found.");
      return;
    }

    // Print eligible source files
    if (DEBUG) {
      System.out.println("Eligible block source files:");
      for (File sourceFile : blockSourceFiles) {
        System.out.println("  " + sourceFile.getName());
      }
      System.out.println("Eligible item source files:");
      for (File sourceFile : itemSourceFiles) {
        System.out.println("  " + sourceFile.getName());
      }
    }

    // Build useful files/object references
    final File blockstateFolder =
        new File(devEnvironmentPath, BLOCKSTATE_FILE_FOLDER_PATH_RELATIVE);
    final File langFolder = new File(devEnvironmentPath, LANG_FILE_FOLDER_PATH_RELATIVE);
    final File blockModelsFolder =
        new File(devEnvironmentPath, BLOCK_MODELS_FILE_FOLDER_PATH_RELATIVE);
    final File itemModelsFolder =
        new File(devEnvironmentPath, ITEM_MODELS_FILE_FOLDER_PATH_RELATIVE);
    final File customModelsFolder =
        new File(devEnvironmentPath, CUSTOM_MODELS_FILE_FOLDER_PATH_RELATIVE);
    final File blockTexturesFolder =
        new File(devEnvironmentPath, BLOCK_TEXTURES_FILE_FOLDER_PATH_RELATIVE);
    final File itemTexturesFolder =
        new File(devEnvironmentPath, ITEM_TEXTURES_FILE_FOLDER_PATH_RELATIVE);

    // Loop through source files
    Thread blocksThread = new Thread(() -> {
      for (File sourceFile : blockSourceFiles) {
        // Skip excluded files
        if (sourceExcludes.contains(sourceFile)) {
          if (DEBUG) {
            System.out.println("Skipping excluded file: " + sourceFile.getPath());
          }
          continue;
        }

        // Verify block/item integrity
        checkCount.incrementAndGet();
        verifyBlockIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
            blockstateFolder, langFolder, blockTexturesFolder, itemTexturesFolder, sourceFile);
      }
    });
    blocksThread.start();
    if (DEBUG) {
      System.out.println("[THREADMGMT] Started block check thread.");
    }
    Thread itemsThread = new Thread(() -> {
      for (File sourceFile : itemSourceFiles) {
        // Skip excluded files
        if (sourceExcludes.contains(sourceFile)) {
          if (DEBUG) {
            System.out.println("Skipping excluded file: " + sourceFile.getPath());
          }
          continue;
        }

        // Verify block/item integrity
        checkCount.incrementAndGet();
        verifyItemIntegrity(blockModelsFolder, itemModelsFolder, customModelsFolder,
            blockstateFolder, langFolder, blockTexturesFolder, itemTexturesFolder, sourceFile);
      }
    });
    itemsThread.start();
    if (DEBUG) {
      System.out.println("[THREADMGMT] Started item check thread.");
    }

    try {
      if (DEBUG) {
        System.out.println("[THREADMGMT] Joining threads...");
      }
      blocksThread.join();
      itemsThread.join();
      if (DEBUG) {
        System.out.println("[THREADMGMT] Threads joined.");
      }
    } catch (InterruptedException e) {
      System.err.println(
          "[THREADMGMT] Thread join interrupted. Remaining threads may not have completed. Results "
              + "may be inaccurate.");
      e.printStackTrace();
    }
  }

  /**
   * Gets the block/item ID from the source file contents.
   *
   * @param file         The source file.
   * @param fileContents The contents of the source file.
   * @param asItem       Whether to get the ID as an item or block.
   *
   * @return The block/item ID.
   *
   * @throws Exception If the block/item ID could not be found or an error occurred.
   */
  public static String getBlockItemIdFromSourceFileContents(File file, String fileContents,
      boolean asItem) throws Exception {
    String filterMethodName = asItem ? "getItemRegistryName" : "getBlockRegistryName";

    String blockId = getIdFromSourceFileContents(file, fileContents, filterMethodName);

    if (asItem) {
      knownItemIds.add(blockId);
      if (DEBUG) {
        System.out.println("Found item ID: " + blockId);
      }
    } else {
      knownBlockIds.add(blockId);
      if (DEBUG) {
        System.out.println("Found block ID: " + blockId);
      }
    }
    return blockId;
  }

  public static String getIdFromSourceFileContents(File file, String fileContents,
      String filterMethodName) throws Exception {
    // Get block ID
    final String filePath = file.getPath();
    String blockIdRegex =
        "public\\sString\\s" + filterMethodName + "\\(\\)\\s?\\{\\s*return\\s?\"(.*)\";";
    int blockIdIndex = 1;
    Matcher matcher = Pattern.compile(blockIdRegex).matcher(fileContents);
    String blockId;
    if (matcher.find()) {
      blockId = matcher.group(blockIdIndex);

    } else {
      throw new Exception("Failed to get ID from file: " + filePath);
    }
    return blockId;
  }

  /**
   * Lists all eligible block source files in the given dev environment path.
   *
   * @param devEnvironmentPath The dev environment path.
   *
   * @return The list of eligible block source files.
   */
  public static List<File> listEligibleBlockSourceFiles(File devEnvironmentPath) {
    // Get source file folder path
    File sourceFileFolderPath = new File(devEnvironmentPath, SOURCE_FILE_FOLDER_PATH_RELATIVE);

    // Get source files
    List<File> eligibleFiles = new ArrayList<>(listFilesMatchingCriteria(sourceFileFolderPath,
        (file) -> isFileEligible(file.getParentFile(), file.getName(), false)));

    // Return source files
    return eligibleFiles;
  }

  /**
   * Lists all eligible item source files in the given dev environment path.
   *
   * @param devEnvironmentPath The dev environment path.
   *
   * @return The list of eligible item source files.
   */
  public static List<File> listEligibleItemSourceFiles(File devEnvironmentPath) {
    // Get source file folder path
    File sourceFileFolderPath = new File(devEnvironmentPath, SOURCE_FILE_FOLDER_PATH_RELATIVE);

    // Get source files
    List<File> eligibleFiles = new ArrayList<>(listFilesMatchingCriteria(sourceFileFolderPath,
        (file) -> isFileEligible(file.getParentFile(), file.getName(), true)));

    // Return source files
    return eligibleFiles;
  }


  /**
   * Determines if a source file is eligible based on the file name and contents.
   *
   * @param dir     The directory of the source file.
   * @param name    The name of the source file.
   * @param asItems Whether to check for eligibility as an item or block.
   *
   * @return Whether the file is eligible.
   */
  public static boolean isFileEligible(File dir, String name, boolean asItems) {
    // Check if ends with .java
    if (!name.endsWith(SOURCE_FILE_EXTENSION)) {
      return false;
    }

    // Read file and check if contains eligible extends
    try {
      String fileContents = Files.readString(new File(dir, name).toPath());
      String fileContentsNoLineBreaks = fileContents == null
          ? null
          : fileContents.replaceAll("\\r\\n", " ").replaceAll("\\r", " ").replaceAll("\\n", " ")
              .replaceAll("\\s+", " ").trim();
      String[] eligibleExtends =
          asItems ? SOURCE_FILE_ELIGIBLE_EXTENDS_ITEMS : SOURCE_FILE_ELIGIBLE_EXTENDS_BLOCKS;
      for (String eligibleExtend : eligibleExtends) {
        String extendsStatement = "extends " + eligibleExtend;
        if (fileContentsNoLineBreaks != null && fileContentsNoLineBreaks.contains(
            extendsStatement)) {
          if (fileContentsNoLineBreaks.contains("extends AbstractBlockSetBasic")) {
            String blockItemIdFromSourceFileContents =
                getBlockItemIdFromSourceFileContents(new File(dir, name), fileContentsNoLineBreaks,
                    false);
            blockSetBlockIds.add(blockItemIdFromSourceFileContents);
          }
          return true;
        }
      }
    } catch (Exception e) {
      System.out.println("Error reading file: " + name);
      e.printStackTrace();
    }

    return false;
  }

  private static List<File> listFilesMatchingCriteria(File directory, Predicate<File> criteria) {
    List<File> matchedFiles = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(directory.toPath()).parallel()) { // Use parallel stream
      paths.filter(Files::isRegularFile).map(Path::toFile).filter(criteria)
          .forEach(matchedFiles::add);
    } catch (IOException e) {
      // Log or handle the error appropriately
      System.out.println("Error listing files in root: " + directory.getPath());
      e.printStackTrace();
    }
    return matchedFiles;
  }


}