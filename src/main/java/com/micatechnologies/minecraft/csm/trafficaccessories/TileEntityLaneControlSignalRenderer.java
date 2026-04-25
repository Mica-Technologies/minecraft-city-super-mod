package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public class TileEntityLaneControlSignalRenderer
        extends TileEntitySpecialRenderer<TileEntityLaneControlSignal> {

    private final Map<BlockPos, Integer> displayListCache = new HashMap<>();

    private static final float VISOR_TINT_SCALE = 1.04f;
    private static final float VISOR_TINT_BASE = 0.01f;
    private static final float VISOR_INNER_R = 0.0f;
    private static final float VISOR_INNER_G = 0.0f;
    private static final float VISOR_INNER_B = 0.0f;

    public void cleanupDisplayList(BlockPos pos) {
        Integer listId = displayListCache.remove(pos);
        if (listId != null) {
            GL11.glDeleteLists(listId, 1);
        }
    }

    @Override
    public void render(TileEntityLaneControlSignal te, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha) {

        if (te.getWorld() == null) return;

        IBlockState blockState = te.getWorld().getBlockState(te.getPos());
        if (!(blockState.getBlock() instanceof BlockLaneControlSignal)) {
            return;
        }

        EnumFacing facing = blockState.getValue(BlockHorizontal.FACING);

        TrafficSignalBodyColor bodyColor = te.getBodyColor();
        TrafficSignalBodyColor visorColor = te.getVisorColor();
        BlankoutBoxVisorType visorType = te.getVisorType();
        CrosswalkMountType mountType = te.getMountType();
        TrafficSignalBodyTilt bodyTilt = te.getBodyTilt();
        LaneControlSignalType signalType = te.getSignalType();

        DirectionSixteen bodyDirection =
                AbstractBlockControllableSignalHead.getTiltedFacing(bodyTilt, facing);
        DirectionSixteen baseDirection =
                AbstractBlockControllableSignalHead.getTiltedFacing(
                        TrafficSignalBodyTilt.NONE, facing);

        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int prevBX = (int) OpenGlHelper.lastBrightnessX;
        int prevBY = (int) OpenGlHelper.lastBrightnessY;

        int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
        int worldLightX = combinedLight % 65536;
        int worldLightY = combinedLight / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, worldLightX,
                worldLightY);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glScaled(0.0625, 0.0625, 0.0625);

        int tiltOffset = 0;
        if (bodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) tiltOffset = -4;
        else if (bodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) tiltOffset = -2;
        else if (bodyTilt == TrafficSignalBodyTilt.LEFT_TILT) tiltOffset = 2;
        else if (bodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) tiltOffset = 4;

        if (mountType != CrosswalkMountType.BASE) {
            GL11.glPushMatrix();
            GL11.glTranslated(8, 8, 8);
            GL11.glRotatef(baseDirection.getRotation(), 0, 1, 0);
            GL11.glTranslated(-8, -8, -8);

            renderBoxes(bodyColor, BlankoutBoxVertexData.getArmData(
                    mountType, tiltOffset,
                    bodyDirection.getRotation(), baseDirection.getRotation()));

            GL11.glPopMatrix();
        }

        GL11.glTranslated(8, 8, 8);
        GL11.glRotatef(bodyDirection.getRotation(), 0, 1, 0);
        GL11.glTranslated(-8, -8, -8);
        if (tiltOffset != 0) {
            GL11.glTranslated(tiltOffset, 0, 0);
        }

        if (mountType == CrosswalkMountType.BASE) {
            BlockPos behind = te.getPos().offset(facing.getOpposite());
            if (te.getWorld().getBlockState(behind).getBlock()
                    instanceof BlockTrafficLightMountKit) {
                GL11.glTranslated(0, 0, 8);
            }
        }

        if (mountType != CrosswalkMountType.BASE) {
            renderBoxes(bodyColor, BlankoutBoxVertexData.getStubData(mountType));
        }

        BlockPos pos = te.getPos();
        Integer displayList = displayListCache.get(pos);
        if (displayList == null || te.isStateDirty()) {
            if (displayList != null) {
                GL11.glDeleteLists(displayList, 1);
            }
            displayList = GL11.glGenLists(1);
            displayListCache.put(pos, displayList);
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            renderStaticParts(bodyColor, visorColor, visorType);
            GL11.glEndList();
            te.clearDirtyFlag();
        }
        GL11.glCallList(displayList);

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

        renderDisplayFace(signalType);

        GL11.glPopMatrix();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.resetColor();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
    }

    private void renderStaticParts(TrafficSignalBodyColor bodyColor,
            TrafficSignalBodyColor visorColor, BlankoutBoxVisorType visorType) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float br = bodyColor.getRed(), bg = bodyColor.getGreen(), bb = bodyColor.getBlue();
        RenderHelper.addBoxesToBuffer(BlankoutBoxVertexData.BODY_VERTEX_DATA,
                buffer, br, bg, bb, 1.0f, 0, 0, 0);

        float vr = Math.min(1.0f, visorColor.getRed() * VISOR_TINT_SCALE + VISOR_TINT_BASE);
        float vg = Math.min(1.0f, visorColor.getGreen() * VISOR_TINT_SCALE + VISOR_TINT_BASE);
        float vb = Math.min(1.0f, visorColor.getBlue() * VISOR_TINT_SCALE + VISOR_TINT_BASE);

        List<RenderHelper.Box> visorData = getVisorData(visorType);
        boolean isHood = visorType == BlankoutBoxVisorType.HOOD
                || visorType == BlankoutBoxVisorType.DEEP_HOOD;
        if (isHood) {
            RenderHelper.addBoxesToBufferDualColor(visorData, buffer,
                    vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                    1.0f, 0, 0, 0,
                    BlankoutBoxVertexData.VISOR_CENTER_X, BlankoutBoxVertexData.VISOR_CENTER_Y);
        } else if (!visorData.isEmpty()) {
            RenderHelper.addBoxesToBuffer(visorData, buffer, vr, vg, vb, 1.0f, 0, 0, 0);
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private void renderBoxes(TrafficSignalBodyColor color, List<RenderHelper.Box> boxes) {
        if (boxes.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        RenderHelper.addBoxesToBuffer(boxes, buffer,
                color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0);
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private List<RenderHelper.Box> getVisorData(BlankoutBoxVisorType visorType) {
        switch (visorType) {
            case HOOD:
                return BlankoutBoxVertexData.VISOR_HOOD_VERTEX_DATA;
            case DEEP_HOOD:
                return BlankoutBoxVertexData.VISOR_DEEP_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return BlankoutBoxVertexData.VISOR_NONE_VERTEX_DATA;
        }
    }

    private void renderDisplayFace(LaneControlSignalType signalType) {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        float[] uv = LaneControlSignalTextureMap.getAtlasUV(signalType);
        float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

        Minecraft.getMinecraft().getTextureManager().bindTexture(
                LaneControlSignalTextureMap.ATLAS_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float x1 = BlankoutBoxVertexData.DISPLAY_X1;
        float y1 = BlankoutBoxVertexData.DISPLAY_Y1;
        float x2 = BlankoutBoxVertexData.DISPLAY_X2;
        float y2 = BlankoutBoxVertexData.DISPLAY_Y2;
        float faceZ = BlankoutBoxVertexData.DISPLAY_FACE_Z;

        buffer.pos(x2, y1, faceZ).tex(u1, v2).endVertex();
        buffer.pos(x1, y1, faceZ).tex(u2, v2).endVertex();
        buffer.pos(x1, y2, faceZ).tex(u2, v1).endVertex();
        buffer.pos(x2, y2, faceZ).tex(u1, v1).endVertex();

        tessellator.draw();
    }
}
