package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import java.util.List;
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
 * Draws what a ballpark scoreboard shows on its baked face, lit: on the line score, the inning
 * numbers, each team's name on a bar of its colour with its runs inning by inning in squares
 * and R H E in yellow, a lamp by the batting team, and along the foot the ball, strike and out
 * lamps and the last pitch's speed; on the count board, the lamps and the speed. The squares and
 * lamps are a few coloured quads a frame; every glyph is in one display list a board, compiled
 * when the board's words change.
 *
 * <p>The face's rectangle is {@code gen_venues_ballpark.py}'s ({@code SB_LARGE},
 * {@code SB_SMALL}): change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityBallparkScoreboardRenderer
    extends TileEntitySpecialRenderer<TileEntityBallparkScoreboard> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_line_score");

  /** The faces in the master's frame, pixels: x0, x1, y0, y1, z of the front. */
  private static final float[] LARGE = {-16f, 176f, 48f, 128f, 2f};
  private static final float[] SMALL = {0f, 48f, 0f, 32f, 10f};
  private static final float LIFT = 0.08f;
  private static final float TEXT_Z = 0.03f;

  private static final int SQUARE = 0x101412;
  private static final int LAMP_OFF = 0x2C302E;
  private static final int BALL = 0x40E060;
  private static final int STRIKE = 0xF04030;
  private static final int OUT = 0xF04030;
  private static final int AMBER = 0xFFC830;
  private static final int WHITE = 0xF4F4EE;

  // the line score's columns and rows, pixels across and down the face
  private static final float NAME_U0 = 10f;
  private static final float NAME_U1 = 53f;
  private static final float INN_U0 = 56f;
  private static final float INN_W = 11f;
  private static final float RHE_U0 = 158f;
  private static final float RHE_W = 11f;
  private static final float HEAD_V = 3f;
  private static final float ROW_AWAY = 15f;
  private static final float ROW_HOME = 34f;
  private static final float ROW_H = 16f;
  private static final float STRIP_V = 57f;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityBallparkScoreboard te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueBallpark
        || te.block() == null) {
      return;
    }
    boolean small = te.small();
    float[] f = small ? SMALL : LARGE;
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.facing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    // the face: u from the reader's left (the face's east edge at facing north), v down
    GlStateManager.translate(f[1] / 16.0, f[3] / 16.0, (f[4] - LIFT) / 16.0);
    GlStateManager.rotate(180F, 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.disableTexture2D();
    panels(te, small);
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
        glyphs(te, fr, small);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, small);
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

  // --- the squares and lamps -----------------------------------------------------------------

  private static void panels(TileEntityBallparkScoreboard te, boolean small) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    if (small) {
      lamps(buf, 10f, 3f, 6f, 8f, 3, te.getBalls(), BALL);
      lamps(buf, 10f, 13f, 6f, 8f, 2, te.getStrikes(), STRIKE);
      lamps(buf, 10f, 23f, 6f, 8f, 2, te.getOuts(), OUT);
      quad(buf, 31f, 3f, 46f, 20f, SQUARE);
    } else {
      for (int t = 0; t < 2; t++) {
        boolean home = t == 1;
        float v = home ? ROW_HOME : ROW_AWAY;
        quad(buf, NAME_U0, v, NAME_U1, v + ROW_H, VenueColours.rgb(te.colour(home)));
        for (int i = 0; i < 9; i++) {
          float u = INN_U0 + i * INN_W;
          quad(buf, u + 0.5f, v, u + INN_W - 0.5f, v + ROW_H, SQUARE);
        }
        for (int i = 0; i < 3; i++) {
          float u = RHE_U0 + i * RHE_W;
          quad(buf, u + 0.5f, v, u + RHE_W - 0.5f, v + ROW_H, SQUARE);
        }
        quad(buf, 3f, v + 5f, 8f, v + 11f, te.homeBatting() == home ? AMBER : LAMP_OFF);
      }
      lamps(buf, 34f, STRIP_V + 4f, 8f, 10f, 3, te.getBalls(), BALL);
      lamps(buf, 98f, STRIP_V + 4f, 8f, 10f, 2, te.getStrikes(), STRIKE);
      lamps(buf, 140f, STRIP_V + 4f, 8f, 10f, 2, te.getOuts(), OUT);
      quad(buf, 162f, STRIP_V, 189f, STRIP_V + 15f, SQUARE);
    }
    tess.draw();
  }

  private static void lamps(BufferBuilder buf, float u, float v, float size, float pitch, int n,
      int lit, int colour) {
    for (int i = 0; i < n; i++) {
      float a = u + i * pitch;
      quad(buf, a, v, a + size, v + size, i < lit ? colour : LAMP_OFF);
    }
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

  // --- the words -----------------------------------------------------------------------------

  private static void glyphs(TileEntityBallparkScoreboard te, CsmFontRenderer fr, boolean small) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    if (small) {
      text(buf, fr, "B", 2.5f, 3f, 8f, 6f, WHITE, -1);
      text(buf, fr, "S", 2.5f, 13f, 8f, 6f, WHITE, -1);
      text(buf, fr, "O", 2.5f, 23f, 8f, 6f, WHITE, -1);
      if (te.getMph() > 0) {
        text(buf, fr, Integer.toString(te.getMph()), 38.5f, 5f, 12f, 14f, AMBER, 0);
      }
      text(buf, fr, "MPH", 38.5f, 22f, 6f, 14f, WHITE, 0);
    } else {
      List<String> away = te.innings(false);
      List<String> home = te.innings(true);
      int played = Math.max(away.size(), home.size());
      int first = Math.max(0, played - 9);
      for (int i = 0; i < 9; i++) {
        text(buf, fr, Integer.toString(first + i + 1), INN_U0 + (i + 0.5f) * INN_W, HEAD_V, 8f,
            INN_W - 1f, WHITE, 0);
      }
      String[] rhe = {"R", "H", "E"};
      for (int i = 0; i < 3; i++) {
        text(buf, fr, rhe[i], RHE_U0 + (i + 0.5f) * RHE_W, HEAD_V, 8f, RHE_W - 1f, AMBER, 0);
      }
      for (int t = 0; t < 2; t++) {
        boolean isHome = t == 1;
        float v = isHome ? ROW_HOME : ROW_AWAY;
        int bar = te.colour(isHome);
        int ink = VenueColours.isLight(bar) ? 0x141414 : WHITE;
        String name = (isHome ? te.getHome() : te.getAway()).toUpperCase(java.util.Locale.ROOT);
        text(buf, fr, name, (NAME_U0 + NAME_U1) / 2f, v + 3f, 10f, NAME_U1 - NAME_U0 - 3f, ink,
            0);
        List<String> inn = isHome ? home : away;
        for (int i = 0; i < 9 && first + i < inn.size(); i++) {
          text(buf, fr, inn.get(first + i), INN_U0 + (i + 0.5f) * INN_W, v + 2f, 12f,
              INN_W - 2f, WHITE, 0);
        }
        int[] vals = {te.runs(isHome), te.hits(isHome), te.errors(isHome)};
        for (int i = 0; i < 3; i++) {
          text(buf, fr, Integer.toString(vals[i]), RHE_U0 + (i + 0.5f) * RHE_W, v + 2f, 12f,
              RHE_W - 2f, AMBER, 0);
        }
      }
      text(buf, fr, "BALL", 10f, STRIP_V + 5f, 8f, 22f, WHITE, -1);
      text(buf, fr, "STRIKE", 66f, STRIP_V + 5f, 8f, 30f, WHITE, -1);
      text(buf, fr, "OUT", 120f, STRIP_V + 5f, 8f, 18f, WHITE, -1);
      if (te.getMph() > 0) {
        text(buf, fr, Integer.toString(te.getMph()), 175.5f, STRIP_V + 2f, 11f, 25f, AMBER, 0);
      }
      text(buf, fr, "MPH", 175.5f, STRIP_V + 16f, 5f, 25f, WHITE, 0);
    }
    tess.draw();
  }

  private static void text(BufferBuilder buf, CsmFontRenderer fr, String s, float u, float v,
      float height, float maxW, int rgb, int align) {
    TileEntityScoreboardRenderer.line(buf, fr, s, u, v, height, maxW, rgb, align);
  }
}
