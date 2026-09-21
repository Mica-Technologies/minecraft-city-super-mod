package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public class TileEntityLaneControlSignalRenderer
        extends TileEntitySpecialRenderer<TileEntityLaneControlSignal> {

    /**
     * Cached body + visor geometry, keyed by position. Released precisely from the tile entity's
     * lifecycle callbacks and bounded as a backstop -- see {@link CsmDisplayListCache}.
     */
    private static final CsmDisplayListCache DISPLAY_LISTS =
            new CsmDisplayListCache("lane_control_signal");

    /**
     * The mounting arms and stubs (any mount but {@code BASE}). They depend on the mount, the
     * tilt, the facing, the body colour and the light, never on the position, so every signal
     * that looks the same replays one list -- see {@link #mountKey}. Before, they were emitted
     * and drawn immediate-mode every frame, two Tessellator draws per signal.
     */
    private static final CsmSharedDisplayLists MOUNT_LISTS =
            new CsmSharedDisplayLists("lane_control_mount");

    /**
     * The aspect's atlas UVs by {@link LaneControlSignalType} ordinal, resolved once rather than
     * allocated by {@link LaneControlSignalTextureMap#getAtlasUV} every frame. Read only; never
     * handed out.
     */
    private static final float[][] FACE_UVS = new float[LaneControlSignalType.values().length][];

    static {
        for (LaneControlSignalType type : LaneControlSignalType.values()) {
            FACE_UVS[type.ordinal()] = LaneControlSignalTextureMap.getAtlasUV(type);
        }
    }

    private static final float VISOR_TINT_SCALE = 1.04f;
    private static final float VISOR_TINT_BASE = 0.01f;
    private static final float VISOR_INNER_R = 0.0f;
    private static final float VISOR_INNER_G = 0.0f;
    private static final float VISOR_INNER_B = 0.0f;

    private static final ResourceLocation WHITE_TEXTURE =
        new ResourceLocation("csm", "textures/blocks/white1px.png");
    private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
    private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

    /**
     * Releases the cached display list for a position. Called from the block's
     * {@code breakBlock} and from the tile entity's {@code invalidate()} / {@code onChunkUnload()}.
     *
     * @param pos the block position
     */
    public static void cleanupDisplayList(BlockPos pos) {
        DISPLAY_LISTS.invalidate(pos);
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

        int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
        int worldSkyLight = (combinedLight >> 16) & 0xFFFF;
        int worldBlockLight = combinedLight & 0xFFFF;

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

            renderMountPart(false, bodyColor, mountType, bodyTilt, facing, tiltOffset,
                    bodyDirection, baseDirection, combinedLight);

            GL11.glPopMatrix();
        }

        GL11.glTranslated(8, 8, 8);
        GL11.glRotatef(bodyDirection.getRotation(), 0, 1, 0);
        GL11.glTranslated(-8, -8, -8);
        if (tiltOffset != 0) {
            GL11.glTranslated(tiltOffset, 0, 0);
        }

        // Push the body back onto a mount kit behind it. The tile entity caches what is behind it
        // rather than this reading the world per frame.
        if (mountType == CrosswalkMountType.BASE && te.isBehindMountKit(facing)) {
            GL11.glTranslated(0, 0, 8);
        }

        if (mountType != CrosswalkMountType.BASE) {
            renderMountPart(true, bodyColor, mountType, bodyTilt, facing, tiltOffset,
                    bodyDirection, baseDirection, combinedLight);
        }

        BlockPos pos = te.getPos();
        // The compiled geometry depends only on the block light level here; everything else that
        // can change it routes through the tile entity's explicit dirty flag.
        long stateKey = combinedLight;
        // A dirty flag retires every state compiled for this position, not just the one about to
        // be redrawn -- the list is keyed on the light level, so the states left behind would be
        // the ones for the other light levels this block has been rendered at, and the daylight
        // cycle brings them back around. See TileEntityTrafficSignalHeadRenderer.
        boolean stateDirty = te.isStateDirty();
        if (stateDirty) {
            DISPLAY_LISTS.invalidate(pos);
        }
        int displayList = stateDirty
                ? CsmDisplayListCache.NO_LIST
                : DISPLAY_LISTS.get(pos, stateKey);
        if (displayList == CsmDisplayListCache.NO_LIST) {
            displayList = DISPLAY_LISTS.allocate(pos, stateKey);
            if (displayList != CsmDisplayListCache.NO_LIST) {
                GL11.glNewList(displayList, GL11.GL_COMPILE);
                renderStaticParts(bodyColor, visorColor, visorType, worldSkyLight, worldBlockLight);
                GL11.glEndList();
                te.clearDirtyFlag();
            }
        }
        // See TileEntityTrafficSignalHeadRenderer for why this bind must be unconditional —
        // TextureManager no-ops bindTexture inside renderStaticParts() when WHITE_TEXTURE is
        // already current at GL_COMPILE, so the display list ends up without a recorded bind
        // and renders white-tinted at replay (independent of shaders).
        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
        if (displayList != CsmDisplayListCache.NO_LIST) {
            GL11.glCallList(displayList);
            // The body's vertex colours leave GL's colour where GlStateManager's cache cannot see
            // it; a direct draw resets the cache afterwards, so this keeps the two paths the same.
            GlStateManager.resetColor();
        } else {
            // The driver refused a list name; draw directly this frame rather than calling list 0,
            // which draws nothing and would blank the signal.
            renderStaticParts(bodyColor, visorColor, visorType, worldSkyLight, worldBlockLight);
        }

        renderDisplayFace(signalType);

        GL11.glPopMatrix();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.resetColor();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
    }

    private void renderStaticParts(TrafficSignalBodyColor bodyColor,
            TrafficSignalBodyColor visorColor, BlankoutBoxVisorType visorType,
            int skyLight, int blockLight) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        float br = bodyColor.getRed(), bg = bodyColor.getGreen(), bb = bodyColor.getBlue();
        RenderHelper.addBoxesToBufferLit(BlankoutBoxVertexData.BODY_VERTEX_DATA,
                buffer, br, bg, bb, 1.0f, 0, 0, 0, skyLight, blockLight);

        float vr = Math.min(1.0f, visorColor.getRed() * VISOR_TINT_SCALE + VISOR_TINT_BASE);
        float vg = Math.min(1.0f, visorColor.getGreen() * VISOR_TINT_SCALE + VISOR_TINT_BASE);
        float vb = Math.min(1.0f, visorColor.getBlue() * VISOR_TINT_SCALE + VISOR_TINT_BASE);

        List<RenderHelper.Box> visorData = getVisorData(visorType);
        boolean isHood = visorType == BlankoutBoxVisorType.HOOD
                || visorType == BlankoutBoxVisorType.DEEP_HOOD;
        if (isHood) {
            RenderHelper.addBoxesToBufferDualColorLit(visorData, buffer,
                    vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                    1.0f, 0, 0, 0,
                    BlankoutBoxVertexData.VISOR_CENTER_X, BlankoutBoxVertexData.VISOR_CENTER_Y,
                    skyLight, blockLight);
        } else if (!visorData.isEmpty()) {
            RenderHelper.addBoxesToBufferLit(visorData, buffer, vr, vg, vb, 1.0f, 0, 0, 0,
                    skyLight, blockLight);
        }

        tessellator.draw();
    }

    /**
     * Draws the arms ({@code stubs == false}, in the base-facing context) or the stubs (in the
     * tilted context) from a list shared by every lane control signal that looks the same,
     * compiling it the first time that look is seen. The caller has already set up the matrix; a
     * list does not capture it, so one list serves every position.
     *
     * <p>The white texture is bound here, outside the list, every frame -- see
     * "Display lists: one texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}.
     * {@link CsmRenderToggles#sharedBakesPerFrame} draws them per frame instead, as they were
     * drawn before they were baked.</p>
     */
    private void renderMountPart(boolean stubs, TrafficSignalBodyColor color,
            CrosswalkMountType mountType, TrafficSignalBodyTilt bodyTilt, EnumFacing facing,
            int tiltOffset, DirectionSixteen bodyDirection, DirectionSixteen baseDirection,
            int combinedLight) {
        int skyLight = (combinedLight >> 16) & 0xFFFF;
        int blockLight = combinedLight & 0xFFFF;
        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
        if (CsmRenderToggles.sharedBakesPerFrame) {
            drawMountPart(stubs, color, mountType, tiltOffset, bodyDirection, baseDirection,
                    skyLight, blockLight);
            return;
        }
        long key = mountKey(stubs, color, mountType, bodyTilt, facing, combinedLight);
        int list = MOUNT_LISTS.get(key);
        if (list == CsmDisplayListCache.NO_LIST) {
            list = MOUNT_LISTS.allocate(key);
            if (list != CsmDisplayListCache.NO_LIST) {
                GL11.glNewList(list, GL11.GL_COMPILE);
                drawMountPart(stubs, color, mountType, tiltOffset, bodyDirection, baseDirection,
                        skyLight, blockLight);
                GL11.glEndList();
            }
        }
        if (list != CsmDisplayListCache.NO_LIST) {
            GL11.glCallList(list);
            // The replay leaves GL's colour at the last vertex's without GlStateManager knowing;
            // a direct draw resets its cache afterwards, so this keeps the two paths the same.
            GlStateManager.resetColor();
        } else {
            // The driver refused a list name: draw directly rather than calling list 0.
            drawMountPart(stubs, color, mountType, tiltOffset, bodyDirection, baseDirection,
                    skyLight, blockLight);
        }
    }

    /**
     * Packs everything the arm or stub geometry depends on into a shared-list key. Never a
     * position: the lightmap is baked into the vertices, so the light is part of the key instead
     * (sky and block light are 0-15 each, so at most 256 lights per look).
     *
     * <pre>
     *  bits  0-31  combinedLight, as getCombinedLight returns it
     *  bits 32-39  body colour ordinal
     *  bits 40-43  mount type ordinal
     *  bits 48-51  body tilt ordinal   (arms only: they angle to meet the tilted stubs)
     *  bits 52-54  facing index        (arms only: the tilt is resolved against the facing)
     *  bit  60     1 = stubs, 0 = arms
     * </pre>
     */
    private static long mountKey(boolean stubs, TrafficSignalBodyColor color,
            CrosswalkMountType mountType, TrafficSignalBodyTilt bodyTilt, EnumFacing facing,
            int combinedLight) {
        long key = (combinedLight & 0xFFFFFFFFL)
                | ((long) (color.ordinal() & 0xFF) << 32)
                | ((long) (mountType.ordinal() & 0xF) << 40);
        if (stubs) {
            return key | (1L << 60);
        }
        return key
                | ((long) (bodyTilt.ordinal() & 0xF) << 48)
                | ((long) (facing.getIndex() & 0x7) << 52);
    }

    /** Emits and draws the arms or the stubs. Geometry only: the caller owns every GL state. */
    private static void drawMountPart(boolean stubs, TrafficSignalBodyColor color,
            CrosswalkMountType mountType, int tiltOffset, DirectionSixteen bodyDirection,
            DirectionSixteen baseDirection, int skyLight, int blockLight) {
        List<RenderHelper.Box> boxes = stubs
                ? BlankoutBoxVertexData.getStubData(mountType)
                : BlankoutBoxVertexData.getArmData(mountType, tiltOffset,
                        bodyDirection.getRotation(), baseDirection.getRotation());
        drawBoxes(color, boxes, skyLight, blockLight);
    }

    /** Draws a list of colored boxes against whatever texture is bound (the caller binds it). */
    private static void drawBoxes(TrafficSignalBodyColor color, List<RenderHelper.Box> boxes,
            int skyLight, int blockLight) {
        if (boxes.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        RenderHelper.addBoxesToBufferLit(boxes, buffer,
                color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0,
                skyLight, blockLight);
        tessellator.draw();
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
        float[] uv = FACE_UVS[signalType.ordinal()];
        float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

        Minecraft.getMinecraft().getTextureManager().bindTexture(
                LaneControlSignalTextureMap.ATLAS_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        // BLOCK format with fullbright lightmap so the lit signal face stays bright under shaders.
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        float x1 = BlankoutBoxVertexData.DISPLAY_X1;
        float y1 = BlankoutBoxVertexData.DISPLAY_Y1;
        float x2 = BlankoutBoxVertexData.DISPLAY_X2;
        float y2 = BlankoutBoxVertexData.DISPLAY_Y2;
        float faceZ = BlankoutBoxVertexData.DISPLAY_FACE_Z;

        displayVertex(buffer, x2, y1, faceZ, u1, v2);
        displayVertex(buffer, x1, y1, faceZ, u2, v2);
        displayVertex(buffer, x1, y2, faceZ, u2, v1);
        displayVertex(buffer, x2, y2, faceZ, u1, v1);

        tessellator.draw();
    }

    private static void displayVertex(BufferBuilder buf, float x, float y, float z,
            float u, float v) {
        buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v)
                .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
    }
}
