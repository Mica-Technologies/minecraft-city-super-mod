package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws what a centre-hung scoreboard's four screens show, fullbright, over the dark screens its
 * baked model draws (so past this renderer's range the board is still there, its screens off).
 *
 * <p>Two layouts. <b>Score</b>: the home and away names on bars in the teams' colours, the
 * scores under them, the clock in a panel in the middle, the period under it, the shots at the
 * foot. <b>Video</b>: the idle video over the top four-fifths and a strip along the foot with
 * the score, period and clock. Each face is drawn in its own frame: x to the right as seen, y
 * down, in pixels from the screen's top-left (16 to a block).</p>
 *
 * <p>Per face and frame: the panels as untextured quads, the video frame (a sheet bound on its
 * own and its frame picked from the time, so no animation optimiser can stop it), one display
 * list holding every glyph of the static text (names, scores, shots, period and labels: one
 * {@code POSITION_TEX_COLOR} draw, compiled when the text changes, a list for the long faces and
 * one for the short), and the clock drawn as it is, since it changes ten times a second under a
 * minute. Following "Display lists: one texture, no cached state", the lists hold geometry only:
 * the font atlas, blending and the depth mask are set outside them.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityScoreboardRenderer extends TileEntitySpecialRenderer<TileEntityScoreboard> {

  /** The idle video: 32 frames of 128 x 64 on a 512 sheet, four across. */
  static final ResourceLocation VIDEO = new ResourceLocation("csm",
      "textures/blocks/venues/av/scoreboard_video.png");
  private static final int VIDEO_FRAMES = 32;
  private static final long VIDEO_FRAME_MS = 250;

  private static final CsmDisplayListCache LONG = new CsmDisplayListCache(
      "venue_scoreboard_long");
  private static final CsmDisplayListCache SHORT = new CsmDisplayListCache(
      "venue_scoreboard_short");

  private static final int BG = 0x06080C;
  private static final int PANEL = 0x121722;
  private static final int AMBER = 0xFFB428;
  private static final int YELLOW = 0xFFDC3C;
  private static final int PALE = 0xBFE4F2;
  private static final int WHITE = 0xFFFFFF;

  /** In front of the baked screen, blocks; then the layers in front of each other, pixels. */
  private static final float FACE_LIFT = 0.03f;
  private static final float VIDEO_Z = 0.05f;
  private static final float TEXT_Z = 0.15f;

  /** The share of a screen the video takes in the video layout; the strip has the rest. */
  private static final float VIDEO_SHARE = 0.8f;

  /** Releases a board's text lists. */
  static void release(BlockPos pos) {
    LONG.invalidate(pos);
    SHORT.invalidate(pos);
  }

  @Override
  public boolean isGlobalRenderer(TileEntityScoreboard te) {
    return false;
  }

  @Override
  public void render(TileEntityScoreboard te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null || CsmRenderToggles.skipVenueScoreboard) {
      return;
    }
    ScoreboardSize size = te.getSize();
    long now = te.getWorld().getTotalWorldTime();
    boolean score = te.showsScore(now);
    String clock = TileEntityScoreboard.clockText(te.remaining(now, partialTicks));
    if (te.renderedVersion != te.getTextVersion()) {
      release(te.getPos());
      te.renderedVersion = te.getTextVersion();
    }
    long ms = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks);
    int frame = (int) ((ms / VIDEO_FRAME_MS) % VIDEO_FRAMES);
    CsmFontRenderer fr = CsmFontRenderer.electronicSign();

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-(te.getFacing().getHorizontalAngle() + 180.0F), 0F, 1F, 0F);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.color(1f, 1f, 1f, 1f);

    for (int face = 0; face < 4; face++) {
      boolean longFace = face % 2 == 0;
      float hw = (longFace ? size.width : size.depth) / 2f;
      float hd = (longFace ? size.depth : size.width) / 2f;
      float w = (longFace ? size.longScreen() : size.shortScreen()) * 16f;
      float h = size.screenHeight() * 16f;
      GlStateManager.pushMatrix();
      GlStateManager.rotate(face * 90f, 0F, 1F, 0F);
      GlStateManager.translate(-hw + size.post, size.screenTop, hd + FACE_LIFT);
      GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

      GlStateManager.disableTexture2D();
      panels(te, score, w, h);
      GlStateManager.enableTexture2D();
      // a colour array leaves GL's current colour undefined, where the state cache cannot see
      GlStateManager.resetColor();
      GlStateManager.color(1f, 1f, 1f, 1f);
      if (!score) {
        video(frame, w, h);
      }
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GlStateManager.depthMask(false);
      fr.bindAtlas();
      GlStateManager.translate(0, 0, TEXT_Z);
      staticText(te, fr, score, longFace, w, h);
      clockText(fr, clock, score, w, h);
      GlStateManager.resetColor();
      GlStateManager.color(1f, 1f, 1f, 1f);
      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
    }

    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  // --- panels and video ----------------------------------------------------------------------

  private static void panels(TileEntityScoreboard te, boolean score, float w, float h) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    if (score) {
      quad(buf, 0, 0, w, h, BG, 0);
      quad(buf, 0.03f * w, 0.06f * h, 0.31f * w, 0.22f * h,
          VenueColours.rgb(te.getHomeColour()), 0.02f);
      quad(buf, 0.69f * w, 0.06f * h, 0.97f * w, 0.22f * h,
          VenueColours.rgb(te.getAwayColour()), 0.02f);
      quad(buf, 0.35f * w, 0.04f * h, 0.65f * w, 0.42f * h, PANEL, 0.02f);
    } else {
      float s = VIDEO_SHARE * h;
      quad(buf, 0, s, w, h, BG, 0);
      float bh = h - s;
      quad(buf, 0.012f * w, s + 0.2f * bh, 0.012f * w + 0.6f * bh, h - 0.2f * bh,
          VenueColours.rgb(te.getHomeColour()), 0.02f);
      quad(buf, 0.988f * w - 0.6f * bh, s + 0.2f * bh, 0.988f * w, h - 0.2f * bh,
          VenueColours.rgb(te.getAwayColour()), 0.02f);
    }
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

  /** The video's frame over the top of the screen, cropped to its shape from the middle. */
  private void video(int frame, float w, float h) {
    bindTexture(VIDEO);
    float vh = VIDEO_SHARE * h;
    float aspect = w / vh;
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
    buf.pos(0, vh, VIDEO_Z).tex(u0, v0 + dv).endVertex();
    buf.pos(w, vh, VIDEO_Z).tex(u0 + du, v0 + dv).endVertex();
    buf.pos(w, 0, VIDEO_Z).tex(u0 + du, v0).endVertex();
    buf.pos(0, 0, VIDEO_Z).tex(u0, v0).endVertex();
    tess.draw();
  }

  // --- text ----------------------------------------------------------------------------------

  /** The static text's list for this face's kind, compiled when missing. */
  private static void staticText(TileEntityScoreboard te, CsmFontRenderer fr, boolean score,
      boolean longFace, float w, float h) {
    CsmDisplayListCache cache = longFace ? LONG : SHORT;
    long key = score ? 1L : 0L;
    int list = cache.get(te.getPos(), key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = cache.allocate(te.getPos(), key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        drawStatic(te, fr, score, w, h);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      drawStatic(te, fr, score, w, h);
    }
  }

  private static void drawStatic(TileEntityScoreboard te, CsmFontRenderer fr, boolean score,
      float w, float h) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    int homeInk = VenueColours.isLight(te.getHomeColour()) ? 0x101010 : WHITE;
    int awayInk = VenueColours.isLight(te.getAwayColour()) ? 0x101010 : WHITE;
    if (score) {
      float hx = 0.17f * w;
      float ax = 0.83f * w;
      float colW = 0.26f * w;
      line(buf, fr, upper(te.getHome()), hx, 0.08f * h, 0.12f * h, colW, homeInk, 0);
      line(buf, fr, upper(te.getAway()), ax, 0.08f * h, 0.12f * h, colW, awayInk, 0);
      line(buf, fr, Integer.toString(te.getHomeScore()), hx, 0.28f * h, 0.36f * h, colW, AMBER,
          0);
      line(buf, fr, Integer.toString(te.getAwayScore()), ax, 0.28f * h, 0.36f * h, colW, AMBER,
          0);
      line(buf, fr, "SHOTS " + te.getHomeShots(), hx, 0.76f * h, 0.1f * h, colW, PALE, 0);
      line(buf, fr, "SHOTS " + te.getAwayShots(), ax, 0.76f * h, 0.1f * h, colW, PALE, 0);
      line(buf, fr, "PERIOD", 0.5f * w, 0.5f * h, 0.08f * h, 0.28f * w, PALE, 0);
      line(buf, fr, upper(te.getPeriod()), 0.5f * w, 0.61f * h, 0.24f * h, 0.28f * w, AMBER,
          0);
    } else {
      float s = VIDEO_SHARE * h;
      float bh = h - s;
      float th = 0.56f * bh;
      float ty = s + (bh - th) / 2f;
      float sq = 0.012f * w + 0.6f * bh;
      line(buf, fr, upper(te.getHome()) + " " + te.getHomeScore(), sq + 0.02f * w, ty, th,
          0.36f * w - sq, WHITE, -1);
      line(buf, fr, te.getAwayScore() + " " + upper(te.getAway()), w - sq - 0.02f * w, ty, th,
          0.36f * w - sq, WHITE, 1);
      line(buf, fr, "PER " + upper(te.getPeriod()), 0.43f * w, ty, th, 0.11f * w, PALE, 0);
    }
    tess.draw();
  }

  /** The clock: the score layout's big yellow figures, or in the strip. Drawn every frame. */
  private static void clockText(CsmFontRenderer fr, String clock, boolean score, float w,
      float h) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    if (score) {
      line(buf, fr, clock, 0.5f * w, 0.08f * h, 0.3f * h, 0.28f * w, YELLOW, 0);
    } else {
      float s = VIDEO_SHARE * h;
      float bh = h - s;
      float th = 0.56f * bh;
      line(buf, fr, clock, 0.57f * w, s + (bh - th) / 2f, th, 0.13f * w, YELLOW, 0);
    }
    tess.draw();
  }

  private static String upper(String s) {
    return s.toUpperCase(java.util.Locale.ROOT);
  }

  /**
   * One line of text, its glyphs placed and coloured: {@code height} pixels tall, shrunk to fit
   * {@code maxW}, centred on {@code x} (align 0), starting at it (-1) or ending at it (1).
   */
  static void line(BufferBuilder buf, CsmFontRenderer fr, String text, float x, float top,
      float height, float maxW, int rgb, int align) {
    if (text == null || text.isEmpty()) {
      return;
    }
    float scale = height / fr.FONT_HEIGHT;
    int width = fr.getStringWidth(text);
    if (width * scale > maxW && width > 0) {
      scale = maxW / width;
    }
    float drawn = width * scale;
    float left = align == 0 ? x - drawn / 2f : align < 0 ? x : x - drawn;
    float y = top + (height - fr.FONT_HEIGHT * scale) / 2f;
    fr.addString(buf, text, 0, 0, left, y, scale, rgb);
  }
}
