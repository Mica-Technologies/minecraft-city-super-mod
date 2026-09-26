package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * An inline pipe fitting: a gate, butterfly or check valve, a magnetic flow meter, an air
 * release valve on its tee. It carries the pipe through along the way the player was looking
 * when placing it (it faces the player, and its model's pipe runs front to back), and a
 * {@link BlockWaterPipe} joins it at either end.
 *
 * @since 2026.9
 */
public class BlockPipeFitting extends BlockUtilityFixture implements IWaterPipeJoint {

  public BlockPipeFitting(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  public boolean joinsWaterPipe(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side) {
    return side.getAxis() == state.getValue(FACING).getAxis();
  }

  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
