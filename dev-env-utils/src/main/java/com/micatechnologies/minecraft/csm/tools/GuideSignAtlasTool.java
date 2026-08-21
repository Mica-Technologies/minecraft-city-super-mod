package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmToolUtility;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
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
 * <p>Atlas layout (512x1024, 64x64 cells, matches {@code GuideSignAtlas.java}):
 * <ul>
 *   <li>Row 0, cols 0-7: Generic shield backgrounds (no text baked in)</li>
 *   <li>Row 1, cols 0-7: State markers (CA, TX, FL, NY, CT, MA, ME, NH)</li>
 *   <li>Row 2, cols 0-1: State markers (RI, VT)</li>
 *   <li>Row 2, cols 2-7: State markers (AL, AK, AZ, AR, CO, DE)</li>
 *   <li>Row 3, cols 0-7: State markers (GA, HI, ID, IL, IN, IA, KS, KY)</li>
 *   <li>Rows 4-5, cols 0-4: Directional arrows (white on transparent)</li>
 *   <li>Row 6, cols 0-7: State markers (LA, MD, MI, MN, MS, MO, MT, NE)</li>
 *   <li>Row 7, cols 0-7: State markers (NV, NJ, NM, NC, ND, OH, OK, OR)</li>
 *   <li>Row 8, cols 0-7: State markers (PA, SC, SD, TN, UT, VA, WA, WV)</li>
 *   <li>Row 9, cols 0-1: State markers (WI, WY)</li>
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

  private static final int ATLAS_WIDTH = 512;
  private static final int ATLAS_HEIGHT = 1024;
  private static final int CELL_SIZE = 64;

  private static final int ARROW_ROW_OFFSET = 4;

  private static final int SHIELD_PADDING = 2;

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Guide Sign Atlas Generator", args,
        (devEnvironmentPath) -> {
          File outputFile = new File(devEnvironmentPath, OUTPUT_FILE);
          outputFile.getParentFile().mkdirs();

          BufferedImage atlas = new BufferedImage(
              ATLAS_WIDTH, ATLAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
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

    // Remaining 40 states — mix of a few iconic silhouettes (keystone, Ohio's
    // jagged eastern border, a beehive, a notched Arizona rectangle, a bust-like
    // oval for Washington) and generic archetypes (circle/hexagon/diamond/
    // octagon/oval/rounded-square/peak) in state-themed colors for the rest.
    drawStateShield(g, 2, 2, makeOctagon(2, 2), new Color(176, 48, 64));       // Alabama
    drawStateShield(g, 3, 2, makeHexagon(3, 2), new Color(46, 111, 142));      // Alaska
    drawStateShield(g, 4, 2, makeArizonaShape(4, 2), new Color(181, 101, 29)); // Arizona
    drawStateShield(g, 5, 2, makeDiamond(5, 2), new Color(47, 107, 58));       // Arkansas
    drawStateShield(g, 6, 2, makeRoundedSquare(6, 2), Color.WHITE);            // Colorado
    drawStateShield(g, 7, 2, makeCircle(7, 2), new Color(34, 68, 170));        // Delaware
    drawStateShield(g, 0, 3, makePeakShape(0, 3, true), new Color(224, 128, 48)); // Georgia
    drawStateShield(g, 1, 3, makeWideOval(1, 3), new Color(30, 138, 138));     // Hawaii
    drawStateShield(g, 2, 3, makePeakShape(2, 3, true), new Color(44, 71, 112)); // Idaho
    drawStateShield(g, 3, 3, makeRoundedSquare(3, 3), new Color(0, 51, 160));  // Illinois
    drawStateShield(g, 4, 3, makeDiamond(4, 3), new Color(27, 63, 139));       // Indiana
    drawStateShield(g, 5, 3, makeWideOval(5, 3), new Color(199, 154, 46));     // Iowa
    drawStateShield(g, 6, 3, makeCircle(6, 3), new Color(232, 201, 58));       // Kansas
    drawStateShield(g, 7, 3, makeHexagon(7, 3), new Color(46, 90, 62));        // Kentucky
    drawStateShield(g, 0, 6, makeDiamond(0, 6), new Color(91, 42, 134));       // Louisiana
    drawStateShield(g, 1, 6, makeHexagon(1, 6), new Color(26, 26, 26));        // Maryland
    drawStateShield(g, 2, 6, makePeakShape(2, 6, false), new Color(0, 39, 76)); // Michigan
    drawStateShield(g, 3, 6, makeRoundedSquare(3, 6), Color.WHITE);            // Minnesota
    drawStateShield(g, 4, 6, makeWideOval(4, 6), new Color(31, 110, 82));      // Mississippi
    drawStateShield(g, 5, 6, makeRoundedSquare(5, 6), Color.WHITE);            // Missouri
    drawStateShield(g, 6, 6, makePeakShape(6, 6, true), new Color(61, 111, 166)); // Montana
    drawStateShield(g, 7, 6, makeHexagon(7, 6), new Color(176, 30, 40));       // Nebraska
    drawStateShield(g, 0, 7, makeDiamond(0, 7), new Color(138, 141, 145));     // Nevada
    drawStateShield(g, 1, 7, makeOctagon(1, 7), new Color(30, 58, 138));       // New Jersey
    drawStateShield(g, 2, 7, makeRoundedSquare(2, 7), new Color(186, 12, 47)); // New Mexico (zia red)
    drawStateShield(g, 3, 7, makeDiamond(3, 7), Color.WHITE);                  // North Carolina
    drawStateShield(g, 4, 7, makeWideOval(4, 7), new Color(212, 175, 55));     // North Dakota
    drawStateShield(g, 5, 7, makeOhioShape(5, 7), Color.WHITE);                // Ohio
    drawStateShield(g, 6, 7, makeHexagon(6, 7), new Color(163, 63, 31));       // Oklahoma
    drawStateShield(g, 7, 7, makePeakShape(7, 7, true), new Color(27, 77, 62)); // Oregon
    drawStateShield(g, 0, 8, makeKeystoneShape(0, 8), new Color(20, 38, 75));  // Pennsylvania
    drawStateShield(g, 1, 8, makeDiamond(1, 8), new Color(102, 0, 31));        // South Carolina
    drawStateShield(g, 2, 8, makeCircle(2, 8), new Color(107, 107, 107));      // South Dakota
    drawStateShield(g, 3, 8, makeHexagon(3, 8), new Color(200, 16, 46));       // Tennessee
    drawStateShield(g, 4, 8, makeBeehiveShape(4, 8), Color.WHITE);             // Utah
    drawStateShield(g, 5, 8, makeWideOval(5, 8), new Color(35, 45, 75));       // Virginia
    drawStateShield(g, 6, 8, makeWashingtonBustShape(6, 8), Color.WHITE);      // Washington
    drawStateShield(g, 7, 8, makePeakShape(7, 8, true), new Color(0, 40, 85)); // West Virginia
    drawStateShield(g, 0, 9, makeRoundedSquare(0, 9), new Color(155, 27, 48)); // Wisconsin
    drawStateShield(g, 1, 9, makePeakShape(1, 9, true), new Color(28, 63, 110)); // Wyoming
  }

  // ---- State shield helpers ----

  private static void drawStateShield(Graphics2D g, int col, int row, Shape shape, Color color) {
    g.setColor(color);
    g.fill(shape);
    // Light fills (white/yellow/gold state shields) need a dark outline to stay
    // visible; dark fills keep the original white outline. Mirrors the dark-fill
    // -> white-text / light-fill -> dark-text rule used for routeTextColor.
    g.setColor(isLightColor(color) ? new Color(20, 20, 20) : Color.WHITE);
    g.setStroke(new BasicStroke(2.5f));
    g.draw(shape);
  }

  private static boolean isLightColor(Color c) {
    double luminance = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    return luminance > 150;
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

  private static Shape makeCircle(int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    return new Ellipse2D.Float(x + m, y + m, s - m * 2, s - m * 2);
  }

  private static Shape makeHexagon(int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    double cx = x + s / 2.0;
    double cy = y + s / 2.0;
    double r = (s - m * 2) / 2.0;
    GeneralPath p = new GeneralPath();
    for (int i = 0; i < 6; i++) {
      double angle = Math.toRadians(60 * i - 30);
      double px = cx + r * Math.cos(angle);
      double py = cy + r * Math.sin(angle);
      if (i == 0) {
        p.moveTo(px, py);
      } else {
        p.lineTo(px, py);
      }
    }
    p.closePath();
    return p;
  }

  private static Shape makeDiamond(int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 5;
    double cx = x + s / 2.0;
    double cy = y + s / 2.0;
    GeneralPath p = new GeneralPath();
    p.moveTo(cx, y + m);
    p.lineTo(x + s - m, cy);
    p.lineTo(cx, y + s - m);
    p.lineTo(x + m, cy);
    p.closePath();
    return p;
  }

  private static Shape makeOctagon(int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    double w = s - m * 2;
    double inset = w * 0.3;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m + inset, y + m);
    p.lineTo(x + s - m - inset, y + m);
    p.lineTo(x + s - m, y + m + inset);
    p.lineTo(x + s - m, y + s - m - inset);
    p.lineTo(x + s - m - inset, y + s - m);
    p.lineTo(x + m + inset, y + s - m);
    p.lineTo(x + m, y + s - m - inset);
    p.lineTo(x + m, y + m + inset);
    p.closePath();
    return p;
  }

  private static Shape makeArizonaShape(int col, int row) {
    // Rectangular state outline with a notch cut from the top-right corner,
    // evoking AZ's straight borders with the Utah/Four Corners step.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s * 0.65, y + m);
    p.lineTo(x + s * 0.65, y + s * 0.35);
    p.lineTo(x + s - m, y + s * 0.35);
    p.lineTo(x + s - m, y + s - m);
    p.lineTo(x + m, y + s - m);
    p.closePath();
    return p;
  }

  private static Shape makeOhioShape(int col, int row) {
    // Straight western/southern edges with a jagged eastern edge, hinting at
    // the Ohio River border.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s * 0.70, y + m);
    p.lineTo(x + s * 0.78, y + s * 0.15);
    p.lineTo(x + s * 0.68, y + s * 0.28);
    p.lineTo(x + s - m, y + s * 0.40);
    p.lineTo(x + s * 0.85, y + s * 0.55);
    p.lineTo(x + s - m, y + s * 0.68);
    p.lineTo(x + s * 0.75, y + s - m);
    p.lineTo(x + m, y + s - m);
    p.closePath();
    return p;
  }

  private static Shape makeKeystoneShape(int col, int row) {
    // Classic architectural keystone: narrow top, flared wider bottom.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + s * 0.34, y + m);
    p.lineTo(x + s * 0.66, y + m);
    p.lineTo(x + s * 0.80, y + s * 0.45);
    p.lineTo(x + s * 0.86, y + s - m);
    p.lineTo(x + s * 0.14, y + s - m);
    p.lineTo(x + s * 0.20, y + s * 0.45);
    p.closePath();
    return p;
  }

  private static Shape makeBeehiveShape(int col, int row) {
    // Domed top (chord arc) fused with a trapezoid base — a simple beehive.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    double baseBottomY = y + s - m;
    double baseTopY = y + s * 0.55;
    double domeTopY = y + m;
    double leftTop = x + s * 0.34;
    double rightTop = x + s * 0.66;
    double leftBottom = x + s * 0.22;
    double rightBottom = x + s * 0.78;

    Arc2D.Double dome = new Arc2D.Double(
        leftTop, domeTopY, rightTop - leftTop, (baseTopY - domeTopY) * 2, 0, 180, Arc2D.CHORD);

    GeneralPath trapezoid = new GeneralPath();
    trapezoid.moveTo(leftTop, baseTopY);
    trapezoid.lineTo(rightTop, baseTopY);
    trapezoid.lineTo(rightBottom, baseBottomY);
    trapezoid.lineTo(leftBottom, baseBottomY);
    trapezoid.closePath();

    Area beehive = new Area(dome);
    beehive.add(new Area(trapezoid));
    return beehive;
  }

  private static Shape makeWashingtonBustShape(int col, int row) {
    // Upright oval with a flat bottom (chord arc), suggesting a bust profile
    // without attempting actual portraiture.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 8;
    double height = s - m * 2;
    double width = height * 0.72;
    double left = x + (s - width) / 2.0;
    return new Arc2D.Double(left, y + m, width, height, 0, 180, Arc2D.CHORD);
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
        drawStraightArrow(g2, s, -45);
        break;
      case UP_RIGHT:
        drawStraightArrow(g2, s, 45);
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
    float m = 6f;

    // Bold FHWA/MUTCD-style proportions: thick shaft, large solid triangular
    // head clearly wider than the shaft, short overall length.
    float headW = s * 0.52f;
    float headH = s * 0.30f;
    float shaftW = s * 0.20f;

    float tipY = -cy + m;
    float shaftTop = tipY + headH;
    float shaftBot = cy - m;

    GeneralPath arrow = new GeneralPath();
    arrow.moveTo(0, tipY);
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

  private static void drawSplitArrow(Graphics2D g, int s) {
    float cx = s / 2f;
    float m = 6f;
    float shaftW = s * 0.20f;
    float headW = s * 0.36f;
    float headH = s * 0.26f;

    float shaftBot = s - m;
    float splitY = s * 0.56f;
    float tipY = m;

    GeneralPath center = new GeneralPath();
    center.moveTo(cx - shaftW / 2, shaftBot);
    center.lineTo(cx - shaftW / 2, splitY);
    center.lineTo(cx + shaftW / 2, splitY);
    center.lineTo(cx + shaftW / 2, shaftBot);
    center.closePath();
    g.fill(center);

    drawStraightArrow(g, s, 0);

    float offsetX = s * 0.24f;
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
