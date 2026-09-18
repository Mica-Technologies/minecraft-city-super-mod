package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A narrow steel stud wall: the 2.5 in partition stud rather than the 3.625 in one.
 *
 * <p>What a real building uses where a partition only has to divide a space and carry nothing —
 * around a service riser, or between two offices. It joins the standard wall, because both are
 * steel framing, and the change in thickness at the joint is what a real wall does there too.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSteelStudWallNarrow extends AbstractBlockSteelFraming {

  @Override
  public String getBlockRegistryName() {
    return "steel_stud_wall_narrow";
  }

  /**
   * Has open stud bays, so it offers the three insulation variants.
   *
   * @since 1.0
   */
  @Override
  public boolean supportsInsulation() {
    return true;
  }
}
