package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Draws the route numbers on a bus stop flag's route plates, on both faces.
 *
 * <p>The plates themselves are baked; only the numbers are drawn here. Each number, 1 to 99, is
 * compiled once into a display list shared by every flag ({@link CsmSharedDisplayLists}, keyed
 * on the number), centred on the origin in the font atlas, and replayed under each plate's own
 * transform: a flag's six numbers cost six list calls. Nothing position-dependent is in a list:
 * the atlas is bound, the colour set and the depth mask cleared outside, every frame (see
 * "Display lists: one texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}), and the
 * lightmap is the block's own, which the dispatcher sets before calling this -- the numbers are
 * printed, not lit.</p>
 *
 * @since 2026.9
 */
public class TileEntityBusStopFlagRenderer
    extends TileEntitySpecialRenderer<TileEntityBusStopFlag> {

  /** How tall the numbers are, in sixteenths (a plate is 1.8). */
  private static final float TEXT_HEIGHT = 1.25f;

  /** How far in front of each face the numbers sit, in sixteenths. */
  private static final float LIFT = 0.03f;

  /** The route numbers, in the font atlas, keyed on the number. */
  private static final CsmSharedDisplayLists NUMBER_LISTS =
      new CsmSharedDisplayLists("bus_stop_route_numbers");

  /** The numbers as text, made once rather than per frame. */
  private static final String[] NUMBER_TEXT = new String[TileEntityBusStopFlag.MAX_ROUTE + 1];

  static {
    for (int i = 0; i < NUMBER_TEXT.length; i++) {
      NUMBER_TEXT[i] = Integer.toString(i);
    }
  }

  @Override
  public void render(TileEntityBusStopFlag te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    boolean any = false;
    for (int i = 0; i < TileEntityBusStopFlag.PLATES; i++) {
      any |= te.getRoute(i) != 0;
    }
    if (!any) {
      return;
    }
    te.refreshView();

    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    float scale = TEXT_HEIGHT / fr.FONT_HEIGHT;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y + te.getViewOffset(), z + 0.5);
    GlStateManager.rotate(rotationOf(te), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.enableTexture2D();
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.color(0.96f, 0.96f, 0.94f, 1.0f);

    for (int i = 0; i < TileEntityBusStopFlag.PLATES; i++) {
      int route = te.getRoute(i);
      if (route == 0) {
        continue;
      }
      float midY = BlockBusStopFlag.PLATE_MIDDLE_Y[i];
      // the north face, read by someone looking south: text runs toward -x
      GlStateManager.pushMatrix();
      GlStateManager.translate(BlockBusStopFlag.PLATE_MIDDLE_X, midY,
          BlockBusStopFlag.PLATE_NORTH_Z - LIFT);
      GlStateManager.rotate(180, 0, 1, 0);
      GlStateManager.scale(scale, -scale, scale);
      drawNumber(fr, route);
      GlStateManager.popMatrix();
      // the south face
      GlStateManager.pushMatrix();
      GlStateManager.translate(BlockBusStopFlag.PLATE_MIDDLE_X, midY,
          BlockBusStopFlag.PLATE_SOUTH_Z + LIFT);
      GlStateManager.scale(scale, -scale, scale);
      drawNumber(fr, route);
      GlStateManager.popMatrix();
    }

    GlStateManager.depthMask(true);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * The rotation, about the block's middle, that turns a north-facing drawing to the block's
   * facing: the blockstate's y rotation is clockwise from above, GL's is counter-clockwise.
   */
  static float rotationOf(AbstractTileEntityBusStopFitting te) {
    switch (te.getViewFacing()) {
      case EAST:
        return 270;
      case SOUTH:
        return 180;
      case WEST:
        return 90;
      default:
        return 0;
    }
  }

  private static void drawNumber(CsmFontRenderer fr, int route) {
    if (CsmRenderToggles.sharedBakesPerFrame) {
      addNumber(fr, route);
      return;
    }
    int list = NUMBER_LISTS.get(route);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = NUMBER_LISTS.allocate(route);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        addNumber(fr, route);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      // The driver refused a list name: draw directly rather than calling list 0.
      addNumber(fr, route);
    }
  }

  /** The number centred on the origin. Geometry only: the caller binds and colours. */
  private static void addNumber(CsmFontRenderer fr, int route) {
    String text = NUMBER_TEXT[route];
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, text, -fr.getStringWidth(text) / 2, -fr.FONT_HEIGHT / 2);
    tess.draw();
  }
}
