package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

public class TileEntityCrosswalkSignalRenderer extends
    TileEntitySpecialRenderer<TileEntityCrosswalkSignal> {

  private static final float Z_EPSILON = 0.005f;

  // 7-segment display: segment rectangles in a 5-wide × 9-tall grid.
  // Gaps between horizontal and vertical segments mimic real LED displays.
  // {x1, y1, x2, y2} where y=0 is top, y=9 is bottom.
  private static final float[][] SEGMENTS = {
      {0.5f, 0.0f, 4.5f, 1.0f},   // a: top
      {3.8f, 0.5f, 5.0f, 4.2f},   // b: upper right
      {3.8f, 4.8f, 5.0f, 8.5f},   // c: lower right
      {0.5f, 8.0f, 4.5f, 9.0f},   // d: bottom
      {0.0f, 4.8f, 1.2f, 8.5f},   // e: lower left
      {0.0f, 0.5f, 1.2f, 4.2f},   // f: upper left
      {0.5f, 4.0f, 4.5f, 5.0f},   // g: middle
  };

  // Which segments are lit for digits 0-9 (a,b,c,d,e,f,g)
  private static final boolean[][] DIGIT_SEGMENTS = {
      {true,  true,  true,  true,  true,  true,  false}, // 0
      {false, true,  true,  false, false, false, false}, // 1
      {true,  true,  false, true,  true,  false, true},  // 2
      {true,  true,  true,  true,  false, false, true},  // 3
      {false, true,  true,  false, false, true,  true},  // 4
      {true,  false, true,  true,  false, true,  true},  // 5
      {true,  false, true,  true,  true,  true,  true},  // 6
      {true,  true,  true,  false, false, false, false}, // 7
      {true,  true,  true,  true,  true,  true,  true},  // 8
      {true,  true,  true,  true,  false, true,  true},  // 9
  };

  // Digit dimensions in block-space units
  private static final float DIGIT_HEIGHT = 0.6f;
  private static final float DIGIT_WIDTH = 0.22f;
  private static final float DIGIT_GAP = 0.04f;
  // Center of the countdown digit area (in the right half of the signal face)
  private static final float AREA_CENTER_X = 0.24f;

  // Countdown color: amber/orange (matches real-world countdown modules)
  private static final int COLOR_R = 255;
  private static final int COLOR_G = 136;
  private static final int COLOR_B = 0;
  private static final int COLOR_A = 255;

  @Override
  public void render(TileEntityCrosswalkSignal te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    int countdown = te.getCurrentCountdown();
    if (countdown < 0) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof AbstractBlockControllableCrosswalkSignal)) return;

    AbstractBlockControllableCrosswalkSignal block =
        (AbstractBlockControllableCrosswalkSignal) state.getBlock();

    int color = state.getValue(AbstractBlockControllableSignal.COLOR);
    if (color != 1) return;

    float zOffset = block.getCountdownZOffset();
    if (zOffset < 0) return;

    float yCenter = block.getCountdownYCenter();
    EnumFacing facing = state.getValue(AbstractBlockControllableSignal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + yCenter, (float) z + 0.5f);

    float rot = 0;
    switch (facing) {
      case NORTH: rot = 180; break;
      case EAST:  rot = 90;  break;
      case SOUTH: rot = 0;   break;
      case WEST:  rot = -90; break;
      default: break;
    }
    GlStateManager.rotate(rot, 0, 1, 0);

    // Position at the display face surface with small epsilon to prevent z-fighting
    GlStateManager.translate(0, 0, zOffset + Z_EPSILON);

    // Fullbright lightmap
    int prevBX = (int) OpenGlHelper.lastBrightnessX;
    int prevBY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableTexture2D();

    // Cap at 99 for two-digit display
    int displayValue = Math.min(countdown, 99);
    String text = String.valueOf(displayValue);
    int numDigits = text.length();

    float totalWidth = numDigits * DIGIT_WIDTH + (numDigits - 1) * DIGIT_GAP;
    float startX = AREA_CENTER_X - totalWidth / 2;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

    for (int i = 0; i < numDigits; i++) {
      int digit = text.charAt(i) - '0';
      float dx = startX + i * (DIGIT_WIDTH + DIGIT_GAP);
      drawDigitSegments(buf, digit, dx, -DIGIT_HEIGHT / 2, DIGIT_WIDTH, DIGIT_HEIGHT);
    }

    tess.draw();

    GlStateManager.enableTexture2D();
    GlStateManager.enableLighting();
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);

    GlStateManager.popMatrix();
  }

  /**
   * Draws the lit segments of a single 7-segment digit into the buffer.
   *
   * @param buf   buffer to append vertices to
   * @param digit 0-9
   * @param dx    left edge X in local face space
   * @param dy    bottom edge Y in local face space
   * @param w     digit width in block units
   * @param h     digit height in block units
   */
  private static void drawDigitSegments(BufferBuilder buf, int digit, float dx, float dy,
      float w, float h) {
    boolean[] segs = DIGIT_SEGMENTS[digit];
    for (int s = 0; s < 7; s++) {
      if (!segs[s]) continue;
      float[] seg = SEGMENTS[s];

      // Transform from 5×9 grid to local face coordinates.
      // Grid Y=0 is top, Y=9 is bottom; local Y+ is up.
      float x1 = dx + (seg[0] / 5f) * w;
      float x2 = dx + (seg[2] / 5f) * w;
      float y1 = dy + (1f - seg[3] / 9f) * h; // grid bottom → local bottom
      float y2 = dy + (1f - seg[1] / 9f) * h; // grid top → local top

      // Quad vertices (CCW winding, facing +Z toward viewer)
      buf.pos(x1, y1, 0).color(COLOR_R, COLOR_G, COLOR_B, COLOR_A).endVertex();
      buf.pos(x2, y1, 0).color(COLOR_R, COLOR_G, COLOR_B, COLOR_A).endVertex();
      buf.pos(x2, y2, 0).color(COLOR_R, COLOR_G, COLOR_B, COLOR_A).endVertex();
      buf.pos(x1, y2, 0).color(COLOR_R, COLOR_G, COLOR_B, COLOR_A).endVertex();
    }
  }
}
