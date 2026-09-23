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
   * Each preset's average quads when the geometry was last tuned (2026-09-23, after the sheeted
   * leaves, fewer log sides and straight-through tubes took one of every preset from 82,178 quads
   * to 37,687). A preset may grow 15% past this before the test fails; beyond that, look at what
   * grew, and raise the number here only if it earns its cost.
   */
  private static final Map<String, Integer> BUDGET = new HashMap<>();
  /** One of every preset together. */
  private static final int TOTAL_BUDGET = 45000;

  static {
    String[] rows = {"liveoak 3414", "elm 2746", "plane 3171", "honeylocust 2245", "cypress 188",
        "ginkgo 516", "fanpalm 242", "leaningpalm 240", "lollipopplane 576", "jacaranda 4210",
        "peppertree 4767", "coastliveoak 2712", "weepingwillow 7822", "poplar 661",
        "sweetgum 369", "hornbeam 479", "queenpalm 178", "lemongum 2268", "arborvitae 74",
        "pleachedlinden 306", "pollardedplane 503"};
    for (String row : rows) {
      String[] kv = row.split(" ");
      BUDGET.put(kv[0], Integer.parseInt(kv[1]));
    }
  }

  static TreeLeafType leafType(String block) {
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
    boolean[] faceLog = new boolean[6];
    for (EnumFacing f : EnumFacing.values()) {
      BlockPos q = pos.offset(f);
      if (isLog(plan, q)) {
        faceLog[f.getIndex()] = true;
        mask |= 1L << (TreeLogConnections.FACE_LOG_SHIFT + f.getIndex());
        mask |= ((long) width(plan.get(q).block).getMaskIndex())
            << (TreeLogConnections.WIDTH_SHIFT + 3 * f.getIndex());
      }
    }
    for (int i = 0; i < TreeLogConnections.DIAGONALS.length; i++) {
      int[] d = TreeLogConnections.DIAGONALS[i];
      if (!isLog(plan, pos.add(d[0], d[1], d[2]))) {
        continue;
      }
      int[][] between = TreeLogConnections.betweenCells(d);
      if (!isLog(plan, pos.add(between[0][0], between[0][1], between[0][2]))
          && !isLog(plan, pos.add(between[1][0], between[1][1], between[1][2]))) {
        mask |= 1L << (TreeLogConnections.DIAGONAL_SHIFT + i);
      }
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
      assertTrue(total <= budget * 1.15, preset.id + " draws " + total
          + " quads, over its budget of " + budget + " (+15%)");
      out.append(String.format("%-16s %7d %7d %7d %7d %9d%n", preset.id, sum[0] / SEEDS,
          sum[1] / SEEDS, sum[2] / SEEDS, sum[3] / SEEDS, total));
    }
    out.append(String.format("all presets, one of each: %d quads%n", all));
    System.out.println(out);
    assertTrue(all <= TOTAL_BUDGET, "one of every preset draws " + all + " quads, over "
        + TOTAL_BUDGET);
  }
}
