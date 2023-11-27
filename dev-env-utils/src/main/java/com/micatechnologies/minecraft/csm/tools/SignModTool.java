package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
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
      String blockStateFileName = blockId + ".json";
      File blockStateFile = new File(blockStateFolder, blockStateFileName);
      if (blockStateFile.exists()) {
        System.out.println("\tUpdating blockstate file: " + blockStateFileName);
        try {
          String blockStateFileContents = FileUtils.readFileToString(blockStateFile);
          String blockStateFileContentsUpdated =
              updateBlockStateFileContents(blockStateFileContents);
          FileUtils.writeStringToFile(blockStateFile, blockStateFileContentsUpdated);
        } catch (Exception e) {
          System.err.println("\tFailed to update blockstate file: " + blockStateFileName);
        }
      } else {
        throw new Exception("Blockstate file does not exist: " + blockStateFileName);
      }
    }
  }

  public static List<String> buildListOfSigns(File signsCodeFolder) throws Exception {
    List<String> signs = new ArrayList<>();
    for (File signFile : signsCodeFolder.listFiles()) {
      if (signFile.isDirectory()) {
        System.out.println("\tEntering directory: " + signFile.getName());
        buildListOfSigns(signFile);
      } else if (signFile.getName().endsWith(".java")) {
        System.out.println("\tProcessing Java file: " + signFile.getName());
        String fileContents = FileUtils.readFileToString(signFile);
        if (checkExtendsAbstractBlockSign(signFile, fileContents)) {
          System.out.println("\t\tFound eligible sign class: " + signFile.getName());
          signs.add(getBlockIdFromBlockCodeFileContents(signFile, fileContents));
        } else {
          System.out.println("\t\tSkipping non-sign class: " + signFile.getName());
        }

      } else {
        System.out.println("\tSkipping non-Java file: " + signFile.getName());
      }
    }
    return signs;
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