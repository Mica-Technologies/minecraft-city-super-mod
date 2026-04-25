package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import net.minecraft.util.ResourceLocation;

public class GuideSignAtlas {

  public static final ResourceLocation ATLAS_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/trafficaccessories/guidesign/sign_atlas.png");

  private static final int ATLAS_SIZE = 256;
  private static final int CELL_SIZE = 32;
  private static final int COLS = ATLAS_SIZE / CELL_SIZE;

  private static final int ARROW_ROW_OFFSET = 4;

  public static float[] getShieldUV(GuideSignShieldType type) {
    int col = type.getAtlasCol();
    int row = type.getAtlasRow();
    return getCellUV(col, row);
  }

  public static float[] getArrowUV(GuideSignArrowType type) {
    int col = type.getAtlasCol();
    int row = type.getAtlasRow() + ARROW_ROW_OFFSET;
    return getCellUV(col, row);
  }

  private static float[] getCellUV(int col, int row) {
    float u0 = (float) (col * CELL_SIZE) / ATLAS_SIZE;
    float v0 = (float) (row * CELL_SIZE) / ATLAS_SIZE;
    float u1 = (float) ((col + 1) * CELL_SIZE) / ATLAS_SIZE;
    float v1 = (float) ((row + 1) * CELL_SIZE) / ATLAS_SIZE;
    return new float[]{u0, v0, u1, v1};
  }
}
