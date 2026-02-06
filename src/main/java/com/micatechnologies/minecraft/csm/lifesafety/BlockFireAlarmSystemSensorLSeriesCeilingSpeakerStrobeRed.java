package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeRed extends
    AbstractBlockFireAlarmSounderVoiceEvac {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsystemsensorlseriesceilingspeakerstrobered";
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
    return new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000);
  }
}
