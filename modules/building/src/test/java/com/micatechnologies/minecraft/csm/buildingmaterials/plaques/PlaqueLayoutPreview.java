package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.micatechnologies.minecraft.csm.buildingmaterials.plaques.PlaqueLayout.Piece;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Not a test: draws every plaque style, head on, from the real sheets and the real layout, into
 * one image for review without a game client. Runs only when asked:
 * {@code CSM_PLAQUE_PREVIEW=<file.png> ./gradlew test --tests '*PlaqueLayoutPreview*'}.
 */
class PlaqueLayoutPreview {

  private static final int PX = 16;   // image pixels per model pixel

  private static BufferedImage sheet(String name) throws Exception {
    try (InputStream in = PlaqueLayoutPreview.class.getResourceAsStream(
        "/assets/csm/textures/blocks/plaques/" + name + ".png")) {
      return ImageIO.read(in);
    }
  }

  private static BufferedImage tinted(BufferedImage src, int rgb) {
    BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(),
        BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < src.getHeight(); y++) {
      for (int x = 0; x < src.getWidth(); x++) {
        int p = src.getRGB(x, y);
        int a = p >>> 24;
        int r = (p >> 16 & 0xFF) * (rgb >> 16 & 0xFF) / 255;
        int g = (p >> 8 & 0xFF) * (rgb >> 8 & 0xFF) / 255;
        int b = (p & 0xFF) * (rgb & 0xFF) / 255;
        out.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
      }
    }
    return out;
  }

  private static void draw(Graphics2D g, BufferedImage[] sheets, PlaqueStyle style,
      AddressPlaque plaque, int ox, int oy) {
    g.setColor(new Color(0x8A6F5A));   // a brownstone wall
    g.fillRect(ox, oy, 16 * PX, 16 * PX);
    List<Piece> pieces = new ArrayList<>(PlaqueLayout.layout(style, plaque).pieces);
    pieces.sort(Comparator.comparingDouble(p -> p.d));
    for (Piece p : pieces) {
      if (p.back) {
        continue;
      }
      BufferedImage src = sheets[p.sheet];
      if (p.rgb != 0xFFFFFF) {
        src = tinted(src.getSubimage((int) p.u0, (int) p.v0,
            Math.max(1, (int) (p.u1 - p.u0)), Math.max(1, (int) (p.v1 - p.v0))), p.rgb);
      } else {
        src = src.getSubimage((int) p.u0, (int) p.v0, Math.max(1, (int) (p.u1 - p.u0)),
            Math.max(1, (int) (p.v1 - p.v0)));
      }
      int x0 = ox + Math.round(p.x0 * PX);
      int x1 = ox + Math.round(p.x1 * PX);
      int y0 = oy + Math.round((16 - p.y1) * PX);
      int y1 = oy + Math.round((16 - p.y0) * PX);
      g.drawImage(src, x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0), null);
    }
  }

  @Test
  void preview() throws Exception {
    String path = System.getenv("CSM_PLAQUE_PREVIEW");
    assumeTrue(path != null && !path.isEmpty());
    BufferedImage[] sheets = {sheet("address_plaque_glyphs"), sheet("address_plaque_bodies")};
    AddressPlaque[] samples = {
        new AddressPlaque.Builder().number("12").build(),
        new AddressPlaque.Builder().number("245").street("West 86th St").build(),
        new AddressPlaque.Builder().number("12345").build(),
        new AddressPlaque.Builder().number("1847").vertical(true).build(),
        new AddressPlaque.Builder().number("12345").vertical(true).plate(PlaqueSize.SMALL)
            .build(),
        new AddressPlaque.Builder().number("12345").street("Riverside Drive")
            .plate(PlaqueSize.SMALL).font(PlaqueFont.NARROW).build(),
        new AddressPlaque.Builder().number("310").lit(false).build(),
    };
    PlaqueStyle[] styles = PlaqueStyle.values();
    int cell = 16 * PX + 8;
    BufferedImage img = new BufferedImage(cell * samples.length, cell * styles.length,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.setComposite(AlphaComposite.SrcOver);
    g.setColor(Color.DARK_GRAY);
    g.fillRect(0, 0, img.getWidth(), img.getHeight());
    for (int r = 0; r < styles.length; r++) {
      for (int c = 0; c < samples.length; c++) {
        draw(g, sheets, styles[r], samples[c], c * cell, r * cell);
      }
    }
    g.dispose();
    ImageIO.write(img, "png", new File(path));
  }
}
