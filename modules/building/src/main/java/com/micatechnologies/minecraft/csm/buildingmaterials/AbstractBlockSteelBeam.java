package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A wide-flange steel beam: the same section laid down, flanges top and bottom.
 *
 * <p>Spans the block, so a row of them makes a girder line.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelBeam extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0D, 3 / 16.0D, 0 / 16.0D,
          12 / 16.0D, 13 / 16.0D, 16 / 16.0D);

  /**
   * Constructs an {@link AbstractBlockSteelBeam}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelBeam() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
