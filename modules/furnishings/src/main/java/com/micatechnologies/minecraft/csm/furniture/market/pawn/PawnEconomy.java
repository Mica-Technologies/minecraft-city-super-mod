package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.economy.CsmEmeralds;
import com.micatechnologies.minecraft.csm.codeutils.economy.CsmSumEconomy;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnLedger.Entry;
import com.micatechnologies.minecraft.csm.furniture.market.shop.ShopEconomy;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * How a pawn shop pays: money through SUM when it is installed and allows
 * {@code csm_furnishings} (the shops' own link, {@link ShopEconomy#SUM}), emeralds otherwise, at
 * {@code moneyPerEmerald}. In emerald mode the part of a payout that does not make a whole emerald
 * is kept in the {@link PawnLedger} and paid once it adds up to one, so small sales still count.
 * A reselling counter is paid the same way round: money, or whole emeralds rounded up. Server
 * side only.
 *
 * @since 2026.10
 */
public final class PawnEconomy {

  private PawnEconomy() {
  }

  /** Whether pawn shops pay money (SUM) rather than emeralds right now. */
  public static boolean isMoneyMode() {
    return ShopEconomy.isMoneyMode();
  }

  /** Money as SUM shows it: "$1.50". */
  public static String money(double amount) {
    return CsmSumEconomy.format(ShopEconomy.SUM.currencySymbol(), amount);
  }

  /**
   * {@code amount} of value as a player would be paid it now: "$1.50", "3 emeralds" or
   * "3.76 emeralds' worth".
   */
  public static ITextComponent describe(double amount) {
    if (isMoneyMode()) {
      return new TextComponentString(money(amount));
    }
    double emeralds = amount / CsmConfig.getPawnMoneyPerEmerald();
    if (Math.abs(emeralds - Math.rint(emeralds)) < 1e-6) {
      return emeralds(Math.round(emeralds));
    }
    return new TextComponentTranslation("csm.pawn.worth.emeralds_part",
        String.format(Locale.ROOT, "%.2f", emeralds));
  }

  /** "1 emerald", "5 emeralds". */
  static ITextComponent emeralds(long count) {
    return count == 1 ? new TextComponentTranslation("csm.pawn.worth.emerald_one")
        : new TextComponentTranslation("csm.pawn.worth.emeralds", count);
  }

  /**
   * Pays an online player.
   *
   * @param amount money (value units), already rounded to cents
   *
   * @return what was paid, or why nothing was
   */
  public static Payment pay(EntityPlayerMP player, double amount, String reason) {
    if (amount <= 0) {
      return new Payment(null, describe(0));
    }
    if (isMoneyMode()) {
      String refused = ShopEconomy.SUM.credit(player, amount, reason);
      return new Payment(refused, describe(amount));
    }
    PawnLedger ledger = PawnLedger.get(player.world);
    Entry e = ledger.entry(player.getUniqueID());
    double rate = CsmConfig.getPawnMoneyPerEmerald();
    double[] split = splitEmeralds(e.emeraldRemainder, amount, rate);
    e.emeraldRemainder = split[1];
    ledger.markDirty();
    long whole = (long) split[0];
    CsmEmeralds.give(player, whole);
    String carried = String.format(Locale.ROOT, "%.2f", split[1] / rate);
    ITextComponent text;
    if (whole == 0) {
      text = new TextComponentTranslation("csm.pawn.paid.none_yet", carried);
    } else if (split[1] > 0) {
      text = new TextComponentTranslation("csm.pawn.paid.carry", emeralds(whole), carried);
    } else {
      text = emeralds(whole);
    }
    return new Payment(null, text);
  }

  /**
   * A payout in emeralds: the remainder carried and the new amount make so many whole emeralds,
   * and what is left over is carried again.
   *
   * @return {whole emeralds, new remainder}
   */
  static double[] splitEmeralds(double remainder, double amount, double rate) {
    double total = remainder + amount;
    long whole = (long) Math.floor(total / rate + 1e-9);
    return new double[]{whole, Math.max(0, total - whole * rate)};
  }

  /** What a resale price of {@code amount} costs in emeralds: rounded up, never free. */
  static long emeraldPrice(double amount) {
    return Math.max(1, (long) Math.ceil(amount / CsmConfig.getPawnMoneyPerEmerald() - 1e-9));
  }

  /** A resale price as the buyer pays it now: "$4.50" or "2 emeralds". */
  public static ITextComponent describePrice(double amount) {
    return isMoneyMode() ? new TextComponentString(money(amount)) : emeralds(emeraldPrice(amount));
  }

  /**
   * Charges a buyer a resale price.
   *
   * @return null if paid, otherwise why not (nothing was taken)
   */
  @Nullable
  static ITextComponent charge(EntityPlayerMP player, double amount, String reason) {
    if (isMoneyMode()) {
      String refused = ShopEconomy.SUM.spend(player, amount, reason);
      return refused == null ? null : new TextComponentString(refused);
    }
    long count = emeraldPrice(amount);
    return CsmEmeralds.take(player, (int) Math.min(Integer.MAX_VALUE, count)) ? null
        : new TextComponentTranslation("csm.pawn.msg.cant_afford", emeralds(count));
  }

  /** A payout: why it was refused, or what the player got. */
  public static final class Payment {

    /** Why nothing was paid, or null. */
    @Nullable
    public final String refusal;
    /** What was paid: "$37.55", or "3 emeralds, with 0.76 of the next one carried over". */
    public final ITextComponent paid;

    Payment(@Nullable String refusal, ITextComponent paid) {
      this.refusal = refusal;
      this.paid = paid;
    }

    public boolean isPaid() {
      return refusal == null;
    }
  }

  /**
   * Pays a player who may be offline: at once if they are online and the payment goes through,
   * otherwise kept as owed and paid when they next log in ({@link #settle}).
   */
  public static void payOrOwe(MinecraftServer server, UUID payee, double amount, String reason) {
    if (amount <= 0) {
      return;
    }
    EntityPlayerMP online = server.getPlayerList().getPlayerByUUID(payee);
    if (online != null && pay(online, amount, reason).isPaid()) {
      return;
    }
    PawnLedger ledger = PawnLedger.get(server.getEntityWorld());
    ledger.entry(payee).owed += amount;
    ledger.markDirty();
  }

  /** Pays what a player is owed, if anything; called when they log in. */
  public static void settle(EntityPlayerMP player) {
    PawnLedger ledger = PawnLedger.get(player.world);
    Entry e = ledger.entry(player.getUniqueID());
    if (e.owed <= 0) {
      return;
    }
    double owed = e.owed;
    // Cleared first: in emerald mode pay() saves the remainder into this same entry.
    e.owed = 0;
    ledger.markDirty();
    Payment paid = pay(player, owed, "Pawn shop: items pawned while you were away");
    if (paid.isPaid()) {
      ITextComponent text = new TextComponentTranslation("csm.pawn.msg.owed", paid.paid);
      text.getStyle().setColor(TextFormatting.GREEN);
      player.sendMessage(text);
    } else {
      e.owed = owed;
    }
  }

  /** The player's emerald remainder, as value. */
  public static double emeraldRemainder(EntityPlayer player) {
    return PawnLedger.get(player.world).entry(player.getUniqueID()).emeraldRemainder;
  }
}
