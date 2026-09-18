package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * A light-gauge steel framing member: galvanised sections in galvanised track.
 *
 * <p>Exists so the members share one set of material properties rather than repeating them. It
 * decides nothing about what joins what — framing joins framing, wood included, which is what the
 * photograph this family was built from actually shows.</p>
 *
 * @version 1.0
 * @see ICsmFramingMember
 * @since 2026.9
 */
public abstract class AbstractBlockSteelFraming extends BlockFramingWall {

  /**
   * Constructs an {@link AbstractBlockSteelFraming}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelFraming() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1.5F, 8F);
  }
}
