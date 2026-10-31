package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
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
 * Draws what an exterior marquee board shows, once across the whole board, fullbright, over the
 * dark screens its cells' baked models draw (so past this renderer's range the board is still
 * there, its screens off). Two pictures, shown in turn eight seconds each or one alone: the
 * <b>text</b>, three centred lines (the first biggest) between two thin rules on the background
 * colour, and the <b>video</b>, the centre-hung scoreboard's idle video cropped to the board's
 * shape from its middle. A double-sided board shows the same on its back.
 *
 * <p>Per face and frame: one or two untextured quads, the video frame (a sheet bound on its own,
 * its frame picked from the time, so no animation optimiser can stop it) or one display list
 * holding every glyph of the text (one {@code POSITION_TEX_COLOR} draw, compiled when the words,
 * their colour or the board's size change). Following "Display lists: one texture, no cached
 * state", the list holds geometry only. Global, since a board reaches past its controller's chunk
 * section; its range is the tile entity's.</p>
 *
 * <p>The screen's depth in its cell is {@code gen_venues_exterior.py}'s ({@code MARQUEE_*}):
 * change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityMarqueeRenderer extends TileEntitySpecialRenderer<TileEntityMarquee> {

  private static final CsmDisplayListCache LISTS = new CsmDisplayListCache("venue_marquee");

  /** The idle video: 32 frames of 128 x 64 on a 512 sheet, four across. */
  private static final int VIDEO_FRAMES = 32;
  private static final long VIDEO_FRAME_MS = 250;

  /** The screen's front: a single-sided cabinet's, a double-sided one's two, pixels in z. */
  private static final float SINGLE_FACE_Z = 9f;
  private static final float DOUBLE_FRONT_Z = 4f;
  private static final float DOUBLE_BACK_Z = 12f;
  /** In front of the baked screen, pixels; then the layers in front of each other. */
  private static final float LIFT = 0.45f;
  private static final float VIDEO_Z = 0.05f;
  private static final float TEXT_Z = 0.15f;
  /** The frame's lip round the board, pixels: the picture stops inside it. */
  private static final float INSET = 1f;

  /** Each line's share of the text's height, and the gap between lines. */
  private static final float[] WEIGHT = {0.42f, 0.32f, 0.2f};
  private static final float GAP = 0.06f;

  static void release(BlockPos pos) {
    LISTS.invalidate(pos);
  }

  @Override
  public boolean isGlobalRenderer(TileEntityMarquee te) {
    return true;
  }

  @Override
  public void render(TileEntityMarquee te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueMarquee
        || te.state() == null) {
      return;
    }
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    int cols = te.width();
    int rows = te.height();
    float w = cols * 16f - 2 * INSET;
    float h = rows * 16f - 2 * INSET;
    boolean text = te.showsText(te.getWorld().getTotalWorldTime());
    long ms = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks);
    int frame = (int) ((ms / VIDEO_FRAME_MS) % VIDEO_FRAMES);
    boolean both = te.isDoubleSided();

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.getFacing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.color(1f, 1f, 1f, 1f);

    // the front: its top-left as the reader sees it is the far top corner, x to the reader's
    // right (the model's -x), y down, pixels
    GlStateManager.pushMatrix();
    float frontZ = both ? DOUBLE_FRONT_Z : SINGLE_FACE_Z;
    GlStateManager.translate(0.5, rows, (frontZ - 8f - LIFT) / 16.0);
    GlStateManager.rotate(180F, 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);
    GlStateManager.translate(INSET, INSET, 0);
    face(te, text, frame, w, h);
    GlStateManager.popMatrix();
    if (both) {
      // the back, read from behind: its top-left is the controller's end
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.5 - cols, rows, (DOUBLE_BACK_Z - 8f + LIFT) / 16.0);
      GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);
      GlStateManager.translate(INSET, INSET, 0);
      face(te, text, frame, w, h);
      GlStateManager.popMatrix();
    }

    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /** One face, w x h pixels from its top-left. */
  private void face(TileEntityMarquee te, boolean text, int frame, float w, float h) {
    GlStateManager.disableTexture2D();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    if (text) {
      int bg = VenueColours.rgb(te.getBackground());
      quad(buf, 0, 0, w, h, bg, 0);
      int rule = mix(VenueColours.rgb(te.getForeground()), bg, 0.55f);
      float th = Math.max(0.5f, 0.022f * h);
      quad(buf, 0.05f * w, 0.06f * h, 0.95f * w, 0.06f * h + th, rule, VIDEO_Z);
      quad(buf, 0.05f * w, 0.94f * h - th, 0.95f * w, 0.94f * h, rule, VIDEO_Z);
    } else {
      quad(buf, 0, 0, w, h, 0x05070A, 0);
    }
    tess.draw();
    GlStateManager.enableTexture2D();
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    if (!text) {
      video(frame, w, h);
      return;
    }
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    CsmFontRenderer fr = CsmFontRenderer.electronicSign();
    fr.bindAtlas();
    GlStateManager.pushMatrix();
    GlStateManager.translate(0, 0, TEXT_Z);
    long key = (long) Math.round(w) << 16 | Math.round(h);
    int list = LISTS.get(te.getPos(), key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(te.getPos(), key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        glyphs(te, fr, w, h);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      glyphs(te, fr, w, h);
    }
    GlStateManager.popMatrix();
    GlStateManager.resetColor();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();
  }

  /** The lines with text, stacked and centred between the rules, each shrunk to fit across. */
  private static void glyphs(TileEntityMarquee te, CsmFontRenderer fr, float w, float h) {
    float total = 0f;
    int count = 0;
    for (int i = 0; i < 3; i++) {
      if (!te.getLine(i).isEmpty()) {
        total += WEIGHT[i];
        count++;
      }
    }
    if (count == 0) {
      return;
    }
    float room = 0.76f * h;
    float gap = GAP * h;
    float k = (room - gap * (count - 1)) / total;
    float y = (h - room) / 2f;
    int ink = VenueColours.rgb(te.getForeground());
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int i = 0; i < 3; i++) {
      String line = te.getLine(i);
      if (line.isEmpty()) {
        continue;
      }
      float lh = WEIGHT[i] * k;
      TileEntityScoreboardRenderer.line(buf, fr, line.toUpperCase(Locale.ROOT), w / 2f, y, lh,
          0.92f * w, ink, 0);
      y += lh + gap;
    }
    tess.draw();
  }

  /** The video's frame over the whole face, cropped to its shape from the middle. */
  private void video(int frame, float w, float h) {
    bindTexture(TileEntityScoreboardRenderer.VIDEO);
    float aspect = w / h;
    float u0 = (frame % 4) * 0.25f;
    float v0 = (frame / 4) * 0.125f;
    float du = 0.25f;
    float dv = 0.125f;
    if (aspect < 2f) {
      float keep = du * aspect / 2f;
      u0 += (du - keep) / 2f;
      du = keep;
    } else {
      float keep = dv * 2f / aspect;
      v0 += (dv - keep) / 2f;
      dv = keep;
    }
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(0, h, VIDEO_Z).tex(u0, v0 + dv).endVertex();
    buf.pos(w, h, VIDEO_Z).tex(u0 + du, v0 + dv).endVertex();
    buf.pos(w, 0, VIDEO_Z).tex(u0 + du, v0).endVertex();
    buf.pos(0, 0, VIDEO_Z).tex(u0, v0).endVertex();
    tess.draw();
  }

  private static void quad(BufferBuilder buf, float x0, float y0, float x1, float y1, int rgb,
      float z) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    buf.pos(x0, y1, z).color(r, g, b, 255).endVertex();
    buf.pos(x1, y1, z).color(r, g, b, 255).endVertex();
    buf.pos(x1, y0, z).color(r, g, b, 255).endVertex();
    buf.pos(x0, y0, z).color(r, g, b, 255).endVertex();
  }

  /** {@code a} mixed towards {@code b} by {@code t}. */
  private static int mix(int a, int b, float t) {
    int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
    int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
    int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
    return r << 16 | g << 8 | bl;
  }
}
