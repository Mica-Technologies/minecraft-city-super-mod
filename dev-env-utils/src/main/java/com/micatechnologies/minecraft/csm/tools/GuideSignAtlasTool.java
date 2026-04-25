package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates the combined guide sign atlas texture. Draws MUTCD-style highway
 * shields and directional arrows programmatically into a 256x256 atlas with
 * 32x32 cells.
 *
 * <p>Atlas layout (matches {@code GuideSignAtlas.java} in main mod):
 * <ul>
 *   <li>Row 0, cols 0-7: Shield backgrounds (no text baked in)</li>
 *   <li>Rows 4-5, cols 0-4: Directional arrows (white on transparent)</li>
 * </ul>
 *
 * <p>Run via IntelliJ run configuration or:
 * {@code mvn exec:java -Dexec.mainClass="...GuideSignAtlasTool" -Dexec.args="<project-root>"}
 */
public class GuideSignAtlasTool {

  private static final String OUTPUT_FILE =
      "src/main/resources/assets/csm/textures/blocks/trafficaccessories/guidesign/sign_atlas.png";

  private static final int ATLAS_SIZE = 512;
  private static final int CELL_SIZE = 64;
  private static final int COLS = ATLAS_SIZE / CELL_SIZE;

  private static final int ARROW_ROW_OFFSET = 4;

