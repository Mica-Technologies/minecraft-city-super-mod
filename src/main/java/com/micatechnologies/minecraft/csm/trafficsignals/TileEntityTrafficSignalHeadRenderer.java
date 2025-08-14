package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
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

  // Add per-instance display list for caching static geometry
  private int staticDisplayList = -1;

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
    TrafficSignalBodyTilt bodyTilt = te.getBodyTilt();
    DirectionSixteen bodyDirection =
        AbstractBlockControllableSignalHead.getTiltedFacing(bodyTilt, facing);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend(); // Added for smoother edges/lights
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    // Push matrix once
    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);
    GL11.glScaled(0.0625, 0.0625, 0.0625);
    GL11.glTranslated(8, 8, 8);

    // Apply rotation once
    float rotationAngle = bodyDirection.getRotation();
    GL11.glRotatef(rotationAngle, 0, 1, 0);
    GL11.glTranslated(-8, -8, -8);

    // --- Compensation for tilt: shift slightly left/right for visual alignment ---
    // 1 model unit = 1/16 block, so shift by ±2 for tilt, ±4 for angle
    int tiltOffset = 0;
    if (bodyTilt
        == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.RIGHT_ANGLE) {
      tiltOffset = -4;
    } else if (bodyTilt
        == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.RIGHT_TILT) {
      tiltOffset = -2;
    } else if (bodyTilt
        == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.LEFT_TILT) {
      tiltOffset = 2;
    } else if (bodyTilt
        == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.LEFT_ANGLE) {
      tiltOffset = 4;
    }

    // Reverse the offset for SOUTH facing
    if (facing == net.minecraft.util.EnumFacing.SOUTH) {
      tiltOffset = -tiltOffset;
    }

    if (tiltOffset != 0) {
      GL11.glTranslated(tiltOffset, 0, 0);
    }

    if (staticDisplayList == -1 || te.isStateDirty()) {
      if (staticDisplayList != -1) GL11.glDeleteLists(staticDisplayList, 1);
      staticDisplayList = GL11.glGenLists(1);
      GL11.glNewList(staticDisplayList, GL11.GL_COMPILE);
      renderStaticParts(te, sectionInfos);
      GL11.glEndList();
      te.clearDirtyFlag();
    }
    GL11.glCallList(staticDisplayList);

    renderBulbs(te, sectionInfos);

    GL11.glPopMatrix();
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  // New: Render all static colored geometry (batched)
  private void renderStaticParts(TileEntityTrafficSignalHead te,
      TrafficSignalSectionInfo[] sectionInfos) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    GlStateManager.disableTexture2D();

    // Batch all quads first
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];
      TrafficSignalBodyColor bodyColor = sectionInfo.getBodyColor();
      TrafficSignalBodyColor doorColor = sectionInfo.getDoorColor();
      TrafficSignalBodyColor visorColor = sectionInfo.getVisorColor();
      TrafficSignalVisorType visorType = sectionInfo.getVisorType();

      float yOffset = ((sectionInfos.length - 1 - i) - (sectionInfos.length - 1) / 2.0f) * 12.0f;
      // For 3 sections: i=0 -> 12, i=1 -> 0, i=2 -> -12

      RenderHelper.addBoxesToBuffer(TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA, buffer,
          bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 1.0f, 0.0f, yOffset, 0.0f);
      RenderHelper.addBoxesToBuffer(TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA, buffer,
          doorColor.getRed(), doorColor.getGreen(), doorColor.getBlue(), 1.0f, 0.0f, yOffset, 0.0f);

      if (visorType != TrafficSignalVisorType.CIRCLE) {
        addVisorQuadsToBuffer(visorType, buffer, visorColor.getRed(), visorColor.getGreen(),
            visorColor.getBlue(), 1.0f, yOffset);
      }
    }
    tessellator.draw();

    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];
      TrafficSignalVisorType visorType = sectionInfo.getVisorType();
      TrafficSignalBodyColor visorColor = sectionInfo.getVisorColor();

      float yOffset = ((sectionInfos.length - 1 - i) - (sectionInfos.length - 1) / 2.0f) * 12.0f;
      // For 3 sections: i=0 -> 12, i=1 -> 0, i=2 -> -12

      if (visorType == TrafficSignalVisorType.CIRCLE) {
        BufferBuilder triBuffer = tessellator.getBuffer();
        triBuffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        float centerX = 8.0f;
        float centerY = 6.0f + yOffset;
        float centerZ = 10.0f;
        float radius = 6.0f;
        float depth = 2.0f;
        List<float[]> perimeter =
            TrafficSignalVertexData.getOptimizedCircleVisorPerimeter(centerX, centerY, centerZ,
                radius, depth, 16); // 16 segments for smoothness
        RenderHelper.addTriangleFanToBuffer(triBuffer, centerX, centerY, centerZ, perimeter,
            visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(), 1.0f);

        tessellator.draw();
      }
    }

    GlStateManager.enableTexture2D();
  }

  private void addVisorQuadsToBuffer(TrafficSignalVisorType visorType, BufferBuilder buffer,
      float red, float green, float blue, float alpha, float yOffset) {
    List<RenderHelper.Box> visorData;
    switch (visorType) {
      case TUNNEL:
        visorData = TrafficSignalVertexData.TUNNEL_VISOR_VERTEX_DATA;
        break;
      case CUTAWAY:
        visorData = TrafficSignalVertexData.CAP_VISOR_VERTEX_DATA;
        break;
      case BOTH_LOUVERED:
        visorData = TrafficSignalVertexData.BOTH_LOUVERED_VISOR_VERTEX_DATA;
        break;
      case VERTICAL_LOUVERED:
        visorData = TrafficSignalVertexData.VERTICAL_LOUVERED_VISOR_VERTEX_DATA;
        break;
      case HORIZONTAL_LOUVERED:
        visorData = TrafficSignalVertexData.HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA;
        break;
      case NONE:
        visorData = TrafficSignalVertexData.NONE_VISOR_VERTEX_DATA;
        break;
      default:
        return;
    }
    RenderHelper.addBoxesToBuffer(visorData, buffer, red, green, blue, alpha, 0.0f, yOffset, 0.0f);
  }

  private void renderBulbs(TileEntityTrafficSignalHead te, TrafficSignalSectionInfo[] sectionInfos) {
    // Always bind the correct texture before rendering
    ResourceLocation texLoc = new ResourceLocation("csm", "textures/blocks/trafficsignals/lights/atlas.png");
    Minecraft.getMinecraft().getTextureManager().bindTexture(texLoc);

    Tessellator tessellator = Tessellator.getInstance();

    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];
      TrafficSignalBulbStyle bulbStyle = sectionInfo.getBulbStyle();
      TrafficSignalBulbType bulbType = sectionInfo.getBulbType();
      TrafficSignalBulbColor bulbColor = sectionInfo.getBulbCustomColor();
      boolean isBulbLit = sectionInfo.isBulbLit();
      TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(bulbStyle, bulbType, bulbColor, isBulbLit);

      // Push OpenGL transformation matrix
      GL11.glPushMatrix();

      // Bulb quad parameters: cover the entire 12x12 front face of the section
      float x = 2f;
      float y = ((sectionInfos.length - 1 - i) - (sectionInfos.length - 1) / 2.0f) * 12.0f;
      // For 3 sections: i=0 -> 12, i=1 -> 0, i=2 -> -12
      float z = 10.4f; // Just in front of the door
      float size = 12f;

      // Center of the quad for rotation
      float cx = x + size / 2f;
      float cy = y + size / 2f;

      // Translate to center, rotate, then translate back
      GL11.glTranslatef(cx, cy, z);
      GL11.glRotatef(texInfo.getRotation(), 0.0f, 0.0f, 1.0f); // Rotate around Z-axis
      GL11.glTranslatef(-cx, -cy, -z);

      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

      // Use correct UVs from TextureInfo (with u1/u2 swap for horizontal mirroring)
      float u1 = texInfo.getU1();
      float v1 = texInfo.getV1();
      float u2 = texInfo.getU2();
      float v2 = texInfo.getV2();

      // Vertex order: bottom-left, bottom-right, top-right, top-left (counterclockwise)
      buffer.pos(x, y, z).tex(u2, v1).endVertex(); // bottom-left
      buffer.pos(x + size, y, z).tex(u1, v1).endVertex(); // bottom-right
      buffer.pos(x + size, y + size, z).tex(u1, v2).endVertex(); // top-right
      buffer.pos(x, y + size, z).tex(u2, v2).endVertex(); // top-left

      tessellator.draw();

      GL11.glPopMatrix();
    }
  }


}