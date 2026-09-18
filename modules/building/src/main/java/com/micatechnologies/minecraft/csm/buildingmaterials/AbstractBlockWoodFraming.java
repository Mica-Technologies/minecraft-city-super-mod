package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * A wood framing member: dimensional lumber, studs standing in a sole plate.
 *
 * <p>The same shapes as the steel family in a different material, drawn from the same shared
 * geometry with a different pair of textures. It joins the steel members: a run may change from
 * one to the other and stay one wall, which is what a real building does and what the photograph
 * this family was built from shows.</p>
 *
 * <p>Softer than steel and cut with an axe, which is the only thing that really separates the two
 * in play.</p>
 *
 * @version 1.0
 * @see AbstractBlockSteelFraming
 * @since 2026.9
 */
public abstract class AbstractBlockWoodFraming extends BlockFramingWall {

  /**
   * Constructs an {@link AbstractBlockWoodFraming}.
   *
   * @since 1.0
   */
  protected AbstractBlockWoodFraming() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 1.0F, 5F);
  }
}
