package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A block with a working tap: the kitchen sink base, the bathroom vanity, the pedestal sink, the
 * laundry tub and the bathtub. An empty bucket or a glass bottle held to it is filled with water
 * ({@link #fillAtTap}), and an appliance that uses water (the dishwasher, the coffee machine,
 * the washing machine) standing beside one is plumbed in and never runs dry
 * ({@link KitchenAppliances#nextToSink}).
 *
 * @since 2026.9
 */
public interface IWaterTap {

  /**
   * Fills the empty bucket or glass bottle in the player's hand at the tap at {@code pos}, with
   * the vanilla fill sound.
   *
   * @param world  the world
   * @param pos    the tap's block
   * @param player the player
   * @param hand   the hand clicked with
   *
   * @return true if the hand held a bucket or a bottle (the click is used), false for anything
   *     else, which the block handles as it likes
   */
  static boolean fillAtTap(World world, BlockPos pos, EntityPlayer player, EnumHand hand) {
    ItemStack held = player.getHeldItem(hand);
    Item item = held.getItem();
    if (item != Items.BUCKET && item != Items.GLASS_BOTTLE) {
      return false;
    }
    if (!world.isRemote) {
      boolean bucket = item == Items.BUCKET;
      ItemStack filled = bucket ? new ItemStack(Items.WATER_BUCKET)
          : PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
      give(player, hand, held, filled);
      SoundEvent sound = bucket ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BOTTLE_FILL;
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
    return true;
  }

  /**
   * Swaps one of {@code held} for {@code result}: into the hand if that was the last one, else
   * into the inventory or, failing that, dropped. A creative player keeps what they held.
   *
   * @param player the player
   * @param hand   the hand
   * @param held   what the hand holds
   * @param result what it becomes
   */
  static void give(EntityPlayer player, EnumHand hand, ItemStack held, ItemStack result) {
    if (!player.capabilities.isCreativeMode) {
      held.shrink(1);
    }
    if (held.isEmpty()) {
      player.setHeldItem(hand, result);
    } else if (!player.inventory.addItemStackToInventory(result)) {
      player.dropItem(result, false);
    }
  }
}
