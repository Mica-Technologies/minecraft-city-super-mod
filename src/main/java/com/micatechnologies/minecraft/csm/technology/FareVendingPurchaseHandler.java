package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link FareVendingPurchasePacket}. Validates that:
 * <ul>
 *   <li>The packet's purchase ordinal is in range</li>
 *   <li>The player is still within ~6 blocks of the vending machine (anti-cheat against
 *       packet replay from a distance)</li>
 *   <li>The block at the vending pos is still a {@link BlockFareVendingMachine}</li>
 *   <li>The player has enough emeralds for the selected purchase</li>
 *   <li>For RELOAD purchases, the player is holding an {@link ItemTransitCard}</li>
 * </ul>
 *
 * <p>On success: deducts the emeralds, then either gives the player a fare ticket, gives
 * them a new transit card with the requested balance, or increments the held card's
 * balance.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class FareVendingPurchaseHandler
    implements IMessageHandler<FareVendingPurchasePacket, IMessage> {

  /** Maximum squared distance the player may be from the vending machine when purchasing. */
  private static final double MAX_INTERACT_DIST_SQ = 36.0; // 6 blocks

  @Override
  public IMessage onMessage(FareVendingPurchasePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> handle(message, player));
    return null;
  }

  private static void handle(FareVendingPurchasePacket message, EntityPlayerMP player) {
    FareVendingPurchase purchase = FareVendingPurchase.fromOrdinal(message.getPurchaseOrdinal());
    if (purchase == null) {
      return;
    }
    World world = player.world;
    if (player.getDistanceSq(message.getVendingPos()) > MAX_INTERACT_DIST_SQ) {
      return;
    }
    if (!(world.getBlockState(message.getVendingPos()).getBlock()
        instanceof BlockFareVendingMachine)) {
      return;
    }

    InventoryPlayer inv = player.inventory;
    ItemStack heldCard = ItemStack.EMPTY;
    if (purchase.kind == FareVendingPurchase.Kind.RELOAD) {
      ItemStack mainHand = player.getHeldItemMainhand();
      if (mainHand.getItem() instanceof ItemTransitCard) {
        heldCard = mainHand;
      } else {
        sendError(player, "You need to be holding a Transit Card in your main hand to reload.");
        return;
      }
    }

    if (countEmeralds(inv) < purchase.costEmeralds) {
      sendError(player, "Not enough emeralds — need " + purchase.costEmeralds + ".");
      return;
    }

    // Build the resulting item *before* deducting payment, so a failure to construct it
    // (shouldn't ever happen, but still) doesn't strand the player having paid for nothing.
    ItemStack resultStack = ItemStack.EMPTY;
    switch (purchase.kind) {
      case TICKET: {
        Item ticket = CsmRegistry.getItem("csm:fareticket");
        if (!(ticket instanceof ItemFareTicket)) {
          return;
        }
        resultStack = new ItemStack(ticket, 1);
        break;
      }
      case NEW_CARD: {
        Item card = CsmRegistry.getItem("csm:transitcard");
        if (!(card instanceof ItemTransitCard)) {
          return;
        }
        resultStack = ItemTransitCard.newCard((ItemTransitCard) card, purchase.trips);
        break;
      }
      case RELOAD:
        // Mutates the held stack in-place below.
        break;
    }

    deductEmeralds(inv, purchase.costEmeralds);

    if (purchase.kind == FareVendingPurchase.Kind.RELOAD) {
      int newBalance = ItemTransitCard.getBalance(heldCard) + purchase.trips;
      ItemTransitCard.setBalance(heldCard, newBalance);
      sendInfo(player, "Card reloaded — new balance: " + newBalance + " trips.");
    } else {
      // Hand the player the new item; if their inventory is full, drop it at their feet.
      if (!inv.addItemStackToInventory(resultStack)) {
        player.dropItem(resultStack, false);
      }
      String summary = purchase.kind == FareVendingPurchase.Kind.TICKET
          ? "Issued single-use fare ticket."
          : "Issued transit card with " + purchase.trips + " trips.";
      sendInfo(player, summary);
    }

    // Audible feedback at the machine: reuse the existing transaction-approved chime so
    // the vending machine and the Verifone terminal feel like they're part of the same
    // payment ecosystem.
    SoundEvent chime =
        com.micatechnologies.minecraft.csm.CsmSounds.SOUND.VERIFONE_MX915.getSoundEvent();
    if (chime != null) {
      world.playSound(null,
          message.getVendingPos().getX() + 0.5,
          message.getVendingPos().getY() + 1.0,
          message.getVendingPos().getZ() + 0.5,
          chime, SoundCategory.BLOCKS, 0.7F, 1.0F);
    }

    // Refresh the inventory on the client so emerald counts update visibly.
    player.inventoryContainer.detectAndSendChanges();
  }

  /** Counts emeralds held across the player's main inventory + hotbar (not armor/offhand). */
  private static int countEmeralds(InventoryPlayer inv) {
    int total = 0;
    for (int i = 0; i < inv.mainInventory.size(); i++) {
      ItemStack s = inv.mainInventory.get(i);
      if (s.getItem() == Items.EMERALD) {
        total += s.getCount();
      }
    }
    return total;
  }

  /**
   * Removes {@code amount} emeralds from the player's main inventory. Caller must verify
   * the player has enough first via {@link #countEmeralds}.
   */
  private static void deductEmeralds(InventoryPlayer inv, int amount) {
    int remaining = amount;
    for (int i = 0; i < inv.mainInventory.size() && remaining > 0; i++) {
      ItemStack s = inv.mainInventory.get(i);
      if (s.getItem() == Items.EMERALD) {
        int take = Math.min(s.getCount(), remaining);
        s.shrink(take);
        remaining -= take;
        if (s.isEmpty()) {
          inv.mainInventory.set(i, ItemStack.EMPTY);
        }
      }
    }
  }

  private static void sendInfo(EntityPlayerMP player, String message) {
    player.sendMessage(new TextComponentString("§a" + message));
  }

  private static void sendError(EntityPlayerMP player, String message) {
    player.sendMessage(new TextComponentString("§c" + message));
  }
}
