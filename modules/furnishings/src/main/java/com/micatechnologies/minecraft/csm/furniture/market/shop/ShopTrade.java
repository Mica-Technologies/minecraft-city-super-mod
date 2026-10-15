package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.codeutils.economy.CsmEmeralds;
import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Locale;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Trading with a shop, on the server: a customer buying its item ({@link #buy}), a customer
 * selling it back ({@link #sell}), and its owner emptying or topping up the till
 * ({@link #withdraw}, {@link #deposit}).
 *
 * <p>Each trade checks everything it can before anything moves, then takes the payment, then
 * moves the goods; if the goods then cannot move after all, the payment goes back. Money moves
 * through SUM's wallet ({@link ShopEconomy#SUM}), which only reaches players who are online, so a
 * player shop's takings wait in its till until the owner collects them at the shop. A server
 * shop keeps no till: what it is paid is destroyed and what it pays is made, as SUM's own server
 * shop does.</p>
 *
 * <p>Every outcome is told to the player in the action bar and their screen is refreshed
 * ({@link ShopStatusPacket}).</p>
 *
 * @since 2026.10
 */
public final class ShopTrade {

  private ShopTrade() {
  }

  /** The customer buys one sale's worth of the shop's item. */
  public static void buy(EntityPlayerMP player, TileEntityShop shop) {
    if (!open(player, shop)) {
      return;
    }
    if (!shop.isServerShop() && shop.isOwner(player)) {
      tell(player, "csm.shop.msg.own_shop");
      return;
    }
    boolean money = shop.isMoneyMode();
    double price = shop.getSellPrice(money);
    int amount = shop.getSellAmount();
    if (!shop.hasStock(amount)) {
      tell(player, "csm.shop.msg.out_of_stock");
      return;
    }
    // Payment first.
    if (money) {
      String refused = ShopEconomy.SUM.spend(player, price, reason("Bought at", shop));
      if (refused != null) {
        player.sendStatusMessage(new TextComponentString(refused), true);
        return;
      }
    } else if (!CsmEmeralds.take(player, (int) price)) {
      tell(player, "csm.shop.msg.need_emeralds", price(shop, money, price));
      return;
    }
    // Then the goods.
    int taken = shop.takeStock(amount);
    if (taken < amount) {
      if (taken > 0) {
        shop.putStock(copy(shop, taken), false);
      }
      refund(player, money, price, shop);
      tell(player, "csm.shop.msg.out_of_stock");
      return;
    }
    if (!shop.isServerShop()) {
      shop.addTill(money, price);
    }
    give(player, shop, amount);
    sound(player, shop.getPos(), FurnishingsSounds.REGISTER_DRAWER);
    tell(player, "csm.shop.msg.bought", amount, shop.getTemplate().getDisplayName(),
        price(shop, money, price));
    shop.sync();
    ShopStatusPacket.send(player, shop);
  }

  /** The customer sells one buy-back's worth of the shop's item to it. */
  public static void sell(EntityPlayerMP player, TileEntityShop shop) {
    if (!open(player, shop)) {
      return;
    }
    if (!shop.isServerShop() && shop.isOwner(player)) {
      tell(player, "csm.shop.msg.own_shop");
      return;
    }
    boolean money = shop.isMoneyMode();
    double price = shop.getBuyPrice(money);
    int amount = shop.getBuyAmount();
    if (price <= 0) {
      tell(player, "csm.shop.msg.not_buying");
      return;
    }
    if (countHeld(player, shop) < amount) {
      tell(player, "csm.shop.msg.need_items", amount, shop.getTemplate().getDisplayName());
      return;
    }
    // Unlimited stock needs nothing more; anything else must have room for what it buys.
    boolean keep = !shop.isInfinite();
    if (keep && !shop.putStock(copy(shop, amount), true).isEmpty()) {
      tell(player, "csm.shop.msg.shop_full");
      return;
    }
    if (!shop.isServerShop() && shop.getTill(money) + 1e-9 < price) {
      tell(player, "csm.shop.msg.shop_cannot_pay");
      return;
    }
    // Goods first: the customer cannot be paid for items they no longer hold.
    takeHeld(player, shop, amount);
    if (money) {
      String refused = ShopEconomy.SUM.credit(player, price, reason("Sold at", shop));
      if (refused != null) {
        ItemHandlerHelper.giveItemToPlayer(player, copy(shop, amount));
        player.sendStatusMessage(new TextComponentString(refused), true);
        return;
      }
    } else {
      CsmEmeralds.give(player, (long) price);
    }
    if (!shop.isServerShop()) {
      shop.addTill(money, -price);
    }
    if (keep) {
      ItemStack rest = shop.putStock(copy(shop, amount), false);
      if (!rest.isEmpty()) {
        // Room was checked a moment ago on this thread, so this should never happen; never
        // destroy what does not fit.
        ItemHandlerHelper.giveItemToPlayer(player, rest);
        CsmFurnishings.getLogger().warn("A shop at {} could not keep {} it bought",
            shop.getPos(), rest);
      }
    }
    sound(player, shop.getPos(), FurnishingsSounds.REGISTER_DRAWER);
    tell(player, "csm.shop.msg.sold", amount, shop.getTemplate().getDisplayName(),
        price(shop, money, price));
    shop.sync();
    ShopStatusPacket.send(player, shop);
  }

  /**
   * The owner empties the till: emeralds always, money while the economy is there. Only the
   * owner: an admin can manage anyone's shop, but never collects its takings.
   */
  public static void withdraw(EntityPlayerMP player, TileEntityShop shop) {
    if (!shop.isOwner(player) || shop.isServerShop()) {
      if (!shop.isServerShop()) {
        tell(player, "csm.shop.msg.owner_collects");
      }
      return;
    }
    boolean any = false;
    long emeralds = shop.getTillEmeralds();
    if (emeralds > 0) {
      CsmEmeralds.give(player, emeralds);
      shop.addTill(false, -emeralds);
      tell(player, "csm.shop.msg.collected", CsmEmeralds.format(emeralds));
      any = true;
    }
    double cash = shop.getTillMoney();
    if (cash > 0) {
      any = true;
      if (!ShopEconomy.isMoneyMode()) {
        tell(player, "csm.shop.msg.money_waits", ShopEconomy.format(true, shop.getSymbol(),
            cash));
      } else {
        String refused = ShopEconomy.SUM.credit(player, cash, reason("Takings at", shop));
        if (refused != null) {
          player.sendStatusMessage(new TextComponentString(refused), true);
        } else {
          shop.addTill(true, -cash);
          tell(player, "csm.shop.msg.collected", ShopEconomy.format(true, shop.getSymbol(),
              cash));
        }
      }
    }
    if (!any) {
      tell(player, "csm.shop.msg.till_empty");
    }
    ShopStatusPacket.send(player, shop);
  }

  /** The owner puts {@code amount} into the till, to pay for buy-backs. */
  public static void deposit(EntityPlayerMP player, TileEntityShop shop, double amount) {
    if (!ShopPermissions.mayManage(player, shop) || shop.isServerShop()
        || !(amount > 0) || Double.isInfinite(amount)) {
      return;
    }
    boolean money = shop.isMoneyMode();
    double value = money ? Math.round(amount * 100.0) / 100.0 : Math.floor(amount);
    if (value <= 0) {
      return;
    }
    if (money) {
      String refused = ShopEconomy.SUM.spend(player, value, reason("Float for", shop));
      if (refused != null) {
        player.sendStatusMessage(new TextComponentString(refused), true);
        return;
      }
    } else if (value > Integer.MAX_VALUE || !CsmEmeralds.take(player, (int) value)) {
      tell(player, "csm.shop.msg.need_emeralds", price(shop, false, value));
      return;
    }
    shop.addTill(money, value);
    tell(player, "csm.shop.msg.deposited", price(shop, money, value));
    ShopStatusPacket.send(player, shop);
  }

  // --- helpers -------------------------------------------------------------------------

  /** Whether the shop trades at all; tells the player why not. */
  private static boolean open(EntityPlayerMP player, TileEntityShop shop) {
    if (shop.isDecorative()) {
      tell(player, "csm.shop.msg.decorative");
      return false;
    }
    if (!shop.isSetUp()) {
      tell(player, "csm.shop.msg.not_open");
      return false;
    }
    return true;
  }

  private static void refund(EntityPlayerMP player, boolean money, double price,
      TileEntityShop shop) {
    if (money) {
      String refused = ShopEconomy.SUM.credit(player, price, reason("Refund at", shop));
      if (refused != null) {
        CsmFurnishings.getLogger().warn("Could not refund {} to {} at a shop at {}: {}", price,
            player.getName(), shop.getPos(), refused);
      }
    } else {
      CsmEmeralds.give(player, (long) price);
    }
  }

  /** {@code count} of the shop's item, as one stack (may be over its stack size). */
  private static ItemStack copy(TileEntityShop shop, int count) {
    ItemStack stack = shop.getTemplate().copy();
    stack.setCount(count);
    return stack;
  }

  /** Hands the player {@code amount} of the shop's item, a stack at a time, dropping the rest. */
  private static void give(EntityPlayerMP player, TileEntityShop shop, int amount) {
    int max = Math.max(1, shop.getTemplate().getMaxStackSize());
    int left = amount;
    while (left > 0) {
      int n = Math.min(max, left);
      ItemHandlerHelper.giveItemToPlayer(player, copy(shop, n));
      left -= n;
    }
  }

  private static int countHeld(EntityPlayerMP player, TileEntityShop shop) {
    int count = 0;
    for (ItemStack stack : player.inventory.mainInventory) {
      if (shop.matches(stack)) {
        count += stack.getCount();
      }
    }
    return count;
  }

  private static void takeHeld(EntityPlayerMP player, TileEntityShop shop, int amount) {
    int left = amount;
    for (ItemStack stack : player.inventory.mainInventory) {
      if (left <= 0) {
        break;
      }
      if (shop.matches(stack)) {
        int take = Math.min(left, stack.getCount());
        stack.shrink(take);
        left -= take;
      }
    }
    player.inventory.markDirty();
    player.inventoryContainer.detectAndSendChanges();
  }

  private static String price(TileEntityShop shop, boolean money, double amount) {
    return ShopEconomy.format(money, shop.getSymbol(), amount);
  }

  private static String reason(String what, TileEntityShop shop) {
    BlockPos p = shop.getPos();
    return String.format(Locale.ROOT, "%s shop at %d, %d, %d", what, p.getX(), p.getY(),
        p.getZ());
  }

  private static void sound(EntityPlayerMP player, BlockPos pos, ICsmSound sound) {
    SoundEvent event = sound.getSoundEvent();
    if (event != null) {
      player.world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.6F, 1.0F);
    }
  }

  static void tell(EntityPlayerMP player, String key, Object... args) {
    ITextComponent text = new TextComponentTranslation(key, args);
    player.sendStatusMessage(text, true);
  }
}
