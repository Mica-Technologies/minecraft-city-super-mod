package com.micatechnologies.minecraft.csm.venues.bowling;

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
 * Draws a pair of lanes' scores on the two baked screens of a scoring monitor, lit: each screen a
 * header in the screens' colour with the lane's number and the frame numbers, then a row a bowler
 * (name, ten frames with their marks in the small boxes and the running total under them, the
 * total), the bowler up picked out in amber with the frame they are in. The boxes are a few
 * coloured quads a frame, the words one display list a monitor compiled when they change.
 *
 * <p>The screens' rectangle is {@code gen_venues_bowling.py}'s ({@code MON_FACE},
 * {@code MON_SCREENS}): change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityBowlingMonitorRenderer
    extends TileEntitySpecialRenderer<TileEntityBowlingMonitor> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_bowling");

  /** The face in the master's frame, pixels: x0, x1, y0, y1, z of the front. */
  private static final float[] FACE = {0f, 64f, 6f, 28f, 6f};
  /** Each screen's left edge, along the face from the reader's left; its size. */
  private static final float[] SCREEN_U = {1f, 33f};
  private static final float SCREEN_W = 30f;
  private static final float SCREEN_H = 22f;
  private static final float LIFT = 0.08f;
  private static final float TEXT_Z = 0.03f;

  private static final float HEAD_H = 3.0f;
  private static final float ROW_V = 3.8f;
  private static final float ROW_PITCH = 6.0f;
  private static final float ROW_H = 5.5f;
  private static final float NAME_U = 0.6f;
  private static final float NAME_W = 5.8f;
  private static final float FRAME_U = 6.7f;
  private static final float FRAME_W = 1.9f;
  private static final float TENTH_W = 2.4f;
  private static final float TOTAL_U = 26.4f;
  private static final float TOTAL_W = 3.0f;
  private static final float MARK_H = 2.4f;

  private static final int CELL = 0xE8ECF2;
  private static final int CELL_UP = 0xFFF0B0;
  private static final int BOX = 0xC4CCD8;
  private static final int NAME_BG = 0x1C2436;
  private static final int AMBER = 0xFFB428;
  private static final int TOTAL_BG = 0xFFD84A;
  private static final int INK = 0x10141C;
  private static final int RED = 0xC8282C;
  private static final int WHITE = 0xF4F4EE;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityBowlingMonitor te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueBowling
        || te.block() == null) {
      return;
    }
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    BowlingScore[] scores = te.scores();
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
    panels(te, scores);
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
        glyphs(te, scores, fr);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, scores, fr);
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

  /** A frame's left edge and width within a screen (frame 9 is the wider tenth). */
  private static float frameU(int f) {
    return FRAME_U + f * FRAME_W;
  }

  private static float frameW(int f) {
    return f == 9 ? TENTH_W : FRAME_W - 0.1f;
  }

  private static void panels(TileEntityBowlingMonitor te, BowlingScore[] scores) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    int theme = VenueColours.rgb(te.getColour());
    int bg = darken(theme, 0.22f);
    for (int s = 0; s < 2; s++) {
      float u0 = SCREEN_U[s];
      quad(buf, u0 + 0.5f, 0.5f, u0 + SCREEN_W - 0.5f, SCREEN_H - 0.5f, bg, 0);
      quad(buf, u0 + 0.5f, 0.5f, u0 + SCREEN_W - 0.5f, HEAD_H, theme, 1);
      int up = te.up(s);
      for (int r = 0; r < TileEntityBowlingMonitor.PER_LANE; r++) {
        BowlingScore b = scores[s * TileEntityBowlingMonitor.PER_LANE + r];
        if (b.isEmpty()) {
          continue;
        }
        boolean isUp = up == r + 1;
        float v = ROW_V + r * ROW_PITCH;
        quad(buf, u0 + NAME_U, v, u0 + NAME_U + NAME_W, v + ROW_H, isUp ? AMBER : NAME_BG, 1);
        int current = isUp ? b.currentFrame() : -1;
        for (int f = 0; f < 10; f++) {
          float fu = u0 + frameU(f);
          float fw = frameW(f);
          quad(buf, fu, v, fu + fw, v + ROW_H, f == current ? CELL_UP : CELL, 1);
          if (f < 9) {
            quad(buf, fu + fw / 2f, v + 0.15f, fu + fw - 0.15f, v + MARK_H, BOX, 2);
          } else {
            for (int k = 1; k < 3; k++) {
              float bu = fu + k * fw / 3f;
              quad(buf, bu + 0.05f, v + 0.15f, bu + fw / 3f - 0.1f, v + MARK_H, BOX, 2);
            }
          }
        }
        quad(buf, u0 + TOTAL_U, v, u0 + TOTAL_U + TOTAL_W, v + ROW_H, TOTAL_BG, 1);
      }
    }
    tess.draw();
  }

  private static void glyphs(TileEntityBowlingMonitor te, BowlingScore[] scores,
      CsmFontRenderer fr) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    int theme = VenueColours.rgb(te.getColour());
    int headInk = VenueColours.isLight(te.getColour()) ? INK : WHITE;
    for (int s = 0; s < 2; s++) {
      float u0 = SCREEN_U[s];
      text(buf, fr, "LANE " + (te.getLane() + s), u0 + 0.9f, 0.85f, 1.9f, 5.6f, headInk, -1);
      for (int f = 0; f < 10; f++) {
        text(buf, fr, Integer.toString(f + 1), u0 + frameU(f) + frameW(f) / 2f, 1.1f, 1.4f,
            frameW(f), headInk, 0);
      }
      text(buf, fr, "TOTAL", u0 + TOTAL_U + TOTAL_W / 2f, 1.1f, 1.4f, TOTAL_W, headInk, 0);
      int up = te.up(s);
      for (int r = 0; r < TileEntityBowlingMonitor.PER_LANE; r++) {
        BowlingScore b = scores[s * TileEntityBowlingMonitor.PER_LANE + r];
        if (b.isEmpty()) {
          continue;
        }
        float v = ROW_V + r * ROW_PITCH;
        boolean isUp = up == r + 1;
        text(buf, fr, b.getName(), u0 + NAME_U + 0.4f, v + 1.4f, 2.6f, NAME_W - 0.8f,
            isUp ? INK : WHITE, -1);
        for (int f = 0; f < 10; f++) {
          float fu = u0 + frameU(f);
          float fw = frameW(f);
          String[] m = b.marks(f);
          int n = m.length;
          for (int k = 0; k < n; k++) {
            float cu = n == 2 ? fu + fw * (k == 0 ? 0.25f : 0.75f) : fu + fw * (k + 0.5f) / 3f;
            boolean red = "X".equals(m[k]) || "/".equals(m[k]);
            text(buf, fr, m[k], cu, v + 0.35f, 1.8f, fw / n, red ? RED : INK, 0);
          }
          int t = b.total(f);
          if (t >= 0) {
            text(buf, fr, Integer.toString(t), fu + fw / 2f, v + MARK_H + 0.4f, 2.3f, fw - 0.2f,
                INK, 0);
          }
        }
        text(buf, fr, Integer.toString(b.knownTotal()), u0 + TOTAL_U + TOTAL_W / 2f, v + 1.2f,
            3.0f, TOTAL_W - 0.3f, INK, 0);
      }
      if (allEmpty(scores, s)) {
        text(buf, fr, "OPEN LANE", u0 + SCREEN_W / 2f, 9.5f, 3.2f, SCREEN_W - 6f,
            brighten(theme), 0);
      }
    }
    tess.draw();
  }

  private static boolean allEmpty(BowlingScore[] scores, int side) {
    for (int r = 0; r < TileEntityBowlingMonitor.PER_LANE; r++) {
      if (!scores[side * TileEntityBowlingMonitor.PER_LANE + r].isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static int darken(int rgb, float k) {
    int r = (int) (((rgb >> 16) & 0xFF) * k);
    int g = (int) (((rgb >> 8) & 0xFF) * k);
    int b = (int) ((rgb & 0xFF) * k);
    return (r << 16) | (g << 8) | b;
  }

  private static int brighten(int rgb) {
    int r = Math.min(255, ((rgb >> 16) & 0xFF) + 90);
    int g = Math.min(255, ((rgb >> 8) & 0xFF) + 90);
    int b = Math.min(255, (rgb & 0xFF) + 90);
    return (r << 16) | (g << 8) | b;
  }

  /** A coloured rectangle, {@code layer} steps towards the reader so overlaps never fight. */
  private static void quad(BufferBuilder buf, float u0, float v0, float u1, float v1, int rgb,
      int layer) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    float z = layer * 0.01f;
    buf.pos(u0, v1, z).color(r, g, b, 255).endVertex();
    buf.pos(u1, v1, z).color(r, g, b, 255).endVertex();
    buf.pos(u1, v0, z).color(r, g, b, 255).endVertex();
    buf.pos(u0, v0, z).color(r, g, b, 255).endVertex();
  }

  private static void text(BufferBuilder buf, CsmFontRenderer fr, String s, float u, float v,
      float height, float maxW, int rgb, int align) {
    TileEntityScoreboardRenderer.line(buf, fr, s, u, v, height, maxW, rgb, align);
  }
}
