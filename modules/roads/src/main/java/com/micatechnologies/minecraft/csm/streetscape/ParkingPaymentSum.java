package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.economy.CsmSumEconomy;
import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Parking meters' optional link to SUM's economy (the user's server utility mod, mod id
 * {@code sum}): with SUM installed and allowing this integration, meters charge money instead of
 * emeralds.
 *
 * <p>The reflection lives in Core's {@link CsmSumEconomy}, shared with the shops; this class is
 * the meters' instance of it. The handle is asked for as {@code csm_roads} once the server has
 * started, so a server operator allows {@code csm_roads=wallet_read,wallet_write} in SUM's
 * {@code economy_integration.allowedMods}.</p>
 *
 * <p>SUM only pays and charges players who are online. So a meter set to collect keeps its
 * takings itself, and its owner collects them in person ({@link #credit}); nothing is ever
 * credited to an absent owner.</p>
 *
 * @version 1.1
 */
public final class ParkingPaymentSum {

  private static final CsmSumEconomy ECONOMY = new CsmSumEconomy(CsmRoads.MOD_ID,
      "parking meters");

  private ParkingPaymentSum() {
  }

  /**
   * Asks SUM for an economy handle. Called when the server has started; safe to call with no
   * SUM.
   */
  public static void acquire() {
    ECONOMY.acquire();
  }

  /** Drops the handle, so the next server start asks (and logs) afresh. */
  public static void release() {
    ECONOMY.release();
  }

  /** Whether meters should charge money through SUM right now. */
  public static boolean isAvailable() {
    return ECONOMY.isAvailable();
  }

  /**
   * Takes {@code amount} from the player's wallet.
   *
   * @return {@code null} on success, otherwise SUM's reason, fit to show the player
   */
  @Nullable
  public static String spend(EntityPlayer player, double amount, String reason) {
    return ECONOMY.spend(player, amount, reason);
  }

  /**
   * Gives {@code amount} to the (online) player's wallet: an owner collecting a meter's takings.
   *
   * @return {@code null} on success, otherwise SUM's reason
   */
  @Nullable
  public static String credit(EntityPlayer player, double amount, String reason) {
    return ECONOMY.credit(player, amount, reason);
  }
}
