package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.lang.reflect.Method;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

/**
 * Parking meters' optional link to SUM's economy (the user's server utility mod, mod id
 * {@code sum}): with SUM installed and allowing this integration, meters charge money instead of
 * emeralds.
 *
 * <p><b>SUM is never required,</b> so nothing in CSM is compiled against it. This class reaches
 * SUM's public API ({@code com.micatechnologies.minecraft.sum.api.SumEconomy} and
 * {@code EconomyHandle}) by reflection, and only when SUM is loaded. A missing, older or
 * refusing SUM therefore cannot throw a class-loading error anywhere; the meters fall back to
 * emeralds and a single log line says why.</p>
 *
 * <p>The handle is asked for as {@code csm_roads} once the server has started. SUM attaches its
 * economy in its server-starting handler and loads after CSM, so asking any earlier finds no
 * economy yet. A server operator has to allow that id in SUM's
 * {@code economy_integration.allowedMods} with {@code csm_roads=wallet_read,wallet_write};
 * a fresh SUM server allows nothing.</p>
 *
 * <p>SUM only pays and charges players who are online. So a meter set to collect keeps its
 * takings itself, and its owner collects them in person ({@link #credit}); nothing is ever
 * credited to an absent owner.</p>
 *
 * @version 1.0
 */
public final class ParkingPaymentSum {

  /** SUM's mod id. */
  private static final String SUM_MOD_ID = "sum";
  /** The id this integration is authorised under in SUM's config. */
  private static final String INTEGRATION_ID = CsmRoads.MOD_ID;
  /** The SUM API revision this was written against. */
  private static final int API_VERSION = 1;

  private static final String API = "com.micatechnologies.minecraft.sum.api.";

  @Nullable
  private static Object handle;
  @Nullable
  private static Method walletSpend;
  @Nullable
  private static Method walletCredit;
  @Nullable
  private static Method resultIsOk;
  @Nullable
  private static Method resultMessage;
  private static boolean deniedLogged;

  private ParkingPaymentSum() {
  }

  /**
   * Asks SUM for an economy handle. Called when the server has started; safe to call with no
   * SUM.
   */
  public static void acquire() {
    release();
    if (!Loader.isModLoaded(SUM_MOD_ID)) {
      return;
    }
    try {
      Class<?> sum = Class.forName(API + "SumEconomy");
      Method compatible = sum.getMethod("isCompatible", int.class);
      if (!(Boolean) compatible.invoke(null, API_VERSION)) {
        logOnce("SUM is installed but older than its economy API revision " + API_VERSION
            + "; parking meters take emeralds.");
        return;
      }
      Optional<?> maybe = (Optional<?>) sum.getMethod("acquire", String.class)
          .invoke(null, INTEGRATION_ID);
      if (!maybe.isPresent()) {
        Object why = sum.getMethod("describeDenial", String.class).invoke(null, INTEGRATION_ID);
        logOnce("SUM's economy is not available to parking meters, so they take emeralds: "
            + why);
        return;
      }
      Class<?> handleClass = Class.forName(API + "EconomyHandle");
      Class<?> resultClass = Class.forName(API + "EconomyResult");
      walletSpend = handleClass.getMethod("walletSpend", EntityPlayer.class, double.class,
          String.class);
      walletCredit = handleClass.getMethod("walletCredit", EntityPlayer.class, double.class,
          String.class);
      resultIsOk = resultClass.getMethod("isOk");
      resultMessage = resultClass.getMethod("getMessage");
      handle = maybe.get();
      CsmRoads.getLogger().info("Parking meters take money through SUM's economy.");
    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
      release();
      logOnce("Could not reach SUM's economy API (" + e + "); parking meters take emeralds.");
    }
  }

  /** Drops the handle, so the next server start asks (and logs) afresh. */
  public static void release() {
    handle = null;
    deniedLogged = false;
    walletSpend = null;
    walletCredit = null;
    resultIsOk = null;
    resultMessage = null;
  }

  /** Whether meters should charge money through SUM right now. */
  public static boolean isAvailable() {
    return handle != null;
  }

  /**
   * Takes {@code amount} from the player's wallet.
   *
   * @return {@code null} on success, otherwise SUM's reason, fit to show the player
   */
  @Nullable
  public static String spend(EntityPlayer player, double amount, String reason) {
    return call(walletSpend, player, amount, reason);
  }

  /**
   * Gives {@code amount} to the (online) player's wallet: an owner collecting a meter's takings.
   *
   * @return {@code null} on success, otherwise SUM's reason
   */
  @Nullable
  public static String credit(EntityPlayer player, double amount, String reason) {
    return call(walletCredit, player, amount, reason);
  }

  @Nullable
  private static String call(@Nullable Method method, EntityPlayer player, double amount,
      String reason) {
    Object h = handle;
    if (h == null || method == null || resultIsOk == null) {
      return "The economy is not available.";
    }
    try {
      Object result = method.invoke(h, player, amount, reason);
      if ((Boolean) resultIsOk.invoke(result)) {
        return null;
      }
      Object message = resultMessage != null ? resultMessage.invoke(result) : null;
      return message != null ? message.toString() : "The payment was refused.";
    } catch (ReflectiveOperationException | RuntimeException e) {
      CsmRoads.getLogger().warn("A parking payment through SUM failed", e);
      return "The payment could not be made.";
    }
  }

  private static void logOnce(String message) {
    if (!deniedLogged) {
      deniedLogged = true;
      CsmRoads.getLogger().info(message);
    }
  }
}
