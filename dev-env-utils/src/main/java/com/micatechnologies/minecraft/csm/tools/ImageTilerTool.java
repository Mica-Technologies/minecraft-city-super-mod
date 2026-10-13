package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.AssetFolder;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageTilerTool {

  // Relative to a tree's assets/csm. The tiles live in the module that ships them and the
  // atlas belongs beside them: writing it to Core would put the same resource path in two
  // jars, and which one the game loads would be down to classpath order.
  private static final String INPUT_FOLDER =
      "textures/blocks/trafficsignals/lights";
  private static final String OUTPUT_FILE_NAME = "atlas.png";
  private static final String INPUT_EXTENSION = ".png";

  private static final int TILE_SIZE = 128;
  private static final int OUTPUT_WIDTH = 1024;
  // Eight columns by sixteen rows. The first eight rows are the original 8x8 atlas, unchanged,
  // so every existing cell index keeps its place; the European lenses start at cell 64.
  private static final int OUTPUT_HEIGHT = 2048;

  /** A name that leaves its cell transparent: spare cells, and 63, the texture map's fallback. */
  private static final String EMPTY = "";

  private static final String[] INPUT_IMAGE_NAMES = {
      "biled_green_off",
      "biled_yellow_off",
      "biled_red_off",
      "biled_green",
      "biled_yellow",
      "biled_red",
      "led_leftarrowoff",
      "led_greenarrowleft",
      "led_yellowarrowleft",
      "led_redarrowleft",
      "greenarrowleftoff",
      "yellowarrowleftoff",
      "redarrowleftoff",
      "greenarrowleft",
      "yellowarrowleft",
      "redarrowleft",
      "greenuturnoff",
      "yellowuturnoff",
      "reduturnoff",
      "greenuturn",
      "yellowuturn",
      "reduturn",
      "wled_green_off",
      "wled_yellow_off",
      "wled_red_line_off",
      "wled_green",
      "wled_yellow",
      "wled_red_line",
      "iled_green_off",
      "iled_yellow_off",
      "iled_red_off",
      "iled_green",
      "iled_yellow",
      "iled_red",
      "inca_green_off",
      "inca_yellow_off",
      "inca_red_off",
      "inca_green_on",
      "inca_yellow_on",
      "inca_red_on",
      "inca_arrow_green_off",
      "inca_arrow_yellow_off",
      "inca_arrow_red_off",
      "inca_arrow_green_on",
      "inca_arrow_yellow_on",
      "inca_arrow_red_on",
      "eled_off",
      "eled_green_on",
      "eled_yellow_on",
      "eled_red_on",
      "gtx_off",
      "gtx_green_on",
      "gtx_yellow_on",
      "gtx_red_on",
      "wled_red_x",
      "dr6_off",
      "dr6_green_on",
      "dr6_yellow_on",
      "dr6_red_on",
      EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
      // 64-75: European lenses (dev-env-utils/scripts/gen_euro_lenses.py)
      "euro_green_off",
      "euro_yellow_off",
      "euro_red_off",
      "euro_green_on",
      "euro_yellow_on",
      "euro_red_on",
      "euro_arrow_green_off",
      "euro_arrow_yellow_off",
      "euro_arrow_red_off",
      "euro_arrow_green_on",
      "euro_arrow_yellow_on",
      "euro_arrow_red_on",
      // 76-79: European pedestrian figures, masked (gen_euro_lenses.py)
      "euro_ped_standing_off",
      "euro_ped_standing_on",
      "euro_ped_walking_off",
      "euro_ped_walking_on"
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Image Tiler (Atlas Generator)", args,
        (devEnvironmentPath) -> {
          CsmLayout layout = new CsmLayout(devEnvironmentPath);
          // Read from every tree that has the folder, so a tile shipped by another module
          // is still found; the atlas is written back beside the folder that owns it.
          AssetFolder inputFolder = AssetFolder.ofAsset(layout, INPUT_FOLDER);
          File outputFile = layout.assetInFolderForWrite(INPUT_FOLDER, OUTPUT_FILE_NAME);

          int tilesPerRow = OUTPUT_WIDTH / TILE_SIZE;
          int rows = OUTPUT_HEIGHT / TILE_SIZE;
          int totalSlots = tilesPerRow * rows;

          if (INPUT_IMAGE_NAMES.length > totalSlots) {
            System.err.println("Error: " + INPUT_IMAGE_NAMES.length + " images exceed atlas capacity of "
                + totalSlots + " slots (" + tilesPerRow + "x" + rows + " at " + TILE_SIZE + "px).");
            return;
          }

          BufferedImage[] loadedImages = new BufferedImage[INPUT_IMAGE_NAMES.length];
          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            if (INPUT_IMAGE_NAMES[i].isEmpty()) {
              loadedImages[i] = createTransparentTile();
              continue;
            }
            File imgFile = inputFolder.file(INPUT_IMAGE_NAMES[i] + INPUT_EXTENSION);
            try {
              loadedImages[i] = ImageIO.read(imgFile);
              if (loadedImages[i].getWidth() != TILE_SIZE || loadedImages[i].getHeight() != TILE_SIZE) {
                System.out.println("Warning: " + INPUT_IMAGE_NAMES[i] + " is "
                    + loadedImages[i].getWidth() + "x" + loadedImages[i].getHeight()
                    + ", expected " + TILE_SIZE + "x" + TILE_SIZE + ".");
              }
            } catch (Exception e) {
              System.err.println("Error loading image: " + INPUT_IMAGE_NAMES[i] + " - " + e.getMessage());
              loadedImages[i] = createTransparentTile();
            }
          }

          BufferedImage outputImage = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT,
              BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2d = outputImage.createGraphics();

          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            int row = i / tilesPerRow;
            int col = i % tilesPerRow;
            g2d.drawImage(loadedImages[i], col * TILE_SIZE, row * TILE_SIZE, null);
          }

          g2d.dispose();

          ImageIO.write(outputImage, "png", outputFile);
          System.out.println("Atlas generated: " + outputFile.getAbsolutePath());
          System.out.println("  " + INPUT_IMAGE_NAMES.length + " cells in " + tilesPerRow + "x" + rows
              + " grid (" + OUTPUT_WIDTH + "x" + OUTPUT_HEIGHT + "px)");
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
