package com.micatechnologies.minecraft.csm.parks.trees;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TreePalmGeometryTest {

  private static double minY(List<TreeLogGeometry.Quad> quads) {
    double min = Double.MAX_VALUE;
    for (TreeLogGeometry.Quad q : quads) {
      for (double[] p : q.pos) {
        min = Math.min(min, p[1]);
      }
    }
    return min;
  }

  private static double reach(List<TreeLogGeometry.Quad> quads) {
    double max = 0;
    for (TreeLogGeometry.Quad q : quads) {
      for (double[] p : q.pos) {
        max = Math.max(max, Math.hypot(p[0] - 8, p[2] - 8));
      }
    }
    return max;
  }

  @Test
  void crownReachesWellPastTheCell() {
    for (TreeLeafType type : new TreeLeafType[]{TreeLeafType.PALM_FAN,
        TreeLeafType.PALM_FEATHER}) {
      List<TreeLogGeometry.Quad> quads = TreePalmGeometry.quads(type, 0, true);
      assertFalse(quads.isEmpty());
      assertTrue(reach(quads) > 16, type + " fronds should reach past the neighbouring cells");
    }
  }

  @Test
  void onlyTheSkirtHangsBelowTheCrown() {
    assertTrue(minY(TreePalmGeometry.quads(TreeLeafType.PALM_FAN, 1, true)) > -16);
    assertTrue(minY(TreePalmGeometry.quads(TreeLeafType.PALM_FAN_SKIRT, 1, true)) < -16);
  }

  @Test
  void fastGraphicsDrawsFewerFronds() {
    int fancy = TreePalmGeometry.quads(TreeLeafType.PALM_FAN_SKIRT, 2, true).size();
    int fast = TreePalmGeometry.quads(TreeLeafType.PALM_FAN_SKIRT, 2, false).size();
    assertTrue(fast < fancy);
  }

  @Test
  void palmTypesDispatchFromTheLeavesGeometry() {
    int key = TreeLeavesGeometry.key(0, 3);
    assertTrue(TreeLeavesGeometry.quads(TreeLeafType.PALM_FEATHER, key, true).size()
        == TreePalmGeometry.quads(TreeLeafType.PALM_FEATHER, 3, true).size());
  }
}
