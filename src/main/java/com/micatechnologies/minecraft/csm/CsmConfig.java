package com.micatechnologies.minecraft.csm;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

/**
 * The configuration for the mod.
 *
 * @version 1.0
 * @since 2023.3.1
 */
public class CsmConfig {

  /**
   * The category for general configuration options.
   *
   * @since 1.0
   */
  private static final String CATEGORY_GENERAL = "general";

  /**
   * The category for wiki configuration options.
   *
   * @since 1.0
   */
  private static final String CATEGORY_WIKI = "wiki";

  /**
   * The category for traffic pole configuration options.
   */
  private static final String CATEGORY_TRAFFIC_POLES = "trafficPoles";

  /**
   * The configuration field name for the generateWikiFiles option.
   *
   * @since 1.0
   */
  private static final String FIELD_KEY_GENERATE_WIKI_FILES = "generateWikiFiles";

  /**
   * The configuration field name for the wikiFilesFolder option.
   *
   * @since 1.0
   */
  private static final String FIELD_KEY_WIKI_FILES_FOLDER = "wikiFilesFolder";

  /**
   * The configuration field comment for the generateWikiFiles option.
   *
   * @since 1.0
   */
  private static final String FIELD_DESCRIPTION_GENERATE_WIKI_FILES =
      "Set to true to enable generating wiki files for the mod.";

  /**
   * The configuration field comment for the wikiFilesFolder option.
   *
   * @since 1.0
   */
  private static final String FIELD_DESCRIPTION_WIKI_FILES_FOLDER =
      "The folder to generate wiki files in (relative to Minecraft install).";

  /**
   * The configuration field default value for the generateWikiFiles option.
   *
   * @since 1.0
   */
  private static final boolean FIELD_DEFAULT_GENERATE_WIKI_FILES = false;

  /**
   * The configuration field default value for the wikiFilesFolder option.
   *
   * @since 1.0
   */
  private static final String FIELD_DEFAULT_WIKI_FILES_FOLDER = "csmWiki";

  private static final String FIELD_KEY_ENABLE_STROBE_EFFECT = "enableStrobeEffect";
  private static final String FIELD_DESCRIPTION_ENABLE_STROBE_EFFECT =
      "Set to false to disable the visual strobe flash effect on fire alarm strobe devices.";
  private static final boolean FIELD_DEFAULT_ENABLE_STROBE_EFFECT = true;

  private static final String FIELD_KEY_ENABLE_UPDATE_CHECK = "enableUpdateCheck";
  private static final String FIELD_DESCRIPTION_ENABLE_UPDATE_CHECK =
      "Set to false to disable automatic update checking on world join.";
  private static final boolean FIELD_DEFAULT_ENABLE_UPDATE_CHECK = true;

  private static final String FIELD_KEY_ENABLE_THERMOSTAT_DISPLAY = "enableThermostatDisplay";
  private static final String FIELD_DESCRIPTION_ENABLE_THERMOSTAT_DISPLAY =
      "Set to true to enable the dynamic in-world display on HVAC thermostats showing time, "
          + "room temperature, and outside temperature.";
  private static final boolean FIELD_DEFAULT_ENABLE_THERMOSTAT_DISPLAY = true;

  private static final String FIELD_KEY_TRAFFIC_POLE_IGNORE_BLOCKS = "trafficPoleIgnoreBlocks";
  private static final String FIELD_DESCRIPTION_TRAFFIC_POLE_IGNORE_BLOCKS =
      "Additional block registry names that traffic poles should NOT visually connect/mount to, "
          + "beyond the mod's built-in list. Entries may be fully qualified "
          + "(\"modid:blockname\") or a bare name (\"blockname\") which is treated as "
          + "\"minecraft:<name>\". Invalid entries are logged and ignored. "
          + "Manageable in-game by ops via \"/csm poleignore add|remove <block>\".";
  private static final String[] FIELD_DEFAULT_TRAFFIC_POLE_IGNORE_BLOCKS = new String[0];

  /**
   * The configuration field value for the enableUpdateCheck option.
   */
  private static boolean enableStrobeEffect;

  private static boolean enableUpdateCheck;
  private static boolean enableThermostatDisplay;

  /**
   * The configuration field value for the generateWikiFiles option.
   *
   * @since 1.0
   */
  private static boolean generateWikiFiles;

  /**
   * The configuration field value for the wikiFilesFolder option.
   *
   * @since 1.0
   */
  private static String wikiFilesFolder;

