package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
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
 * Draws a ribbon board text piece's text once across its run: a band in the background colour
 * over the LED faces, and the text on it, sliding left at the piece's speed and coming round
 * again three blocks after its end (or still and centred at speed 0).
 *
 * <p>The text is one display list per piece holding its glyphs (one {@code POSITION_TEX_COLOR}
 * draw, compiled when the text or its colour changes), replayed at each place a copy falls; two
 * clip planes at the run's ends cut the copies off there, so the glyphs need no clipping of
 * their own. A run is cheap however long: a band quad and a few list calls. Global, since a run
 * can leave its piece's chunk section; its render range is the tile entity's.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityRibbonTextRenderer extends TileEntitySpecialRenderer<TileEntityRibbonText> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_ribbon_text");

  /** The LED face: its front, and its rows, in pixels of the model's cell. */
  private static final float FACE_Z = 13.5f;
  private static final float LED_TOP = 14f;
  private static final float LED_H = 12f;
  private static final float LIFT = 0.04f;
  private static final float TEXT_H = 9f;
  /** The gap between copies of the text, pixels. */
  private static final float GAP = 48f;

  private static final DoubleBuffer PLANE = ByteBuffer.allocateDirect(4 * 8)
      .order(ByteOrder.nativeOrder()).asDoubleBuffer();

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public boolean isGlobalRenderer(TileEntityRibbonText te) {
    return true;
  }

  @Override
  public void render(TileEntityRibbonText te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueRibbonText) {
      return;
    }
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    CsmFontRenderer fr = CsmFontRenderer.electronicSign();
    float length = te.runCells() * 16f;
    float scale = TEXT_H / fr.FONT_HEIGHT;
    float textW = fr.getStringWidth(te.getText()) * scale;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.getFacing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);
    // the LED face's left end as seen from the floor (the cell's east side, drawn facing north),
    // x to the right as seen, y down, pixels
    GlStateManager.translate(0.5, LED_TOP / 16.0, (FACE_Z - 8 - LIFT) / 16.0);
    GlStateManager.rotate(180F, 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();

    GlStateManager.disableTexture2D();
    int bg = VenueColours.rgb(te.getBackground());
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    int r = (bg >> 16) & 0xFF;
    int g = (bg >> 8) & 0xFF;
    int b = bg & 0xFF;
    buf.pos(0, LED_H, 0).color(r, g, b, 255).endVertex();
    buf.pos(length, LED_H, 0).color(r, g, b, 255).endVertex();
    buf.pos(length, 0, 0).color(r, g, b, 255).endVertex();
    buf.pos(0, 0, 0).color(r, g, b, 255).endVertex();
    tess.draw();
    GlStateManager.enableTexture2D();
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);

    if (!te.getText().isEmpty()) {
      clip(0, 1, 0);
      clip(1, -1, length);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GlStateManager.depthMask(false);
      fr.bindAtlas();
      float top = (LED_H - TEXT_H) / 2f;
      float speed = TileEntityRibbonText.SPEEDS[te.getSpeed()];
      if (speed <= 0f) {
        at(te, fr, (length - textW) / 2f, top, scale);
      } else {
        float period = textW + GAP;
        long ms = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks);
        float offset = (float) ((ms * speed / 1000.0) % period);
        for (float at = length - offset; at > -period; at -= period) {
          if (at < length) {
            at(te, fr, at, top, scale);
          }
        }
      }
      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GL11.glDisable(GL11.GL_CLIP_PLANE0);
      GL11.glDisable(GL11.GL_CLIP_PLANE1);
      GlStateManager.resetColor();
      GlStateManager.color(1f, 1f, 1f, 1f);
    }

    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /** A clip plane keeping {@code sign * x + d >= 0}, in the current frame. */
  private static void clip(int plane, double sign, double d) {
    PLANE.clear();
    PLANE.put(sign).put(0).put(0).put(sign > 0 ? 0 : d);
    PLANE.flip();
    GL11.glClipPlane(GL11.GL_CLIP_PLANE0 + plane, PLANE);
    GL11.glEnable(GL11.GL_CLIP_PLANE0 + plane);
  }

  /** One copy of the text with its left edge at {@code left}. */
  private static void at(TileEntityRibbonText te, CsmFontRenderer fr, float left, float top,
      float scale) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(left, top, 0.1f);
    int list = LISTS.get(te.getPos(), 0L);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(te.getPos(), 0L);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        glyphs(te, fr, scale);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, scale);
    }
    GlStateManager.popMatrix();
  }

  private static void glyphs(TileEntityRibbonText te, CsmFontRenderer fr, float scale) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    fr.addString(buf, te.getText(), 0, 0, 0, 0, scale, VenueColours.rgb(te.getForeground()));
    tess.draw();
  }
}
