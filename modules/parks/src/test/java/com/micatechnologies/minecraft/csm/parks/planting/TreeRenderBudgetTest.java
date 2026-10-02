package com.micatechnologies.minecraft.csm.parks.planting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.parks.trees.TreeLeafType;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLeavesGeometry;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogConnections;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogGeometry;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * How many quads a planted tree puts into its chunks, per preset: every log's geometry from the
 * mask its neighbours in the plan give it, every leaves cell's cards from its open faces, and the
 * hanging blocks' crosses. Exact (it runs the same geometry the baked models do), so it is the
 * number to watch when changing any of that geometry; it prints a table and checks each preset
 * against a ceiling.
 */
class TreeRenderBudgetTest {

  private static final int SEEDS = 10;

  /**
   * Each preset's average quads when the geometry was last tuned. The 2026-09-23 pass (sheeted
   * leaves, fewer log sides, straight-through tubes and a curtain hung only from a weeping
   * crown's underside) took one of every preset from 82,178 quads to 31,199; the adaptive
   * generator that followed it (forked leaders, side branches, lopsided clusters) brought that
   * back to about 33,700, the price of limbed trees that no longer all look alike. The six
   * regional presets that followed (paper birch to beech) added about 9,800. The cabbage palm's own
   * full crown (2026-09-27) took it from 231 to 439, and the four big trees that followed (coast
   * redwood, eastern white pine, old English oak, camphor) added about 29,400: the oak, the
   * camphor and the white pine are some 9,000 each, twice and more a live oak's width or height,
   * and nearly half of it their thick limbs. Plant them as specimens, not as a street. A preset
   * may grow
   * 15% past this before the test fails; beyond that, look at what grew, and raise the number
   * here only if it earns its cost.
   */
  private static final Map<String, Integer> BUDGET = new HashMap<>();
  /** One of every preset together. */
  private static final int TOTAL_BUDGET = 76000;

  static {
    String[] rows = {"liveoak 3832", "elm 3210", "plane 3588", "honeylocust 1670", "cypress 188",
        "ginkgo 514", "fanpalm 228", "leaningpalm 244", "lollipopplane 576", "jacaranda 3122",
        "peppertree 3562", "coastliveoak 2798", "weepingwillow 5708", "poplar 638",
        "sweetgum 372", "hornbeam 484", "queenpalm 178", "lemongum 1789", "arborvitae 74",
        "pleachedlinden 306", "pollardedplane 590", "paperbirch 1635", "bluespruce 974",
        "cabbagepalm 439", "japanesemaple 1664", "scotspine 1368", "beech 3951",
        "coastredwood 2455", "whitepine 8771", "englishoak 8899", "camphor 9273"};
    for (String row : rows) {
      String[] kv = row.split(" ");
      BUDGET.put(kv[0], Integer.parseInt(kv[1]));
    }
  }

  static TreeLeafType leafType(String block) {
    if (block.startsWith("tree_crown_palm_cabbage_skirt")) {
      return TreeLeafType.PALM_CABBAGE_SKIRT;
    }
    if (block.startsWith("tree_crown_palm_cabbage")) {
      return TreeLeafType.PALM_CABBAGE;
    }
    if (block.startsWith("tree_crown_palm_fan_skirt")) {
      return TreeLeafType.PALM_FAN_SKIRT;
    }
    if (block.startsWith("tree_crown_palm_fan")) {
      return TreeLeafType.PALM_FAN;
    }
    if (block.startsWith("tree_crown_palm_feather")) {
      return TreeLeafType.PALM_FEATHER;
    }
    if (block.contains("cypress") || block.contains("arborvitae")) {
      return TreeLeafType.NEEDLE;
    }
    if (block.contains("honeylocust") || block.contains("jacaranda") || block.endsWith("_gum")) {
      return TreeLeafType.AIRY;
    }
    if (block.contains("pepper") || block.contains("willow")) {
      return TreeLeafType.WEEPING;
    }
    if (block.contains("clipped")) {
      return TreeLeafType.CLIPPED;
    }
    return TreeLeafType.BROADLEAF;
  }

  private static TreeLogWidth width(String block) {
    String id = block.substring(block.lastIndexOf('_') + 1);
    for (TreeLogWidth w : TreeLogWidth.values()) {
      if (w.getId().equals(id)) {
        return w;
      }
    }
    throw new IllegalArgumentException(block);
  }

  private static boolean isLog(TreePlan plan, BlockPos p) {
    TreePlan.Part part = plan.get(p);
    return part != null && part.kind == TreePlan.Kind.LOG;
  }

