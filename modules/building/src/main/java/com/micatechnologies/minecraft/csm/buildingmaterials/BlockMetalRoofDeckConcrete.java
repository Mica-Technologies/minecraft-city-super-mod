package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Composite deck: the same corrugated sheet with concrete poured into the flutes and over.\n *\n * <p>How nearly every multi-storey floor slab is actually built. Solid to the top of the\n * topping, unlike the bare deck.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockMetalRoofDeckConcrete extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(0 / 16.0D, 12 / 16.0D, 0.0D,
          16 / 16.0D, 16 / 16.0D, 1.0D);

  /**
   * Constructs a {@link BlockMetalRoofDeckConcrete}.
   *
   * @since 1.0
   */
  public BlockMetalRoofDeckConcrete() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2.0F,
        10F);
  }

  @Override
  public String getBlockRegistryName() {
    return "metal_roof_deck_concrete";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
