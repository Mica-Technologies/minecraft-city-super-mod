package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The bathroom vanity: a cabinet under a stone vanity top with a porcelain basin and a tap in
 * every block. Vanities of one finish side by side join like kitchen base cabinets
 * ({@link KitchenLine#VANITY}, a line of their own, so a vanity never runs on into a kitchen):
 * one vanity top across them, end panels only where the run stops. Right-click with an empty
 * bucket or a glass bottle to fill it at the tap ({@link IWaterTap}); with anything else the
 * doors open on nine slots.
 *
 * @since 2026.9
 */
public class BlockBathroomVanity extends BlockKitchenCabinet implements IWaterTap {

  private static final int[] BOX = {0, 0, 3, 16, 15, 16};

  /**
   * Constructs a vanity.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockBathroomVanity(String registryName) {
    super(registryName, BOX, KitchenLine.VANITY, 9, KitchenFront.DOORS);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    return IWaterTap.fillAtTap(world, pos, player, hand)
        || super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
  }
}
