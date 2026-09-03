package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Reusable transit card. Each card is unique (stack size 1) and stores its trip balance in
 * NBT. Purchased and reloaded at a {@link BlockFareVendingMachine}; consumed one trip at a
 * time by the fare gate when used to open it.
 *
 * <p>The card's tooltip surfaces the current balance so players don't have to remember which
 * card has what. Stack size is 1 because mixing two cards into one stack would collapse
 * their independent balances.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ItemTransitCard extends AbstractItem {

  /** NBT tag holding the trip balance (int). */
  public static final String NBT_BALANCE = "trips";

  public ItemTransitCard() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "transitcard";
  }

  /** Reads the trip balance from a card stack. Treats missing NBT or wrong-item as 0. */
  public static int getBalance(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemTransitCard)) {
      return 0;
    }
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null || !tag.hasKey(NBT_BALANCE)) {
      return 0;
    }
    return Math.max(0, tag.getInteger(NBT_BALANCE));
  }

  /**
   * Sets the trip balance on a card stack, clamping at zero (no negative balances). Creates
   * the NBT compound if one didn't exist yet.
   */
  public static void setBalance(ItemStack stack, int trips) {
    if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemTransitCard)) {
      return;
    }
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null) {
      tag = new NBTTagCompound();
      stack.setTagCompound(tag);
    }
    tag.setInteger(NBT_BALANCE, Math.max(0, trips));
  }

  /**
   * Builds a new transit card stack with the given starting balance. Convenience for the
   * vending-machine purchase flow.
   */
  public static ItemStack newCard(ItemTransitCard item, int startingBalance) {
    ItemStack stack = new ItemStack(item, 1);
    setBalance(stack, startingBalance);
    return stack;
  }

  /**
   * Deducts one trip from the card. Returns true if a trip was successfully consumed,
   * false if the card had zero balance.
   */
  public static boolean consumeTrip(ItemStack stack) {
    int balance = getBalance(stack);
    if (balance <= 0) {
      return false;
    }
    setBalance(stack, balance - 1);
    return true;
  }

  @Override
  public void addInformation(ItemStack stack, World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    int balance = getBalance(stack);
    tooltip.add("§bBalance: §f" + balance + (balance == 1 ? " trip" : " trips"));
    tooltip.add("§7Reload at a Fare Vending Machine");
  }

  @Override
  public boolean hasEffect(ItemStack stack) {
    // Loaded cards get the vanilla enchant shimmer so they stand out visually from a card
    // that's been drained dry. Cheap differentiation without needing a second texture.
    return getBalance(stack) > 0;
  }
}
