package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Corrugated steel roof deck.\n *\n * <p>The rib pitch divides the block exactly, so a sheet tiles without the ribs bunching\n * or splitting at the seam. Walkable, since a deck is what a roof is walked on.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockMetalRoofDeck extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(0 / 16.0D, 12 / 16.0D, 0.0D,
          16 / 16.0D, 15 / 16.0D, 1.0D);

  /**
   * Constructs a {@link BlockMetalRoofDeck}.
   *
   * @since 1.0
   */
  public BlockMetalRoofDeck() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1.5F,
        8F);
  }

  @Override
  public String getBlockRegistryName() {
    return "metal_roof_deck";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
