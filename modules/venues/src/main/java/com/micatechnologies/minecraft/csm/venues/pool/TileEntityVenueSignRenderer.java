package com.micatechnologies.minecraft.csm.venues.pool;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import java.util.Locale;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a {@link BlockVenueSign}'s words, lit, on its baked casing: a pool scoreboard's event,
 * heat and race clock over a row a lane (place, lane, swimmer, time; the winner's row in amber),
 * or a golf tee sign's hole, par and yards from each tee under a coloured square. The panels are a
 * few coloured quads a frame, the words one display list a sign compiled when they change.
 *
 * <p>The faces' rectangles are {@code gen_venues_pool.py}'s and {@code gen_venues_golf.py}'s
 * ({@code BOARD_FACE}, {@code HOLE_FACE}): change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityVenueSignRenderer extends TileEntitySpecialRenderer<TileEntityVenueSign> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_sign");

  /** The faces in the master's frame, pixels: x0, x1, y0, y1, z of the front. */
  static final float[] BOARD_FACE = {1f, 63f, 17f, 47f, 6f};
  static final float[] HOLE_FACE = {2.5f, 13.5f, 14.5f, 29.5f, 7f};
  private static final float LIFT = 0.06f;
  private static final float TEXT_Z = 0.02f;

  private static final int PANEL = 0x0C0E0C;
  private static final int AMBER = 0xFFC830;
  private static final int WHITE = 0xF4F4EE;
  private static final int[] TEES = {0x2050C0, 0xF0F0EC, 0xC82828};
  /** Rows on an eight-lane pool's board (lanes 1-8). */
  private static final int LANES = 8;
  /** Rows on a ten-lane pool's board (lanes 0-9), shown when more than eight swimmers are named. */
  private static final int LANES_TEN = 10;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityVenueSign te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueCourts) {
      return;
    }
    BlockVenueSign.Style style = te.style();
    if (style == null) {
      return;
    }
    boolean pool = style == BlockVenueSign.Style.POOL_BOARD;
    float[] face = pool ? BOARD_FACE : HOLE_FACE;
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.facing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.translate(face[1] / 16.0, face[3] / 16.0, (face[4] - LIFT) / 16.0);
    GlStateManager.rotate(180F, 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    if (pool) {
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    }
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.disableTexture2D();
    panels(te, pool, face[1] - face[0], face[3] - face[2]);
    GlStateManager.enableTexture2D();
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, TEXT_Z);
    int list = LISTS.get(te.getPos(), 0L);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(te.getPos(), 0L);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        glyphs(te, fr, pool, face[1] - face[0]);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, pool, face[1] - face[0]);
    }
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /** A pool board's row v, on a board of {@code rows} rows. */
  private static float rowV(int r, int rows) {
    return 8.6f + r * (rows > LANES ? 2.12f : 2.7f);
  }

  private static void panels(TileEntityVenueSign te, boolean pool, float w, float h) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    if (pool) {
      quad(buf, 0f, 0f, w, h, PANEL);
      quad(buf, 0f, 0f, w, 5.4f, VenueColours.rgb(te.colour()));
    } else {
      quad(buf, 0f, 0f, w, h, 0xECECE4);
      quad(buf, 0f, 0f, w, 8f, VenueColours.rgb(te.colour()));
      String[] yards = words(te.line(2));
      int n = Math.min(3, yards.length);
      for (int k = 0; k < n; k++) {
        float u = teeU(k, n, w);
        quad(buf, u - 0.95f, 9.85f, u + 0.95f, 11.35f, 0x30302E);
        quad(buf, u - 0.8f, 10.0f, u + 0.8f, 11.2f, TEES[k]);
      }
    }
    tess.draw();
  }

  /** A tee's column on the golf sign. */
  private static float teeU(int k, int n, float w) {
    return w * (k + 0.5f) / n;
  }

  private static void quad(BufferBuilder buf, float u0, float v0, float u1, float v1, int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    buf.pos(u0, v1, 0).color(r, g, b, 255).endVertex();
    buf.pos(u1, v1, 0).color(r, g, b, 255).endVertex();
    buf.pos(u1, v0, 0).color(r, g, b, 255).endVertex();
    buf.pos(u0, v0, 0).color(r, g, b, 255).endVertex();
  }

  static String[] words(String s) {
    String t = s == null ? "" : s.trim();
    return t.isEmpty() ? new String[0] : t.split("\\s+");
  }

  private static void glyphs(TileEntityVenueSign te, CsmFontRenderer fr, boolean pool,
      float w) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    if (pool) {
      int col = te.colour();
      int head = VenueColours.isLight(col) ? 0x18181A : WHITE;
      text(buf, fr, up(te.line(0)), 1.5f, 0.9f, 3.6f, 38f, head, -1);
      text(buf, fr, up(te.line(5)), w - 1.5f, 0.9f, 3.6f, 18f, AMBER, 1);
      text(buf, fr, up(te.line(1)), 1.5f, 5.9f, 1.9f, 30f, WHITE, -1);
      String[] names = te.line(2).split(",", -1);
      String[] times = words(te.line(3));
      String[] places = words(te.line(4));
      // A ten-lane pool numbers its lanes 0 to 9; eight named swimmers or fewer keep 1 to 8.
      int rows = names.length > LANES ? LANES_TEN : LANES;
      int first = rows > LANES ? 0 : 1;
      float tall = rows > LANES ? 1.75f : 2.1f;
      for (int r = 0; r < rows; r++) {
        float v = rowV(r, rows);
        String place = r < places.length ? places[r] : "";
        int rgb = "1".equals(place) ? AMBER : WHITE;
        text(buf, fr, place, 3f, v, tall, 4f, rgb, 0);
        text(buf, fr, Integer.toString(r + first), 7.5f, v, tall, 3f, AMBER, 0);
        String name = r < names.length ? names[r].trim() : "";
        text(buf, fr, up(name), 11f, v, tall, 34f, rgb, -1);
        text(buf, fr, r < times.length ? times[r] : "", w - 1.5f, v, tall, 14f, rgb, 1);
      }
    } else {
      text(buf, fr, "HOLE", w / 2f, 0.6f, 1.4f, w - 1f, WHITE, 0);
      text(buf, fr, up(te.line(0)), w / 2f, 2.2f, 5.2f, w - 1f, WHITE, 0);
      text(buf, fr, "PAR " + up(te.line(1)), w / 2f, 8.4f, 1.6f, w - 1f, 0x18181A, 0);
      String[] yards = words(te.line(2));
      int n = Math.min(3, yards.length);
      for (int k = 0; k < n; k++) {
        text(buf, fr, yards[k], teeU(k, n, w), 11.6f, 1.5f, w / n - 0.4f, 0x18181A, 0);
      }
      text(buf, fr, up(te.line(3)), w / 2f, 13.4f, 1.2f, w - 1f, 0x30302E, 0);
    }
    tess.draw();
  }

  private static String up(String s) {
    return s == null ? "" : s.toUpperCase(Locale.ROOT);
  }

  private static void text(BufferBuilder buf, CsmFontRenderer fr, String s, float u, float v,
      float height, float maxW, int rgb, int align) {
    TileEntityScoreboardRenderer.line(buf, fr, s, u, v, height, maxW, rgb, align);
  }
}
