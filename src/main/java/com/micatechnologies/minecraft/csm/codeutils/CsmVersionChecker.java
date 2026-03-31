package com.micatechnologies.minecraft.csm.codeutils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.micatechnologies.minecraft.csm.CsmConstants;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Checks for mod updates on player world join by querying the GitHub releases API.
 * Runs the HTTP request on a background daemon thread to avoid blocking. Fails silently
 * on any error — no crash, no user-visible message on failure.
 */
@SideOnly(Side.CLIENT)
public class CsmVersionChecker {

  private static final String RELEASES_API_URL =
      "https://api.github.com/repos/Mica-Technologies/minecraft-city-super-mod/releases/latest";
  private static final String RELEASES_PAGE_URL =
      "https://github.com/Mica-Technologies/minecraft-city-super-mod/releases";
  private static final int TIMEOUT_MS = 5000;

  private static boolean hasChecked = false;

  /**
   * Initiates an async version check. Safe to call multiple times — only the first
   * call per JVM session actually performs the check.
   */
  public static void checkForUpdatesAsync() {
    if (hasChecked) return;
    hasChecked = true;

    Thread thread = new Thread(CsmVersionChecker::doCheck, "CSM-VersionCheck");
    thread.setDaemon(true);
    thread.start();
  }

  private static void doCheck() {
    try {
      String latestTag = fetchLatestReleaseTag();
      if (latestTag == null || latestTag.isEmpty()) return;

      String currentVersion = CsmConstants.MOD_VERSION;
      if (currentVersion == null || currentVersion.isEmpty()) return;

      // Extract the date prefix from the current version (strip -pre.* suffix if present)
      String currentDatePrefix = currentVersion.contains("-pre.")
          ? currentVersion.substring(0, currentVersion.indexOf("-pre."))
          : currentVersion;

      // Only notify if the latest release is strictly newer than the current date prefix
      if (latestTag.compareTo(currentDatePrefix) > 0) {
        notifyPlayer(latestTag);
      }
    } catch (Exception ignored) {
      // Fail silently — never crash, never show error to user
    }
  }

  private static String fetchLatestReleaseTag() throws Exception {
    URL url = new URL(RELEASES_API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    try {
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
      conn.setConnectTimeout(TIMEOUT_MS);
      conn.setReadTimeout(TIMEOUT_MS);

      if (conn.getResponseCode() != 200) return null;

      StringBuilder sb = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(conn.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
      }

      JsonObject json = new JsonParser().parse(sb.toString()).getAsJsonObject();
      if (json.has("tag_name")) {
        return json.get("tag_name").getAsString();
      }
      return null;
    } finally {
      conn.disconnect();
    }
  }

  private static void notifyPlayer(String latestVersion) {
    Minecraft.getMinecraft().addScheduledTask(() -> {
      EntityPlayer player = Minecraft.getMinecraft().player;
      if (player == null) return;

      ITextComponent msg = new TextComponentString("[CSM] A new version is available: "
          + latestVersion + " (you have " + CsmConstants.MOD_VERSION + ") ");
      msg.setStyle(new Style().setColor(TextFormatting.GOLD));

      ITextComponent link = new TextComponentString("[Download]");
      link.setStyle(new Style()
          .setColor(TextFormatting.GREEN)
          .setUnderlined(true)
          .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, RELEASES_PAGE_URL)));

      msg.appendSibling(link);
      player.sendMessage(msg);
    });
  }
}
