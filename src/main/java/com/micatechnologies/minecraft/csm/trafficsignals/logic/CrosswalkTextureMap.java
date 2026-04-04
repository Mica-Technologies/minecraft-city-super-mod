package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.ResourceLocation;

/**
 * Maps crosswalk signal display type, bulb type, and color state to texture ResourceLocations.
 */
public class CrosswalkTextureMap {

    // --- Single 16-inch (symbol) textures ---
    private static final ResourceLocation TEX_HAND_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_hand_lit.png" );
    private static final ResourceLocation TEX_MAN_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_man_lit.png" );
    private static final ResourceLocation TEX_OFF = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_off.png" );

    // --- Double 12-inch worded (text) textures ---
    private static final ResourceLocation TEX_DONTWALK_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextdontwalkon.png" );
    private static final ResourceLocation TEX_DONTWALK_OFF = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextdontwalkoff.png" );
    private static final ResourceLocation TEX_WALK_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextwalkon.png" );
    private static final ResourceLocation TEX_WALK_OFF = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextwalkoff.png" );

    // --- Double 12-inch hand/man with countdown textures ---
    private static final ResourceLocation TEX_HAND_LIT_12IN = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_hand_lit_12in.png" );
    private static final ResourceLocation TEX_MAN_LIT_12IN = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_man_lit_12in.png" );
    private static final ResourceLocation TEX_OFF_12IN = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_off_12in.png" );
    private static final ResourceLocation TEX_BASE_12IN = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_base_texture_12in.png" );

    // =========================================================================
    // Single 16-inch signal (always SYMBOL display type)
    // =========================================================================

    public static ResourceLocation getSingleFaceTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return TEX_HAND_LIT;
            case 1: return flashOn ? TEX_HAND_LIT : TEX_OFF;
            case 2: return TEX_MAN_LIT;
            case 3:
            default: return TEX_OFF;
        }
    }

    // =========================================================================
    // Double 12-inch stacked — WORDED bulb type
    // =========================================================================

    public static ResourceLocation getWordedUpperTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return TEX_DONTWALK_LIT;
            case 1: return flashOn ? TEX_DONTWALK_LIT : TEX_DONTWALK_OFF;
            case 2: return TEX_DONTWALK_OFF;
            case 3:
            default: return TEX_DONTWALK_OFF;
        }
    }

    public static ResourceLocation getWordedLowerTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return TEX_WALK_OFF;
            case 1: return TEX_WALK_OFF;
            case 2: return TEX_WALK_LIT;
            case 3:
            default: return TEX_WALK_OFF;
        }
    }

    // =========================================================================
    // Double 12-inch stacked — HAND_MAN_COUNTDOWN bulb type
    // Upper section: bimodal hand/man. Lower section: countdown module base.
    // =========================================================================

    /**
     * Upper section texture for hand/man countdown mode. Shows hand or man bimodally.
     */
    public static ResourceLocation getHandManUpperTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return TEX_HAND_LIT_12IN;
            case 1: return flashOn ? TEX_HAND_LIT_12IN : TEX_OFF_12IN;
            case 2: return TEX_MAN_LIT_12IN;
            case 3:
            default: return TEX_OFF_12IN;
        }
    }

    /**
     * Lower section texture for the countdown module. This is the dark base that sits behind
     * the 7-segment countdown overlay. Always shows the base texture (the countdown digits
     * are rendered on top by the renderer).
     */
    public static ResourceLocation getHandManLowerTexture() {
        return TEX_BASE_12IN;
    }
}
