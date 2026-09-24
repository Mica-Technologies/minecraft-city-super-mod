package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
public class BlockKitchenSink extends BlockKitchenCabinet {

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
    ItemStack held = player.getHeldItem(hand);
    Item item = held.getItem();
    if (item != Items.BUCKET && item != Items.GLASS_BOTTLE) {
      return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
    if (!world.isRemote) {
      boolean bucket = item == Items.BUCKET;
      ItemStack filled = bucket ? new ItemStack(Items.WATER_BUCKET)
          : PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
      if (!player.capabilities.isCreativeMode) {
        held.shrink(1);
      }
      if (held.isEmpty()) {
        player.setHeldItem(hand, filled);
      } else if (!player.inventory.addItemStackToInventory(filled)) {
        player.dropItem(filled, false);
      }
      SoundEvent sound = bucket ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BOTTLE_FILL;
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
    return true;
  }
}
