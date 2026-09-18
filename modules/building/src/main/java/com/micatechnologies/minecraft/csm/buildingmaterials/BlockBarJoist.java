package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * An open-web steel bar joist: two chords with a bracing web zigzagging between them.\n *\n * <p>What holds up the roof of nearly every single-storey commercial building, and the\n * reason those roofs are full of exposed steel. Seen through between the chords.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockBarJoist extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(6 / 16.0D, 4 / 16.0D, 0.0D,
          10 / 16.0D, 14 / 16.0D, 1.0D);

  /**
   * Constructs a {@link BlockBarJoist}.
   *
   * @since 1.0
   */
  public BlockBarJoist() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1.5F,
        8F);
  }

  @Override
  public String getBlockRegistryName() {
    return "bar_joist";
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
