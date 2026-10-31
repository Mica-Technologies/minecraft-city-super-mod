package com.micatechnologies.minecraft.csm.venues.field;

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
 * Draws what a football or soccer scoreboard shows on its baked face, lit: each team's name on a
 * bar of its colour with its score under it in amber, the clock in a panel in the middle with the
 * quarter or half under it; for football the time-out lamps under each score, a possession lamp
 * beside the team with the ball and the DOWN, TO GO and BALL ON strip along the foot; for soccer
 * the added time along the foot. The bars and lamps are a few coloured quads a frame, the words
 * one display list a board compiled when they change, the clock a few glyphs a frame (it is
 * worked out from the world's time).
 *
 * <p>The face's rectangle is {@code gen_venues_football.py}'s ({@code FB_BOARD}), the ballpark
 * line score's: change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityFieldScoreboardRenderer
    extends TileEntitySpecialRenderer<TileEntityFieldScoreboard> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_field_score");

  /** The face in the master's frame, pixels: x0, x1, y0, y1, z of the front. */
  private static final float[] FACE = {-16f, 176f, 48f, 128f, 2f};
  private static final float LIFT = 0.08f;
  private static final float TEXT_Z = 0.03f;

  private static final int PANEL = 0x0C0E0C;
  private static final int LAMP_OFF = 0x2C302E;
  private static final int AMBER = 0xFFC830;
  private static final int YELLOW = 0xFAE63C;
  private static final int WHITE = 0xF4F4EE;

  private static final float HOME_U = 33f;
  private static final float AWAY_U = 159f;
  private static final float BAR_W = 58f;
  private static final float MID_U = 96f;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityFieldScoreboard te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueField
        || te.block() == null) {
      return;
    }
    boolean soccer = te.soccer();
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.facing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    GlStateManager.translate(FACE[1] / 16.0, FACE[3] / 16.0, (FACE[4] - LIFT) / 16.0);
    GlStateManager.rotate(180F, 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.disableTexture2D();
    panels(te, soccer);
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
        glyphs(te, fr, soccer);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, soccer);
    }
    // the clock changes every tenth: drawn each frame
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    TileEntityScoreboardRenderer.line(buf, fr,
        te.clockText(te.getWorld().getTotalWorldTime(), partialTicks), MID_U, 6f, 17f, 44f,
        YELLOW, 0);
    tess.draw();
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  private static void panels(TileEntityFieldScoreboard te, boolean soccer) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    for (int t = 0; t < 2; t++) {
      boolean home = t == 0;
      float u = home ? HOME_U : AWAY_U;
      quad(buf, u - BAR_W / 2, 3f, u + BAR_W / 2, 17f, VenueColours.rgb(te.colour(home)));
      quad(buf, u - 16f, 20f, u + 16f, 44f, PANEL);
      if (!soccer) {
        for (int i = 0; i < 3; i++) {
          float a = u - 13f + i * 10f;
          quad(buf, a, 47f, a + 6f, 51f, i < te.timeouts(home) ? AMBER : LAMP_OFF);
        }
        float lamp = home ? HOME_U + BAR_W / 2 + 2f : AWAY_U - BAR_W / 2 - 6f;
        quad(buf, lamp, 7f, lamp + 4f, 13f,
            te.getPossession() == (home ? 1 : 2) ? AMBER : LAMP_OFF);
      }
    }
    quad(buf, MID_U - 24f, 3f, MID_U + 24f, 27f, PANEL);
    quad(buf, MID_U - 10f, 37f, MID_U + 10f, 50f, PANEL);
    if (soccer) {
      quad(buf, MID_U - 14f, 62f, MID_U + 14f, 77f, PANEL);
    } else {
      for (float u : new float[]{30f, MID_U, 162f}) {
        quad(buf, u - 18f, 64f, u + 18f, 77f, PANEL);
      }
    }
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

  private static void glyphs(TileEntityFieldScoreboard te, CsmFontRenderer fr, boolean soccer) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int t = 0; t < 2; t++) {
      boolean home = t == 0;
      float u = home ? HOME_U : AWAY_U;
      int bar = te.colour(home);
      int ink = VenueColours.isLight(bar) ? 0x141414 : WHITE;
      String name = (home ? te.getHome() : te.getAway()).toUpperCase(java.util.Locale.ROOT);
      text(buf, fr, name, u, 5f, 10f, BAR_W - 4f, ink);
      text(buf, fr, Integer.toString(te.score(home)), u, 22f, 20f, 30f, AMBER);
    }
    text(buf, fr, soccer ? "HALF" : "QTR", MID_U, 30f, 6f, 30f, WHITE);
    text(buf, fr, te.getPeriod().toUpperCase(java.util.Locale.ROOT), MID_U, 38.5f, 10f, 18f,
        AMBER);
    if (soccer) {
      text(buf, fr, "ADDED TIME", MID_U, 55f, 6f, 60f, WHITE);
      if (te.getAdded() > 0) {
        text(buf, fr, "+" + te.getAdded(), MID_U, 64f, 11f, 26f, AMBER);
      }
    } else {
      String[] labels = {"DOWN", "TO GO", "BALL ON"};
      String[] values = {te.getDown() > 0 ? Integer.toString(te.getDown()) : "",
          te.getDistance(), te.getBallOn()};
      float[] us = {30f, MID_U, 162f};
      for (int i = 0; i < 3; i++) {
        text(buf, fr, labels[i], us[i], 56f, 6f, 36f, WHITE);
        text(buf, fr, values[i].toUpperCase(java.util.Locale.ROOT), us[i], 65f, 11f, 34f,
            AMBER);
      }
    }
    tess.draw();
  }

  private static void text(BufferBuilder buf, CsmFontRenderer fr, String s, float u, float v,
      float height, float maxW, int rgb) {
    TileEntityScoreboardRenderer.line(buf, fr, s, u, v, height, maxW, rgb, 0);
  }
}
