package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.IItemHandler;

/**
 * Buying back from a reselling pawn counter, at an item's value times {@code resaleMarkup}
 * (always above what the counter paid for it, so a player can never buy back and pawn again at
 * a profit). The price goes nowhere: a pawn counter is the server's, and keeps no till.
 *
 * <ol>
 *   <li>Is the counter reselling, and is the slot stocked?</li>
 *   <li>Charge the price (money, or whole emeralds rounded up). Nothing moves if it is refused.
 *   </li>
 *   <li>Take the items from the stock and give them, dropping what does not fit.</li>
 * </ol>
 *
 * @since 2026.10
 */
public final class PawnResale {

  private PawnResale() {
  }

  /** What one of the stack's items costs, rounded up to a cent. */
  static double priceEach(ItemStack stack) {
    ItemStack one = stack.copy();
    one.setCount(1);
    PawnValuation.Quote q = PawnValuation.quote(one);
    double value = q.isAccepted() ? q.valueEach : CsmConfig.getPawnDefaultValue();
    return Math.ceil(Math.max(0.01, value * CsmConfig.getPawnResaleMarkup()) * 100 - 1e-6)
        / 100.0;
  }

  /** Buys one item from {@code slot}, or the whole stack. */
  static void buy(EntityPlayerMP player, TileEntityPawnCounter counter, int slot, boolean whole) {
    if (!counter.isResale() || slot < 0 || slot >= ContainerPawnResale.STOCK_SLOTS) {
      return;
    }
    IItemHandler stock = counter.getItems();
    ItemStack there = stock.getStackInSlot(slot);
    if (there.isEmpty()) {
      return;
    }
    int count = whole ? there.getCount() : 1;
    double price = priceEach(there) * count;
    ITextComponent refused = PawnEconomy.charge(player, price, "Pawn shop resale");
    if (refused != null) {
      player.sendMessage(refused);
      return;
    }
    ItemStack bought = stock.extractItem(slot, count, false);
    if (!player.inventory.addItemStackToInventory(bought)) {
      player.dropItem(bought, false);
    }
    counter.markDirty();
    ITextComponent done = new TextComponentTranslation("csm.pawn.msg.bought",
        bought.getCount() > 1 ? new TextComponentTranslation("csm.pawn.items", count)
            : new TextComponentTranslation("csm.pawn.item_one"),
        PawnEconomy.describePrice(price));
    done.getStyle().setColor(TextFormatting.GREEN);
    player.sendMessage(done);
    Container open = player.openContainer;
    if (open instanceof ContainerPawnResale) {
      ((ContainerPawnResale) open).requote();
    }
  }
}
