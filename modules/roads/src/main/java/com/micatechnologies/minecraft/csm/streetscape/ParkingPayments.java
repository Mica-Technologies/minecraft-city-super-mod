package com.micatechnologies.minecraft.csm.streetscape;

import java.util.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Paying a parking meter, and collecting from one, on the server.
 *
 * <p>A meter charges money through SUM's economy when {@link ParkingPaymentSum} says it can, and
 * emeralds otherwise. Either way a payment buys whole blocks of time, never past the meter's
 * maximum, and a meter set to collect keeps what it took for its owner.</p>
 *
 * @version 1.0
 */
public final class ParkingPayments {

  private ParkingPayments() {
  }

  /** Whether the player may change a meter's settings and collect from it: its owner, or an op. */
  public static boolean mayConfigure(EntityPlayer player, TileEntityParkingMeter meter) {
    return (meter.getOwner() != null && meter.getOwner().equals(player.getUniqueID()))
        || player.canUseCommand(2, "");
  }

  /**
   * Buys up to {@code blocks} blocks of time on {@code space} for {@code player}, telling them
   * the outcome in the action bar.
   */
  public static void pay(EntityPlayer player, World world, BlockPos pos,
      TileEntityParkingMeter meter, int space, int blocks) {
    long now = System.currentTimeMillis();
    if (space < 0 || space >= meter.getSpaces()) {
      tell(player, "There is no space " + (space + 1) + " here.");
      return;
    }
    int wanted = Math.min(blocks, meter.blocksToMax(space, now));
    if (wanted <= 0) {
      tell(player, "Space " + (space + 1) + " is already paid to the maximum.");
      return;
    }
    if (ParkingPaymentSum.isAvailable()) {
      double amount = wanted * meter.getMoneyPerBlock();
      String refused = ParkingPaymentSum.spend(player, amount,
          String.format(Locale.ROOT, "Parking at %d, %d, %d", pos.getX(), pos.getY(),
              pos.getZ()));
      if (refused != null) {
        tell(player, refused);
        return;
      }
      int bought = meter.extend(space, wanted, now);
      if (meter.isCollect()) {
        meter.addTakings(0, amount);
      }
      paid(player, world, pos, meter, space, bought, now, money(amount));
      return;
    }
    int perBlock = meter.getEmeraldsPerBlock();
    int held = countEmeralds(player);
    int affordable = player.capabilities.isCreativeMode ? wanted : Math.min(wanted,
        held / perBlock);
    if (affordable <= 0) {
      tell(player, "This meter takes " + emeralds(perBlock) + " per "
          + duration(meter.getMinutesPerBlock()) + ".");
      return;
    }
    int bought = meter.extend(space, affordable, now);
    int cost = bought * perBlock;
    if (!player.capabilities.isCreativeMode) {
      removeEmeralds(player, cost);
    }
    if (meter.isCollect()) {
      meter.addTakings(cost, 0);
    }
    paid(player, world, pos, meter, space, bought, now, emeralds(cost));
  }

  private static void paid(EntityPlayer player, World world, BlockPos pos,
      TileEntityParkingMeter meter, int space, int bought, long now, String cost) {
    meter.markDirtySync(world, pos, true);
    BlockParkingMeter.refresh(world, pos);
    tell(player, "Paid " + cost + " for " + duration(bought * meter.getMinutesPerBlock())
        + " -- space " + (space + 1) + ": " + remaining(meter, space, now) + " left");
  }

  /** Hands the owner what the meter has collected. */
  public static void collect(EntityPlayer player, World world, BlockPos pos,
      TileEntityParkingMeter meter) {
    if (!mayConfigure(player, meter)) {
      return;
    }
    StringBuilder said = new StringBuilder();
    long emeralds = meter.getStoredEmeralds();
    if (emeralds > 0) {
      giveEmeralds(player, emeralds);
      meter.clearEmeralds();
      said.append("Collected ").append(emeralds(emeralds));
    }
    double money = meter.getStoredMoney();
    if (money > 0) {
      if (!ParkingPaymentSum.isAvailable()) {
        said.append(said.length() > 0 ? "; " : "")
            .append(money(money)).append(" waits until the economy is available");
      } else {
        String refused = ParkingPaymentSum.credit(player, money,
            String.format(Locale.ROOT, "Parking takings at %d, %d, %d", pos.getX(),
                pos.getY(), pos.getZ()));
        if (refused == null) {
          meter.clearMoney();
          said.append(said.length() > 0 ? "; " : "").append("Collected ").append(money(money));
        } else {
          said.append(said.length() > 0 ? "; " : "").append(refused);
        }
      }
    }
    meter.markDirtySync(world, pos, true);
    tell(player, said.length() > 0 ? said.toString() : "Nothing to collect.");
  }

  /** Every space's time left, for the status line a click shows. */
  public static String status(TileEntityParkingMeter meter) {
    long now = System.currentTimeMillis();
    StringBuilder out = new StringBuilder();
    int shown = Math.min(meter.getSpaces(), 4);
    for (int i = 0; i < shown; i++) {
      if (i > 0) {
        out.append("  |  ");
      }
      if (meter.getSpaces() > 1) {
        out.append("Space ").append(i + 1).append(": ");
      }
      out.append(meter.isExpired(i, now) ? "EXPIRED" : remaining(meter, i, now) + " left");
    }
    if (meter.getSpaces() > shown) {
      out.append("  |  ...");
    }
    return out.toString();
  }

  public static void tell(EntityPlayer player, String message) {
    player.sendStatusMessage(new TextComponentString(message), true);
  }

  // ----------------------------------------------------------------------------------------
  // Formatting
  // ----------------------------------------------------------------------------------------

  /** "12:34" under an hour, "1:05:00" over it. */
  public static String remaining(TileEntityParkingMeter meter, int space, long now) {
    return clock(meter.remaining(space, now));
  }

  public static String clock(long millis) {
    long seconds = Math.max(0, (millis + 999) / 1000);
    long h = seconds / 3600;
    long m = (seconds % 3600) / 60;
    long s = seconds % 60;
    return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        : String.format(Locale.ROOT, "%d:%02d", m, s);
  }

  public static String duration(int minutes) {
    if (minutes % 60 == 0) {
      return minutes / 60 + (minutes == 60 ? " hour" : " hours");
    }
    if (minutes > 60) {
      return minutes / 60 + " h " + minutes % 60 + " min";
    }
    return minutes + " min";
  }

  public static String money(double amount) {
    return String.format(Locale.ROOT, "$%.2f", amount);
  }

  public static String emeralds(long count) {
    return count + (count == 1 ? " emerald" : " emeralds");
  }

  // ----------------------------------------------------------------------------------------
  // Emeralds
  // ----------------------------------------------------------------------------------------

  private static int countEmeralds(EntityPlayer player) {
    int count = 0;
    for (ItemStack stack : player.inventory.mainInventory) {
      if (stack.getItem() == Items.EMERALD) {
        count += stack.getCount();
      }
    }
    return count;
  }

  private static void removeEmeralds(EntityPlayer player, int count) {
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
  }

  private static void giveEmeralds(EntityPlayer player, long count) {
    long left = count;
    while (left > 0) {
      int n = (int) Math.min(64, left);
      ItemStack stack = new ItemStack(Items.EMERALD, n);
      if (!player.inventory.addItemStackToInventory(stack)) {
        player.dropItem(stack, false);
      }
      left -= n;
    }
  }
}
