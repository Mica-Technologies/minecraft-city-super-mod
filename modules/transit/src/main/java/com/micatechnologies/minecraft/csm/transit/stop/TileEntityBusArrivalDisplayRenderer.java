package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Draws a bus arrival display's text: three lines a page, each a route, where it is going and how
 * many minutes away it is, in amber dot-matrix, lit.
 *
 * <p><b>What is listed.</b> Each route the display lists ({@link TileEntityBusArrivalDisplay})
 * has a made-up headway of 6 to 16 minutes and a destination, both fixed by its number, so a
 * route goes to the same place at every stop; where in its cycle it is depends on the world's
 * clock and the stop's position, so neighbouring stops differ but every player at one stop sees
 * the same thing. The next two buses of each route are listed, soonest first, and the page turns
 * every {@link #PAGE_TICKS} ticks. A bus under a minute away reads DUE.</p>
 *
 * <p><b>How it is drawn.</b> The route numbers, the destinations and the minute readings are each
 * compiled once into a display list shared by every display ({@link CsmSharedDisplayLists}), laid
 * out within its column (the reading already right-aligned), so a line is three list calls under
 * one translation, and a frame's text is at most nine. There are 99 routes, 16 destinations and
 * 33 readings, so the lists are bounded. The atlas, the colour, the fullbright lightmap and the
 * depth mask are set outside the lists, every frame.</p>
 *
 * @since 2026.9
 */
public class TileEntityBusArrivalDisplayRenderer
    extends TileEntitySpecialRenderer<TileEntityBusArrivalDisplay> {

  /** How long a page shows, in ticks. */
  static final int PAGE_TICKS = 100;

  /** Ticks in a minute of the countdown: one real minute. */
  private static final long TICKS_PER_MINUTE = 1200;

  private static final int LINES = 3;
  /** Text height, line pitch and the margins at the screen's sides and top, in sixteenths. */
  private static final float TEXT_HEIGHT = 1.2f;
  private static final float LINE_PITCH = 1.5f;
  private static final float EDGE_X = 0.35f;
  private static final float EDGE_Y = 0.2f;
  /** How far in front of the screen the text sits, in sixteenths. */
  private static final float LIFT = 0.03f;

  /** Invented, and generic on purpose: no real place is named. */
  static final String[] DESTINATIONS = {
      "DOWNTOWN", "UPTOWN", "HARBOR", "AIRPORT", "UNIVERSITY", "STADIUM", "HOSPITAL",
      "RIVERSIDE", "OLD TOWN", "LAKESIDE", "TRANSIT CTR", "NORTH END", "SOUTH END", "WEST SIDE",
      "EAST SIDE", "CITY HALL"};

  /** The longest wait listed: the second bus of a route with the longest headway. */
  private static final int MAX_MINUTES = 32;

  private static final String[] ROUTE_TEXT = new String[TileEntityBusStopFlag.MAX_ROUTE + 1];
  private static final String[] MINUTE_TEXT = new String[MAX_MINUTES + 1];

  static {
    for (int i = 0; i < ROUTE_TEXT.length; i++) {
      ROUTE_TEXT[i] = Integer.toString(i);
    }
    MINUTE_TEXT[0] = "DUE";
    for (int i = 1; i < MINUTE_TEXT.length; i++) {
      MINUTE_TEXT[i] = i + " MIN";
    }
  }

  private static final CsmSharedDisplayLists ROUTE_LISTS =
      new CsmSharedDisplayLists("bus_arrival_routes");
  private static final CsmSharedDisplayLists DESTINATION_LISTS =
      new CsmSharedDisplayLists("bus_arrival_destinations");
  private static final CsmSharedDisplayLists MINUTE_LISTS =
      new CsmSharedDisplayLists("bus_arrival_minutes");

  /** The arrivals being drawn: scratch, render thread only. Two per route. */
  private static final int[] ARRIVAL_ROUTE = new int[TileEntityBusStopFlag.PLATES * 2];
  private static final int[] ARRIVAL_MINUTES = new int[TileEntityBusStopFlag.PLATES * 2];

  /** The layout in font units, worked out once the font exists. */
  private static float scale;
  private static float left;
  private static float right;
  private static float top;
  private static float destinationX;
  private static float destinationWidth;
  private static float lineStep;

  @Override
  public void render(TileEntityBusArrivalDisplay te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshView();
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.electronicSign();
    layout(fr);

    long time = te.getWorld().getTotalWorldTime();
    int count = arrivals(te, time / TICKS_PER_MINUTE);
    int pages = (count + LINES - 1) / LINES;
    int first = (int) ((time / PAGE_TICKS) % pages) * LINES;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y + te.getViewOffset(), z + 0.5);
    GlStateManager.rotate(TileEntityBusStopFlagRenderer.rotationOf(te), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    // the screen is on the north face, read by someone looking south: text runs toward -x
    GlStateManager.translate(BlockBusArrivalDisplay.SCREEN_MIDDLE_X,
        BlockBusArrivalDisplay.SCREEN_MIDDLE_Y, BlockBusArrivalDisplay.SCREEN_Z - LIFT);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(scale, -scale, scale);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.enableTexture2D();
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.color(1.0f, 0.64f, 0.1f, 1.0f);

    for (int line = 0; line < LINES && first + line < count; line++) {
      int route = ARRIVAL_ROUTE[first + line];
      GlStateManager.pushMatrix();
      GlStateManager.translate(left, top + line * lineStep, 0);
      draw(ROUTE_LISTS, route, fr, 0, route);
      GlStateManager.translate(destinationX, 0, 0);
      draw(DESTINATION_LISTS, destinationOf(route), fr, 1, destinationOf(route));
      GlStateManager.translate(right - left - destinationX, 0, 0);
      draw(MINUTE_LISTS, ARRIVAL_MINUTES[first + line], fr, 2, ARRIVAL_MINUTES[first + line]);
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

  /**
   * Fills the scratch arrays with the next two buses of every route, soonest first.
   *
   * @return how many arrivals there are
   */
  private static int arrivals(TileEntityBusArrivalDisplay te, long minute) {
    int seed = (te.getPos().getX() * 31 + te.getPos().getZ() * 17 + te.getPos().getY()) & 0xFFFF;
    int n = 0;
    for (int i = 0; i < te.getRouteCount(); i++) {
      int route = te.getRoute(i);
      int headway = headwayOf(route);
      int phase = (int) ((minute + route * 13L + seed) % headway);
      int next = headway - 1 - phase;
      n = insert(n, route, next);
      n = insert(n, route, next + headway);
    }
    return n;
  }

  private static int insert(int n, int route, int minutes) {
    int at = n;
    while (at > 0 && ARRIVAL_MINUTES[at - 1] > minutes) {
      ARRIVAL_MINUTES[at] = ARRIVAL_MINUTES[at - 1];
      ARRIVAL_ROUTE[at] = ARRIVAL_ROUTE[at - 1];
      at--;
    }
    ARRIVAL_MINUTES[at] = minutes;
    ARRIVAL_ROUTE[at] = route;
    return n + 1;
  }

  /** A route's minutes between buses, 6 to 16, fixed by its number. */
  static int headwayOf(int route) {
    return 6 + (route * 5) % 11;
  }

  /** A route's destination, fixed by its number. */
  static int destinationOf(int route) {
    return (route * 7 + 3) % DESTINATIONS.length;
  }

  private static void layout(CsmFontRenderer fr) {
    if (scale != 0) {
      return;
    }
    float s = TEXT_HEIGHT / fr.FONT_HEIGHT;
    float width = BlockBusArrivalDisplay.SCREEN_WIDTH / s;
    float height = BlockBusArrivalDisplay.SCREEN_HEIGHT / s;
    left = -width / 2 + EDGE_X / s;
    right = width / 2 - EDGE_X / s;
    top = -height / 2 + EDGE_Y / s;
    lineStep = LINE_PITCH / s;
    float space = fr.getStringWidth(" ");
    destinationX = fr.getStringWidth("88") + space;
    destinationWidth = right - left - destinationX - fr.getStringWidth(MINUTE_TEXT[MAX_MINUTES])
        - space;
    scale = s;
  }

  /**
   * Replays a text's list, compiling it the first time. kind 0 is a route number, 1 a
   * destination, 2 a minute reading.
   */
  private static void draw(CsmSharedDisplayLists lists, long key, CsmFontRenderer fr, int kind,
      int value) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      add(fr, kind, value);
      return;
    }
    int list = lists.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = lists.allocate(key);
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

  /** One text at the origin, its top-left there (a reading's top-right). Geometry only. */
  private static void add(CsmFontRenderer fr, int kind, int value) {
    String text;
    int x = 0;
    if (kind == 0) {
      text = ROUTE_TEXT[value];
    } else if (kind == 1) {
      text = fit(fr, DESTINATIONS[value], destinationWidth);
    } else {
      text = MINUTE_TEXT[Math.min(value, MAX_MINUTES)];
      x = -fr.getStringWidth(text);
    }
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, text, x, 0);
    tess.draw();
  }

  /** The text cut short to fit a width. Compile time only. */
  private static String fit(CsmFontRenderer fr, String text, float width) {
    String out = text;
    while (out.length() > 1 && fr.getStringWidth(out) > width) {
      out = out.substring(0, out.length() - 1).trim();
    }
    return out;
  }
}
