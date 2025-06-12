package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap.TextureInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;


public class TileEntityTrafficSignalHeadRenderer extends
    TileEntitySpecialRenderer<TileEntityTrafficSignalHead> {

  @Override
  public void render(TileEntityTrafficSignalHead te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    // Gather block state information
    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(AbstractBlockControllableSignalHead.FACING);
    int signalColorState = te.getWorld().getBlockState(te.getPos())
        .getValue(AbstractBlockControllableSignalHead.COLOR);

    // Gather tile entity information
    TrafficSignalSectionInfo[] sectionInfos = te.getSectionInfos(signalColorState);
    TrafficSignalBodyTilt bodyTilt = te.getBodyTilt(); // Body tilt angle
    DirectionSixteen bodyDirection =
        AbstractBlockControllableSignalHead.getTiltedFacing(bodyTilt, facing);

    // Push OpenGL transformation matrix.
    GL11.glPushMatrix();

    // 1. Translate to the block position in world space
    GL11.glTranslated(x, y, z);

    // 2. Scale down to 1/16th size (model coordinates to block coordinates)
    GL11.glScaled(0.0625, 0.0625, 0.0625);

    // 3. Translate to the block center (8, 8, 8)
    GL11.glTranslated(8, 8, 8);

    // 4. Rotate based on the facing direction (around the block center)
    float rotationAngle = bodyDirection.getRotation();
    GL11.glRotatef(rotationAngle, 0, 1, 0);

    // 5. Translate back from block center
    GL11.glTranslated(-8, -8, -8);

    // --- Compensation for tilt: shift slightly left/right for visual alignment ---
    // 1 model unit = 1/16 block, so shift by ±2 for tilt, ±4 for angle
    int tiltOffset = 0;
    if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.RIGHT_ANGLE) {
      tiltOffset = -4;
    } else if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.RIGHT_TILT) {
      tiltOffset = -2;
    } else if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.LEFT_TILT) {
      tiltOffset = 2;
    } else if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.LEFT_ANGLE) {
      tiltOffset = 4;
    }

    // Reverse the offset for SOUTH facing
    if (facing == net.minecraft.util.EnumFacing.SOUTH) {
      tiltOffset = -tiltOffset;
    }

    if (tiltOffset != 0) {
      GL11.glTranslated(tiltOffset, 0, 0);
    }

    // Now, rendering the signal body at (2, 0, 11) will always keep it at the correct offset
    // relative to the block, regardless of facing.

    GlStateManager.disableCull(); // Disable culling for visibility.
    GlStateManager.disableLighting(); // Disable lighting for color rendering.

    // Get first section index for rendering
    int firstSectionIndex = sectionInfos.length > 2 ? 1 : 0;

    // Render signal bodies and visors
    int startingSectionIndex = firstSectionIndex;
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      TrafficSignalBodyColor bodyColor = sectionInfo.getBodyColor();
      TrafficSignalBodyColor doorColor = sectionInfo.getDoorColor();
      TrafficSignalBodyColor visorColor = sectionInfo.getVisorColor();
      TrafficSignalVisorType visorType = sectionInfo.getVisorType();
      TrafficSignalBulbStyle bulbStyle = sectionInfo.getBulbStyle();
      TrafficSignalBulbType bulbType = sectionInfo.getBulbType();
      TrafficSignalBulbColor bulbColor = sectionInfo.getBulbCustomColor();
      boolean isBulbLit = sectionInfo.isBulbLit();

      // Render each section with the gathered colors and visor type
      renderSignalSection(te, bodyColor, doorColor, visorColor, visorType, startingSectionIndex);
      renderSignalBulb(te, bulbStyle,bulbType,bulbColor,isBulbLit, startingSectionIndex--);
    }

    GlStateManager.enableLighting(); // Enable lighting.
    GlStateManager.enableCull(); // Enable culling.

    GL11.glPopMatrix(); // Pop transformation matrix.
  }



  private void renderSignalSection(TileEntityTrafficSignalHead te,
      TrafficSignalBodyColor bodyColor,
      TrafficSignalBodyColor doorColor, TrafficSignalBodyColor visorColor,
      TrafficSignalVisorType visorType, int index) {
    // Push OpenGL transformation matrix.
    GL11.glPushMatrix();

    // Disable textures and set baseline color
    GlStateManager.disableTexture2D();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    renderSignalBody(te, bodyColor, index);
    renderFrontDoor(te, doorColor, index);
    renderSignalVisor(te, visorColor, visorType, index);

    // Restore textures and reset color
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    GlStateManager.enableTexture2D();

    // Pop OpenGL transformation matrix.
    GL11.glPopMatrix();
  }

  /**
   * Renders the traffic signal body. Coordinates are based on the "golden coordinates" provided.
   */
  private void renderSignalBody(TileEntityTrafficSignalHead te, TrafficSignalBodyColor bodyColor,
      int index) {
    // Render the body using the vertex data
    float yOffset = index * 12.0f;
    RenderHelper.drawBoxes(TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA, bodyColor.getRed(), bodyColor.getGreen(),
        bodyColor.getBlue(), 1.0f,0.0f,yOffset,0.0f);
  }

  /**
   * Renders the front door of the traffic signal. This sits on the front face of the signal body.
   */
  private void renderFrontDoor(TileEntityTrafficSignalHead te, TrafficSignalBodyColor doorColor,
      int index) {
    // Render the door using the vertex data
    float yOffset = index * 12.0f;
    RenderHelper.drawBoxes(TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA, doorColor.getRed(), doorColor.getGreen(),
        doorColor.getBlue(), 1.0f,0.0f,yOffset,0.0f);
  }


  private void renderSignalBulb(TileEntityTrafficSignalHead te, TrafficSignalBulbStyle bulbStyle,
      TrafficSignalBulbType bulbType, TrafficSignalBulbColor bulbColor, boolean isBulbLit, int index) {
    TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(bulbStyle, bulbType, bulbColor, isBulbLit);
    ResourceLocation texLoc = new ResourceLocation("csm", texInfo.getTexture());

    // Push OpenGL transformation matrix.
    GL11.glPushMatrix();

    // Bulb quad parameters: cover the entire 12x12 front face of the section
    float x = 2f;
    float y = (index * 12f);
    float z = 10.4f; // just in front of the door
    float size = 12f;

    // Center of the quad for rotation
    float cx = x + size / 2f;
    float cy = y + size / 2f;

    // Translate to center, rotate, then translate back
    GL11.glTranslatef(cx, cy, z);
    GL11.glRotatef(texInfo.getRotation(), 0, 0, 1);
    GL11.glTranslatef(-cx, -cy, -z);

    // Always bind the correct texture before rendering
    Minecraft.getMinecraft().getTextureManager().bindTexture(texLoc);

    // Use correct UVs from TextureInfo
    float u1 = texInfo.getU1();
    float v1 = texInfo.getV1();
    float u2 = texInfo.getU2();
    float v2 = texInfo.getV2();

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

    buffer.pos(x,      y,      z).tex(u1, v1).endVertex();
    buffer.pos(x,      y+size, z).tex(u1, v2).endVertex();
    buffer.pos(x+size, y+size, z).tex(u2, v2).endVertex();
    buffer.pos(x+size, y,      z).tex(u2, v1).endVertex();

    tessellator.draw();

    // Pop OpenGL transformation matrix.
    GL11.glPopMatrix();
  }

  /**
   * Renders the visor of the traffic signal using the baked model and applies color tinting.
   */
  private void renderSignalVisor(TileEntityTrafficSignalHead te, TrafficSignalBodyColor visorColor,
      TrafficSignalVisorType visorType, int index) {

    float colorAlpha = 1.0f; // Full opacity
    float xOffset = 0.0f; // No X offset
    float yOffset = (index * 12.0f);
    float zOffset = 0.0f; // No Z offset

    if (visorType == TrafficSignalVisorType.CIRCLE) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.TUNNEL) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.CUTAWAY) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.CAP_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.VERTICAL_LOUVERED) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    }else if (visorType == TrafficSignalVisorType.HORIZONTAL_LOUVERED) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    }else if (visorType == TrafficSignalVisorType.BOTH_LOUVERED) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.BOTH_LOUVERED_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.NONE) {
      RenderHelper.drawBoxes(TrafficSignalVertexData.NONE_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    }
  }


}