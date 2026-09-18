package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Steel track laid on the floor: a wall that has been set out but not yet stood up.
 *
 * <p>The runner is screwed down first and the studs go into it afterwards, so this is what the
 * first day of a partition looks like. It joins the rest of the steel family, which means a run
 * can be half laid out and half built and still read as one wall.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSteelTrack extends AbstractBlockSteelFraming {

  /** The track is one sixteenth deep, which is what it is walked over as well as drawn as. */
  private static final AxisAlignedBB TRACK_BOX =
      new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D / 16.0D, 1.0D);

  @Override
  public String getBlockRegistryName() {
    return "steel_track";
  }

  /**
   * A flat box the height of the track itself. The family's default full cube would make a laid
   * out wall a wall already — you could not walk across the floor it has just been set out on.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return TRACK_BOX;
  }
}
