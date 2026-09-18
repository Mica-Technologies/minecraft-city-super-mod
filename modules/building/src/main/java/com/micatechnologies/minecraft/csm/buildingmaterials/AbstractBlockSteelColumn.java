package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A wide-flange steel column: two flanges either side of a web, standing the height of
 * its block.
 *
 * <p>The shape nearly every column in a steel building actually is. Its axis says which
 * way the flanges face rather than which way it spans, which is the one thing a column
 * means differently from a beam.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelColumn extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0D, 0 / 16.0D, 5 / 16.0D,
          12 / 16.0D, 16 / 16.0D, 11 / 16.0D);

  /**
   * Constructs an {@link AbstractBlockSteelColumn}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelColumn() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
