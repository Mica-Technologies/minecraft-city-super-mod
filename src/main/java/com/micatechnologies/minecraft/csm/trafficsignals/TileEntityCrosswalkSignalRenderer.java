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

public class TileEntityCrosswalkSignalRenderer extends
    TileEntitySpecialRenderer<TileEntityCrosswalkSignal> {

  @Override
  public void render(TileEntityCrosswalkSignal te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    int countdown = te.getCurrentCountdown();
    if (countdown < 0) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof AbstractBlockControllableSignal)) return;
    int color = state.getValue(AbstractBlockControllableSignal.COLOR);
    if (color != 1) return;

    EnumFacing facing = state.getValue(AbstractBlockControllableSignal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) y + 0.5625f, (float) z + 0.5f);

    // Rotate to match the blockstate model rotation. The blockstate rotates the model:
    // north=0, east=90, south=180, west=270. We apply the same rotation so the text
    // overlay stays aligned with the rotated model's display face.
    float rot = 0;
    switch (facing) {
      case NORTH: rot = 180; break;
      case EAST: rot = 90; break;
      case SOUTH: rot = 0; break;
      case WEST: rot = -90; break;
      default: break;
    }
    GlStateManager.rotate(rot, 0, 1, 0);

    // Move to just in front of the block face, shifted down and right for countdown area
    // The model's north face is at Z=1/16=0.0625 from block origin.
    // Block center is at 0.5, so the face is at -0.4375 from center.
    GlStateManager.translate(0.12f, -0.15f, -0.4376f);

    // Scale: make the number tall enough to fill the lower portion of the signal face
    // Use non-uniform scale to make the number taller
    float scaleX = 0.04f;
    float scaleY = 0.055f;
    GlStateManager.scale(scaleX, -scaleY, scaleX);

    // Fullbright
    int prevBX = (int) OpenGlHelper.lastBrightnessX;
    int prevBY = (int) OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    GlStateManager.disableDepth(); // Render on top of everything
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    String text = String.valueOf(countdown);
    int textWidth = font.getStringWidth(text);
    font.drawString(text, -textWidth / 2, -4, 0xFFFF8800);

    GlStateManager.enableDepth();
    GlStateManager.enableLighting();
    GlStateManager.depthMask(true);
    GlStateManager.disableBlend();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);

    GlStateManager.popMatrix();
  }
}
