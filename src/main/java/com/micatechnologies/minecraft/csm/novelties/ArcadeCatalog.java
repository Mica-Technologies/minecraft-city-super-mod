package com.micatechnologies.minecraft.csm.novelties;

/**
 * The list of games installed in the multi-game arcade cabinet, as identifiers and titles only.
 *
 * <p>This class deliberately holds no reference to any {@code ArcadeGame} class. The identifiers
 * travel over the network and are stored in the cabinet's tile entity, both of which happen on a
 * dedicated server — and the games' render methods take a client-only screen type. Keeping the
 * catalogue to plain strings means the server can validate and store scores without ever loading a
 * rendering class. {@code ArcadeGui} maps an identifier to an actual game, client-side.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public final class ArcadeCatalog {

  /**
   * The identifier of each installed game. Stored in cabinet NBT, so entries must not be renamed
   * once released.
   *
   * @since 1.0
   */
  public static final String[] GAME_IDS = {
      "streetsweeper",
      "highrise",
      "tunnelrunner",
      "pipepressure",
      "loadbalance",
      "salvagerun",
      "sitesurvey"
  };

  /**
   * The display title of each installed game, in the same order as {@link #GAME_IDS}.
   *
   * @since 1.0
   */
  public static final String[] GAME_TITLES = {
      "STREET SWEEPER",
      "HIGH RISE",
      "TUNNEL RUNNER",
      "PIPE PRESSURE",
      "LOAD BALANCE",
      "SALVAGE RUN",
      "SITE SURVEY"
  };

  /**
   * A one-line description of each installed game, in the same order as {@link #GAME_IDS}.
   *
   * @since 1.0
   */
  public static final String[] GAME_BLURBS = {
      "Clear every street before the traffic gets you",
      "Stack steel beams into the tallest tower you can",
      "Switch tracks at speed and pick up your riders",
      "Route the main before the header redlines",
      "Keep six feeders out of the red",
      "Tow cargo pods home past the tumbling scrap",
      "Find the survey markers before the clock runs out"
  };

  /**
   * Prevents instantiation of this utility class.
   *
   * @since 1.0
   */
  private ArcadeCatalog() {
  }

  /**
   * Retrieves whether the specified identifier names an installed game. Used to reject anything
   * unrecognised arriving from a client before it reaches cabinet NBT.
   *
   * @param gameId the identifier to check
   *
   * @return {@code true} if the identifier is in the catalogue
   *
   * @since 1.0
   */
  public static boolean isKnownGame(String gameId) {
    if (gameId == null) {
      return false;
    }
    for (String candidate : GAME_IDS) {
      if (candidate.equals(gameId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieves the display title of the specified game.
   *
   * @param gameId the game's identifier
   *
   * @return the game's title, or the identifier itself if it is not in the catalogue
   *
   * @since 1.0
   */
  public static String titleOf(String gameId) {
    for (int i = 0; i < GAME_IDS.length; i++) {
      if (GAME_IDS[i].equals(gameId)) {
        return GAME_TITLES[i];
      }
    }
    return String.valueOf(gameId);
  }
}
