package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A bolted connection: a gusset plate on the column flange, with its bolt group.
 *
 * <p>The plate sits just outside the flange rather than in it, so no two surfaces share
 * a plane.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelConnection extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0D, 0 / 16.0D, 5 / 16.0D,
          12 / 16.0D, 16 / 16.0D, 13 / 16.0D);

  /**
   * Constructs an {@link AbstractBlockSteelConnection}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelConnection() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }
}
