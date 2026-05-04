package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap.TextureInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVertexData;
import java.util.ArrayList;
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

public class TileEntityPortableSpeedLimitRenderer
    extends TileEntitySpecialRenderer<TileEntityVariableSpeedLimit> {

  // Sign panel — narrower and taller than the DMS for a speed limit sign shape
  private static final float SIGN_WIDTH = 27.0f;
  private static final float SIGN_HEIGHT = 36.0f;
  private static final float SIGN_DEPTH = 3.0f;
  private static final float SIGN_FRAME = 1.5f;

  // Mast
  private static final float MAST_HEIGHT = 24.0f;
  private static final float MAST_SIZE = 4.5f;

  // Trailer body (same as PCMS)
  private static final float TRAILER_LENGTH = 36.0f;
  private static final float TRAILER_WIDTH = 19.5f;
  private static final float TRAILER_HEIGHT = 6.0f;
  private static final float TRAILER_GROUND_CLEARANCE = 4.5f;

  // Wheels
  private static final float WHEEL_DIAMETER = 7.5f;
  private static final float WHEEL_WIDTH = 2.25f;

  // Outrigger legs
  private static final float OUTRIGGER_SPREAD = 27.0f;
  private static final float OUTRIGGER_LEG_SIZE = 1.5f;
  private static final float OUTRIGGER_FOOT_SIZE = 3.75f;

  // Speed number text — black digits on white LED screen
  private static final float SPEED_TEXT_SCALE = 1.35f;
  private static final int TEXT_COLOR_BLACK = 0x111111;

  // Derived positions
  private static final float CX = 8.0f;
  private static final float CZ = 8.0f;

  private static final float TRAILER_BOTTOM = TRAILER_GROUND_CLEARANCE;
  private static final float TRAILER_TOP = TRAILER_BOTTOM + TRAILER_HEIGHT;
  private static final float MAST_BOTTOM = TRAILER_TOP;
  private static final float MAST_TOP = MAST_BOTTOM + MAST_HEIGHT;
  private static final float SIGN_BOTTOM = MAST_TOP;
  private static final float SIGN_TOP = SIGN_BOTTOM + SIGN_HEIGHT;

  // Sign face zones: upper "SPEED LIMIT" text area, lower speed number area
  private static final float SIGN_DIVIDER_Y = SIGN_BOTTOM + SIGN_HEIGHT * 0.42f;

  // Colors
  private static final float[] COL_DARK_GRAY = {0.18f, 0.18f, 0.18f, 1.0f};
  private static final float[] COL_SIGN_BG = {0.85f, 0.85f, 0.85f, 1.0f};
  private static final float[] COL_SCREEN_WHITE = {0.95f, 0.95f, 0.95f, 1.0f};
  private static final float[] COL_FRAME = {0.45f, 0.45f, 0.47f, 1.0f};
  private static final float[] COL_RUBBER = {0.10f, 0.10f, 0.10f, 1.0f};
  private static final float[] COL_SILVER = {0.50f, 0.50f, 0.52f, 1.0f};
  private static final float[] COL_SIGN_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};

  // Trailer color palette
  private static final float[][] TRAILER_COLORS = {
      {0.93f, 0.45f, 0.08f, 1.0f},  // 0: Orange
      {0.95f, 0.85f, 0.10f, 1.0f},  // 1: Yellow
      {0.12f, 0.12f, 0.12f, 1.0f},  // 2: Black
      {0.70f, 0.70f, 0.72f, 1.0f},  // 3: Silver
      {0.92f, 0.92f, 0.92f, 1.0f},  // 4: White
  };

  // Angle rotations
  private static final float[] ANGLE_ROTATIONS = {
      0f, -15f, 15f, -45f, 45f
  };

  // Flasher constants (reuses traffic signal 8-inch geometry)
  private static final ResourceLocation SIGNAL_ATLAS =
      new ResourceLocation("csm", "textures/blocks/trafficsignals/lights/atlas.png");

  private static final float FLASHER_BODY_R = 0.094f;
  private static final float FLASHER_BODY_G = 0.094f;
  private static final float FLASHER_BODY_B = 0.094f;

  private static final float VISOR_TINT_SCALE = 1.04f;
  private static final float VISOR_TINT_BASE = 0.01f;
  private static final float VISOR_OUTER_R =
      Math.min(1.0f, FLASHER_BODY_R * VISOR_TINT_SCALE + VISOR_TINT_BASE);
  private static final float VISOR_OUTER_G =
      Math.min(1.0f, FLASHER_BODY_G * VISOR_TINT_SCALE + VISOR_TINT_BASE);
  private static final float VISOR_OUTER_B =
      Math.min(1.0f, FLASHER_BODY_B * VISOR_TINT_SCALE + VISOR_TINT_BASE);

  private static final float VISOR_TILT_DEGREES = 9.0f;
  private static final float VISOR_PIVOT_Z = 11.0f;
  private static final float VISOR_CENTER_X = 8.0f;
  private static final float VISOR_CENTER_Y = 6.0f;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  @Override
  public void render(TileEntityVariableSpeedLimit te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }

    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(BlockHorizontal.FACING);

    int colorIdx = te.getTrailerColor();
    if (colorIdx < 0 || colorIdx >= TRAILER_COLORS.length) colorIdx = 0;
    float[] trailerCol = TRAILER_COLORS[colorIdx];

    int angleIdx = te.getSignAngle();
    if (angleIdx < 0 || angleIdx >= ANGLE_ROTATIONS.length) angleIdx = 0;
    float angleOffset = ANGLE_ROTATIONS[angleIdx];

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
    GlStateManager.rotate(rotY + angleOffset, 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);

    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int sky = (combinedLight >> 16) & 0xFFFF;
    int block = combinedLight & 0xFFFF;

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    renderTrailer(trailerCol, sky, block);
    renderWheels(sky, block);
    renderOutriggers(trailerCol, sky, block);
    renderMast(sky, block);
    renderSignPanel(sky, block);
    renderFlashers(te, sky, block);

    renderSpeedText(te);
    renderLabelText();

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  private void renderTrailer(float[] col, int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> boxes = new ArrayList<>();

    boxes.add(new RenderHelper.Box(
        new float[]{CX - TRAILER_WIDTH / 2, TRAILER_BOTTOM, CZ - TRAILER_LENGTH / 2 + 4},
        new float[]{CX + TRAILER_WIDTH / 2, TRAILER_TOP, CZ + TRAILER_LENGTH / 2 + 4}));

    boxes.add(new RenderHelper.Box(
        new float[]{CX - 1.0f, TRAILER_BOTTOM + 1, CZ + TRAILER_LENGTH / 2 + 4},
        new float[]{CX + 1.0f, TRAILER_BOTTOM + 2.5f, CZ + TRAILER_LENGTH / 2 + 10}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(boxes, buf, col[0], col[1], col[2], col[3], 0, 0, 0,
        sky, block);
    tess.draw();
  }

  private void renderWheels(int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> boxes = new ArrayList<>();

    float wheelY = TRAILER_GROUND_CLEARANCE - WHEEL_DIAMETER / 2 + 1;
    float wheelZ = CZ + 4;

    boxes.add(new RenderHelper.Box(
        new float[]{CX - TRAILER_WIDTH / 2 - WHEEL_WIDTH, wheelY, wheelZ - WHEEL_DIAMETER / 2},
        new float[]{CX - TRAILER_WIDTH / 2, wheelY + WHEEL_DIAMETER,
            wheelZ + WHEEL_DIAMETER / 2}));

    boxes.add(new RenderHelper.Box(
        new float[]{CX + TRAILER_WIDTH / 2, wheelY, wheelZ - WHEEL_DIAMETER / 2},
        new float[]{CX + TRAILER_WIDTH / 2 + WHEEL_WIDTH, wheelY + WHEEL_DIAMETER,
            wheelZ + WHEEL_DIAMETER / 2}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(boxes, buf,
        COL_RUBBER[0], COL_RUBBER[1], COL_RUBBER[2], COL_RUBBER[3], 0, 0, 0, sky, block);
    tess.draw();

    List<RenderHelper.Box> axle = new ArrayList<>();
    axle.add(new RenderHelper.Box(
        new float[]{CX - TRAILER_WIDTH / 2 - WHEEL_WIDTH, wheelY + WHEEL_DIAMETER / 2 - 0.5f,
            wheelZ - 0.5f},
        new float[]{CX + TRAILER_WIDTH / 2 + WHEEL_WIDTH, wheelY + WHEEL_DIAMETER / 2 + 0.5f,
            wheelZ + 0.5f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(axle, buf,
        COL_DARK_GRAY[0], COL_DARK_GRAY[1], COL_DARK_GRAY[2], COL_DARK_GRAY[3], 0, 0, 0,
        sky, block);
    tess.draw();
  }

  private void renderOutriggers(float[] col, int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> legs = new ArrayList<>();
    List<RenderHelper.Box> feet = new ArrayList<>();

    float legZ1 = CZ - TRAILER_LENGTH / 2 + 6;
    float legZ2 = CZ + TRAILER_LENGTH / 2 + 2;

    float[][] legPositions = {
        {CX - OUTRIGGER_SPREAD / 2, legZ1},
        {CX + OUTRIGGER_SPREAD / 2, legZ1},
        {CX - OUTRIGGER_SPREAD / 2, legZ2},
        {CX + OUTRIGGER_SPREAD / 2, legZ2}
    };

    for (float[] p : legPositions) {
      legs.add(new RenderHelper.Box(
          new float[]{p[0] - OUTRIGGER_LEG_SIZE / 2, 0, p[1] - OUTRIGGER_LEG_SIZE / 2},
          new float[]{p[0] + OUTRIGGER_LEG_SIZE / 2, TRAILER_BOTTOM,
              p[1] + OUTRIGGER_LEG_SIZE / 2}));

      feet.add(new RenderHelper.Box(
          new float[]{p[0] - OUTRIGGER_FOOT_SIZE / 2, 0, p[1] - OUTRIGGER_FOOT_SIZE / 2},
          new float[]{p[0] + OUTRIGGER_FOOT_SIZE / 2, 0.5f, p[1] + OUTRIGGER_FOOT_SIZE / 2}));

      float armEndX = p[0] > CX ? CX + TRAILER_WIDTH / 2 : CX - TRAILER_WIDTH / 2;
      legs.add(new RenderHelper.Box(
          new float[]{Math.min(p[0], armEndX) - 0.25f, TRAILER_BOTTOM - 0.5f,
              p[1] - OUTRIGGER_LEG_SIZE / 2},
          new float[]{Math.max(p[0], armEndX) + 0.25f, TRAILER_BOTTOM + 0.5f,
              p[1] + OUTRIGGER_LEG_SIZE / 2}));
    }

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(legs, buf, col[0], col[1], col[2], col[3], 0, 0, 0,
        sky, block);
    tess.draw();

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(feet, buf,
        COL_SILVER[0], COL_SILVER[1], COL_SILVER[2], COL_SILVER[3], 0, 0, 0, sky, block);
    tess.draw();
  }

  private void renderMast(int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> boxes = new ArrayList<>();

    boxes.add(new RenderHelper.Box(
        new float[]{CX - MAST_SIZE / 2, MAST_BOTTOM, CZ - MAST_SIZE / 2},
        new float[]{CX + MAST_SIZE / 2, MAST_TOP, CZ + MAST_SIZE / 2}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(boxes, buf,
        COL_DARK_GRAY[0], COL_DARK_GRAY[1], COL_DARK_GRAY[2], COL_DARK_GRAY[3], 0, 0, 0,
        sky, block);
    tess.draw();
  }

  private void renderSignPanel(int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // Outer frame/border
    List<RenderHelper.Box> border = new ArrayList<>();
    border.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
            CZ - SIGN_DEPTH / 2},
        new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
            CZ + SIGN_DEPTH / 2}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(border, buf,
        COL_SIGN_BORDER[0], COL_SIGN_BORDER[1], COL_SIGN_BORDER[2], COL_SIGN_BORDER[3],
        0, 0, 0, sky, block);
    tess.draw();

    // Subtle gray sign background (full sign face — "SPEED LIMIT" area + behind screen)
    List<RenderHelper.Box> bgFace = new ArrayList<>();
    bgFace.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, CZ - SIGN_DEPTH / 2 - 0.05f},
        new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, CZ - SIGN_DEPTH / 2 + 0.3f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bgFace, buf,
        COL_SIGN_BG[0], COL_SIGN_BG[1], COL_SIGN_BG[2], COL_SIGN_BG[3], 0, 0, 0,
        sky, block);
    tess.draw();

    // Bright white LED screen (inset from sign edges) — rendered at fullbright per-vertex
    float screenInset = 2.25f;
    List<RenderHelper.Box> screenFace = new ArrayList<>();
    screenFace.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 + screenInset, SIGN_BOTTOM + screenInset,
            CZ - SIGN_DEPTH / 2 - 0.1f},
        new float[]{CX + SIGN_WIDTH / 2 - screenInset, SIGN_DIVIDER_Y - 1.0f,
            CZ - SIGN_DEPTH / 2 + 0.25f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(screenFace, buf,
        COL_SCREEN_WHITE[0], COL_SCREEN_WHITE[1], COL_SCREEN_WHITE[2], COL_SCREEN_WHITE[3],
        0, 0, 0, LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK);
    tess.draw();
  }

  private void renderFlashers(TileEntityVariableSpeedLimit te, int sky, int block) {
    int mode = te.getFlasherMode();
    if (mode == TileEntityVariableSpeedLimit.FLASHER_NONE) {
      return;
    }

    boolean bulbLit = false;
    if (mode == TileEntityVariableSpeedLimit.FLASHER_ON) {
      bulbLit = (System.currentTimeMillis() / 500) % 2 == 0;
    }

    float signLeftEdge = CX - SIGN_WIDTH / 2 - SIGN_FRAME;
    float signRightEdge = CX + SIGN_WIDTH / 2 + SIGN_FRAME;
    float flasherY = SIGN_TOP + SIGN_FRAME - 6.0f;

    float zOff = (CZ - SIGN_DEPTH / 2) - 11.0f;

    float leftXOff = (signLeftEdge - 4.0f) - VISOR_CENTER_X;
    float leftYOff = flasherY - VISOR_CENTER_Y;

    float rightXOff = (signRightEdge + 4.0f) - VISOR_CENTER_X;
    float rightYOff = flasherY - VISOR_CENTER_Y;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // Housing + mounting arms — white1px bound, BLOCK format with world lightmap
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    addFlasherHousing(buf, leftXOff, leftYOff, zOff, sky, block);
    addFlasherHousing(buf, rightXOff, rightYOff, zOff, sky, block);

    float armZ1 = CZ - SIGN_DEPTH / 2;
    float armZ2 = armZ1 + 2.0f;
    List<RenderHelper.Box> arms = new ArrayList<>();
    arms.add(new RenderHelper.Box(
        new float[]{signLeftEdge - 4.0f, flasherY - 0.5f, armZ1},
        new float[]{signLeftEdge, flasherY + 0.5f, armZ2}));
    arms.add(new RenderHelper.Box(
        new float[]{signRightEdge, flasherY - 0.5f, armZ1},
        new float[]{signRightEdge + 4.0f, flasherY + 0.5f, armZ2}));
    RenderHelper.addBoxesToBufferLit(arms, buf,
        COL_FRAME[0], COL_FRAME[1], COL_FRAME[2], COL_FRAME[3], 0, 0, 0, sky, block);

    tess.draw();

    // Bulb faces — atlas texture with fullbright per-vertex lightmap
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    Minecraft.getMinecraft().getTextureManager().bindTexture(SIGNAL_ATLAS);

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    addFlasherBulb(buf, leftXOff, leftYOff, zOff, bulbLit);
    addFlasherBulb(buf, rightXOff, rightYOff, zOff, bulbLit);

    tess.draw();
  }

  private void addFlasherHousing(BufferBuilder buf, float xOff, float yOff, float zOff,
      int sky, int block) {
    RenderHelper.addBoxesToBufferLit(
        TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA, buf,
        FLASHER_BODY_R, FLASHER_BODY_G, FLASHER_BODY_B, 1.0f, xOff, yOff, zOff, sky, block);

    RenderHelper.addBoxesToBufferLit(
        TrafficSignalVertexData.SIGNAL_DOOR_8INCH_VERTEX_DATA, buf,
        FLASHER_BODY_R, FLASHER_BODY_G, FLASHER_BODY_B, 1.0f, xOff, yOff, zOff, sky, block);

    RenderHelper.addTiltedBoxesToBufferDualColorLit(
        TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA, buf,
        VISOR_OUTER_R, VISOR_OUTER_G, VISOR_OUTER_B,
        0f, 0f, 0f, 1.0f,
        xOff, yOff, zOff, VISOR_PIVOT_Z + zOff, VISOR_TILT_DEGREES,
        VISOR_CENTER_X, VISOR_CENTER_Y, 0f, sky, block);
  }

  private void addFlasherBulb(BufferBuilder buf, float xOff, float yOff, float zOff,
      boolean lit) {
    TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.YELLOW, lit);

    float fullSize = 8.0f;
    float sizeScale = fullSize / 12f;
    float inset = fullSize * 0.02f;
    float size = fullSize - inset * 2f;
    float sectionOffset = (12f - fullSize) / 2f;
    float baseX = 2f + inset + xOff + sectionOffset;
    float baseY = yOff + inset + sectionOffset;
    float z = VISOR_PIVOT_Z + (10.4f - VISOR_PIVOT_Z) * sizeScale + zOff;

    float u1 = texInfo.getU1();
    float v1 = texInfo.getV1();
    float u2 = texInfo.getU2();
    float v2 = texInfo.getV2();

    bulbVertex(buf, baseX, baseY, z, u2, v2);
    bulbVertex(buf, baseX + size, baseY, z, u1, v2);
    bulbVertex(buf, baseX + size, baseY + size, z, u1, v1);
    bulbVertex(buf, baseX, baseY + size, z, u2, v1);
  }

  private static void bulbVertex(BufferBuilder buf, float x, float y, float z,
      float u, float v) {
    buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v)
        .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
  }

  private void renderLabelText() {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.1f;
    float upperCenterY = (SIGN_DIVIDER_Y + SIGN_TOP) / 2.0f;
    GlStateManager.translate(CX, upperCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);

    float labelScale = 0.82f;
    GlStateManager.scale(labelScale, -labelScale, labelScale);

    GlStateManager.depthMask(false);

    String line1 = "SPEED";
    String line2 = "LIMIT";
    int w1 = fr.getStringWidth(line1);
    int w2 = fr.getStringWidth(line2);

    fr.drawString(line1, -w1 / 2, -fr.FONT_HEIGHT - 1, 0x000000);
    fr.drawString(line2, -w2 / 2, 1, 0x000000);

    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();
  }

  private void renderSpeedText(TileEntityVariableSpeedLimit te) {
    int speed = te.getSpeedValue();
    String speedStr = String.valueOf(speed);

    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.2f;
    float lowerCenterY = (SIGN_BOTTOM + SIGN_DIVIDER_Y) / 2.0f;
    GlStateManager.translate(CX, lowerCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(SPEED_TEXT_SCALE, -SPEED_TEXT_SCALE, SPEED_TEXT_SCALE);

    GlStateManager.depthMask(false);

    int textWidth = fr.getStringWidth(speedStr);
    fr.drawString(speedStr, -textWidth / 2, -fr.FONT_HEIGHT / 2, TEXT_COLOR_BLACK);

    GlStateManager.depthMask(true);

    GlStateManager.popMatrix();
  }
}
