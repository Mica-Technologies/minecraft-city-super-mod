package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
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
 * <p><b>The text.</b> Every piece is a display list shared by every board
 * ({@link CsmSharedDisplayLists}), laid out in its column across the screen in the font's units
 * when compiled: route numbers (99), destinations (16), bays (16), minute readings (33), the
 * title for each filter (5), the column heads, the page count and the two empty-board notices.
 * A row is one translation and four list calls, a frame one quad and at most 36 list calls.
 * Following "Display lists: one texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md},
 * the lists hold geometry only: the font atlas, the colours, the depth mask and the fullbright
 * lightmap are set outside them, every frame.</p>
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
  /** How much of the texture's height the screen's window takes. */
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

  private static final CsmSharedDisplayLists LISTS = new CsmSharedDisplayLists("bus_board_text");

  /** Kinds of text; a list's key is its kind times 4096 plus its value. */
  private static final int KIND_ROUTE = 0;
  private static final int KIND_DESTINATION = 1;
  private static final int KIND_BAY = 2;
  private static final int KIND_MINUTES = 3;
  private static final int KIND_TITLE = 4;
  private static final int KIND_HEAD = 5;
  private static final int KIND_CYCLE = 6;
  private static final int KIND_EMPTY = 7;

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

    colour(TITLE_COLOUR);
    at(0, (HEADER - TITLE_TEXT_H) / 2, titleScale);
    draw(KIND_TITLE, filter, fr);
    if (cycles > 1) {
      draw(KIND_CYCLE, cycle * 16 + cycles, fr);
    }
    GlStateManager.popMatrix();

    colour(HEAD_COLOUR);
    at(0, HEADER + (COLHEAD - HEAD_TEXT_H) / 2, headScale);
    draw(KIND_HEAD, 0, fr);
    GlStateManager.popMatrix();

    if (list.count() == 0) {
      if (te.getPage() == 0) {
        colour(EMPTY_COLOUR);
        at(0, HEADER + COLHEAD + (PITCH - TEXT_H) / 2, rowScale);
        draw(KIND_EMPTY, station.bays() == 0 ? 0 : 1, fr);
        GlStateManager.popMatrix();
      }
    } else {
      for (int i = 0; i < ROWS && first + i < list.count(); i++) {
        int n = first + i;
        at(0, HEADER + COLHEAD + i * PITCH + (PITCH - TEXT_H) / 2, rowScale);
        colour(BusAgency.values()[list.agency(n)].getTextColour());
        draw(KIND_ROUTE, list.route(n), fr);
        colour(ROW_COLOUR);
        draw(KIND_DESTINATION, BusDepartures.destinationOf(list.route(n)), fr);
        draw(KIND_BAY, list.bay(n), fr);
        int minutes = Math.min(list.minutes(n), BusDepartures.MAX_MINUTES);
        colour(minutes == 0 ? DUE_COLOUR : MINUTES_COLOUR);
        draw(KIND_MINUTES, minutes, fr);
        GlStateManager.popMatrix();
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

  /** Replays a text's list, compiling it the first time. */
  private static void draw(int kind, int value, CsmFontRenderer fr) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      add(fr, kind, value);
      return;
    }
    long key = kind * 4096L + value;
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
      // The driver refused a list name: draw directly rather than calling list 0.
      add(fr, kind, value);
    }
  }

  /**
   * One piece of text, laid out in its place across the screen in the font's units at its
   * height. Geometry only.
   */
  private static void add(CsmFontRenderer fr, int kind, int value) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    switch (kind) {
      case KIND_ROUTE:
        fr.addString(buf, Integer.toString(value), u(xRoute, rowScale), 0);
        break;
      case KIND_DESTINATION:
        fr.addString(buf, fit(fr, BusDepartures.DESTINATIONS[value],
            destinationWidth / rowScale), u(xDestination, rowScale), 0);
        break;
      case KIND_BAY:
        fr.addString(buf, Integer.toString(value), u(xBay, rowScale), 0);
        break;
      case KIND_MINUTES: {
        String text = value == 0 ? "DUE" : value + " MIN";
        fr.addString(buf, text, u(xMinutesRight, rowScale) - fr.getStringWidth(text), 0);
        break;
      }
      case KIND_TITLE: {
        String text = value == 0 ? "BUS DEPARTURES"
            : TileEntityBusDepartureBoard.agencyTitle(value) + " DEPARTURES";
        fr.addString(buf, text, u(TITLE_X, titleScale), 0);
        break;
      }
      case KIND_HEAD: {
        float h = headScale;
        fr.addString(buf, "ROUTE", u(xRoute, h), 0);
        fr.addString(buf, "DESTINATION", u(xDestination, h), 0);
        fr.addString(buf, "BAY", u(xBay, h), 0);
        fr.addString(buf, "DEPARTS", u(xMinutesRight, h) - fr.getStringWidth("DEPARTS"), 0);
        break;
      }
      case KIND_CYCLE: {
        String text = (value / 16 + 1) + "/" + (value % 16);
        fr.addString(buf, text, u(WIDTH - EDGE, titleScale) - fr.getStringWidth(text), 0);
        break;
      }
      default:
        fr.addString(buf, value == 0 ? "NO BUS STOPS NEARBY" : "NO DEPARTURES LISTED",
            u(xDestination, rowScale), 0);
        break;
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
