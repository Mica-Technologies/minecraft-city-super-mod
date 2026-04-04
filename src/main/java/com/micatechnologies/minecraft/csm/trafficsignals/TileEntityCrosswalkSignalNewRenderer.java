package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
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
        // Bracket: rendered with BASE facing only (no tilt) so pole-side mount stays
        // stationary. The housing-side of the stubs leans by tiltOffset to meet the
        // tilted body position.
        // =============================================
        if ( mountType != CrosswalkMountType.BASE ) {
            GL11.glPushMatrix();
            GL11.glTranslated( 8, 8, 8 );
            GL11.glRotatef( baseDirection.getRotation(), 0, 1, 0 );
            GL11.glTranslated( -8, -8, -8 );

            boolean isDouble = displayType == CrosswalkDisplayType.TEXT;
            renderBracket( bodyColor, mountType, isDouble, tiltOffset );

            GL11.glPopMatrix();
        }

        // =============================================
        // Body + visor: rendered with TILTED facing
        // =============================================
        GL11.glTranslated( 8, 8, 8 );
        float rotationAngle = bodyDirection.getRotation();
        GL11.glRotatef( rotationAngle, 0, 1, 0 );
        GL11.glTranslated( -8, -8, -8 );
        if ( tiltOffset != 0 ) {
            GL11.glTranslated( tiltOffset, 0, 0 );
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
        renderDisplayFace( displayType, colorState );

        // Countdown overlay (single-face only, during clearance)
        if ( displayType == CrosswalkDisplayType.SYMBOL ) {
            renderCountdown( te, colorState );
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

    /**
     * Renders the body and visor geometry (no bracket). Compiled into a display list.
     */
    private void renderStaticParts( TrafficSignalBodyColor bodyColor,
            TrafficSignalBodyColor visorColor, CrosswalkVisorType visorType,
            CrosswalkDisplayType displayType ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );

        float br = bodyColor.getRed(), bg = bodyColor.getGreen(), bb = bodyColor.getBlue();
        float vr = visorColor.getRed(), vg = visorColor.getGreen(), vb = visorColor.getBlue();

        if ( displayType == CrosswalkDisplayType.SYMBOL ) {
            RenderHelper.addBoxesToBuffer( CrosswalkSignalVertexData.SINGLE_BODY_VERTEX_DATA,
                    buffer, br, bg, bb, 1.0f, 0, 0, 0 );
            List<RenderHelper.Box> visorData = getSingleVisorData( visorType );
            RenderHelper.addBoxesToBuffer( visorData, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
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
            RenderHelper.addBoxesToBuffer( upperVisor, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
            RenderHelper.addBoxesToBuffer( lowerVisor, buffer, vr, vg, vb, 1.0f, 0, 0, 0 );
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    /**
     * Renders the mount bracket separately from the body. This is called in the BASE facing
     * rotation context (no tilt) so the pole-side mount stays stationary. The housing-side
     * stubs lean by tiltOffset to connect to the tilted body.
     */
    private void renderBracket( TrafficSignalBodyColor bodyColor, CrosswalkMountType mountType,
            boolean isDouble, int tiltOffset ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );

        float br = bodyColor.getRed(), bg = bodyColor.getGreen(), bb = bodyColor.getBlue();
        List<RenderHelper.Box> bracketData =
                CrosswalkSignalVertexData.getBracketData( mountType, isDouble, tiltOffset );
        RenderHelper.addBoxesToBuffer( bracketData, buffer, br, bg, bb, 1.0f, 0, 0, 0 );

        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private List<RenderHelper.Box> getSingleVisorData( CrosswalkVisorType visorType ) {
        switch ( visorType ) {
            case CRATE:
                return CrosswalkSignalVertexData.SINGLE_VISOR_CRATE_VERTEX_DATA;
            case HOOD:
                return CrosswalkSignalVertexData.SINGLE_VISOR_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return CrosswalkSignalVertexData.SINGLE_VISOR_NONE_VERTEX_DATA;
        }
    }

    private List<RenderHelper.Box> getDoubleUpperVisorData( CrosswalkVisorType visorType ) {
        switch ( visorType ) {
            case CRATE:
                return CrosswalkSignalVertexData.DOUBLE_UPPER_VISOR_CRATE_VERTEX_DATA;
            case HOOD:
                return CrosswalkSignalVertexData.DOUBLE_UPPER_VISOR_HOOD_VERTEX_DATA;
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
            case NONE:
            default:
                return CrosswalkSignalVertexData.DOUBLE_VISOR_NONE_VERTEX_DATA;
        }
    }

    // =====================================================================================
    // Dynamic: display face rendering
    // =====================================================================================

    private void renderDisplayFace( CrosswalkDisplayType displayType, int colorState ) {
        // Reset GL color after display list (which may leave stale color)
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );

        // Flash timing: 1Hz on/off cycle during clearance (color=1)
        boolean flashOn = ( System.currentTimeMillis() % 1000 ) < 500;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        if ( displayType == CrosswalkDisplayType.SYMBOL ) {
            ResourceLocation tex = CrosswalkTextureMap.getSingleFaceTexture(
                    colorState, flashOn );
            Minecraft.getMinecraft().getTextureManager().bindTexture( tex );

            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addTexturedQuad( buffer,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_Y1,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_Y2,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_FACE_Z );
            tessellator.draw();
        }
        else {
            // Upper section (DON'T WALK)
            ResourceLocation upperTex = CrosswalkTextureMap.getDoubleUpperTexture(
                    colorState, flashOn );
            Minecraft.getMinecraft().getTextureManager().bindTexture( upperTex );
            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addTexturedQuad( buffer,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.DOUBLE_UPPER_DISPLAY_Y1,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.DOUBLE_UPPER_DISPLAY_Y2,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z );
            tessellator.draw();

            // Lower section (WALK)
            ResourceLocation lowerTex = CrosswalkTextureMap.getDoubleLowerTexture(
                    colorState, flashOn );
            Minecraft.getMinecraft().getTextureManager().bindTexture( lowerTex );
            buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );
            addTexturedQuad( buffer,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X1,
                    CrosswalkSignalVertexData.DOUBLE_LOWER_DISPLAY_Y1,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_X2,
                    CrosswalkSignalVertexData.DOUBLE_LOWER_DISPLAY_Y2,
                    CrosswalkSignalVertexData.DOUBLE_DISPLAY_FACE_Z );
            tessellator.draw();
        }
    }

    /**
     * Adds a textured quad facing toward the viewer (toward -Z / north face).
     * UV maps the full texture (0,0)-(1,1) onto the quad.
     */
    private void addTexturedQuad( BufferBuilder buffer, float x1, float y1, float x2, float y2,
            float z ) {
        // CCW winding, facing -Z (north)
        buffer.pos( x2, y1, z ).tex( 0.0, 1.0 ).endVertex();
        buffer.pos( x1, y1, z ).tex( 1.0, 1.0 ).endVertex();
        buffer.pos( x1, y2, z ).tex( 1.0, 0.0 ).endVertex();
        buffer.pos( x2, y2, z ).tex( 0.0, 0.0 ).endVertex();
    }

    // =====================================================================================
    // Dynamic: countdown overlay (7-segment display)
    // =====================================================================================

    private void renderCountdown( TileEntityCrosswalkSignalNew te, int colorState ) {
        int countdown = te.getCurrentCountdown();
        if ( countdown < 0 || colorState != 1 ) return;

        GlStateManager.disableTexture2D();
        GlStateManager.depthMask( false );

        int displayValue = Math.min( countdown, 99 );
        String text = String.valueOf( displayValue );
        int numDigits = text.length();

        float totalWidth = numDigits * DIGIT_WIDTH + ( numDigits - 1 ) * DIGIT_GAP;
        float centerX = numDigits > 1 ? AREA_CENTER_X - 0.03f : AREA_CENTER_X;
        float startX = centerX - totalWidth / 2;

        // The countdown rendering needs to be in block-space coordinates relative to
        // the signal body center. Since we're already in model units (1/16 block),
        // we need to convert the block-space countdown coordinates to model units.
        // The countdown area is in the right half of the display face.
        // Scale factor: 1 block-space unit = 16 model units
        float scale = 16.0f;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );

        for ( int i = 0; i < numDigits; i++ ) {
            int digit = text.charAt( i ) - '0';
            float dx = startX + i * ( DIGIT_WIDTH + DIGIT_GAP );
            // Draw in model units: center of display face
            float modelDx = 8.0f + dx * scale;
            float modelDy = 8.0f - ( DIGIT_HEIGHT / 2 ) * scale;
            float modelW = DIGIT_WIDTH * scale;
            float modelH = DIGIT_HEIGHT * scale;
            drawDigitSegments( buf, digit, modelDx, modelDy, modelW, modelH,
                    CrosswalkSignalVertexData.SINGLE_DISPLAY_FACE_Z - Z_EPSILON );
        }

        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.depthMask( true );
    }

    private static void drawDigitSegments( BufferBuilder buf, int digit, float dx, float dy,
            float w, float h, float z ) {
        boolean[] segs = DIGIT_SEGMENTS[digit];
        for ( int s = 0; s < 7; s++ ) {
            if ( !segs[s] ) continue;
            float[] seg = SEGMENTS[s];

            float x1 = dx + ( seg[0] / 5f ) * w;
            float x2 = dx + ( seg[2] / 5f ) * w;
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
