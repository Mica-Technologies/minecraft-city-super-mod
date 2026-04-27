package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
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
  private static final float LABEL_TEXT_SCALE = 1.26f;
  private static final int TEXT_COLOR_BLACK = 0x111111;

  // Colors
  private static final float[] COL_HOUSING = {0.18f, 0.18f, 0.18f, 1.0f};
  private static final float[] COL_SIGN_BG = {0.85f, 0.85f, 0.85f, 1.0f};
  private static final float[] COL_SCREEN_WHITE = {0.95f, 0.95f, 0.95f, 1.0f};
  private static final float[] COL_FRAME = {0.45f, 0.45f, 0.47f, 1.0f};
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
    GlStateManager.disableTexture2D();

    boolean fullScreen = te.isFullScreen();
    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;

    if (fullScreen) {
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    }

    renderHousing(te);
    renderSignFace(fullScreen);

    GlStateManager.enableTexture2D();

    renderSpeedText(te);
    renderLabelText();

    if (fullScreen) {
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
    }

    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  private void renderHousing(TileEntityOverheadSpeedLimit te) {
    TrafficSignalBodyColor color = te.getHousingColor();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // Border
    List<RenderHelper.Box> border = new ArrayList<>();
    border.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
            FACE_Z - SIGN_FRAME},
        new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
            BACK_Z + SIGN_FRAME}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(border, buf,
        color.getRed() * 0.7f, color.getGreen() * 0.7f, color.getBlue() * 0.7f, 1.0f,
        0, 0, 0);
    tess.draw();

    // Frame
    List<RenderHelper.Box> frame = new ArrayList<>();
    frame.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - 0.5f, SIGN_BOTTOM - 0.5f, FACE_Z - 0.5f},
        new float[]{CX + SIGN_WIDTH / 2 + 0.5f, SIGN_TOP + 0.5f, BACK_Z + 0.5f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(frame, buf,
        color.getRed() * 0.85f, color.getGreen() * 0.85f, color.getBlue() * 0.85f, 1.0f,
        0, 0, 0);
    tess.draw();

    // Housing body
    List<RenderHelper.Box> housing = new ArrayList<>();
    housing.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, FACE_Z},
        new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, BACK_Z}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(housing, buf,
        color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0);
    tess.draw();
  }

  private void renderSignFace(boolean fullScreen) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float faceFront = FACE_Z - SIGN_FRAME - 0.1f;
    float[] bgColor = fullScreen ? COL_SCREEN_WHITE : COL_SIGN_BG;

    List<RenderHelper.Box> bgFace = new ArrayList<>();
    bgFace.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 + 1.0f, SIGN_BOTTOM + 1.0f, faceFront - 0.3f},
        new float[]{CX + SIGN_WIDTH / 2 - 1.0f, SIGN_TOP - 1.0f, faceFront}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(bgFace, buf,
        bgColor[0], bgColor[1], bgColor[2], bgColor[3], 0, 0, 0);
    tess.draw();

    if (!fullScreen) {
      float prevBX = OpenGlHelper.lastBrightnessX;
      float prevBY = OpenGlHelper.lastBrightnessY;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

      float screenInset = 4.0f;
      List<RenderHelper.Box> screenFace = new ArrayList<>();
      screenFace.add(new RenderHelper.Box(
          new float[]{CX - SIGN_WIDTH / 2 + screenInset, SIGN_BOTTOM + screenInset,
              faceFront - 0.4f},
          new float[]{CX + SIGN_WIDTH / 2 - screenInset, SIGN_DIVIDER_Y - 1.0f,
              faceFront - 0.1f}));

      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      RenderHelper.addBoxesToBuffer(screenFace, buf,
          COL_SCREEN_WHITE[0], COL_SCREEN_WHITE[1], COL_SCREEN_WHITE[2], COL_SCREEN_WHITE[3],
          0, 0, 0);
      tess.draw();

      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
    }
  }

  private void renderLabelText() {
    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    GlStateManager.pushMatrix();

    float textZ = FACE_Z - SIGN_FRAME - 0.5f;
    float upperCenterY = (SIGN_DIVIDER_Y + SIGN_TOP - 1.0f) / 2.0f;
    GlStateManager.translate(CX, upperCenterY, textZ);
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
  }

  private void renderSpeedText(TileEntityOverheadSpeedLimit te) {
    int speed = te.getSpeedValue();
    String speedStr = String.valueOf(speed);

    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();

    float textZ = FACE_Z - SIGN_FRAME - 0.6f;
    float lowerCenterY = (SIGN_BOTTOM + 1.0f + SIGN_DIVIDER_Y) / 2.0f;
    GlStateManager.translate(CX, lowerCenterY, textZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(SPEED_TEXT_SCALE, -SPEED_TEXT_SCALE, SPEED_TEXT_SCALE);

    GlStateManager.depthMask(false);
    GlStateManager.enableTexture2D();

    int textWidth = fr.getStringWidth(speedStr);
    fr.drawString(speedStr, -textWidth / 2, -fr.FONT_HEIGHT / 2, TEXT_COLOR_BLACK);

    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }
}
