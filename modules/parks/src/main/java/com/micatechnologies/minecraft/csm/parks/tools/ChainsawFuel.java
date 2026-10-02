package com.micatechnologies.minecraft.csm.parks.tools;

/**
 * The chainsaw's fuel arithmetic, in furnace burn ticks: what a furnace would burn an item for is
 * what it puts in the saw's tank. Pure, so it is tested without a game ({@code ChainsawFuelTest}).
 *
 * <p>A piece of coal (1,600 ticks) runs the saw for 80 seconds idling, or fells 40 logs. The tank
 * holds a lava bucket's worth, so a lava bucket only goes into an empty saw.</p>
 *
 * @since 2026.10
 */
public final class ChainsawFuel {

  /** The most the tank holds: a lava bucket. */
  public static final int CAPACITY = 20000;
  /** Burned per tick while the saw runs in a player's hand. */
  public static final int IDLE_PER_TICK = 1;
  /** Burned per log felled. */
  public static final int PER_LOG = 40;

  private ChainsawFuel() {
  }

  /**
   * Whether an item worth {@code burnTime} goes in on top of {@code fuel}: only whole items, and
   * only while it fits.
   */
  public static boolean accepts(int fuel, int burnTime) {
    return burnTime > 0 && fuel >= 0 && (long) fuel + burnTime <= CAPACITY;
  }

  /** The fuel after idling {@code ticks} ticks, never below zero. */
  public static int afterIdle(int fuel, int ticks) {
    return Math.max(0, fuel - Math.max(0, ticks) * IDLE_PER_TICK);
  }

  /** The fuel after felling {@code logs} logs, never below zero. */
  public static int afterCut(int fuel, int logs) {
    return (int) Math.max(0L, fuel - (long) Math.max(0, logs) * PER_LOG);
  }

  /** The fuel as a whole percentage of the tank, rounding a little fuel up to 1%. */
  public static int percent(int fuel) {
    if (fuel <= 0) {
      return 0;
    }
    return Math.max(1, Math.min(100, (int) ((long) fuel * 100 / CAPACITY)));
  }

  /** How many pulls of the cord start a cold saw, from a roll of 0, 1 or 2: three to five. */
  public static int pullsToStart(int roll) {
    return 3 + Math.floorMod(roll, 3);
  }
}
