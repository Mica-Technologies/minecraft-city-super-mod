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
import net.minecraft.util.math.BlockPos;
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
 * the font atlas, the depth mask and the fullbright lightmap are set outside them, every
 * frame.</p>
 *
 * <p><b>The board's lists.</b> Drawing those pieces one by one cost about 50 microseconds a
 * board whatever its size (a push, a translation, a scale, a colour and a call per row and
 * column). So each board also has two lists of its own ({@link #BOARDS}, on its controller): the
 * screen, compiled again when the board's size or list changes or the atlas moves its sprite;
 * and its heads, page label and rows as calls to the shared text lists, each under its place and
 * colour (plain {@code glColor}, which a list records, and {@code GlStateManager.resetColor()}
 * after it, since the state manager no longer knows the colour), compiled again in place only
 * when {@link FlightBoardPage}'s version moves: a flight leaves the page, a remark changes, the
 * page turns. The clock, which changes every game minute, is drawn outside them. The lists are
 * released with the controller's tile entity (removal and chunk unload) and on disconnect.</p>
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

  /**
   * Each board's two lists, on its controller: its screen ({@link #SCREEN_KEY}) and its heads,
   * page and rows ({@link #TEXT_KEY}), a list of calls to the shared text lists above. Each is
   * compiled again in place when what it shows changes, so a board holds two lists whatever its
   * size; compiling the text list costs next to nothing, which matters because every board in a
   * terminal changes on the same game minute.
   */
  private static final CsmDisplayListCache BOARDS =
      new CsmDisplayListCache("large flight boards");
  private static final long TEXT_KEY = 0L;
  private static final long SCREEN_KEY = 1L;

  /** How {@link #text} draws: directly, compiling the pieces, or calling them. */
  private static final int TEXT_PER_FRAME = 0;
  private static final int TEXT_COMPILE_PIECES = 1;
  private static final int TEXT_CALL_PIECES = 2;

  private static final String DEPARTURES_SPRITE = "csm:blocks/transit/airport/fids_departures";
  private static final String ARRIVALS_SPRITE = "csm:blocks/transit/airport/fids_arrivals";

  private static final FlightSchedule.Status[] STATUSES = FlightSchedule.Status.values();

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
    float k = scale(width, height);
    float header = HEADER * k;
    int rows = rows(width, height);
    int pages = pages(rows);
    long total = te.getWorld().getTotalWorldTime();
    int page = pages <= 1 ? 0 : (int) ((total / PAGE_TICKS) % pages);
    long now = FlightSchedule.minuteOf(te.getWorld().getWorldTime());
    FlightBoardPage shown = te.clientPage;
    shown.update(arrivals, now, page * rows, rows, layoutKey(width, height, page, pages));

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
    // The screen: one list a board, compiled again when its size or list changes, or when the
    // atlas is stitched again and moves its texture.
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(arrivals ? ARRIVALS_SPRITE : DEPARTURES_SPRITE);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      screen(sprite, width, height);
    } else {
      long screenKey = layoutKey(width, height, arrivals ? 1 : 0, 0);
      float[] uv = te.clientScreenUv;
      int list = BOARDS.get(te.getPos(), SCREEN_KEY);
      if (list == CsmDisplayListCache.NO_LIST || te.clientScreenLayout != screenKey
          || uv[0] != sprite.getMinU() || uv[1] != sprite.getMinV()
          || uv[2] != sprite.getMaxU() || uv[3] != sprite.getMaxV()) {
        list = BOARDS.allocate(te.getPos(), SCREEN_KEY);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          screen(sprite, width, height);
          GL11.glEndList();
          te.clientScreenLayout = screenKey;
          uv[0] = sprite.getMinU();
          uv[1] = sprite.getMinV();
          uv[2] = sprite.getMaxU();
          uv[3] = sprite.getMaxV();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        screen(sprite, width, height);
      }
    }

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, SCREEN_LIFT + TEXT_LIFT);

    // The heads, the page and the rows: one list per board, calling the shared text lists under
    // their colours and places, compiled again only when a row, a remark or the page changes.
    if (CsmRenderToggles.sharedBakesPerFrame) {
      text(TEXT_PER_FRAME, fr, shown, arrivals, width, height, page, pages);
    } else {
      int list = BOARDS.get(te.getPos(), TEXT_KEY);
      if (list == CsmDisplayListCache.NO_LIST
          || te.clientCompiledVersion != shown.getVersion()) {
        // every text list the board's calls first, since a list cannot be compiled inside another
        text(TEXT_COMPILE_PIECES, fr, shown, arrivals, width, height, page, pages);
        list = BOARDS.allocate(te.getPos(), TEXT_KEY);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          text(TEXT_CALL_PIECES, fr, shown, arrivals, width, height, page, pages);
          GL11.glEndList();
          te.clientCompiledVersion = shown.getVersion();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        text(TEXT_CALL_PIECES, fr, shown, arrivals, width, height, page, pages);
      }
    }
    // The board's colours were set by plain glColor (a list cannot hold GlStateManager's), so
    // its cache no longer knows the colour: make the next color() call reach GL.
    GlStateManager.resetColor();

    // The clock, which changes every game minute, outside the board's list.
    colour(CLOCK_COLOUR);
    int minuteOfDay = (int) Math.floorMod(now, 24L * 60L);
    float clockScale = TileEntityFlightBoardRenderer.clockScale * k;
    at(sw - EDGE * k, (header - CLOCK_TEXT_H * k) / 2, clockScale);
    draw(CLOCK_LISTS, minuteOfDay, fr, KIND_CLOCK, minuteOfDay, false, 0);
    GlStateManager.popMatrix();

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    GlStateManager.popMatrix();
  }

  /**
   * What a board's text list is drawn from besides its rows, packed: its size, the page shown and
   * how many there are (the page label). {@link FlightBoardPage} moves its version when it changes.
   */
  static long layoutKey(int width, int height, int page, int pages) {
    return (width & 0xFFFFL) | (height & 0xFFFFL) << 16 | (page & 0xFFFFL) << 32
        | (pages & 0xFFFFL) << 48;
  }

  /**
   * The column heads, the page label and the rows, in their places and colours, in one of three
   * ways: every piece drawn directly ({@link #TEXT_PER_FRAME}); every shared text list a board's
   * list calls compiled if it is missing ({@link #TEXT_COMPILE_PIECES}, before the board's list is
   * opened, since a list cannot be compiled inside another); or every piece its shared list's call
   * ({@link #TEXT_CALL_PIECES}, what the board's list is compiled from). Colours are plain
   * {@code glColor} and the places plain matrix calls, which a list records.
   */
  private static void text(int mode, CsmFontRenderer fr, FlightBoardPage shown, boolean arrivals,
      int width, int height, int page, int pages) {
    // the layout, worked out as the frame's own was before the board had a list
    float sw = 16f * width - 2 * INSET;
    float sh = 16f * height - 2 * INSET;
    float k = scale(width, height);
    float header = HEADER * k;
    float colhead = COLHEAD * k;
    int rows = rows(width, height);
    float pitch = (sh - header - colhead) / rows;
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

    TileEntityFlightBoardRenderer.glColour(HEAD_COLOUR);
    float headY = header + (colhead - HEAD_TEXT_H * k) / 2;
    at(xTime, headY, headScale);
    piece(mode, HEAD_LISTS, arrivals ? 1 : 0, fr, KIND_HEAD_LEFT, arrivals ? 1 : 0, false, 0);
    GlStateManager.popMatrix();
    at(xGate, headY, headScale);
    piece(mode, HEAD_LISTS, 2, fr, KIND_HEAD_RIGHT, 0, false, 0);
    GlStateManager.popMatrix();

    if (pages > 1) {
      float clockRight = sw - EDGE * k;
      float clockWidth = fr.getStringWidth("00:00") * clockScale;
      at(clockRight - clockWidth - 4 * EDGE * k, (header - HEAD_TEXT_H * k) / 2, headScale);
      int key = page * 64 + pages;
      piece(mode, PAGE_LISTS, key, fr, KIND_PAGE, key, false, 0);
      GlStateManager.popMatrix();
    }

    for (int i = 0; i < shown.getCount(); i++) {
      long slot = shown.getSlot(i);
      int daily = FlightSchedule.dailyOf(slot);
      int remark = shown.getRemark(i);
      float rowY = header + colhead + i * pitch + (pitch - TEXT_H * k) / 2;
      int flight = (arrivals ? 1000 : 0) + daily;
      TileEntityFlightBoardRenderer.glColour(ROW_COLOUR);
      at(xTime, rowY, rowScale);
      piece(mode, LEFT_LISTS, flight, fr, KIND_LEFT, daily, arrivals, 0);
      GlStateManager.popMatrix();
      at(xCity, rowY, rowScale);
      piece(mode, CITY_LISTS, (long) flight * 4096 + cityUnits, fr, KIND_CITY, daily, arrivals,
          cityUnits);
      GlStateManager.popMatrix();
      int gate = FlightSchedule.gateValue(slot, arrivals);
      at(xGate, rowY, rowScale);
      piece(mode, GATE_LISTS, gate, fr, KIND_GATE, gate, arrivals, 0);
      GlStateManager.popMatrix();
      TileEntityFlightBoardRenderer.glColour(STATUSES[remark].getColour());
      at(xRemark, rowY, rowScale);
      piece(mode, STATUS_LISTS, remark, fr, KIND_STATUS, remark, arrivals, 0);
      GlStateManager.popMatrix();
    }
  }

  /** One piece of text, as {@link #text}'s mode says. */
  private static void piece(int mode, CsmSharedDisplayLists lists, long key, CsmFontRenderer fr,
      int kind, int value, boolean arrivals, int room) {
    if (mode == TEXT_PER_FRAME) {
      add(fr, kind, value, arrivals, room);
      return;
    }
    int list = lists.get(key);
    if (mode == TEXT_COMPILE_PIECES) {
      if (list == CsmDisplayListCache.NO_LIST) {
        list = lists.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          add(fr, kind, value, arrivals, room);
          GL11.glEndList();
        }
      }
    } else if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // no list for this piece (the driver refused one): its geometry instead
      add(fr, kind, value, arrivals, room);
    }
  }

  /** Releases a board's lists, when a cell's tile entity goes. */
  public static void release(BlockPos pos) {
    BOARDS.invalidate(pos);
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
  private static void screen(TextureAtlasSprite sprite, int width, int height) {
    float sw = 16f * width - 2 * INSET;
    float sh = 16f * height - 2 * INSET;
    float k = scale(width, height);
    float header = HEADER * k;
    float colhead = COLHEAD * k;
    int rows = rows(width, height);
    float pitch = (sh - header - colhead) / rows;
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
