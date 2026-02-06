package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFireAlarmSprinklerWhite extends AbstractBlockFireAlarmDetector {

  @Override
  public String getBlockRegistryName() {
    return "firealarmsprinklerwhite";
  }

  @Override
  public void onFire(World world, BlockPos blockPos, IBlockState blockState) {
    int waterX = blockPos.getX();
    int waterY = blockPos.getY() - 1;
    int waterZ = blockPos.getZ();
    BlockPos waterBlockPos = new BlockPos(waterX, waterY, waterZ);
    IBlockState previousBlockState = world.getBlockState(waterBlockPos);
    world.setBlockState(waterBlockPos, Blocks.FLOWING_WATER.getDefaultState(), 3);
    world.notifyBlockUpdate(waterBlockPos, previousBlockState,
        Blocks.FLOWING_WATER.getDefaultState(), 3);
    world.notifyNeighborsOfStateChange(waterBlockPos, Blocks.FLOWING_WATER, true);
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
    return new AxisAlignedBB(0.500000, 0.437500, 0.890625, 0.593750, 0.562500, 1.000000);
  }
}
