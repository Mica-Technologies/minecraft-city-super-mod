package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.ResourceLocation;

/**
 * Maps blankout box type and on/off state to atlas UV coordinates. Uses a single atlas texture
 * with an 8x2 grid of 256x256 tiles in a 2048x512 atlas: row 0 holds the lit faces in
 * {@link BlankoutBoxType} order, row 1 the unlit ones. Columns past the last type are spare
 * capacity for future legends.
 *
 * <pre>
 * Index  Texture        Row  Col  Type           State
 * -----  ----------     ---  ---  -------------- -----
 *   0    DW_BO          0    0    Don't Walk     ON
 *   1    NLT_BO         0    1    No Left Turn   ON
 *   2    NRT_BO         0    2    No Right Turn  ON
 *   3    DNE_BO         0    3    Do Not Enter   ON
 *   4    NUT_BO         0    4    No U-Turn      ON
 *   5    TRAIN_BO       0    5    Train          ON
 *   8    DW_BO_OFF      1    0    Don't Walk     OFF
 *   9    NLT_BO_OFF     1    1    No Left Turn   OFF
 *  10    NRT_BO_OFF     1    2    No Right Turn  OFF
 *  11    DNE_BO_OFF     1    3    Do Not Enter   OFF
 *  12    NUT_BO_OFF     1    4    No U-Turn      OFF
 *  13    TRAIN_BO_OFF   1    5    Train          OFF
 * </pre>
 */
public class BlankoutBoxTextureMap {

    public static final ResourceLocation ATLAS_TEXTURE = new ResourceLocation( "csm",
            "textures/blocks/trafficsignals/blankout_boxes/blankout_box_atlas.png" );

    private static final int COLS = 8;
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
