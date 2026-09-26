package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * An inline pipe fitting: a gate, butterfly or check valve, a magnetic flow meter, an air
 * release valve on its tee; in the gas yard a ball valve, a pressure regulator, a turbine meter. It carries the pipe through along the way the player was looking
 * when placing it (it faces the player, and its model's pipe runs front to back), and a
 * {@link BlockWaterPipe} joins it at either end.
 *
 * @since 2026.9
 */
public class BlockPipeFitting extends BlockUtilityFixture implements IWaterPipeJoint {

  private final String service;

  public BlockPipeFitting(String registryName, double[] box) {
    this(registryName, box, BlockWaterPipe.WATER);
  }

  /**
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param service      the service of the pipe it joins ({@link BlockWaterPipe#GAS} for the
   *                     gas yard's valves, regulators and meter)
   */
  public BlockPipeFitting(String registryName, double[] box, String service) {
    super(registryName, box);
    this.service = service;
  }

  @Override
  public String pipeService() {
    return service;
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
