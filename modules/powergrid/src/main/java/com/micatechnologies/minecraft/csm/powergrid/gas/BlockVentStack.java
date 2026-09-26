package com.micatechnologies.minecraft.csm.powergrid.gas;

import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockWaterPipe;
import com.micatechnologies.minecraft.csm.powergrid.water.IWaterPipeJoint;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The top of a gas regulator station's relief vent stack: the last length of vent pipe with its
 * bird screen and the hinged rain cap that the vented gas blows open. It stands on a vertical run
 * of gas pipe, which joins it from below, so a stack is built to any height out of pipe; the cap
 * hinges on the side away from the player who placed it.
 *
 * @since 2026.9
 */
public class BlockVentStack extends BlockUtilityFixture implements IWaterPipeJoint {

  /**
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockVentStack(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  public boolean joinsWaterPipe(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side) {
    return side == EnumFacing.DOWN;
  }

  @Override
  public String pipeService() {
    return BlockWaterPipe.GAS;
  }

  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
