package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightMountKit;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVertexData;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class TileEntityBlankoutBoxRenderer
        extends TileEntitySpecialRenderer<TileEntityBlankoutBox> {

    private final Map<BlockPos, Integer> displayListCache = new HashMap<>();
    private final Map<BlockPos, Integer> lastCombinedLightCache = new HashMap<>();

    // Visor tint parameters (same as crosswalk)
    private static final float VISOR_TINT_SCALE = 1.04f;
    private static final float VISOR_TINT_BASE = 0.01f;
    private static final float VISOR_INNER_R = 0.0f;
    private static final float VISOR_INNER_G = 0.0f;
    private static final float VISOR_INNER_B = 0.0f;

    private static final ResourceLocation WHITE_TEXTURE =
        new ResourceLocation("csm", "textures/blocks/white1px.png");
    private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
    private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

    public void cleanupDisplayList( BlockPos pos ) {
        Integer listId = displayListCache.remove( pos );
        if ( listId != null ) {
            GL11.glDeleteLists( listId, 1 );
        }
        lastCombinedLightCache.remove( pos );
    }

    @Override
    public void render( TileEntityBlankoutBox te, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha ) {

        if ( te.getWorld() == null ) return;

        IBlockState blockState = te.getWorld().getBlockState( te.getPos() );
        if ( !( blockState.getBlock() instanceof BlockBlankoutBox ) ) {
            return;
        }

        EnumFacing facing = blockState.getValue( BlockBlankoutBox.FACING );
        int colorValue = blockState.getValue(
                com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.COLOR );

        TrafficSignalBodyColor bodyColor = te.getBodyColor();
        TrafficSignalBodyColor visorColor = te.getVisorColor();
        BlankoutBoxVisorType visorType = te.getVisorType();
        CrosswalkMountType mountType = te.getMountType();
        TrafficSignalBodyTilt bodyTilt = te.getBodyTilt();
        BlankoutBoxType blankoutType = te.getBlankoutType();

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

        // World lightmap is now baked per-vertex via the BLOCK vertex format (shader pipeline
        // doesn't honor OpenGlHelper.setLightmapTextureCoords global state).
        int combinedLight = te.getWorld().getCombinedLight( te.getPos(), 0 );
        int worldSkyLight = ( combinedLight >> 16 ) & 0xFFFF;
        int worldBlockLight = combinedLight & 0xFFFF;

        GL11.glPushMatrix();
        GL11.glTranslated( x, y, z );
        GL11.glScaled( 0.0625, 0.0625, 0.0625 );

        int tiltOffset = 0;
        if ( bodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE ) tiltOffset = -4;
        else if ( bodyTilt == TrafficSignalBodyTilt.RIGHT_TILT ) tiltOffset = -2;
        else if ( bodyTilt == TrafficSignalBodyTilt.LEFT_TILT ) tiltOffset = 2;
        else if ( bodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE ) tiltOffset = 4;

        // Horizontal arms (BASE facing context)
        if ( mountType != CrosswalkMountType.BASE ) {
            GL11.glPushMatrix();
            GL11.glTranslated( 8, 8, 8 );
            GL11.glRotatef( baseDirection.getRotation(), 0, 1, 0 );
            GL11.glTranslated( -8, -8, -8 );

            renderBoxes( bodyColor, BlankoutBoxVertexData.getArmData(
                    mountType, tiltOffset,
                    bodyDirection.getRotation(), baseDirection.getRotation() ),
                    worldSkyLight, worldBlockLight );

            GL11.glPopMatrix();
        }

        // Body + visor + stubs (TILTED facing context)
        GL11.glTranslated( 8, 8, 8 );
        GL11.glRotatef( bodyDirection.getRotation(), 0, 1, 0 );
        GL11.glTranslated( -8, -8, -8 );
        if ( tiltOffset != 0 ) {
            GL11.glTranslated( tiltOffset, 0, 0 );
        }

        // Push body back toward mount kit when placed in front of one with no side/rear mount
        if ( mountType == CrosswalkMountType.BASE ) {
            BlockPos behind = te.getPos().offset( facing.getOpposite() );
            if ( te.getWorld().getBlockState( behind ).getBlock()
                    instanceof BlockTrafficLightMountKit ) {
                GL11.glTranslated( 0, 0, 8 );
            }
        }

        // Stubs
        if ( mountType != CrosswalkMountType.BASE ) {
            renderBoxes( bodyColor, BlankoutBoxVertexData.getStubData( mountType ),
                    worldSkyLight, worldBlockLight );
        }

        // Display list: body + visor
        BlockPos pos = te.getPos();
        Integer displayList = displayListCache.get( pos );
        Integer lastLight = lastCombinedLightCache.get( pos );
        if ( displayList == null || te.isStateDirty()
                || lastLight == null || lastLight != combinedLight ) {
            if ( displayList != null ) {
                GL11.glDeleteLists( displayList, 1 );
            }
            displayList = GL11.glGenLists( 1 );
            displayListCache.put( pos, displayList );
            lastCombinedLightCache.put( pos, combinedLight );
            GL11.glNewList( displayList, GL11.GL_COMPILE );
            renderStaticParts( bodyColor, visorColor, visorType, worldSkyLight, worldBlockLight );
            GL11.glEndList();
            te.clearDirtyFlag();
        }
        // Bind WHITE_TEXTURE outside the display list so it's always current at replay time
        // — see TileEntityTrafficSignalHeadRenderer for the full explanation. Gated on
        // shaderCompatibilityMode so non-shader users skip the per-frame bind.
        if ( CsmConfig.isShaderCompatibilityModeEnabled() ) {
            Minecraft.getMinecraft().getTextureManager().bindTexture( WHITE_TEXTURE );
        }
        GL11.glCallList( displayList );

        // Display face texture
        boolean flashOn = ( CsmRenderUtils.gameMillis( te.getWorld(), partialTicks )
                % 1000L ) < 500L;
        renderDisplayFace( blankoutType, colorValue, flashOn );

        GL11.glPopMatrix();

        // Reset GL state
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
        GlStateManager.resetColor();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
    }

    private void renderStaticParts( TrafficSignalBodyColor bodyColor,
            TrafficSignalBodyColor visorColor, BlankoutBoxVisorType visorType,
            int skyLight, int blockLight ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        Minecraft.getMinecraft().getTextureManager().bindTexture( WHITE_TEXTURE );
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.BLOCK );

        float br = bodyColor.getRed(), bg = bodyColor.getGreen(), bb = bodyColor.getBlue();
        RenderHelper.addBoxesToBufferLit( BlankoutBoxVertexData.BODY_VERTEX_DATA,
                buffer, br, bg, bb, 1.0f, 0, 0, 0, skyLight, blockLight );

        float vr = Math.min( 1.0f, visorColor.getRed() * VISOR_TINT_SCALE + VISOR_TINT_BASE );
        float vg = Math.min( 1.0f, visorColor.getGreen() * VISOR_TINT_SCALE + VISOR_TINT_BASE );
        float vb = Math.min( 1.0f, visorColor.getBlue() * VISOR_TINT_SCALE + VISOR_TINT_BASE );

        List<RenderHelper.Box> visorData = getVisorData( visorType );
        boolean isHood = visorType == BlankoutBoxVisorType.HOOD
                || visorType == BlankoutBoxVisorType.DEEP_HOOD;
        if ( isHood ) {
            RenderHelper.addBoxesToBufferDualColorLit( visorData, buffer,
                    vr, vg, vb, VISOR_INNER_R, VISOR_INNER_G, VISOR_INNER_B,
                    1.0f, 0, 0, 0,
                    BlankoutBoxVertexData.VISOR_CENTER_X, BlankoutBoxVertexData.VISOR_CENTER_Y,
                    skyLight, blockLight );
        }
        else if ( !visorData.isEmpty() ) {
            RenderHelper.addBoxesToBufferLit( visorData, buffer, vr, vg, vb, 1.0f, 0, 0, 0,
                    skyLight, blockLight );
        }

        tessellator.draw();
    }

    private void renderBoxes( TrafficSignalBodyColor color, List<RenderHelper.Box> boxes,
            int skyLight, int blockLight ) {
        if ( boxes.isEmpty() ) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        Minecraft.getMinecraft().getTextureManager().bindTexture( WHITE_TEXTURE );
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.BLOCK );
        RenderHelper.addBoxesToBufferLit( boxes, buffer,
                color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0,
                skyLight, blockLight );
        tessellator.draw();
    }

    private List<RenderHelper.Box> getVisorData( BlankoutBoxVisorType visorType ) {
        switch ( visorType ) {
            case HOOD:
                return BlankoutBoxVertexData.VISOR_HOOD_VERTEX_DATA;
            case DEEP_HOOD:
                return BlankoutBoxVertexData.VISOR_DEEP_HOOD_VERTEX_DATA;
            case NONE:
            default:
                return BlankoutBoxVertexData.VISOR_NONE_VERTEX_DATA;
        }
    }

    private void renderDisplayFace( BlankoutBoxType blankoutType, int colorValue,
            boolean flashOn ) {
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );

        boolean showOn;
        if ( blankoutType == BlankoutBoxType.NO_LEFT_TURN
                || blankoutType == BlankoutBoxType.NO_RIGHT_TURN ) {
            // No-turn blankouts: ON during ped/protected phases (GREEN), OFF otherwise
            switch ( colorValue ) {
                case AbstractBlockControllableSignalHead.SIGNAL_GREEN:
                    showOn = true;
                    break;
                case AbstractBlockControllableSignalHead.SIGNAL_YELLOW:
                    showOn = true;
                    break;
                default:
                    showOn = false;
                    break;
            }
        } else {
            // Dont-walk / Do-not-enter: inverted pedestrian mapping
            switch ( colorValue ) {
                case AbstractBlockControllableSignalHead.SIGNAL_RED:
                    showOn = true;
                    break;
                case AbstractBlockControllableSignalHead.SIGNAL_YELLOW:
                    showOn = flashOn;
                    break;
                default:
                    showOn = false;
                    break;
            }
        }

        int atlasIdx = BlankoutBoxTextureMap.getAtlasIndex( blankoutType, showOn );
        float[] uv = BlankoutBoxTextureMap.getAtlasUV( atlasIdx );
        float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

        Minecraft.getMinecraft().getTextureManager().bindTexture(
                BlankoutBoxTextureMap.ATLAS_TEXTURE );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        // BLOCK format with fullbright lightmap so the lit display face stays bright under
        // shaders (and shaders see a known vertex layout).
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.BLOCK );

        float x1 = BlankoutBoxVertexData.DISPLAY_X1;
        float y1 = BlankoutBoxVertexData.DISPLAY_Y1;
        float x2 = BlankoutBoxVertexData.DISPLAY_X2;
        float y2 = BlankoutBoxVertexData.DISPLAY_Y2;
        float z = BlankoutBoxVertexData.DISPLAY_FACE_Z;

        // CCW winding, facing -Z (north). X is mirrored in model space.
        displayVertex( buffer, x2, y1, z, u1, v2 );
        displayVertex( buffer, x1, y1, z, u2, v2 );
        displayVertex( buffer, x1, y2, z, u2, v1 );
        displayVertex( buffer, x2, y2, z, u1, v1 );

        tessellator.draw();
    }

    private static void displayVertex( BufferBuilder buf, float x, float y, float z,
            float u, float v ) {
        buf.pos( x, y, z ).color( 1.0f, 1.0f, 1.0f, 1.0f ).tex( u, v )
                .lightmap( LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK ).endVertex();
    }
}
