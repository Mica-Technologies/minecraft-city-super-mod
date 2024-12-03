package com.micatechnologies.minecraft.csm.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class SignModTool {

  public static void main(String[] args) {

    CsmToolUtility.doToolExecuteWrapped("CSM Sign Mod Tool", args, (devEnvironmentPath) -> {
      final String signsCodeFolderPathRelative =
          "src/main/java/com/micatechnologies/minecraft/csm/trafficsigns";
      final String blockStateFolderPathRelative = "src/main/resources/assets/csm/blockstates";
      File signsCodeFolder = new File(devEnvironmentPath, signsCodeFolderPathRelative);
      File blockStateFolder = new File(devEnvironmentPath, blockStateFolderPathRelative);

      System.out.println("Building list of eligible sign classes...");
      List<String> signs = buildListOfSigns(signsCodeFolder);
      System.out.println("Done! Found " + signs.size() + " eligible sign classes.");

      // Update blockstate files with new property support
      System.out.println("Updating blockstate files...");
      updateSignBlockStateFiles(signs, blockStateFolder);
      System.out.println("Done!");
    });
  }

  public static void updateSignBlockStateFiles(List<String> signBlockIds, File blockStateFolder)
      throws Exception {
    for (String blockId : signBlockIds) {
      String blockIdFileName = blockId + ".json";
      File blockStateFile = new File(blockStateFolder, blockIdFileName);
      if (blockStateFile.exists()) {
        System.out.println("\tUpdating blockstate file: " + blockIdFileName);
        try {
          // Update blockstate file contents
          updateBlockStateFileContents(blockId, blockStateFile);
        } catch (Exception e) {
          System.err.println("\tFailed to update blockstate file: " + blockIdFileName);
        }
      } else {
        throw new Exception("Blockstate file does not exist: " + blockIdFileName);
      }
    }
  }

  public static void updateBlockStateFileContents(String blockId, File blockStateFile)
      throws Exception {
    // Read the blockstate file
    String blockStateJson = FileUtils.readFileToString(blockStateFile, "UTF-8");
    JsonObject blockStateJsonObject = new Gson().fromJson(blockStateJson, JsonObject.class);

    // Fetch the default model
    JsonObject defaults = blockStateJsonObject.getAsJsonObject("defaults");
    String defaultModel = defaults.get("model").getAsString();

    // Create variants
    JsonObject variants = createVariants(defaultModel);

    // Update the blockstate JSON object
    blockStateJsonObject.add("variants", variants);

    // Write the updated JSON back to the blockstate file
    Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    FileUtils.writeStringToFile(blockStateFile, gson.toJson(blockStateJsonObject), "UTF-8");
  }

  public static JsonObject createVariants(String defaultModel) {
    JsonObject variants = new JsonObject();

    // Define facing transformations
    JsonObject facing = new JsonObject();
    facing.add("n", new JsonObject());
    facing.add("nw", createRotationJson(45));
    facing.add("w", createRotationJson(90));
    facing.add("sw", createRotationJson(135));
    facing.add("s", createRotationJson(180));
    facing.add("se", createRotationJson(225));
    facing.add("e", createRotationJson(270));
    facing.add("ne", createRotationJson(315));

    // Add facing to variants
    variants.add("facing", facing);

    // Add inventory variant
    JsonArray inventoryArray = new JsonArray();
    inventoryArray.add(new JsonObject());
    variants.add("inventory", inventoryArray);

    // Define downward submodel
    JsonObject downwardFalse = new JsonObject();
    JsonObject downwardTrue = new JsonObject();
    downwardTrue.add("submodel", createDownwardJson());

    JsonObject downward = new JsonObject();
    downward.add("false", downwardFalse);
    downward.add("true", downwardTrue);

    // Add downward to variants
    variants.add("downward", downward);

    // Define setback model switch
    JsonObject setbackFalse = new JsonObject();
    JsonObject setbackTrue = new JsonObject();
    setbackTrue.add("model", new JsonPrimitive(defaultModel + "_setback"));

    JsonObject setback = new JsonObject();
    setback.add("false", setbackFalse);
    setback.add("true", setbackTrue);

    // Add setback to variants
    variants.add("setback", setback);

    return variants;
  }

  private static JsonObject createRotationJson(int angleY) {
    JsonObject rotation = new JsonObject();
    JsonArray rotationArray = new JsonArray();
    rotationArray.add(createRotationComponent("x", 0));
    rotationArray.add(createRotationComponent("y", angleY));
    rotationArray.add(createRotationComponent("z", 0));

    JsonObject transformObject = new JsonObject();
    transformObject.add("rotation", rotationArray);

    rotation.add("transform", transformObject);
    return rotation;
  }

  private static JsonObject createRotationComponent(String axis, int value) {
    JsonObject component = new JsonObject();
    component.add(axis, new JsonPrimitive(value));
    return component;
  }

  private static JsonObject createDownwardJson() {
    JsonObject extension = new JsonObject();
    extension.add("model", new JsonPrimitive("csm:metal_signpost"));
    JsonArray translationArray = new JsonArray();
    translationArray.add(new JsonPrimitive(0.0));
    translationArray.add(new JsonPrimitive(-1.0));
    translationArray.add(new JsonPrimitive(0.0));

    JsonObject transformObject = new JsonObject();
    transformObject.add("translation", translationArray);

    extension.add("transform", transformObject);

    JsonObject submodel = new JsonObject();
    submodel.add("extension", extension);

    return submodel;
  }

  public static List<String> buildListOfSigns(File signsCodeFolder) throws Exception {
    // Loop through all files in the signs code folder (and subfolders)
    List<String> eligibleSignClasses = new ArrayList<>();
    List<Exception> exceptions = new ArrayList<>();
    Files.walkFileTree(signsCodeFolder.toPath(), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        try {
          // Check if file is a Java file
          if (file.toString().endsWith(".java")) {
            // Read file contents
            String fileContents = FileUtils.readFileToString(file.toFile());
            // Check if file extends AbstractBlockSign
            if (checkExtendsAbstractBlockSign(file.toFile(), fileContents)) {
              // Get block ID
              String blockId = getBlockIdFromBlockCodeFileContents(file.toFile(), fileContents);
              // Add block ID to list of eligible sign classes
              eligibleSignClasses.add(blockId);
            }
          }
        } catch (Exception e) {
          exceptions.add(e);
          return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
      }
    });

    if (!exceptions.isEmpty()) {
      throw new Exception("Failed to build list of eligible sign classes.", exceptions.get(0));
    }

    return eligibleSignClasses;
  }

  public static boolean checkExtendsAbstractBlockSign(File file, String fileContents) {
    final String filePath = file.getPath();
    return fileContents.contains("extends AbstractBlockSign");
  }

  public static String getBlockIdFromBlockCodeFileContents(File file, String fileContents)
      throws Exception {
    // Get block ID
    final String filePath = file.getPath();
    String blockIdRegex =
        "public\\sString\\sgetBlockRegistryName\\(\\)\\s?\\{\\s*return\\s?\"(.*)\";";
    int blockIdIndex = 1;
    Matcher matcher = Pattern.compile(blockIdRegex).matcher(fileContents);
    String blockId;
    if (matcher.find()) {
      blockId = matcher.group(blockIdIndex);
    } else {
      throw new Exception("Failed to get block ID from file: " + filePath);
    }
    return blockId;
  }

}