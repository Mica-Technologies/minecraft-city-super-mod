package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * An engineered I-joist: two lumber flanges either side of a thin web.\n *\n * <p>What a modern timber floor actually uses over any span worth the name. It is lighter\n * than solid lumber for the same depth, and it does not cup or crown.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodIJoist extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(5 / 16.0D, 4 / 16.0D, 0.0D,
          11 / 16.0D, 14 / 16.0D, 1.0D);

  /**
   * Constructs a {@link BlockWoodIJoist}.
   *
   * @since 1.0
   */
  public BlockWoodIJoist() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 1.0F,
        5F);
  }

  @Override
  public String getBlockRegistryName() {
    return "wood_i_joist";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
