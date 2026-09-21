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
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Renderer for the overhead dynamic message sign.
 *
 * <p>The housing and the dark face, all in the white swatch, are compiled once per look into a list
 * shared by every sign that looks the same ({@link CsmSharedDisplayLists}), keyed on the housing
 * colour and the combined light, which stays in the vertices exactly as before (at most 256 lights
 * per look), and replayed under each sign's own facing. The pages stay live: their text cannot be
 * packed into a key, and a hash could collide and show another sign's message.
 * {@link CsmRenderToggles#sharedBakesPerFrame} draws the body per frame, for comparison.</p>
 */
public class TileEntityOverheadMessageSignRenderer
    extends TileEntitySpecialRenderer<TileEntityOverheadMessageSign> {

  // Sign housing dimensions (model units, 16 = 1 block)
  private static final float SIGN_WIDTH = 144.0f;
  private static final float SIGN_HEIGHT = 64.02f;
  private static final float SIGN_DEPTH = 20.0f;
  private static final float SIGN_FRAME = 2.0f;

  // Text rendering
  private static final float TEXT_SCALE = 1.55f;
  private static final int TEXT_COLOR_AMBER = 0xFFAA00;

  // Center of block
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;

  // Vertical positioning: centered on block
  private static final float SIGN_TOP = CY + SIGN_HEIGHT / 2.0f;
  private static final float SIGN_BOTTOM = CY - SIGN_HEIGHT / 2.0f;
  private static final float SIGN_CENTER_Y = CY;

  // Z positioning: housing extends from front to back edge of block (Z=16)
  private static final float BACK_Z = 16.0f;
  private static final float FACE_Z = BACK_Z - SIGN_DEPTH;

  // Colors
  private static final float[] COL_HOUSING = {0.18f, 0.18f, 0.18f, 1.0f};
  private static final float[] COL_SIGN_FACE = {0.06f, 0.06f, 0.06f, 1.0f};
  private static final float[] COL_FRAME = {0.45f, 0.45f, 0.47f, 1.0f};

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  // The housing and face boxes. Every coordinate is a constant, so they are built once.
  private static final float FACE_FRONT = FACE_Z - SIGN_FRAME - 0.1f;

  // Frame (slightly larger than housing)
  private static final List<RenderHelper.Box> FRAME_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
              FACE_Z - SIGN_FRAME},
          new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
              BACK_Z + SIGN_FRAME}));

  // Main housing body
  private static final List<RenderHelper.Box> HOUSING_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, FACE_Z},
          new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, BACK_Z}));

  private static final List<RenderHelper.Box> FACE_BOX = Collections.singletonList(
      new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 + 1.0f, SIGN_BOTTOM + 1.0f, FACE_FRONT - 0.3f},
          new float[]{CX + SIGN_WIDTH / 2 - 1.0f, SIGN_TOP - 1.0f, FACE_FRONT}));

  /** The housing and face, in the white swatch. Key, see {@link #bodyKey}. */
  private static final CsmSharedDisplayLists BODY_LISTS =
      new CsmSharedDisplayLists("overhead_message_body");

  @Override
  public void render(TileEntityOverheadMessageSign te, double x, double y, double z,
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

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    TrafficSignalBodyColor color = te.getHousingColor();

    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawBody(color, combinedLight);
    } else {
      long key = bodyKey(color, combinedLight);
      int list = BODY_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = BODY_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawBody(color, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawBody(color, combinedLight);
      }
    }

    renderText(te);

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
   *  bits 32-39  housing colour ordinal
   * </pre>
   */
  private static long bodyKey(TrafficSignalBodyColor color, int combinedLight) {
    return (combinedLight & 0xFFFFFFFFL)
        | ((long) (color.ordinal() & 0xFF) << 32);
  }

  /**
   * Draws the frame, the housing body and the face in one draw, in the order they were once drawn
   * one by one. All opaque, all world lit and in one texture, so one draw changes no pixel.
   * Geometry only: the caller binds the texture and owns every GL state.
   */
  private static void drawBody(TrafficSignalBodyColor color, int combinedLight) {
    int skyLight = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    RenderHelper.addBoxesToBufferLit(FRAME_BOX, buf,
        color.getRed() * 0.85f, color.getGreen() * 0.85f, color.getBlue() * 0.85f, 1.0f,
        0, 0, 0, skyLight, blockLight);

    RenderHelper.addBoxesToBufferLit(HOUSING_BOX, buf,
        color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0,
        skyLight, blockLight);

    RenderHelper.addBoxesToBufferLit(FACE_BOX, buf,
        COL_SIGN_FACE[0], COL_SIGN_FACE[1], COL_SIGN_FACE[2], COL_SIGN_FACE[3], 0, 0, 0,
        skyLight, blockLight);

    tess.draw();
  }

  private void renderText(TileEntityOverheadMessageSign te) {
    int pageIdx = te.getCurrentPageIndex();
    String[] page = te.getPage(pageIdx);
    String line1 = page[0];
    String line2 = page[1];
    String line3 = page[2];

    if (line1.isEmpty() && line2.isEmpty() && line3.isEmpty()) {
      return;
    }

    CsmFontRenderer fr = CsmFontRenderer.electronicSign();

    // Force the lightmap unit to fullbright for the text pass. CsmFontRenderer.drawString
    // uses POSITION_TEX (no per-vertex lightmap), so it inherits the GL lightmap state set
    // by the prior BLOCK-format draws — which is the world's combined-light. Without this
    // override the LED text dims with the surrounding world instead of glowing.
    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK);

    GlStateManager.pushMatrix();

    float textZ = FACE_Z - SIGN_FRAME - 0.5f;
    GlStateManager.translate(CX, SIGN_CENTER_Y, textZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

    GlStateManager.depthMask(false);

    float lineSpacing = fr.FONT_HEIGHT + 2;
    float totalHeight = 3 * lineSpacing;
    float startY = -totalHeight / 2.0f + 1;

    // What drawString did per line, done once: the atlas, texturing and the amber, then every
    // non-empty line in one draw.
    fr.bindAtlas();
    GlStateManager.enableTexture2D();
    GlStateManager.color(((TEXT_COLOR_AMBER >> 16) & 0xFF) / 255.0f,
        ((TEXT_COLOR_AMBER >> 8) & 0xFF) / 255.0f, (TEXT_COLOR_AMBER & 0xFF) / 255.0f, 1.0f);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    addLine(fr, buf, line1, startY);
    addLine(fr, buf, line2, startY + lineSpacing);
    addLine(fr, buf, line3, startY + 2 * lineSpacing);
    tess.draw();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }

  /** Adds one line centred on the sign's axis, where drawString used to put it. */
  private static void addLine(CsmFontRenderer fr, BufferBuilder buf, String line, float textY) {
    if (!line.isEmpty()) {
      int textWidth = fr.getStringWidth(line);
      float textX = -textWidth / 2.0f;
      fr.addString(buf, line, (int) textX, (int) textY);
    }
  }
}
