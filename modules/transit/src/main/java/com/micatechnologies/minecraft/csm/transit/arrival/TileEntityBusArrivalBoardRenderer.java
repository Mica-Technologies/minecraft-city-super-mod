package com.micatechnologies.minecraft.csm.transit.arrival;

import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.BAND;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_HALF_D;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_HALF_W;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_INNER;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_MAP_Y0;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_MAP_Y1;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_NAME_Y0;
import static com.micatechnologies.minecraft.csm.transit.arrival.BoardShape.TOTEM_NAME_Y1;

import com.micatechnologies.minecraft.csm.codeutils.CsmCompileBudget;
import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.transit.stop.BusDepartures;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusArrivalDisplayRenderer;
import com.micatechnologies.minecraft.csm.transit.stop.TransitRoute;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a large arrival board or the info totem ({@link BlockBusArrivalBoard}) from its master:
 * the housing, the rods or posts, the totem's plinth, name panel and map, and the screen's text,
 * on one face or both.
 *
 * <p><b>Frame.</b> Everything is laid out in sixteenths in the reader's space: x to the reader's
 * right from the master's column, y up from the master's floor, z toward the reader from the
 * middle of the block; the facing turns it into the world.</p>
 *
 * <p><b>Geometry</b> (housing, top band, screen ground, rods, posts, the totem's body) is one
 * display list shared by every board of the same look ({@link CsmSharedDisplayLists}), keyed on
 * the size, the sidedness, the mount, the rods' and posts' lengths and the agency's band colour
 * ({@link #geometryKey}). It is drawn on {@code white1px} with each face's colour and shade on its
 * vertices, so the list has no atlas UVs to go stale when the block atlas is stitched again, and
 * the world's light at the board is set as the lightmap before the call, never baked in. The
 * totem's map, the one textured piece, is a quad drawn each frame on the block atlas.</p>
 *
 * <p><b>Text</b> is one flat {@code POSITION_TEX_COLOR} list per board per page, every glyph
 * placed and coloured at compile time ({@link CsmFontRenderer#addString(BufferBuilder, String,
 * int, int, float, float, float, int)}), lit fullbright, kept in {@link #BOARDS} under the page's
 * number and compiled again when the page's content key ({@link #contentKey}: the data's version,
 * the stop looked up, and for a route page the countdown's minute) moves. A stale list waits its
 * turn through {@link CsmCompileBudget}; a missing one compiles at once. A two-sided board replays
 * the same list turned half round. The lists go with the tile entity (removal, chunk unload) and on
 * disconnect.</p>
 *
 * <p><b>Pages and sizes.</b> The pages turn every
 * {@link TileEntityBusArrivalDisplayRenderer#PAGE_TICKS}, every board in step; their words are
 * {@link ArrivalBoardText}'s. The text is as large as the screen's line slots allow and as narrow
 * as its widest line needs, one size through every page; a line still too long at
 * {@value #MIN_FIT} of that is cut. The 3 x 1 lists {@value #PER_PAGE_SMALL} routes a page under
 * its header (text about 2.4 px high, legible from a few blocks), the 5 x 2 up to
 * {@value #PER_PAGE_LARGE}, the totem's panel {@value #PER_PAGE_TOTEM}; more routes than that turn
 * over more pages.</p>
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class TileEntityBusArrivalBoardRenderer
    extends TileEntitySpecialRenderer<TileEntityBusArrivalBoard> {

  /** The most pages a board has a list for. */
  public static final int MAX_PAGES = 8;

  static final int PER_PAGE_SMALL = 2;
  static final int PER_PAGE_LARGE = 6;
  static final int PER_PAGE_TOTEM = 3;
  /** The fewest line slots a screen is laid out for: header, blank, direction. */
  private static final int MIN_SLOTS = 3;
  /** A line too long for the screen may shrink to this share of the slot's size, then is cut. */
  static final float MIN_FIT = 0.55f;
  /** The text's height in its slot. */
  private static final float TEXT_FILL = 0.74f;

  /** The screen text's amber, as the small arrival display's. */
  static final int AMBER = 0xFFA31A;
  private static final int HOUSING = 0x2B2E33;
  private static final int SCREEN = 0x08090A;
  private static final int STEEL = 0x8C9096;
  private static final int CONCRETE = 0x8E8C86;

  /** How far the screen and its text stand off the housing's face, px. */
  private static final float SCREEN_LIFT = 0.03f;
  private static final float TEXT_LIFT = 0.06f;
  private static final float ROD = 0.55f;
  private static final float POST_HALF = 1.25f;

  private static final ResourceLocation WHITE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final String MAP_SPRITE = "csm:blocks/transit/platforms/network_map";

  private static final CsmSharedDisplayLists GEOMETRY =
      new CsmSharedDisplayLists("bus_arrival_board_geometry");
  /** Each board's text, one list a page, under the page's number. */
  private static final CsmDisplayListCache BOARDS = new CsmDisplayListCache("bus arrival boards");

  private static final int[] MINUTES = new int[TransitRoute.MAX_PER_LINE];

  @Override
  public boolean isGlobalRenderer(TileEntityBusArrivalBoard te) {
    return true;
  }

  @Override
  public void render(TileEntityBusArrivalBoard te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshView();
    BlockBusArrivalBoard board = te.getBoard();
    if (board == null) {
      return;
    }
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.electronicSign();
    BusStopInfo info = te.getInfo();
    BoardMount mount = te.getMount();
    boolean totem = board.isTotem();
    // a board on a wall has its back to the wall, whatever it is set to
    boolean twoSided = mount != BoardMount.WALL && info.isTwoSided(false);
    BoardShape shape = te.getShape();

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.scale(1 / 16.0, 1 / 16.0, 1 / 16.0);

    float lastX = OpenGlHelper.lastBrightnessX;
    float lastY = OpenGlHelper.lastBrightnessY;
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    int light = te.getLight();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >>> 16);

    // the housing, rods, posts: one shared list a look, on white, lit by the world
    int band = info.getAgency().ground(info);
    bindTexture(WHITE);
    geometry(shape, twoSided, te.getRodPx(), te.getPostPx(), band);

    if (totem) {
      bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      map(shape, twoSided);
    }

    // the text, lit
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    int perPage = totem ? PER_PAGE_TOTEM : board.getHeight() >= 2 ? PER_PAGE_LARGE
        : PER_PAGE_SMALL;
    int count = te.getRouteCount();
    int pages = Math.min(MAX_PAGES, ArrivalBoardText.pages(info, count, perPage));
    long time = te.getWorld().getTotalWorldTime();
    int page = pages <= 1 ? 0
        : (int) ((time / TileEntityBusArrivalDisplayRenderer.PAGE_TICKS) % pages);
    long minute = BusDepartures.minuteOf(time);
    long key = contentKey(info, te.getSourceVersion(),
        ArrivalBoardText.isRoutePage(info, count, perPage, page) ? minute : -1L);
    int list = textList(te, fr, shape, perPage, page, minute, key);

    for (int side = 0; side < (twoSided ? 2 : 1); side++) {
      GlStateManager.pushMatrix();
      if (side == 1) {
        GlStateManager.rotate(180F, 0F, 1F, 0F);
      }
      GlStateManager.translate(shape.screenX0, shape.screenY1, shape.frontZ + TEXT_LIFT);
      GlStateManager.scale(1, -1, 1);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name (or the per-frame toggle is on): draw directly.
        text(te, fr, shape, perPage, page, minute);
      }
      GlStateManager.popMatrix();
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

  /** Releases a board's lists, when its tile entity goes. */
  public static void release(BlockPos pos) {
    BOARDS.invalidate(pos);
  }

  /**
   * What a page's text is drawn from: the data's version, the stop looked up, and the countdown's
   * minute ({@code -1} for a page that does not count down).
   */
  static long contentKey(BusStopInfo info, int sourceVersion, long minute) {
    long k = info.getVersion() * 0x9E3779B97F4A7C15L;
    k ^= (sourceVersion + 0x632BE59BD9B4E019L) * 0xC2B2AE3D27D4EB4FL;
    k ^= (minute + 0x165667B19E3779F9L) * 0xFF51AFD7ED558CCDL;
    return k;
  }

  /** The page's list, compiled again if its content moved and the frame's budget allows. */
  private static int textList(TileEntityBusArrivalBoard te, CsmFontRenderer fr, BoardShape shape,
      int perPage, int page, long minute, long key) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      return CsmDisplayListCache.NO_LIST;
    }
    int list = BOARDS.get(te.getPos(), page);
    boolean stale = te.compiledKey[page] != key;
    if (list == CsmDisplayListCache.NO_LIST || stale && CsmCompileBudget.mayRecompile()) {
      list = BOARDS.allocate(te.getPos(), page);
      if (list != CsmDisplayListCache.NO_LIST) {
        long started = CsmCompileBudget.begin();
        GL11.glNewList(list, GL11.GL_COMPILE);
        text(te, fr, shape, perPage, page, minute);
        GL11.glEndList();
        CsmCompileBudget.end(started);
        te.compiledKey[page] = key;
      }
    }
    return list;
  }

  // --- text -----------------------------------------------------------------------------------

  /**
   * A page's text, every glyph placed in the screen's sixteenths (origin its top-left, y down)
   * and coloured per vertex, in one draw; the totem's name sits above the screen.
   */
  private static void text(TileEntityBusArrivalBoard te, CsmFontRenderer fr, BoardShape shape,
      int perPage, int page, long minute) {
    BusStopInfo info = te.getInfo();
    int count = te.getRouteCount();
    int[] routes = te.getRoutes();
    for (int i = 0; i < count; i++) {
      MINUTES[i] = BusDepartures.minutesToNext(routes[i], te.getSeedX(), te.getSeedZ(), minute);
    }
    boolean compact = shape.totem;
    List<List<ArrivalBoardText.Line>> all = ArrivalBoardText.allPages(info, routes, MINUTES,
        count, perPage, compact);
    List<ArrivalBoardText.Line> lines = all.get(Math.floorMod(page, all.size()));
    float sw = shape.screenX1 - shape.screenX0;
    float sh = shape.screenY1 - shape.screenY0;
    int slots = ArrivalBoardText.slots(info, count, perPage, compact, MIN_SLOTS);
    float pitch = sh / slots;
    float base = pitch * TEXT_FILL / fr.FONT_HEIGHT;
    float gap = fr.getStringWidth("  ");
    float readingW = Math.max(fr.getStringWidth(ArrivalBoardText.WIDEST_READING),
        fr.getStringWidth(ArrivalBoardText.NO_BUS));
    // one size for the body through every page, as wide as its widest line needs
    float widest = 1;
    float headerW = 1;
    for (List<ArrivalBoardText.Line> p : all) {
      for (ArrivalBoardText.Line l : p) {
        if (l.kind == ArrivalBoardText.KIND_HEADER) {
          headerW = Math.max(headerW, fr.getStringWidth(l.left));
        } else {
          widest = Math.max(widest, fr.getStringWidth(l.left)
              + (l.right.isEmpty() ? 0 : gap + readingW));
        }
      }
    }
    float bodyScale = Math.max(base * MIN_FIT, Math.min(base, sw / widest));
    float headerScale = Math.max(base * MIN_FIT, Math.min(base, sw / headerW));
    int header = info.getAgency().screenColour(info);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int i = 0; i < lines.size() && i < slots; i++) {
      ArrivalBoardText.Line l = lines.get(i);
      boolean head = l.kind == ArrivalBoardText.KIND_HEADER;
      float s = head ? headerScale : bodyScale;
      int rgb = head ? header : AMBER;
      float top = i * pitch + (pitch - fr.FONT_HEIGHT * s) / 2;
      if (l.kind == ArrivalBoardText.KIND_CENTRED) {
        String t = fit(fr, l.left, sw / s);
        fr.addString(buf, t, 0, 0, (sw - fr.getStringWidth(t) * s) / 2, top, s, rgb);
        continue;
      }
      float room = sw / s - (l.right.isEmpty() ? 0 : gap + fr.getStringWidth(l.right));
      fr.addString(buf, fit(fr, l.left, room), 0, 0, 0, top, s, rgb);
      if (!l.right.isEmpty()) {
        fr.addString(buf, l.right, 0, 0, sw - fr.getStringWidth(l.right) * s, top, s, rgb);
      }
    }
    if (shape.totem) {
      name(fr, buf, info, shape);
    }
    tess.draw();
  }

  /**
   * The totem's stop name on its name panel, in the agency's lettering: one line, or two split
   * at a space if one would be too small.
   */
  private static void name(CsmFontRenderer fr, BufferBuilder buf, BusStopInfo info,
      BoardShape shape) {
    String name = info.getStopName().isEmpty() ? info.getAgency().title(info)
        : info.getStopName();
    if (name.isEmpty()) {
      return;
    }
    int rgb = info.getAgency().legend(info);
    float w = shape.screenX1 - shape.screenX0;
    float h = TOTEM_NAME_Y1 - TOTEM_NAME_Y0 - 1f;
    // the panel's top, in the screen's y-down frame
    float top = shape.screenY1 - (TOTEM_NAME_Y1 - 0.5f);
    float one = Math.min(h * 0.8f / fr.FONT_HEIGHT, w / Math.max(1, fr.getStringWidth(name)));
    int split = name.lastIndexOf(' ', name.length() / 2 + 2);
    if (split <= 0) {
      split = name.indexOf(' ');
    }
    if (split > 0 && one < h * 0.8f / fr.FONT_HEIGHT * 0.6f) {
      String a = name.substring(0, split).trim();
      String b = name.substring(split + 1).trim();
      float two = Math.min(h * 0.45f / fr.FONT_HEIGHT,
          w / Math.max(1, Math.max(fr.getStringWidth(a), fr.getStringWidth(b))));
      if (two > one) {
        float lineH = h / 2;
        fr.addString(buf, a, 0, 0, (w - fr.getStringWidth(a) * two) / 2,
            top + (lineH - fr.FONT_HEIGHT * two) / 2, two, rgb);
        fr.addString(buf, b, 0, 0, (w - fr.getStringWidth(b) * two) / 2,
            top + lineH + (lineH - fr.FONT_HEIGHT * two) / 2, two, rgb);
        return;
      }
    }
    fr.addString(buf, name, 0, 0, (w - fr.getStringWidth(name) * one) / 2,
        top + (h - fr.FONT_HEIGHT * one) / 2, one, rgb);
  }

  /** The text cut short to fit a width in the font's units. Compile time only. */
  static String fit(CsmFontRenderer fr, String text, float units) {
    String out = text;
    while (out.length() > 1 && fr.getStringWidth(out) > units) {
      out = out.substring(0, out.length() - 1).trim();
    }
    return out;
  }

  // --- geometry -------------------------------------------------------------------------------

  /**
   * What the shared geometry list is drawn from, packed: the size and kind, the sidedness, the
   * mount, the rods' and posts' lengths in sixteenths and the band's colour.
   */
  static long geometryKey(BoardShape shape, boolean twoSided, int rodPx, int postPx, int band) {
    return (shape.totem ? 1L : 0L)
        | (shape.width & 7L) << 1
        | (shape.height & 3L) << 4
        | (twoSided ? 1L : 0L) << 6
        | (shape.mount.ordinal() & 3L) << 7
        | (Math.min(rodPx, 511) & 511L) << 9
        | (Math.min(postPx, 511) & 511L) << 18
        | (band & 0xFFFFFFL) << 27;
  }

  private static void geometry(BoardShape shape, boolean twoSided, int rodPx, int postPx, int band) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      addGeometry(shape, twoSided, rodPx, postPx, band);
      return;
    }
    long key = geometryKey(shape, twoSided, rodPx, postPx, band);
    int list = GEOMETRY.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = GEOMETRY.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        addGeometry(shape, twoSided, rodPx, postPx, band);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      addGeometry(shape, twoSided, rodPx, postPx, band);
    }
  }

  /** The geometry's quads, coloured and shaded per vertex, on white. Geometry only. */
  private static void addGeometry(BoardShape s, boolean twoSided, int rodPx, int postPx, int band) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    if (s.totem) {
      box(buf, -7, 0, -5, 7, 3, 5, CONCRETE);
      box(buf, -TOTEM_HALF_W, 3, -TOTEM_HALF_D, TOTEM_HALF_W, TOTEM_NAME_Y0, TOTEM_HALF_D,
          HOUSING);
      box(buf, -TOTEM_HALF_W, TOTEM_NAME_Y0, -TOTEM_HALF_D, TOTEM_HALF_W, TOTEM_NAME_Y1,
          TOTEM_HALF_D, band);
      box(buf, -6.5f, TOTEM_NAME_Y1, -3, 6.5f, 48, 3, HOUSING);
    } else {
      box(buf, s.x0, s.y0, s.backZ, s.x1, s.y1 - BAND, s.frontZ, HOUSING);
      box(buf, s.x0, s.y1 - BAND, s.backZ, s.x1, s.y1, s.frontZ, band);
      float midZ = (s.backZ + s.frontZ) / 2;
      if (s.mount == BoardMount.HUNG) {
        for (float rx : s.rodX) {
          box(buf, rx - ROD, s.y1, midZ - ROD, rx + ROD, s.y1 + rodPx, midZ + ROD, STEEL);
        }
      } else if (s.mount == BoardMount.POST) {
        float bottom = s.y0 - postPx;
        for (float px : s.postX) {
          box(buf, px - POST_HALF, bottom, midZ - POST_HALF, px + POST_HALF, s.y0, midZ + POST_HALF,
              HOUSING);
          box(buf, px - 3, bottom, midZ - 3, px + 3, bottom + 0.6f, midZ + 3, STEEL);
        }
      }
    }
    // the dark screen, on the front and (two-sided) the back
    quadZ(buf, s.screenX0, s.screenY0, s.screenX1, s.screenY1, s.frontZ + SCREEN_LIFT, SCREEN);
    if (twoSided) {
      quadZ(buf, -s.screenX1, s.screenY0, -s.screenX0, s.screenY1, s.backZ - SCREEN_LIFT,
          SCREEN);
    }
    tess.draw();
  }

  /** The totem's map, on the block atlas, under the screen: each frame, one or two quads. */
  private static void map(BoardShape s, boolean twoSided) {
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite(MAP_SPRITE);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int side = 0; side < (twoSided ? 2 : 1); side++) {
      float m = side == 0 ? 1 : -1;
      float z = m * (TOTEM_HALF_D + SCREEN_LIFT);
      float xl = -TOTEM_INNER * m;
      float xr = TOTEM_INNER * m;
      float u0 = sprite.getMinU();
      float u1 = sprite.getMaxU();
      float v0 = sprite.getMinV();
      float v1 = sprite.getMaxV();
      buf.pos(xl, TOTEM_MAP_Y0, z).tex(u0, v1).color(255, 255, 255, 255).endVertex();
      buf.pos(xr, TOTEM_MAP_Y0, z).tex(u1, v1).color(255, 255, 255, 255).endVertex();
      buf.pos(xr, TOTEM_MAP_Y1, z).tex(u1, v0).color(255, 255, 255, 255).endVertex();
      buf.pos(xl, TOTEM_MAP_Y1, z).tex(u0, v0).color(255, 255, 255, 255).endVertex();
    }
    tess.draw();
  }

  /** A quad facing +z (or -z behind), x0..x1 by y0..y1 at z, flat colour. */
  private static void quadZ(BufferBuilder buf, float x0, float y0, float x1, float y1, float z,
      int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    buf.pos(x0, y0, z).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
    buf.pos(x1, y0, z).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
    buf.pos(x1, y1, z).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
    buf.pos(x0, y1, z).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
  }

  /** A box, each face shaded as the world's directional light would. */
  private static void box(BufferBuilder buf, float x0, float y0, float z0, float x1, float y1,
      float z1, int rgb) {
    // up, down
    face(buf, rgb, 1.0f, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
    face(buf, rgb, 0.5f, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    // front (+z), back (-z)
    face(buf, rgb, 0.8f, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
    face(buf, rgb, 0.8f, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
    // right (+x), left (-x)
    face(buf, rgb, 0.6f, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
    face(buf, rgb, 0.6f, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
  }

  private static void face(BufferBuilder buf, int rgb, float shade, float ax, float ay, float az,
      float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
    int r = (int) (((rgb >> 16) & 0xFF) * shade);
    int g = (int) (((rgb >> 8) & 0xFF) * shade);
    int b = (int) ((rgb & 0xFF) * shade);
    buf.pos(ax, ay, az).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
    buf.pos(bx, by, bz).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
    buf.pos(cx, cy, cz).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
    buf.pos(dx, dy, dz).tex(0.5, 0.5).color(r, g, b, 255).endVertex();
  }
}
