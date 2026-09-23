package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The SCBA fill station, which also recharges fire extinguishers: use an
 * {@link ItemFireExtinguisher} on it and it comes back full.
 *
 * @since 2026.9
 */
public class BlockScbaFillStation extends BlockFireProtectionProp {

  public BlockScbaFillStation(String registryName, int[] box) {
    super(registryName, box, true);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (!(held.getItem() instanceof ItemFireExtinguisher)) {
      return false;
    }
    if (!world.isRemote) {
      ItemFireExtinguisher.refill(held);
      world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.6F,
          0.6F);
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.extinguisher.refilled"));
    }
    return true;
  }
}
