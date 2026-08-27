package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards the tick phase stagger in {@link AbstractTickableTileEntity} against the failure that
 * originally hid in it.
 *
 * <p>The stagger exists so that many tile entities sharing a tick rate do not all fire on the same
 * world tick. It derives a phase from the block position's hash. {@code BlockPos.hashCode()} is
 * linear in x, y and z, so on a regular lattice -- a city street grid -- the hash advances by a
 * constant step and the phases collapse onto {@code rate / gcd(step, rate)} values. With
 * intersections 16 blocks apart that was five phases no matter whether the rate was 20, 40 or 80,
 * putting a fifth of every controller on one tick.</p>
 *
 * <p>These tests reproduce that lattice and assert the offsets actually spread. They deliberately
 * re-implement the hash and the mixer rather than reaching into the tile entity: constructing one
 * needs a live Minecraft world, and the property under test is pure arithmetic.</p>
 */
class TickPhaseOffsetTest {

  /** Mirrors {@code net.minecraft.util.math.BlockPos.hashCode()} for 1.12.2. */
  private static int blockPosHash(int x, int y, int z) {
    return (y + z * 31) * 31 + x;
  }

  /** Mirrors {@code AbstractTickableTileEntity.avalanche} (murmur3 fmix32). */
  private static int avalanche(int hash) {
    int h = hash;
    h ^= h >>> 16;
    h *= 0x85EBCA6B;
    h ^= h >>> 13;
    h *= 0xC2B2AE35;
    h ^= h >>> 16;
    return h;
  }

  private static long phase(int x, int y, int z, long tickRate) {
    long offset = Math.floorMod((long) avalanche(blockPosHash(x, y, z)), Integer.MAX_VALUE);
    return offset % tickRate;
  }

  /**
   * One controller per intersection on a 16-block street grid -- the layout the raw hash collapsed
   * on. Ten by ten is a modest downtown.
   */
  private static int[][] gridOfControllers() {
    int[][] positions = new int[100][3];
    int index = 0;
    for (int gx = 0; gx < 10; gx++) {
      for (int gz = 0; gz < 10; gz++) {
        positions[index][0] = gx * 16 + 4;
        positions[index][1] = 5;
        positions[index][2] = gz * 16 + 4;
        index++;
      }
    }
    return positions;
  }

  private static int distinctPhases(int[][] positions, long tickRate) {
    Set<Long> phases = new HashSet<>();
    for (int[] pos : positions) {
      phases.add(phase(pos[0], pos[1], pos[2], tickRate));
    }
    return phases.size();
  }

  private static int worstTickLoad(int[][] positions, long tickRate) {
    Map<Long, Integer> counts = new HashMap<>();
    for (int[] pos : positions) {
      long p = phase(pos[0], pos[1], pos[2], tickRate);
      counts.put(p, counts.getOrDefault(p, 0) + 1);
    }
    int worst = 0;
    for (int count : counts.values()) {
      worst = Math.max(worst, count);
    }
    return worst;
  }

  @Test
  void gridAlignedControllersSpreadAcrossMostOfTheTickPeriod() {
    int[][] grid = gridOfControllers();
    // The raw hash gave 5 of 20 here. Anything at or below that is the collapse coming back.
    assertTrue(distinctPhases(grid, 20L) >= 15,
        "expected the phases to spread across most of a 20-tick period, got "
            + distinctPhases(grid, 20L));
    assertTrue(distinctPhases(grid, 40L) >= 28,
        "expected a wide spread across a 40-tick period, got " + distinctPhases(grid, 40L));
    assertTrue(distinctPhases(grid, 80L) >= 45,
        "expected a wide spread across an 80-tick period, got " + distinctPhases(grid, 80L));
  }

  @Test
  void noSingleTickCarriesADisproportionateShareOfTheGrid() {
    int[][] grid = gridOfControllers();
    // The raw hash put 20 of 100 controllers on one tick at every rate tested.
    assertTrue(worstTickLoad(grid, 20L) <= 14,
        "one tick carried " + worstTickLoad(grid, 20L) + " of 100 controllers at rate 20");
    assertTrue(worstTickLoad(grid, 40L) <= 8,
        "one tick carried " + worstTickLoad(grid, 40L) + " of 100 controllers at rate 40");
  }

  @Test
  void controllersInAStraightLineAlsoSpread() {
    // A single long avenue is the degenerate lattice: only one coordinate varies.
    int[][] avenue = new int[64][3];
    for (int i = 0; i < avenue.length; i++) {
      avenue[i][0] = i * 16;
      avenue[i][1] = 5;
      avenue[i][2] = 0;
    }
    assertTrue(distinctPhases(avenue, 20L) >= 15,
        "expected an avenue of controllers to spread, got " + distinctPhases(avenue, 20L));
  }

  @Test
  void offsetIsStableForAGivenPosition() {
    // The stagger is only useful if a tile entity keeps the same phase across reloads.
    assertTrue(phase(37, 9, -114, 20L) == phase(37, 9, -114, 20L));
  }

  @Test
  void offsetIsNeverNegative() {
    // A negative offset would break the modulo gate in update() for some positions.
    for (int x = -2000; x <= 2000; x += 137) {
      for (int z = -2000; z <= 2000; z += 211) {
        long offset = Math.floorMod((long) avalanche(blockPosHash(x, 64, z)), Integer.MAX_VALUE);
        assertTrue(offset >= 0L, "negative offset at " + x + "," + z);
      }
    }
  }
}
