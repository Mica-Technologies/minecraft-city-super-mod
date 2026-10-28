package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareScreenSprites;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws the ED tracking board's screen: its texture again, lit, and on it the title, the clock,
 * the column heads and {@link #ROWS} beds (bed, patient initials, status, time in the department,
 * nurse) from {@link EdTrackingSchedule}, starting at the board's page.
 *
 * <p>Built as the Transit flight board's renderer is: every piece of text is a display list
 * shared by every board ({@link CsmSharedDisplayLists}), compiled once and keyed by a bounded
 * value (a bed's label, a patient, a nurse, a status, an elapsed minute, the minute of the day),
 * and laid out in its place across the screen in the font's units. Following "Display lists: one
 * texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}, the lists hold geometry only:
 * the font atlas, the colours, the depth mask and the fullbright lightmap are set outside them,
 * every frame. The tile entity's render range stops it past 32 blocks
 * ({@link TileEntityEdTrackingBoard#getMaxRenderDistanceSquared}).</p>
 *
 * <p>Replaying some forty shared lists a board, each under its own matrix and colour, was still
 * about 20 us a board. What a board shows changes only with the world's minute (about every 17
 * ticks) and its page, so each board also keeps its whole page of text in one list of its own
 * ({@link CsmDisplayListCache}, keyed on {@link #pageKey}), compiled from the same pieces with
 * their colours as plain {@code glColor} calls and replayed with one {@code glCallList}. The
 * screen quad stays outside it, on the block atlas: one texture a list. The screen's sprite is
 * looked up once a stitch ({@link HealthcareScreenSprites}).
 * {@link CsmRenderToggles#sharedBakesPerFrame} draws from the shared lists alone, as before.</p>
 *
 * <p>The screen's place and bands are {@code gen_healthcare_clinical.py}'s ({@code ED_W},
 * {@code ED_H}, {@code ED_SCREEN_Z}, {@code ED_HEADER}, {@code ED_COLHEAD}, {@code ED_PITCH}):
 * change one, change both.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityEdTrackingBoardRenderer
    extends TileEntitySpecialRenderer<TileEntityEdTrackingBoard> {

  /** Beds a board lists: a page. */
  static final int ROWS = 7;

  private static final float SCREEN_LEFT_X = 15.0f;
  private static final float SCREEN_TOP_Y = 12.5f;
  private static final float SCREEN_Z = 13.7f;
  static final float WIDTH = 14.0f;
  private static final float HEIGHT = 9.0f;
  static final float HEADER = 1.5f;
  static final float COLHEAD = 0.8f;
  static final float PITCH = 0.95f;
  static final float TEXT_H = 0.5f;
  static final float HEAD_TEXT_H = 0.38f;
  static final float TITLE_TEXT_H = 0.62f;
  static final float EDGE = 0.35f;
  static final float GAP = 0.35f;
  private static final float SCREEN_LIFT = 0.02f;
  private static final float TEXT_LIFT = 0.02f;

  static final int TITLE_COLOUR = 0xFFFFFF;
  static final int HEAD_COLOUR = 0x9FC8D8;
  static final int CLOCK_COLOUR = 0xFFC830;
  static final int ROW_COLOUR = 0xF2F4F8;

  private static final CsmSharedDisplayLists LISTS = new CsmSharedDisplayLists("ed_board_text");

  /** Each board's whole page of text, one list a board, keyed on {@link #pageKey}. */
  private static final CsmDisplayListCache PAGES = new CsmDisplayListCache("ed_board_page");

  // list kinds, packed into a key with their value
  private static final int KIND_HEAD = 0;
  private static final int KIND_CLOCK = 1;
  private static final int KIND_BED = 2;
  private static final int KIND_PATIENT = 3;
  private static final int KIND_STATUS = 4;
  private static final int KIND_TIME = 5;
  private static final int KIND_NURSE = 6;

  private static final EdTrackingSchedule.Line LINE = new EdTrackingSchedule.Line();

  static float rowScale;
  static float headScale;
  static float titleScale;
  static float xBed;
  static float xPatient;
  static float xStatus;
  static float xTime;
  static float xNurse;

  @Override
  public void render(TileEntityEdTrackingBoard te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshView();
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    layout(fr);
    long now = EdTrackingSchedule.minuteOf(te.getWorld().getWorldTime());

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate((8 - SCREEN_LEFT_X) / 16.0, SCREEN_TOP_Y / 16.0,
        (8 - SCREEN_Z) / 16.0);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    screen();

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, SCREEN_LIFT + TEXT_LIFT);

    if (CsmRenderToggles.sharedBakesPerFrame) {
      text(fr, te, now, false);
    } else {
      // The whole page in one list for this board, rebuilt when the minute or the page changes.
      long key = pageKey(now, te.getPage(), te.getBeds());
      int list = PAGES.get(te.getPos(), key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = PAGES.allocate(te.getPos(), key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          text(fr, te, now, true);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // The list sets the colour with glColor, which GlStateManager does not see.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw from the shared lists rather than calling list 0.
        text(fr, te, now, false);
      }
    }

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /**
   * The text: title, column heads, clock and the page's rows. Drawn into a board's page list
   * ({@code inline}), every piece is tessellated in place and every colour is a plain
   * {@code glColor}, which a list records: {@code GlStateManager.color} is cached, so it could be
   * left out of the list, and a list cannot compile the shared lists inside itself. Otherwise each
   * piece replays its shared list ({@link #draw}) under {@code GlStateManager} colours.
   */
  private static void text(CsmFontRenderer fr, TileEntityEdTrackingBoard te, long now,
      boolean inline) {
    colour(TITLE_COLOUR, inline);
    at(0, (HEADER - TITLE_TEXT_H) / 2, titleScale);
    piece(fr, KIND_HEAD, 0, inline);
    GlStateManager.popMatrix();
    colour(HEAD_COLOUR, inline);
    at(0, HEADER + (COLHEAD - HEAD_TEXT_H) / 2, headScale);
    piece(fr, KIND_HEAD, 1, inline);
    GlStateManager.popMatrix();
    colour(CLOCK_COLOUR, inline);
    at(0, (HEADER - TITLE_TEXT_H) / 2, titleScale);
    piece(fr, KIND_CLOCK, (int) Math.floorMod(now, 24L * 60L), inline);
    GlStateManager.popMatrix();

    int first = te.getPage() * ROWS;
    int beds = te.getBeds();
    for (int i = 0; i < ROWS && first + i < beds; i++) {
      int bed = first + i;
      EdTrackingSchedule.line(bed, te.getSeed(), now, LINE);
      at(0, HEADER + COLHEAD + i * PITCH + (PITCH - TEXT_H) / 2, rowScale);
      colour(ROW_COLOUR, inline);
      piece(fr, KIND_BED, bed, inline);
      piece(fr, KIND_NURSE, LINE.nurse, inline);
      if (LINE.status.occupied()) {
        piece(fr, KIND_PATIENT, LINE.patient, inline);
        piece(fr, KIND_TIME, LINE.minutes, inline);
      }
      colour(LINE.status.getColour(), inline);
      piece(fr, KIND_STATUS, LINE.status.ordinal(), inline);
      GlStateManager.popMatrix();
    }
  }

  /**
   * The key of a board's page list. The world's minute is the clock and every row's status and
   * time; the page picks the rows; the bed count ends them. The seed is not in it: a change of
   * seed or bed count through NBT releases the board's lists ({@link #release}).
   *
   * @param now  the world's minute, {@link EdTrackingSchedule#minuteOf}
   * @param page the board's page, under 256
   * @param beds the department's beds, at most {@link EdTrackingSchedule#MAX_BEDS}
   *
   * @return the key
   */
  static long pageKey(long now, int page, int beds) {
    return (now << 14) | ((long) (beds & 0x3F) << 8) | (page & 0xFF);
  }

  /**
   * Releases a board's page lists. Called on the client from the tile entity when it goes and
   * when its seed or bed count changes.
   *
   * @param pos the board's position
   */
  public static void release(BlockPos pos) {
    PAGES.invalidate(pos);
  }

  /** One piece of text: tessellated in place for a page list, else its shared list. */
  private static void piece(CsmFontRenderer fr, int kind, int value, boolean inline) {
    if (inline) {
      add(fr, kind, value);
    } else {
      draw(fr, kind, value);
    }
  }

  /** The screen's texture, one quad a hair in front of the baked one, fullbright. */
  private void screen() {
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    TextureAtlasSprite sprite = HealthcareScreenSprites.edBoard();
    float u0 = sprite.getMinU();
    float u1 = sprite.getMaxU();
    float v0 = sprite.getMinV();
    float v1 = sprite.getInterpolatedV(16.0 * HEIGHT / WIDTH);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(0, HEIGHT, SCREEN_LIFT).tex(u0, v1).endVertex();
    buf.pos(WIDTH, HEIGHT, SCREEN_LIFT).tex(u1, v1).endVertex();
    buf.pos(WIDTH, 0, SCREEN_LIFT).tex(u1, v0).endVertex();
    buf.pos(0, 0, SCREEN_LIFT).tex(u0, v0).endVertex();
    tess.draw();
  }

  private static void at(float x, float y, float scale) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, 0);
    GlStateManager.scale(scale, scale, 1);
  }

  private static void colour(int rgb, boolean inline) {
    float r = ((rgb >> 16) & 0xFF) / 255f;
    float g = ((rgb >> 8) & 0xFF) / 255f;
    float b = (rgb & 0xFF) / 255f;
    if (inline) {
      GL11.glColor4f(r, g, b, 1.0f);
    } else {
      GlStateManager.color(r, g, b, 1.0f);
    }
  }

  private static void layout(CsmFontRenderer fr) {
    if (rowScale != 0) {
      return;
    }
    float s = TEXT_H / fr.FONT_HEIGHT;
    xBed = EDGE;
    xPatient = xBed + fr.getStringWidth("HW4") * s + GAP;
    xStatus = xPatient + fr.getStringWidth("W.W.") * s + GAP * 2;
    xNurse = WIDTH - EDGE - fr.getStringWidth("WW") * s;
    xTime = xNurse - GAP * 2 - fr.getStringWidth("0:00") * s;
    headScale = HEAD_TEXT_H / fr.FONT_HEIGHT;
    titleScale = TITLE_TEXT_H / fr.FONT_HEIGHT;
    rowScale = s;
  }

  /** Replays a text's list, compiling it the first time. */
  private static void draw(CsmFontRenderer fr, int kind, int value) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      add(fr, kind, value);
      return;
    }
    long key = ((long) kind << 32) | (value & 0xFFFFFFFFL);
    int list = LISTS.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        add(fr, kind, value);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      add(fr, kind, value);
    }
  }

  /** One piece of text in its place, in the font's units at its height. Geometry only. */
  private static void add(CsmFontRenderer fr, int kind, int value) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    float s = rowScale;
    switch (kind) {
      case KIND_HEAD:
        if (value == 0) {
          fr.addString(buf, "EMERGENCY DEPARTMENT", u(EDGE, titleScale), 0);
        } else {
          float h = headScale;
          fr.addString(buf, "BED", u(xBed, h), 0);
          fr.addString(buf, "PATIENT", u(xPatient, h), 0);
          fr.addString(buf, "STATUS", u(xStatus, h), 0);
          fr.addString(buf, "TIME", u(xTime, h), 0);
          fr.addString(buf, "RN", u(xNurse, h), 0);
        }
        break;
      case KIND_CLOCK: {
        String text = EdTrackingSchedule.clock(value);
        fr.addString(buf, text, u(WIDTH - EDGE, titleScale) - fr.getStringWidth(text), 0);
        break;
      }
      case KIND_BED:
        fr.addString(buf, EdTrackingSchedule.bedLabel(value), u(xBed, s), 0);
        break;
      case KIND_PATIENT:
        fr.addString(buf, EdTrackingSchedule.initials(value), u(xPatient, s), 0);
        break;
      case KIND_STATUS:
        fr.addString(buf, EdTrackingSchedule.Status.values()[value].getText(), u(xStatus, s), 0);
        break;
      case KIND_TIME:
        fr.addString(buf, EdTrackingSchedule.elapsed(value), u(xTime, s), 0);
        break;
      default:
        fr.addString(buf, EdTrackingSchedule.nurse(value), u(xNurse, s), 0);
        break;
    }
    tess.draw();
  }

  private static int u(float sixteenths, float scale) {
    return Math.round(sixteenths / scale);
  }
}
