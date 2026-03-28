package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageTilerTool {

  private static final String INPUT_FOLDER =
      "src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/";
  private static final String OUTPUT_FILE =
      "src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/atlas.png";
  private static final String INPUT_EXTENSION = ".png";

  private static final int TILE_SIZE = 128;
  private static final int OUTPUT_SIZE = 1024;

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
      "wled_red_x"
  };

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Image Tiler (Atlas Generator)", args,
        (devEnvironmentPath) -> {
          File inputFolder = new File(devEnvironmentPath, INPUT_FOLDER);
          File outputFile = new File(devEnvironmentPath, OUTPUT_FILE);

          int tilesPerRow = OUTPUT_SIZE / TILE_SIZE;
          int totalSlots = tilesPerRow * tilesPerRow;

          if (INPUT_IMAGE_NAMES.length > totalSlots) {
            System.err.println("Error: " + INPUT_IMAGE_NAMES.length + " images exceed atlas capacity of "
                + totalSlots + " slots (" + tilesPerRow + "x" + tilesPerRow + " at " + TILE_SIZE + "px).");
            return;
          }

          BufferedImage[] loadedImages = new BufferedImage[INPUT_IMAGE_NAMES.length];
          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            File imgFile = new File(inputFolder, INPUT_IMAGE_NAMES[i] + INPUT_EXTENSION);
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

          BufferedImage outputImage = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2d = outputImage.createGraphics();

          for (int i = 0; i < INPUT_IMAGE_NAMES.length; i++) {
            int row = i / tilesPerRow;
            int col = i % tilesPerRow;
            g2d.drawImage(loadedImages[i], col * TILE_SIZE, row * TILE_SIZE, null);
          }

          g2d.dispose();

          ImageIO.write(outputImage, "png", outputFile);
          System.out.println("Atlas generated: " + outputFile.getAbsolutePath());
          System.out.println("  " + INPUT_IMAGE_NAMES.length + " tiles in " + tilesPerRow + "x" + tilesPerRow
              + " grid (" + OUTPUT_SIZE + "x" + OUTPUT_SIZE + "px)");
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
