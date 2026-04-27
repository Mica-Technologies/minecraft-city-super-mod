package com.micatechnologies.minecraft.csm.codeutils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public final class CsmFontRenderer {

  public final int FONT_HEIGHT;

  private ResourceLocation textureLocation;
  private final float[] atlasCharWidths = new float[128];
  private final float[] displayWidths = new float[128];
  private final int cellW;
  private final int cellH;
  private final int atlasW;
  private final int atlasH;

  private static CsmFontRenderer highwayGothic;
  private static CsmFontRenderer electronicSign;

  public static CsmFontRenderer highwayGothic() {
    if (highwayGothic == null) {
      highwayGothic = new CsmFontRenderer(
          new ResourceLocation("csm", "fonts/highway_gothic_wide.ttf"), 28f, 9, 0);
    }
    return highwayGothic;
  }

  public static CsmFontRenderer electronicSign() {
    if (electronicSign == null) {
      electronicSign = new CsmFontRenderer(
          new ResourceLocation("csm", "fonts/highway_gothic_wide.ttf"), 56f, 9, 3);
    }
    return electronicSign;
  }

  /**
   * @param fontResource resource location of the TTF font file
   * @param renderSize   AWT font size for atlas rendering (larger = higher quality)
   * @param fontHeight   the reported FONT_HEIGHT (display units); quads are drawn at this height
   * @param dotPitch     dot matrix grid pitch in pixels (0 = no dot matrix filter)
   */
  private CsmFontRenderer(ResourceLocation fontResource, float renderSize, int fontHeight,
      int dotPitch) {
    this.FONT_HEIGHT = fontHeight;

    try {
      InputStream is = Minecraft.getMinecraft().getResourceManager()
          .getResource(fontResource).getInputStream();
      Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(renderSize);
      is.close();

      BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      Graphics2D tg = tmp.createGraphics();
      tg.setFont(font);
      FontMetrics fm = tg.getFontMetrics();

      int ascent = fm.getAscent();
      int descent = fm.getDescent();
      int actualHeight = ascent + descent;

      int maxW = 0;
      for (int i = 32; i < 127; i++) {
        int w = fm.charWidth((char) i);
        if (w > maxW) maxW = w;
      }

      cellW = maxW + 2;
      cellH = actualHeight + 2;
      atlasW = cellW * 16;
      atlasH = cellH * 16;

      float displayScale = (float) FONT_HEIGHT / cellH;

      BufferedImage atlas = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = atlas.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
          dotPitch > 0 ? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
              : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setFont(font);
      g.setColor(Color.WHITE);

      int widthSum = 0;
      int widthCount = 0;
      for (int i = 32; i < 127; i++) {
        int w = fm.charWidth((char) i);
        if (w > 0) {
          widthSum += w;
          widthCount++;
        }
      }
      int fallbackW = widthCount > 0 ? widthSum / widthCount : maxW;

      for (int i = 32; i < 127; i++) {
        char c = (char) i;
        int col = i % 16;
        int row = i / 16;
        g.drawString(String.valueOf(c), col * cellW + 1, row * cellH + ascent + 1);
        int cw = fm.charWidth(c);
        if (cw <= 0 && font.canDisplay(c)) {
          cw = fallbackW;
        }
        atlasCharWidths[i] = cw > 0 ? cw : fallbackW;
        displayWidths[i] = atlasCharWidths[i] * displayScale;
      }
      g.dispose();
      tg.dispose();

      if (dotPitch > 0) {
        applyDotMatrix(atlas, dotPitch, cellW, cellH);
      }

      DynamicTexture dynamicTexture = new DynamicTexture(atlas);
      textureLocation = Minecraft.getMinecraft().getTextureManager()
          .getDynamicTextureLocation(
              "csm_font_" + fontResource.getPath().replace('/', '_')
                  + (dotPitch > 0 ? "_dot" + dotPitch : ""),
              dynamicTexture);

    } catch (Exception e) {
      throw new RuntimeException("Failed to load CSM font: " + fontResource, e);
    }
  }

  private static void applyDotMatrix(BufferedImage img, int pitch, int cW, int cH) {
    int w = img.getWidth();
    int h = img.getHeight();
    int[] src = img.getRGB(0, 0, w, h, null, 0, w);
    int[] dst = new int[w * h];

    int dotSize = Math.max(pitch - 1, 1);
    int cellCols = w / cW;
    int cellRows = h / cH;

    for (int cr = 0; cr < cellRows; cr++) {
      for (int cc = 0; cc < cellCols; cc++) {
        int cx = cc * cW;
        int cy = cr * cH;
        int dotsX = cW / pitch;
        int dotsY = cH / pitch;

        for (int dy = 0; dy < dotsY; dy++) {
          for (int dx = 0; dx < dotsX; dx++) {
            int regionX = cx + dx * pitch;
            int regionY = cy + dy * pitch;

            int centerIdx = (regionY + pitch / 2) * w + (regionX + pitch / 2);
            boolean on = centerIdx >= 0 && centerIdx < src.length
                && ((src[centerIdx] >> 24) & 0xFF) > 64;

            if (on) {
              for (int fy = 0; fy < dotSize; fy++) {
                for (int fx = 0; fx < dotSize; fx++) {
                  int px = regionX + fx;
                  int py = regionY + fy;
                  if (px < w && py < h) {
                    dst[py * w + px] = 0xFFFFFFFF;
                  }
                }
              }
            }
          }
        }
      }
    }

    img.setRGB(0, 0, w, h, dst, 0, w);
  }

  public int getStringWidth(String text) {
    if (text == null) return 0;
    float width = 0;
    for (int i = 0; i < text.length(); i++) {
      int c = text.charAt(i);
      if (c >= 0 && c < 128) {
        width += displayWidths[c];
      }
    }
    return Math.round(width);
  }

  public void drawString(String text, int x, int y, int color) {
    if (text == null || text.isEmpty()) return;

    Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);
    GlStateManager.enableTexture2D();

    float r = ((color >> 16) & 0xFF) / 255.0f;
    float g = ((color >> 8) & 0xFF) / 255.0f;
    float b = (color & 0xFF) / 255.0f;
    GlStateManager.color(r, g, b, 1.0f);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

    float curX = x;
    for (int i = 0; i < text.length(); i++) {
      int c = text.charAt(i);
      if (c < 32 || c >= 127) {
        curX += displayWidths[' '];
        continue;
      }
      if (atlasCharWidths[c] == 0) {
        curX += displayWidths[' '];
        continue;
      }

      int col = c % 16;
      int row = c / 16;

      float u1 = (float) (col * cellW + 1) / atlasW;
      float v1 = (float) (row * cellH + 1) / atlasH;
      float u2 = u1 + atlasCharWidths[c] / atlasW;
      float v2 = (float) ((row + 1) * cellH) / atlasH;

      float dw = displayWidths[c];

      buf.pos(curX, y, 0).tex(u1, v1).endVertex();
      buf.pos(curX + dw, y, 0).tex(u2, v1).endVertex();
      buf.pos(curX + dw, y + FONT_HEIGHT, 0).tex(u2, v2).endVertex();
      buf.pos(curX, y + FONT_HEIGHT, 0).tex(u1, v2).endVertex();

      curX += dw;
    }

    tess.draw();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }
}
