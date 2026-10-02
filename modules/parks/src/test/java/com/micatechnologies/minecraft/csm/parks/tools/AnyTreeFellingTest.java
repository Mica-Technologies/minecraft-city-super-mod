package com.micatechnologies.minecraft.csm.parks.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class AnyTreeFellingTest {

  private static AnyTreeFelling.Cells cells(Map<BlockPos, AnyTreeFelling.Kind> world) {
    return p -> world.getOrDefault(p, AnyTreeFelling.Kind.OTHER);
  }

  /** A vanilla oak: a trunk of {@code height} logs at x, z and a 5 x 5 crown round its top. */
  private static void oak(Map<BlockPos, AnyTreeFelling.Kind> world, int x, int z, int height) {
    for (int y = 0; y < height; y++) {
      world.put(new BlockPos(x, y, z), AnyTreeFelling.Kind.LOG);
    }
    for (int y = height - 2; y <= height; y++) {
      for (int dx = -2; dx <= 2; dx++) {
        for (int dz = -2; dz <= 2; dz++) {
          BlockPos p = new BlockPos(x + dx, y, z + dz);
          world.putIfAbsent(p, AnyTreeFelling.Kind.LEAVES);
        }
      }
    }
  }

  @Test
  void cuttingTheBaseFellsTrunkAndCrown() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    oak(world, 0, 0, 6);
    BlockPos cut = BlockPos.ORIGIN;
    world.remove(cut);
    AnyTreeFelling.Result r = AnyTreeFelling.fell(cells(world), cut);
    assertEquals(5, r.logs.size());
    long leaves = world.values().stream().filter(k -> k == AnyTreeFelling.Kind.LEAVES).count();
    assertEquals(leaves, r.leaves.size());
  }

  @Test
  void nothingBelowTheCutFalls() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    oak(world, 0, 0, 7);
    BlockPos cut = new BlockPos(0, 2, 0);
    world.remove(cut);
    AnyTreeFelling.Result r = AnyTreeFelling.fell(cells(world), cut);
    assertFalse(r.logs.contains(new BlockPos(0, 1, 0)));
    assertFalse(r.logs.contains(BlockPos.ORIGIN));
    assertEquals(4, r.logs.size());
  }

  @Test
  void aLogWallWithoutLeavesIsABuild() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    for (int x = 0; x < 6; x++) {
      for (int y = 0; y < 4; y++) {
        world.put(new BlockPos(x, y, 0), AnyTreeFelling.Kind.LOG);
      }
    }
    BlockPos cut = new BlockPos(2, 0, 0);
    world.remove(cut);
    assertTrue(AnyTreeFelling.fell(cells(world), cut).isEmpty());
  }

  @Test
  void tooManyLogsIsLeftStanding() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    oak(world, 0, 0, AnyTreeFelling.MAX_LOGS + 10);
    BlockPos cut = BlockPos.ORIGIN;
    world.remove(cut);
    assertTrue(AnyTreeFelling.fell(cells(world), cut).isEmpty());
  }

  @Test
  void aNeighbouringTreeKeepsItsLeavesAndLogs() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    oak(world, 0, 0, 6);
    oak(world, 4, 0, 6);
    BlockPos cut = BlockPos.ORIGIN;
    world.remove(cut);
    AnyTreeFelling.Result r = AnyTreeFelling.fell(cells(world), cut);
    for (BlockPos p : r.logs) {
      assertEquals(0, p.getX(), "only the cut tree's trunk falls: " + p);
    }
    for (BlockPos p : r.leaves) {
      assertTrue(p.getX() < 2, "a leaf near the other tree was taken: " + p);
    }
    assertFalse(r.leaves.contains(new BlockPos(3, 5, 0)));
    assertTrue(r.leaves.contains(new BlockPos(-2, 5, -2)), "the far side of the crown goes");
  }

  @Test
  void placedLeavesDoNotMakeABuildATree() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    for (int y = 0; y < 4; y++) {
      world.put(new BlockPos(0, y, 0), AnyTreeFelling.Kind.LOG);
    }
    // A player's leaves read as OTHER, so the column has no leaves at all.
    BlockPos cut = BlockPos.ORIGIN;
    world.remove(cut);
    assertTrue(AnyTreeFelling.fell(cells(world), cut).isEmpty());
  }

  @Test
  void spreadIsBounded() {
    Map<BlockPos, AnyTreeFelling.Kind> world = new HashMap<>();
    oak(world, 0, 0, 6);
    // A long horizontal run of logs off the trunk, as a log fence would be.
    for (int x = 1; x <= AnyTreeFelling.MAX_SPREAD + 6; x++) {
      world.put(new BlockPos(x, 1, 0), AnyTreeFelling.Kind.LOG);
    }
    BlockPos cut = BlockPos.ORIGIN;
    world.remove(cut);
    AnyTreeFelling.Result r = AnyTreeFelling.fell(cells(world), cut);
    for (BlockPos p : r.logs) {
      assertTrue(p.getX() <= AnyTreeFelling.MAX_SPREAD, "past the spread: " + p);
    }
  }
}
