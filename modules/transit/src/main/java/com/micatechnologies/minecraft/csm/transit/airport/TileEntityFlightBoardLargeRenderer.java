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

import com.micatechnologies.minecraft.csm.codeutils.CsmCompileBudget;
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
 * <p><b>The board's lists.</b> Each board has two lists of its own ({@link #BOARDS}, on its
 * controller), one texture each: the screen, on the block atlas, compiled again when the board's
 * size or list changes or the atlas moves its sprite; and its heads, page label and rows on the
 * font atlas, one flat list of glyph quads placed and scaled into the screen's sixteenths and
 * coloured per vertex ({@link CsmFontRenderer#addString(BufferBuilder, String, int, int, float,
 * float, float, int)}), in one draw. Drawing the pieces one by one cost about 100 us a board; a
 * board list that called a shared list per piece, about 45 us, nearly all of it the driver
 * replaying the nested calls in {@code glCallList}. Following "Display lists: one texture, no
 * cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}, the atlas, the depth mask and the
 * fullbright lightmap are set outside the lists, every frame, and
 * {@code GlStateManager.resetColor()} follows the text, since its vertex colours leave GL's
 * current colour where the state manager's cache does not know it.</p>
 *
 * <p>The text list is compiled again in place only when {@link FlightBoardPage}'s version moves:
 * a flight leaves the page, a remark changes, the page turns. Every board changes on the same
 * game minute and turns its page on the same tick, so a board whose list is out of date asks
 * {@link CsmCompileBudget} first and draws its old list until its turn comes. The clock, which
 * changes every game minute, is a list shared by every large board (keyed by the minute of the
 * day) and is drawn outside them. The lists are released with the controller's tile entity
 * (removal and chunk unload) and on disconnect.</p>
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

  /** The clock, one list a minute of the day, shared by every large board. */
  private static final CsmSharedDisplayLists CLOCK_LISTS =
      new CsmSharedDisplayLists("flight_board_large_clock");

  /**
   * Each board's two lists, on its controller: its screen ({@link #SCREEN_KEY}) and its heads,
   * page and rows as one flat list of glyph quads ({@link #TEXT_KEY}). Each is compiled again in
   * place when what it shows changes, so a board holds two lists whatever its size.
   */
  private static final CsmDisplayListCache BOARDS =
      new CsmDisplayListCache("large flight boards");
  private static final long TEXT_KEY = 0L;
  private static final long SCREEN_KEY = 1L;

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

    // The heads, the page and the rows: one flat list per board, compiled again only when a
    // row, a remark or the page changes, and then only when this frame's compile budget allows.
    if (CsmRenderToggles.sharedBakesPerFrame) {
      text(fr, shown, arrivals, width, height, page, pages);
    } else {
      int list = BOARDS.get(te.getPos(), TEXT_KEY);
      boolean stale = te.clientCompiledVersion != shown.getVersion();
      if (list == CsmDisplayListCache.NO_LIST || stale && CsmCompileBudget.mayRecompile()) {
        list = BOARDS.allocate(te.getPos(), TEXT_KEY);
        if (list != CsmDisplayListCache.NO_LIST) {
          long started = CsmCompileBudget.begin();
          GL11.glNewList(list, GL11.GL_COMPILE);
          text(fr, shown, arrivals, width, height, page, pages);
          GL11.glEndList();
          CsmCompileBudget.end(started);
          te.clientCompiledVersion = shown.getVersion();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        text(fr, shown, arrivals, width, height, page, pages);
      }
    }

    // The clock, which changes every game minute, outside the board's list.
    int minuteOfDay = (int) Math.floorMod(now, 24L * 60L);
    float clockScale = TileEntityFlightBoardRenderer.clockScale * k;
    GlStateManager.pushMatrix();
    GlStateManager.translate(sw - EDGE * k, (header - CLOCK_TEXT_H * k) / 2, 0);
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
   * What a board's text list is drawn from besides its rows, packed: its size, the page shown and
   * how many there are (the page label). {@link FlightBoardPage} moves its version when it changes.
   */
  static long layoutKey(int width, int height, int page, int pages) {
    return (width & 0xFFFFL) | (height & 0xFFFFL) << 16 | (page & 0xFFFFL) << 32
        | (pages & 0xFFFFL) << 48;
  }

  /**
   * The column heads, the page label and the rows, every glyph placed in the screen's sixteenths
   * and coloured per vertex, in one draw: the geometry of a board's flat text list, or drawn
   * directly. Each piece sits where it sat when it was its own list placed and scaled under the
   * board's: its words the small board's gaps apart, in the font's units at its height.
   */
  private static void text(CsmFontRenderer fr, FlightBoardPage shown, boolean arrivals,
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
    // the gaps inside a piece, in the font's units at the small board's heights
    float rs = TileEntityFlightBoardRenderer.rowScale;
    float hs = TileEntityFlightBoardRenderer.headScale;
    float smallTime = TileEntityFlightBoardRenderer.xTime;
    int flightRow = Math.round((TileEntityFlightBoardRenderer.xFlight - smallTime) / rs);
    int flightHead = Math.round((TileEntityFlightBoardRenderer.xFlight - smallTime) / hs);
    int cityHead = Math.round((TileEntityFlightBoardRenderer.xCity - smallTime) / hs);
    int remarksHead = Math.round((TileEntityFlightBoardRenderer.xRemark
        - TileEntityFlightBoardRenderer.xGate) / hs);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

    float headY = header + (colhead - HEAD_TEXT_H * k) / 2;
    fr.addString(buf, "TIME", 0, 0, xTime, headY, headScale, HEAD_COLOUR);
    fr.addString(buf, "FLIGHT", flightHead, 0, xTime, headY, headScale, HEAD_COLOUR);
    fr.addString(buf, arrivals ? "FROM" : "TO", cityHead, 0, xTime, headY, headScale,
        HEAD_COLOUR);
    fr.addString(buf, "GATE", 0, 0, xGate, headY, headScale, HEAD_COLOUR);
    fr.addString(buf, "REMARKS", remarksHead, 0, xGate, headY, headScale, HEAD_COLOUR);

    if (pages > 1) {
      // right-aligned to clear the clock, in the heads' colour
      float clockRight = sw - EDGE * k;
      float clockWidth = fr.getStringWidth("00:00") * clockScale;
      String text = "PAGE " + (page + 1) + "/" + pages;
      fr.addString(buf, text, -fr.getStringWidth(text), 0, clockRight - clockWidth - 4 * EDGE * k,
          (header - HEAD_TEXT_H * k) / 2, headScale, HEAD_COLOUR);
    }

    for (int i = 0; i < shown.getCount(); i++) {
      long slot = shown.getSlot(i);
      long daily = FlightSchedule.dailyOf(slot);
      float rowY = header + colhead + i * pitch + (pitch - TEXT_H * k) / 2;
      fr.addString(buf, FlightSchedule.clock(FlightSchedule.scheduled(daily, arrivals)), 0, 0,
          xTime, rowY, rowScale, ROW_COLOUR);
      fr.addString(buf, FlightSchedule.flight(daily, arrivals), flightRow, 0, xTime, rowY,
          rowScale, ROW_COLOUR);
      fr.addString(buf, TileEntityFlightBoardRenderer.fit(fr,
          FlightSchedule.city(daily, arrivals), cityUnits), 0, 0, xCity, rowY, rowScale,
          ROW_COLOUR);
      fr.addString(buf, FlightSchedule.gateName(FlightSchedule.gateValue(slot, arrivals)), 0, 0,
          xGate, rowY, rowScale, ROW_COLOUR);
      FlightSchedule.Status status = STATUSES[shown.getRemark(i)];
      fr.addString(buf, status.getText(), 0, 0, xRemark, rowY, rowScale, status.getColour());
    }
    tess.draw();
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

  /**
   * The clock, right-aligned to x = 0 in the font's units: one list a minute of the day, shared by
   * every large board.
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

  /** The clock's geometry, right-aligned to x = 0, in its colour. */
  private static void addClock(CsmFontRenderer fr, int minuteOfDay) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    String text = FlightSchedule.clock(minuteOfDay);
    fr.addString(buf, text, -fr.getStringWidth(text), 0, 0, 0, 1, CLOCK_COLOUR);
    tess.draw();
  }
}
