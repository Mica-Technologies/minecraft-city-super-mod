package com.micatechnologies.minecraft.csm.novelties;

import java.util.Random;

/**
 * Base class for the games that run inside an arcade cabinet. A game owns nothing but its own
 * simulation: {@code ArcadeGui} drives it at a fixed {@link #TICK_RATE}, hands it an
 * {@link ArcadeInput} for the control panel, and gives it an {@link ArcadeScreen} to draw on. Games
 * are created fresh per play session and are pure client-side — only the final score ever leaves the
 * client, and only through {@link ArcadeHighScorePacket}.
 *
 * <p>Each game declares its own playfield size in whatever units suit it; the screen letterboxes
 * that into the cabinet bezel, so the upright titles can be tall and the vector titles wide without
 * either having to care about the player's resolution.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public abstract class ArcadeGame {

  /**
   * The fixed simulation rate, in steps per second. Frames are decoupled from this — a slow frame
   * runs several steps, a fast one may run none — so gameplay speed does not follow frame rate.
   *
   * @since 1.0
   */
  public static final int TICK_RATE = 60;

  /**
   * The largest score a game will count up to, matching what a cabinet is willing to store.
   *
   * @since 1.0
   */
  public static final int MAX_SCORE = TileEntityArcadeCabinet.MAX_SCORE;

  /**
   * The number of lives a game starts with.
   *
   * @since 1.0
   */
  protected static final int STARTING_LIVES = 3;

  /**
   * Source of randomness for the game. One per instance, so two cabinets played at once do not
   * share a sequence.
   *
   * @since 1.0
   */
  protected final Random random = new Random();

  /**
   * The player's current score.
   *
   * @since 1.0
   */
  protected int score;

  /**
   * The number of lives remaining.
   *
   * @since 1.0
   */
  protected int lives = STARTING_LIVES;

  /**
   * The current level or wave, starting at one.
   *
   * @since 1.0
   */
  protected int level = 1;

  /**
   * Whether the run has ended.
   *
   * @since 1.0
   */
  protected boolean gameOver;

  /**
   * The number of simulation steps taken since the game was reset. Games use this for animation
   * phases and timed spawns.
   *
   * @since 1.0
   */
  protected long ticks;

  /**
   * Retrieves the game's display title, shown on the cabinet's attract screen.
   *
   * @return the game title
   *
   * @since 1.0
   */
  public abstract String getTitle();

  /**
   * Retrieves a one-line summary of the game's controls, shown on the attract screen.
   *
   * @return the control summary
   *
   * @since 1.0
   */
  public abstract String getControls();

  /**
   * Retrieves the width of the game's playfield, in the game's own units.
   *
   * @return the playfield width
   *
   * @since 1.0
   */
  public abstract float getPlayfieldWidth();

  /**
   * Retrieves the height of the game's playfield, in the game's own units.
   *
   * @return the playfield height
   *
   * @since 1.0
   */
  public abstract float getPlayfieldHeight();

  /**
   * Returns the game to its starting state, ready for a new run.
   *
   * @since 1.0
   */
  public void reset() {
    score = 0;
    lives = STARTING_LIVES;
    level = 1;
    gameOver = false;
    ticks = 0;
    resetGame();
  }

  /**
   * Hook for subclasses to clear their own state when a new run starts. Called by {@link #reset()}
   * after the shared counters have been cleared.
   *
   * @since 1.0
   */
  protected abstract void resetGame();

  /**
   * Advances the simulation by one fixed step.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  public final void step(ArcadeInput input) {
    if (gameOver) {
      return;
    }
    ticks++;
    update(input);
  }

  /**
   * Hook for subclasses to advance their simulation by one step.
   *
   * @param input the state of the cabinet's controls
   *
   * @since 1.0
   */
  protected abstract void update(ArcadeInput input);

  /**
   * Draws the current frame.
   *
   * @param screen the screen to draw on
   *
   * @since 1.0
   */
  public abstract void render(ArcadeScreen screen);

  /**
   * Retrieves the player's current score.
   *
   * @return the score
   *
   * @since 1.0
   */
  public int getScore() {
    return score;
  }

  /**
   * Retrieves the number of lives remaining.
   *
   * @return the remaining lives
   *
   * @since 1.0
   */
  public int getLives() {
    return lives;
  }

  /**
   * Retrieves the current level or wave.
   *
   * @return the level number
   *
   * @since 1.0
   */
  public int getLevel() {
    return level;
  }

  /**
   * Retrieves whether the run has ended.
   *
   * @return {@code true} if the game is over
   *
   * @since 1.0
   */
  public boolean isGameOver() {
    return gameOver;
  }

  /**
   * Adds to the player's score, clamping at {@link #MAX_SCORE}.
   *
   * @param points the points to award
   *
   * @since 1.0
   */
  protected void addScore(int points) {
    score = (int) Math.min(MAX_SCORE, (long) score + points);
  }

  /**
   * Removes a life and ends the run if that was the last one.
   *
   * @return {@code true} if the run ended
   *
   * @since 1.0
   */
  protected boolean loseLife() {
    lives--;
    if (lives <= 0) {
      lives = 0;
      gameOver = true;
      return true;
    }
    return false;
  }

  /**
   * Wraps a coordinate into the range {@code [0, limit)}, as the vector cabinets' screens do.
   *
   * @param value the coordinate to wrap
   * @param limit the exclusive upper bound
   *
   * @return the wrapped coordinate
   *
   * @since 1.0
   */
  protected static float wrap(float value, float limit) {
    if (value < 0F) {
      return value + limit * (float) (Math.floor(-value / limit) + 1);
    }
    if (value >= limit) {
      return value - limit * (float) Math.floor(value / limit);
    }
    return value;
  }

  /**
   * Clamps a value into an inclusive range.
   *
   * @param value the value to clamp
   * @param min   the lower bound
   * @param max   the upper bound
   *
   * @return the clamped value
   *
   * @since 1.0
   */
  protected static float clamp(float value, float min, float max) {
    return value < min ? min : (value > max ? max : value);
  }
}
