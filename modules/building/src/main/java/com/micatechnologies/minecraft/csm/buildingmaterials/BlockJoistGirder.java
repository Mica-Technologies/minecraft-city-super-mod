package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A joist girder: the same open web, deeper and heavier, carrying joists rather than deck.\n *\n * <p>The member that spans between columns and that the bar joists land on.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockJoistGirder extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(5 / 16.0D, 1 / 16.0D, 0.0D,
          11 / 16.0D, 15 / 16.0D, 1.0D);

  /**
   * Constructs a {@link BlockJoistGirder}.
   *
   * @since 1.0
   */
  public BlockJoistGirder() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1.5F,
        8F);
  }

  @Override
  public String getBlockRegistryName() {
    return "joist_girder";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
