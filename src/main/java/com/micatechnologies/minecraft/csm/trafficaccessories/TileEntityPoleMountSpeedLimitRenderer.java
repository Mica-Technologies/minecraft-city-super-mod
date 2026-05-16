package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.util.ArrayList;
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

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    int worldSkyLight = (combinedLight >> 16) & 0xFFFF;
    int worldBlockLight = combinedLight & 0xFFFF;
    int faceSkyLight = fullScreen ? LIGHTMAP_FULLBRIGHT_SKY : worldSkyLight;
    int faceBlockLight = fullScreen ? LIGHTMAP_FULLBRIGHT_BLOCK : worldBlockLight;

    renderSignPanel(te, fullScreen, faceSkyLight, faceBlockLight);

    renderSpeedText(te);
    renderLabelText();

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();

    GlStateManager.popMatrix();
  }

  /**
   * The housing-colored border + frame + sign face. Mirrors the overhead variant's
   * face split: full-screen mode lights the whole face; otherwise the upper area is
   * the housing-tinted label panel and only the inset LED screen is fullbright.
   */
  private void renderSignPanel(TileEntityPoleMountSpeedLimit te, boolean fullScreen,
      int faceSkyLight, int faceBlockLight) {
    TrafficSignalBodyColor color = te.getHousingColor();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    int worldSky = te.getWorld() == null ? faceSkyLight :
        ((te.getWorld().getCombinedLight(te.getPos(), 0) >> 16) & 0xFFFF);
    int worldBlock = te.getWorld() == null ? faceBlockLight :
        (te.getWorld().getCombinedLight(te.getPos(), 0) & 0xFFFF);

    // Outer housing-colored border (slightly larger than the visible face)
    List<RenderHelper.Box> border = new ArrayList<>();
    border.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2 - SIGN_FRAME, SIGN_BOTTOM - SIGN_FRAME,
            CZ - SIGN_DEPTH / 2},
        new float[]{CX + SIGN_WIDTH / 2 + SIGN_FRAME, SIGN_TOP + SIGN_FRAME,
            CZ + SIGN_DEPTH / 2}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(border, buf,
        color.getRed() * 0.7f, color.getGreen() * 0.7f, color.getBlue() * 0.7f, 1.0f,
        0, 0, 0, worldSky, worldBlock);
    tess.draw();

    // Sign background — light gray (or fullbright white when in full-screen mode)
    float[] bgColor = fullScreen ? COL_SCREEN_WHITE : COL_SIGN_BG;
    List<RenderHelper.Box> bgFace = new ArrayList<>();
    bgFace.add(new RenderHelper.Box(
        new float[]{CX - SIGN_WIDTH / 2, SIGN_BOTTOM, CZ - SIGN_DEPTH / 2 - 0.05f},
        new float[]{CX + SIGN_WIDTH / 2, SIGN_TOP, CZ - SIGN_DEPTH / 2 + 0.3f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bgFace, buf,
        bgColor[0], bgColor[1], bgColor[2], bgColor[3], 0, 0, 0,
        faceSkyLight, faceBlockLight);
    tess.draw();

    if (!fullScreen) {
      // Inset LED screen for the digits — always fullbright
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
  }

  private void renderLabelText() {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();

    GlStateManager.pushMatrix();

    float faceZ = CZ - SIGN_DEPTH / 2 - 0.1f;
    float upperCenterY = (SIGN_DIVIDER_Y + SIGN_TOP) / 2.0f;
    GlStateManager.translate(CX, upperCenterY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(LABEL_TEXT_SCALE, -LABEL_TEXT_SCALE, LABEL_TEXT_SCALE);

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

  private void renderSpeedText(TileEntityPoleMountSpeedLimit te) {
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
