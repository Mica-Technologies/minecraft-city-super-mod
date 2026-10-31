package com.micatechnologies.minecraft.csm.venues.courts;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
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
 * Draws a courtside scoreboard's or a lap counter's face, lit, on its baked casing: for the
 * scoreboard each player's colour bar, serve lamp, name, games in each set (the current set
 * last, in amber) and points; for the lap counter the laps to go on two flip cards. The panels
 * are a few coloured quads a frame, the words one display list a board compiled when they change.
 *
 * <p>The faces' rectangles are {@code gen_venues_courts.py}'s ({@code SB_FACE}, {@code LAP_FACE}):
 * change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityCourtBoardRenderer extends TileEntitySpecialRenderer<TileEntityCourtBoard> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_court_board");

  /** The faces in the master's frame, pixels: x0, x1, y0, y1, z of the front. */
  private static final float[] SB_FACE = {-32f, 16f, 17f, 39f, 6f};
  private static final float[] LAP_FACE = {1f, 15f, 21f, 31f, 6f};
  private static final float LIFT = 0.06f;
  private static final float TEXT_Z = 0.02f;

  private static final int PANEL = 0x0C0E0C;
  private static final int LAMP_OFF = 0x2C302E;
  private static final int AMBER = 0xFFC830;
  private static final int YELLOW = 0xFAE63C;
  private static final int WHITE = 0xF4F4EE;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityCourtBoard te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueCourts
        || te.block() == null) {
      return;
    }
    boolean laps = te.lapCounter();
    float[] face = laps ? LAP_FACE : SB_FACE;
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
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.disableTexture2D();
    if (laps) {
      lapPanels();
    } else {
      panels(te);
    }
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
        glyphs(te, fr, laps);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, laps);
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

  /** The scoreboard's columns: u of each set's middle, and of the points. */
  private static float setU(int s) {
    return 27.5f + s * 3.4f;
  }

  private static final float PTS_U = 44.5f;

  private static void panels(TileEntityCourtBoard te) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    int shown = te.setsShown();
    for (int p = 1; p <= 2; p++) {
      float v = p == 1 ? 6f : 14.5f;
      quad(buf, 1f, v, 3f, v + 7f, VenueColours.rgb(te.colour(p)));
      quad(buf, 4f, v + 2.5f, 6f, v + 4.5f, te.getServe() == p ? AMBER : LAMP_OFF);
      for (int s = 0; s < shown; s++) {
        quad(buf, setU(s) - 1.5f, v, setU(s) + 1.5f, v + 7f, PANEL);
      }
      quad(buf, PTS_U - 2.8f, v, PTS_U + 2.8f, v + 7f, PANEL);
    }
    tess.draw();
  }

  private static void lapPanels() {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    quad(buf, 1f, 3f, 6.8f, 9.6f, PANEL);
    quad(buf, 7.2f, 3f, 13f, 9.6f, PANEL);
    tess.draw();
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

  private static void glyphs(TileEntityCourtBoard te, CsmFontRenderer fr, boolean laps) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    if (laps) {
      text(buf, fr, "LAPS TO GO", 7f, 0.4f, 2.2f, 13f, WHITE, 0);
      String n = Integer.toString(te.getLaps());
      if (n.length() < 2) {
        n = " " + n;
      }
      text(buf, fr, n.substring(0, 1).trim(), 3.9f, 3.6f, 5.6f, 5f,
          te.getLaps() == 1 ? AMBER : WHITE, 0);
      text(buf, fr, n.substring(1), 10.1f, 3.6f, 5.6f, 5f, te.getLaps() == 1 ? AMBER : WHITE, 0);
    } else {
      int shown = te.setsShown();
      text(buf, fr, "SETS", setU(0) - 1.5f, 1.4f, 3f, 16f, WHITE, -1);
      text(buf, fr, "PTS", PTS_U, 1.4f, 3f, 6f, WHITE, 0);
      for (int p = 1; p <= 2; p++) {
        float v = p == 1 ? 6f : 14.5f;
        text(buf, fr, te.name(p).toUpperCase(java.util.Locale.ROOT), 7f, v + 1.2f, 4.6f, 17.5f,
            WHITE, -1);
        for (int s = 0; s < shown; s++) {
          int g = te.games(p, s);
          if (g >= 0) {
            text(buf, fr, Integer.toString(g), setU(s), v + 1f, 5f, 2.8f,
                s == shown - 1 ? AMBER : WHITE, 0);
          }
        }
        text(buf, fr, te.points(p).toUpperCase(java.util.Locale.ROOT), PTS_U, v + 1f, 5f, 5f,
            YELLOW, 0);
      }
    }
    tess.draw();
  }

  private static void text(BufferBuilder buf, CsmFontRenderer fr, String s, float u, float v,
      float height, float maxW, int rgb, int align) {
    TileEntityScoreboardRenderer.line(buf, fr, s, u, v, height, maxW, rgb, align);
  }
}
