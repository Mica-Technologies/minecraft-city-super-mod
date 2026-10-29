package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmCompileBudget;
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
import net.minecraft.util.math.BlockPos;
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
 * <p><b>The board's list.</b> The column heads and the rows are one flat display list per board
 * ({@link #BOARDS}): every glyph quad of every piece of text, placed and scaled into the screen's
 * sixteenths and coloured per vertex ({@link CsmFontRenderer#addString(BufferBuilder, String,
 * int, int, float, float, float, int)}), in one draw. It once called a shared list per piece
 * instead, which cost the driver per nested call: about 13 us a board, nearly all of it in
 * {@code glCallList}. Following "Display lists: one texture, no cached state" in
 * {@code TRAFFIC_SIGNAL_SYSTEM.md}, the list holds the font atlas's geometry only: the atlas,
 * the depth mask and the fullbright lightmap are set outside it, every frame, and
 * {@code GlStateManager.resetColor()} follows it, since its vertex colours leave GL's current
 * colour where the state manager's cache does not know it.</p>
 *
 * <p>It is compiled again, in place, only when {@link FlightBoardPage}'s version moves: a flight
 * leaves the page, a remark changes, the page changes. Every board in a terminal changes on the
 * same game minute, so a board whose list is out of date asks {@link CsmCompileBudget} first and
 * draws its old list until its turn comes, a frame or two later. The clock, which changes every
 * game minute, is a list shared by every board (keyed by the minute of the day) and stays
 * outside the board's.</p>
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
  static final float WIDTH = 15.2f;
  private static final float HEIGHT = 8.8f;
  /**
   * How much of the texture's height the screen's window takes: a fraction of the sprite, measured
   * where the generator draws it (a 256 square) and true at the size it is stored at (128).
   */
  static final float WINDOW_V = 148f / 256f;
  /** The bands down the screen, in sixteenths. */
  static final float HEADER = 1.6f;
  static final float COLHEAD = 0.8f;
  static final float PITCH = 0.9f;
  /** Text heights and spacing, in sixteenths. */
  static final float TEXT_H = 0.5f;
  static final float HEAD_TEXT_H = 0.38f;
  static final float CLOCK_TEXT_H = 0.72f;
  static final float EDGE = 0.3f;
  static final float GAP = 0.3f;
  /** How far in front of the baked screen the lit one and the text are, in sixteenths. */
  private static final float SCREEN_LIFT = 0.02f;
  private static final float TEXT_LIFT = 0.02f;

  static final int HEAD_COLOUR = 0x9FB4D8;
  static final int CLOCK_COLOUR = 0xFFC830;
  static final int ROW_COLOUR = 0xF2F4F8;

  /** The clock, one list a minute of the day, shared by every board. */
  private static final CsmSharedDisplayLists CLOCK_LISTS =
      new CsmSharedDisplayLists("flight_board_clock");

  /**
   * Each board's heads and rows as one flat list of glyph quads, at key 0, compiled again in place
   * when {@link FlightBoardPage}'s version moves.
   */
  private static final CsmDisplayListCache BOARDS = new CsmDisplayListCache("flight boards");

  private static final FlightSchedule.Status[] STATUSES = FlightSchedule.Status.values();

  /** The layout, in the font's units at each text height, worked out once the font exists. */
  static float rowScale;
  static float headScale;
  static float clockScale;
  static float xTime;
  static float xFlight;
  static float xCity;
  static float xGate;
  static float xRemark;
  static float cityWidth;

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
    FlightBoardPage rows = te.clientPage;
    rows.update(arrivals, now, te.getPage() * ROWS, ROWS, 0L);

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

    // The column heads and the rows: one flat list per board, compiled again only when a row or
    // its remark changes (a flight leaves the page, a flight boards), not every frame or game
    // minute, and then only when this frame's compile budget allows.
    if (CsmRenderToggles.sharedBakesPerFrame) {
      text(fr, rows, arrivals);
    } else {
      int list = BOARDS.get(te.getPos(), 0L);
      boolean stale = te.clientCompiledVersion != rows.getVersion();
      if (list == CsmDisplayListCache.NO_LIST || stale && CsmCompileBudget.mayRecompile()) {
        list = BOARDS.allocate(te.getPos(), 0L);
        if (list != CsmDisplayListCache.NO_LIST) {
          long started = CsmCompileBudget.begin();
          GL11.glNewList(list, GL11.GL_COMPILE);
          text(fr, rows, arrivals);
          GL11.glEndList();
          CsmCompileBudget.end(started);
          te.clientCompiledVersion = rows.getVersion();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        text(fr, rows, arrivals);
      }
    }

    // The clock, which changes every game minute, outside the board's list.
    int minuteOfDay = (int) Math.floorMod(now, 24L * 60L);
    GlStateManager.pushMatrix();
    GlStateManager.translate(0, (HEADER - CLOCK_TEXT_H) / 2, 0);
    GlStateManager.scale(clockScale, clockScale, 1);
    clock(fr, minuteOfDay);
    GlStateManager.popMatrix();
    // The lists' colours are per vertex, which leaves GL's current colour where the state
    // manager's cache does not know it: make the next color() call reach GL.
    GlStateManager.resetColor();

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /**
   * The column heads and the rows, every glyph placed in the screen's sixteenths and coloured per
   * vertex, in one draw: the geometry of a board's flat list, or drawn directly.
   */
  private static void text(CsmFontRenderer fr, FlightBoardPage rows, boolean arrivals) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    float s = headScale;
    float y = HEADER + (COLHEAD - HEAD_TEXT_H) / 2;
    fr.addString(buf, "TIME", u(xTime, s), 0, 0, y, s, HEAD_COLOUR);
    fr.addString(buf, "FLIGHT", u(xFlight, s), 0, 0, y, s, HEAD_COLOUR);
    fr.addString(buf, arrivals ? "FROM" : "TO", u(xCity, s), 0, 0, y, s, HEAD_COLOUR);
    fr.addString(buf, "GATE", u(xGate, s), 0, 0, y, s, HEAD_COLOUR);
    fr.addString(buf, "REMARKS", u(xRemark, s), 0, 0, y, s, HEAD_COLOUR);
    s = rowScale;
    for (int i = 0; i < rows.getCount(); i++) {
      // a flight's white columns; any slot with this place in the day is the same flight
      long slot = FlightSchedule.dailyOf(rows.getSlot(i));
      y = HEADER + COLHEAD + i * PITCH + (PITCH - TEXT_H) / 2;
      fr.addString(buf, FlightSchedule.clock(FlightSchedule.scheduled(slot, arrivals)),
          u(xTime, s), 0, 0, y, s, ROW_COLOUR);
      fr.addString(buf, FlightSchedule.flight(slot, arrivals), u(xFlight, s), 0, 0, y, s,
          ROW_COLOUR);
      fr.addString(buf, fit(fr, FlightSchedule.city(slot, arrivals), cityWidth / s),
          u(xCity, s), 0, 0, y, s, ROW_COLOUR);
      fr.addString(buf, FlightSchedule.gateName(FlightSchedule.gateValue(slot, arrivals)),
          u(xGate, s), 0, 0, y, s, ROW_COLOUR);
      FlightSchedule.Status status = STATUSES[rows.getRemark(i)];
      fr.addString(buf, status.getText(), u(xRemark, s), 0, 0, y, s, status.getColour());
    }
    tess.draw();
  }

  /** Releases a board's list, when its tile entity goes. */
  public static void release(BlockPos pos) {
    BOARDS.invalidate(pos);
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

  static void layout(CsmFontRenderer fr) {
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

  /**
   * The clock in the header, in the font's units at its height, right-aligned to the screen's
   * edge less {@link #EDGE}: one list a minute of the day, shared by every board.
   */
  private static void clock(CsmFontRenderer fr, int minuteOfDay) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      addClock(fr, minuteOfDay);
      return;
    }
    int list = CLOCK_LISTS.get(minuteOfDay);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = CLOCK_LISTS.allocate(minuteOfDay);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        addClock(fr, minuteOfDay);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      addClock(fr, minuteOfDay);
    }
  }

  /** The clock's geometry, in its colour. */
  private static void addClock(CsmFontRenderer fr, int minuteOfDay) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    String text = FlightSchedule.clock(minuteOfDay);
    fr.addString(buf, text, u(WIDTH - EDGE, clockScale) - fr.getStringWidth(text), 0, 0, 0, 1,
        CLOCK_COLOUR);
    tess.draw();
  }

  /** Sixteenths as whole font units at a scale. */
  private static int u(float sixteenths, float scale) {
    return Math.round(sixteenths / scale);
  }

  /** The text cut short to fit a width in font units. Compile time only. */
  static String fit(CsmFontRenderer fr, String text, float width) {
    String out = text;
    while (out.length() > 1 && fr.getStringWidth(out) > width) {
      out = out.substring(0, out.length() - 1).trim();
    }
    return out;
  }
}
