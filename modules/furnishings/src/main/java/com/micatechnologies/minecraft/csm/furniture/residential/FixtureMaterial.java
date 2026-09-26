package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * What a bathroom or laundry fitting is made of, which decides how it sounds and how long it
 * takes to break. None needs a tool to drop (a porcelain toilet is broken by hand like a glass
 * pane, a chrome rail like an appliance), so the materials are the ones the kitchen's
 * appliances and tableware already use.
 *
 * @since 2026.9
 */
public enum FixtureMaterial {
  /** Glazed porcelain: toilets, urinals, basins, the bathtub. */
  PORCELAIN(Material.GLASS, SoundType.STONE, 1.2F),
  /** Chrome or stainless steel: rails, holders, grab bars, dispensers. */
  METAL(Material.WOOD, SoundType.METAL, 1.5F),
  /** White or grey plastic: the changing station, the laundry tub. */
  PLASTIC(Material.WOOD, SoundType.STONE, 1.0F),
  /** Glass in a chrome frame: the shower enclosure. */
  GLASS(Material.GLASS, SoundType.GLASS, 1.0F),
  /** Timber: a pallet, a display table's frame. */
  WOOD(Material.WOOD, SoundType.WOOD, 1.5F);

  private final Material material;
  private final SoundType sound;
  private final float hardness;

  FixtureMaterial(Material material, SoundType sound, float hardness) {
    this.material = material;
    this.sound = sound;
    this.hardness = hardness;
  }

  public Material getMaterial() {
    return material;
  }

  public SoundType getSound() {
    return sound;
  }

  public float getHardness() {
    return hardness;
  }
}
