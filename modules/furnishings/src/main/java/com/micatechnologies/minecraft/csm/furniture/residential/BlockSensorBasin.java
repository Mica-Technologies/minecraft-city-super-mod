package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A basin with a sensor faucet: the commercial restroom's wall-hung lavatory. A bucket or a
 * bottle is filled at the tap ({@link IWaterTap}); an empty hand under the spout runs the
 * water for a moment ({@link SensorFaucet}).
 *
 * @since 2026.9
 */
public class BlockSensorBasin extends BlockBasin {

  private final double[] spout;

  /**
   * Constructs a basin with a sensor faucet.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   * @param spout        the spout's outlet, {x, y, z} facing north in sixteenths
   */
  public BlockSensorBasin(String registryName, int[] box, FixtureMaterial material,
      double[] spout) {
    super(registryName, box, material);
    this.spout = spout.clone();
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (IWaterTap.fillAtTap(world, pos, player, hand)) {
      return true;
    }
    if (player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    SensorFaucet.run(world, pos, state.getValue(FACING), spout);
    return true;
  }
}
