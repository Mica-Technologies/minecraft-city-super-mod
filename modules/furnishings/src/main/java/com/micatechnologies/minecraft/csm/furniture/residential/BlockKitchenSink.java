package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The sink base: a base cabinet with a stainless bowl in its countertop and a tap behind it. It
 * joins a countertop run like any base cabinet. Right-click with an empty bucket or a glass
 * bottle to fill it at the tap, as at the water dispenser; with anything else the doors under
 * the sink open on nine slots.
 *
 * @since 2026.9
 */
public class BlockKitchenSink extends BlockKitchenCabinet implements IWaterTap {

  private static final int[] BOX = {0, 0, 0, 16, 15, 16};

  /**
   * Constructs a sink base.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockKitchenSink(String registryName) {
    super(registryName, BOX, KitchenLine.BASE, 9, KitchenFront.DOORS);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    return IWaterTap.fillAtTap(world, pos, player, hand)
        || super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
  }
}
