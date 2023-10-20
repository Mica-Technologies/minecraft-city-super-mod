package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class BatchRenameTool {

  private static final String BATCH_RENAME_TOOL_INPUT_FOLDER_PATH_RELATIVE =
      "dev-env-utils/batchRenameToolInput";
  private static final String BATCH_RENAME_TOOL_OUTPUT_FOLDER_PATH_RELATIVE =
      "dev-env-utils/batchRenameToolOutput";

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Batch Rename Tool", args, (devEnvironmentPath) -> {
      // File name replacements
      /*
       * NOTE: Multiple `to`s are supported to allow for multiple replacement variations of the
       * same original file(s).
       *
       * EXAMPLE: Replacing "A" with "B" in file names
       * > FileNameReplacement.from( "A" ).to( "B" );
       *
       * EXAMPLE: Replacing "A" with "B" and "C" in file names, and "X" with "Y" in contents of
       * files renamed with "C"
       * > FileNameReplacement.from( "A" ).to( "B" ).to( "C", FileContentReplacement.from( "X" )
       * .to( "Y" ) );
       *
       * EXAMPLE: Replacing "A" with "B" and "C" in both file names and corresponding contents
       * > FileNameReplacement.from( "A" ).to( "B", true ).to( "C", true );
       */
      final FileNameReplacement[] fileNameReplacements =
          new FileNameReplacement[]{FileNameReplacement.from(
                  "blackmetal")
              .to("coppermetal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("coppermetal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_copper"))
              .to("lightbluemetal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("lightbluemetal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_lightblue"))
              .to("limemetal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("limemetal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_lime"))
              .to("magentametal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("magentametal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_magenta"))
              .to("orangemetal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("orangemetal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_orange"))
              .to("pinkmetal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("pinkmetal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_pink"))
              .to("purplemetal",
                  FileContentReplacement.from(
                          "blackmetal")
                      .to("purplemetal"),
                  FileContentReplacement.from(
                          "metal_black")
                      .to("metal_purple"))
              .to("yellowmetal",
              FileContentReplacement.from(
                      "blackmetal")
                  .to("yellowmetal"),
              FileContentReplacement.from(
                      "metal_black")
                  .to("metal_yellow"))
          };

      // Process folder
      final String processPath = new File(devEnvironmentPath,
          BATCH_RENAME_TOOL_INPUT_FOLDER_PATH_RELATIVE).getAbsolutePath();
      final String outputPath = new File(devEnvironmentPath,
          BATCH_RENAME_TOOL_OUTPUT_FOLDER_PATH_RELATIVE).getAbsolutePath();
      processFolder(processPath, outputPath, fileNameReplacements);
    });
  }

  private static void processFolder(String processPath,
      String outputPath,
      FileNameReplacement[] fileNameReplacements,
      int depth) {
    // Create log buffer string based on depth (for indenting)
    StringBuilder logBuffer = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      logBuffer.append("  ");
    }
    String logIndent = logBuffer.toString();

    System.out.println(logIndent + "Processing folder: " + processPath);

    try {
      // Create output folder if it doesn't exist
      File outputFolder = new File(outputPath);
      if (!outputFolder.exists()) {
        outputFolder.mkdirs();
      } else {
        // Delete all files in output folder
        for (File file : outputFolder.listFiles()) {
          if (file.isFile() && !file.getName().equalsIgnoreCase(".gitkeep")) {
            file.delete();
          }
        }
      }

      // Recursively process all files and folders in the input folder
      File processFolder = new File(processPath);
      for (File file : processFolder.listFiles()) {
        if (file.isDirectory()) {
          System.out.println(logIndent + "  Entering directory: " + file.getName());
          processFolder(file.getAbsolutePath(), outputPath, fileNameReplacements);
        } else {
          System.out.println(logIndent + "  Processing file: " + file.getName());

          // Process file name replacements
          String fileName = file.getName();
          for (FileNameReplacement fileNameReplacement : fileNameReplacements) {
            if (fileName.contains(fileNameReplacement.getOriginalText())) {
              if (fileNameReplacement.getReplacementTexts().isEmpty()) {
                throw new IllegalArgumentException(
                    "No new/replacement file name text(s) found for " +
                        "original file name text: " +
                        fileNameReplacement.getOriginalText());
              } else {
                for (String replacementText : fileNameReplacement.getReplacementTexts()) {
                  // Create new file + name
                  File newFile = new File(outputFolder,
                      fileName.replaceAll(fileNameReplacement.getOriginalText(),
                          replacementText));
                  String newFileName = newFile.getName();

                  // Process file content replacements (if any)
                  if (fileNameReplacement.getFileContentReplacements().isEmpty() ||
                      !fileNameReplacement.getFileContentReplacements()
                          .containsKey(replacementText)) {
                    // No file content replacements, just copy the file
                    FileUtils.copyFile(file, newFile);
                    System.out.println(logIndent + "    ->" + newFileName + " (copied)");
                  } else {
                    // Get file contents and replace original text with replacement text
                    String fileContent = FileUtils.readFileToString(file);
                    for (FileContentReplacement fileContentReplacement :
                        fileNameReplacement.getFileContentReplacements()
                            .get(replacementText)) {
                      fileContent = fileContent.replaceAll(
                          fileContentReplacement.getOriginalText(),
                          fileContentReplacement.getReplacementText());
                    }

                    // Write file contents to new file
                    FileUtils.writeStringToFile(newFile, fileContent);
                    System.out.println(logIndent + "    ->" + newFileName + " (copied+modified)");
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to process folder: " + processPath);
      e.printStackTrace();
    }

    System.out.println("Finished processing folder: " + processPath);
  }

  private static void processFolder(String processPath,
      String outputPath,
      FileNameReplacement[] fileNameReplacements) {
    processFolder(processPath, outputPath, fileNameReplacements, 0);
  }

  private static class FileNameReplacement {

    protected final String originalText;
    protected final ArrayList<String> replacementTexts = new ArrayList<>();
    protected final Map<String, ArrayList<FileContentReplacement>> fileContentReplacements =
        new HashMap<>();

    private FileNameReplacement(String originalText) {
      this.originalText = originalText;
    }

    public static FileNameReplacement from(String originalText) {
      return new FileNameReplacement(originalText);
    }

    public FileNameReplacement to(String replacementText, boolean replaceInFileContents) {
      if (replaceInFileContents) {
        return to(replacementText, FileContentReplacement.from(originalText).to(replacementText));
      } else {
        return to(replacementText);
      }
    }

    public FileNameReplacement to(String replacementText,
        FileContentReplacement... fileContentReplacements) {
      checkReplacementTextExists(replacementText);
      this.replacementTexts.add(replacementText);
      for (FileContentReplacement fileContentReplacement : fileContentReplacements) {
        if (!this.fileContentReplacements.containsKey(replacementText)) {
          this.fileContentReplacements.put(replacementText, new ArrayList<>());
        }
        this.fileContentReplacements.get(replacementText).add(fileContentReplacement);
      }
      return this;
    }

    public FileNameReplacement to(String replacementText) {
      checkReplacementTextExists(replacementText);
      this.replacementTexts.add(replacementText);
      return this;
    }

    private void checkReplacementTextExists(String replacementText) {
      if (this.replacementTexts.contains(replacementText)) {
        throw new IllegalArgumentException(
            "Duplicate replacement text: " + replacementText + " for original text: "
                + originalText);
      }
    }

    public String getOriginalText() {
      return originalText;
    }

    public ArrayList<String> getReplacementTexts() {
      return replacementTexts;
    }

    public Map<String, ArrayList<FileContentReplacement>> getFileContentReplacements() {
      return fileContentReplacements;
    }
  }

  private static class FileContentReplacement {

    private final String originalText;
    private String replacementText;

    private FileContentReplacement(String originalText) {
      this.originalText = originalText;
      this.replacementText = originalText;
    }

    public static FileContentReplacement from(String originalText) {
      return new FileContentReplacement(originalText);
    }

    public FileContentReplacement to(String replacementText) {
      this.replacementText = replacementText;
      return this;
    }

    public String getOriginalText() {
      return originalText;
    }

    public String getReplacementText() {
      return replacementText;
    }
  }
}
