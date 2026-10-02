package com.micatechnologies.minecraft.csm.parks.trees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.parks.planting.TreeGenerators;
import com.micatechnologies.minecraft.csm.parks.planting.TreePlan;
import com.micatechnologies.minecraft.csm.parks.planting.TreePreset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class TreeFellingTest {

  /** A planted tree as the felling sees it: its logs and leaves, on ground at y below 0. */
  private static Map<BlockPos, TreeFelling.Kind> world(TreePlan plan) {
    Map<BlockPos, TreeFelling.Kind> world = new HashMap<>();
    for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      TreePlan.Kind k = e.getValue().kind;
      if (k == TreePlan.Kind.LOG) {
        world.put(e.getKey(), TreeFelling.Kind.LOG);
      } else if (k == TreePlan.Kind.LEAVES) {
        world.put(e.getKey(), TreeFelling.Kind.LEAVES);
      }
    }
    return world;
  }

  private static TreeFelling.Cells cells(Map<BlockPos, TreeFelling.Kind> world) {
    return p -> p.getY() < 0 ? TreeFelling.Kind.GROUND
        : world.getOrDefault(p, TreeFelling.Kind.OTHER);
  }

  private static long count(Map<BlockPos, TreeFelling.Kind> world, TreeFelling.Kind kind) {
    return world.values().stream().filter(k -> k == kind).count();
  }

  @Test
  void cuttingTheTrunkFellsTheWholeTree() {
    for (TreePreset preset : TreePreset.values()) {
      TreePlan plan = TreeGenerators.grow(preset, EnumFacing.EAST, new Random(5));
      Map<BlockPos, TreeFelling.Kind> world = world(plan);
      BlockPos cut = BlockPos.ORIGIN;
      world.remove(cut);
      Set<BlockPos> logs = TreeFelling.unsupportedLogs(cells(world), cut);
      assertEquals(count(world, TreeFelling.Kind.LOG), logs.size(),
          preset + ": every log above the cut should fall");
      logs.forEach(world::remove);
      logs.add(cut);
      Set<BlockPos> leaves = TreeFelling.orphanedLeaves(cells(world), logs);
      assertEquals(count(world, TreeFelling.Kind.LEAVES), leaves.size(),
          preset + ": every leaf should go with the tree");
    }
  }

  @Test
  void cuttingALimbTakesOnlyThatLimb() {
    TreePlan plan = TreeGenerators.grow(TreePreset.LIVE_OAK, EnumFacing.EAST, new Random(2));
    Map<BlockPos, TreeFelling.Kind> world = world(plan);
    // The log furthest east is at the end of a limb; cut the limb a little way in from it.
    BlockPos tip = null;
    for (BlockPos p : world.keySet()) {
      if (world.get(p) == TreeFelling.Kind.LOG && (tip == null || p.getX() > tip.getX())) {
        tip = p;
      }
    }
    BlockPos cut = null;
    for (BlockPos p : world.keySet()) {
      if (world.get(p) == TreeFelling.Kind.LOG && p.getX() == tip.getX() - 2
          && p.getY() > 4) {
        cut = p;
        break;
      }
    }
    if (cut == null) {
      cut = tip;
    }
    long logsBefore = count(world, TreeFelling.Kind.LOG);
    world.remove(cut);
    Set<BlockPos> logs = TreeFelling.unsupportedLogs(cells(world), cut);
    assertFalse(logs.contains(BlockPos.ORIGIN), "the trunk must stay");
    assertTrue(logs.size() < logsBefore / 2, "a limb, not the tree: " + logs.size());
    logs.forEach(world::remove);
    logs.add(cut);
    Set<BlockPos> leaves = TreeFelling.orphanedLeaves(cells(world), logs);
    assertTrue(leaves.size() < count(world, TreeFelling.Kind.LEAVES) / 2,
        "most of the crown should stay");
  }

  @Test
  void aTreeStillOnTheGroundStands() {
    TreePlan plan = TreeGenerators.grow(TreePreset.ELM, EnumFacing.NORTH, new Random(9));
    Map<BlockPos, TreeFelling.Kind> world = world(plan);
    // Breaking a leaves-only cell or any log with the trunk below it intact fells nothing
    // that is still joined down to the ground.
    BlockPos top = BlockPos.ORIGIN;
    for (BlockPos p : world.keySet()) {
      if (world.get(p) == TreeFelling.Kind.LOG && p.getY() > top.getY()) {
        top = p;
      }
    }
    world.remove(top);
    for (BlockPos p : TreeFelling.unsupportedLogs(cells(world), top)) {
      assertTrue(p.getY() >= top.getY() - 1, "only what hung off the top can fall: " + p);
    }
  }

  @Test
  void aVanillaLogHoldsUpWhatIsJoinedToIt() {
    Map<BlockPos, TreeFelling.Kind> world = new HashMap<>();
    world.put(new BlockPos(0, 5, 0), TreeFelling.Kind.ANCHOR);
    world.put(new BlockPos(1, 5, 0), TreeFelling.Kind.LOG);
    world.put(new BlockPos(2, 5, 0), TreeFelling.Kind.LOG);
    BlockPos cut = new BlockPos(3, 5, 0);
    world.put(new BlockPos(4, 5, 0), TreeFelling.Kind.LOG);
    Set<BlockPos> logs = TreeFelling.unsupportedLogs(cells(world), cut);
    assertEquals(1, logs.size());
    assertTrue(logs.contains(new BlockPos(4, 5, 0)));
  }

  /**
   * A hand-built tree whose limb steps on all three axes at once (issue #250): a trunk four logs
   * tall on the ground, and a limb off its top at (+1, +1, +1), (+2, +2, +2), (+3, +3, +3).
   */
  private static Map<BlockPos, TreeFelling.Kind> cornerSteppedTree() {
    Map<BlockPos, TreeFelling.Kind> world = new HashMap<>();
    for (int y = 0; y < 4; y++) {
      world.put(new BlockPos(0, y, 0), TreeFelling.Kind.LOG);
    }
    for (int k = 1; k <= 3; k++) {
      world.put(new BlockPos(k, 3 + k, k), TreeFelling.Kind.LOG);
    }
    return world;
  }

  @Test
  void aCornerSteppedLimbFallsWithItsTree() {
    Map<BlockPos, TreeFelling.Kind> world = cornerSteppedTree();
    BlockPos cut = BlockPos.ORIGIN;
    world.remove(cut);
    Set<BlockPos> logs = TreeFelling.unsupportedLogs(cells(world), cut);
    assertEquals(6, logs.size(), "the trunk and the whole limb: " + logs);
    assertTrue(logs.contains(new BlockPos(3, 6, 3)));
  }

  @Test
  void cuttingACornerSteppedLimbTakesWhatIsBeyondTheCut() {
    Map<BlockPos, TreeFelling.Kind> world = cornerSteppedTree();
    BlockPos cut = new BlockPos(1, 4, 1);
    world.remove(cut);
    Set<BlockPos> logs = TreeFelling.unsupportedLogs(cells(world), cut);
    assertEquals(2, logs.size(), "the rest of the limb: " + logs);
    assertTrue(logs.contains(new BlockPos(2, 5, 2)));
    assertTrue(logs.contains(new BlockPos(3, 6, 3)));
  }

  @Test
  void aCornerSteppedLimbHoldsOnWhileItsTrunkStands() {
    Map<BlockPos, TreeFelling.Kind> world = cornerSteppedTree();
    BlockPos cut = new BlockPos(3, 6, 3);
    world.remove(cut);
    assertTrue(TreeFelling.unsupportedLogs(cells(world), cut).isEmpty());
  }
}