  private static boolean isLeaves(TreePlan plan, BlockPos p) {
    TreePlan.Part part = plan.get(p);
    return part != null && part.kind == TreePlan.Kind.LEAVES;
  }

  /** TreeLogConnections.compute, on a plan (the ground is everything below y = 0). */
  static long mask(TreePlan plan, BlockPos pos, EnumFacing.Axis axis) {
    long mask = ((long) axis.ordinal()) << TreeLogConnections.AXIS_SHIFT;
    mask |= TreeLogConnections.links((dx, dy, dz) -> {
      TreePlan.Part part = plan.get(pos.add(dx, dy, dz));
      return part != null && part.kind == TreePlan.Kind.LOG ? width(part.block).getMaskIndex() : 0;
    });
    boolean[] faceLog = new boolean[6];
    for (EnumFacing f : EnumFacing.values()) {
      faceLog[f.getIndex()] = TreeLogConnections.faceLog(mask, f.getIndex());
    }
    for (EnumFacing f : EnumFacing.values()) {
      if (isLeaves(plan, pos.offset(f))
          && (f == EnumFacing.UP || faceLog[f.getOpposite().getIndex()])) {
        mask |= 1L << (TreeLogConnections.FACE_LEAVES_SHIFT + f.getIndex());
      }
    }
    if (!faceLog[EnumFacing.DOWN.getIndex()] && pos.getY() == 0) {
      mask |= 1L << TreeLogConnections.GROUND_BIT;
    }
    return mask;
  }

  /** {logs, leaves, hanging, leaves cells} quads for one plan. */
  static int[] count(TreePlan plan) {
    int logs = 0;
    int leaves = 0;
    int hanging = 0;
    int cells = 0;
    for (Map.Entry<BlockPos, TreePlan.Part> e : plan.parts().entrySet()) {
      BlockPos p = e.getKey();
      TreePlan.Part part = e.getValue();
      if (part.kind == TreePlan.Kind.LOG) {
        logs += TreeLogGeometry.quads(width(part.block), mask(plan, p, part.axis)).size();
      } else if (part.kind == TreePlan.Kind.LEAVES) {
        TreeLeafType type = leafType(part.block);
        int open = 0;
        for (EnumFacing f : EnumFacing.values()) {
          // Open where no leaves are beyond; a log is not opaque (all but full-width ones).
          if (!isLeaves(plan, p.offset(f))) {
            open |= 1 << f.getIndex();
          }
        }
        if (type.isPalm()) {
          open = 0;
        }
        int variant = (p.getX() * 31 + p.getZ() * 17 + p.getY() * 7) & 3;
        leaves += TreeLeavesGeometry.quads(type, TreeLeavesGeometry.key(open, variant), true)
            .size();
        cells++;
      } else {
        hanging += 4;
      }
    }
    return new int[]{logs, leaves, hanging, cells};
  }

  @Test
  void everyPresetStaysWithinItsBudget() {
    StringBuilder out = new StringBuilder(String.format(
        "%-16s %7s %7s %7s %7s %9s%n", "preset", "logs", "leaves", "hang", "cells", "total"));
    long all = 0;
    StringBuilder over = new StringBuilder();
    for (TreePreset preset : TreePreset.values()) {
      long[] sum = new long[4];
      for (long seed = 0; seed < SEEDS; seed++) {
        int[] c = count(TreeGenerators.grow(preset, EnumFacing.SOUTH, new Random(seed)));
        for (int i = 0; i < 4; i++) {
          sum[i] += c[i];
        }
      }
      long total = (sum[0] + sum[1] + sum[2]) / SEEDS;
      all += total;
      Integer budget = BUDGET.get(preset.id);
      assertTrue(budget != null, "no budget for preset " + preset.id + "; add it to BUDGET");
      if (total > budget * 1.15) {
        over.append(preset.id).append(" draws ").append(total)
            .append(" quads, over its budget of ").append(budget).append(" (+15%); ");
      }
      out.append(String.format("%-16s %7d %7d %7d %7d %9d%n", preset.id, sum[0] / SEEDS,
          sum[1] / SEEDS, sum[2] / SEEDS, sum[3] / SEEDS, total));
    }
    out.append(String.format("all presets, one of each: %d quads%n", all));
    System.out.println(out);
    assertTrue(over.length() == 0, over.toString());
    assertTrue(all <= TOTAL_BUDGET, "one of every preset draws " + all + " quads, over "
        + TOTAL_BUDGET);
  }
}
