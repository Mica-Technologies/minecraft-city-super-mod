package com.micatechnologies.minecraft.csm.venues.av;

/**
 * A tile entity whose words and colours {@link GuiVenueText} edits and
 * {@link VenueTextPacket} carries: the ribbon board's scrolling text, the banners, the marquee, the
 * ballpark scoreboards and the outfield wall's distance markers. Lines are
 * free text, cleaned and cut by the holder; choices are small numbers (a colour index, a speed)
 * the holder clamps.
 *
 * @since 2026.10
 */
public interface IVenueTextHolder {

  /** The most lines any holder has, and the most choices: what a packet may carry. */
  int MAX_LINES = 7;
  int MAX_CHOICES = 3;

  /** How many lines of text. */
  int lineCount();

  /** The longest line {@code i} may be. */
  int maxLength(int i);

  /** Line {@code i}. */
  String getLine(int i);

  /** The lang key naming line {@code i} in the editor. */
  String lineCaption(int i);

  /** How many choices (colours, a speed). */
  int choiceCount();

  /** How many values choice {@code i} takes, 0 to n - 1. */
  int choiceRange(int i);

  /** Choice {@code i}'s value. */
  int getChoice(int i);

  /** The lang key of choice {@code i}'s button, with a {@code %s} for its value. */
  String choiceCaption(int i);

  /** The lang key naming value {@code v} of choice {@code i}. */
  String choiceValueKey(int i, int v);

  /**
   * Sets every line and choice, server side, and sends the change to the clients watching. The
   * holder cleans and clamps what it is given.
   *
   * @param lines   the lines; fewer than {@link #lineCount()} leaves the rest empty
   * @param choices the choices; fewer leaves the rest as they are
   */
  void applyText(String[] lines, int[] choices);
}
