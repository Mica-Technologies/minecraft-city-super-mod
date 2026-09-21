package com.micatechnologies.minecraft.csm.signage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ads a server supplies itself: PNGs its admin drops in {@code config/csm/ads/}, sent to each
 * client that shows one, so a server can run its own ads without a resource pack.
 *
 * <p>Nothing here trusts the file or the network. On the server a file is refused before it is
 * decoded if its PNG header claims more than {@link #MAX_SIDE} pixels a side, if it is over
 * {@link #MAX_BYTES}, or if there are already {@link #MAX_ADS}; on the client the same header
 * check and a SHA-256 check run again before a downloaded image is decoded, so a server cannot
 * make a client decode something the server itself would have refused. Ids are made from file
 * names but never used as paths: the client caches an image under its hash alone.</p>
 */
public final class ServerAds {

  /** The most ads a server offers. */
  public static final int MAX_ADS = 64;
  /** The largest file, in bytes. */
  public static final int MAX_BYTES = 1024 * 1024;
  /** The largest side, in pixels. */
  public static final int MAX_SIDE = 1024;
  /** The largest piece of an image one packet carries. */
  public static final int CHUNK = 30000;
  /** The id every server ad's id starts with, so it never meets a bundled ad's. */
  public static final String PREFIX = "server_";
  /** The longest id and the longest name a catalogue carries. */
  public static final int MAX_ID = 48;
  public static final int MAX_NAME = 64;

  private static final Logger LOGGER = LogManager.getLogger("csm_signage");
  private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A,
      0x0A};

  /** One ad in a server's catalogue. */
  public static final class Ad {

    public final String id;
    public final String name;
    public final byte[] hash;
    public final int width;
    public final int height;
    public final int length;
    /** The file itself; on the server only. */
    @Nullable
    final byte[] bytes;

    public Ad(String id, String name, byte[] hash, int width, int height, int length,
        @Nullable byte[] bytes) {
      this.id = id;
      this.name = name;
      this.hash = hash;
      this.width = width;
      this.height = height;
      this.length = length;
      this.bytes = bytes;
    }

    /** The hash as lower-case hex: the client's cache file name. */
    public String hex() {
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    }
  }

  private static volatile List<Ad> serverCatalogue = Collections.emptyList();

  private ServerAds() {
  }

  /** The catalogue this server offers; empty on a client that is not also the server. */
  public static List<Ad> catalogue() {
    return serverCatalogue;
  }

  /**
   * Reads {@code folder} into the catalogue, or empties it when {@code enabled} is false. Called
   * as the server starts. Every file that is refused says why in the log.
   */
  public static void load(File folder, boolean enabled) {
    List<Ad> ads = new ArrayList<>();
    if (enabled) {
      if (!folder.isDirectory() && !folder.mkdirs()) {
        LOGGER.warn("Could not create the server ad folder {}", folder);
      }
      File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT)
          .endsWith(".png"));
      if (files != null) {
        Arrays.sort(files);
        for (File file : files) {
          if (ads.size() >= MAX_ADS) {
            LOGGER.warn("More than {} server ads; {} and later are ignored", MAX_ADS,
                file.getName());
            break;
          }
          Ad ad = read(file, ads);
          if (ad != null) {
            ads.add(ad);
          }
        }
      }
      LOGGER.info("Offering {} server ads from {}", ads.size(), folder);
    }
    serverCatalogue = Collections.unmodifiableList(ads);
    AdLibrary.get().setServerAds(entries(ads));
  }

  /** Forgets the catalogue, as the server stops. */
  public static void clear() {
    serverCatalogue = Collections.emptyList();
    AdLibrary.get().setServerAds(Collections.emptyList());
  }

  @Nullable
  private static Ad read(File file, List<Ad> sofar) {
    try {
      if (file.length() > MAX_BYTES) {
        LOGGER.warn("Server ad {} is over {} bytes; ignored", file.getName(), MAX_BYTES);
        return null;
      }
      byte[] bytes = Files.readAllBytes(file.toPath());
      int[] size = pngSize(bytes);
      if (size == null) {
        LOGGER.warn("Server ad {} is not a PNG, or is over {} pixels a side; ignored",
            file.getName(), MAX_SIDE);
        return null;
      }
      if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
        LOGGER.warn("Server ad {} could not be decoded; ignored", file.getName());
        return null;
      }
      String id = idFor(file.getName());
      for (Ad other : sofar) {
        if (other.id.equals(id)) {
          LOGGER.warn("Server ad {} has the same id as another ({}); ignored", file.getName(),
              id);
          return null;
        }
      }
      return new Ad(id, nameFor(file.getName()), sha256(bytes), size[0], size[1], bytes.length,
          bytes);
    } catch (IOException | RuntimeException e) {
      LOGGER.warn("Server ad {} could not be read; ignored", file.getName(), e);
      return null;
    }
  }

  /** The library entries for a catalogue. */
  static List<AdEntry> entries(List<Ad> ads) {
    List<AdEntry> out = new ArrayList<>();
    for (Ad ad : ads) {
      out.add(AdEntry.server(ad));
    }
    return out;
  }

  // --- pure helpers, tested ---------------------------------------------------------------------

  /**
   * The width and height a PNG's header declares, or {@code null} if the bytes are not a PNG or
   * declare more than {@link #MAX_SIDE} a side. Read from the header alone, before anything is
   * decoded: a tiny file can declare an enormous image.
   */
  @Nullable
  public static int[] pngSize(byte[] bytes) {
    if (bytes == null || bytes.length < 24) {
      return null;
    }
    for (int i = 0; i < PNG_SIGNATURE.length; i++) {
      if (bytes[i] != PNG_SIGNATURE[i]) {
        return null;
      }
    }
    if (bytes[12] != 'I' || bytes[13] != 'H' || bytes[14] != 'D' || bytes[15] != 'R') {
      return null;
    }
    long w = readInt(bytes, 16) & 0xFFFFFFFFL;
    long h = readInt(bytes, 20) & 0xFFFFFFFFL;
    if (w < 1 || h < 1 || w > MAX_SIDE || h > MAX_SIDE) {
      return null;
    }
    return new int[]{(int) w, (int) h};
  }

  private static int readInt(byte[] b, int at) {
    return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16) | ((b[at + 2] & 0xFF) << 8)
        | (b[at + 3] & 0xFF);
  }

  /** An ad id from a file name: {@link #PREFIX} and the name's letters and digits, lower case. */
  public static String idFor(String fileName) {
    String base = fileName.replaceAll("(?i)\\.png$", "").toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    if (base.isEmpty()) {
      base = "ad";
    }
    String id = PREFIX + base;
    return id.length() > MAX_ID ? id.substring(0, MAX_ID) : id;
  }

  /** What the picker calls a server ad: the file name, without its extension. */
  public static String nameFor(String fileName) {
    String name = clean(fileName.replaceAll("(?i)\\.png$", ""));
    return name.isEmpty() ? "Server ad" : name;
  }

  /**
   * A name from the network or a file, made safe to draw: printable characters only, no
   * formatting codes, at most {@link #MAX_NAME} long.
   */
  public static String clean(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length() && sb.length() < MAX_NAME; i++) {
      char c = s.charAt(i);
      if (c == '§') {
        // A formatting code is the section sign and the character after it: drop both.
        i++;
      } else if (c >= 0x20 && c != 0x7F) {
        sb.append(c);
      }
    }
    return sb.toString().trim();
  }

  /** Whether an id from the network is one a server may use. */
  public static boolean validId(String id) {
    return id.length() <= MAX_ID && id.startsWith(PREFIX)
        && id.substring(PREFIX.length()).matches("[a-z0-9_]+");
  }

  public static byte[] sha256(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
