package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import net.minecraft.util.ResourceLocation;

public class GuideSignAtlas {

  public static final ResourceLocation ATLAS_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/trafficaccessories/guidesign/sign_atlas.png");

  private static final int ATLAS_WIDTH = 512;
  private static final int ATLAS_HEIGHT = 1024;
  private static final int CELL_SIZE = 64;
  private static final int COLS = ATLAS_WIDTH / CELL_SIZE;

  private static final int ARROW_ROW_OFFSET = 4;

  public static float[] getShieldUV(GuideSignShieldType type) {
    int col = type.getAtlasCol();
    int row = type.getAtlasRow();
    return getCellUV(col, row);
  }

  /** UVs for a type's wide (3+ digit) variant cell; only valid when the type has one. */
  public static float[] getShieldWideUV(GuideSignShieldType type) {
    return getCellUV(type.getWideCol(), type.getWideRow());
  }

  public static float[] getArrowUV(GuideSignArrowType type) {
    int col = type.getAtlasCol();
    int row = type.getAtlasRow() + ARROW_ROW_OFFSET;
    return getCellUV(col, row);
  }

  /**
   * UVs for an arbitrary atlas cell. Public because the atlas is shared: the dynamic street
   * sign addresses its civic-logo cells (rows 11-12) by coordinate rather than through a
   * shield or arrow enum this package knows about.
   */
  public static float[] getCellUV(int col, int row) {
    float u0 = (float) (col * CELL_SIZE) / ATLAS_WIDTH;
    float v0 = (float) (row * CELL_SIZE) / ATLAS_HEIGHT;
    float u1 = (float) ((col + 1) * CELL_SIZE) / ATLAS_WIDTH;
    float v1 = (float) ((row + 1) * CELL_SIZE) / ATLAS_HEIGHT;
    return new float[]{u0, v0, u1, v1};
  }
}
