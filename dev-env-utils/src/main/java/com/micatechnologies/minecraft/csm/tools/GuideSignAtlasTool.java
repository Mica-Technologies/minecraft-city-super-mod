package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

/**
 * Generates the combined guide sign atlas texture. Renders MUTCD highway shields
 * from Wikimedia Commons SVGs via Apache Batik, and draws directional arrows
 * programmatically.
 *
 * <p>Atlas layout (512x512, 64x64 cells, matches {@code GuideSignAtlas.java}):
 * <ul>
 *   <li>Row 0, cols 0-7: Generic shield backgrounds (no text baked in)</li>
 *   <li>Row 1, cols 0-7: State markers (CA, TX, FL, NY, CT, MA, ME, NH)</li>
 *   <li>Row 2, cols 0-1: State markers (RI, VT)</li>
 *   <li>Rows 4-5, cols 0-4: Directional arrows (white on transparent)</li>
 * </ul>
 *
 * <p>Shield SVG sources (public domain MUTCD designs from Wikimedia Commons):
 * <ul>
 *   <li>Interstate: I-blank.svg</li>
 *   <li>Interstate Business: Business_Loop_blank.svg</li>
 *   <li>US Route: US_blank.svg</li>
 *   <li>State Circle: Circle_sign_blank.svg</li>
 *   <li>County Route: County_Blank.svg</li>
 * </ul>
 *
 * <p>Run via IntelliJ run configuration or:
 * {@code mvn exec:java -Dexec.mainClass="...GuideSignAtlasTool" -Dexec.args="<project-root>"}
 */
public class GuideSignAtlasTool {

  private static final String OUTPUT_FILE =
      "src/main/resources/assets/csm/textures/blocks/trafficaccessories/guidesign/sign_atlas.png";

  private static final String SHIELD_RESOURCE_DIR = "/guidesign/shields/";

  private static final int ATLAS_SIZE = 512;
  private static final int CELL_SIZE = 64;

  private static final int ARROW_ROW_OFFSET = 4;

