package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Common-side (client + server) lifecycle event handler that clears static caches which would
 * otherwise outlive the world or player session that populated them. Registered on the Forge
 * event bus during {@code Csm.preInit}.
 *
 * <p>The caches themselves belong to the subsystems that populate them; each registers its
 * clean-up with {@link CsmLifecycleHooks}.</p>
 *
 * <p>See the memory &amp; lifecycle hygiene review in
 * {@code assets/docs/agent_progress/PERFORMANCE_IMPROVEMENT_PLAN.md} (§15).</p>
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class CsmCommonLifecycleHandler {

  /**
   * Runs the registered logout hooks for a player who logs out — today that drops any
   * in-progress overheight sensor pairing, so the per-player pairing map cannot accumulate
   * entries across a long server uptime.
   *
   * @param event the player logout event
   */
  @SubscribeEvent
  public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.player != null) {
      CsmLifecycleHooks.runPlayerLoggedOut(event.player.getUniqueID());
    }
  }
}
