package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a flight information board's screen: the screen's own texture, lit, and on it the column
 * heads, the clock in the header and {@link #ROWS} flights -- time, flight, city, gate and a
 * coloured remark -- from {@link FlightSchedule}, starting at the board's page of the list
 * ({@link TileEntityFlightBoard}).
 *
 * <p><b>The screen.</b> The baked model has the same texture on the screen, lit by the world, so
 * it is dark at night; this draws it again a hair in front, fullbright, one quad from the block
 * atlas, so a board glows as a screen does. Past {@link TileEntityFlightBoard#getMaxRenderDistanceSquared}
 * only the baked screen is seen.</p>
 *
 * <p><b>The text.</b> Every piece of text is a display list shared by every board
 * ({@link CsmSharedDisplayLists}), compiled once and laid out in its place across the screen in
 * the font's units: a row's white part (time, flight, city, gate) keyed by the flight's place in
 * the day, so there are at most 144 of them over both lists; each remark, nine; the column heads,
 * two; the clock, keyed by the minute of the day. A board's frame is one quad and at most
 * sixteen list calls, each under its own translation. Following "Display lists: one texture, no
 * cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}, the lists hold geometry only: the font
 * atlas, the colours, the depth mask and the fullbright lightmap are set outside them, every
 * frame.</p>
 *
 * <p>The screen's place and the row bands are {@code gen_transit_airport.py}'s ({@code SCREEN_*},
 * {@code HEADER_H}, {@code COLHEAD_H}, {@code ROW_PITCH}, {@code ROWS}, and the texture's window,
 * 256 x 148 texels of 256): change one, change both.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityFlightBoardRenderer
    extends TileEntitySpecialRenderer<TileEntityFlightBoard> {

  /** Flights a board lists: a page. */
  static final int ROWS = 7;

  /** The screen, facing north, in sixteenths: its reader's left edge (high x), top and face. */
  private static final float SCREEN_LEFT_X = 15.6f;
  private static final float SCREEN_TOP_Y = 12.4f;
  private static final float SCREEN_Z = 14.6f;
  private static final float WIDTH = 15.2f;
  private static final float HEIGHT = 8.8f;
  /** How much of the texture's height the screen's window takes. */
  private static final float WINDOW_V = 148f / 256f;
  /** The bands down the screen, in sixteenths. */
  private static final float HEADER = 1.6f;
  private static final float COLHEAD = 0.8f;
  private static final float PITCH = 0.9f;
  /** Text heights and spacing, in sixteenths. */
  private static final float TEXT_H = 0.5f;
  private static final float HEAD_TEXT_H = 0.38f;
  private static final float CLOCK_TEXT_H = 0.72f;
  private static final float EDGE = 0.3f;
  private static final float GAP = 0.3f;
  /** How far in front of the baked screen the lit one and the text are, in sixteenths. */
  private static final float SCREEN_LIFT = 0.02f;
  private static final float TEXT_LIFT = 0.02f;

  private static final int HEAD_COLOUR = 0x9FB4D8;
  private static final int CLOCK_COLOUR = 0xFFC830;
  private static final int ROW_COLOUR = 0xF2F4F8;

  private static final CsmSharedDisplayLists ROW_LISTS =
      new CsmSharedDisplayLists("flight_board_rows");
  private static final CsmSharedDisplayLists STATUS_LISTS =
      new CsmSharedDisplayLists("flight_board_remarks");
  private static final CsmSharedDisplayLists HEAD_LISTS =
      new CsmSharedDisplayLists("flight_board_heads");
  private static final CsmSharedDisplayLists CLOCK_LISTS =
      new CsmSharedDisplayLists("flight_board_clock");

  private static final int KIND_ROW = 0;
  private static final int KIND_STATUS = 1;
  private static final int KIND_HEAD = 2;
  private static final int KIND_CLOCK = 3;

  /** The flights being drawn: scratch, render thread only. */
  private static final long[] SLOTS = new long[ROWS];

  /** The layout, in the font's units at each text height, worked out once the font exists. */
  private static float rowScale;
  private static float headScale;
  private static float clockScale;
  private static float xTime;
  private static float xFlight;
  private static float xCity;
  private static float xGate;
  private static float xRemark;
  private static float cityWidth;

  @Override
  public void render(TileEntityFlightBoard te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshView();
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    layout(fr);
    boolean arrivals = te.isArrivals();
    long now = FlightSchedule.minuteOf(te.getWorld().getWorldTime());
    int count = FlightSchedule.list(arrivals, now, te.getPage() * ROWS, SLOTS);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so the model's front (north) faces +z, as the platform clock's renderer does: the
    // model's point (mx, my, mz) is (8 - mx, my, 8 - mz) / 16 here, and the reader's right is +x.
    GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate((8 - SCREEN_LEFT_X) / 16.0, SCREEN_TOP_Y / 16.0,
        (8 - SCREEN_Z) / 16.0);
    // sixteenths, y down the screen from its top-left corner
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    screen(arrivals);

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, SCREEN_LIFT + TEXT_LIFT);

    colour(HEAD_COLOUR);
    at(0, HEADER + (COLHEAD - HEAD_TEXT_H) / 2, headScale);
    draw(HEAD_LISTS, arrivals ? 1 : 0, fr, KIND_HEAD, arrivals ? 1 : 0, false);
    GlStateManager.popMatrix();

    colour(CLOCK_COLOUR);
    int minuteOfDay = (int) Math.floorMod(now, 24L * 60L);
    at(0, (HEADER - CLOCK_TEXT_H) / 2, clockScale);
    draw(CLOCK_LISTS, minuteOfDay, fr, KIND_CLOCK, minuteOfDay, false);
    GlStateManager.popMatrix();

    for (int i = 0; i < count; i++) {
      long slot = SLOTS[i];
      int daily = FlightSchedule.dailyOf(slot);
      FlightSchedule.Status status = FlightSchedule.status(slot, arrivals, now);
      at(0, HEADER + COLHEAD + i * PITCH + (PITCH - TEXT_H) / 2, rowScale);
      colour(ROW_COLOUR);
      draw(ROW_LISTS, (arrivals ? 1000 : 0) + daily, fr, KIND_ROW, daily, arrivals);
      colour(status.getColour());
      draw(STATUS_LISTS, status.ordinal(), fr, KIND_STATUS, status.ordinal(), arrivals);
      GlStateManager.popMatrix();
    }

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /** The screen's texture, one quad a hair in front of the baked one. */
  private void screen(boolean arrivals) {
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
        "csm:blocks/transit/airport/" + (arrivals ? "fids_arrivals" : "fids_departures"));
    float u0 = sprite.getMinU();
    float u1 = sprite.getMaxU();
    float v0 = sprite.getMinV();
    float v1 = sprite.getInterpolatedV(16.0 * WINDOW_V);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(0, HEIGHT, SCREEN_LIFT).tex(u0, v1).endVertex();
    buf.pos(WIDTH, HEIGHT, SCREEN_LIFT).tex(u1, v1).endVertex();
    buf.pos(WIDTH, 0, SCREEN_LIFT).tex(u1, v0).endVertex();
    buf.pos(0, 0, SCREEN_LIFT).tex(u0, v0).endVertex();
    tess.draw();
  }

  /** Pushes a frame at (x, y) sixteenths, in font units at a text height's scale. */
  private static void at(float x, float y, float scale) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, 0);
    GlStateManager.scale(scale, scale, 1);
  }

  private static void colour(int rgb) {
    GlStateManager.color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
        (rgb & 0xFF) / 255f, 1.0f);
  }

  private static void layout(CsmFontRenderer fr) {
    if (rowScale != 0) {
      return;
    }
    float s = TEXT_H / fr.FONT_HEIGHT;
    xTime = EDGE;
    xFlight = xTime + fr.getStringWidth("00:00") * s + GAP;
    xCity = xFlight + fr.getStringWidth("WW 000") * s + GAP;
    xRemark = WIDTH - EDGE - fr.getStringWidth("GATE CLOSED") * s;
    xGate = xRemark - GAP - fr.getStringWidth("D20") * s;
    cityWidth = xGate - GAP - xCity;
    headScale = HEAD_TEXT_H / fr.FONT_HEIGHT;
    clockScale = CLOCK_TEXT_H / fr.FONT_HEIGHT;
    rowScale = s;
  }

  /** Replays a text's list, compiling it the first time. */
  private static void draw(CsmSharedDisplayLists lists, long key, CsmFontRenderer fr, int kind,
      int value, boolean arrivals) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      add(fr, kind, value, arrivals);
      return;
    }
    int list = lists.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = lists.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        add(fr, kind, value, arrivals);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      add(fr, kind, value, arrivals);
    }
  }

  /**
   * One piece of text, laid out in its place across the screen in the font's units at its
   * height. Geometry only.
   */
  private static void add(CsmFontRenderer fr, int kind, int value, boolean arrivals) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    if (kind == KIND_ROW) {
      // a flight's white columns; any slot with this place in the day is the same flight
      long slot = value;
      float s = rowScale;
      fr.addString(buf, FlightSchedule.clock(FlightSchedule.scheduled(slot, arrivals)),
          u(xTime, s), 0);
      fr.addString(buf, FlightSchedule.flight(slot, arrivals), u(xFlight, s), 0);
      fr.addString(buf, fit(fr, FlightSchedule.city(slot, arrivals), cityWidth / s),
          u(xCity, s), 0);
      fr.addString(buf, FlightSchedule.gateName(FlightSchedule.gateValue(slot, arrivals)),
          u(xGate, s), 0);
    } else if (kind == KIND_STATUS) {
      fr.addString(buf, FlightSchedule.Status.values()[value].getText(), u(xRemark, rowScale),
          0);
    } else if (kind == KIND_HEAD) {
      float s = headScale;
      fr.addString(buf, "TIME", u(xTime, s), 0);
      fr.addString(buf, "FLIGHT", u(xFlight, s), 0);
      fr.addString(buf, value == 1 ? "FROM" : "TO", u(xCity, s), 0);
      fr.addString(buf, "GATE", u(xGate, s), 0);
      fr.addString(buf, "REMARKS", u(xRemark, s), 0);
    } else {
      String text = FlightSchedule.clock(value);
      fr.addString(buf, text, u(WIDTH - EDGE, clockScale) - fr.getStringWidth(text), 0);
    }
    tess.draw();
  }

  /** Sixteenths as whole font units at a scale. */
  private static int u(float sixteenths, float scale) {
    return Math.round(sixteenths / scale);
  }

  /** The text cut short to fit a width in font units. Compile time only. */
  private static String fit(CsmFontRenderer fr, String text, float width) {
    String out = text;
    while (out.length() > 1 && fr.getStringWidth(out) > width) {
      out = out.substring(0, out.length() - 1).trim();
    }
    return out;
  }
}
