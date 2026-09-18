package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A ceiling joist: lumber spanning the room at ceiling level, carrying the ceiling below it.
 *
 * <p>Sits at the TOP of its block, on the plate, rather than in the middle where a floor joist
 * sits, and is shallower because it carries a ceiling and not a floor.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockCeilingJoist extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(6 / 16.0D, 10 / 16.0D, 0.0D, 10 / 16.0D, 1.0D, 1.0D);

  /**
   * Constructs a {@link BlockCeilingJoist}.
   *
   * @since 1.0
   */
  public BlockCeilingJoist() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 1.0F, 5F);
  }

  @Override
  public String getBlockRegistryName() {
    return "ceiling_joist";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
