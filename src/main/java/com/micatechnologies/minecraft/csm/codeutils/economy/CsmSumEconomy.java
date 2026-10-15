package com.micatechnologies.minecraft.csm.codeutils.economy;

import com.micatechnologies.minecraft.csm.Csm;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

/**
 * One CSM module's optional link to SUM's economy (the user's server utility mod, mod id
 * {@code sum}): with SUM installed and allowing the integration, the module charges and pays
 * money instead of emeralds.
 *
 * <p><b>SUM is never required,</b> so nothing in CSM is compiled against it. This class reaches
 * SUM's public API ({@code com.micatechnologies.minecraft.sum.api.SumEconomy},
 * {@code EconomyHandle}, {@code EconomyResult}, {@code EconomyStatus}) by reflection, and only
 * when SUM is loaded. A missing, older or refusing SUM therefore cannot throw a class-loading
 * error anywhere; the module falls back to emeralds and a single log line says why.</p>
 *
 * <p>Each module asks under its own integration id (its mod id: parking meters as
 * {@code csm_roads}, shops as {@code csm_furnishings}), so a server operator allows each one
 * separately in SUM's {@code economy_integration.allowedMods}, e.g.
 * {@code csm_furnishings=wallet_read,wallet_write}. A fresh SUM server allows nothing.</p>
 *
 * <p>Ask for the handle ({@link #acquire()}) once the server has <em>started</em>: SUM attaches
 * its economy in its own server-starting handler and loads after CSM, so asking any earlier finds
 * no economy yet. Release it when the server stops. Every call is server-thread only; SUM refuses
 * any other thread.</p>
 *
 * <p>SUM only pays and charges players who are online, so anything owed to an absent player
 * (a meter's or a shop's takings) is kept by the block and collected in person.</p>
 *
 * @since 2026.10
 */
public final class CsmSumEconomy {

  /** SUM's mod id. */
  public static final String SUM_MOD_ID = "sum";
  /** The SUM API revision this was written against. */
  private static final int API_VERSION = 1;

  private static final String API = "com.micatechnologies.minecraft.sum.api.";

  /** The id this integration is authorised under in SUM's config. */
  private final String integrationId;
  /** What the module calls the things paying through it, for the log ("parking meters"). */
  private final String what;

  @Nullable
  private Object handle;
  @Nullable
  private Method walletSpend;
  @Nullable
  private Method walletCredit;
  @Nullable
  private Method walletBalance;
  @Nullable
  private Method resultIsOk;
  @Nullable
  private Method resultMessage;
  @Nullable
  private Method handleStatus;
  @Nullable
  private Method statusSymbol;
  private boolean deniedLogged;

  /**
   * Makes a link that asks under {@code integrationId}.
   *
   * @param integrationId the module's mod id, as allowed in SUM's config
   * @param what          what pays through it, plural, for the log ("parking meters")
   */
  public CsmSumEconomy(String integrationId, String what) {
    this.integrationId = integrationId;
    this.what = what;
  }

  /** The id this link asks under. */
  public String getIntegrationId() {
    return integrationId;
  }

