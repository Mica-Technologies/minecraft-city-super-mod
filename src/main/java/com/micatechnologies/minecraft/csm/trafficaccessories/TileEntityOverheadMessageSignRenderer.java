package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

public class TileEntityOverheadMessageSignRenderer
    extends TileEntitySpecialRenderer<TileEntityOverheadMessageSign> {

  // Sign housing dimensions (model units, 16 = 1 block)
  private static final float SIGN_WIDTH = 96.0f;
  private static final float SIGN_HEIGHT = 40.0f;
  private static final float SIGN_DEPTH = 20.0f;
  private static final float SIGN_FRAME = 2.0f;

  // Text rendering
  private static final float TEXT_SCALE = 0.975f;
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
      case SOUTH:
        rotY = 0;
        break;
      case WEST:
        rotY = 90;
        break;
      case NORTH:
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
    GlStateManager.disableTexture2D();

    renderHousing();
    renderSignFace();

    GlStateManager.enableTexture2D();

    renderText(te);

    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  private void renderHousing() {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // Frame (slightly larger than housing)
    List<RenderHelper.Box> frame = new ArrayList<>();
    frame.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
            FACE_Z - SIGN_FRAME},
        new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
            BACK_Z + SIGN_FRAME}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(frame, buf,
        COL_FRAME[0], COL_FRAME[1], COL_FRAME[2], COL_FRAME[3], 0, 0, 0);
    tess.draw();

    // Main housing body
    List<RenderHelper.Box> housing = new ArrayList<>();
    housing.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, FACE_Z},
        new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, BACK_Z}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(housing, buf,
        COL_HOUSING[0], COL_HOUSING[1], COL_HOUSING[2], COL_HOUSING[3], 0, 0, 0);
    tess.draw();
  }

  private void renderSignFace() {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    List<RenderHelper.Box> face = new ArrayList<>();
    float faceFront = FACE_Z - SIGN_FRAME - 0.1f;
    face.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 + 1.0f, SIGN_BOTTOM + 1.0f, faceFront - 0.3f},
        new float[]{CX + SIGN_WIDTH / 2 - 1.0f, SIGN_TOP - 1.0f, faceFront}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(face, buf,
        COL_SIGN_FACE[0], COL_SIGN_FACE[1], COL_SIGN_FACE[2], COL_SIGN_FACE[3], 0, 0, 0);
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

    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();

    float textZ = FACE_Z - SIGN_FRAME - 0.5f;
    GlStateManager.translate(CX, SIGN_CENTER_Y, textZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

    GlStateManager.depthMask(false);
    GlStateManager.enableTexture2D();

    float lineSpacing = fr.FONT_HEIGHT + 2;
    float totalHeight = 3 * lineSpacing;
    float startY = -totalHeight / 2.0f + 1;

    String[] lines = {line1, line2, line3};
    for (int i = 0; i < 3; i++) {
      if (!lines[i].isEmpty()) {
        int textWidth = fr.getStringWidth(lines[i]);
        float textX = -textWidth / 2.0f;
        float textY = startY + i * lineSpacing;
        fr.drawString(lines[i], (int) textX, (int) textY, TEXT_COLOR_AMBER);
      }
    }

    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }
}
