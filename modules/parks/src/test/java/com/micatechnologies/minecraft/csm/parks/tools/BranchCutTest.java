package com.micatechnologies.minecraft.csm.parks.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.parks.planting.TreeGenerators;
import com.micatechnologies.minecraft.csm.parks.planting.TreePlan;
import com.micatechnologies.minecraft.csm.parks.planting.TreePreset;
import com.micatechnologies.minecraft.csm.parks.trees.TreeFelling;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The pole trimmer and tree shears cut a twig or thin log through {@code TreeFelling}, and only
 * where what falls is all small ({@link BranchCutting#isBranch}): that must only ever take the
 * branch past the cut, never the tree.
 */
class BranchCutTest {

  @Test
  void cuttingATwigOrThinBranchNeverFellsTheTree() {
    int cuts = 0;
    int refused = 0;
    for (TreePreset preset : TreePreset.values()) {
      TreePlan plan = TreeGenerators.grow(preset, EnumFacing.EAST, new Random(7));
      Map<BlockPos, TreeFelling.Kind> world = new HashMap<>();
      Map<BlockPos, String> blocks = new HashMap<>();
      for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
        if (e.getValue().kind == TreePlan.Kind.LOG) {
          world.put(e.getKey(), TreeFelling.Kind.LOG);
          blocks.put(e.getKey(), e.getValue().block);
        }
      }
      for (Map.Entry<BlockPos, String> e : blocks.entrySet()) {
        BlockPos cut = e.getKey();
        if (!small(e.getValue()) || cut.getX() == 0 && cut.getZ() == 0) {
          continue;
        }
        Map<BlockPos, TreeFelling.Kind> w = new HashMap<>(world);
        w.remove(cut);
        TreeFelling.Cells cells = p -> p.getY() < 0 ? TreeFelling.Kind.GROUND
            : w.getOrDefault(p, TreeFelling.Kind.OTHER);
        Set<BlockPos> falling = TreeFelling.unsupportedLogs(cells, cut);
        if (!BranchCutting.isBranch(falling, p -> small(blocks.get(p)))) {
          refused++;
          continue;
        }
        assertFalse(falling.contains(BlockPos.ORIGIN),
            preset + ": cutting the branch at " + cut + " felled the trunk");
        cuts++;
      }
    }
    assertTrue(cuts > refused, "most small logs should be branches a hand tool may cut: "
        + cuts + " cut, " + refused + " refused");
  }

  private static boolean small(String block) {
    return block != null && (block.endsWith("_twig") || block.endsWith("_thin"));
  }
}