  private static final int SHIELD_PADDING = 2;

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Guide Sign Atlas Generator", args,
        (devEnvironmentPath) -> {
          File outputFile = new File(devEnvironmentPath, OUTPUT_FILE);
          outputFile.getParentFile().mkdirs();

          BufferedImage atlas = new BufferedImage(
              ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g = atlas.createGraphics();
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON);
          g.setRenderingHint(RenderingHints.KEY_RENDERING,
              RenderingHints.VALUE_RENDER_QUALITY);

          drawShields(g);
          drawArrows(g);

          g.dispose();
          ImageIO.write(atlas, "PNG", outputFile);
          System.out.println("Wrote atlas: " + outputFile.getAbsolutePath());
        });
  }

  private static void drawShields(Graphics2D g) {
    drawSvgShield(g, 0, 0, "interstate.svg");
    drawSvgShield(g, 1, 0, "interstate_business.svg");
    drawSvgShield(g, 2, 0, "us_route.svg");
    drawStateSquare(g, 3, 0);
    drawSvgShield(g, 4, 0, "state_circle.svg");
    drawSvgShield(g, 5, 0, "county_route.svg");
    drawToll(g, 6, 0);
    drawBlankCustom(g, 7, 0);

    // State-specific markers — programmatic approximations (no SVG sources).
    // Each uses a distinct silhouette + state-themed color so it's recognizable
    // alongside the route number rendered in white by the TESR.
    drawStateShield(g, 0, 1, makeCaliforniaShape(0, 1), new Color(36, 110, 60));
    drawStateShield(g, 1, 1, makeTexasShape(1, 1), new Color(170, 40, 40));
    drawStateShield(g, 2, 1, makeFloridaShape(2, 1), new Color(190, 95, 35));
    drawStateShield(g, 3, 1, makeNewYorkShape(3, 1), new Color(20, 20, 20));
    drawStateShield(g, 4, 1, makeWideOval(4, 1), new Color(30, 70, 150));
    drawStateShield(g, 5, 1, makeRoundedSquare(5, 1), new Color(35, 55, 130));
    drawStateShield(g, 6, 1, makeMaineShape(6, 1), new Color(165, 40, 50));
    drawStateShield(g, 7, 1, makePeakShape(7, 1, true), new Color(40, 100, 60));
    drawStateShield(g, 0, 2, makeRhodeIslandShape(0, 2), new Color(55, 95, 160));
    drawStateShield(g, 1, 2, makePeakShape(1, 2, false), new Color(55, 120, 70));
  }

  // ---- State shield helpers ----

  private static void drawStateShield(Graphics2D g, int col, int row, Shape shape, Color color) {
    g.setColor(color);
    g.fill(shape);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2.5f));
    g.draw(shape);
  }

  private static Shape makeCaliforniaShape(int col, int row) {
    // Spade-like outline: rounded top, narrow point at bottom.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + s / 2.0, y + m);
    p.curveTo(x + s - m, y + s * 0.10, x + s - m, y + s * 0.62, x + s / 2.0, y + s - m);
    p.curveTo(x + m, y + s * 0.62, x + m, y + s * 0.10, x + s / 2.0, y + m);
    p.closePath();
    return p;
  }

  private static Shape makeTexasShape(int col, int row) {
    // Polygon hinting at the panhandle on top-left and pointed bottom.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s * 0.42, y + m);
    p.lineTo(x + s * 0.42, y + s * 0.22);
    p.lineTo(x + s - m, y + s * 0.22);
    p.lineTo(x + s - m, y + s * 0.55);
    p.lineTo(x + s * 0.78, y + s * 0.62);
    p.lineTo(x + s * 0.55, y + s - m);
    p.lineTo(x + s * 0.30, y + s * 0.55);
    p.lineTo(x + m, y + s * 0.55);
    p.closePath();
    return p;
  }

  private static Shape makeFloridaShape(int col, int row) {
    // L-ish: panhandle across top-left, peninsula down the right.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s - m, y + m);
    p.lineTo(x + s - m, y + s * 0.30);
    p.lineTo(x + s * 0.62, y + s * 0.30);
    p.lineTo(x + s * 0.78, y + s - m);
    p.lineTo(x + s * 0.50, y + s - m);
    p.lineTo(x + s * 0.30, y + s * 0.30);
    p.lineTo(x + m, y + s * 0.30);
    p.closePath();
    return p;
  }

  private static Shape makeNewYorkShape(int col, int row) {
    // Stair-step polygon roughly evoking NY's east-west spread with a Long Island hint.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + s * 0.20, y + m);
    p.lineTo(x + s - m, y + s * 0.10);
    p.lineTo(x + s - m, y + s * 0.55);
    p.lineTo(x + s * 0.80, y + s * 0.65);
    p.lineTo(x + s * 0.95, y + s * 0.78);
    p.lineTo(x + s * 0.55, y + s - m);
    p.lineTo(x + s * 0.30, y + s * 0.80);
    p.lineTo(x + m, y + s * 0.55);
    p.lineTo(x + s * 0.05, y + s * 0.18);
    p.closePath();
    return p;
  }

  private static Shape makeWideOval(int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;
    return new Ellipse2D.Float(x + m, y + s / 4f, s - m * 2, s / 2f);
  }

  private static Shape makeRoundedSquare(int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    return new RoundRectangle2D.Float(x + m, y + m, s - m * 2, s - m * 2, 14, 14);
  }

  private static Shape makeMaineShape(int col, int row) {
    // Tall blocky shape with a notch on the left for the lakes/coast.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s - m, y + m);
    p.lineTo(x + s - m, y + s - m);
    p.lineTo(x + s * 0.30, y + s - m);
    p.lineTo(x + s * 0.20, y + s * 0.65);
    p.lineTo(x + m, y + s * 0.55);
    p.closePath();
    return p;
  }

  private static Shape makePeakShape(int col, int row, boolean apexUp) {
    // Triangle-with-base; up = NH (mountain), down = VT (inverted, hint at bottom narrowing).
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    GeneralPath p = new GeneralPath();
    if (apexUp) {
      p.moveTo(x + s / 2.0, y + m);
      p.lineTo(x + s - m, y + s * 0.45);
      p.lineTo(x + s - m, y + s - m);
      p.lineTo(x + m, y + s - m);
      p.lineTo(x + m, y + s * 0.45);
    } else {
      p.moveTo(x + m, y + m);
      p.lineTo(x + s - m, y + m);
      p.lineTo(x + s - m, y + s * 0.55);
      p.lineTo(x + s / 2.0, y + s - m);
      p.lineTo(x + m, y + s * 0.55);
    }
    p.closePath();
    return p;
  }

  private static Shape makeRhodeIslandShape(int col, int row) {
    // Compact near-square (RI is the smallest state).
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 12;
    return new RoundRectangle2D.Float(x + m, y + m, s - m * 2, s - m * 2, 8, 8);
  }

  private static void drawSvgShield(Graphics2D g, int col, int row, String svgFile) {
    try {
      String resourcePath = SHIELD_RESOURCE_DIR + svgFile;
      InputStream svgStream = GuideSignAtlasTool.class.getResourceAsStream(resourcePath);
      if (svgStream == null) {
        System.err.println("SVG not found: " + resourcePath + " — falling back to placeholder");
        drawPlaceholder(g, col, row);
        return;
      }

      int targetSize = CELL_SIZE - SHIELD_PADDING * 2;
      BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
      transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) targetSize);
      transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) targetSize);

      TranscoderInput input = new TranscoderInput(svgStream);
      transcoder.transcode(input, null);
      BufferedImage shieldImg = transcoder.getImage();
      svgStream.close();

      if (shieldImg != null) {
        int cellX = col * CELL_SIZE;
        int cellY = row * CELL_SIZE;
        int imgW = shieldImg.getWidth();
        int imgH = shieldImg.getHeight();
        int drawX = cellX + (CELL_SIZE - imgW) / 2;
        int drawY = cellY + (CELL_SIZE - imgH) / 2;
        g.drawImage(shieldImg, drawX, drawY, null);
        System.out.println("  Rendered SVG: " + svgFile + " (" + imgW + "x" + imgH + ")");
      }
    } catch (Exception e) {
      System.err.println("Failed to render SVG " + svgFile + ": " + e.getMessage());
      drawPlaceholder(g, col, row);
    }
  }

  private static void drawPlaceholder(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int m = 6;
    g.setColor(new Color(200, 200, 200));
    g.fillRect(x + m, y + m, CELL_SIZE - m * 2, CELL_SIZE - m * 2);
    g.setColor(new Color(120, 120, 120));
    g.setStroke(new BasicStroke(1.0f));
    g.drawRect(x + m, y + m, CELL_SIZE - m * 2, CELL_SIZE - m * 2);
  }

  // ---- Programmatic shields (no SVG source available) ----

  private static void drawStateSquare(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;

    RoundRectangle2D outer = new RoundRectangle2D.Float(
        x + m, y + m, s - m * 2, s - m * 2, 6, 6);
    g.setColor(Color.WHITE);
    g.fill(outer);
    g.setColor(new Color(20, 20, 20));
    g.setStroke(new BasicStroke(2.5f));
    g.draw(outer);
  }

  private static void drawToll(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;

    RoundRectangle2D outer = new RoundRectangle2D.Float(
        x + m, y + m + 3, s - m * 2, s - m * 2 - 6, 10, 10);
    g.setColor(new Color(100, 50, 120));
    g.fill(outer);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2.0f));
    g.draw(outer);
  }

  private static void drawBlankCustom(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 8;

    RoundRectangle2D rect = new RoundRectangle2D.Float(
        x + m, y + m, s - m * 2, s - m * 2, 5, 5);
    g.setColor(new Color(180, 180, 180));
    g.fill(rect);
    g.setColor(new Color(100, 100, 100));
    g.setStroke(new BasicStroke(1.5f));
    g.draw(rect);
  }

  // ---- Arrow drawing ----

  private static void drawArrows(Graphics2D g) {
    int r0 = ARROW_ROW_OFFSET;
    drawArrow(g, 0, r0, ArrowDir.UP);
    drawArrow(g, 1, r0, ArrowDir.DOWN);
    drawArrow(g, 2, r0, ArrowDir.LEFT);
    drawArrow(g, 3, r0, ArrowDir.RIGHT);
    drawArrow(g, 4, r0, ArrowDir.UP_LEFT);
    drawArrow(g, 0, r0 + 1, ArrowDir.UP_RIGHT);
    drawArrow(g, 1, r0 + 1, ArrowDir.DOWN_LEFT);
    drawArrow(g, 2, r0 + 1, ArrowDir.DOWN_RIGHT);
    drawArrow(g, 3, r0 + 1, ArrowDir.UP_LEFT_RIGHT);
    drawArrow(g, 4, r0 + 1, ArrowDir.LEFT_RIGHT);
  }

  private enum ArrowDir {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT,
    UP_LEFT_RIGHT, LEFT_RIGHT
  }

  private static void drawArrow(Graphics2D g, int col, int row, ArrowDir dir) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;

    Graphics2D g2 = (Graphics2D) g.create(x, y, s, s);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.WHITE);

    switch (dir) {
      case UP:
        drawStraightArrow(g2, s, 0);
        break;
      case DOWN:
        drawStraightArrow(g2, s, 180);
        break;
      case LEFT:
        drawStraightArrow(g2, s, -90);
        break;
      case RIGHT:
        drawStraightArrow(g2, s, 90);
        break;
      case UP_LEFT:
        drawBentArrow(g2, s, false);
        break;
      case UP_RIGHT:
        drawBentArrow(g2, s, true);
        break;
      case DOWN_LEFT:
        drawStraightArrow(g2, s, -135);
        break;
      case DOWN_RIGHT:
        drawStraightArrow(g2, s, 135);
        break;
      case UP_LEFT_RIGHT:
        drawSplitArrow(g2, s);
        break;
      case LEFT_RIGHT:
        drawDoubleArrow(g2, s);
        break;
    }

    g2.dispose();
  }

  private static void drawStraightArrow(Graphics2D g, int s, double angleDeg) {
    float cx = s / 2f;
    float cy = s / 2f;

    GeneralPath arrow = new GeneralPath();
    float headW = s * 0.45f;
    float headH = s * 0.3f;
    float shaftW = s * 0.16f;
    float shaftTop = -cy + headH;
    float shaftBot = cy - 3;

    arrow.moveTo(0, -cy + 3);
    arrow.lineTo(-headW / 2, shaftTop);
    arrow.lineTo(-shaftW / 2, shaftTop);
    arrow.lineTo(-shaftW / 2, shaftBot);
    arrow.lineTo(shaftW / 2, shaftBot);
    arrow.lineTo(shaftW / 2, shaftTop);
    arrow.lineTo(headW / 2, shaftTop);
    arrow.closePath();

    AffineTransform at = new AffineTransform();
    at.translate(cx, cy);
    at.rotate(Math.toRadians(angleDeg));
    arrow.transform(at);

    g.fill(arrow);
  }

  private static void drawBentArrow(Graphics2D g, int s, boolean mirrorRight) {
    float cx = s / 2f;
    float m = 4;
    float shaftW = s * 0.15f;
    float headW = s * 0.4f;
    float headH = s * 0.28f;

    GeneralPath arrow = new GeneralPath();
    float shaftBot = s - m;
    float bendY = s * 0.45f;
    float tipX = mirrorRight ? s - m : m;
    float shaftX = cx;

    arrow.moveTo(shaftX - shaftW / 2, shaftBot);
    arrow.lineTo(shaftX - shaftW / 2, bendY);
    if (mirrorRight) {
      arrow.lineTo(tipX - headH, bendY - headW / 2);
      arrow.lineTo(tipX, bendY);
      arrow.lineTo(tipX - headH, bendY + headW / 2);
    } else {
      arrow.lineTo(tipX + headH, bendY - headW / 2);
      arrow.lineTo(tipX, bendY);
      arrow.lineTo(tipX + headH, bendY + headW / 2);
    }
    arrow.lineTo(shaftX + shaftW / 2, bendY);
    arrow.lineTo(shaftX + shaftW / 2, shaftBot);
    arrow.closePath();

    g.fill(arrow);
  }

  private static void drawSplitArrow(Graphics2D g, int s) {
    float cx = s / 2f;
    float m = 4;
    float shaftW = s * 0.14f;
    float headW = s * 0.32f;
    float headH = s * 0.24f;

    float shaftBot = s - m;
    float splitY = s * 0.55f;
    float tipY = m;

    GeneralPath center = new GeneralPath();
    center.moveTo(cx - shaftW / 2, shaftBot);
    center.lineTo(cx - shaftW / 2, splitY);
    center.lineTo(cx + shaftW / 2, splitY);
    center.lineTo(cx + shaftW / 2, shaftBot);
    center.closePath();
    g.fill(center);

    drawStraightArrow(g, s, 0);

    float offsetX = s * 0.28f;
    GeneralPath leftHead = new GeneralPath();
    float lx = cx - offsetX;
    leftHead.moveTo(lx, tipY);
    leftHead.lineTo(lx - headW / 2, tipY + headH);
    leftHead.lineTo(lx - shaftW / 2, tipY + headH);
    leftHead.lineTo(lx - shaftW / 2, splitY);
    leftHead.lineTo(lx + shaftW / 2, splitY);
    leftHead.lineTo(lx + shaftW / 2, tipY + headH);
    leftHead.lineTo(lx + headW / 2, tipY + headH);
    leftHead.closePath();
    g.fill(leftHead);

    GeneralPath rightHead = new GeneralPath();
    float rx = cx + offsetX;
    rightHead.moveTo(rx, tipY);
    rightHead.lineTo(rx - headW / 2, tipY + headH);
    rightHead.lineTo(rx - shaftW / 2, tipY + headH);
    rightHead.lineTo(rx - shaftW / 2, splitY);
    rightHead.lineTo(rx + shaftW / 2, splitY);
    rightHead.lineTo(rx + shaftW / 2, tipY + headH);
    rightHead.lineTo(rx + headW / 2, tipY + headH);
    rightHead.closePath();
    g.fill(rightHead);
  }

  private static void drawDoubleArrow(Graphics2D g, int s) {
    drawStraightArrow(g, s, -90);
    drawStraightArrow(g, s, 90);
  }

  // ---- Batik SVG to BufferedImage transcoder ----

  private static class BufferedImageTranscoder extends ImageTranscoder {
    private BufferedImage image;

    @Override
    public BufferedImage createImage(int w, int h) {
      return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage img, TranscoderOutput output) {
      this.image = img;
    }

    public BufferedImage getImage() {
      return image;
    }
  }
}
