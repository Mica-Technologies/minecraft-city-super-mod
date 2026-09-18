package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A wood stud wall: dimensional studs standing in a sole plate, under a double top plate.
 *
 * <p>The wood counterpart of {@link BlockSteelStudWall}, and it joins it: a run can change
 * material part way along and still read as one wall.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodStudWall extends AbstractBlockWoodFraming {

  @Override
  public String getBlockRegistryName() {
    return "wood_stud_wall";
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
