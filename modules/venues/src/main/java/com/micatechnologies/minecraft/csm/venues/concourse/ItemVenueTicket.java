package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A paper event ticket ({@code venue_ticket}), ADMIT ONE. The ticket scanner (and a ticketed
 * turnstile's own reader) takes one, marks it scanned (NBT {@code scanned}) and lets one person
 * through; a scanned ticket is a stub and opens nothing. Unscanned tickets stack; scanning one
 * from a stack splits it off. Crafted four at a time from paper and a dye.
 *
 * @since 2026.10
 */
public class ItemVenueTicket extends AbstractItem {

  private static final String KEY_SCANNED = "scanned";

  public ItemVenueTicket() {
    super(0, 16);
  }

  @Override
  public String getItemRegistryName() {
    return "venue_ticket";
  }

  /** Whether the stack is a ticket that has not been scanned yet. */
  public static boolean isUnscanned(ItemStack stack) {
    return stack.getItem() instanceof ItemVenueTicket && !isScanned(stack);
  }

  /** Whether the stack is a scanned ticket stub. */
  public static boolean isScanned(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null && tag.getBoolean(KEY_SCANNED);
  }

  /**
   * Scans one ticket from the stack in the player's hand: a lone ticket becomes a stub; one
   * from a stack is split off as a stub and given to the player.
   *
   * @param player the player
   * @param hand   the hand holding the tickets
   */
  public static void scanOne(EntityPlayer player, EnumHand hand) {
    ItemStack held = player.getHeldItem(hand);
    if (player.capabilities.isCreativeMode) {
      return;    // a creative player's tickets are never used up
    }
    ItemStack stub = held.splitStack(1);
    NBTTagCompound tag = stub.hasTagCompound() ? stub.getTagCompound() : new NBTTagCompound();
    tag.setBoolean(KEY_SCANNED, true);
    stub.setTagCompound(tag);
    if (held.isEmpty()) {
      player.setHeldItem(hand, stub);
    } else if (!player.inventory.addItemStackToInventory(stub)) {
      player.dropItem(stub, false);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add("§7" + I18n.format("csm.venues.ticket.admit"));
    if (isScanned(stack)) {
      tooltip.add("§c" + I18n.format("csm.venues.ticket.scanned"));
    } else {
      tooltip.add("§a" + I18n.format("csm.venues.ticket.valid"));
    }
  }
}
