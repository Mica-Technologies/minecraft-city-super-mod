package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

/**
 * TESR for crosswalk signals that renders a countdown number overlay during the
 * pedestrian clearance phase. The countdown is managed by TileEntityCrosswalkSignal
 * which learns the clearance duration from the traffic signal controller.
 */
public class TileEntityCrosswalkSignalRenderer extends
    TileEntitySpecialRenderer<TileEntityCrosswalkSignal> {

  @Override
  public void render(TileEntityCrosswalkSignal te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    int countdown = te.getCurrentCountdown();
    if (countdown < 0) return; // No countdown active, nothing to render

    // Only render during clearance phase (color=1)
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof AbstractBlockControllableSignal)) return;
    int color = state.getValue(AbstractBlockControllableSignal.COLOR);
    if (color != 1) return;

    EnumFacing facing = state.getValue(AbstractBlockControllableSignal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);

    // Rotate to face the same direction as the block
    float rotation = 0;
    switch (facing) {
      case SOUTH: rotation = 0; break;
      case WEST: rotation = 90; break;
      case NORTH: rotation = 180; break;
      case EAST: rotation = 270; break;
      default: break;
    }
    GlStateManager.rotate(rotation, 0, 1, 0);

    // Position the text on the front face of the signal
    GlStateManager.translate(0, -0.05f, -0.44f);
    GlStateManager.scale(0.02f, -0.02f, 0.02f);

    // Fullbright
    int prevBrightnessX = (int) OpenGlHelper.lastBrightnessX;
    int prevBrightnessY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    // Render the countdown number
    FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
    String countdownStr = String.valueOf(countdown);
    int textWidth = fontRenderer.getStringWidth(countdownStr);
    // Orange/amber color for countdown (matches real-world pedestrian countdown modules)
    int textColor = 0xFFFF8800;
    fontRenderer.drawString(countdownStr, -textWidth / 2, 0, textColor);

    GlStateManager.enableLighting();
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightnessX, prevBrightnessY);

    GlStateManager.popMatrix();
  }
}
