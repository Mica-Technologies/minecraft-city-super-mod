package com.micatechnologies.minecraft.csm.parks.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class StumpGrindingTest {

  private static StumpGrinding.Cells cells(Set<BlockPos> logs) {
    return logs::contains;
  }

  /** Soil everywhere at or below y = -1 that is not a log: the ground the tests stand in. */
  private static StumpGrinding.Cells soil(Set<BlockPos> logs) {
    return p -> p.getY() <= -1 && !logs.contains(p);
  }

  private static Set<BlockPos> grind(Set<BlockPos> logs, BlockPos stump) {
    return StumpGrinding.grind(cells(logs), soil(logs), stump);
  }

  private static BlockPos at(int x, int y, int z) {
    return new BlockPos(x, y, z);
  }

  @Test
  void aStandingTrunkIsNotAStump() {
    Set<BlockPos> logs = new HashSet<>();
    for (int y = 0; y < 5; y++) {
      logs.add(at(0, y, 0));
    }
    assertTrue(StumpGrinding.isStanding(cells(logs), at(0, 0, 0)));
    assertFalse(StumpGrinding.isStump(cells(logs), at(0, 0, 0)));
    assertTrue(grind(logs, at(0, 0, 0)).isEmpty());
    // The top of the trunk has nothing over it.
    assertTrue(StumpGrinding.isStump(cells(logs), at(0, 4, 0)));
  }

  @Test
  void aLogLeaningOverIsStillStanding() {
    Set<BlockPos> logs = new HashSet<>();
    logs.add(at(0, 0, 0));
    logs.add(at(1, 1, 1));
    assertTrue(StumpGrinding.isStanding(cells(logs), at(0, 0, 0)));
  }

  @Test
  void airIsNotAStump() {
    assertFalse(StumpGrinding.isStump(cells(new HashSet<>()), at(0, 0, 0)));
    assertTrue(grind(new HashSet<>(), at(0, 0, 0)).isEmpty());
  }

  @Test
  void theStumpAndItsRootsGo() {
    Set<BlockPos> logs = new HashSet<>();
    logs.add(at(0, 0, 0));                 // the stump
    logs.add(at(0, -1, 0));                // trunk below ground
    logs.add(at(1, -1, 0));                // roots spreading
    logs.add(at(2, -2, 0));
    logs.add(at(-1, -2, 1));               // diagonal
    logs.add(at(0, -1, 1));
    Set<BlockPos> ground = grind(logs, at(0, 0, 0));
    assertEquals(logs, ground);
    assertEquals(at(0, 0, 0), ground.iterator().next());
  }

  @Test
  void nothingPastTheBoxGoes() {
    Set<BlockPos> logs = new HashSet<>();
    logs.add(at(0, 0, 0));
    for (int x = 1; x <= 6; x++) {
      logs.add(at(x, -1, 0));
    }
    for (int y = -1; y >= -8; y--) {
      logs.add(at(0, y, 0));
    }
    Set<BlockPos> ground = grind(logs, at(0, 0, 0));
    assertTrue(ground.contains(at(StumpGrinding.REACH, -1, 0)));
    assertFalse(ground.contains(at(StumpGrinding.REACH + 1, -1, 0)));
    assertTrue(ground.contains(at(0, -StumpGrinding.DEPTH, 0)));
    assertFalse(ground.contains(at(0, -StumpGrinding.DEPTH - 1, 0)));
  }

  @Test
  void nothingAboveTheStumpGoes() {
    Set<BlockPos> logs = new HashSet<>();
    logs.add(at(0, 0, 0));
    logs.add(at(2, 0, 0));
    logs.add(at(1, 0, 0));
    logs.add(at(3, 1, 0));                 // joined to (2, 0, 0) diagonally, but higher
    Set<BlockPos> ground = grind(logs, at(0, 0, 0));
    assertFalse(ground.contains(at(3, 1, 0)));
  }

  @Test
  void aNeighbouringTreeIsNotUndercut() {
    Set<BlockPos> logs = new HashSet<>();
    logs.add(at(0, 0, 0));                 // the stump
    logs.add(at(0, -1, 0));
    logs.add(at(1, -1, 0));                // a root running under the next tree
    logs.add(at(2, -1, 0));
    for (int y = 0; y < 6; y++) {
      logs.add(at(2, y, 0));               // the next tree, still standing
    }
    logs.add(at(3, -2, 0));                // that tree's own root, joined only through it
    Set<BlockPos> ground = grind(logs, at(0, 0, 0));
    assertTrue(ground.contains(at(0, 0, 0)));
    assertTrue(ground.contains(at(0, -1, 0)));
    // (1, -1, 0) has the standing tree's foot (2, 0, 0) diagonally over it.
    assertFalse(ground.contains(at(1, -1, 0)));
    assertFalse(ground.contains(at(2, -1, 0)));
    assertFalse(ground.contains(at(2, 0, 0)));
    assertFalse(ground.contains(at(3, -2, 0)));
  }

  @Test
  void aWideStumpGoesAColumnAtATime() {
    // Above ground only the clicked column goes, and below it nothing another column still
    // stands over, even diagonally: here, only the clicked log itself.
    Set<BlockPos> logs = new HashSet<>();
    for (int x = 0; x < 2; x++) {
      for (int z = 0; z < 2; z++) {
        logs.add(at(x, 0, z));
        logs.add(at(x, -1, z));
      }
    }
    Set<BlockPos> ground = grind(logs, at(0, 0, 0));
    Set<BlockPos> only = new HashSet<>();
    only.add(at(0, 0, 0));
    assertEquals(only, ground);
  }

  @Test
  void aLogWallGivesUpOneColumnAtMost() {
    // A wall three logs high and five long, standing on the soil: its top logs have nothing over
    // them, but the wall beside the clicked log stands over the logs under it, so only the
    // clicked log goes.
    Set<BlockPos> logs = new HashSet<>();
    for (int x = 0; x < 5; x++) {
      for (int y = 0; y < 3; y++) {
        logs.add(at(x, y, 0));
      }
    }
    Set<BlockPos> ground = grind(logs, at(2, 2, 0));
    Set<BlockPos> only = new HashSet<>();
    only.add(at(2, 2, 0));
    assertEquals(only, ground);
  }

  @Test
  void aLoneStumpGoesWholeColumn() {
    Set<BlockPos> logs = new HashSet<>();
    for (int y = 0; y < 3; y++) {
      logs.add(at(0, y, 0));
    }
    assertEquals(logs, grind(logs, at(0, 2, 0)));
  }

  @Test
  void aLogOnAFloorIsNoStump() {
    // Standing on something that is not soil (a floor, a foundation) or on nothing at all.
    Set<BlockPos> logs = new HashSet<>();
    logs.add(at(0, 5, 0));
    assertEquals(StumpGrinding.NOT_GROUNDED,
        StumpGrinding.groundLevel(cells(logs), soil(logs), at(0, 5, 0)));
    assertTrue(grind(logs, at(0, 5, 0)).isEmpty());
  }

  @Test
  void aStumpTooTallIsNoStump() {
    Set<BlockPos> logs = new HashSet<>();
    // MAX_HEIGHT + 1 logs on the soil (y = -1): too tall to be a stump.
    for (int y = 0; y <= StumpGrinding.MAX_HEIGHT; y++) {
      logs.add(at(0, y, 0));
    }
    assertTrue(grind(logs, at(0, StumpGrinding.MAX_HEIGHT, 0)).isEmpty());
    // One log lower, the same column is a stump, and goes whole.
    logs.remove(at(0, StumpGrinding.MAX_HEIGHT, 0));
    assertEquals(StumpGrinding.MAX_HEIGHT,
        grind(logs, at(0, StumpGrinding.MAX_HEIGHT - 1, 0)).size());
  }
}
