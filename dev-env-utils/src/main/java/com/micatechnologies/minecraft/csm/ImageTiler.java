package com.micatechnologies.minecraft.csm;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageTiler {

  public static void main(String[] args) {
    // Define input image folder
    String inputImageFolder = "D:\\gitRepos\\minecraft-city-super-mod\\src\\main\\resources\\assets\\csm\\textures\\blocks\\trafficsignals\\lights\\";
    // Define list of image filenames
    String[] inputImagePath = {
        "biled_green_off",
        "biled_green",
        "biled_yellow_off",
        "biled_yellow",
        "biled_red_off",
        "biled_red",
        "eled_off",
        "eled_green_on",
        "eled_yellow_on",
        "eled_red_on",
        "led_leftarrowoff",
        "led_greenarrowleft",
        "led_yellowarrowleft",
        "led_redarrowleft",
        "greenarrowleft",
        "yellowarrowleft",
        "redarrowleft",
        "greenarrowleftoff",
        "yellowarrowleftoff",
        "redarrowleftoff",
        "greenuturn",
        "yellowuturn",
        "reduturn",
        "greenuturnoff",
        "yellowuturnoff",
        "reduturnoff",
        "wled_green",
        "wled_green_off",
        "wled_yellow",
        "wled_yellow_off",
        "wled_red_line",
        "wled_red_line_off",
        "iled_green",
        "iled_yellow",
        "iled_red",
        "iled_red_off",
        "iled_green_off",
        "iled_yellow_off",
        "inca_green_on",
        "inca_yellow_on",
        "inca_red_on",
        "inca_green_off",
        "inca_yellow_off",
        "inca_red_off",
        "gtx_green_on",
        "gtx_yellow_on",
        "gtx_red_on",
        "gtx_off"
    };
    String inputImageExtension = ".png";
    // Define output image
    String outputImagePath = "D:\\gitRepos\\minecraft-city-super-mod\\src\\main\\resources\\assets\\csm\\textures\\blocks\\trafficsignals\\lights\\output.png";

    int tileSize = 128;
    int outputSize = 1024;
    int tilesPerRow = outputSize / tileSize;
    int totalSlots = tilesPerRow * tilesPerRow;

    try {
      // Load all input images
      BufferedImage[] loadedImages = new BufferedImage[inputImagePath.length];
      for (int i = 0; i < inputImagePath.length; i++) {
        try {
          File imgFile = new File(inputImageFolder, inputImagePath[i] + inputImageExtension);
          loadedImages[i] = ImageIO.read(imgFile);

          // Optional sanity check for image size
          if (loadedImages[i].getWidth() != tileSize || loadedImages[i].getHeight() != tileSize) {
            System.out.println("Warning: Image " + inputImagePath[i] + " is not 128x128.");
          }
        }catch (IOException e) {
          System.err.println("Error loading image: " + inputImagePath[i] + " - " + e.getMessage());
          loadedImages[i] = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
          // Optionally fill with a placeholder color (e.g., transparent)
          Graphics2D g = loadedImages[i].createGraphics();
          g.setColor(new Color(0, 0, 0, 0)); // Transparent
          g.fillRect(0, 0, tileSize, tileSize);
          g.dispose();
        }
      }

      // Prepare output image
      BufferedImage outputImage = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = outputImage.createGraphics();

      // Draw images in grid
      for (int slot = 0; slot < totalSlots; slot++) {
        int row = slot / tilesPerRow;
        int col = slot % tilesPerRow;
        int x = col * tileSize;
        int y = row * tileSize;

        // Select image, repeat last if we run out
        int imgIndex = Math.min(slot, loadedImages.length - 1);
        g2d.drawImage(loadedImages[imgIndex], x, y, null);
      }

      g2d.dispose();

      // Write output
      ImageIO.write(outputImage, "png", new File(outputImagePath));
      System.out.println("Atlas generation complete. Output saved as " + outputImagePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}


