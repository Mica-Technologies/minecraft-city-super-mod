package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.ResourceLocation;

/**
 * Maps blankout box type and on/off state to atlas UV coordinates. Uses a single atlas texture
 * with a 4x2 grid of 256x256 tiles in a 1024x512 atlas.
 *
 * <pre>
 * Index  Texture       Row  Col  Type           State
 * -----  ----------    ---  ---  -------------- -----
 *   0    DW_BO         0    0    Don't Walk     ON
 *   1    NLT_BO        0    1    No Left Turn   ON
 *   2    NRT_BO        0    2    No Right Turn  ON
 *   3    DNE_BO        0    3    Do Not Enter   ON
 *   4    DW_BO_OFF     1    0    Don't Walk     OFF
 *   5    NLT_BO_OFF    1    1    No Left Turn   OFF
 *   6    NRT_BO_OFF    1    2    No Right Turn  OFF
 *   7    DNE_BO_OFF    1    3    Do Not Enter   OFF
 * </pre>
 */
public class BlankoutBoxTextureMap {

    public static final ResourceLocation ATLAS_TEXTURE = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/blankout_boxes/blankout_box_atlas.png" );

    private static final int COLS = 4;
    private static final int ROWS = 2;
    private static final float TILE_U = 1.0f / COLS;
    private static final float TILE_V = 1.0f / ROWS;

    public static int getAtlasIndex( BlankoutBoxType type, boolean isOn ) {
        return isOn ? type.ordinal() : type.ordinal() + COLS;
    }

    public static float[] getAtlasUV( int index ) {
        int col = index % COLS;
        int row = index / COLS;
        float u1 = col * TILE_U;
        float v1 = row * TILE_V;
        float u2 = u1 + TILE_U;
        float v2 = v1 + TILE_V;
        return new float[]{ u1, v1, u2, v2 };
    }
}
