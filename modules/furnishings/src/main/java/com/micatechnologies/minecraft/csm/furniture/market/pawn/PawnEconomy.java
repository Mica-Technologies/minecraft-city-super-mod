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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * How a pawn shop pays: money through SUM when it is installed and allows
 * {@code csm_furnishings} (the shops' own link, {@link ShopEconomy#SUM}), emeralds otherwise, at
 * {@code moneyPerEmerald}. In emerald mode the part of a payout that does not make a whole emerald
 * is kept in the {@link PawnLedger} and paid once it adds up to one, so small sales still count.
 * Server side only.
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

  /** {@code amount} of value as a player would be paid it now: "$1.50", "3 emeralds" or "3.76 emeralds' worth". */
  public static String describe(double amount) {
    if (isMoneyMode()) {
      return CsmSumEconomy.format(ShopEconomy.SUM.currencySymbol(), amount);
    }
    double emeralds = amount / CsmConfig.getPawnMoneyPerEmerald();
    if (emeralds == Math.floor(emeralds)) {
      return CsmEmeralds.format((long) emeralds);
    }
    return String.format(Locale.ROOT, "%.2f emeralds' worth", emeralds);
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
    String text;
    if (whole == 0) {
      text = String.format(Locale.ROOT, "no emerald yet, %.2f of one carried over",
          split[1] / rate);
    } else {
      text = CsmEmeralds.format(whole);
      if (split[1] > 0) {
        text += String.format(Locale.ROOT, ", with %.2f of the next one carried over",
            split[1] / rate);
      }
    }
    return new Payment(null, text);
  }

  /** A payout: why it was refused, or what the player got. */
  public static final class Payment {

    /** Why nothing was paid, or null. */
    @Nullable
    public final String refusal;
    /** What was paid: "$37.55", or "3 emeralds, with 0.76 of the next one carried over". */
    public final String paid;

    Payment(@Nullable String refusal, String paid) {
      this.refusal = refusal;
      this.paid = paid;
    }

    public boolean isPaid() {
      return refusal == null;
    }
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
      player.sendMessage(new TextComponentString(TextFormatting.GREEN
          + "A pawn shop paid you " + paid.paid + " for items pawned while you were away."));
    } else {
      e.owed = owed;
    }
  }

  /** The player's emerald remainder, as value, for the counter screen. */
  public static double emeraldRemainder(EntityPlayer player) {
    return PawnLedger.get(player.world).entry(player.getUniqueID()).emeraldRemainder;
  }
}
