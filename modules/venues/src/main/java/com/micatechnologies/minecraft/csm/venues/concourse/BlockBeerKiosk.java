package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The portable beer kiosk ({@code beer_kiosk}): a rolling bar two blocks wide and three tall, a
 * tap tower of four handles on its counter, a price board behind and a lit COLD BEER canopy
 * on two posts. A {@link BlockVenueUnit} drawn whole by its master, the left cell of its bottom
 * layer (looking at its front); metadata is the facing, towards the customers. A click anywhere
 * on it pours (a sound; nothing is handed out).
 *
 * @since 2026.10
 */
public class BlockBeerKiosk extends BlockVenueUnit {

  /**
   * Constructs the kiosk.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockBeerKiosk(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.8F,
          0.8F + world.rand.nextFloat() * 0.2F);
    }
    return true;
  }
}
