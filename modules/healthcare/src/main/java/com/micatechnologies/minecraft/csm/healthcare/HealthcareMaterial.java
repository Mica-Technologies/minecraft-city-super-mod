package com.micatechnologies.minecraft.csm.healthcare;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * What a Healthcare piece is made of, which sets how it sounds and breaks.
 *
 * @since 2026.10
 */
public enum HealthcareMaterial {
  /** Steel, aluminium: beds, poles, carts, outlets. */
  METAL(Material.IRON, SoundType.METAL, "pickaxe", 2.0F),
  /** Moulded plastic and painted panels: dispensers, bins, monitors, headwalls. */
  PLASTIC(Material.ROCK, SoundType.STONE, "pickaxe", 1.5F),
  /** Upholstery and curtains. */
  CLOTH(Material.CLOTH, SoundType.CLOTH, "axe", 0.8F),
  /** Wood and laminate furniture. */
  WOOD(Material.WOOD, SoundType.WOOD, "axe", 1.5F);

  private final Material material;
  private final SoundType sound;
  private final String tool;
  private final float hardness;

  HealthcareMaterial(Material material, SoundType sound, String tool, float hardness) {
    this.material = material;
    this.sound = sound;
    this.tool = tool;
    this.hardness = hardness;
  }

  public Material getMaterial() {
    return material;
  }

  public SoundType getSound() {
    return sound;
  }

  public String getTool() {
    return tool;
  }

  public float getHardness() {
    return hardness;
  }
}
