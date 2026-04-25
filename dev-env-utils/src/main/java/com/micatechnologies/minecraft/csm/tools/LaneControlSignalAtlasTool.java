package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates the lane control signal display face texture atlas. Composites individual textures into
 * a single atlas PNG for efficient rendering.
 *
 * <p>Atlas layout: 4x2 grid of tiles. Tiles are numbered left-to-right, top-to-bottom starting at
 * index 0, matching the ordinal order of {@code LaneControlSignalType}.
 *
 * <p>Run via IntelliJ run configuration or:
 * {@code mvn exec:java -Dexec.mainClass="...LaneControlSignalAtlasTool" -Dexec.args="<project-root>"}
 */
public class LaneControlSignalAtlasTool {

    private static final String INPUT_FOLDER =
        "src/main/resources/assets/csm/textures/blocks/trafficaccessories/lane_control_signal/";

    private static final String OUTPUT_FILE =
        "src/main/resources/assets/csm/textures/blocks/trafficaccessories/lane_control_signal/lane_control_signal_atlas.png";

    private static final String INPUT_EXTENSION = ".png";
    private static final int TILE_SIZE = 256;
    private static final int COLS = 4;
    private static final int ROWS = 2;
    private static final int OUTPUT_WIDTH = COLS * TILE_SIZE;
    private static final int OUTPUT_HEIGHT = ROWS * TILE_SIZE;

    /**
     * Ordered list of texture filenames (without extension). The array index corresponds to the
     * {@code LaneControlSignalType} ordinal.
     */
    private static final String[] INPUT_IMAGE_NAMES = {
        "green_arrow",          // 0: Green Arrow
        "off",                  // 1: Off
        "red_x",                // 2: Red X
        "shared_turn",          // 3: Shared Turn
        "turn",                 // 4: Turn
        "yellow_left_arrow",    // 5: Yellow Left Arrow
        "yellow_right_arrow",   // 6: Yellow Right Arrow
        "yellow_x",             // 7: Yellow X
    };

    public static void main(String[] args) {
        CsmToolUtility.doToolExecuteWrapped("CSM Lane Control Signal Atlas Generator", args,
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
                    File imgFile = new File(inputFolder,
                        INPUT_IMAGE_NAMES[i] + INPUT_EXTENSION);
                    try {
                        loadedImages[i] = ImageIO.read(imgFile);
                        System.out.println("  [" + i + "] " + INPUT_IMAGE_NAMES[i] + " ("
                            + loadedImages[i].getWidth() + "x"
                            + loadedImages[i].getHeight() + ")");
                    } catch (Exception e) {
                        System.err.println("  [" + i + "] Error loading: "
                            + INPUT_IMAGE_NAMES[i] + " - " + e.getMessage());
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
            });
    }

    private static BufferedImage createTransparentTile() {
        BufferedImage tile = new BufferedImage(
            TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tile.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.dispose();
        return tile;
    }
}
