package com.micatechnologies.minecraft.csm.tools;

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
 * <p>Atlas layout: 4x2 grid of 256x256 tiles = 1024x512 atlas. Tiles are numbered left-to-right,
 * top-to-bottom starting at index 0.
 *
 * <p>Run via IntelliJ run configuration "Generate Blankout Box Atlas" or:
 * {@code mvn exec:java -Dexec.mainClass="...BlankoutBoxAtlasTool" -Dexec.args="<project-root>"}
 */
public class BlankoutBoxAtlasTool {

  private static final String INPUT_FOLDER =
      "src/main/resources/assets/csm/textures/blocks/trafficsignals/blankout_boxes/";

  private static final String OUTPUT_FILE =
      "src/main/resources/assets/csm/textures/blocks/trafficsignals/blankout_boxes/blankout_box_atlas.png";

  private static final String INPUT_EXTENSION = ".png";
  private static final int TILE_SIZE = 256;
  private static final int COLS = 4;
  private static final int ROWS = 2;
  private static final int OUTPUT_WIDTH = COLS * TILE_SIZE;   // 1024
  private static final int OUTPUT_HEIGHT = ROWS * TILE_SIZE;  // 512

  /**
   * Ordered list of texture filenames (without extension). The array index corresponds to the
   * atlas tile index used in {@code BlankoutBoxTextureMap}. DO NOT reorder existing entries —
   * only append new ones at the end to preserve index stability.
   *
   * <pre>
   * Index  Texture       Row  Col  Type           State
   * -----  ----------    ---  ---  -------------- -----
   *   0    DW_BO         0    0    Don't Walk     ON
   *   1    NLT_BO        0    1    No Left Turn   ON
   *   2    NRT_BO        0    2    No Right Turn  ON
   *   3    DNE_BO        0    3    Do Not Enter   ON
   *   4    DW_BO_OFF     1    0    Don't Walk     OFF
   *   5    NLT_BO_OFF    1    1    No Left Turn   OFF
   *   6    NRT_BO_OFF    1    2    No Right Turn  OFF
   *   7    DNE_BO_OFF    1    3    Do Not Enter   OFF
   * </pre>
   */
  private static final String[] INPUT_IMAGE_NAMES = {
      // Row 0: ON textures (indices 0-3)
      "dw_bo",      // 0: Don't Walk ON
      "nlt_bo",     // 1: No Left Turn ON
      "nrt_bo",     // 2: No Right Turn ON
      "dne_bo",     // 3: Do Not Enter ON

      // Row 1: OFF textures (indices 4-7)
      "dw_bo_off",  // 4: Don't Walk OFF
      "nlt_bo_off", // 5: No Left Turn OFF
      "nrt_bo_off",  // 6: No Right Turn OFF
      "dne_bo_off",  // 7: Do Not Enter OFF
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Blankout Box Atlas Generator", args,
        (devEnvironmentPath) -> {
          File inputFolder = new File(devEnvironmentPath, INPUT_FOLDER);
          File outputFile = new File(devEnvironmentPath, OUTPUT_FILE);

          int totalSlots = COLS * ROWS;

          if (INPUT_IMAGE_NAMES.length > totalSlots) {
            System.err.println("Error: " + INPUT_IMAGE_NAMES.length
                + " images exceed atlas capacity of " + totalSlots + " slots ("
                + COLS + "x" + ROWS + " at " + TILE_SIZE + "px).");
            return;
          }

          BufferedImage[] loadedImages = new BufferedImage[INPUT_IMAGE_NAMES.length];
          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            File imgFile = new File(inputFolder, INPUT_IMAGE_NAMES[i] + INPUT_EXTENSION);
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
            int row = i / COLS;
            int col = i % COLS;
            g2d.drawImage(loadedImages[i],
                col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
          }

          g2d.dispose();

          ImageIO.write(outputImage, "png", outputFile);
          System.out.println("\nAtlas generated: " + outputFile.getAbsolutePath());
          System.out.println("  " + INPUT_IMAGE_NAMES.length + " tiles in "
              + COLS + "x" + ROWS + " grid ("
              + OUTPUT_WIDTH + "x" + OUTPUT_HEIGHT + "px)");
          System.out.println("  " + (totalSlots - INPUT_IMAGE_NAMES.length)
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
