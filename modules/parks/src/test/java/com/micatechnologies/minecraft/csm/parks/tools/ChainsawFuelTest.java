package com.micatechnologies.minecraft.csm.parks.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class ChainsawFuelTest {

  @Test
  void onlyWholeItemsThatFitGoIn() {
    assertTrue(ChainsawFuel.accepts(0, 1600));
    assertTrue(ChainsawFuel.accepts(0, ChainsawFuel.CAPACITY), "a lava bucket fills an empty saw");
    assertFalse(ChainsawFuel.accepts(1, ChainsawFuel.CAPACITY), "but not a part-full one");
    assertFalse(ChainsawFuel.accepts(ChainsawFuel.CAPACITY - 100, 1600));
    assertFalse(ChainsawFuel.accepts(0, 0), "a thing a furnace will not burn is not fuel");
  }

  @Test
  void burningNeverGoesNegative() {
    assertEquals(1580, ChainsawFuel.afterIdle(1600, 20));
    assertEquals(0, ChainsawFuel.afterIdle(10, 20));
    assertEquals(1600 - 3 * ChainsawFuel.PER_LOG, ChainsawFuel.afterCut(1600, 3));
    assertEquals(0, ChainsawFuel.afterCut(100, 1000));
    assertEquals(500, ChainsawFuel.afterCut(500, -4), "a negative count burns nothing");
  }

  @Test
  void aPieceOfCoalIsFortyLogs() {
    assertEquals(0, ChainsawFuel.afterCut(1600, 40));
    assertEquals(ChainsawFuel.PER_LOG, ChainsawFuel.afterCut(1600, 39));
  }

  @Test
  void percentRoundsALittleFuelUp() {
    assertEquals(0, ChainsawFuel.percent(0));
    assertEquals(1, ChainsawFuel.percent(1));
    assertEquals(100, ChainsawFuel.percent(ChainsawFuel.CAPACITY));
    assertEquals(50, ChainsawFuel.percent(ChainsawFuel.CAPACITY / 2));
  }

  @Test
  void itStartsOnTheThirdToFifthPull() {
    Set<Integer> seen = new HashSet<>();
    for (int roll = -5; roll < 10; roll++) {
      int pulls = ChainsawFuel.pullsToStart(roll);
      assertTrue(pulls >= 3 && pulls <= 5);
      seen.add(pulls);
    }
    assertEquals(3, seen.size());
  }

  @Test
  void brushPileCounts() {
    assertEquals(0, BrushPiles.count("NONE", 40));
    assertEquals(1, BrushPiles.count("FEW", 3));
    assertEquals(3, BrushPiles.count("FEW", 200));
    assertTrue(BrushPiles.count("MANY", 40) > BrushPiles.count("FEW", 40));
    assertEquals(8, BrushPiles.count("MANY", 500));
  }

  @Test
  void brushPilesGoOnlyWhereOpenAndOnePerColumn() {
    BlockPos stump = new BlockPos(10, 64, 10);
    // Open only on ground at y 64, west of the stump.
    List<BlockPos> piles = BrushPiles.choose(p -> p.getY() == 64 && p.getX() < 10, stump, 5,
        new Random(1));
    assertEquals(5, piles.size());
    Set<Long> columns = new HashSet<>();
    for (BlockPos p : piles) {
      assertTrue(p.getX() < 10 && p.getY() == 64);
      assertTrue(columns.add((long) p.getX() << 32 | (p.getZ() & 0xffffffffL)));
      assertTrue(Math.abs(p.getX() - 10) <= BrushPiles.RADIUS);
    }
    assertTrue(BrushPiles.choose(p -> false, stump, 5, new Random(1)).isEmpty());
  }
}
