package com.micatechnologies.minecraft.csm.buildingmaterials;

/**
 * A wood stud wall framed for a window: king studs each side, a header over and a sill under.
 *
 * <p>Solid to walk into, unlike {@link BlockWoodStudWallDoor}: the opening starts above the
 * sill, which is above the height anything walks through.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodStudWallWindow extends AbstractBlockWoodFraming {

  @Override
  public String getBlockRegistryName() {
    return "wood_stud_wall_window";
  }
}
