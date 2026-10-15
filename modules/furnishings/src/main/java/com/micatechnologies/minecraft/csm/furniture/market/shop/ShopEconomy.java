package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.codeutils.economy.CsmEmeralds;
import com.micatechnologies.minecraft.csm.codeutils.economy.CsmSumEconomy;
import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;

/**
 * The shops' currency: money through SUM's economy when it is installed and allows
 * {@code csm_furnishings} ({@link CsmSumEconomy}), emeralds otherwise ({@link CsmEmeralds}).
 * Which one is in force is decided on the server and sent to clients in each shop's sync.
 *
 * @since 2026.10
 */
public final class ShopEconomy {

  /** The shops' link to SUM, asked for when the server has started. */
  public static final CsmSumEconomy SUM = new CsmSumEconomy(CsmFurnishings.MOD_ID, "shops");

  private ShopEconomy() {
  }

  /** Whether shops trade in money (SUM) rather than emeralds right now. Server side. */
  public static boolean isMoneyMode() {
    return SUM.isAvailable();
  }

  /**
   * A price as shown to players.
   *
   * @param money    whether it is money (else emeralds)
   * @param symbol   the currency symbol, for money
   * @param amount   the amount: money, or a whole number of emeralds
   *
   * @return "$1.50" or "3 emeralds"
   */
  public static String format(boolean money, String symbol, double amount) {
    return money ? CsmSumEconomy.format(symbol, amount) : CsmEmeralds.format((long) amount);
  }
}
