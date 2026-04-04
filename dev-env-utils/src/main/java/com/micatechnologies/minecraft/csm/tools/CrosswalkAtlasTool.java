package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates the crosswalk signal display face texture atlas. Composites individual crosswalk
 * textures into a single atlas PNG for efficient rendering.
 *
 * <p>To add new textures: append the filename (without extension) to {@link #INPUT_IMAGE_NAMES}
 * and update the corresponding atlas index constants in
 * {@code CrosswalkTextureMap.java} in the main mod source.
 *
 * <p>Atlas layout: 4x4 grid of 128x128 tiles = 512x512 atlas. Tiles are numbered left-to-right,
 * top-to-bottom starting at index 0. Current capacity: 16 tiles (expandable by increasing
 * OUTPUT_SIZE or reducing TILE_SIZE).
 *
 * <p>Run via IntelliJ run configuration "Generate Crosswalk Atlas" or:
 * {@code mvn exec:java -Dexec.mainClass="...CrosswalkAtlasTool" -Dexec.args="<project-root>"}
 */
public class CrosswalkAtlasTool {

  /**
   * Folder containing individual crosswalk textures (relative to project root).
   */
  private static final String INPUT_FOLDER =
      "src/main/resources/assets/csm/textures/blocks/trafficsignals/crosswalk/";

  /**
   * Output atlas file path (relative to project root).
   */
  private static final String OUTPUT_FILE =
      "src/main/resources/assets/csm/textures/blocks/trafficsignals/crosswalk/crosswalk_atlas.png";

  private static final String INPUT_EXTENSION = ".png";
  private static final int TILE_SIZE = 128;
  private static final int OUTPUT_SIZE = 512; // 4x4 grid = 16 slots

  /**
   * Ordered list of texture filenames (without extension). The array index corresponds to the
   * atlas tile index used in {@code CrosswalkTextureMap}. DO NOT reorder existing entries —
   * only append new ones at the end to preserve index stability.
   *
   * <pre>
   * Index  Texture                          Signal    Section   State
   * -----  -------------------------------  --------  --------  ----------------
   *   0    crosswalk_hand_lit               16-inch   face      don't walk (lit)
   *   1    crosswalk_man_lit                16-inch   face      walk (lit)
   *   2    crosswalk_off                    16-inch   face      off (ghosted)
   *   3    crosswalk_hand_lit_12in          12-inch   upper     don't walk (lit)
   *   4    crosswalk_man_lit_12in           12-inch   upper     walk (lit)
   *   5    crosswalk_off_12in              12-inch   upper     off (ghosted)
   *   6    crosswalk_base_texture_12in     12-inch   lower     countdown base
   *   7    crosswalktextdontwalkon         12-inch   upper     DON'T WALK text lit
   *   8    crosswalktextdontwalkoff        12-inch   upper     DON'T WALK text off
   *   9    crosswalktextwalkon             12-inch   lower     WALK text lit
   *  10    crosswalktextwalkoff            12-inch   lower     WALK text off
   * </pre>
   */
  private static final String[] INPUT_IMAGE_NAMES = {
      // 16-inch single signal textures (indices 0-2)
      "crosswalk_hand_lit",           // 0: don't walk
      "crosswalk_man_lit",            // 1: walk
      "crosswalk_off",                // 2: off

      // 12-inch stacked signal — hand/man + countdown textures (indices 3-6)
      "crosswalk_hand_lit_12in",      // 3: don't walk (upper section)
      "crosswalk_man_lit_12in",       // 4: walk (upper section)
      "crosswalk_off_12in",           // 5: off (upper section)
      "crosswalk_base_texture_12in",  // 6: countdown module base (lower section)

      // 12-inch stacked signal — worded/text textures (indices 7-10)
      "crosswalktextdontwalkon",      // 7: DON'T WALK text lit
      "crosswalktextdontwalkoff",     // 8: DON'T WALK text off
      "crosswalktextwalkon",          // 9: WALK text lit
      "crosswalktextwalkoff",         // 10: WALK text off
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Crosswalk Atlas Generator", args,
        (devEnvironmentPath) -> {
          File inputFolder = new File(devEnvironmentPath, INPUT_FOLDER);
          File outputFile = new File(devEnvironmentPath, OUTPUT_FILE);

          int tilesPerRow = OUTPUT_SIZE / TILE_SIZE;
          int totalSlots = tilesPerRow * tilesPerRow;

          if (INPUT_IMAGE_NAMES.length > totalSlots) {
            System.err.println("Error: " + INPUT_IMAGE_NAMES.length
                + " images exceed atlas capacity of " + totalSlots + " slots ("
                + tilesPerRow + "x" + tilesPerRow + " at " + TILE_SIZE + "px).");
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
              OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2d = outputImage.createGraphics();

          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            int row = i / tilesPerRow;
            int col = i % tilesPerRow;
            g2d.drawImage(loadedImages[i],
                col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
          }

          g2d.dispose();

          ImageIO.write(outputImage, "png", outputFile);
          System.out.println("\nAtlas generated: " + outputFile.getAbsolutePath());
          System.out.println("  " + INPUT_IMAGE_NAMES.length + " tiles in "
              + tilesPerRow + "x" + tilesPerRow + " grid ("
              + OUTPUT_SIZE + "x" + OUTPUT_SIZE + "px)");
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
