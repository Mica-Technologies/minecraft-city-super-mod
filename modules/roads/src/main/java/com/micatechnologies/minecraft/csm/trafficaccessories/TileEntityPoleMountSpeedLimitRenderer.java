package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Renderer for {@link BlockPoleMountSpeedLimitSign}. Re-uses the overhead speed-limit
 * sign's two-zone face layout ("SPEED LIMIT" label over a fullbright LED screen
 * showing the digits) but at the smaller panel proportions of the portable trailer
 * variant. The panel's back face sits flush against the rear face of the placed
 * cell so it visually mounts directly to whatever pole/block sits behind it — no
 * bracket geometry of its own.
 *
 * <p>Nothing here animates. The panel, all in the white swatch, is compiled once per look into a
 * list shared by every sign that looks the same ({@link CsmSharedDisplayLists}), keyed on the
 * housing colour, the full-screen setting and the combined light, which stays in the vertices
 * exactly as before (at most 256 lights per look). The legend is two more lists in the font atlas:
 * the posted speed, keyed on its value, and the constant "SPEED LIMIT", each coloured from outside
 * because the two are drawn in different blacks. All are replayed under each sign's own facing.
 * {@link CsmRenderToggles#sharedBakesPerFrame} draws them per frame, for comparison.</p>
 */
public class TileEntityPoleMountSpeedLimitRenderer
    extends TileEntitySpecialRenderer<TileEntityPoleMountSpeedLimit> {

  // Sign panel — matches the portable trailer's panel dimensions (3:4 aspect)
  private static final float SIGN_WIDTH = 27.0f;
  private static final float SIGN_HEIGHT = 36.0f;
  private static final float SIGN_DEPTH = 3.0f;
  private static final float SIGN_FRAME = 1.5f;

  // Center of block (X) and vertical sign centerline. CZ is pushed back so the
  // panel's back face is exactly at Z=16 (the cell's rear face) — the sign mounts
  // directly to whatever block sits behind, no separate bracket geometry.
  //   CZ = 16 - SIGN_DEPTH/2  →  14.5
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;
  private static final float CZ = 14.5f;

  // Vertical positioning: panel centered on block
  private static final float SIGN_TOP = CY + SIGN_HEIGHT / 2.0f;
  private static final float SIGN_BOTTOM = CY - SIGN_HEIGHT / 2.0f;

  // Sign face zones: upper "SPEED LIMIT" area, lower speed number area (matches the
  // portable / overhead variants).
  private static final float SIGN_DIVIDER_Y = SIGN_BOTTOM + SIGN_HEIGHT * 0.42f;

  // Text — scaled to the smaller panel
  private static final float SPEED_TEXT_SCALE = 1.35f;
  private static final float LABEL_TEXT_SCALE = 0.82f;
  private static final int TEXT_COLOR_BLACK = 0x111111;

  // Colors
  private static final float[] COL_SIGN_BG = {0.85f, 0.85f, 0.85f, 1.0f};
  private static final float[] COL_SCREEN_WHITE = {0.95f, 0.95f, 0.95f, 1.0f};
  private static final float[] COL_SIGN_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  // The panel boxes. Every coordinate is a constant, so they are built once.
  private static final float SCREEN_INSET = 2.25f;

  // Outer housing-colored border (slightly larger than the visible face)
  private static final List<RenderHelper.Box> BORDER_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
              CZ - SIGN_DEPTH / 2},
          new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
              CZ + SIGN_DEPTH / 2}));

  // Sign background — light gray (or fullbright white when in full-screen mode)
  private static final List<RenderHelper.Box> BG_FACE_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, CZ - SIGN_DEPTH / 2 - 0.05f},
          new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, CZ - SIGN_DEPTH / 2 + 0.3f}));

  // Inset LED screen for the digits — always fullbright
  private static final List<RenderHelper.Box> SCREEN_FACE_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 + SCREEN_INSET, SIGN_BOTTOM + SCREEN_INSET,
              CZ - SIGN_DEPTH / 2 - 0.1f},
          new float[]{CX + SIGN_WIDTH / 2 - SCREEN_INSET, SIGN_DIVIDER_Y - 1.0f,
              CZ - SIGN_DEPTH / 2 + 0.25f}));

  /** The border, face and screen, in the white swatch. Key, see {@link #panelKey}. */
  private static final CsmSharedDisplayLists PANEL_LISTS =
      new CsmSharedDisplayLists("pole_speed_panel");

  /** The posted speed, in the font atlas, keyed on its value. */
  private static final CsmSharedDisplayLists SPEED_LISTS =
      new CsmSharedDisplayLists("pole_speed_digits");

  /** The constant "SPEED LIMIT" label, in the font atlas; one list, key 0. */
  private static final CsmSharedDisplayLists LABEL_LISTS =
      new CsmSharedDisplayLists("pole_speed_label");

  @Override
  public void render(TileEntityPoleMountSpeedLimit te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }

    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(BlockHorizontal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, z);
    GlStateManager.translate(0.5, 0.0, 0.5);

    float rotY = 0;
    switch (facing) {
      case NORTH:
        rotY = 0;
        break;
      case WEST:
        rotY = 90;
        break;
      case SOUTH:
        rotY = 180;
        break;
      case EAST:
        rotY = 270;
        break;
      default:
        break;
    }
    GlStateManager.rotate(rotY, 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);

    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    boolean fullScreen = te.isFullScreen();
    TrafficSignalBodyColor color = te.getHousingColor();

    // Read once and handed down: the panel used to read it again for the border, twice.
    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);

    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawSignPanel(color, fullScreen, combinedLight);
    } else {
      long key = panelKey(color, fullScreen, combinedLight);
      int list = PANEL_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = PANEL_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawSignPanel(color, fullScreen, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawSignPanel(color, fullScreen, combinedLight);
      }
    }

    renderLegend(te.getSpeedValue());

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  /**
   * Packs everything the panel depends on. Facing is not in it: it is the matrix the list is
   * replayed under.
   *
   * <pre>
   *  bits  0-31  combined light, as getCombinedLight returns it (sky in the high half)
   *  bit  32     full screen
   *  bits 33-40  housing colour ordinal
   * </pre>
   */
  private static long panelKey(TrafficSignalBodyColor color, boolean fullScreen,
      int combinedLight) {
    return (combinedLight & 0xFFFFFFFFL)
        | ((fullScreen ? 1L : 0L) << 32)
        | ((long) (color.ordinal() & 0xFF) << 33);
  }

  /**
   * The housing-colored border + frame + sign face, in one draw. Mirrors the overhead
   * variant's face split: full-screen mode lights the whole face; otherwise the upper area
   * is the housing-tinted label panel and only the inset LED screen is fullbright. The boxes
   * are emitted in the order they were once drawn one by one, all opaque and in one texture,
   * and the screen stays last so the lightmap the legend inherits after it is the same.
   * Geometry only: the caller binds the texture and owns every GL state.
   */
  private static void drawSignPanel(TrafficSignalBodyColor color, boolean fullScreen,
      int combinedLight) {
    int worldSky = (combinedLight >> 16) & 0xFFFF;
    int worldBlock = combinedLight & 0xFFFF;
    int faceSkyLight = fullScreen ? LIGHTMAP_FULLBRIGHT_SKY : worldSky;
    int faceBlockLight = fullScreen ? LIGHTMAP_FULLBRIGHT_BLOCK : worldBlock;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    RenderHelper.addBoxesToBufferLit(BORDER_BOX, buf,
        color.getRed() * 0.7f, color.getGreen() * 0.7f, color.getBlue() * 0.7f, 1.0f,
        0, 0, 0, worldSky, worldBlock);

    float[] bgColor = fullScreen ? COL_SCREEN_WHITE : COL_SIGN_BG;
    RenderHelper.addBoxesToBufferLit(BG_FACE_BOX, buf,
        bgColor[0], bgColor[1], bgColor[2], bgColor[3], 0, 0, 0,
        faceSkyLight, faceBlockLight);

    if (!fullScreen) {
      RenderHelper.addBoxesToBufferLit(SCREEN_FACE_BOX, buf,
          COL_SCREEN_WHITE[0], COL_SCREEN_WHITE[1], COL_SCREEN_WHITE[2], COL_SCREEN_WHITE[3],
          0, 0, 0, LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK);
    }

    tess.draw();
  }

  /**
   * Draws the speed and then the label, as drawString did: the atlas bound, texturing on, the
   * depth mask off and each in its own black, all set here outside the lists.
   */
  private static void renderLegend(int speed) {
    // Fetched before any list is opened, so its lazy constructor cannot bind during a compile.
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    GlStateManager.depthMask(false);
    fr.bindAtlas();
    GlStateManager.enableTexture2D();

    GlStateManager.color(((TEXT_COLOR_BLACK >> 16) & 0xFF) / 255.0f,
        ((TEXT_COLOR_BLACK >> 8) & 0xFF) / 255.0f, (TEXT_COLOR_BLACK & 0xFF) / 255.0f, 1.0f);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawSpeedText(fr, speed);
    } else {
      int list = SPEED_LISTS.get(speed);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = SPEED_LISTS.allocate(speed);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawSpeedText(fr, speed);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawSpeedText(fr, speed);
      }
    }

    GlStateManager.color(0.0f, 0.0f, 0.0f, 1.0f);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawLabelText(fr);
    } else {
      int list = LABEL_LISTS.get(0L);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = LABEL_LISTS.allocate(0L);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawLabelText(fr);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawLabelText(fr);
      }
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.depthMask(true);
  }

  /**
   * Draws "SPEED" over "LIMIT". Geometry and matrix only (the matrix calls are not cached by
   * {@code GlStateManager}, so they compile into a list): the caller binds the atlas and sets the
   * colour.
   */
  private static void drawLabelText(CsmFontRenderer fr) {
    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.1f;
    float upperCenterY = (SIGN_DIVIDER_Y + SIGN_TOP) / 2.0f;
    GlStateManager.translate(CX, upperCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(LABEL_TEXT_SCALE, -LABEL_TEXT_SCALE, LABEL_TEXT_SCALE);

    String line1 = "SPEED";
    String line2 = "LIMIT";
    int w1 = fr.getStringWidth(line1);
    int w2 = fr.getStringWidth(line2);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, line1, -w1 / 2, -fr.FONT_HEIGHT - 1);
    fr.addString(buf, line2, -w2 / 2, 1);
    tess.draw();

    GlStateManager.popMatrix();
  }

  /**
   * Draws the posted speed centred on the LED screen. Geometry and matrix only: the caller binds
   * the atlas and sets the colour.
   */
  private static void drawSpeedText(CsmFontRenderer fr, int speed) {
    String speedStr = String.valueOf(speed);

    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.2f;
    float lowerCenterY = (SIGN_BOTTOM + SIGN_DIVIDER_Y) / 2.0f;
    GlStateManager.translate(CX, lowerCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(SPEED_TEXT_SCALE, -SPEED_TEXT_SCALE, SPEED_TEXT_SCALE);

    int textWidth = fr.getStringWidth(speedStr);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, speedStr, -textWidth / 2, -fr.FONT_HEIGHT / 2);
    tess.draw();

    GlStateManager.popMatrix();
  }
}
