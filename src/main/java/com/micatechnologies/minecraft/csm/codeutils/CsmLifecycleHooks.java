package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The registry of lifecycle clean-up hooks that Core's lifecycle handlers run.
 * <p>
 * Core owns the two Forge event handlers, but the static caches that need clearing belong to the
 * subsystems that populate them. Each subsystem registers its clean-up here from its own
 * pre-initialization instead of Core naming it, and the handlers run whatever is registered, in
 * registration order.
 * <p>
 * A client-disconnect hook may safely touch client-only code: the hooks are only ever run from
 * the client-side disconnect handler, and merely registering one on a dedicated server does
 * nothing. Registering such a hook with a lambda rather than a method reference matters, though —
 * see the note on the registration side.
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CsmLifecycleHooks {

  /**
   * The registered client-disconnect hooks, in registration order.
   *
   * @since 1.0
   */
  private static final List<Runnable> CLIENT_DISCONNECT_HOOKS = new ArrayList<>();

  /**
   * The registered player-logout hooks, in registration order.
   *
   * @since 1.0
   */
  private static final List<Consumer<UUID>> PLAYER_LOGGED_OUT_HOOKS = new ArrayList<>();

  /**
   * Private constructor: this class is a static registry and is never instantiated.
   *
   * @since 1.0
   */
  private CsmLifecycleHooks() {
    throw new UnsupportedOperationException("CsmLifecycleHooks must not be instantiated");
  }

  /**
   * Registers a hook to run when the client disconnects from a server or closes a single-player
   * world. Call this from a mod's pre-initialization.
   *
   * @param hook the clean-up to run on disconnect
   *
   * @since 1.0
   */
  public static void onClientDisconnect(Runnable hook) {
    if (hook != null) {
      CLIENT_DISCONNECT_HOOKS.add(hook);
    }
  }

  /**
   * Registers a hook to run when a player logs out. Call this from a mod's pre-initialization.
   *
   * @param hook the clean-up to run, given the unique id of the player who logged out
   *
   * @since 1.0
   */
  public static void onPlayerLoggedOut(Consumer<UUID> hook) {
    if (hook != null) {
      PLAYER_LOGGED_OUT_HOOKS.add(hook);
    }
  }

  /**
   * Runs every registered client-disconnect hook, in registration order. The hooks are
   * independent of one another, so their relative order carries no meaning.
   *
   * @since 1.0
   */
  public static void runClientDisconnect() {
    for (Runnable hook : CLIENT_DISCONNECT_HOOKS) {
      hook.run();
    }
  }

  /**
   * Runs every registered player-logout hook, in registration order.
   *
   * @param playerId the unique id of the player who logged out
   *
   * @since 1.0
   */
  public static void runPlayerLoggedOut(UUID playerId) {
    for (Consumer<UUID> hook : PLAYER_LOGGED_OUT_HOOKS) {
      hook.accept(playerId);
    }
  }
}
