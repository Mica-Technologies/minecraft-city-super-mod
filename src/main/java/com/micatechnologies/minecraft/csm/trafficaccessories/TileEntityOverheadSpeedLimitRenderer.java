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

public class TileEntityOverheadSpeedLimitRenderer
    extends TileEntitySpecialRenderer<TileEntityOverheadSpeedLimit> {

  // Sign dimensions: 3 blocks wide × 5 blocks tall (48 × 80 model units)
  private static final float SIGN_WIDTH = 48.0f;
  private static final float SIGN_HEIGHT = 80.0f;
  private static final float SIGN_DEPTH = 12.0f;
  private static final float SIGN_FRAME = 2.0f;

  // Mounting bracket
  private static final float BRACKET_WIDTH = 10.0f;
  private static final float BRACKET_HEIGHT = 4.0f;
  private static final float BRACKET_DEPTH = 10.0f;

  // Center of block
  private static final float CX = 8.0f;
  private static final float CZ = 8.0f;

  // Vertical positioning: sign hangs below block
  private static final float SIGN_TOP = 0.0f;
  private static final float SIGN_BOTTOM = SIGN_TOP - SIGN_HEIGHT;

  // Sign face zones: upper "SPEED LIMIT" area, lower speed number area
  private static final float SIGN_DIVIDER_Y = SIGN_TOP - SIGN_HEIGHT * 0.38f;

  // Text
  private static final float SPEED_TEXT_SCALE = 5.0f;
  private static final float LABEL_TEXT_SCALE = 1.2f;
  private static final int TEXT_COLOR_AMBER = 0xFFAA00;

  // Colors
  private static final float[] COL_HOUSING = {0.18f, 0.18f, 0.18f, 1.0f};
  private static final float[] COL_SIGN_WHITE = {0.92f, 0.92f, 0.92f, 1.0f};
  private static final float[] COL_SIGN_BLACK = {0.06f, 0.06f, 0.06f, 1.0f};
  private static final float[] COL_FRAME = {0.45f, 0.45f, 0.47f, 1.0f};
  private static final float[] COL_BRACKET = {0.35f, 0.35f, 0.37f, 1.0f};
  private static final float[] COL_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};

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

    renderBrackets();
    renderHousing();
    renderSignFace();

    GlStateManager.enableTexture2D();

    renderSpeedText(te);
    renderLabelText();

    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  private void renderBrackets() {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> boxes = new ArrayList<>();

    float bracketY = SIGN_TOP;

    boxes.add(new RenderHelper.Box(
        new float[]{CX - BRACKET_WIDTH / 2, bracketY, CZ - BRACKET_DEPTH / 2},
        new float[]{CX + BRACKET_WIDTH / 2, bracketY + BRACKET_HEIGHT,
            CZ + BRACKET_DEPTH / 2}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(boxes, buf,
        COL_BRACKET[0], COL_BRACKET[1], COL_BRACKET[2], COL_BRACKET[3], 0, 0, 0);
    tess.draw();
  }

  private void renderHousing() {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // Border
    List<RenderHelper.Box> border = new ArrayList<>();
    border.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
            CZ - SIGN_DEPTH / 2 - SIGN_FRAME},
        new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
            CZ + SIGN_DEPTH / 2 + SIGN_FRAME}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(border, buf,
        COL_BORDER[0], COL_BORDER[1], COL_BORDER[2], COL_BORDER[3], 0, 0, 0);
    tess.draw();

    // Frame
    List<RenderHelper.Box> frame = new ArrayList<>();
    frame.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - 0.5f, SIGN_BOTTOM - 0.5f,
            CZ - SIGN_DEPTH / 2 - 0.5f},
        new float[]{CX + SIGN_WIDTH / 2 + 0.5f, SIGN_TOP + 0.5f,
            CZ + SIGN_DEPTH / 2 + 0.5f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(frame, buf,
        COL_FRAME[0], COL_FRAME[1], COL_FRAME[2], COL_FRAME[3], 0, 0, 0);
    tess.draw();

    // Housing body
    List<RenderHelper.Box> housing = new ArrayList<>();
    housing.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, CZ - SIGN_DEPTH / 2},
        new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, CZ + SIGN_DEPTH / 2}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(housing, buf,
        COL_HOUSING[0], COL_HOUSING[1], COL_HOUSING[2], COL_HOUSING[3], 0, 0, 0);
    tess.draw();
  }

  private void renderSignFace() {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // White upper portion
    List<RenderHelper.Box> upperFace = new ArrayList<>();
    upperFace.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 + 1.0f, SIGN_DIVIDER_Y,
            CZ - SIGN_DEPTH / 2 - 0.1f},
        new float[]{CX + SIGN_WIDTH / 2 - 1.0f, SIGN_TOP - 1.0f,
            CZ - SIGN_DEPTH / 2 + 0.3f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(upperFace, buf,
        COL_SIGN_WHITE[0], COL_SIGN_WHITE[1], COL_SIGN_WHITE[2], COL_SIGN_WHITE[3], 0, 0, 0);
    tess.draw();

    // Black LED lower portion
    List<RenderHelper.Box> lowerFace = new ArrayList<>();
    lowerFace.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 + 1.0f, SIGN_BOTTOM + 1.0f,
            CZ - SIGN_DEPTH / 2 - 0.1f},
        new float[]{CX + SIGN_WIDTH / 2 - 1.0f, SIGN_DIVIDER_Y,
            CZ - SIGN_DEPTH / 2 + 0.3f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(lowerFace, buf,
        COL_SIGN_BLACK[0], COL_SIGN_BLACK[1], COL_SIGN_BLACK[2], COL_SIGN_BLACK[3], 0, 0, 0);
    tess.draw();
  }

  private void renderLabelText() {
    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.15f;
    float upperCenterY = (SIGN_DIVIDER_Y + SIGN_TOP - 1.0f) / 2.0f;
    GlStateManager.translate(CX, upperCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(LABEL_TEXT_SCALE, -LABEL_TEXT_SCALE, LABEL_TEXT_SCALE);

    GlStateManager.depthMask(false);
    GlStateManager.enableTexture2D();

    String line1 = "SPEED";
    String line2 = "LIMIT";
    int w1 = fr.getStringWidth(line1);
    int w2 = fr.getStringWidth(line2);

    fr.drawString(line1, -w1 / 2, -fr.FONT_HEIGHT - 1, 0x000000);
    fr.drawString(line2, -w2 / 2, 1, 0x000000);

    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }

  private void renderSpeedText(TileEntityOverheadSpeedLimit te) {
    int speed = te.getSpeedValue();
    String speedStr = String.valueOf(speed);

    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.15f;
    float lowerCenterY = (SIGN_BOTTOM + 1.0f + SIGN_DIVIDER_Y) / 2.0f;
    GlStateManager.translate(CX, lowerCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(SPEED_TEXT_SCALE, -SPEED_TEXT_SCALE, SPEED_TEXT_SCALE);

    GlStateManager.depthMask(false);
    GlStateManager.enableTexture2D();

    int textWidth = fr.getStringWidth(speedStr);
    fr.drawString(speedStr, -textWidth / 2, -fr.FONT_HEIGHT / 2, TEXT_COLOR_AMBER);

    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }
}
