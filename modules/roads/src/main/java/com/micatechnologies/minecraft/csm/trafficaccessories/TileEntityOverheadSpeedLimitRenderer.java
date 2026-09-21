package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
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
 * Renderer for the overhead speed limit sign.
 *
 * <p>Nothing here animates. The housing and face, all in the white swatch, are compiled once per
 * look into a list shared by every sign that looks the same ({@link CsmSharedDisplayLists}), keyed
 * on the housing colour, the full-screen setting and the combined light, which stays in the
 * vertices exactly as before (at most 256 lights per look). The legend is two more lists in the
 * font atlas: the posted speed, keyed on its value, and the constant "SPEED LIMIT", each coloured
 * from outside because the two are drawn in different blacks. All are replayed under each sign's
 * own facing. {@link CsmRenderToggles#sharedBakesPerFrame} draws them per frame, for
 * comparison.</p>
 */
public class TileEntityOverheadSpeedLimitRenderer
    extends TileEntitySpecialRenderer<TileEntityOverheadSpeedLimit> {

  // Sign dimensions: proportions matched to portable speed limit sign (3:4 aspect)
  private static final float SIGN_WIDTH = 48.0f;
  private static final float SIGN_HEIGHT = 64.0f;
  private static final float SIGN_DEPTH = 12.0f;
  private static final float SIGN_FRAME = 2.0f;

  // Center of block
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;

  // Vertical positioning: centered on block
  private static final float SIGN_TOP = CY + SIGN_HEIGHT / 2.0f;
  private static final float SIGN_BOTTOM = CY - SIGN_HEIGHT / 2.0f;

  // Z positioning: housing extends from front to back edge of block (Z=16)
  private static final float BACK_Z = 16.0f;
  private static final float FACE_Z = BACK_Z - SIGN_DEPTH;

  // Sign face zones: upper "SPEED LIMIT" area, lower speed number area (same 42% split as portable)
  private static final float SIGN_DIVIDER_Y = SIGN_BOTTOM + SIGN_HEIGHT * 0.42f;

  // Text (scaled from portable: 1.35 * 1.778, 0.71 * 1.778)
  private static final float SPEED_TEXT_SCALE = 2.4f;
  private static final float LABEL_TEXT_SCALE = 1.45f;
  private static final int TEXT_COLOR_BLACK = 0x111111;

  // Colors
  private static final float[] COL_HOUSING = {0.18f, 0.18f, 0.18f, 1.0f};
  private static final float[] COL_SIGN_BG = {0.85f, 0.85f, 0.85f, 1.0f};
  private static final float[] COL_SCREEN_WHITE = {0.95f, 0.95f, 0.95f, 1.0f};
  private static final float[] COL_FRAME = {0.45f, 0.45f, 0.47f, 1.0f};
  private static final float[] COL_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  // The housing and face boxes. Every coordinate is a constant, so they are built once.
  private static final float FACE_FRONT = FACE_Z - SIGN_FRAME - 0.1f;
  private static final float SCREEN_INSET = 4.0f;

  private static final List<RenderHelper.Box> BORDER_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
              FACE_Z - SIGN_FRAME},
          new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
              BACK_Z + SIGN_FRAME}));

  private static final List<RenderHelper.Box> FRAME_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 - 0.5f, SIGN_BOTTOM - 0.5f, FACE_Z - 0.5f},
          new float[]{CX + SIGN_WIDTH / 2 + 0.5f, SIGN_TOP + 0.5f, BACK_Z + 0.5f}));

  private static final List<RenderHelper.Box> HOUSING_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, FACE_Z},
          new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, BACK_Z}));

  private static final List<RenderHelper.Box> BG_FACE_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 + 1.0f, SIGN_BOTTOM + 1.0f, FACE_FRONT - 0.3f},
          new float[]{CX + SIGN_WIDTH / 2 - 1.0f, SIGN_TOP - 1.0f, FACE_FRONT}));

  private static final List<RenderHelper.Box> SCREEN_FACE_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 + SCREEN_INSET, SIGN_BOTTOM + SCREEN_INSET,
              FACE_FRONT - 0.4f},
          new float[]{CX + SIGN_WIDTH / 2 - SCREEN_INSET, SIGN_DIVIDER_Y - 1.0f,
              FACE_FRONT - 0.1f}));

  /** The housing and face, in the white swatch. Key, see {@link #bodyKey}. */
  private static final CsmSharedDisplayLists BODY_LISTS =
      new CsmSharedDisplayLists("overhead_speed_body");

  /** The posted speed, in the font atlas, keyed on its value. */
  private static final CsmSharedDisplayLists SPEED_LISTS =
      new CsmSharedDisplayLists("overhead_speed_digits");

  /** The constant "SPEED LIMIT" label, in the font atlas; one list, key 0. */
  private static final CsmSharedDisplayLists LABEL_LISTS =
      new CsmSharedDisplayLists("overhead_speed_label");

  @Override
  public void render(TileEntityOverheadSpeedLimit te, double x, double y, double z,
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
    // Bind 1x1 white texture instead of disableTexture2D — shaders ignore disableTexture2D.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    boolean fullScreen = te.isFullScreen();
    TrafficSignalBodyColor color = te.getHousingColor();

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);

    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawBody(color, fullScreen, combinedLight);
    } else {
      long key = bodyKey(color, fullScreen, combinedLight);
      int list = BODY_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = BODY_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawBody(color, fullScreen, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawBody(color, fullScreen, combinedLight);
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
   * Packs everything the housing and face depend on. Facing is not in it: it is the matrix the
   * list is replayed under.
   *
   * <pre>
   *  bits  0-31  combined light, as getCombinedLight returns it (sky in the high half)
   *  bit  32     full screen
   *  bits 33-40  housing colour ordinal
   * </pre>
   */
  private static long bodyKey(TrafficSignalBodyColor color, boolean fullScreen,
      int combinedLight) {
    return (combinedLight & 0xFFFFFFFFL)
        | ((fullScreen ? 1L : 0L) << 32)
        | ((long) (color.ordinal() & 0xFF) << 33);
  }

  /**
   * Draws the housing and face in one draw, in the order they were once drawn one by one: border,
   * frame, housing body, face, then the inset screen. All opaque and in one texture, so one draw
   * changes no pixel. The screen stays last, so the lightmap the legend inherits after it is the
   * same. Geometry only: the caller binds the texture and owns every GL state.
   */
  private static void drawBody(TrafficSignalBodyColor color, boolean fullScreen,
      int combinedLight) {
    int worldSkyLight = (combinedLight >> 16) & 0xFFFF;
    int worldBlockLight = combinedLight & 0xFFFF;
    // Full-screen mode lights the entire face fullbright; otherwise the housing/border
    // takes world ambient light and only the inset screen panel is fullbright.
    int faceSkyLight = fullScreen ? LIGHTMAP_FULLBRIGHT_SKY : worldSkyLight;
    int faceBlockLight = fullScreen ? LIGHTMAP_FULLBRIGHT_BLOCK : worldBlockLight;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    addHousing(buf, color, worldSkyLight, worldBlockLight);
    addSignFace(buf, fullScreen, faceSkyLight, faceBlockLight);
    tess.draw();
  }

  private static void addHousing(BufferBuilder buf, TrafficSignalBodyColor color, int skyLight,
      int blockLight) {
    // Border
    RenderHelper.addBoxesToBufferLit(BORDER_BOX, buf,
        color.getRed() * 0.7f, color.getGreen() * 0.7f, color.getBlue() * 0.7f, 1.0f,
        0, 0, 0, skyLight, blockLight);

    // Frame
    RenderHelper.addBoxesToBufferLit(FRAME_BOX, buf,
        color.getRed() * 0.85f, color.getGreen() * 0.85f, color.getBlue() * 0.85f, 1.0f,
        0, 0, 0, skyLight, blockLight);

    // Housing body
    RenderHelper.addBoxesToBufferLit(HOUSING_BOX, buf,
        color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0,
        skyLight, blockLight);
  }

  private static void addSignFace(BufferBuilder buf, boolean fullScreen, int skyLight,
      int blockLight) {
    float[] bgColor = fullScreen ? COL_SCREEN_WHITE : COL_SIGN_BG;

    RenderHelper.addBoxesToBufferLit(BG_FACE_BOX, buf,
        bgColor[0], bgColor[1], bgColor[2], bgColor[3], 0, 0, 0,
        skyLight, blockLight);

    if (!fullScreen) {
      RenderHelper.addBoxesToBufferLit(SCREEN_FACE_BOX, buf,
          COL_SCREEN_WHITE[0], COL_SCREEN_WHITE[1], COL_SCREEN_WHITE[2], COL_SCREEN_WHITE[3],
          0, 0, 0, LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK);
    }
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

    float textZ = FACE_Z - SIGN_FRAME - 0.5f;
    float upperCenterY = (SIGN_DIVIDER_Y + SIGN_TOP - 1.0f) / 2.0f;
    GlStateManager.translate(CX, upperCenterY, textZ);
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
   * Draws the posted speed centred in the lower zone. Geometry and matrix only: the caller binds
   * the atlas and sets the colour.
   */
  private static void drawSpeedText(CsmFontRenderer fr, int speed) {
    String speedStr = String.valueOf(speed);

    GlStateManager.pushMatrix();

    float textZ = FACE_Z - SIGN_FRAME - 0.6f;
    float lowerCenterY = (SIGN_BOTTOM + 1.0f + SIGN_DIVIDER_Y) / 2.0f;
    GlStateManager.translate(CX, lowerCenterY, textZ);
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
