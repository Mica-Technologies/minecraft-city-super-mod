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
 * <p>The plates themselves are baked; only the numbers are drawn here, on the front and the back
 * of each plate, at the depth the sign system's shift puts the sign ({@link BusStopSigns#SHIFT_Z}),
 * across the side of the post the flag hangs off ({@link BlockBusStopFlag#middleX}) and at the
 * plate's own shade, since a tile entity renderer gets none of the diffuse shading a
 * baked face carries. Each number, 1 to 99, is
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

  /** How tall the numbers are, in sixteenths (a plate is 2.5). */
  private static final float TEXT_HEIGHT = 1.6f;

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
    float shift = BusStopSigns.shiftZ(te.getViewShift());
    // the sign frame puts a face the model has at z at 16 - z
    float front = 16.0f - (BlockBusStopFlag.FACE_FRONT_Z + shift) + LIFT;
    float back = 16.0f - (BlockBusStopFlag.FACE_BACK_Z + shift) - LIFT;
    float numberX = BlockBusStopFlag.NUMBER_FROM_LEFT - BlockBusStopFlag.PLATE_WIDTH / 2.0f;
    // the plates' middle across, where the model has it at x the frame has it at 16 - x
    float middle = 16.0f - BlockBusStopFlag.middleX(te.isLeft());
    float shade = BusStopSigns.shade(te.getViewFacing());

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    enterSignFrame(te);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.enableTexture2D();
    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.color(0.96f * shade, 0.96f * shade, 0.94f * shade, 1.0f);

    for (int i = 0; i < TileEntityBusStopFlag.PLATES; i++) {
      int route = te.getRoute(i);
      if (route == 0) {
        continue;
      }
      float midY = BlockBusStopFlag.PLATE_MIDDLE_Y[i];
      // the front, read with +x to the reader's right
      GlStateManager.pushMatrix();
      GlStateManager.translate(middle + numberX, midY, front);
      GlStateManager.scale(scale, -scale, scale);
      drawNumber(fr, route);
      GlStateManager.popMatrix();
      // the back, read from behind: turned a half turn about the plate's middle
      GlStateManager.pushMatrix();
      GlStateManager.translate(middle, midY, back);
      GlStateManager.rotate(180, 0, 1, 0);
      GlStateManager.translate(numberX, 0.0f, 0.0f);
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
   * Turns the GL frame to the sign's facing and scales it to sixteenths, the way
   * {@code TileEntityDynamicRouteMarkerSignRenderer} does: the blockstate's turn, plus a half turn
   * that puts the reader in front of the sign with +x to their right. A point the model has at
   * {@code (x, y, z)} is at {@code (16 - x, y, 16 - z)} here. Call with the origin at the middle
   * of the block's bottom.
   *
   * @param te the sign's tile entity, its view already refreshed
   */
  static void enterSignFrame(AbstractTileEntityBusStopSign te) {
    GlStateManager.rotate(180.0f + te.getViewFacing().getRotationDegrees(), 0.0f, 1.0f, 0.0f);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    GlStateManager.translate(-8.0, 0.0, -8.0);
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
