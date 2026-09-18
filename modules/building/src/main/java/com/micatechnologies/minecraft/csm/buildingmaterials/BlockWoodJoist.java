package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A dimensional lumber floor joist, on edge.\n *\n * <p>The plainest thing in this family and the one most of a timber floor is made of.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodJoist extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(6 / 16.0D, 4 / 16.0D, 0.0D,
          10 / 16.0D, 14 / 16.0D, 1.0D);

  /**
   * Constructs a {@link BlockWoodJoist}.
   *
   * @since 1.0
   */
  public BlockWoodJoist() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 1.0F,
        5F);
  }

  @Override
  public String getBlockRegistryName() {
    return "wood_joist";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
