package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.trafficsignals.BlockOverheightDetectionSensor;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Common-side (client + server) lifecycle event handler that clears static caches which would
 * otherwise outlive the world or player session that populated them. Registered on the Forge
 * event bus during {@code Csm.preInit}.
 *
 * <p>See the memory &amp; lifecycle hygiene review in
 * {@code assets/docs/agent_progress/PERFORMANCE_IMPROVEMENT_PLAN.md} (§15).</p>
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class CsmCommonLifecycleHandler {

  /**
   * Drops any in-progress overheight sensor pairing for a player who logs out, so the
   * per-player pairing map cannot accumulate entries across a long server uptime.
   *
   * @param event the player logout event
   */
  @SubscribeEvent
  public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.player != null) {
      BlockOverheightDetectionSensor.clearPendingPairing(event.player.getUniqueID());
    }
  }
}
