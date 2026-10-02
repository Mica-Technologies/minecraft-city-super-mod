package com.micatechnologies.minecraft.csm.parks.planting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.parks.trees.TreeFelling;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class TreeGeneratorsTest {

  private static final EnumFacing[] FACINGS = {EnumFacing.NORTH, EnumFacing.EAST,
      EnumFacing.SOUTH, EnumFacing.WEST};

  /** A building face two blocks east of the trunk, running north-south and up forever. */
  static final TreeSpace WALL_EAST = rel -> rel.getX() >= 2 ? TreeSpace.Cell.BLOCKED
      : TreeSpace.Cell.FREE;

  /** A building corner: an east face and a north face, both two blocks off. */
  static final TreeSpace CORNER = rel -> rel.getX() >= 2 || rel.getZ() <= -2
      ? TreeSpace.Cell.BLOCKED : TreeSpace.Cell.FREE;

  private static final TreeSpace[] SPACES = {TreeSpace.OPEN, WALL_EAST, CORNER};

  static Set<BlockPos> logs(TreePlan plan) {
    return cells(plan, TreePlan.Kind.LOG);
  }

  static Set<BlockPos> cells(TreePlan plan, TreePlan.Kind kind) {
    Set<BlockPos> cells = new HashSet<>();
    for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      if (e.getValue().kind == kind) {
        cells.add(e.getKey());
      }
    }
    return cells;
  }

  /**
   * Face and edge-diagonal neighbours. The log kit joins corner diagonals too, but only so a
   * hand-built limb holds together: the generators split a corner step in two, and this holds them
   * to it, so a planted tree's shape and quad count never lean on a corner bridge.
   */
  private static boolean joined(BlockPos a, BlockPos b) {
    int dx = Math.abs(a.getX() - b.getX());
    int dy = Math.abs(a.getY() - b.getY());
    int dz = Math.abs(a.getZ() - b.getZ());
    int moved = (dx > 0 ? 1 : 0) + (dy > 0 ? 1 : 0) + (dz > 0 ? 1 : 0);
    return dx <= 1 && dy <= 1 && dz <= 1 && moved >= 1 && moved <= 2;
  }

  private static void assertConnected(TreePlan plan, String what) {
    Set<BlockPos> logs = logs(plan);
    assertTrue(logs.contains(BlockPos.ORIGIN), what + ": no trunk at the origin");
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> todo = new ArrayDeque<>();
    todo.add(BlockPos.ORIGIN);
    seen.add(BlockPos.ORIGIN);
    while (!todo.isEmpty()) {
      BlockPos at = todo.poll();
      for (BlockPos other : logs) {
        if (!seen.contains(other) && joined(at, other)) {
          seen.add(other);
          todo.add(other);
        }
      }
    }
    assertEquals(logs.size(), seen.size(), what + ": logs not joined into one tree");
    for (BlockPos p : plan.parts().keySet()) {
      assertTrue(p.getY() >= 0, what + ": a part below the ground");
    }
  }

  private static TreePlan grow(TreePreset preset, EnumFacing facing, long seed, TreeSpace space) {
    return TreeGenerators.grow(preset, TreeGenerators.heading(facing), new Random(seed), space);
  }

  @Test
  void everyPresetGrowsOneConnectedTreeFromTheOrigin() {
    for (TreePreset preset : TreePreset.values()) {
      for (EnumFacing facing : FACINGS) {
        for (long seed = 0; seed < 20; seed++) {
          for (int s = 0; s < SPACES.length; s++) {
            TreePlan plan = grow(preset, facing, seed, SPACES[s]);
            String what = preset + " " + facing + " seed " + seed + " space " + s;
            assertFalse(plan.hasNoRoom(), what + ": no room");
            assertConnected(plan, what);
          }
        }
      }
    }
  }

  @Test
  void nothingGrowsIntoOrThroughAWall() {
    for (TreePreset preset : TreePreset.values()) {
      for (EnumFacing facing : FACINGS) {
        for (long seed = 0; seed < 10; seed++) {
          TreePlan plan = grow(preset, facing, seed, CORNER);
          for (BlockPos p : plan.parts().keySet()) {
            assertTrue(CORNER.free(p), preset + " " + facing + ": a part in the wall at " + p);
          }
          if (preset.shape != TreePreset.Shape.LIMB) {
            continue;
          }
          for (BlockPos p : logs(plan)) {
            // Limbs keep a block off the wall; only the trunk may stand against it.
            if (p.getY() <= preset.trunkMax) {
              continue;
            }
            for (EnumFacing f : EnumFacing.HORIZONTALS) {
              assertTrue(CORNER.free(p.offset(f)),
                  preset + " " + facing + ": a limb against the wall at " + p);
            }
          }
        }
      }
    }
  }

  @Test
  void aTreeByAWallLeansAndReachesAwayFromIt() {
    for (TreePreset preset : new TreePreset[]{TreePreset.LIVE_OAK, TreePreset.ELM,
        TreePreset.PLANE, TreePreset.HONEY_LOCUST}) {
      int west = 0;
      int east = 0;
      for (long seed = 0; seed < 20; seed++) {
        // Planted facing the wall: the wall wins over the planter.
        TreePlan plan = grow(preset, EnumFacing.EAST, seed, WALL_EAST);
        for (BlockPos p : plan.parts().keySet()) {
          west += p.getX() < 0 ? 1 : 0;
          east += p.getX() > 0 ? 1 : 0;
        }
      }
      assertTrue(west > 2 * east, preset + " should grow away from the wall: " + west + " west, "
          + east + " east");
    }
  }

  @Test
  void aTreeByAWallIsPrunedNotRefused() {
    TreePlan plan = grow(TreePreset.LIVE_OAK, EnumFacing.EAST, 1, WALL_EAST);
    assertTrue(plan.getTrimmed() > 0, "a live oak by a wall should be pruned");
    assertTrue(cells(plan, TreePlan.Kind.LEAVES).size() > 40, "and still have a crown");
    assertEquals(0, grow(TreePreset.LIVE_OAK, EnumFacing.EAST, 1, TreeSpace.OPEN).getTrimmed());
  }

  @Test
  void nothingGrowsUnderARoof() {
    // An awning over everything east of the trunk, five blocks up.
    TreeSpace awning = rel -> rel.getX() >= 1 && rel.getY() == 5 ? TreeSpace.Cell.BLOCKED
        : TreeSpace.Cell.FREE;
    for (long seed = 0; seed < 10; seed++) {
      TreePlan plan = grow(TreePreset.PLANE, EnumFacing.EAST, seed, awning);
      for (BlockPos p : cells(plan, TreePlan.Kind.LEAVES)) {
        assertFalse(p.getX() >= 1 && p.getY() < 5, "leaves under the awning at " + p);
      }
    }
  }

  @Test
  void aTrunkWithNoRoomIsRefused() {
    TreeSpace ceiling = rel -> rel.getY() >= 2 ? TreeSpace.Cell.BLOCKED : TreeSpace.Cell.FREE;
    for (TreePreset preset : TreePreset.values()) {
      assertTrue(grow(preset, EnumFacing.NORTH, 0, ceiling).hasNoRoom(),
          preset + " should not fit under a ceiling two blocks up");
    }
    TreeSpace occupied = rel -> TreeSpace.Cell.BLOCKED;
    assertTrue(grow(TreePreset.ELM, EnumFacing.NORTH, 0, occupied).hasNoRoom());
  }

  @Test
  void anotherTreesFoliageIsMetNotEntered() {
    TreeSpace neighbour = rel -> rel.getX() >= 3 && rel.getY() >= 5 ? TreeSpace.Cell.FOLIAGE
        : TreeSpace.Cell.FREE;
    for (long seed = 0; seed < 10; seed++) {
      TreePlan plan = grow(TreePreset.ELM, EnumFacing.EAST, seed, neighbour);
      for (BlockPos p : plan.parts().keySet()) {
        assertTrue(neighbour.free(p), "grew into the other tree's leaves at " + p);
      }
    }
  }

  @Test
  void leaningTreesKeepTheirCanopyAboveTheStreet() {
    for (TreePreset preset : TreePreset.values()) {
      if (preset.shape != TreePreset.Shape.LIMB) {
        continue;
      }
      for (long seed = 0; seed < 20; seed++) {
        for (TreeSpace space : SPACES) {
          TreePlan plan = grow(preset, EnumFacing.EAST, seed, space);
          for (BlockPos p : cells(plan, TreePlan.Kind.LEAVES)) {
            assertTrue(p.getY() >= preset.clearance,
                preset + ": leaves at " + p + " under the clearance");
          }
        }
      }
    }
  }

  /**
   * Felling a limb keeps only leaves near a log, so a tree must be grown with every leaf that
   * near, or cutting one limb would thin the rest of the crown.
   */
  @Test
  void everyLeafIsNearALog() {
    for (TreePreset preset : TreePreset.values()) {
      for (long seed = 0; seed < 10; seed++) {
        for (TreeSpace space : SPACES) {
          TreePlan plan = grow(preset, EnumFacing.SOUTH, seed, space);
          Set<BlockPos> logs = logs(plan);
          Set<BlockPos> leaves = new HashSet<>();
          for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
            if (e.getValue().kind == TreePlan.Kind.LEAVES
                && !e.getValue().block.startsWith("tree_crown_palm_")) {
              leaves.add(e.getKey());
            }
          }
          Set<BlockPos> all = new HashSet<>(leaves);
          TreeFelling.Cells cells = p -> logs.contains(p) ? TreeFelling.Kind.LOG
              : all.contains(p) ? TreeFelling.Kind.LEAVES : TreeFelling.Kind.OTHER;
          // Cut nothing but ask about every leaf: seed the search from every leaves cell.
          Set<BlockPos> orphans = TreeFelling.orphanedLeaves(cells, leaves);
          assertTrue(orphans.isEmpty(), preset + " seed " + seed + ": leaves too far from a log "
              + orphans);
        }
      }
    }
  }

  @Test
  void aLeaningTreeReachesTheWayItFaces() {
    TreePlan plan = TreeGenerators.grow(TreePreset.LIVE_OAK, EnumFacing.EAST, new Random(3));
    int east = 0;
    int west = 0;
    for (BlockPos p : plan.parts().keySet()) {
      east = Math.max(east, p.getX());
      west = Math.max(west, -p.getX());
    }
    assertTrue(east > west + 3, "live oak should reach far further east than west");
  }

  @Test
  void palmsWearACrownOnTop() {
    for (TreePreset preset : new TreePreset[]{TreePreset.FAN_PALM, TreePreset.LEANING_PALM,
        TreePreset.CABBAGE_PALM, TreePreset.CANARY_PALM, TreePreset.COCONUT_PALM,
        TreePreset.KING_PALM}) {
      for (TreeSpace space : SPACES) {
        TreePlan plan = grow(preset, EnumFacing.SOUTH, 7, space);
        BlockPos topLog = null;
        for (BlockPos p : logs(plan)) {
          if (topLog == null || p.getY() > topLog.getY()) {
            topLog = p;
          }
        }
        assertNotNull(topLog);
        TreePlan.Part crown = plan.get(topLog.up());
        assertNotNull(crown, preset + ": nothing on top of the trunk");
        assertTrue(crown.block.startsWith("tree_crown_palm_"));
      }
    }
  }

  @Test
  void theSameSeedGrowsTheSameTree() {
    TreePlan a = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(42));
    TreePlan b = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(42));
    assertEquals(a.parts().keySet(), b.parts().keySet());
    TreePlan c = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(43));
    assertFalse(a.parts().keySet().equals(c.parts().keySet()), "seeds should vary the tree");
  }

  @Test
  void aPlayersLookBecomesAHeading() {
    assertEquals(Math.PI / 2, TreeGenerators.headingOfYaw(0), 1e-9); // yaw 0 looks south, +Z
    assertEquals(Math.PI, Math.abs(TreeGenerators.headingOfYaw(90)), 1e-9); // west, -X
    assertEquals(0, TreeGenerators.headingOfYaw(-90), 1e-9); // east, +X
  }
}
