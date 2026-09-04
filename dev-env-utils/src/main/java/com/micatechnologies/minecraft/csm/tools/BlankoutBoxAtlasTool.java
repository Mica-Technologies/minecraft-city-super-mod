package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.AssetFolder;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates the blankout box display face texture atlas. Composites individual blankout box
 * textures into a single atlas PNG for efficient rendering.
 *
 * <p>To add new textures: append the filename (without extension) to {@link #INPUT_IMAGE_NAMES}
 * and update the corresponding atlas index constants in
 * {@code BlankoutBoxTextureMap.java} in the main mod source.
 *
 * <p>Atlas layout: 8x2 grid of 256x256 tiles = 2048x512 atlas. Tiles are numbered left-to-right,
 * top-to-bottom starting at index 0: row 0 is the lit faces in {@code BlankoutBoxType} order, row 1
 * the unlit ones, so the off tile of a type is always its on tile plus 8. An empty string leaves a
 * slot blank (spare capacity for future legends).
 *
 * <p>Run via IntelliJ run configuration "Generate Blankout Box Atlas" or:
 * {@code mvn exec:java -Dexec.mainClass="...BlankoutBoxAtlasTool" -Dexec.args="<project-root>"}
 */
public class BlankoutBoxAtlasTool {

  // Relative to a tree's assets/csm. The tiles live in the module that ships them and the
  // atlas belongs beside them: writing it to Core would put the same resource path in two
  // jars, and which one the game loads would be down to classpath order.
  private static final String INPUT_FOLDER =
      "textures/blocks/trafficsignals/blankout_boxes";

  private static final String OUTPUT_FILE_NAME = "blankout_box_atlas.png";

  private static final String INPUT_EXTENSION = ".png";
  private static final int TILE_SIZE = 256;
  private static final int COLS = 8;
  private static final int ROWS = 2;
  private static final int OUTPUT_WIDTH = COLS * TILE_SIZE;   // 2048
  private static final int OUTPUT_HEIGHT = ROWS * TILE_SIZE;  // 512

  /**
   * Ordered list of texture filenames (without extension). The array index corresponds to the
   * atlas tile index used in {@code BlankoutBoxTextureMap}. DO NOT reorder existing entries —
   * only append new ones at the end to preserve index stability.
   *
   * <pre>
   * Index  Texture        Row  Col  Type           State
   * -----  ----------     ---  ---  -------------- -----
   *   0    DW_BO          0    0    Don't Walk     ON
   *   1    NLT_BO         0    1    No Left Turn   ON
   *   2    NRT_BO         0    2    No Right Turn  ON
   *   3    DNE_BO         0    3    Do Not Enter   ON
   *   4    NUT_BO         0    4    No U-Turn      ON
   *   5    TRAIN_BO       0    5    Train          ON
   *   8    DW_BO_OFF      1    0    Don't Walk     OFF
   *   9    NLT_BO_OFF     1    1    No Left Turn   OFF
   *  10    NRT_BO_OFF     1    2    No Right Turn  OFF
   *  11    DNE_BO_OFF     1    3    Do Not Enter   OFF
   *  12    NUT_BO_OFF     1    4    No U-Turn      OFF
   *  13    TRAIN_BO_OFF   1    5    Train          OFF
   * </pre>
   */
  private static final String[] INPUT_IMAGE_NAMES = {
      // Row 0: ON textures (indices 0-7)
      "dw_bo",         // 0: Don't Walk ON
      "nlt_bo",        // 1: No Left Turn ON
      "nrt_bo",        // 2: No Right Turn ON
      "dne_bo",        // 3: Do Not Enter ON
      "nut_bo",        // 4: No U-Turn ON
      "train_bo",      // 5: Train ON
      "",              // 6: spare
      "",              // 7: spare

      // Row 1: OFF textures (indices 8-15)
      "dw_bo_off",     // 8: Don't Walk OFF
      "nlt_bo_off",    // 9: No Left Turn OFF
      "nrt_bo_off",    // 10: No Right Turn OFF
      "dne_bo_off",    // 11: Do Not Enter OFF
      "nut_bo_off",    // 12: No U-Turn OFF
      "train_bo_off",  // 13: Train OFF
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Blankout Box Atlas Generator", args,
        (devEnvironmentPath) -> {
          CsmLayout layout = new CsmLayout(devEnvironmentPath);
          // Read from every tree that has the folder, so a tile shipped by another module
          // is still found; the atlas is written back beside the folder that owns it.
          AssetFolder inputFolder = AssetFolder.ofAsset(layout, INPUT_FOLDER);
          File outputFile = layout.assetInFolderForWrite(INPUT_FOLDER, OUTPUT_FILE_NAME);

          int totalSlots = COLS * ROWS;

          if (INPUT_IMAGE_NAMES.length > totalSlots) {
            System.err.println("Error: " + INPUT_IMAGE_NAMES.length
                + " images exceed atlas capacity of " + totalSlots + " slots ("
                + COLS + "x" + ROWS + " at " + TILE_SIZE + "px).");
            return;
          }

          BufferedImage[] loadedImages = new BufferedImage[INPUT_IMAGE_NAMES.length];
          int tileCount = 0;
          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            if (INPUT_IMAGE_NAMES[i].isEmpty()) {
              loadedImages[i] = null;  // spare slot: left blank
              continue;
            }
            tileCount++;
            File imgFile = inputFolder.file(INPUT_IMAGE_NAMES[i] + INPUT_EXTENSION);
            try {
              loadedImages[i] = ImageIO.read(imgFile);
              System.out.println("  [" + i + "] " + INPUT_IMAGE_NAMES[i] + " ("
                  + loadedImages[i].getWidth() + "x" + loadedImages[i].getHeight() + ")");
            } catch (Exception e) {
              System.err.println("  [" + i + "] Error loading: " + INPUT_IMAGE_NAMES[i]
                  + " - " + e.getMessage());
              loadedImages[i] = createTransparentTile();
            }
          }

          BufferedImage outputImage = new BufferedImage(
              OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2d = outputImage.createGraphics();

          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            if (loadedImages[i] == null) {
              continue;
            }
            int row = i / COLS;
            int col = i % COLS;
            g2d.drawImage(loadedImages[i],
                col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
          }

          g2d.dispose();

          ImageIO.write(outputImage, "png", outputFile);
          System.out.println("\nAtlas generated: " + outputFile.getAbsolutePath());
          System.out.println("  " + tileCount + " tiles in "
              + COLS + "x" + ROWS + " grid ("
              + OUTPUT_WIDTH + "x" + OUTPUT_HEIGHT + "px)");
          System.out.println("  " + (totalSlots - tileCount)
              + " empty slots remaining for future textures");
        });
  }

  private static BufferedImage createTransparentTile() {
    BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = tile.createGraphics();
    g.setColor(new Color(0, 0, 0, 0));
    g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
    g.dispose();
    return tile;
  }
}
