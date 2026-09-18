package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A narrow wood stud wall: the 2x3 partition rather than the 2x4.
 *
 * <p>What a real building uses for a partition that divides a space and carries nothing.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodStudWallNarrow extends AbstractBlockWoodFraming {

  @Override
  public String getBlockRegistryName() {
    return "wood_stud_wall_narrow";
  }

  /**
   * Has open stud bays, so it offers an insulated variant.
   *
   * @since 1.0
   */
  @Override
  public boolean supportsInsulation() {
    return true;
  }
}
