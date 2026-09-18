package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A column landing on its base plate, held down by four anchor bolts.
 *
 * <p>Where a steel frame meets its foundation, and the first thing erected on a site
 * once the concrete has cured.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelBasePlate extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(2 / 16.0D, 0 / 16.0D, 2 / 16.0D,
          14 / 16.0D, 16 / 16.0D, 14 / 16.0D);

  /**
   * Constructs an {@link AbstractBlockSteelBasePlate}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelBasePlate() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
