package com.micatechnologies.minecraft.csm.hvac;

/**
 * What the thermal scanner needs to know about one block cell, and nothing more. The world
 * implementation ({@link HvacAirflow#worldSource}) answers from block states; the unit tests answer
 * from a hand-drawn grid, which is what lets the scanner be tested without a running game.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public interface ThermalCellSource {

  /** The cell is in a chunk that is not loaded, so nothing can be said about it. */
  byte UNLOADED = 0;

  /** Enclosed air: air cannot see the sky from here. A space is made of these. */
  byte AIR = 1;

  /** Air open to the sky. A space touching one of these has an opening to the outdoors. */
  byte SKY = 2;

  /** Something air does not pass through: a wall, floor, roof, window or closed door. */
  byte WALL = 3;

  /**
   * Classifies a cell.
   *
   * @return one of {@link #UNLOADED}, {@link #AIR}, {@link #SKY} or {@link #WALL}
   */
  byte classify(int x, int y, int z);

  /**
   * How readily heat passes through a {@link #WALL} cell, relative to stone (1.0). Glass passes
   * about twice as much, wool about a third.
   */
  float wallFactor(int x, int y, int z);
}
