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
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GLContext;

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

      // The mip levels of a dot-matrix font are made from its lit cells without the gaps
      int[] mipSource = dotPitch > 0 ? applyDotMatrix(atlas, dotPitch, cellW, cellH) : null;
      int[] levelZero = atlas.getRGB(0, 0, atlasW, atlasH, null, 0, atlasW);
      whiten(levelZero);
      atlas.setRGB(0, 0, atlasW, atlasH, levelZero, 0, atlasW);

      DynamicTexture dynamicTexture = new DynamicTexture(atlas);
      textureLocation = Minecraft.getMinecraft().getTextureManager()
          .getDynamicTextureLocation(
              "csm_font_" + fontResource.getPath().replace('/', '_')
                  + (dotPitch > 0 ? "_dot" + dotPitch : ""),
              dynamicTexture);

      // Generate mipmaps so the atlas reads cleanly at distance. Without this, distant
      // text "shimmers" — the dot-matrix variant has 1-2-px-wide bright dots that beat
      // against screen pixels under nearest-filter minification, and even the
      // anti-aliased gothic variant aliases when minified significantly. Mip levels let
      // the GPU sample a properly downsampled version instead of the full-resolution
      // atlas. Keep mag=NEAREST so close-up text retains the hard pixel-art look the
      // dot-matrix design depends on.
      //
      // A dot-matrix font's levels are built here rather than by glGenerateMipmap. Averaged,
      // the dots and the gaps between them come to under half the brightness of a lit dot, so
      // an LED message went dim with distance and dark at a grazing angle, where the GPU picks
      // a small level (#309). Built from the lit cells, gaps filled, a distant message keeps the
      // brightness of its strokes, as a real one does.
      int id = dynamicTexture.getGlTextureId();
      if (dotPitch > 0) {
        // A level is (width >> level) by (height >> level), so stop before either reaches 0
        int levels = 31 - Integer.numberOfLeadingZeros(Math.min(atlasW, atlasH));
        int[][] mips = new int[levels + 1][];
        mips[0] = levelZero;
        int[] prev = mipSource;
        int pw = atlasW;
        int ph = atlasH;
        for (int level = 1; level <= levels; level++) {
          prev = halve(prev, pw, ph);
          pw >>= 1;
          ph >>= 1;
          mips[level] = prev;
        }
        // Allocates every level and sets MAX_LEVEL and MAX_LOD to match
        TextureUtil.allocateTextureImpl(id, levels, atlasW, atlasH);
        TextureUtil.uploadTextureMipmap(mips, atlasW, atlasH, 0, 0, false, false);
      } else {
        int maxLevel = (int) Math.ceil(Math.log(Math.max(atlasW, atlasH)) / Math.log(2.0));
        GlStateManager.bindTexture(id);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL,
            maxLevel);
        // DynamicTexture's allocation (TextureUtil.allocateTextureImpl with 0 mip levels) also
        // clamps GL_TEXTURE_MAX_LOD to 0, which pins sampling to the full-size atlas whatever
        // the levels hold: minified text is then bilinear on the full-size glyphs and a thin
        // stroke (an I, the bars of an E) falls between the four texels a pixel reads and
        // vanishes. Lift the clamp, or the mip levels below are never used.
        GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, maxLevel);
      }
      GlStateManager.bindTexture(id);
      GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
          GL11.GL_LINEAR_MIPMAP_LINEAR);
      GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
          GL11.GL_NEAREST);
      // A sign is frequently seen at a grazing angle. Ordinary trilinear mipmapping chooses a
      // single low-detail level from the most-compressed screen axis, then uses it for both axes;
      // that averages the sparse LED dots away and makes an otherwise-lit message look dark.
      // Anisotropic filtering keeps the less-compressed axis sharp while retaining the mipmaps
      // that stop distant text shimmering. The extension is optional, so older drivers keep the
      // existing trilinear fallback.
      if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
        float maxAnisotropy = GL11.glGetFloat(
            EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D,
            EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
            Math.max(1.0f, maxAnisotropy));
      }
      if (dotPitch <= 0) {
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to load CSM font: " + fontResource, e);
    }
  }

  /**
   * Makes every texel white, keeping its alpha. The glyphs are drawn white, but the texels around
   * them are transparent black, and every filter that mixes texels (mip levels, linear sampling)
   * mixed that black into the edges of the strokes and darkened them.
   */
  private static void whiten(int[] argb) {
    for (int i = 0; i < argb.length; i++) {
      argb[i] |= 0x00FFFFFF;
    }
  }

  /** The next mip level down: each texel the mean alpha of the two by two below it, white. */
  private static int[] halve(int[] src, int w, int h) {
    int nw = w >> 1;
    int nh = h >> 1;
    int[] dst = new int[nw * nh];
    for (int y = 0; y < nh; y++) {
      for (int x = 0; x < nw; x++) {
        int i = (y * 2) * w + x * 2;
        int a = (src[i] >>> 24) + (src[i + 1] >>> 24) + (src[i + w] >>> 24)
            + (src[i + w + 1] >>> 24);
        dst[y * nw + x] = ((a + 2) / 4) << 24 | 0x00FFFFFF;
      }
    }
    return dst;
  }

  /**
   * Redraws the atlas as a grid of lit dots, and returns the same grid with each lit dot filling
   * its whole cell, gaps closed, which the mip levels are made from.
   */
  private static int[] applyDotMatrix(BufferedImage img, int pitch, int cW, int cH) {
    int w = img.getWidth();
    int h = img.getHeight();
    int[] src = img.getRGB(0, 0, w, h, null, 0, w);
    int[] dst = new int[w * h];
    int[] solid = new int[w * h];

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
              for (int fy = 0; fy < pitch; fy++) {
                for (int fx = 0; fx < pitch; fx++) {
                  int px = regionX + fx;
                  int py = regionY + fy;
                  if (px < w && py < h) {
                    solid[py * w + px] = 0xFFFFFFFF;
                  }
                }
              }
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
    return solid;
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

    bindAtlas();
    GlStateManager.enableTexture2D();

    float r = ((color >> 16) & 0xFF) / 255.0f;
    float g = ((color >> 8) & 0xFF) / 255.0f;
    float b = (color & 0xFF) / 255.0f;
    GlStateManager.color(r, g, b, 1.0f);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    addString(buf, text, x, y);
    tess.draw();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  /**
   * Binds this font's atlas. For a caller that compiles {@link #addString} output into a display
   * list, which must bind the atlas outside the list, every frame.
   */
  public void bindAtlas() {
    Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);
  }

  /**
   * Adds the quads {@link #drawString} draws for {@code text}, and nothing else: no texture bind,
   * no colour, no draw. The caller binds the atlas ({@link #bindAtlas()}) and sets the colour.
   *
   * @param buf  the buffer, begun as {@code GL_QUADS} in {@code POSITION_TEX}
   * @param text the text to add
   * @param x    left edge
   * @param y    top edge
   */
  public void addString(BufferBuilder buf, String text, int x, int y) {
    if (text == null || text.isEmpty()) return;

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
  }
}
