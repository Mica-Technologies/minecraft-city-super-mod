package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Steel X-bracing: two flats crossing corner to corner of the block.
 *
 * <p>What keeps a steel frame from racking. Takes a full cube bounding box, as the
 * rafter does, because a diagonal is not something an axis-aligned box can describe.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelBraceX extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = SQUARE_BOUNDING_BOX;

  /**
   * Constructs an {@link AbstractBlockSteelBraceX}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelBraceX() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
