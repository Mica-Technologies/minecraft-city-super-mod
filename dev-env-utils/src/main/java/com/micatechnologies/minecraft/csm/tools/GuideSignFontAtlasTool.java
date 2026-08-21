package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/**
 * Generates the FHWA-style glyph atlas the dynamic guide sign's TESR uses for sign
 * legend text, plus a metrics JSON the mod parses at runtime.
 *
 * <p>Source font: {@code assets/csm/fonts/highway_gothic_wide.ttf} (already shipped for
 * the static road-sign texture pipeline). Glyphs are rendered white on transparent at a
 * normalized cap height so the TESR can scale text by "cap height in sign pixels".
 *
 * <p>Outputs:
 * <ul>
 *   <li>{@code assets/csm/textures/fonts/guide_sign_font.png} — 1024x512 atlas,
 *       fixed 64x72 cells, 16 columns, baseline at y=52 within each cell</li>
 *   <li>{@code assets/csm/fonts/guide_sign_font.json} — texture/cell geometry,
 *       cap height, per-glyph cell coordinates and advances (in atlas pixels)</li>
 * </ul>
 *
 * <p>Run via IntelliJ run configuration or:
 * {@code mvn exec:java -Dexec.mainClass="...GuideSignFontAtlasTool" -Dexec.args="<project-root>"}
 */
public class GuideSignFontAtlasTool {

  private static final String FONT_FILE = "src/main/resources/assets/csm/fonts/highway_gothic_wide.ttf";
  private static final String OUTPUT_PNG =
      "src/main/resources/assets/csm/textures/fonts/guide_sign_font.png";
  private static final String OUTPUT_JSON = "src/main/resources/assets/csm/fonts/guide_sign_font.json";

  private static final int ATLAS_WIDTH = 1024;
  private static final int ATLAS_HEIGHT = 512;
  private static final int CELL_WIDTH = 64;
  private static final int CELL_HEIGHT = 72;
  private static final int COLS = ATLAS_WIDTH / CELL_WIDTH;
  // Baseline y within a cell; leaves 52px of ascender room and 20px for descenders.
  private static final int BASELINE = 52;
  // Pen origin x within a cell, so glyphs with negative left side bearing don't clip.
  private static final int ORIGIN_X = 8;
  // Every glyph is rendered so the capital height is exactly this many pixels.
  private static final int CAP_HEIGHT = 40;

  private static final String GLYPHS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          + "abcdefghijklmnopqrstuvwxyz"
          + "0123456789"
          + " -.,'\"/&():;?!+#%";

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Guide Sign Font Atlas Generator", args,
        (devEnvironmentPath) -> {
          File fontFile = new File(devEnvironmentPath, FONT_FILE);
          Font base = Font.createFont(Font.TRUETYPE_FONT, fontFile);

          // Find the point size whose capital 'H' is exactly CAP_HEIGHT pixels tall.
          float size = 60f;
          for (int i = 0; i < 8; i++) {
            Font probe = base.deriveFont(size);
            double capH = capHeight(probe);
            if (Math.abs(capH - CAP_HEIGHT) < 0.05) {
              break;
            }
            size = (float) (size * CAP_HEIGHT / capH);
          }
          Font font = base.deriveFont(size);

          BufferedImage atlas =
              new BufferedImage(ATLAS_WIDTH, ATLAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g = atlas.createGraphics();
          g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
              RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
              RenderingHints.VALUE_FRACTIONALMETRICS_ON);
          g.setFont(font);
          g.setColor(Color.WHITE);

          StringBuilder json = new StringBuilder();
          json.append("{\n");
          json.append("  \"textureWidth\": ").append(ATLAS_WIDTH).append(",\n");
          json.append("  \"textureHeight\": ").append(ATLAS_HEIGHT).append(",\n");
          json.append("  \"cellWidth\": ").append(CELL_WIDTH).append(",\n");
          json.append("  \"cellHeight\": ").append(CELL_HEIGHT).append(",\n");
          json.append("  \"baseline\": ").append(BASELINE).append(",\n");
          json.append("  \"originX\": ").append(ORIGIN_X).append(",\n");
          json.append("  \"capHeight\": ").append(CAP_HEIGHT).append(",\n");
          json.append("  \"glyphs\": {\n");

          for (int i = 0; i < GLYPHS.length(); i++) {
            char c = GLYPHS.charAt(i);
            int col = i % COLS;
            int row = i / COLS;
            int cellX = col * CELL_WIDTH;
            int cellY = row * CELL_HEIGHT;

            String s = String.valueOf(c);
            GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), s);
            Rectangle2D ink = gv.getVisualBounds();
            if (ink.getWidth() > CELL_WIDTH - ORIGIN_X || ink.getHeight() > CELL_HEIGHT) {
              throw new IllegalStateException(
                  "Glyph '" + c + "' ink " + ink + " exceeds cell " + CELL_WIDTH + "x" + CELL_HEIGHT);
            }
            g.drawString(s, cellX + ORIGIN_X, cellY + BASELINE);

            // Space has empty ink but a real advance; getStringBounds covers both cases.
            double advance = font.getStringBounds(s, g.getFontRenderContext()).getWidth();

            json.append("    \"").append(escape(c)).append("\": {\"col\": ").append(col)
                .append(", \"row\": ").append(row)
                .append(", \"advance\": ").append(String.format("%.2f", advance)).append("}");
            json.append(i < GLYPHS.length() - 1 ? ",\n" : "\n");
          }
          json.append("  }\n}\n");

          g.dispose();

          File pngOut = new File(devEnvironmentPath, OUTPUT_PNG);
          pngOut.getParentFile().mkdirs();
          ImageIO.write(atlas, "PNG", pngOut);
          System.out.println("Wrote font atlas: " + pngOut.getAbsolutePath());

          File jsonOut = new File(devEnvironmentPath, OUTPUT_JSON);
          try (PrintWriter pw = new PrintWriter(jsonOut, StandardCharsets.UTF_8)) {
            pw.print(json);
          }
          System.out.println("Wrote font metrics: " + jsonOut.getAbsolutePath()
              + " (" + GLYPHS.length() + " glyphs, font size " + String.format("%.1f", size) + ")");
        });
  }

  private static double capHeight(Font font) {
    BufferedImage tmp = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
    Graphics2D tg = tmp.createGraphics();
    GlyphVector gv = font.createGlyphVector(tg.getFontRenderContext(), "H");
    double h = gv.getVisualBounds().getHeight();
    tg.dispose();
    return h;
  }

  private static String escape(char c) {
    if (c == '"' || c == '\\') {
      return "\\" + c;
    }
    return String.valueOf(c);
  }
}