  /**
   * Asks SUM for an economy handle. Called when the server has started; safe to call with no
   * SUM.
   */
  public synchronized void acquire() {
    release();
    if (!Loader.isModLoaded(SUM_MOD_ID)) {
      return;
    }
    try {
      Class<?> sum = Class.forName(API + "SumEconomy");
      Method compatible = sum.getMethod("isCompatible", int.class);
      if (!(Boolean) compatible.invoke(null, API_VERSION)) {
        logOnce("SUM is installed but older than its economy API revision " + API_VERSION
            + "; " + what + " take emeralds.");
        return;
      }
      Optional<?> maybe = (Optional<?>) sum.getMethod("acquire", String.class)
          .invoke(null, integrationId);
      if (!maybe.isPresent()) {
        Object why = sum.getMethod("describeDenial", String.class).invoke(null, integrationId);
        logOnce("SUM's economy is not available to " + what + " (" + integrationId
            + "), so they take emeralds: " + why);
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
      // Reading a balance and the currency symbol are nice to have, never needed to pay.
      walletBalance = optionalMethod(handleClass, "getWalletBalance", EntityPlayer.class);
      handleStatus = optionalMethod(handleClass, "getStatus");
      Class<?> statusClass = optionalClass(API + "EconomyStatus");
      statusSymbol = statusClass == null ? null : optionalMethod(statusClass,
          "getCurrencySymbol");
      handle = maybe.get();
      Csm.getLogger().info("{} take money through SUM's economy ({}).",
          capitalised(what), integrationId);
    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
      release();
      logOnce("Could not reach SUM's economy API (" + e + "); " + what + " take emeralds.");
    }
  }

  /** Drops the handle, so the next server start asks (and logs) afresh. */
  public synchronized void release() {
    handle = null;
    deniedLogged = false;
    walletSpend = null;
    walletCredit = null;
    walletBalance = null;
    resultIsOk = null;
    resultMessage = null;
    handleStatus = null;
    statusSymbol = null;
  }

  /** Whether money should move through SUM right now. */
  public boolean isAvailable() {
    return handle != null;
  }

  /**
   * Takes {@code amount} from the player's wallet.
   *
   * @return {@code null} on success, otherwise SUM's reason, fit to show the player
   */
  @Nullable
  public String spend(EntityPlayer player, double amount, String reason) {
    return call(walletSpend, player, amount, reason);
  }

  /**
   * Gives {@code amount} to the (online) player's wallet. This makes money: use it to pay out
   * what a block has collected, or a server shop's purchases.
   *
   * @return {@code null} on success, otherwise SUM's reason
   */
  @Nullable
  public String credit(EntityPlayer player, double amount, String reason) {
    return call(walletCredit, player, amount, reason);
  }

  /** The player's wallet balance, or empty if it cannot be read. */
  public OptionalDouble balance(EntityPlayer player) {
    Object h = handle;
    Method m = walletBalance;
    if (h == null || m == null) {
      return OptionalDouble.empty();
    }
    try {
      Object result = m.invoke(h, player);
      return result instanceof OptionalDouble ? (OptionalDouble) result : OptionalDouble.empty();
    } catch (ReflectiveOperationException | RuntimeException e) {
      return OptionalDouble.empty();
    }
  }

  /** SUM's currency symbol, {@code "$"} unless its bank says otherwise. */
  public String currencySymbol() {
    Object h = handle;
    if (h != null && handleStatus != null && statusSymbol != null) {
      try {
        Object symbol = statusSymbol.invoke(handleStatus.invoke(h));
        if (symbol != null && !symbol.toString().isEmpty()) {
          return symbol.toString();
        }
      } catch (ReflectiveOperationException | RuntimeException ignored) {
        // Fall back to the dollar sign SUM uses itself.
      }
    }
    return "$";
  }

  /** {@code amount} as SUM shows money: the symbol and two decimals. */
  public static String format(String symbol, double amount) {
    return symbol + String.format(Locale.ROOT, "%.2f", amount);
  }

  @Nullable
  private String call(@Nullable Method method, EntityPlayer player, double amount,
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
      Csm.getLogger().warn("A payment through SUM for " + what + " failed", e);
      return "The payment could not be made.";
    }
  }

  @Nullable
  private static Method optionalMethod(Class<?> owner, String name, Class<?>... args) {
    try {
      return owner.getMethod(name, args);
    } catch (NoSuchMethodException | RuntimeException e) {
      return null;
    }
  }

  @Nullable
  private static Class<?> optionalClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException | LinkageError e) {
      return null;
    }
  }

  private static String capitalised(String text) {
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  private void logOnce(String message) {
    if (!deniedLogged) {
      deniedLogged = true;
      Csm.getLogger().info(message);
    }
  }
}
