package com.micatechnologies.minecraft.csm.buildingmaterials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The garage door's position is worked out from the world's total time, and a world's total time
 * does not stay small: a server a few years old is past three billion ticks. These hold the clock
 * to being right there, where a partial tick added as {@code long + float} moved the door in
 * steps of 256 ticks on every client while the server ran it correctly.
 */
class GarageDoorClockTest {

  /** The world time a real server was at when its doors were seen to jump. */
  private static final long OLD_WORLD = 3_803_291_727L;
  /** A six-block sectional door: its height and the quarter turn round the bend, at a block a second. */
  private static final long DURATION = 132L;

  private static double clock(long worldTime, float partialTicks) {
    return TileEntityGarageDoor.clock(worldTime, partialTicks);
  }

  @Test
  void aDoorMovesEveryTickInAnOldWorld() {
    double last = -1.0;
    for (long t = 0; t <= DURATION; t++) {
      double p = TileEntityGarageDoor.positionAt(0.0, 1, OLD_WORLD, DURATION,
          clock(OLD_WORLD + t, 0.0F));
      assertTrue(p > last, "the door stood still at tick " + t);
      assertEquals((double) t / DURATION, p, 1e-9);
      last = p;
    }
    assertEquals(1.0, last, 0.0);
  }

  @Test
  void thePartialTickCountsInAnOldWorld() {
    double whole = TileEntityGarageDoor.positionAt(0.0, 1, OLD_WORLD, DURATION,
        clock(OLD_WORLD + 10, 0.0F));
    double half = TileEntityGarageDoor.positionAt(0.0, 1, OLD_WORLD, DURATION,
        clock(OLD_WORLD + 10, 0.5F));
    assertEquals(0.5 / DURATION, half - whole, 1e-9);
  }

  @Test
  void aClosingDoorRunsDownAndStopsAtClosed() {
    assertEquals(0.5, TileEntityGarageDoor.positionAt(1.0, -1, OLD_WORLD, DURATION,
        clock(OLD_WORLD + DURATION / 2, 0.0F)), 1e-9);
    assertEquals(0.0, TileEntityGarageDoor.positionAt(1.0, -1, OLD_WORLD, DURATION,
        clock(OLD_WORLD + DURATION * 3, 0.0F)), 0.0);
  }

  @Test
  void aStoppedDoorStaysWhereItStopped() {
    assertEquals(0.4, TileEntityGarageDoor.positionAt(0.4, 0, OLD_WORLD, DURATION,
        clock(OLD_WORLD + 5_000, 0.25F)), 0.0);
  }

  /**
   * Why {@link #clock} is written the way it is: the same sum without the cast is a float, and
   * is wrong by tens of ticks. If this ever stops holding, the cast has stopped mattering.
   */
  @Test
  void theSameSumAsAFloatIsWrongByManyTicks() {
    long t = OLD_WORLD + 60;
    double asFloat = t + 0.5F;
    assertTrue(Math.abs(asFloat - clock(t, 0.5F)) > 20.0);
  }
}
