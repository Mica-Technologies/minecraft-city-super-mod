package com.micatechnologies.minecraft.csm.tools;

import com.micatechnologies.minecraft.csm.tools.tool_framework.CsmLayout;
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
 *   <li>Row 9, cols 2-7: DC + Canadian province markers (DC, ON, QC, NB, NS, NL)</li>
 *   <li>Row 10, col 0: Canadian province marker (PE)</li>
 *   <li>Row 10, cols 1-4: Alto route markers (2- and 3-digit variants)</li>
 *   <li>Rows 11-12, cols 0-7: Street sign civic logos (StreetSignLogoType)</li>
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

  // Relative to a tree's assets/csm. The Roads module ships the guide sign textures, so the
  // atlas belongs in that tree; writing it to Core would put the path in two jars.
  private static final String OUTPUT_FOLDER =
      "textures/blocks/trafficaccessories/guidesign";
  private static final String OUTPUT_FILE_NAME = "sign_atlas.png";

  private static final String SHIELD_RESOURCE_DIR = "/guidesign/shields/";

  private static final int ATLAS_WIDTH = 512;
  private static final int ATLAS_HEIGHT = 1024;
  private static final int CELL_SIZE = 64;

  private static final int ARROW_ROW_OFFSET = 4;

  private static final int SHIELD_PADDING = 2;

  /** Inset of a civic logo's background plate inside its 64px cell. */
  private static final int LOGO_PADDING = 3;

  public static void main(String[] args) {
    CsmToolUtility.doToolExecuteWrapped("CSM Guide Sign Atlas Generator", args,
        (devEnvironmentPath) -> {
          CsmLayout layout = new CsmLayout(devEnvironmentPath);
          File outputFile =
              layout.assetInFolderForWrite(OUTPUT_FOLDER, OUTPUT_FILE_NAME);
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
          drawLogos(g);

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

    // Washington DC + Canadian provinces — programmatic approximations styled
    // after each jurisdiction's real route marker (DC flag bars, Ontario's
    // white King's Highway crest, Quebec's blue Autoroute band, etc).
    drawDcShield(g, 2, 9);          // District of Columbia
    drawOntarioShield(g, 3, 9);     // Ontario
    drawQuebecShield(g, 4, 9);      // Quebec
    drawStateShield(g, 5, 9, makeRoundedSquare(5, 9), new Color(30, 110, 55));  // New Brunswick
    drawStateShield(g, 6, 9, makePeakShape(6, 9, true), new Color(30, 70, 140)); // Nova Scotia
    drawStateShield(g, 7, 9, makeWideOval(7, 9), new Color(120, 20, 30));       // Newfoundland and Labrador
    drawStateShield(g, 0, 10, makeCircle(0, 10), new Color(120, 72, 40));       // Prince Edward Island

    // Alto route markers — provided PNG artwork, used as-is. The 3-digit variants are
    // wider (693x512); they are stretched to fill the square cell here and the TESR
    // draws them back at their true aspect (GuideSignShieldType wide-variant support).
    drawPngShield(g, 1, 10, "shieldalto2.png");
    drawPngShield(g, 2, 10, "shieldalto3.png");
    drawPngShield(g, 3, 10, "shieldaltoblue2.png");
    drawPngShield(g, 4, 10, "shieldaltoblue3.png");
  }

  private static void drawPngShield(Graphics2D g, int col, int row, String pngFile) {
    try {
      String resourcePath = SHIELD_RESOURCE_DIR + pngFile;
      InputStream pngStream = GuideSignAtlasTool.class.getResourceAsStream(resourcePath);
      if (pngStream == null) {
        System.err.println("PNG not found: " + resourcePath + " — falling back to placeholder");
        drawPlaceholder(g, col, row);
        return;
      }
      BufferedImage img = ImageIO.read(pngStream);
      pngStream.close();
      int x = col * CELL_SIZE;
      int y = row * CELL_SIZE;
      int target = CELL_SIZE - SHIELD_PADDING * 2;
      // Fill the padded cell fully (aspect restored at render time for wide variants).
      g.drawImage(img, x + SHIELD_PADDING, y + SHIELD_PADDING, target, target, null);
      System.out.println("  Rendered PNG: " + pngFile + " (" + img.getWidth() + "x"
          + img.getHeight() + ")");
    } catch (Exception e) {
      System.err.println("Failed to render PNG " + pngFile + ": " + e.getMessage());
      drawPlaceholder(g, col, row);
    }
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
    // Rectangular state outline with a modest notch cut from the top-right corner,
    // evoking AZ's straight borders with the Utah/Four Corners step. The notch is
    // kept small and the margins are balanced (rather than uniform) so the shape's
    // visual mass — not just its bounding box — centers in the cell.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    float mLeft = 7f;
    float mRight = 5f;
    float mTop = 5f;
    float mBottom = 7f;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + mLeft, y + mTop);
    p.lineTo(x + s * 0.72, y + mTop);
    p.lineTo(x + s * 0.72, y + s * 0.30);
    p.lineTo(x + s - mRight, y + s * 0.30);
    p.lineTo(x + s - mRight, y + s - mBottom);
    p.lineTo(x + mLeft, y + s - mBottom);
    p.closePath();
    return p;
  }

  private static Shape makeOhioShape(int col, int row) {
    // Straight western/southern edges with a jagged eastern edge, hinting at
    // the Ohio River border. Enlarged (smaller margin, gentler notch) versus the
    // original so the mid-band stays comfortably wide for a 2-digit route number.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 4;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s * 0.72, y + m);
    p.lineTo(x + s * 0.80, y + s * 0.14);
    p.lineTo(x + s * 0.70, y + s * 0.26);
    p.lineTo(x + s - m, y + s * 0.38);
    p.lineTo(x + s * 0.88, y + s * 0.55);
    p.lineTo(x + s - m, y + s * 0.66);
    p.lineTo(x + s * 0.78, y + s - m);
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
    // Domed top (chord arc) fused with a trapezoid base — a simple beehive. Widened
    // from the original (which tapered to only ~20px across its dome/base) so the
    // trapezoid — which now starts above the text mid-band — carries a route number.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    double baseBottomY = y + s - m;
    double baseTopY = y + s * 0.375;
    double domeTopY = y + m;
    double leftTop = x + s * 0.22;
    double rightTop = x + s * 0.78;
    double leftBottom = x + s * 0.125;
    double rightBottom = x + s * 0.875;

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
    // Upright oval with a flat-cut bottom, suggesting a bust profile without
    // attempting actual portraiture. Built as an ellipse intersected with a
    // rectangle (rather than a half-height CHORD arc, which only ever traced the
    // ellipse's top semicircle and left the shape stranded in the cell's upper
    // half) so the silhouette is properly centered and wide across its middle.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    double ry = 24;
    double rx = 21.6;
    double domeTopY = y + 12;
    double equatorY = domeTopY + ry;
    double clipBottomY = equatorY + ry - 8;

    Ellipse2D.Double oval = new Ellipse2D.Double(
        x + s / 2.0 - rx, domeTopY, rx * 2, ry * 2);
    java.awt.geom.Rectangle2D.Double clip = new java.awt.geom.Rectangle2D.Double(
        x, domeTopY, s, clipBottomY - domeTopY);

    Area bust = new Area(oval);
    bust.intersect(new Area(clip));
    return bust;
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

  private static void drawDcShield(Graphics2D g, int col, int row) {
    // White rounded rectangle with two thin red horizontal bars near the top,
    // a simplified nod to the DC flag's stars-and-bars motif.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;

    RoundRectangle2D outer = new RoundRectangle2D.Float(
        x + m, y + m, s - m * 2, s - m * 2, 8, 8);
    g.setColor(Color.WHITE);
    g.fill(outer);

    g.setColor(new Color(178, 34, 52));
    float barX = x + m + 6;
    float barW = s - m * 2 - 12;
    g.fill(new java.awt.geom.Rectangle2D.Float(barX, y + m + 8, barW, 4));
    g.fill(new java.awt.geom.Rectangle2D.Float(barX, y + m + 16, barW, 4));

    g.setColor(new Color(20, 20, 20));
    g.setStroke(new BasicStroke(2.5f));
    g.draw(outer);
  }

  private static Shape makeOntarioCrestShape(int col, int row) {
    // Shield crest: flat top, sides curving inward to a point at the bottom —
    // evokes Ontario's white King's Highway marker silhouette.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;
    GeneralPath p = new GeneralPath();
    p.moveTo(x + m, y + m);
    p.lineTo(x + s - m, y + m);
    p.curveTo(x + s - m, y + s * 0.55, x + s * 0.72, y + s * 0.85, x + s / 2.0, y + s - m);
    p.curveTo(x + s * 0.28, y + s * 0.85, x + m, y + s * 0.55, x + m, y + m);
    p.closePath();
    return p;
  }

  private static void drawOntarioShield(Graphics2D g, int col, int row) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;

    Shape crest = makeOntarioCrestShape(col, row);
    g.setColor(Color.WHITE);
    g.fill(crest);

    Area band = new Area(new java.awt.geom.Rectangle2D.Float(x, y + m + 4, s, 6));
    band.intersect(new Area(crest));
    g.setColor(new Color(0, 100, 55));
    g.fill(band);

    g.setColor(new Color(20, 20, 20));
    g.setStroke(new BasicStroke(2.5f));
    g.draw(crest);
  }

  private static void drawQuebecShield(Graphics2D g, int col, int row) {
    // White rounded square with a blue top band — Quebec's Autoroute markers
    // are blue/white, and the band alone reads as distinctly Quebec at 64px.
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = 6;

    Shape square = makeRoundedSquare(col, row);
    g.setColor(Color.WHITE);
    g.fill(square);

    Area band = new Area(new java.awt.geom.Rectangle2D.Float(x, y + m, s, 12));
    band.intersect(new Area(square));
    g.setColor(new Color(35, 70, 160));
    g.fill(band);

    g.setColor(new Color(20, 20, 20));
    g.setStroke(new BasicStroke(2.5f));
    g.draw(square);
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

    // Rotation changes the ink's bounding box (a bbox-centered shape is no longer
    // bbox-centered after 45 degrees), which made diagonal arrows sit ~2px off-center
    // and render ~20% smaller than straight ones — the TESR draws every cell with the
    // same quad, so all arrows must have identically sized, centered ink. Scale the
    // ink's larger extent to the straight arrow's height and recenter on the cell.
    java.awt.geom.Rectangle2D b = arrow.getBounds2D();
    double targetExtent = s - 2 * m;
    double scale = targetExtent / Math.max(b.getWidth(), b.getHeight());
    AffineTransform fix = new AffineTransform();
    fix.translate(cx, cy);
    fix.scale(scale, scale);
    fix.translate(-b.getCenterX(), -b.getCenterY());
    arrow.transform(fix);

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

  // ---- Street sign civic logos (rows 11-12) ----

  /**
   * Draws the civic logo cells the dynamic street sign's emblem slot uses
   * ({@code StreetSignLogoType}, atlas rows 11-12). Unlike a route shield these cells are
   * self-contained artwork: each carries its own background plate so it reads on a green,
   * blue, brown, or white blade, and the renderer draws no text over it.
   *
   * <p>All sixteen are generic civic motifs drawn programmatically -- no real municipality's
   * seal or agency mark, which would be someone's trademark.
   */
  private static void drawLogos(Graphics2D g) {
    drawStarSeal(g, 0, 11);
    drawLaurelSeal(g, 1, 11);
    drawCivicShield(g, 2, 11);
    drawFleurDeLis(g, 3, 11);
    drawOakLeaf(g, 4, 11);
    drawPineTree(g, 5, 11);
    drawSkyline(g, 6, 11);
    drawBridge(g, 7, 11);

    drawMountain(g, 0, 12);
    drawRiver(g, 1, 12);
    drawSunrise(g, 2, 12);
    drawCompass(g, 3, 12);
    drawCapitol(g, 4, 12);
    drawTransit(g, 5, 12);
    drawAirport(g, 6, 12);
    drawHospital(g, 7, 12);
  }

  /**
   * Fills a logo cell's background plate and returns the padded drawing rectangle so each
   * motif can lay itself out in cell-local coordinates. {@code circle} picks a disc (seal
   * style) over a rounded square (agency-plate style).
   */
  private static java.awt.geom.Rectangle2D.Float logoPlate(Graphics2D g, int col, int row,
      Color background, boolean circle) {
    int x = col * CELL_SIZE;
    int y = row * CELL_SIZE;
    int s = CELL_SIZE;
    int m = LOGO_PADDING;
    Shape plate = circle
        ? new Ellipse2D.Float(x + m, y + m, s - 2f * m, s - 2f * m)
        : new RoundRectangle2D.Float(x + m, y + m, s - 2f * m, s - 2f * m, 10f, 10f);
    g.setColor(background);
    g.fill(plate);
    // A hairline outline in the contrasting color keeps the plate's edge crisp against a
    // sign face of a similar tone (a dark logo on a black blade, a white one on white).
    g.setColor(isLightColor(background) ? new Color(20, 20, 20) : new Color(245, 245, 240));
    g.setStroke(new BasicStroke(2.0f));
    g.draw(plate);
    return new java.awt.geom.Rectangle2D.Float(x + m, y + m, s - 2f * m, s - 2f * m);
  }

  /** A regular {@code points}-pointed star centered on (cx, cy). */
  private static Shape makeStar(float cx, float cy, float outerR, float innerR, int points) {
    GeneralPath p = new GeneralPath();
    for (int i = 0; i < points * 2; i++) {
      double angle = -Math.PI / 2 + i * Math.PI / points;
      float r = (i % 2 == 0) ? outerR : innerR;
      float px = cx + (float) (Math.cos(angle) * r);
      float py = cy + (float) (Math.sin(angle) * r);
      if (i == 0) {
        p.moveTo(px, py);
      } else {
        p.lineTo(px, py);
      }
    }
    p.closePath();
    return p;
  }

  private static void drawStarSeal(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(24, 54, 120), true);
    float cx = r.x + r.width / 2f;
    float cy = r.y + r.height / 2f;
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2.0f));
    g.draw(new Ellipse2D.Float(r.x + 4f, r.y + 4f, r.width - 8f, r.height - 8f));
    g.fill(makeStar(cx, cy, r.width * 0.30f, r.width * 0.13f, 5));
  }

  private static void drawLaurelSeal(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(28, 82, 56), true);
    float cx = r.x + r.width / 2f;
    float cy = r.y + r.height / 2f;
    float d = r.width - 14f;
    float ox = r.x + 7f;
    float oy = r.y + 7f;
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    // Two branches sweeping up from the bottom of the disc, stopping short of the top so the
    // wreath reads as open -- one arc on its own just looks like a crescent.
    g.draw(new Arc2D.Float(ox, oy, d, d, 275, 145, Arc2D.OPEN));
    g.draw(new Arc2D.Float(ox, oy, d, d, 265, -145, Arc2D.OPEN));
    // Leaves pinned along each branch, angled outward from the wreath center.
    for (int i = 0; i < 4; i++) {
      double angle = Math.toRadians(300 + i * 32);
      addLeaf(g, cx, cy, angle, d / 2f);
      addLeaf(g, cx, cy, Math.PI - angle, d / 2f);
    }
    g.fill(makeStar(cx, cy + 1f, r.width * 0.17f, r.width * 0.075f, 5));
  }

  /** One wreath leaf: a small ellipse tangent to the branch at the given polar angle. */
  private static void addLeaf(Graphics2D g, float cx, float cy, double angle, float radius) {
    float lx = cx + (float) (Math.cos(angle) * radius);
    float ly = cy - (float) (Math.sin(angle) * radius);
    AffineTransform saved = g.getTransform();
    g.rotate(-angle, lx, ly);
    g.fill(new Ellipse2D.Float(lx - 5f, ly - 2.2f, 10f, 4.4f));
    g.setTransform(saved);
  }

  private static void drawCivicShield(Graphics2D g, int col, int row) {
    Color plate = new Color(140, 26, 34);
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, plate, false);
    float l = r.x + r.width * 0.22f;
    float rr = r.x + r.width * 0.78f;
    float t = r.y + r.height * 0.15f;
    float b = r.y + r.height * 0.88f;
    GeneralPath shield = new GeneralPath();
    shield.moveTo(l, t);
    shield.lineTo(rr, t);
    shield.lineTo(rr, r.y + r.height * 0.58f);
    shield.quadTo(rr, b, (l + rr) / 2f, b);
    shield.quadTo(l, b, l, r.y + r.height * 0.58f);
    shield.closePath();
    g.setColor(Color.WHITE);
    g.fill(shield);
    // Heraldic chief: a narrow band across the head in the plate color, charged with a white
    // mullet. A deep band reads as a container rim rather than a crest.
    float chiefHeight = r.height * 0.13f;
    g.setColor(plate);
    g.fill(new java.awt.geom.Rectangle2D.Float(l, t, rr - l, chiefHeight));
    g.setColor(Color.WHITE);
    g.fill(makeStar((l + rr) / 2f, t + chiefHeight / 2f, chiefHeight * 0.48f,
        chiefHeight * 0.20f, 5));
  }

  private static void drawFleurDeLis(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(72, 40, 120), false);
    float cx = r.x + r.width / 2f;
    float top = r.y + r.height * 0.14f;
    float mid = r.y + r.height * 0.58f;
    float bot = r.y + r.height * 0.86f;
    float w = r.width * 0.34f;
    g.setColor(Color.WHITE);
    // Center petal, wide enough at the shoulders to read as a petal rather than a spike.
    GeneralPath center = new GeneralPath();
    center.moveTo(cx, top);
    center.quadTo(cx + w * 0.85f, r.y + r.height * 0.42f, cx, mid);
    center.quadTo(cx - w * 0.85f, r.y + r.height * 0.42f, cx, top);
    center.closePath();
    g.fill(center);
    // Side petals curl outward and hook back up, the way the heraldic charge does.
    GeneralPath left = new GeneralPath();
    left.moveTo(cx - 2f, mid - 4f);
    left.quadTo(cx - w * 1.20f, r.y + r.height * 0.34f, cx - w * 1.10f, r.y + r.height * 0.56f);
    left.quadTo(cx - w * 0.55f, r.y + r.height * 0.66f, cx - 2f, mid);
    left.closePath();
    g.fill(left);
    GeneralPath right = new GeneralPath();
    right.moveTo(cx + 2f, mid - 4f);
    right.quadTo(cx + w * 1.20f, r.y + r.height * 0.34f, cx + w * 1.10f, r.y + r.height * 0.56f);
    right.quadTo(cx + w * 0.55f, r.y + r.height * 0.66f, cx + 2f, mid);
    right.closePath();
    g.fill(right);
    // Binding band and foot.
    g.fill(new java.awt.geom.Rectangle2D.Float(cx - w * 0.78f, mid + 1f, w * 1.56f, 4.5f));
    g.fill(new java.awt.geom.Rectangle2D.Float(cx - 3.5f, mid + 5f, 7f, bot - mid - 5f));
  }

  private static void drawOakLeaf(Graphics2D g, int col, int row) {
    Color plate = new Color(34, 96, 52);
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, plate, false);
    float cx = r.x + r.width / 2f;
    float tip = r.y + r.height * 0.13f;
    float stemTop = r.y + r.height * 0.78f;
    float stemBottom = r.y + r.height * 0.89f;
    float halfWidth = r.width * 0.29f;
    // A symmetric almond blade: two mirrored quadratics from tip to stem. Lobed outlines
    // collapsed into a blob at this cell size, so the leaf reads from its silhouette plus
    // the midrib and veins instead.
    GeneralPath blade = new GeneralPath();
    blade.moveTo(cx, tip);
    blade.quadTo(cx + halfWidth, r.y + r.height * 0.34f, cx + halfWidth * 0.62f,
        r.y + r.height * 0.62f);
    blade.quadTo(cx + halfWidth * 0.30f, r.y + r.height * 0.74f, cx, stemTop);
    blade.quadTo(cx - halfWidth * 0.30f, r.y + r.height * 0.74f, cx - halfWidth * 0.62f,
        r.y + r.height * 0.62f);
    blade.quadTo(cx - halfWidth, r.y + r.height * 0.34f, cx, tip);
    blade.closePath();
    g.setColor(Color.WHITE);
    g.fill(blade);
    g.fill(new java.awt.geom.Rectangle2D.Float(cx - 2f, stemTop - 2f, 4f,
        stemBottom - stemTop + 2f));
    g.setColor(plate);
    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.draw(new java.awt.geom.Line2D.Float(cx, tip + 4f, cx, stemTop));
    for (int i = 0; i < 3; i++) {
      float y = r.y + r.height * (0.34f + i * 0.14f);
      float reach = halfWidth * (0.62f - i * 0.13f);
      g.draw(new java.awt.geom.Line2D.Float(cx, y, cx + reach, y - 4f));
      g.draw(new java.awt.geom.Line2D.Float(cx, y, cx - reach, y - 4f));
    }
  }

  private static void drawPineTree(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(20, 70, 48), true);
    float cx = r.x + r.width / 2f;
    g.setColor(Color.WHITE);
    // Three stacked boughs plus a trunk.
    for (int i = 0; i < 3; i++) {
      float top = r.y + r.height * (0.16f + i * 0.18f);
      float bottom = r.y + r.height * (0.44f + i * 0.18f);
      float halfWidth = r.width * (0.16f + i * 0.09f);
      GeneralPath bough = new GeneralPath();
      bough.moveTo(cx, top);
      bough.lineTo(cx + halfWidth, bottom);
      bough.lineTo(cx - halfWidth, bottom);
      bough.closePath();
      g.fill(bough);
    }
    g.fill(new java.awt.geom.Rectangle2D.Float(cx - 3f, r.y + r.height * 0.78f, 6f,
        r.height * 0.10f));
  }

  private static void drawSkyline(Graphics2D g, int col, int row) {
    Color plate = new Color(22, 42, 86);
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, plate, false);
    g.setColor(Color.WHITE);
    float base = r.y + r.height * 0.80f;
    float[] heights = {0.28f, 0.44f, 0.62f, 0.34f, 0.22f};
    float slotWidth = r.width * 0.68f / heights.length;
    float x0 = r.x + r.width * 0.16f;
    for (int i = 0; i < heights.length; i++) {
      float h = r.height * heights[i];
      float bx = x0 + i * slotWidth + 1f;
      float bw = slotWidth - 2f;
      g.fill(new java.awt.geom.Rectangle2D.Float(bx, base - h, bw, h));
      // A mast on the tallest tower plus punched window bands: without them the shapes read
      // as a bar chart rather than a skyline.
      if (heights[i] > 0.55f) {
        g.fill(new java.awt.geom.Rectangle2D.Float(bx + bw / 2f - 1f, base - h - 6f, 2f, 6f));
      }
      g.setColor(plate);
      for (float wy = base - h + 4f; wy < base - 4f; wy += 6f) {
        g.fill(new java.awt.geom.Rectangle2D.Float(bx + 2f, wy, bw - 4f, 2f));
      }
      g.setColor(Color.WHITE);
    }
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.12f, base, r.width * 0.76f,
        3f));
  }

  private static void drawBridge(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(20, 80, 96), false);
    g.setColor(Color.WHITE);
    float deck = r.y + r.height * 0.66f;
    float towerTop = r.y + r.height * 0.20f;
    float leftTower = r.x + r.width * 0.28f;
    float rightTower = r.x + r.width * 0.72f;
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.08f, deck, r.width * 0.84f,
        4f));
    g.fill(new java.awt.geom.Rectangle2D.Float(leftTower - 2f, towerTop, 4f, deck - towerTop));
    g.fill(new java.awt.geom.Rectangle2D.Float(rightTower - 2f, towerTop, 4f, deck - towerTop));
    // Main cable sagging between the towers, and the two back stays.
    g.setStroke(new BasicStroke(2.2f));
    GeneralPath cable = new GeneralPath();
    cable.moveTo(leftTower, towerTop + 2f);
    cable.quadTo((leftTower + rightTower) / 2f, deck + 4f, rightTower, towerTop + 2f);
    g.draw(cable);
    g.draw(new java.awt.geom.Line2D.Float(r.x + r.width * 0.08f, deck - 2f, leftTower,
        towerTop + 2f));
    g.draw(new java.awt.geom.Line2D.Float(r.x + r.width * 0.92f, deck - 2f, rightTower,
        towerTop + 2f));
  }

  private static void drawMountain(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(52, 68, 92), false);
    g.setColor(Color.WHITE);
    float base = r.y + r.height * 0.78f;
    GeneralPath big = new GeneralPath();
    big.moveTo(r.x + r.width * 0.12f, base);
    big.lineTo(r.x + r.width * 0.44f, r.y + r.height * 0.20f);
    big.lineTo(r.x + r.width * 0.74f, base);
    big.closePath();
    g.fill(big);
    GeneralPath small = new GeneralPath();
    small.moveTo(r.x + r.width * 0.50f, base);
    small.lineTo(r.x + r.width * 0.70f, r.y + r.height * 0.38f);
    small.lineTo(r.x + r.width * 0.90f, base);
    small.closePath();
    g.fill(small);
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.10f, base, r.width * 0.80f,
        3f));
  }

  private static void drawRiver(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(24, 76, 140), false);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (int i = 0; i < 3; i++) {
      float y = r.y + r.height * (0.32f + i * 0.18f);
      GeneralPath wave = new GeneralPath();
      wave.moveTo(r.x + r.width * 0.12f, y);
      wave.quadTo(r.x + r.width * 0.30f, y - r.height * 0.10f, r.x + r.width * 0.50f, y);
      wave.quadTo(r.x + r.width * 0.70f, y + r.height * 0.10f, r.x + r.width * 0.88f, y);
      g.draw(wave);
    }
  }

  private static void drawSunrise(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(196, 104, 20), false);
    float cx = r.x + r.width / 2f;
    float horizon = r.y + r.height * 0.68f;
    g.setColor(Color.WHITE);
    g.fill(new Arc2D.Float(cx - r.width * 0.24f, horizon - r.width * 0.24f, r.width * 0.48f,
        r.width * 0.48f, 0, 180, Arc2D.PIE));
    g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (int i = 0; i <= 4; i++) {
      double angle = Math.PI * (0.1 + i * 0.2);
      float x1 = cx - (float) (Math.cos(angle) * r.width * 0.30f);
      float y1 = horizon - (float) (Math.sin(angle) * r.width * 0.30f);
      float x2 = cx - (float) (Math.cos(angle) * r.width * 0.40f);
      float y2 = horizon - (float) (Math.sin(angle) * r.width * 0.40f);
      g.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));
    }
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.10f, horizon, r.width * 0.80f,
        3.5f));
  }

  private static void drawCompass(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(24, 24, 28), true);
    float cx = r.x + r.width / 2f;
    float cy = r.y + r.height / 2f;
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(1.8f));
    g.draw(new Ellipse2D.Float(r.x + 4f, r.y + 4f, r.width - 8f, r.height - 8f));
    // Four-point rose: long primaries with pinched waists, short diagonals behind.
    g.fill(makeStar(cx, cy, r.width * 0.34f, r.width * 0.07f, 4));
    AffineTransform saved = g.getTransform();
    g.rotate(Math.PI / 4, cx, cy);
    g.fill(makeStar(cx, cy, r.width * 0.20f, r.width * 0.05f, 4));
    g.setTransform(saved);
  }

  private static void drawCapitol(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(70, 74, 82), false);
    float cx = r.x + r.width / 2f;
    float base = r.y + r.height * 0.80f;
    float colonnadeTop = r.y + r.height * 0.52f;
    g.setColor(Color.WHITE);
    g.fill(new Arc2D.Float(cx - r.width * 0.20f, colonnadeTop - r.width * 0.20f,
        r.width * 0.40f, r.width * 0.40f, 0, 180, Arc2D.PIE));
    g.fill(new java.awt.geom.Rectangle2D.Float(cx - 2f, r.y + r.height * 0.18f, 4f,
        r.height * 0.14f));
    // Four columns under a stylobate.
    for (int i = 0; i < 4; i++) {
      float x = r.x + r.width * (0.24f + i * 0.15f);
      g.fill(new java.awt.geom.Rectangle2D.Float(x, colonnadeTop + 2f, 5f, base - colonnadeTop
          - 2f));
    }
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.14f, base, r.width * 0.72f,
        4f));
  }

  private static void drawTransit(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(26, 60, 130), false);
    g.setColor(Color.WHITE);
    float bodyX = r.x + r.width * 0.16f;
    float bodyY = r.y + r.height * 0.24f;
    float bodyW = r.width * 0.68f;
    float bodyH = r.height * 0.44f;
    g.fill(new RoundRectangle2D.Float(bodyX, bodyY, bodyW, bodyH, 8f, 8f));
    // Windscreen and side glazing punched back out in the plate color.
    g.setColor(new Color(26, 60, 130));
    g.fill(new java.awt.geom.Rectangle2D.Float(bodyX + 4f, bodyY + 5f, bodyW - 8f,
        bodyH * 0.38f));
    g.setColor(Color.WHITE);
    g.fill(new Ellipse2D.Float(bodyX + bodyW * 0.14f, bodyY + bodyH - 2f, 9f, 9f));
    g.fill(new Ellipse2D.Float(bodyX + bodyW * 0.68f, bodyY + bodyH - 2f, 9f, 9f));
  }

  private static void drawAirport(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(20, 58, 110), false);
    float cx = r.x + r.width / 2f;
    g.setColor(Color.WHITE);
    GeneralPath plane = new GeneralPath();
    plane.moveTo(cx, r.y + r.height * 0.14f);
    plane.quadTo(cx + 4f, r.y + r.height * 0.28f, cx + 4f, r.y + r.height * 0.42f);
    plane.lineTo(r.x + r.width * 0.90f, r.y + r.height * 0.60f);
    plane.lineTo(r.x + r.width * 0.90f, r.y + r.height * 0.68f);
    plane.lineTo(cx + 4f, r.y + r.height * 0.62f);
    plane.lineTo(cx + 4f, r.y + r.height * 0.76f);
    plane.lineTo(cx + 10f, r.y + r.height * 0.86f);
    plane.lineTo(cx + 10f, r.y + r.height * 0.90f);
    plane.lineTo(cx, r.y + r.height * 0.86f);
    plane.lineTo(cx - 10f, r.y + r.height * 0.90f);
    plane.lineTo(cx - 10f, r.y + r.height * 0.86f);
    plane.lineTo(cx - 4f, r.y + r.height * 0.76f);
    plane.lineTo(cx - 4f, r.y + r.height * 0.62f);
    plane.lineTo(r.x + r.width * 0.10f, r.y + r.height * 0.68f);
    plane.lineTo(r.x + r.width * 0.10f, r.y + r.height * 0.60f);
    plane.lineTo(cx - 4f, r.y + r.height * 0.42f);
    plane.quadTo(cx - 4f, r.y + r.height * 0.28f, cx, r.y + r.height * 0.14f);
    plane.closePath();
    g.fill(plane);
  }

  private static void drawHospital(Graphics2D g, int col, int row) {
    java.awt.geom.Rectangle2D.Float r = logoPlate(g, col, row, new Color(20, 62, 150), false);
    g.setColor(Color.WHITE);
    // Drawn as bars rather than a glyph so the mark does not depend on a system font.
    float armW = r.width * 0.16f;
    float armH = r.height * 0.56f;
    float top = r.y + r.height * 0.22f;
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.24f, top, armW, armH));
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.60f, top, armW, armH));
    g.fill(new java.awt.geom.Rectangle2D.Float(r.x + r.width * 0.24f,
        top + armH / 2f - r.height * 0.07f, r.width * 0.52f, r.height * 0.14f));
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
