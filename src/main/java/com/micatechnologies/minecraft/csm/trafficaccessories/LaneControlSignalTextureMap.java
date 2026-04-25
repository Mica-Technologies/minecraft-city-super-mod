package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.ResourceLocation;

public class LaneControlSignalTextureMap {

    public static final ResourceLocation ATLAS_TEXTURE = new ResourceLocation("csm",
            "textures/blocks/trafficaccessories/lane_control_signal/lane_control_signal_atlas.png");

    private static final int COLS = 4;
    private static final int ROWS = 2;
    private static final float TILE_U = 1.0f / COLS;
    private static final float TILE_V = 1.0f / ROWS;

    public static float[] getAtlasUV(LaneControlSignalType type) {
        int index = type.ordinal();
        int col = index % COLS;
        int row = index / COLS;
        float u1 = col * TILE_U;
        float v1 = row * TILE_V;
        float u2 = u1 + TILE_U;
        float v2 = v1 + TILE_V;
        return new float[]{u1, v1, u2, v2};
    }
}
