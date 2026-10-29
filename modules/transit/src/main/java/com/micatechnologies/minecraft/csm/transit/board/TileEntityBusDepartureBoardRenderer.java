package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.codeutils.CsmCompileBudget;
import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.transit.stop.BusAgency;
import com.micatechnologies.minecraft.csm.transit.stop.BusDepartures;
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
 * Draws a bus departure board's screen: the screen's own texture, lit, and on it the title, the
 * column heads and {@link #ROWS} departures -- route (in its agency's colour), destination, bay
 * and minutes -- from the stops the board can see ({@link BusStation}), starting at the board's
 * page within its bank ({@link TileEntityBusDepartureBoard}).
 *
 * <p><b>The screen.</b> As on the flight information board, the baked model carries the screen's
 * texture lit by the world, and this draws it again a hair in front, fullbright, one quad from the
 * block atlas, so a board glows at night. Beyond the render range only the baked screen shows.</p>
 *
 * <p><b>The text.</b> One flat display list per board ({@link #BOARDS}): the title, the page
 * count, the column heads and the rows, every glyph quad placed and scaled into the screen's
 * sixteenths and coloured per vertex ({@link CsmFontRenderer#addString(BufferBuilder, String,
 * int, int, float, float, float, int)}), in one draw. It replaced a shared list per piece of text
 * called one by one, each under its own matrix and colour: at most 36 calls a frame, the pattern
 * that cost the flight boards about 30 us each. What the board shows is kept on the tile entity
 * ({@link TileEntityBusDepartureBoard#clientShown}) and the list is compiled again in place when
 * it changes: the minutes count down once a game minute, a bus leaves, the bank turns its set.
 * Every board changes on the same game minute, so an out-of-date list waits for
 * {@link CsmCompileBudget} and is drawn as it was until its turn. Following "Display lists: one
 * texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}, the list holds the font atlas's
 * geometry only: the atlas, the depth mask and the fullbright lightmap are set outside it, every
 * frame, and {@code GlStateManager.resetColor()} follows it. The list is released with the tile
 * entity (removal and chunk unload) and on disconnect.</p>
 *
 * <p><b>Pages.</b> The bank shows the list page after page, left to right and then down; if the
 * list is longer than the bank, the whole bank turns to the next set every {@link #CYCLE_TICKS}
 * ticks, and each header shows which set ("1/3").</p>
 *
 * <p>The screen's place and bands ({@code SCREEN_*}, {@code HEADER_H}, {@code COLHEAD_H},
 * {@code ROW_PITCH}, {@code ROWS}, and the texture's 256 x 189 window) are
 * {@code gen_transit_boards.py}'s: change one, change both.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityBusDepartureBoardRenderer
    extends TileEntitySpecialRenderer<TileEntityBusDepartureBoard> {

  /** Departures a board lists: a page. */
  static final int ROWS = 8;
  /** How long a bank shows one set of departures when there are more than it has rows. */
  static final int CYCLE_TICKS = 200;
  /** The most sets a bank cycles through. */
  private static final int MAX_CYCLES = 12;

  /** The screen, facing north, in sixteenths: its reader's left edge (high x), top and face. */
  private static final float SCREEN_LEFT_X = 15.6f;
  private static final float SCREEN_TOP_Y = 13.6f;
  private static final float SCREEN_Z = 14.6f;
  private static final float WIDTH = 15.2f;
  private static final float HEIGHT = 11.2f;
  /**
   * How much of the texture's height the screen's window takes: a fraction of the sprite, measured
   * where the generator draws it (a 256 square) and true at the size it is stored at (128).
   */
  private static final float WINDOW_V = 189f / 256f;
  /** The bands down the screen, in sixteenths. */
  private static final float HEADER = 1.8f;
  private static final float COLHEAD = 0.8f;
  private static final float PITCH = 1.0f;
  /** Where the title starts, clear of the bus pictogram printed in the header. */
  private static final float TITLE_X = 2.3f;
  /** Text heights and spacing, in sixteenths. */
  private static final float TEXT_H = 0.56f;
  private static final float HEAD_TEXT_H = 0.4f;
  private static final float TITLE_TEXT_H = 0.8f;
  private static final float EDGE = 0.35f;
  private static final float GAP = 0.4f;
  /** How far in front of the baked screen the lit one and the text are, in sixteenths. */
  private static final float SCREEN_LIFT = 0.02f;
  private static final float TEXT_LIFT = 0.02f;

  private static final int TITLE_COLOUR = 0xF4F4F0;
  private static final int HEAD_COLOUR = 0xA8B0BC;
  private static final int ROW_COLOUR = 0xF2F2EE;
  private static final int MINUTES_COLOUR = 0xFFC032;
  private static final int DUE_COLOUR = 0x6AE08A;
  private static final int EMPTY_COLOUR = 0x8A929E;

  /** Each board's text as one flat list of glyph quads, at key 0. */
  private static final CsmDisplayListCache BOARDS = new CsmDisplayListCache("bus boards");

  /**
   * What a board shows, packed as {@link #shown} packs it: four values and four a row. The length
   * of {@link TileEntityBusDepartureBoard#clientShown}.
   */
  static final int SHOWN_LENGTH = 4 + 4 * ROWS;

  /** What the board being drawn shows: scratch, render thread only. */
  private static final int[] SHOWN = new int[SHOWN_LENGTH];

  /** The layout, in the font's units at each text height, worked out once the font exists. */
  private static float rowScale;
  private static float headScale;
  private static float titleScale;
  private static float xRoute;
  private static float xDestination;
  private static float xBay;
  private static float xMinutesRight;
  private static float destinationWidth;

  @Override
  public void render(TileEntityBusDepartureBoard te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshView();
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    layout(fr);
    long time = te.getWorld().getTotalWorldTime();
    BusStation station = BusStation.at(te.getWorld(), te.getPos());
    int filter = te.getFilter();
    BusStation.Departures list = station.departures(filter, BusDepartures.minuteOf(time));
    int capacity = Math.max(1, te.getBankSize()) * ROWS;
    int cycles = Math.max(1, Math.min(MAX_CYCLES, (list.count() + capacity - 1) / capacity));
    int cycle = (int) ((time / CYCLE_TICKS) % cycles);
    int first = cycle * capacity + te.getPage() * ROWS;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // Turned so the model's front (north) faces +z, as the flight board's renderer does: the
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

    screen();

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.translate(0, 0, SCREEN_LIFT + TEXT_LIFT);

    int length = shown(station, list, filter, te.getPage(), first, cycle, cycles, SHOWN);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      text(fr, SHOWN);
    } else {
      // One flat list per board, compiled again when what it shows changes (once a game minute
      // at most, as the minutes count down), and then only when this frame's budget allows.
      int boardList = BOARDS.get(te.getPos(), 0L);
      boolean stale = !same(te, SHOWN, length);
      if (boardList == CsmDisplayListCache.NO_LIST || stale && CsmCompileBudget.mayRecompile()) {
        boardList = BOARDS.allocate(te.getPos(), 0L);
        if (boardList != CsmDisplayListCache.NO_LIST) {
          long started = CsmCompileBudget.begin();
          GL11.glNewList(boardList, GL11.GL_COMPILE);
          text(fr, SHOWN);
          GL11.glEndList();
          CsmCompileBudget.end(started);
          System.arraycopy(SHOWN, 0, te.clientShown, 0, length);
          te.clientShownLength = length;
        }
      }
      if (boardList != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(boardList);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        text(fr, SHOWN);
      }
    }
    // The text's colours are per vertex, which leaves GL's current colour where the state
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

  /** The screen's texture, one quad a hair in front of the baked one. */
  private void screen() {
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite("csm:blocks/transit/boards/bus_board_screen");
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

  /**
   * Packs what a board shows into {@code out}: the filter (the title), the set shown and how many
   * there are (0 when there is one, as no count is drawn), the empty-board notice (-1 for none,
   * else which), the row count, then each row's agency, route, bay and minutes.
   *
   * @return how many values were written
   */
  private static int shown(BusStation station, BusStation.Departures list, int filter, int page,
      int first, int cycle, int cycles, int[] out) {
    out[0] = filter;
    out[1] = cycles > 1 ? cycle * 16 + cycles : 0;
    int rows = 0;
    if (list.count() == 0) {
      out[2] = page == 0 ? (station.bays() == 0 ? 0 : 1) : -1;
    } else {
      out[2] = -1;
      for (int i = 0; i < ROWS && first + i < list.count(); i++) {
        int n = first + i;
        out[4 + 4 * i] = list.agency(n);
        out[5 + 4 * i] = list.route(n);
        out[6 + 4 * i] = list.bay(n);
        out[7 + 4 * i] = Math.min(list.minutes(n), BusDepartures.MAX_MINUTES);
        rows++;
      }
    }
    out[3] = rows;
    return 4 + 4 * rows;
  }

  /** Whether the board's list was compiled for what it shows now. */
  private static boolean same(TileEntityBusDepartureBoard te, int[] shown, int length) {
    if (te.clientShownLength != length) {
      return false;
    }
    int[] was = te.clientShown;
    for (int i = 0; i < length; i++) {
      if (was[i] != shown[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * The title, the set count, the column heads and the rows (or the empty-board notice), every
   * glyph placed in the screen's sixteenths and coloured per vertex, in one draw: the geometry of
   * a board's flat list, or drawn directly.
   */
  private static void text(CsmFontRenderer fr, int[] shown) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

    float t = titleScale;
    float y = (HEADER - TITLE_TEXT_H) / 2;
    String title = shown[0] == 0 ? "BUS DEPARTURES"
        : TileEntityBusDepartureBoard.agencyTitle(shown[0]) + " DEPARTURES";
    fr.addString(buf, title, u(TITLE_X, t), 0, 0, y, t, TITLE_COLOUR);
    if (shown[1] != 0) {
      String text = (shown[1] / 16 + 1) + "/" + (shown[1] % 16);
      fr.addString(buf, text, u(WIDTH - EDGE, t) - fr.getStringWidth(text), 0, 0, y, t,
          TITLE_COLOUR);
    }

    float h = headScale;
    y = HEADER + (COLHEAD - HEAD_TEXT_H) / 2;
    fr.addString(buf, "ROUTE", u(xRoute, h), 0, 0, y, h, HEAD_COLOUR);
    fr.addString(buf, "DESTINATION", u(xDestination, h), 0, 0, y, h, HEAD_COLOUR);
    fr.addString(buf, "BAY", u(xBay, h), 0, 0, y, h, HEAD_COLOUR);
    fr.addString(buf, "DEPARTS", u(xMinutesRight, h) - fr.getStringWidth("DEPARTS"), 0, 0, y, h,
        HEAD_COLOUR);

    float s = rowScale;
    if (shown[2] >= 0) {
      y = HEADER + COLHEAD + (PITCH - TEXT_H) / 2;
      fr.addString(buf, shown[2] == 0 ? "NO BUS STOPS NEARBY" : "NO DEPARTURES LISTED",
          u(xDestination, s), 0, 0, y, s, EMPTY_COLOUR);
    }
    for (int i = 0; i < shown[3]; i++) {
      int agency = shown[4 + 4 * i];
      int route = shown[5 + 4 * i];
      int bay = shown[6 + 4 * i];
      int minutes = shown[7 + 4 * i];
      y = HEADER + COLHEAD + i * PITCH + (PITCH - TEXT_H) / 2;
      fr.addString(buf, Integer.toString(route), u(xRoute, s), 0, 0, y, s,
          BusAgency.values()[agency].getTextColour());
      fr.addString(buf, fit(fr, BusDepartures.DESTINATIONS[BusDepartures.destinationOf(route)],
          destinationWidth / s), u(xDestination, s), 0, 0, y, s, ROW_COLOUR);
      fr.addString(buf, Integer.toString(bay), u(xBay, s), 0, 0, y, s, ROW_COLOUR);
      String reading = minutes == 0 ? "DUE" : minutes + " MIN";
      fr.addString(buf, reading, u(xMinutesRight, s) - fr.getStringWidth(reading), 0, 0, y, s,
          minutes == 0 ? DUE_COLOUR : MINUTES_COLOUR);
    }
    tess.draw();
  }

  /** Releases a board's list, when its tile entity goes. */
  public static void release(BlockPos pos) {
    BOARDS.invalidate(pos);
  }

  private static void layout(CsmFontRenderer fr) {
    if (rowScale != 0) {
      return;
    }
    float s = TEXT_H / fr.FONT_HEIGHT;
    float h = HEAD_TEXT_H / fr.FONT_HEIGHT;
    xRoute = EDGE;
    xDestination = xRoute + Math.max(fr.getStringWidth("88") * s,
        fr.getStringWidth("ROUTE") * h) + GAP;
    xMinutesRight = WIDTH - EDGE;
    float minutesWidth = Math.max(fr.getStringWidth("32 MIN") * s,
        fr.getStringWidth("DEPARTS") * h);
    float bayWidth = Math.max(fr.getStringWidth("16") * s, fr.getStringWidth("BAY") * h);
    xBay = xMinutesRight - minutesWidth - GAP - bayWidth;
    destinationWidth = xBay - GAP - xDestination;
    headScale = h;
    titleScale = TITLE_TEXT_H / fr.FONT_HEIGHT;
    rowScale = s;
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
