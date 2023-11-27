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


  public static void updateSignBlockStateFiles(List<String> signBlockIds, File blockStateFolder
  ) throws Exception {
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

    JsonObject defaults = blockStateJsonObject.getAsJsonObject("defaults");
    if (defaults.has("uvlock")) {
      defaults.remove("uvlock");
    }

    // Update the blockstate JSON object
    blockStateJsonObject.add("defaults", defaults);

    // Write the updated JSON back to the blockstate file
    Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    FileUtils.writeStringToFile(blockStateFile, gson.toJson(blockStateJsonObject), "UTF-8");
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