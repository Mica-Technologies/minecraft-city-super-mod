package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * A light-gauge steel framing member: galvanised sections of the same family, which therefore all
 * join one another.
 *
 * <p>Exists so the members share one set of material properties rather than repeating them. The
 * framing kind is the only thing that decides what joins what, and it is fixed here: every steel
 * member joins every other, and meets wood framing at a corner rather than running into it.</p>
 *
 * @version 1.0
 * @see ICsmFramingMember
 * @since 2026.9
 */
public abstract class AbstractBlockSteelFraming extends BlockFramingWall {

  /** The framing system every member of this family belongs to. */
  public static final String FRAMING_KIND = "steel";

  /**
   * Constructs an {@link AbstractBlockSteelFraming}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelFraming() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1.5F, 8F);
  }

  @Override
  public String getFramingKind() {
    return FRAMING_KIND;
  }
}
