package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A wood stud wall with a row of fire blocking through the bays.
 *
 * <p>Short lengths of the same lumber laid flat between the studs, part way up. Real framing
 * carries a row of it to stop a stud bay acting as a chimney.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodStudWallBlocking extends AbstractBlockWoodFraming {

  @Override
  public String getBlockRegistryName() {
    return "wood_stud_wall_blocking";
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
