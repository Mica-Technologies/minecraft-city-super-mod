package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.codeutils.RoadSurfaceHeight;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalFlashPattern;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
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
 * Renderer for {@link BlockRadarSpeedSign}: a coloured sign blank carrying a printed YOUR SPEED
 * legend over a dark LED window, with an optional white SPEED LIMIT header panel above it.
 *
 * <p>The legend is <em>painted on the blank in black</em>, not lit — only the window's digits
 * are LEDs. That split is what makes a real unit read as a sign with a display in it rather than
 * as a screen, and it is why the legend takes the world's light while the digits are drawn
 * fullbright.</p>
 *
 * <p>Every geometry pass binds its own texture. {@link CsmFontRenderer#drawString} binds the
 * font atlas and never restores what was there, so untextured geometry drawn after any text
 * samples the font sheet at its centre — padding, hence invisible. That cost a full debugging
 * round on the school zone beacon; nothing here trusts inherited texture state.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityRadarSpeedSignRenderer
    extends TileEntitySpecialRenderer<TileEntityRadarSpeedSign> {

  // Main panel: one block wide, a little under a block and a half tall. The 3:4 proportion is
  // the one the real boards use, and it is what leaves the legend room to be two full lines.
  private static final float PANEL_W = 16.0f;
  private static final float PANEL_H = 22.0f;
  private static final float PANEL_D = 1.0f;
  private static final float PANEL_BORDER = 0.8f;
  private static final float PANEL_RADIUS = 1.5f;
  private static final int ROUND_STEPS = 3;

  // Optional SPEED LIMIT header, drawn above the main panel when one is fitted. An R2-1 blank
  // is 24 x 30 inches, so at the board's 16-wide face the header has to be 20 tall; the first
  // pass made it 14 and it read as a squashed placard rather than a speed limit sign.
  private static final float HEADER_H = 20.0f;
  private static final float HEADER_GAP = 0.9f;
  /** The posted number carries an R2-1's oversized numeral, not the legend's height. */
  private static final float HEADER_NUMBER_SCALE = 1.85f;

  // The LED window: a dark recess in the lower half of the blank.
  private static final float WINDOW_W = 13.2f;
  private static final float WINDOW_H = 9.6f;
  private static final float WINDOW_BOTTOM_INSET = 1.2f;

  /** Pivot: the middle of the cell, pushed back so the panel's rear seats on the cell's back. */
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;
  private static final float CZ = 16.0f - PANEL_D / 2.0f;

  private static final float[] COL_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};
  private static final float[] COL_WINDOW = {0.055f, 0.05f, 0.045f, 1.0f};
  private static final float[] COL_HEADER = {0.94f, 0.94f, 0.93f, 1.0f};
  /** LED amber, and the red the boards switch to when they stop showing a number. */
  private static final int LED_AMBER = 0xFFAA1E;
  private static final int LED_RED = 0xFF3A20;
  private static final int TEXT_BLACK = 0x111111;

  private static final float TEXT_SCALE_LEGEND = 0.48f;
  private static final float TEXT_SCALE_HEADER = 0.42f;
  private static final float TEXT_SCALE_DIGITS = 0.95f;
  private static final float TEXT_SCALE_MESSAGE = 0.44f;

  // Legend line centres, measured from the panel's middle.
  private static final float LINE_YOUR = 8.0f;
  private static final float LINE_SPEED = 3.0f;
  // Header line centres, measured from the header panel's middle. Spaced off the real line
  // heights rather than by eye, so the numeral keeps the room an R2-1 gives it.
  private static final float HEADER_LINE_SPEED = 7.2f;
  private static final float HEADER_LINE_LIMIT = 2.6f;
  private static final float HEADER_LINE_NUMBER = -4.6f;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT = 240;

  @Override
  public void render(TileEntityRadarSpeedSign te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }

    EnumFacing facing;
    try {
      facing = te.getWorld().getBlockState(te.getPos()).getValue(BlockHorizontal.FACING);
    } catch (IllegalArgumentException notThisBlockAnyMore) {
      return;
    }

    // A renderer draws in world space and knows nothing about the block's render offset, so
    // the settle onto the road below has to be applied here too. See RoadSurfaceHeight.
    double settle = RoadSurfaceHeight.offsetFor(te.getWorld(), te.getPos());

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y + settle, z);
    GlStateManager.translate(0.5, 0.0, 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    // Scale about the bottom of the assembly, not its centre, or a bigger board grows down
    // through the ground as well as up.
    float scale = te.getScale();
    float pivotY = CY - PANEL_H / 2.0f - PANEL_BORDER;
    GlStateManager.translate(CX, pivotY, CZ);
    GlStateManager.scale(scale, scale, scale);
    GlStateManager.translate(-CX, -pivotY, -CZ);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    int combined = te.getWorld().getCombinedLight(te.getPos(), 0);
    int sky = (combined >> 16) & 0xFFFF;
    int block = combined & 0xFFFF;

    renderPanel(te, sky, block);
    renderLegend(te);
    if (te.isShowHeader()) {
      renderHeaderText(te);
    }
    renderReadout(te);

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  private static float rotationFor(EnumFacing facing) {
    switch (facing) {
      case WEST:
        return 90;
      case SOUTH:
        return 180;
      case EAST:
        return 270;
      case NORTH:
      default:
        return 0;
    }
  }

  /** The bottom edge of the main panel's face, which everything else is measured from. */
  private static float panelBottom() {
    return CY - PANEL_H / 2.0f;
  }

  private static float panelTop() {
    return CY + PANEL_H / 2.0f;
  }

  private static float headerBottom() {
    return panelTop() + PANEL_BORDER + HEADER_GAP + PANEL_BORDER;
  }

  private void renderPanel(TileEntityRadarSpeedSign te, int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float halfW = PANEL_W / 2.0f;
    float backZ = CZ + PANEL_D / 2.0f;
    float frontZ = CZ - PANEL_D / 2.0f;
    float faceRadius = PANEL_RADIUS - PANEL_BORDER;

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    // Border blanks first, then the coloured faces in front of them, so the border reads as the
    // painted edge stripe every sign blank carries rather than as a separate object.
    List<RenderHelper.Box> border = new ArrayList<>();
    RenderHelper.addRoundedRect(border,
        CX - halfW - PANEL_BORDER, panelBottom() - PANEL_BORDER,
        CX + halfW + PANEL_BORDER, panelTop() + PANEL_BORDER,
        frontZ, backZ, PANEL_RADIUS, ROUND_STEPS, true, true);
    if (te.isShowHeader()) {
      RenderHelper.addRoundedRect(border,
          CX - halfW - PANEL_BORDER, headerBottom() - PANEL_BORDER,
          CX + halfW + PANEL_BORDER, headerBottom() + HEADER_H + PANEL_BORDER,
          frontZ, backZ, PANEL_RADIUS, ROUND_STEPS, true, true);
    }
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(border, buf,
        COL_BORDER[0], COL_BORDER[1], COL_BORDER[2], COL_BORDER[3], 0, 0, 0, sky, block);
    tess.draw();

    MutcdSignFaceColor colour = te.getFaceColor();
    List<RenderHelper.Box> face = new ArrayList<>();
    RenderHelper.addRoundedRect(face,
        CX - halfW, panelBottom(), CX + halfW, panelTop(),
        frontZ - 0.1f, frontZ + 0.2f, faceRadius, ROUND_STEPS, true, true);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(face, buf,
        colour.getRed(), colour.getGreen(), colour.getBlue(), 1.0f, 0, 0, 0, sky, block);
    tess.draw();

    if (te.isShowHeader()) {
      // The header is a regulatory sign, so it stays white whatever colour the board is.
      List<RenderHelper.Box> header = new ArrayList<>();
      RenderHelper.addRoundedRect(header,
          CX - halfW, headerBottom(), CX + halfW, headerBottom() + HEADER_H,
          frontZ - 0.1f, frontZ + 0.2f, faceRadius, ROUND_STEPS, true, true);
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(header, buf,
          COL_HEADER[0], COL_HEADER[1], COL_HEADER[2], COL_HEADER[3], 0, 0, 0, sky, block);
      tess.draw();
    }

    // The LED window: a dark recess sitting proud of the face so its edge catches as a lip.
    List<RenderHelper.Box> window = new ArrayList<>();
    RenderHelper.addRoundedRect(window,
        CX - WINDOW_W / 2.0f, windowBottom(),
        CX + WINDOW_W / 2.0f, windowBottom() + WINDOW_H,
        frontZ - 0.25f, frontZ - 0.05f, 0.8f, ROUND_STEPS, true, true);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(window, buf,
        COL_WINDOW[0], COL_WINDOW[1], COL_WINDOW[2], COL_WINDOW[3], 0, 0, 0, sky, block);
    tess.draw();
  }

  private static float windowBottom() {
    return panelBottom() + WINDOW_BOTTOM_INSET;
  }

  /** The printed YOUR SPEED legend. Black paint on the blank, so it takes the world's light. */
  private void renderLegend(TileEntityRadarSpeedSign te) {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    float faceZ = CZ - PANEL_D / 2.0f - 0.35f;
    float legendCentre = (windowBottom() + WINDOW_H + panelTop()) / 2.0f;

    GlStateManager.pushMatrix();
    GlStateManager.translate(CX, legendCentre, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.depthMask(false);

    drawCentred(fr, "YOUR", 0, LINE_YOUR - 5.0f, TEXT_SCALE_LEGEND, PANEL_W - 2.0f, TEXT_BLACK);
    drawCentred(fr, "SPEED", 0, LINE_SPEED - 5.0f, TEXT_SCALE_LEGEND, PANEL_W - 2.0f, TEXT_BLACK);

    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
  }

  /** The optional regulatory header: SPEED LIMIT over the posted number. */
  private void renderHeaderText(TileEntityRadarSpeedSign te) {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    float faceZ = CZ - PANEL_D / 2.0f - 0.35f;
    float centre = headerBottom() + HEADER_H / 2.0f;

    GlStateManager.pushMatrix();
    GlStateManager.translate(CX, centre, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.depthMask(false);

    drawCentred(fr, "SPEED", 0, HEADER_LINE_SPEED, TEXT_SCALE_HEADER, PANEL_W - 2.0f, TEXT_BLACK);
    drawCentred(fr, "LIMIT", 0, HEADER_LINE_LIMIT, TEXT_SCALE_HEADER, PANEL_W - 2.0f, TEXT_BLACK);
    drawCentred(fr, String.valueOf(te.getPostedSpeed()), 0, HEADER_LINE_NUMBER,
        TEXT_SCALE_HEADER * HEADER_NUMBER_SCALE, PANEL_W - 2.0f, TEXT_BLACK);

    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
  }

  /**
   * What is actually lit in the window: nothing, the speed, or SLOW DOWN. Over the posted speed
   * the lit content flashes, which is the one behaviour every real unit shares.
   */
  private void renderReadout(TileEntityRadarSpeedSign te) {
    int reading = te.getReading();
    if (reading <= 0) {
      return;
    }
    if (te.isOverLimit()
        && !TrafficSignalFlashPattern.B.isFlashLit(CsmRenderUtils.gameMillis(te.getWorld()))) {
      return;
    }

    CsmFontRenderer fr = CsmFontRenderer.electronicSign();
    float faceZ = CZ - PANEL_D / 2.0f - 0.45f;
    float centre = windowBottom() + WINDOW_H / 2.0f;

    // CsmFontRenderer.drawString uses POSITION_TEX, which carries no per-vertex lightmap, so it
    // inherits whatever the last BLOCK-format draw set -- the world's combined light. Forcing
    // fullbright here is what makes the LEDs glow at night instead of dimming with the sign.
    float previousBrightnessX = OpenGlHelper.lastBrightnessX;
    float previousBrightnessY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        LIGHTMAP_FULLBRIGHT, LIGHTMAP_FULLBRIGHT);

    GlStateManager.pushMatrix();
    GlStateManager.translate(CX, centre, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.depthMask(false);

    float maxWidth = WINDOW_W - 1.6f;
    if (te.isSlowDown()) {
      drawCentred(fr, "SLOW", 0, 2.3f, TEXT_SCALE_MESSAGE, maxWidth, LED_RED);
      drawCentred(fr, "DOWN", 0, -2.3f, TEXT_SCALE_MESSAGE, maxWidth, LED_RED);
    } else {
      drawCentred(fr, String.valueOf(reading), 0, 0, TEXT_SCALE_DIGITS, maxWidth, LED_AMBER);
    }

    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        previousBrightnessX, previousBrightnessY);
  }

  /**
   * Draws one line centred on the panel's own axis, shrinking it if it would not fit.
   *
   * <p>The auto-fit is not cosmetic: the posted speed can be three digits and SPEED is the
   * widest word on the blank, so a fixed scale either wastes room or runs off the edge
   * depending on the content. The Y scale is negative because the font renderer draws downward
   * and this matrix has already been flipped to face the viewer.</p>
   */
  private void drawCentred(CsmFontRenderer fr, String text, float centreX, float centreY,
      float scale, float maxWidth, int colour) {
    int width = fr.getStringWidth(text);
    float fitted = scale;
    if (width * scale > maxWidth && width > 0) {
      fitted = maxWidth / width;
    }
    GlStateManager.pushMatrix();
    GlStateManager.translate(centreX, centreY, 0);
    GlStateManager.scale(fitted, -fitted, fitted);
    fr.drawString(text, -width / 2, -fr.FONT_HEIGHT / 2, colour);
    GlStateManager.popMatrix();
  }
}
