package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A wood stud wall carrying a let-in brace.
 *
 * <p>The diagonal is what keeps a stud wall from racking. In timber it is let into notches
 * cut across the studs rather than strapped on the face, which is why it sits flush.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodStudWallBraced extends AbstractBlockWoodFraming {

  @Override
  public String getBlockRegistryName() {
    return "wood_stud_wall_braced";
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
