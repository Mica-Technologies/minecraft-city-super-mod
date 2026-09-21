package com.micatechnologies.minecraft.csm.signage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The client's side of a server's ads: the catalogue it was sent, which ads it holds as textures,
 * and the one download it may have in flight.
 *
 * <p>An ad is fetched only when a board on screen needs it ({@link #shown}), from the disk cache
 * ({@code csm_ad_cache/<sha256>.png} in the game folder) if a verified copy is there, else from the
 * server. Every image is checked twice before it is decoded -- its SHA-256 against the catalogue,
 * and its PNG header against the size limits -- so nothing the server would itself have refused
 * is ever decoded here. Until an ad is ready, the board shows the house ad.</p>
 *
 * <p>All on the client thread: the packet handlers schedule onto it.</p>
 */
@SideOnly(Side.CLIENT)
public final class ServerAdImages {

  private static final Logger LOGGER = LogManager.getLogger("csm_signage");
  /** How long a refused or failed ad is left before it is asked for again. */
  private static final long RETRY_MILLIS = 60_000;

  private static List<ServerAds.Ad> catalogue = new ArrayList<>();
  private static final Map<String, ResourceLocation> READY = new HashMap<>();
  private static final Map<String, Long> FAILED = new HashMap<>();
  private static final Set<Integer> WANTED = new LinkedHashSet<>();
  /** Ads whose disk cache has been looked at, so a missing file is not looked for every frame. */
  private static final Set<String> CACHE_CHECKED = new HashSet<>();

  /** The download in flight: its index, what has arrived, and when it was asked for. */
  private static int current = -1;
  private static ByteArrayOutputStream received;
  private static long askedAt;

  private ServerAdImages() {
  }

  /**
   * The ad a board should draw for {@code ad}: the ad itself if it ships in the jar or is ready,
   * otherwise the house ad -- and the missing ad asked for.
   */
  public static AdEntry shown(AdEntry ad) {
    if (!ad.isServer() || READY.containsKey(ad.getServerHex())) {
      return ad;
    }
    want(ad);
    return AdLibrary.get().resolve(AdLibrary.HOUSE_AD);
  }

  private static void want(AdEntry ad) {
    if (!SignageConfig.isServerAdsAccepted()) {
      return;
    }
    Long failedAt = FAILED.get(ad.getServerHex());
    if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_MILLIS) {
      return;
    }
    for (int i = 0; i < catalogue.size(); i++) {
      if (catalogue.get(i).hex().equals(ad.getServerHex())) {
        if (CACHE_CHECKED.add(ad.getServerHex()) && fromCache(catalogue.get(i))) {
          return;
        }
        WANTED.add(i);
        pump();
        return;
      }
    }
  }

  /** Asks for the next wanted ad, if nothing is in flight (or what is has stalled). */
  private static void pump() {
    if (current >= 0 && System.currentTimeMillis() - askedAt < 30_000) {
      return;
    }
    current = -1;
    while (!WANTED.isEmpty()) {
      int next = WANTED.iterator().next();
      WANTED.remove(next);
      if (next < catalogue.size() && !READY.containsKey(catalogue.get(next).hex())) {
        current = next;
        received = new ByteArrayOutputStream();
        askedAt = System.currentTimeMillis();
        CsmSignage.NETWORK.sendToServer(new ServerAdPackets.Request(next));
        return;
      }
    }
  }

  /** A new server's catalogue: forget the last one's, keep only what is well formed. */
  static void onCatalogue(List<ServerAds.Ad> sent) {
    clear();
    List<ServerAds.Ad> kept = new ArrayList<>();
    for (ServerAds.Ad ad : sent) {
      if (ServerAds.validId(ad.id) && ad.width >= 1 && ad.height >= 1
          && ad.width <= ServerAds.MAX_SIDE && ad.height <= ServerAds.MAX_SIDE
          && ad.length > 0 && ad.length <= ServerAds.MAX_BYTES && kept.size() < ServerAds.MAX_ADS) {
        kept.add(ad);
      }
    }
    catalogue = kept;
    AdLibrary.get().setServerAds(ServerAds.entries(kept));
  }

  /** A piece of the ad in flight. Anything out of order ends the download. */
  static void onChunk(int index, int offset, int total, byte[] bytes) {
    if (index != current || index >= catalogue.size()) {
      return;
    }
    ServerAds.Ad ad = catalogue.get(index);
    if (total == 0) {
      fail(ad, "refused by the server");
      return;
    }
    if (total != ad.length || offset != received.size() || offset + bytes.length > total) {
      fail(ad, "arrived out of order");
      return;
    }
    received.write(bytes, 0, bytes.length);
    if (received.size() < total) {
      return;
    }
    byte[] file = received.toByteArray();
    current = -1;
    received = null;
    if (install(ad, file)) {
      writeCache(ad, file);
    } else {
      fail(ad, "failed its checks");
    }
    pump();
  }

  private static void fail(ServerAds.Ad ad, String why) {
    LOGGER.warn("Server ad {} {}", ad.id, why);
    FAILED.put(ad.hex(), System.currentTimeMillis());
    current = -1;
    received = null;
    pump();
  }

  /** Checks a file against its catalogue entry and makes it a texture. */
  private static boolean install(ServerAds.Ad ad, byte[] file) {
    if (!Arrays.equals(ServerAds.sha256(file), ad.hash) || ServerAds.pngSize(file) == null) {
      return false;
    }
    BufferedImage image = decode(file);
    if (image == null) {
      return false;
    }
    // The one location the ad's library entry names, whatever the shape.
    ResourceLocation location = AdEntry.server(ad).texture(AdShape.SQUARE);
    Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(image));
    READY.put(ad.hex(), location);
    return true;
  }

  @Nullable
  private static BufferedImage decode(byte[] file) {
    try {
      return ImageIO.read(new ByteArrayInputStream(file));
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  // --- the disk cache ---------------------------------------------------------------------------

  private static File cacheFile(ServerAds.Ad ad) {
    return new File(new File(Minecraft.getMinecraft().gameDir, "csm_ad_cache"), ad.hex() + ".png");
  }

  private static boolean fromCache(ServerAds.Ad ad) {
    File file = cacheFile(ad);
    if (!file.isFile() || file.length() != ad.length) {
      return false;
    }
    try {
      return install(ad, Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      return false;
    }
  }

  private static void writeCache(ServerAds.Ad ad, byte[] file) {
    File target = cacheFile(ad);
    try {
      Files.createDirectories(target.getParentFile().toPath());
      Files.write(target.toPath(), file);
    } catch (IOException e) {
      LOGGER.warn("Could not cache server ad {}", ad.id, e);
    }
  }

  /** Forgets the server's ads and frees their textures: on leaving a server. */
  public static void clear() {
    for (ResourceLocation location : READY.values()) {
      Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
    }
    READY.clear();
    FAILED.clear();
    WANTED.clear();
    CACHE_CHECKED.clear();
    catalogue = new ArrayList<>();
    current = -1;
    received = null;
    AdLibrary.get().setServerAds(new ArrayList<>());
  }
}
