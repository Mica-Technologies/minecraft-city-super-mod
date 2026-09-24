package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A basin with a working tap that stands on its own: the pedestal sink and the laundry tub.
 * Right-click with an empty bucket or a glass bottle to fill it at the tap
 * ({@link IWaterTap}); a washing machine beside one is plumbed in.
 *
 * @since 2026.9
 */
public class BlockBasin extends BlockBathroomFixture implements IWaterTap {

  /**
   * Constructs a basin.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   */
  public BlockBasin(String registryName, int[] box, FixtureMaterial material) {
    super(registryName, box, material);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    return IWaterTap.fillAtTap(world, pos, player, hand);
  }
}