  private static final Color INTERSTATE_BLUE = new Color(0, 63, 135);
  private static final Color INTERSTATE_RED = new Color(175, 30, 45);
  private static final Color US_ROUTE_BLACK = new Color(20, 20, 20);
  private static final Color STATE_WHITE = new Color(255, 255, 255);
  private static final Color COUNTY_BLUE = new Color(0, 80, 160);
  private static final Color TOLL_PURPLE = new Color(100, 50, 120);

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
    drawInterstate(g, 0, 0);
    drawInterstateBusiness(g, 1, 0);
    drawUsRoute(g, 2, 0);
    drawStateSquare(g, 3, 0);
    drawStateCircle(g, 4, 0);
    drawCountyRoute(g, 5, 0);
    drawToll(g, 6, 0);
    drawBlankCustom(g, 7, 0);
  }

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

  // ---- Shield drawing methods ----

  private static void drawInterstate(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 2;

    GeneralPath shield = new GeneralPath();
    float cx = x + s / 2f;
    float top = y + m;
    float bot = y + s - m;
    float left = x + m;
    float right = x + s - m;
    float midY = y + s * 0.42f;
    float notchY = y + s * 0.30f;
    float notchInset = s * 0.12f;

    shield.moveTo(cx, top);
    shield.lineTo(right, notchY);
    shield.lineTo(right, midY);
    shield.lineTo(right - 1, bot);
    shield.lineTo(left + 1, bot);
    shield.lineTo(left, midY);
    shield.lineTo(left, notchY);
    shield.closePath();

    g.setColor(INTERSTATE_BLUE);
    g.fill(shield);
    g.setColor(INTERSTATE_RED);
    GeneralPath topPart = new GeneralPath();
    topPart.moveTo(cx, top);
    topPart.lineTo(right, notchY);
    topPart.lineTo(right, midY);
    topPart.lineTo(left, midY);
    topPart.lineTo(left, notchY);
    topPart.closePath();
    g.fill(topPart);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(1.2f));
    g.draw(shield);
    g.setStroke(new BasicStroke(0.5f));
    g.drawLine((int) left + 1, (int) midY, (int) right - 1, (int) midY);
  }

  private static void drawInterstateBusiness(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 2;

    GeneralPath shield = new GeneralPath();
    float cx = x + s / 2f;
    float top = y + m;
    float bot = y + s - m;
    float left = x + m;
    float right = x + s - m;
    float midY = y + s * 0.42f;
    float notchY = y + s * 0.30f;

    shield.moveTo(cx, top);
    shield.lineTo(right, notchY);
    shield.lineTo(right, midY);
    shield.lineTo(right - 1, bot);
    shield.lineTo(left + 1, bot);
    shield.lineTo(left, midY);
    shield.lineTo(left, notchY);
    shield.closePath();

    g.setColor(Color.WHITE);
    g.fill(shield);
    g.setColor(new Color(0, 120, 60));
    GeneralPath topPart = new GeneralPath();
    topPart.moveTo(cx, top);
    topPart.lineTo(right, notchY);
    topPart.lineTo(right, midY);
    topPart.lineTo(left, midY);
    topPart.lineTo(left, notchY);
    topPart.closePath();
    g.fill(topPart);
    g.setColor(new Color(0, 120, 60));
    g.setStroke(new BasicStroke(1.2f));
    g.draw(shield);
  }

  private static void drawUsRoute(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 3;

    RoundRectangle2D outer = new RoundRectangle2D.Float(
        x + m, y + m, s - m * 2, s - m * 2, 4, 4);
    RoundRectangle2D inner = new RoundRectangle2D.Float(
        x + m + 2, y + m + 2, s - m * 2 - 4, s - m * 2 - 4, 3, 3);

    g.setColor(US_ROUTE_BLACK);
    g.fill(outer);
    g.setColor(Color.WHITE);
    g.fill(inner);
    g.setColor(US_ROUTE_BLACK);
    g.setStroke(new BasicStroke(0.8f));
    g.draw(outer);
  }

  private static void drawStateSquare(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 3;

    RoundRectangle2D outer = new RoundRectangle2D.Float(
        x + m, y + m, s - m * 2, s - m * 2, 3, 3);
    g.setColor(Color.WHITE);
    g.fill(outer);
    g.setColor(US_ROUTE_BLACK);
    g.setStroke(new BasicStroke(1.5f));
    g.draw(outer);
  }

  private static void drawStateCircle(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 3;

    Ellipse2D outer = new Ellipse2D.Float(x + m, y + m, s - m * 2, s - m * 2);
    g.setColor(Color.WHITE);
    g.fill(outer);
    g.setColor(US_ROUTE_BLACK);
    g.setStroke(new BasicStroke(1.5f));
    g.draw(outer);
  }

  private static void drawCountyRoute(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 3;

    GeneralPath pentagon = new GeneralPath();
    float cx = x + s / 2f;
    float top = y + m;
    float bot = y + s - m;
    float left = x + m;
    float right = x + s - m;
    float midH = y + s * 0.55f;

    pentagon.moveTo(cx, top);
    pentagon.lineTo(right, y + s * 0.35f);
    pentagon.lineTo(right - 1, bot);
    pentagon.lineTo(left + 1, bot);
    pentagon.lineTo(left, y + s * 0.35f);
    pentagon.closePath();

    g.setColor(COUNTY_BLUE);
    g.fill(pentagon);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(1.2f));
    g.draw(pentagon);
  }

  private static void drawToll(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 3;

    RoundRectangle2D outer = new RoundRectangle2D.Float(
        x + m, y + m + 2, s - m * 2, s - m * 2 - 4, 6, 6);
    g.setColor(TOLL_PURPLE);
    g.fill(outer);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(1.2f));
    g.draw(outer);
  }

  private static void drawBlankCustom(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;

    RoundRectangle2D rect = new RoundRectangle2D.Float(
        x + m, y + m, s - m * 2, s - m * 2, 3, 3);
    g.setColor(new Color(180, 180, 180));
    g.fill(rect);
    g.setColor(new Color(100, 100, 100));
    g.setStroke(new BasicStroke(1.0f));
    g.draw(rect);
  }

  // ---- Arrow drawing ----

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

    float cx = s / 2f;
    float cy = s / 2f;

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
    float headW = s * 0.4f;
    float headH = s * 0.3f;
    float shaftW = s * 0.14f;
    float shaftTop = -cy + headH;
    float shaftBot = cy - 2;

    arrow.moveTo(0, -cy + 2);
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
    float m = 3;
    float shaftW = s * 0.13f;
    float headW = s * 0.35f;
    float headH = s * 0.25f;

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
    float m = 3;
    float shaftW = s * 0.13f;
    float headW = s * 0.3f;
    float headH = s * 0.22f;

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
}