  /**
   * Parsed, immutable set of registry IDs that traffic poles should skip when evaluating
   * adjacency. Rebuilt on every load/reload and every runtime add/remove, so the field reference
   * is what changes (replace-by-reference), not the set itself — safe to read concurrently without
   * a lock. Volatile guarantees the pole render path always observes the most recent reference.
   */
  private static volatile Set<ResourceLocation> trafficPoleIgnoreBlockIds =
      Collections.emptySet();

  /**
   * Monotonically incremented whenever the config (or any runtime-mutable piece of it) changes.
   * Consumers that cache anything derived from config can compare their last-seen value against
   * this to detect staleness without string-comparing the whole set.
   */
  private static volatile int configVersion = 0;

  /**
   * The configuration object. This is initialized by {@link #init(File)}, and should not be
   * accessed directly.
   *
   * @since 1.0
   */
  private static Configuration config;

  /**
   * Initializes the configuration object.
   *
   * @param configFile the configuration file
   *
   * @since 1.0
   */
  static void init(File configFile) {
    if (config == null) {
      config = new Configuration(configFile);
      loadConfig();
    }
  }

  /**
   * Loads the configuration from the configuration file.
   *
   * @since 1.0
   */
  private static void loadConfig() {
    enableStrobeEffect = config.getBoolean(FIELD_KEY_ENABLE_STROBE_EFFECT, CATEGORY_GENERAL,
        FIELD_DEFAULT_ENABLE_STROBE_EFFECT, FIELD_DESCRIPTION_ENABLE_STROBE_EFFECT);
    enableUpdateCheck = config.getBoolean(FIELD_KEY_ENABLE_UPDATE_CHECK, CATEGORY_GENERAL,
        FIELD_DEFAULT_ENABLE_UPDATE_CHECK, FIELD_DESCRIPTION_ENABLE_UPDATE_CHECK);
    enableThermostatDisplay = config.getBoolean(FIELD_KEY_ENABLE_THERMOSTAT_DISPLAY,
        CATEGORY_GENERAL, FIELD_DEFAULT_ENABLE_THERMOSTAT_DISPLAY,
        FIELD_DESCRIPTION_ENABLE_THERMOSTAT_DISPLAY);
    generateWikiFiles = config.getBoolean(FIELD_KEY_GENERATE_WIKI_FILES, CATEGORY_WIKI,
        FIELD_DEFAULT_GENERATE_WIKI_FILES, FIELD_DESCRIPTION_GENERATE_WIKI_FILES);
    wikiFilesFolder = config.getString(FIELD_KEY_WIKI_FILES_FOLDER, CATEGORY_WIKI,
        FIELD_DEFAULT_WIKI_FILES_FOLDER, FIELD_DESCRIPTION_WIKI_FILES_FOLDER);

    String[] rawIgnores = config.getStringList(FIELD_KEY_TRAFFIC_POLE_IGNORE_BLOCKS,
        CATEGORY_TRAFFIC_POLES, FIELD_DEFAULT_TRAFFIC_POLE_IGNORE_BLOCKS,
        FIELD_DESCRIPTION_TRAFFIC_POLE_IGNORE_BLOCKS);
    trafficPoleIgnoreBlockIds = parseBlockIds(rawIgnores);
    configVersion++;

    if (config.hasChanged()) {
      config.save();
    }
  }

  /**
   * Reloads the configuration from disk and updates all cached values. Safe to call at runtime
   * (for example, from the {@code /csm reloadconfig} command). No-op if the configuration has
   * not been initialized yet.
   */
  public static synchronized void reload() {
    if (config == null) {
      return;
    }
    config.load();
    loadConfig();
    Csm.getLogger().info("CSM configuration reloaded from disk.");
  }

  /**
   * Parses an array of user-supplied block id strings into a {@link ResourceLocation} set.
   * Bare names (no colon) are treated as {@code minecraft:<name>}. Invalid or empty entries are
   * logged and skipped; existence of the target block is not verified here (the caller checking
   * adjacency simply won't match a nonexistent id).
   */
  private static Set<ResourceLocation> parseBlockIds(String[] rawIds) {
    if (rawIds == null || rawIds.length == 0) {
      return Collections.emptySet();
    }
    Set<ResourceLocation> parsed = new HashSet<>();
    for (String raw : rawIds) {
      if (raw == null) {
        continue;
      }
      String trimmed = raw.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      try {
        ResourceLocation rl = trimmed.contains(":") ? new ResourceLocation(trimmed)
            : new ResourceLocation("minecraft", trimmed);
        parsed.add(rl);
      } catch (Exception e) {
        Csm.getLogger().warn(
            "Ignoring invalid traffic pole ignore block id in config: \"" + trimmed + "\"");
      }
    }
    return Collections.unmodifiableSet(parsed);
  }

