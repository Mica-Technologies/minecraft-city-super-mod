package com.micatechnologies.minecraft.csm.signage;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/**
 * The signage module's own settings, in {@code config/csm_signage.cfg}: whether a server offers
 * the ads in its {@code config/csm/ads/} folder, and whether a client accepts a server's ads.
 */
public final class SignageConfig {

  private static final String CATEGORY_SERVER = "server";
  private static final String CATEGORY_CLIENT = "client";

  private static boolean allowServerAds = true;
  private static boolean acceptServerAds = true;

  private SignageConfig() {
  }

  /** Reads the file, writing it with its defaults the first time. */
  static void load(File configDirectory) {
    Configuration config = new Configuration(new File(configDirectory, "csm_signage.cfg"));
    config.load();
    allowServerAds = config.getBoolean("allowServerAds", CATEGORY_SERVER, true,
        "Offer the PNG ads in config/csm/ads/ to every player, for the advertising boards. "
            + "At most " + ServerAds.MAX_ADS + " files, each at most "
            + (ServerAds.MAX_BYTES / 1024) + " KB and " + ServerAds.MAX_SIDE
            + " pixels a side; anything else is skipped with a message in the log.");
    acceptServerAds = config.getBoolean("acceptServerAds", CATEGORY_CLIENT, true,
        "Download the ads a server offers when a board shows one. Off, a board showing a "
            + "server ad shows the house ad instead, and nothing is downloaded.");
    if (config.hasChanged()) {
      config.save();
    }
  }

  public static boolean isServerAdsAllowed() {
    return allowServerAds;
  }

  public static boolean isServerAdsAccepted() {
    return acceptServerAds;
  }
}
