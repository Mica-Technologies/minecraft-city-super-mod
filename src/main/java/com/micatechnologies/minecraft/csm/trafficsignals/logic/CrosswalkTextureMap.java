package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.ResourceLocation;

/**
 * Maps crosswalk signal display type, bulb type, and color state to texture atlas UV coordinates.
 * Uses a single atlas texture ({@link #ATLAS_TEXTURE}) with tiles indexed by position.
 *
 * <p>Atlas layout (4x4 grid of 128x128 tiles in 512x512 atlas):
 * <pre>
 * Index  Texture                      Row  Col  Signal    Section   State
 * -----  ---------------------------  ---  ---  --------  --------  ----------------
 *   0    crosswalk_hand_lit           0    0    16-inch   face      don't walk (lit)
 *   1    crosswalk_man_lit            0    1    16-inch   face      walk (lit)
 *   2    crosswalk_off                0    2    16-inch   face      off (ghosted)
 *   3    crosswalk_hand_lit_12in      0    3    12-inch   upper     don't walk (lit)
 *   4    crosswalk_man_lit_12in       1    0    12-inch   upper     walk (lit)
 *   5    crosswalk_off_12in           1    1    12-inch   upper     off (ghosted)
 *   6    crosswalk_base_texture_12in  1    2    12-inch   lower     countdown base
 * </pre>
 */
public class CrosswalkTextureMap {

    public static final ResourceLocation ATLAS_TEXTURE = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/crosswalk/crosswalk_atlas.png" );

    // Atlas grid: 4 tiles per row, 4 rows
    private static final int TILES_PER_ROW = 4;
    private static final float TILE_UV = 1.0f / TILES_PER_ROW;

    // --- Atlas tile indices ---
    // 16-inch single signal
    private static final int IDX_HAND_LIT = 0;
    private static final int IDX_MAN_LIT = 1;
    private static final int IDX_OFF = 2;

    // 12-inch stacked — hand/man + countdown
    private static final int IDX_HAND_LIT_12IN = 3;
    private static final int IDX_MAN_LIT_12IN = 4;
    private static final int IDX_OFF_12IN = 5;
    private static final int IDX_BASE_12IN = 6;

    // 12-inch stacked — worded/text
    private static final int IDX_DONTWALK_LIT = 7;
    private static final int IDX_DONTWALK_OFF = 8;
    private static final int IDX_WALK_LIT = 9;
    private static final int IDX_WALK_OFF = 10;

    // =========================================================================
    // UV coordinate helpers
    // =========================================================================

    /** Returns [u1, v1, u2, v2] for the given atlas tile index. */
    public static float[] getAtlasUV( int index ) {
        int col = index % TILES_PER_ROW;
        int row = index / TILES_PER_ROW;
        float u1 = col * TILE_UV;
        float v1 = row * TILE_UV;
        float u2 = u1 + TILE_UV;
        float v2 = v1 + TILE_UV;
        return new float[]{ u1, v1, u2, v2 };
    }

    // =========================================================================
    // 16-inch single signal — atlas indices
    // =========================================================================

    public static int getSingleFaceAtlasIndex( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return IDX_HAND_LIT;
            case 1: return flashOn ? IDX_HAND_LIT : IDX_OFF;
            case 2: return IDX_MAN_LIT;
            case 3:
            default: return IDX_OFF;
        }
    }

    // =========================================================================
    // 12-inch stacked — HAND_MAN_COUNTDOWN atlas indices
    // =========================================================================

    public static int getHandManUpperAtlasIndex( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return IDX_HAND_LIT_12IN;
            case 1: return flashOn ? IDX_HAND_LIT_12IN : IDX_OFF_12IN;
            case 2: return IDX_MAN_LIT_12IN;
            case 3:
            default: return IDX_OFF_12IN;
        }
    }

    public static int getHandManLowerAtlasIndex() {
        return IDX_BASE_12IN;
    }

    // =========================================================================
    // 12-inch stacked — WORDED atlas indices
    // =========================================================================

    public static int getWordedUpperAtlasIndex( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return IDX_DONTWALK_LIT;
            case 1: return flashOn ? IDX_DONTWALK_LIT : IDX_DONTWALK_OFF;
            case 2: return IDX_DONTWALK_OFF;
            case 3:
            default: return IDX_DONTWALK_OFF;
        }
    }

    public static int getWordedLowerAtlasIndex( int colorState, boolean flashOn ) {
        switch ( colorState ) {
            case 0: return IDX_WALK_OFF;
            case 1: return IDX_WALK_OFF;
            case 2: return IDX_WALK_LIT;
            case 3:
            default: return IDX_WALK_OFF;
        }
    }
}