  /**
   * Persists the current in-memory ignore set back to disk, sorted for stable diffs.
   */
  private static void persistTrafficPoleIgnoreBlocks(Set<ResourceLocation> ids) {
    String[] serialized = ids.stream().map(ResourceLocation::toString).sorted()
        .toArray(String[]::new);
    config.get(CATEGORY_TRAFFIC_POLES, FIELD_KEY_TRAFFIC_POLE_IGNORE_BLOCKS,
            FIELD_DEFAULT_TRAFFIC_POLE_IGNORE_BLOCKS,
            FIELD_DESCRIPTION_TRAFFIC_POLE_IGNORE_BLOCKS)
        .setValues(serialized);
    config.save();
  }

  /**
   * Retrieves whether wiki generation is enabled.
   *
   * @return {@code true} if wiki generation is enabled, {@code false} otherwise.
   *
   * @since 1.0
   */
  public static boolean isWikiGenerationEnabled() {
    return generateWikiFiles;
  }

  /**
   * Retrieves the wiki files folder.
   *
   * @return the wiki files folder
   *
   * @since 1.0
   */
  public static String getWikiFilesFolder() {
    return wikiFilesFolder;
  }

  /**
   * Retrieves whether the automatic update check is enabled.
   *
   * @return {@code true} if the update check is enabled, {@code false} otherwise.
   */
  public static boolean isUpdateCheckEnabled() {
    return enableUpdateCheck;
  }

  /**
   * Retrieves whether the visual strobe flash effect on fire alarm devices is enabled.
   *
   * @return {@code true} if the strobe effect is enabled, {@code false} otherwise.
   */
  public static boolean isStrobeEffectEnabled() {
    return enableStrobeEffect;
  }

  /**
   * Retrieves whether the in-world thermostat display (TESR) is enabled.
   *
   * @return {@code true} if the thermostat display is enabled, {@code false} otherwise.
   */
  public static boolean isThermostatDisplayEnabled() {
    return enableThermostatDisplay;
  }

  /**
   * Retrieves the current set of user-configured block registry names that traffic poles should
   * skip when evaluating adjacency. The returned set is immutable; callers must use
   * {@link #addTrafficPoleIgnoreBlock(ResourceLocation)} and
   * {@link #removeTrafficPoleIgnoreBlock(ResourceLocation)} to mutate.
   *
   * @return the immutable current ignore set (never {@code null})
   */
  public static Set<ResourceLocation> getTrafficPoleIgnoreBlockIds() {
    return trafficPoleIgnoreBlockIds;
  }

  /**
   * Retrieves the current config version. Increments on every load, reload, and runtime mutation.
   * Consumers can compare against a stored value to invalidate derived caches cheaply.
   */
  public static int getConfigVersion() {
    return configVersion;
  }

  /**
   * Adds a block registry id to the traffic-pole ignore set and persists to disk. Idempotent:
   * returns {@code false} if the id was already present or the config is not yet initialized.
   */
  public static synchronized boolean addTrafficPoleIgnoreBlock(ResourceLocation blockId) {
    if (config == null || blockId == null) {
      return false;
    }
    if (trafficPoleIgnoreBlockIds.contains(blockId)) {
      return false;
    }
    Set<ResourceLocation> updated = new HashSet<>(trafficPoleIgnoreBlockIds);
    updated.add(blockId);
    trafficPoleIgnoreBlockIds = Collections.unmodifiableSet(updated);
    configVersion++;
    persistTrafficPoleIgnoreBlocks(updated);
    return true;
  }

  /**
   * Removes a block registry id from the traffic-pole ignore set and persists to disk. Returns
   * {@code false} if the id was not present or the config is not yet initialized.
   */
  public static synchronized boolean removeTrafficPoleIgnoreBlock(ResourceLocation blockId) {
    if (config == null || blockId == null) {
      return false;
    }
    if (!trafficPoleIgnoreBlockIds.contains(blockId)) {
      return false;
    }
    Set<ResourceLocation> updated = new HashSet<>(trafficPoleIgnoreBlockIds);
    updated.remove(blockId);
    trafficPoleIgnoreBlockIds = Collections.unmodifiableSet(updated);
    configVersion++;
    persistTrafficPoleIgnoreBlocks(updated);
    return true;
  }

  /**
   * Convenience: parse a user-supplied block id string (with or without a namespace) into a
   * {@link ResourceLocation}, or {@code null} if unparseable. Bare names resolve to the
   * {@code minecraft} namespace.
   */
  public static ResourceLocation parseBlockId(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    try {
      return trimmed.contains(":") ? new ResourceLocation(trimmed)
          : new ResourceLocation("minecraft", trimmed);
    } catch (Exception e) {
      return null;
    }
  }
}
