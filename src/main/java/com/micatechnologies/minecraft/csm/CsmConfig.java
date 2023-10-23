package com.micatechnologies.minecraft.csm;

import java.io.File;
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
    generateWikiFiles = config.getBoolean(FIELD_KEY_GENERATE_WIKI_FILES, CATEGORY_WIKI,
        FIELD_DEFAULT_GENERATE_WIKI_FILES, FIELD_DESCRIPTION_GENERATE_WIKI_FILES);
    wikiFilesFolder = config.getString(FIELD_KEY_WIKI_FILES_FOLDER, CATEGORY_WIKI,
        FIELD_DEFAULT_WIKI_FILES_FOLDER, FIELD_DESCRIPTION_WIKI_FILES_FOLDER);

    if (config.hasChanged()) {
      config.save();
    }
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
}
