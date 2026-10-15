package com.micatechnologies.minecraft.csm.codeutils.economy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * Emeralds as CSM's fallback currency, when SUM's economy ({@link CsmSumEconomy}) is not there:
 * counting, taking and giving the loose emeralds in a player's main inventory. Server side only.
 *
 * @since 2026.10
 */
public final class CsmEmeralds {

  private CsmEmeralds() {
  }

  /** The emeralds in the player's main inventory. */
  public static int count(EntityPlayer player) {
    int count = 0;
    for (ItemStack stack : player.inventory.mainInventory) {
      if (stack.getItem() == Items.EMERALD) {
        count += stack.getCount();
      }
    }
    return count;
  }

  /**
   * Takes {@code count} emeralds from the player's main inventory, all or none.
   *
   * @return whether they were taken
   */
  public static boolean take(EntityPlayer player, int count) {
    if (count <= 0) {
      return true;
    }
    if (count(player) < count) {
      return false;
    }
    int left = count;
    for (ItemStack stack : player.inventory.mainInventory) {
      if (left <= 0) {
        break;
      }
      if (stack.getItem() == Items.EMERALD) {
        int take = Math.min(left, stack.getCount());
        stack.shrink(take);
        left -= take;
      }
    }
    player.inventory.markDirty();
    return true;
  }

  /** Gives the player {@code count} emeralds, dropping what does not fit at their feet. */
  public static void give(EntityPlayer player, long count) {
    long left = count;
    while (left > 0) {
      int n = (int) Math.min(64, left);
      ItemStack stack = new ItemStack(Items.EMERALD, n);
      if (!player.inventory.addItemStackToInventory(stack)) {
        player.dropItem(stack, false);
      }
      left -= n;
    }
    player.inventory.markDirty();
  }

  /** "1 emerald", "5 emeralds". */
  public static String format(long count) {
    return count + (count == 1 ? " emerald" : " emeralds");
  }
}
