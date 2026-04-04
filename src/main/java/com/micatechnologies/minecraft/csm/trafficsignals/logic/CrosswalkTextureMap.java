package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.ResourceLocation;

/**
 * Maps crosswalk signal display type and color state to texture ResourceLocations. Uses the
 * existing individual crosswalk textures for now; a future optimization could composite these
 * into a single atlas like the traffic signal bulb textures.
 */
public class CrosswalkTextureMap {

    // Single-face (symbol) textures — these are the existing textures but will need to be
    // replaced with clean versions (no baked crate pattern) once the crate visor geometry
    // is working. For initial development, use the existing textures.
    private static final ResourceLocation TEX_HAND_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalkhand.png" );
    private static final ResourceLocation TEX_MAN_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalkman.png" );
    private static final ResourceLocation TEX_OFF = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalkoff.png" );

    // Double-worded (text) textures
    private static final ResourceLocation TEX_DONTWALK_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextdontwalkon.png" );
    private static final ResourceLocation TEX_DONTWALK_OFF = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextdontwalkoff.png" );
    private static final ResourceLocation TEX_WALK_LIT = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextwalkon.png" );
    private static final ResourceLocation TEX_WALK_OFF = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/shared_textures/crosswalktextwalkoff.png" );

    /**
     * Gets the texture for a single-face crosswalk signal based on color state and flash timing.
     *
     * @param colorState 0=don't walk, 1=clearance (flashing), 2=walk, 3=off
     * @param flashOn    whether the flash is currently in the "on" phase (only relevant for
     *                   color=1)
     *
     * @return the ResourceLocation of the texture to bind
     */
    public static ResourceLocation getSingleFaceTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: // DON'T WALK
                return TEX_HAND_LIT;
            case 1: // CLEARANCE (flashing hand)
                return flashOn ? TEX_HAND_LIT : TEX_OFF;
            case 2: // WALK
                return TEX_MAN_LIT;
            case 3: // OFF
            default:
                return TEX_OFF;
        }
    }

    /**
     * Gets the texture for the upper section of a double-worded crosswalk signal (DON'T WALK).
     */
    public static ResourceLocation getDoubleUpperTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: // DON'T WALK
                return TEX_DONTWALK_LIT;
            case 1: // CLEARANCE (flashing DON'T WALK)
                return flashOn ? TEX_DONTWALK_LIT : TEX_DONTWALK_OFF;
            case 2: // WALK
                return TEX_DONTWALK_OFF;
            case 3: // OFF
            default:
                return TEX_DONTWALK_OFF;
        }
    }

    /**
     * Gets the texture for the lower section of a double-worded crosswalk signal (WALK).
     */
    public static ResourceLocation getDoubleLowerTexture( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: // DON'T WALK
                return TEX_WALK_OFF;
            case 1: // CLEARANCE
                return TEX_WALK_OFF;
            case 2: // WALK
                return TEX_WALK_LIT;
            case 3: // OFF
            default:
                return TEX_WALK_OFF;
        }
    }
}
