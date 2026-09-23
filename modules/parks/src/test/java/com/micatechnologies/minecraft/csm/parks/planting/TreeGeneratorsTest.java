package com.micatechnologies.minecraft.csm.parks.planting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  private static Set<BlockPos> logs(TreePlan plan) {
    Set<BlockPos> logs = new HashSet<>();
    for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      if (e.getValue().kind == TreePlan.Kind.LOG) {
        logs.add(e.getKey());
      }
    }
    return logs;
  }

  /** Face and edge-diagonal neighbours: what the log kit joins. Corner diagonals are not. */
  private static boolean joined(BlockPos a, BlockPos b) {
    int dx = Math.abs(a.getX() - b.getX());
    int dy = Math.abs(a.getY() - b.getY());
    int dz = Math.abs(a.getZ() - b.getZ());
    int moved = (dx > 0 ? 1 : 0) + (dy > 0 ? 1 : 0) + (dz > 0 ? 1 : 0);
    return dx <= 1 && dy <= 1 && dz <= 1 && moved >= 1 && moved <= 2;
  }

  @Test
  void everyPresetGrowsOneConnectedTreeFromTheOrigin() {
    for (TreePreset preset : TreePreset.values()) {
      for (EnumFacing facing : FACINGS) {
        for (long seed = 0; seed < 20; seed++) {
          TreePlan plan = TreeGenerators.grow(preset, facing, new Random(seed));
          Set<BlockPos> logs = logs(plan);
          assertTrue(logs.contains(BlockPos.ORIGIN), preset + ": no trunk at the origin");
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
          assertEquals(logs.size(), seen.size(),
              preset + " " + facing + " seed " + seed + ": logs not joined into one tree");
          for (BlockPos p : plan.parts().keySet()) {
            assertTrue(p.getY() >= 0, preset + ": a part below the ground");
          }
        }
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
        TreePlan plan = TreeGenerators.grow(preset, EnumFacing.EAST, new Random(seed));
        for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
          BlockPos p = e.getKey();
          if (e.getValue().kind == TreePlan.Kind.LEAVES) {
            assertTrue(p.getY() >= preset.clearance,
                preset + ": leaves at " + p + " under the clearance");
          }
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
    for (TreePreset preset : new TreePreset[]{TreePreset.FAN_PALM, TreePreset.LEANING_PALM}) {
      TreePlan plan = TreeGenerators.grow(preset, EnumFacing.SOUTH, new Random(7));
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

  @Test
  void theSameSeedGrowsTheSameTree() {
    TreePlan a = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(42));
    TreePlan b = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(42));
    assertEquals(a.parts().keySet(), b.parts().keySet());
    TreePlan c = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(43));
    assertFalse(a.parts().keySet().equals(c.parts().keySet()), "seeds should vary the tree");
  }
}
