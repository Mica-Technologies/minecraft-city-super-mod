package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A steel stud wall carrying flat strap X-bracing.
 *
 * <p>The strap is what keeps a stud wall from racking. It is drawn at 45 degrees because a model
 * element can be rotated about one axis and only to 45 or 22.5 degrees, so the strap crosses its
 * own bay rather than running corner to corner across several.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSteelStudWallBraced extends AbstractBlockSteelFraming {

  @Override
  public String getBlockRegistryName() {
    return "steel_stud_wall_braced";
  }
}
