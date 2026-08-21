package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Draws dynamic guide sign legend text in the FHWA-style highway font instead of the
 * Minecraft font. Glyphs come from a pre-generated atlas
 * ({@code textures/fonts/guide_sign_font.png}) with metrics in
 * {@code assets/csm/fonts/guide_sign_font.json}; both are produced by the dev-env-utils
 * {@code GuideSignFontAtlasTool} from {@code highway_gothic_wide.ttf}.
 *
 * <p>All sizes are given as <b>cap height in sign pixel units</b> (the TESR's 1/16-block
 * pixel space), so callers reason in the same units as the rest of the sign layout.
 * Vertices are emitted in the TESR's (un-mirrored) pixel space: +X is the reader's
 * right, +Y up, and {@code u0} maps to a glyph's left edge — matching the renderer's
 * "Mirrored pixel space" contract.
 *
 * <p>Client-side only (referenced from the TESR).
 */
public final class GuideSignFontRenderer {

  private static final ResourceLocation FONT_TEXTURE =
      new ResourceLocation("csm", "textures/fonts/guide_sign_font.png");
  private static final String METRICS_PATH = "/assets/csm/fonts/guide_sign_font.json";
  private static final char FALLBACK_CHAR = '?';

  private static class GlyphJson {
    int col;
    int row;
    float advance;
  }

  private static class MetricsJson {
    float textureWidth;
    float textureHeight;
    float cellWidth;
    float cellHeight;
    float baseline;
    float originX;
    float capHeight;
    Map<String, GlyphJson> glyphs;
  }

  private static MetricsJson metrics;
  private static Map<Character, GlyphJson> glyphMap;
  private static boolean loadAttempted;

  private GuideSignFontRenderer() {
  }

  private static boolean ensureLoaded() {
    if (!loadAttempted) {
      loadAttempted = true;
      try (InputStream in = GuideSignFontRenderer.class.getResourceAsStream(METRICS_PATH)) {
        if (in == null) {
          System.err.println("[CSM] Guide sign font metrics missing: " + METRICS_PATH);
        } else {
          metrics = new Gson().fromJson(
              new InputStreamReader(in, StandardCharsets.UTF_8), MetricsJson.class);
          glyphMap = new HashMap<>();
          for (Map.Entry<String, GlyphJson> e : metrics.glyphs.entrySet()) {
            if (!e.getKey().isEmpty()) {
              glyphMap.put(e.getKey().charAt(0), e.getValue());
            }
          }
        }
      } catch (Exception ex) {
        System.err.println("[CSM] Failed to load guide sign font metrics: " + ex);
        metrics = null;
      }
    }
    return metrics != null && glyphMap != null;
  }

  private static GlyphJson glyph(char c) {
    GlyphJson g = glyphMap.get(c);
    return g != null ? g : glyphMap.get(FALLBACK_CHAR);
  }

  /** Width of {@code text} in sign pixels when drawn at the given cap height. */
  public static float getStringWidth(String text, float capHeightPx) {
    if (text == null || text.isEmpty() || !ensureLoaded()) {
      return 0;
    }
    float s = capHeightPx / metrics.capHeight;
    float w = 0;
    for (int i = 0; i < text.length(); i++) {
      GlyphJson g = glyph(text.charAt(i));
      if (g != null) {
        w += g.advance * s;
      }
    }
    return w;
  }

  /**
   * Draws {@code text} left-aligned at {@code leftX}, with the capital letters
   * vertically centered on {@code centerY}, at depth {@code z}. Color is 0xRRGGBB;
   * sky/block are the pre-split lightmap coordinates the TESR baked for this frame.
   * The caller is responsible for texture state around the call (this binds the font
   * atlas; re-bind your working texture afterwards, per the TESR's convention).
   */
  public static void drawString(String text, float leftX, float centerY, float z,
      float capHeightPx, int color, int sky, int block) {
    if (text == null || text.isEmpty() || !ensureLoaded()) {
      return;
    }
    float s = capHeightPx / metrics.capHeight;
    float r = ((color >> 16) & 0xFF) / 255.0f;
    float g = ((color >> 8) & 0xFF) / 255.0f;
    float b = (color & 0xFF) / 255.0f;

    Minecraft.getMinecraft().getTextureManager().bindTexture(FONT_TEXTURE);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    // Cap spans [baselineY, baselineY + capHeightPx]; center it on centerY.
    float baselineY = centerY - capHeightPx / 2.0f;
    float penX = leftX;

    for (int i = 0; i < text.length(); i++) {
      GlyphJson gl = glyph(text.charAt(i));
      if (gl == null) {
        continue;
      }
      float u0 = gl.col * metrics.cellWidth / metrics.textureWidth;
      float v0 = gl.row * metrics.cellHeight / metrics.textureHeight;
      float u1 = (gl.col + 1) * metrics.cellWidth / metrics.textureWidth;
      float v1 = (gl.row + 1) * metrics.cellHeight / metrics.textureHeight;

      float left = penX - metrics.originX * s;
      float right = left + metrics.cellWidth * s;
      float top = baselineY + metrics.baseline * s;
      float bottom = top - metrics.cellHeight * s;

      buf.pos(left, top, z).color(r, g, b, 1.0f).tex(u0, v0).lightmap(sky, block).endVertex();
      buf.pos(right, top, z).color(r, g, b, 1.0f).tex(u1, v0).lightmap(sky, block).endVertex();
      buf.pos(right, bottom, z).color(r, g, b, 1.0f).tex(u1, v1).lightmap(sky, block).endVertex();
      buf.pos(left, bottom, z).color(r, g, b, 1.0f).tex(u0, v1).lightmap(sky, block).endVertex();

      penX += gl.advance * s;
    }

    tess.draw();
  }
}
