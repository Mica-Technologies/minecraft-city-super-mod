package com.micatechnologies.minecraft.csm.signage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * The server's side of sending its ads: the catalogue to every player as they join, and an ad's
 * file, in chunks, to a player who asks for it.
 *
 * <p>A request is the one thing here a hostile client controls, so it is answered only when the
 * index is in the catalogue, not sooner than {@link #MIN_GAP_MILLIS} after the player's last, and
 * while the player has been sent less than twice the whole catalogue this session -- an honest
 * client downloads each ad once and caches it. Anything else gets an empty, refusing chunk, so
 * the client stops waiting.</p>
 */
public final class ServerAdSync {

  /** The shortest time between two requests from one player. */
  static final long MIN_GAP_MILLIS = 250;

  private static final Map<UUID, long[]> PLAYERS = new ConcurrentHashMap<>();

  /** The event handler; an instance, since the bus wants one for non-static methods. */
  public static final class Events {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.player instanceof EntityPlayerMP) {
        CsmSignage.NETWORK.sendTo(new ServerAdPackets.Catalogue(ServerAds.catalogue()),
            (EntityPlayerMP) event.player);
      }
    }
  }

  private ServerAdSync() {
  }

  /** Forgets a player who has left. */
  static void forget(UUID player) {
    PLAYERS.remove(player);
  }

  /** Answers a player's request for ad {@code index}. On the server thread. */
  static void serve(EntityPlayerMP player, int index) {
    List<ServerAds.Ad> ads = ServerAds.catalogue();
    long now = System.currentTimeMillis();
    // {time of the last request, bytes sent this session}
    long[] state = PLAYERS.computeIfAbsent(player.getUniqueID(), id -> new long[]{0, 0});
    long budget = 2L * totalBytes(ads);
    boolean ok = SignageConfig.isServerAdsAllowed() && index < ads.size()
        && now - state[0] >= MIN_GAP_MILLIS && state[1] < budget;
    state[0] = now;
    if (!ok) {
      CsmSignage.NETWORK.sendTo(new ServerAdPackets.Chunk(index, 0, 0, new byte[0]), player);
      return;
    }
    ServerAds.Ad ad = ads.get(index);
    byte[] bytes = ad.bytes;
    state[1] += bytes.length;
    for (int offset = 0; offset < bytes.length; offset += ServerAds.CHUNK) {
      int end = Math.min(bytes.length, offset + ServerAds.CHUNK);
      CsmSignage.NETWORK.sendTo(new ServerAdPackets.Chunk(index, offset, bytes.length,
          Arrays.copyOfRange(bytes, offset, end)), player);
    }
  }

  private static long totalBytes(List<ServerAds.Ad> ads) {
    long total = 0;
    for (ServerAds.Ad ad : ads) {
      total += ad.length;
    }
    return total;
  }
}
