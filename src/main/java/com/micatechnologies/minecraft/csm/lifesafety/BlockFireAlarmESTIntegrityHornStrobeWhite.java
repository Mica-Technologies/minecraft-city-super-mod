package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockFireAlarmESTIntegrityHornStrobeWhite extends AbstractBlockFireAlarmSounder {

  @Override
  public String getBlockRegistryName() {
    return "firealarmestintegrityhornstrobewhite";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:est_integrity";
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    return 70;
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
    return new AxisAlignedBB(0.187500, 0.312500, 0.687500, 0.875000, 1.000000, 1.000000);
  }
}
