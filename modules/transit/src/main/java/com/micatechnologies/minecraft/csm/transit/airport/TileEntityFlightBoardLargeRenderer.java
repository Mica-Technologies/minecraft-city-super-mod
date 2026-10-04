package com.micatechnologies.minecraft.csm.transit.airport;

import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.CLOCK_COLOUR;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.CLOCK_TEXT_H;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.COLHEAD;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.EDGE;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.GAP;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.HEADER;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.HEAD_COLOUR;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.HEAD_TEXT_H;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.PITCH;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.ROW_COLOUR;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.TEXT_H;
import static com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer.WIDTH;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.transit.panel.PanelLayout;
import com.micatechnologies.minecraft.csm.transit.panel.PanelRods;
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
 * Draws a large flight information board ({@link BlockFlightBoardLarge}) from its controller,
 * once across the whole panel: the hanger rods, the screen, and on it the column heads, the
 * clock, the page and as many flights as fit, from {@link FlightSchedule}.
 *
 * <p><b>Scale.</b> The board is the one-block board ({@link TileEntityFlightBoardRenderer}) drawn
 * bigger: the same bands and text heights, in the same proportions, times a scale
 * {@code k = 2 (h / 2)^0.75} for a board {@code h} blocks tall (1.2 for one block tall, 2 for two,
 * 2.7 for three, 4 for five), so a two-tall board's text is twice the small board's and a taller
 * board has both bigger text and more rows. A board too narrow for its
 * columns at that scale is scaled down to fit them, never narrower in proportion than the small
 * board. The rows are the small board's pitch at that scale, as many as fit the screen below the
 * heads, then spread to fill it. Extra width goes to the city column: time and flight keep the
 * left, gate and remark the right.</p>
 *
 * <p><b>Pages.</b> A board shows {@code rows} flights a page, starting at {@code page * rows} as
 * a bank of small boards does. When a page holds fewer than {@link #PAGE_FLIGHTS} the board turns
 * through enough pages to list that many, one every {@link #PAGE_TICKS} ticks of world time, with
 * PAGE n/m in the header; every board turns in step.</p>
 *
 * <p><b>The screen.</b> The small board's texture ({@code fids_departures}, {@code fids_arrivals})
 * cut into its bands: the header's left part (the plane and the title) at its own proportions,
 * the rest of the header, the column heads' band and the rows' two zebra bands stretched, all
 * fullbright, in one draw.</p>
 *
 * <p><b>The text.</b> Display lists shared by every large board, compiled once each in the font's
 * units at x = 0 and placed and scaled outside: a row's time and flight (keyed by the flight's
 * place in the day), its city (and by the room it has, in steps of eight units), its gate, its
 * remark, the two halves of the column heads, the clock and the page. Following "Display lists:
 * one texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}, the lists hold geometry only:
 * the font atlas, the colours, the depth mask and the fullbright lightmap are set outside them,
 * every frame.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntityFlightBoardLargeRenderer
    extends TileEntitySpecialRenderer<TileEntityFlightBoardLarge> {

  /** The slab's face (the model's z 13), toward the reader from the cell's middle, in blocks. */
  private static final float FACE_Z = -5f / 16f;
  /** The rods' centre line (the model's z 14.5), in blocks. */
  private static final float ROD_Z = -6.5f / 16f;
  /** The screen's inset from the panel's outer edges, inside the frame, in sixteenths. */
  static final float INSET = 1.4f;
  /** How far in front of the slab the screen is, and the text in front of it, in sixteenths. */
  private static final float SCREEN_LIFT = 0.1f;
  private static final float TEXT_LIFT = 0.05f;

  /** The fewest flights a board lists, over as many pages as that takes. */
  static final int PAGE_FLIGHTS = 24;
  /** Ticks each page shows. */
  static final int PAGE_TICKS = 160;
  /** The most rows drawn, whatever the board's shape. */
  private static final int MAX_ROWS = 96;

  /**
   * The small board's texture bands, in texels of the 256 square it is drawn on
   * ({@code gen_transit_airport.py}'s {@code fids()}): the header, the column heads' band, the
   * first two rows' bands (a zebra), and the header's left part holding the plane and the title.
   */
  private static final float HEADER_V = 27f;
  private static final float COLHEAD_V0 = 28f;
  private static final float COLHEAD_V1 = 39f;
  private static final float ROW0_V = 48f;
  private static final float ROW1_V = 63.5f;
  private static final float BAND_HALF = 2f;
  private static final float TITLE_U = 160f;

  private static final CsmSharedDisplayLists LEFT_LISTS =
      new CsmSharedDisplayLists("flight_board_large_left");
  private static final CsmSharedDisplayLists CITY_LISTS =
      new CsmSharedDisplayLists("flight_board_large_city");
  private static final CsmSharedDisplayLists GATE_LISTS =
      new CsmSharedDisplayLists("flight_board_large_gate");
  private static final CsmSharedDisplayLists STATUS_LISTS =
      new CsmSharedDisplayLists("flight_board_large_remarks");
  private static final CsmSharedDisplayLists HEAD_LISTS =
      new CsmSharedDisplayLists("flight_board_large_heads");
  private static final CsmSharedDisplayLists CLOCK_LISTS =
      new CsmSharedDisplayLists("flight_board_large_clock");
  private static final CsmSharedDisplayLists PAGE_LISTS =
      new CsmSharedDisplayLists("flight_board_large_page");

  private static final int KIND_LEFT = 0;
  private static final int KIND_CITY = 1;
  private static final int KIND_GATE = 2;
  private static final int KIND_STATUS = 3;
  private static final int KIND_HEAD_LEFT = 4;
  private static final int KIND_HEAD_RIGHT = 5;
  private static final int KIND_CLOCK = 6;
  private static final int KIND_PAGE = 7;

  /** The flights being drawn, an array for each number of rows: scratch, render thread only. */
  private static final long[][] SLOTS = new long[MAX_ROWS + 1][];

  @Override
  public boolean isGlobalRenderer(TileEntityFlightBoardLarge te) {
    return true;
  }

  @Override
  public void render(TileEntityFlightBoardLarge te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshLayout();
    PanelLayout panel = te.getLayout();
    if (!panel.isController()) {
      return;
    }
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    TileEntityFlightBoardRenderer.layout(fr);
    boolean arrivals = te.isArrivals();
    int width = panel.getWidth();
    int height = panel.getHeight();

    // the screen, in sixteenths
    float sw = 16f * width - 2 * INSET;
    float sh = 16f * height - 2 * INSET;
    float k = scale(width, height);
    float header = HEADER * k;
    float colhead = COLHEAD * k;
    int rows = rows(width, height);
    float pitch = (sh - header - colhead) / rows;
    int pages = pages(rows);
    long total = te.getWorld().getTotalWorldTime();
    int page = pages <= 1 ? 0 : (int) ((total / PAGE_TICKS) % pages);
    long now = FlightSchedule.minuteOf(te.getWorld().getWorldTime());
    long[] slots = slots(rows);
    int count = FlightSchedule.list(arrivals, now, page * rows, slots);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so the model's front (north) faces +z: the model's point (mx, my, mz) is
    // (8 - mx, my, 8 - mz) / 16 here and the reader's right is +x; then moved to the panel's
    // left edge as read (the model's x = 16).
    GlStateManager.rotate(-panel.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, 0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

    PanelRods.draw(te.getWorld(), te.getPos(), panel, ROD_Z);

    // to the screen's top-left corner, in sixteenths with y down the screen
    GlStateManager.translate(INSET / 16.0, (16 * height - INSET) / 16.0, FACE_Z);
    GlStateManager.scale(1 / 16.0, -1 / 16.0, 1 / 16.0);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    screen(arrivals, sw, header, colhead, rows, pitch);

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, SCREEN_LIFT + TEXT_LIFT);

    // the columns, in sixteenths: the left ones as the small board's, the right ones as far from
    // the right edge as the small board's are from its
    float xTime = TileEntityFlightBoardRenderer.xTime * k;
    float xCity = TileEntityFlightBoardRenderer.xCity * k;
    float xGate = sw - (WIDTH - TileEntityFlightBoardRenderer.xGate) * k;
    float xRemark = sw - (WIDTH - TileEntityFlightBoardRenderer.xRemark) * k;
    float rowScale = TileEntityFlightBoardRenderer.rowScale * k;
    float headScale = TileEntityFlightBoardRenderer.headScale * k;
    float clockScale = TileEntityFlightBoardRenderer.clockScale * k;
    // the city's room, in the font's units at the row's scale, in steps of eight
    int cityUnits = Math.max(8, Math.min(4088,
        (int) ((xGate - GAP * k - xCity) / rowScale) / 8 * 8));

    colour(HEAD_COLOUR);
    float headY = header + (colhead - HEAD_TEXT_H * k) / 2;
    at(xTime, headY, headScale);
    draw(HEAD_LISTS, arrivals ? 1 : 0, fr, KIND_HEAD_LEFT, arrivals ? 1 : 0, false, 0);
    GlStateManager.popMatrix();
    at(xGate, headY, headScale);
    draw(HEAD_LISTS, 2, fr, KIND_HEAD_RIGHT, 0, false, 0);
    GlStateManager.popMatrix();

    colour(CLOCK_COLOUR);
    int minuteOfDay = (int) Math.floorMod(now, 24L * 60L);
    float clockRight = sw - EDGE * k;
    at(clockRight, (header - CLOCK_TEXT_H * k) / 2, clockScale);
    draw(CLOCK_LISTS, minuteOfDay, fr, KIND_CLOCK, minuteOfDay, false, 0);
    GlStateManager.popMatrix();
    if (pages > 1) {
      colour(HEAD_COLOUR);
      float clockWidth = fr.getStringWidth("00:00") * clockScale;
      at(clockRight - clockWidth - 4 * EDGE * k, (header - HEAD_TEXT_H * k) / 2, headScale);
      int key = page * 64 + pages;
      draw(PAGE_LISTS, key, fr, KIND_PAGE, key, false, 0);
      GlStateManager.popMatrix();
    }

    for (int i = 0; i < count; i++) {
      long slot = slots[i];
      int daily = FlightSchedule.dailyOf(slot);
      FlightSchedule.Status status = FlightSchedule.status(slot, arrivals, now);
      float rowY = header + colhead + i * pitch + (pitch - TEXT_H * k) / 2;
      int flight = (arrivals ? 1000 : 0) + daily;
      colour(ROW_COLOUR);
      at(xTime, rowY, rowScale);
      draw(LEFT_LISTS, flight, fr, KIND_LEFT, daily, arrivals, 0);
      GlStateManager.popMatrix();
      at(xCity, rowY, rowScale);
      draw(CITY_LISTS, (long) flight * 4096 + cityUnits, fr, KIND_CITY, daily, arrivals,
          cityUnits);
      GlStateManager.popMatrix();
      int gate = FlightSchedule.gateValue(slot, arrivals);
      at(xGate, rowY, rowScale);
      draw(GATE_LISTS, gate, fr, KIND_GATE, gate, arrivals, 0);
      GlStateManager.popMatrix();
      colour(status.getColour());
      at(xRemark, rowY, rowScale);
      draw(STATUS_LISTS, status.ordinal(), fr, KIND_STATUS, status.ordinal(), arrivals, 0);
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

  /** A scratch array of exactly this many flights. Render thread only. */
  private static long[] slots(int rows) {
    long[] slots = SLOTS[rows];
    if (slots == null) {
      slots = new long[rows];
      SLOTS[rows] = slots;
    }
    return slots;
  }

  /**
   * The board's scale against the small board: {@code 2 (height / 2)^0.75}, or less if its screen
   * is narrower than that in proportion to the small board's.
   *
   * @param width  the board's width in blocks
   * @param height its height in blocks
   *
   * @return the scale
   */
  static float scale(int width, int height) {
    float sw = 16f * width - 2 * INSET;
    return (float) Math.min(2.0 * Math.pow(height / 2.0, 0.75), sw / WIDTH);
  }

  /**
   * How many flights a page of the board lists: as many rows of the small board's pitch, at the
   * board's scale, as fit under the heads, at least one and at most {@link #MAX_ROWS}.
   *
   * @param width  the board's width in blocks
   * @param height its height in blocks
   *
   * @return the rows
   */
  static int rows(int width, int height) {
    float k = scale(width, height);
    float sh = 16f * height - 2 * INSET;
    int rows = (int) Math.floor((sh - (HEADER + COLHEAD) * k) / (PITCH * k));
    return Math.max(1, Math.min(MAX_ROWS, rows));
  }

  /** How many pages a board of this many rows turns through. */
  static int pages(int rows) {
    return Math.max(1, (PAGE_FLIGHTS + rows - 1) / rows);
  }

  /**
   * The screen: the small board's texture cut into its bands, fullbright, one quad a band, in
   * sixteenths with y down the screen, {@link #SCREEN_LIFT} in front of the slab.
   */
  private void screen(boolean arrivals, float sw, float header, float colhead, int rows,
      float pitch) {
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
        "csm:blocks/transit/airport/" + (arrivals ? "fids_arrivals" : "fids_departures"));
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    // the header's left part at its own proportions (the texture's 256 texels span the small
    // board's WIDTH sixteenths), then the rest of the header stretched
    float titleW = Math.min(sw, TITLE_U / 256f * WIDTH * (header / HEADER));
    quad(buf, sprite, 0, 0, titleW, header, 0, 0, TITLE_U, HEADER_V);
    quad(buf, sprite, titleW, 0, sw, header, TITLE_U + 8, 0, 256, HEADER_V);
    quad(buf, sprite, 0, header, sw, header + colhead, 0, COLHEAD_V0, 256, COLHEAD_V1);
    float top = header + colhead;
    for (int i = 0; i < rows; i++) {
      float v = i % 2 == 0 ? ROW0_V : ROW1_V;
      quad(buf, sprite, 0, top + i * pitch, sw, top + (i + 1) * pitch, 0, v - BAND_HALF, 256,
          v + BAND_HALF);
    }
    tess.draw();
  }

  /** A quad from (x0, y0) to (x1, y1) sixteenths, its texture a window of the 256 square. */
  private static void quad(BufferBuilder buf, TextureAtlasSprite sprite, float x0, float y0,
      float x1, float y1, float u0, float v0, float u1, float v1) {
    float tu0 = sprite.getInterpolatedU(u0 / 16f);
    float tu1 = sprite.getInterpolatedU(u1 / 16f);
    float tv0 = sprite.getInterpolatedV(v0 / 16f);
    float tv1 = sprite.getInterpolatedV(v1 / 16f);
    float z = SCREEN_LIFT;
    buf.pos(x0, y1, z).tex(tu0, tv1).endVertex();
    buf.pos(x1, y1, z).tex(tu1, tv1).endVertex();
    buf.pos(x1, y0, z).tex(tu1, tv0).endVertex();
    buf.pos(x0, y0, z).tex(tu0, tv0).endVertex();
  }

  /** Pushes a frame at (x, y) sixteenths, in font units at a scale of sixteenths per unit. */
  private static void at(float x, float y, float scale) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, 0);
    GlStateManager.scale(scale, scale, 1);
  }

  private static void colour(int rgb) {
    GlStateManager.color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
        (rgb & 0xFF) / 255f, 1.0f);
  }

  /** Replays a text's list, compiling it the first time. */
  private static void draw(CsmSharedDisplayLists lists, long key, CsmFontRenderer fr, int kind,
      int value, boolean arrivals, int room) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      add(fr, kind, value, arrivals, room);
      return;
    }
    int list = lists.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = lists.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        add(fr, kind, value, arrivals, room);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      add(fr, kind, value, arrivals, room);
    }
  }

  /**
   * One piece of text in the font's units from x = 0 (right-aligned to x = 0 for the clock and
   * the page), the gaps between its words the small board's, in the font's units at the
   * piece's height. Geometry only.
   */
  private static void add(CsmFontRenderer fr, int kind, int value, boolean arrivals, int room) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    float rs = TileEntityFlightBoardRenderer.rowScale;
    float hs = TileEntityFlightBoardRenderer.headScale;
    float xTime = TileEntityFlightBoardRenderer.xTime;
    if (kind == KIND_LEFT) {
      long slot = value;
      fr.addString(buf, FlightSchedule.clock(FlightSchedule.scheduled(slot, arrivals)), 0, 0);
      fr.addString(buf, FlightSchedule.flight(slot, arrivals),
          Math.round((TileEntityFlightBoardRenderer.xFlight - xTime) / rs), 0);
    } else if (kind == KIND_CITY) {
      fr.addString(buf, TileEntityFlightBoardRenderer.fit(fr,
          FlightSchedule.city(value, arrivals), room), 0, 0);
    } else if (kind == KIND_GATE) {
      fr.addString(buf, FlightSchedule.gateName(value), 0, 0);
    } else if (kind == KIND_STATUS) {
      fr.addString(buf, FlightSchedule.Status.values()[value].getText(), 0, 0);
    } else if (kind == KIND_HEAD_LEFT) {
      fr.addString(buf, "TIME", 0, 0);
      fr.addString(buf, "FLIGHT", Math.round((TileEntityFlightBoardRenderer.xFlight - xTime) / hs),
          0);
      fr.addString(buf, value == 1 ? "FROM" : "TO",
          Math.round((TileEntityFlightBoardRenderer.xCity - xTime) / hs), 0);
    } else if (kind == KIND_HEAD_RIGHT) {
      fr.addString(buf, "GATE", 0, 0);
      fr.addString(buf, "REMARKS", Math.round((TileEntityFlightBoardRenderer.xRemark
          - TileEntityFlightBoardRenderer.xGate) / hs), 0);
    } else if (kind == KIND_CLOCK) {
      String text = FlightSchedule.clock(value);
      fr.addString(buf, text, -fr.getStringWidth(text), 0);
    } else {
      String text = "PAGE " + (value / 64 + 1) + "/" + (value % 64);
      fr.addString(buf, text, -fr.getStringWidth(text), 0);
    }
    tess.draw();
  }
}
