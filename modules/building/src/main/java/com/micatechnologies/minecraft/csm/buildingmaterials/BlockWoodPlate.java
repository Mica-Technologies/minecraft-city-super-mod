package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A sole plate on the floor: a wood wall that has been set out but not yet stood up.
 *
 * <p>The plate is nailed down first and the studs go up off it, so this is what the first day of a
 * timber partition looks like. The wood counterpart of {@link BlockSteelTrack}, and it joins the
 * whole family, so a run can be part laid out and part built.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodPlate extends AbstractBlockWoodFraming {

  /** The plate is one sixteenth deep, which is what it is walked over as well as drawn as. */
  private static final AxisAlignedBB PLATE_BOX =
      new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D / 16.0D, 1.0D);

  @Override
  public String getBlockRegistryName() {
    return "wood_plate";
  }

  /**
   * A flat box the height of the plate itself, so a wall that has merely been set out is still a
   * floor that can be walked across.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return PLATE_BOX;
  }
}
