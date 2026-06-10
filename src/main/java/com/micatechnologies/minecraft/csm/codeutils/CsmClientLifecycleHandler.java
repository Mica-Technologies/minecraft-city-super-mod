package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.lifesafety.FireAlarmSoundPacketHandler;
import com.micatechnologies.minecraft.csm.novelties.BlockHd;
import com.micatechnologies.minecraft.csm.technology.SpeakerAmbientPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsignals.APSSoundPacketHandler;
import com.micatechnologies.minecraft.csm.trafficsigns.AbstractBlockSign;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side lifecycle event handler that clears static sound/strobe/render caches when the
 * client disconnects from a server or closes a single-player world. Without this, channel
 * sound maps, strobe position registries, and per-position caches survive into the next world
 * the player joins — both a memory leak and a correctness bug (stale strobe positions could
 * render in a different world at the same coordinates). Registered on the Forge event bus
 * during {@code CsmClientProxy.preInit}.
 *
 * <p>See the memory &amp; lifecycle hygiene review in
 * {@code assets/docs/agent_progress/PERFORMANCE_IMPROVEMENT_PLAN.md} (§15).</p>
 *
 * @author Mica Technologies
 * @since 2026.6
 */
@SideOnly(Side.CLIENT)
public class CsmClientLifecycleHandler {

  /**
   * Clears all client-side static caches on disconnect. The event fires on the netty thread,
   * so the cleanup (which touches the sound handler) is scheduled onto the client thread.
   *
   * @param event the client disconnection event
   */
  @SubscribeEvent
  public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    Minecraft.getMinecraft().addScheduledTask(() -> {
      FireAlarmSoundPacketHandler.stopAllSounds(); // also clears ActiveStrobeRegistry
      APSSoundPacketHandler.stopAllSounds();
      SpeakerAmbientPacketHandler.stopAllSounds();
      BlockHd.clearClientCaches();
      AbstractBlockSign.clearSetbackCache();
    });
  }
}
