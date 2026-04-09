package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkDisplayType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkSignalVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * TESR for the new custom-rendered crosswalk signals. Renders body, visor, and mount bracket as
 * cached display lists (static geometry), and display face textures + countdown overlay
 * dynamically per frame.
 */
public class TileEntityCrosswalkSignalNewRenderer
        extends TileEntitySpecialRenderer<TileEntityCrosswalkSignalNew> {

    private final Map<BlockPos, Integer> displayListCache = new HashMap<>();

    // Countdown display constants (from TileEntityCrosswalkSignalRenderer)
    private static final float Z_EPSILON = 0.005f;
    private static final float[][] SEGMENTS = {
            { 0.5f, 0.0f, 4.5f, 1.0f },   // a: top
            { 3.8f, 0.5f, 5.0f, 4.2f },   // b: upper right
            { 3.8f, 4.8f, 5.0f, 8.5f },   // c: lower right
            { 0.5f, 8.0f, 4.5f, 9.0f },   // d: bottom
            { 0.0f, 4.8f, 1.2f, 8.5f },   // e: lower left
            { 0.0f, 0.5f, 1.2f, 4.2f },   // f: upper left
            { 0.5f, 4.0f, 4.5f, 5.0f },   // g: middle
    };
    private static final boolean[][] DIGIT_SEGMENTS = {
            { true, true, true, true, true, true, false },    // 0
            { false, true, true, false, false, false, false }, // 1
            { true, true, false, true, true, false, true },    // 2
            { true, true, true, true, false, false, true },    // 3
            { false, true, true, false, false, true, true },   // 4
            { true, false, true, true, false, true, true },    // 5
            { true, false, true, true, true, true, true },     // 6
            { true, true, true, false, false, false, false },  // 7
            { true, true, true, true, true, true, true },      // 8
            { true, true, true, true, false, true, true },     // 9
    };
    private static final float DIGIT_HEIGHT = 0.6f;
    private static final float DIGIT_WIDTH = 0.22f;
    private static final float DIGIT_GAP = 0.04f;
    private static final float AREA_CENTER_X = 0.24f;
    private static final int CD_COLOR_R = 255, CD_COLOR_G = 136, CD_COLOR_B = 0, CD_COLOR_A = 255;

    public void cleanupDisplayList( BlockPos pos ) {
        Integer listId = displayListCache.remove( pos );
        if ( listId != null ) {
            GL11.glDeleteLists( listId, 1 );
        }
    }

    @Override
    public void render( TileEntityCrosswalkSignalNew te, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha ) {

        if ( te.getWorld() == null ) return;

        // Gather block state
        IBlockState blockState = te.getWorld().getBlockState( te.getPos() );
        if ( !( blockState.getBlock() instanceof AbstractBlockControllableCrosswalkSignalNew ) ) {
            return;
        }
        AbstractBlockControllableCrosswalkSignalNew block =
                (AbstractBlockControllableCrosswalkSignalNew) blockState.getBlock();

        EnumFacing facing = blockState.getValue(
                AbstractBlockControllableCrosswalkSignalNew.FACING );
        int colorState = blockState.getValue(
                AbstractBlockControllableCrosswalkSignalNew.COLOR );
        CrosswalkDisplayType displayType = block.getDisplayType();

        // Gather TE properties
        TrafficSignalBodyColor bodyColor = te.getBodyColor();
        TrafficSignalBodyColor visorColor = te.getVisorColor();
        CrosswalkVisorType visorType = te.getVisorType();
        CrosswalkMountType mountType = te.getMountType();
        TrafficSignalBodyTilt bodyTilt = te.getBodyTilt();
        CrosswalkBulbType bulbType = te.getBulbType();

        // Compute facing directions
        DirectionSixteen bodyDirection =
                AbstractBlockControllableSignalHead.getTiltedFacing( bodyTilt, facing );
        DirectionSixteen baseDirection =
                AbstractBlockControllableSignalHead.getTiltedFacing(
                        TrafficSignalBodyTilt.NONE, facing );

        // GL state setup
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );

        // Fullbright
        int prevBX = (int) OpenGlHelper.lastBrightnessX;
        int prevBY = (int) OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, 240f, 240f );

        // Common base transform (block position + model unit scale)
        GL11.glPushMatrix();
        GL11.glTranslated( x, y, z );
        GL11.glScaled( 0.0625, 0.0625, 0.0625 );

        // Compute tilt offset for compensation
        int tiltOffset = 0;
        if ( bodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE ) tiltOffset = -4;
        else if ( bodyTilt == TrafficSignalBodyTilt.RIGHT_TILT ) tiltOffset = -2;
        else if ( bodyTilt == TrafficSignalBodyTilt.LEFT_TILT ) tiltOffset = 2;
        else if ( bodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE ) tiltOffset = 4;

        // =============================================
        // Horizontal arms: rendered with BASE facing only (no tilt) so pole-side
        // mount point stays stationary. Arms angle to meet the tilted stubs.
        // =============================================
        if ( mountType != CrosswalkMountType.BASE ) {
            GL11.glPushMatrix();
            GL11.glTranslated( 8, 8, 8 );
            GL11.glRotatef( baseDirection.getRotation(), 0, 1, 0 );
            GL11.glTranslated( -8, -8, -8 );

            renderBoxes( bodyColor, CrosswalkSignalVertexData.getArmData(
                    mountType, displayType, tiltOffset,
                    bodyDirection.getRotation(), baseDirection.getRotation() ) );

            GL11.glPopMatrix();
        }

        // =============================================
        // Body + visor + stubs: rendered with TILTED facing so stubs line up
        // perfectly with the housing at any tilt angle.
        // =============================================
        GL11.glTranslated( 8, 8, 8 );
        float rotationAngle = bodyDirection.getRotation();
        GL11.glRotatef( rotationAngle, 0, 1, 0 );
        GL11.glTranslated( -8, -8, -8 );
        if ( tiltOffset != 0 ) {
            GL11.glTranslated( tiltOffset, 0, 0 );
        }

        // Render stubs in the tilted context (before display list)
        if ( mountType != CrosswalkMountType.BASE ) {
            renderBoxes( bodyColor, CrosswalkSignalVertexData.getStubData(
                    mountType, displayType ) );
        }

        // Display list: body + visor only (no bracket)
        BlockPos pos = te.getPos();
        Integer displayList = displayListCache.get( pos );
        if ( displayList == null || te.isStateDirty() ) {
            if ( displayList != null ) {
                GL11.glDeleteLists( displayList, 1 );
            }
            displayList = GL11.glGenLists( 1 );
            displayListCache.put( pos, displayList );
            GL11.glNewList( displayList, GL11.GL_COMPILE );
            renderStaticParts( bodyColor, visorColor, visorType, displayType );
            GL11.glEndList();
            te.clearDirtyFlag();
        }
        GL11.glCallList( displayList );

        // Display face textures
        renderDisplayFace( displayType, bulbType, colorState );

        // 7-segment countdown area:
        // - Single 16-inch: always renders on the signal face
        // - Double 12-inch HAND_MAN_COUNTDOWN: renders on the lower section
        if ( displayType == CrosswalkDisplayType.SYMBOL ) {
            renderCountdown( te, colorState, false );
        }
        else if ( bulbType == CrosswalkBulbType.HAND_MAN_COUNTDOWN ) {
            renderCountdown( te, colorState, true );
        }

        GL11.glPopMatrix();

        // Reset GL state
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
        GlStateManager.resetColor();
        OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, prevBX, prevBY );
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
    }

    // =====================================================================================
    // Static parts: body, visor, bracket (compiled into display list)
    // =====================================================================================

    // Visor interior is always flat black, matching the vehicle signal visor convention
    private static final float VISOR_INNER_R = TrafficSignalBodyColor.FLAT_BLACK.getRed();
    private static final float VISOR_INNER_G = TrafficSignalBodyColor.FLAT_BLACK.getGreen();
    private static final float VISOR_INNER_B = TrafficSignalBodyColor.FLAT_BLACK.getBlue();

    // Slight visor tint offset — just enough to visually distinguish the visor from the body
    // when both are configured to the same color.
    private static final float VISOR_TINT_OFFSET = 0.025f;

    // Visor center points for inside/outside face determination
    private static final float SINGLE_VISOR_CENTER_X = 8.0f;
    private static final float SINGLE_VISOR_CENTER_Y = 8.0f;
    private static final float SINGLE_12INCH_VISOR_CENTER_X = 8.0f;
    private static final float SINGLE_12INCH_VISOR_CENTER_Y = 6.0f;
    private static final float DOUBLE_UPPER_VISOR_CENTER_X = 8.0f;
    private static final float DOUBLE_UPPER_VISOR_CENTER_Y = 18.0f;
    private static final float DOUBLE_LOWER_VISOR_CENTER_X = 8.0f;
    private static final float DOUBLE_LOWER_VISOR_CENTER_Y = 6.0f;

    /**
     * Renders the body and visor geometry (no bracket). Compiled into a display list.
     * Hood visors use dual-color rendering: configured visor color on the outside,
     * flat black on the inside.
     */
    private void renderStaticParts( TrafficSignalBodyColor bodyColor,
            TrafficSignalBodyColor visorColor, CrosswalkVisorType visorType,
            CrosswalkDisplayType displayType ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );

        float br = bodyColor.getRed(), bg = bodyColor.getGreen(), bb = bodyColor.getBlue();
        float vr = Math.min( 1.0f, visorColor.getRed() + VISOR_TINT_OFFSET );
        float vg = Math.min( 1.0f, visorColor.getGreen() + VISOR_TINT_OFFSET );
        float vb = Math.min( 1.0f, visorColor.getBlue() + VISOR_TINT_OFFSET );
        boolean isHood = visorType == CrosswalkVisorType.HOOD
                || visorType == CrosswalkVisorType.DEEP_HOOD;

        if ( displayType == CrosswalkDisplayType.SYMBOL ) {
            RenderHelper.addBoxesToBuffer( CrosswalkSignalVertexData.SINGLE_BODY_VERTEX_DATA,
                    buffer, br, bg, bb, 1.0f, 0, 0, 0 );
            List<RenderHelper.Box> visorData = getSingleVisorData( visorType );
            if ( isHood ) {
                RenderHelper.addBoxesToBufferDualColor( visorData, buffer,
                        vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                        1.0f, 0, 0, 0, SINGLE_VISOR_CENTER_X, SINGLE_VISOR_CENTER_Y );
            }
            else {
                RenderHelper.addBoxesToBuffer( visorData, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
            }
        }
        else if ( displayType == CrosswalkDisplayType.SYMBOL_12INCH ) {
            RenderHelper.addBoxesToBuffer(
                    CrosswalkSignalVertexData.SINGLE_12INCH_BODY_VERTEX_DATA,
                    buffer, br, bg, bb, 1.0f, 0, 0, 0 );
            List<RenderHelper.Box> visorData = getSingle12InchVisorData( visorType );
            if ( isHood ) {
                RenderHelper.addBoxesToBufferDualColor( visorData, buffer,
                        vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                        1.0f, 0, 0, 0,
                        SINGLE_12INCH_VISOR_CENTER_X, SINGLE_12INCH_VISOR_CENTER_Y );
            }
            else {
                RenderHelper.addBoxesToBuffer( visorData, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
            }
        }
        else {
            RenderHelper.addBoxesToBuffer(
                    CrosswalkSignalVertexData.DOUBLE_UPPER_BODY_VERTEX_DATA,
                    buffer, br, bg, bb, 1.0f, 0, 0, 0 );
            RenderHelper.addBoxesToBuffer(
                    CrosswalkSignalVertexData.DOUBLE_LOWER_BODY_VERTEX_DATA,
                    buffer, br, bg, bb, 1.0f, 0, 0, 0 );
            List<RenderHelper.Box> upperVisor = getDoubleUpperVisorData( visorType );
            List<RenderHelper.Box> lowerVisor = getDoubleLowerVisorData( visorType );
            if ( isHood ) {
                RenderHelper.addBoxesToBufferDualColor( upperVisor, buffer,
                        vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                        1.0f, 0, 0, 0,
                        DOUBLE_UPPER_VISOR_CENTER_X, DOUBLE_UPPER_VISOR_CENTER_Y );
                RenderHelper.addBoxesToBufferDualColor( lowerVisor, buffer,
                        vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                        1.0f, 0, 0, 0,
                        DOUBLE_LOWER_VISOR_CENTER_X, DOUBLE_LOWER_VISOR_CENTER_Y );
            }
            else {
                RenderHelper.addBoxesToBuffer( upperVisor, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
                RenderHelper.addBoxesToBuffer( lowerVisor, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
            }
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    /**
     * Renders a list of colored boxes. Used for bracket stubs and arms which are rendered
     * in different GL matrix contexts.
     */
    private void renderBoxes( TrafficSignalBodyColor color, List<RenderHelper.Box> boxes ) {
        if ( boxes.isEmpty() ) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
        RenderHelper.addBoxesToBuffer( boxes, buffer,
                color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0 );
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private List<RenderHelper.Box> getSingleVisorData( CrosswalkVisorType visorType ) {
        switch ( visorType ) {
            case CRATE:
                return CrosswalkSignalVertexData.SINGLE_VISOR_CRATE_VERTEX_DATA;
            case HOOD:
                return CrosswalkSignalVertexData.SINGLE_VISOR_HOOD_VERTEX_DATA;
            case DEEP_HOOD:
                return CrosswalkSignalVertexData.SINGLE_VISOR_DEEP_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return CrosswalkSignalVertexData.SINGLE_VISOR_NONE_VERTEX_DATA;
        }
    }

    private List<RenderHelper.Box> getSingle12InchVisorData( CrosswalkVisorType visorType ) {
        switch ( visorType ) {
            case CRATE:
                return CrosswalkSignalVertexData.SINGLE_12INCH_VISOR_CRATE_VERTEX_DATA;
            case HOOD:
                return CrosswalkSignalVertexData.SINGLE_12INCH_VISOR_HOOD_VERTEX_DATA;
            case DEEP_HOOD:
                return CrosswalkSignalVertexData.SINGLE_12INCH_VISOR_DEEP_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return CrosswalkSignalVertexData.SINGLE_12INCH_VISOR_NONE_VERTEX_DATA;
        }
    }

    private List<RenderHelper.Box> getDoubleUpperVisorData( CrosswalkVisorType visorType ) {
        switch ( visorType ) {
            case CRATE:
                return CrosswalkSignalVertexData.DOUBLE_UPPER_VISOR_CRATE_VERTEX_DATA;
            case HOOD:
                return CrosswalkSignalVertexData.DOUBLE_UPPER_VISOR_HOOD_VERTEX_DATA;
            case DEEP_HOOD:
                return CrosswalkSignalVertexData.DOUBLE_UPPER_VISOR_DEEP_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return CrosswalkSignalVertexData.DOUBLE_VISOR_NONE_VERTEX_DATA;
        }
    }

    private List<RenderHelper.Box> getDoubleLowerVisorData( CrosswalkVisorType visorType ) {
        switch ( visorType ) {
            case CRATE:
                return CrosswalkSignalVertexData.DOUBLE_LOWER_VISOR_CRATE_VERTEX_DATA;
            case HOOD:
                return CrosswalkSignalVertexData.DOUBLE_LOWER_VISOR_HOOD_VERTEX_DATA;
            case DEEP_HOOD:
                return CrosswalkSignalVertexData.DOUBLE_LOWER_VISOR_DEEP_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return CrosswalkSignalVertexData.DOUBLE_VISOR_NONE_VERTEX_DATA;
        }
    }

    // =====================================================================================
    // Dynamic: display face rendering
    // =====================================================================================

    private void renderDisplayFace( CrosswalkDisplayType displayType,
            CrosswalkBulbType bulbType, int colorState ) {
        // Reset GL color after display list (which may leave stale color)
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );

        // Flash timing: 1Hz on/off cycle during clearance (color=1)
        boolean flashOn = ( System.currentTimeMillis() % 1000 ) < 500;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        if ( displayType == CrosswalkDisplayType.SYMBOL ) {
            // Single 16-inch: atlas-based rendering
            int atlasIdx = CrosswalkTextureMap.getSingleFaceAtlasIndex( colorState, flashOn );
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                    CrosswalkTextureMap.ATLAS_TEXTURE );
            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addAtlasQuad( buffer, atlasIdx,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_Y1,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_Y2,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_FACE_Z );
            tessellator.draw();
        }
        else if ( displayType == CrosswalkDisplayType.SYMBOL_12INCH ) {
            // Single 12-inch: atlas-based rendering with 12-inch bimodal textures
            int atlasIdx = CrosswalkTextureMap.getSingle12InchFaceAtlasIndex(
                    colorState, flashOn );
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                    CrosswalkTextureMap.ATLAS_TEXTURE );
            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addAtlasQuad( buffer, atlasIdx,
                    CrosswalkSignalVertexData.SINGLE_12INCH_DISPLAY_X1,
                    CrosswalkSignalVertexData.SINGLE_12INCH_DISPLAY_Y1,
                    CrosswalkSignalVertexData.SINGLE_12INCH_DISPLAY_X2,
                    CrosswalkSignalVertexData.SINGLE_12INCH_DISPLAY_Y2,
                    CrosswalkSignalVertexData.SINGLE_12INCH_DISPLAY_FACE_Z );
            tessellator.draw();
        }
        else if ( bulbType == CrosswalkBulbType.HAND_MAN_COUNTDOWN ) {
            // Double 12-inch: atlas-based — upper = bimodal hand/man, lower = countdown base
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                    CrosswalkTextureMap.ATLAS_TEXTURE );

            int upperIdx = CrosswalkTextureMap.getHandManUpperAtlasIndex(
                    colorState, flashOn );
            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addAtlasQuad( buffer, upperIdx,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.DOUBLE_UPPER_DISPLAY_Y1,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.DOUBLE_UPPER_DISPLAY_Y2,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z );
            // Lower section in same draw call (same atlas texture bound)
            int lowerIdx = CrosswalkTextureMap.getHandManLowerAtlasIndex();
            addAtlasQuad( buffer, lowerIdx,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.DOUBLE_LOWER_DISPLAY_Y1,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.DOUBLE_LOWER_DISPLAY_Y2,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z );
            tessellator.draw();
        }
        else {
            // Double 12-inch WORDED: atlas-based
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                    CrosswalkTextureMap.ATLAS_TEXTURE );

            int upperIdx = CrosswalkTextureMap.getWordedUpperAtlasIndex(
                    colorState, flashOn );
            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addAtlasQuad( buffer, upperIdx,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.DOUBLE_UPPER_DISPLAY_Y1,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.DOUBLE_UPPER_DISPLAY_Y2,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z );
            int lowerIdx = CrosswalkTextureMap.getWordedLowerAtlasIndex(
                    colorState, flashOn );
            addAtlasQuad( buffer, lowerIdx,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.DOUBLE_LOWER_DISPLAY_Y1,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.DOUBLE_LOWER_DISPLAY_Y2,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z );
            tessellator.draw();
        }
    }

    /**
     * Adds a textured quad using atlas UV coordinates for the given tile index.
     */
    private void addAtlasQuad( BufferBuilder buffer, int atlasIndex,
            float x1, float y1, float x2, float y2, float z ) {
        float[] uv = CrosswalkTextureMap.getAtlasUV( atlasIndex );
        float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];
        // CCW winding, facing -Z (north). X is mirrored in model space.
        buffer.pos( x2, y1, z ).tex( u1, v2 ).endVertex();
        buffer.pos( x1, y1, z ).tex( u2, v2 ).endVertex();
        buffer.pos( x1, y2, z ).tex( u2, v1 ).endVertex();
        buffer.pos( x2, y2, z ).tex( u1, v1 ).endVertex();
    }


    // =====================================================================================
    // Dynamic: countdown overlay (7-segment display)
    // =====================================================================================

    // Dim "88" background color — mimics unlit 7-segment display segments
    private static final int BG_COLOR_R = 50, BG_COLOR_G = 50, BG_COLOR_B = 50;
    private static final int BG_COLOR_A = 255;

    /**
     * Renders the 7-segment countdown display (dim "88" background + lit digits).
     *
     * @param te           the tile entity
     * @param colorState   the current signal color state
     * @param isLowerSection true if rendering on the double signal's lower 12-inch section
     *                       (centered at Y=6, scaled to fit 12-unit section)
     */
    private void renderCountdown( TileEntityCrosswalkSignalNew te, int colorState,
            boolean isLowerSection ) {
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask( false );

        // Positioning: on single 16-inch face, digits are in the right half.
        // On double 12-inch lower section, digits are centered on the section.
        float scale;
        float centerModelX;
        float centerModelY;
        float faceZ;

        if ( isLowerSection ) {
            // Double 12-inch lower section: centered, scaled to fit 12-unit section
            scale = 12.0f;
            centerModelX = 8.0f;  // center of block
            centerModelY = 6.0f;  // center of lower section (Y=0-12)
            faceZ = CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z - Z_EPSILON;
        }
        else {
            // Single 16-inch: countdown in the right half
            scale = 16.0f;
            centerModelX = 8.0f;
            centerModelY = 8.0f;
            faceZ = CrosswalkSignalVertexData.SINGLE_DISPLAY_FACE_Z - Z_EPSILON;
        }

        // Two-digit layout
        float twoDigitWidth = 2 * DIGIT_WIDTH + DIGIT_GAP;
        float centerX = isLowerSection ? 0.0f : ( AREA_CENTER_X - 0.03f );
        float startX = centerX - twoDigitWidth / 2;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // --- Background layer: dim "88" always visible ---
        buf.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
        for ( int i = 0; i < 2; i++ ) {
            float dx = startX + i * ( DIGIT_WIDTH + DIGIT_GAP );
            float modelDx = centerModelX - ( dx + DIGIT_WIDTH ) * scale;
            float modelDy = centerModelY - ( DIGIT_HEIGHT / 2 ) * scale;
            float modelW = DIGIT_WIDTH * scale;
            float modelH = DIGIT_HEIGHT * scale;
            drawDigitSegmentsBg( buf, 8, modelDx, modelDy, modelW, modelH, faceZ );
        }
        tess.draw();

        // --- Foreground layer: lit countdown digits (only during clearance) ---
        int countdown = te.getCurrentCountdown();
        if ( countdown >= 0 && colorState == 1 ) {
            int displayValue = Math.min( countdown, 99 );
            int tens = displayValue / 10;
            int ones = displayValue % 10;

            buf.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
            for ( int i = 0; i < 2; i++ ) {
                int digit = ( i == 0 ) ? tens : ones;
                if ( i == 0 && tens == 0 ) continue;

                float dx = startX + i * ( DIGIT_WIDTH + DIGIT_GAP );
                float modelDx = centerModelX - ( dx + DIGIT_WIDTH ) * scale;
                float modelDy = centerModelY - ( DIGIT_HEIGHT / 2 ) * scale;
                float modelW = DIGIT_WIDTH * scale;
                float modelH = DIGIT_HEIGHT * scale;
                drawDigitSegments( buf, digit, modelDx, modelDy, modelW, modelH,
                        faceZ - Z_EPSILON );
            }
            tess.draw();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.depthMask( true );
    }

    /**
     * Draws digit segments in the dim background color (unlit segment ghost).
     */
    private static void drawDigitSegmentsBg( BufferBuilder buf, int digit, float dx, float dy,
            float w, float h, float z ) {
        boolean[] segs = DIGIT_SEGMENTS[digit];
        for ( int s = 0; s < 7; s++ ) {
            if ( !segs[s] ) continue;
            float[] seg = SEGMENTS[s];

            float x1 = dx + ( ( 5f - seg[2] ) / 5f ) * w;
            float x2 = dx + ( ( 5f - seg[0] ) / 5f ) * w;
            float y1 = dy + ( 1f - seg[3] / 9f ) * h;
            float y2 = dy + ( 1f - seg[1] / 9f ) * h;

            buf.pos( x1, y1, z ).color( BG_COLOR_R, BG_COLOR_G, BG_COLOR_B, BG_COLOR_A )
                    .endVertex();
            buf.pos( x2, y1, z ).color( BG_COLOR_R, BG_COLOR_G, BG_COLOR_B, BG_COLOR_A )
                    .endVertex();
            buf.pos( x2, y2, z ).color( BG_COLOR_R, BG_COLOR_G, BG_COLOR_B, BG_COLOR_A )
                    .endVertex();
            buf.pos( x1, y2, z ).color( BG_COLOR_R, BG_COLOR_G, BG_COLOR_B, BG_COLOR_A )
                    .endVertex();
        }
    }

    private static void drawDigitSegments( BufferBuilder buf, int digit, float dx, float dy,
            float w, float h, float z ) {
        boolean[] segs = DIGIT_SEGMENTS[digit];
        for ( int s = 0; s < 7; s++ ) {
            if ( !segs[s] ) continue;
            float[] seg = SEGMENTS[s];

            // Mirror X: model space is flipped relative to viewer, so invert segment X
            float x1 = dx + ( ( 5f - seg[2] ) / 5f ) * w;
            float x2 = dx + ( ( 5f - seg[0] ) / 5f ) * w;
            float y1 = dy + ( 1f - seg[3] / 9f ) * h;
            float y2 = dy + ( 1f - seg[1] / 9f ) * h;

            buf.pos( x1, y1, z ).color( CD_COLOR_R, CD_COLOR_G, CD_COLOR_B, CD_COLOR_A )
                    .endVertex();
            buf.pos( x2, y1, z ).color( CD_COLOR_R, CD_COLOR_G, CD_COLOR_B, CD_COLOR_A )
                    .endVertex();
            buf.pos( x2, y2, z ).color( CD_COLOR_R, CD_COLOR_G, CD_COLOR_B, CD_COLOR_A )
                    .endVertex();
            buf.pos( x1, y2, z ).color( CD_COLOR_R, CD_COLOR_G, CD_COLOR_B, CD_COLOR_A )
                    .endVertex();
        }
    }
}
