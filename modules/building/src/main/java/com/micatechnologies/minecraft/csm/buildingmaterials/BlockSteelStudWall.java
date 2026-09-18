package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A light-gauge steel stud wall: galvanised C-studs with punched knockouts, standing in top and
 * bottom track.
 *
 * <p>The first member of the framing family, and the one the rest are measured against.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSteelStudWall extends AbstractBlockSteelFraming {

  @Override
  public String getBlockRegistryName() {
    return "steel_stud_wall";
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
