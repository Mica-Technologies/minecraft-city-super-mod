package com.micatechnologies.minecraft.csm.parks.trees;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

class TreeLogGeometryTest {

  private static long faceLog(EnumFacing f, TreeLogWidth neighbour) {
    return (1L << (TreeLogConnections.FACE_LOG_SHIFT + f.getIndex()))
        | ((long) neighbour.getMaskIndex() << (TreeLogConnections.WIDTH_SHIFT + 3 * f.getIndex()));
  }

  private static long diagonal(int[] d) {
    for (int i = 0; i < TreeLogConnections.DIAGONALS.length; i++) {
      int[] g = TreeLogConnections.DIAGONALS[i];
      if (g[0] == d[0] && g[1] == d[1] && g[2] == d[2]) {
        return 1L << (TreeLogConnections.DIAGONAL_SHIFT + i);
      }
    }
    throw new IllegalArgumentException();
  }

  @Test
  void aLoneLogIsAStraightLengthAlongItsAxis() {
    long mask = ((long) EnumFacing.Axis.X.ordinal()) << TreeLogConnections.AXIS_SHIFT;
    List<TreeLogGeometry.Arm> arms = TreeLogGeometry.arms(TreeLogWidth.THIN, mask);
    assertEquals(2, arms.size());
    assertEquals(16, arms.get(0).to[0], 1e-9);
    assertEquals(0, arms.get(1).to[0], 1e-9);
    assertFalse(TreeLogGeometry.bent(arms));
  }

  @Test
  void aSteppedTrunkBridgesToTheSharedEdgeAndCarriesOnStraight() {
    // Only an up-east diagonal log: the bridge to the shared edge, and a free end straight on
    // to the opposite (down-west) edge -- one straight leaning line, no knuckle.
    long mask = diagonal(new int[]{1, 1, 0});
    List<TreeLogGeometry.Arm> arms = TreeLogGeometry.arms(TreeLogWidth.THIN, mask);
    assertEquals(2, arms.size());
    assertArrayEquals(new double[]{16, 16, 8}, arms.get(0).to, 1e-9);
    assertArrayEquals(new double[]{0, 0, 8}, arms.get(1).to, 1e-9);
    assertFalse(TreeLogGeometry.bent(arms));
  }

  @Test
  void whereATrunkStartsToLeanTheJointGetsAKnuckle() {
    long mask = faceLog(EnumFacing.DOWN, TreeLogWidth.THIN) | diagonal(new int[]{1, 1, 0});
    assertTrue(TreeLogGeometry.bent(TreeLogGeometry.arms(TreeLogWidth.THIN, mask)));
  }

  @Test
  void aLogTapersTowardAThinnerNeighbourButNotAThickerOne() {
    long mask = faceLog(EnumFacing.UP, TreeLogWidth.TWIG) | faceLog(EnumFacing.DOWN,
        TreeLogWidth.FULL);
    List<TreeLogGeometry.Arm> arms = TreeLogGeometry.arms(TreeLogWidth.MEDIUM, mask);
    TreeLogGeometry.Arm up = arms.stream().filter(a -> a.to[1] == 16).findFirst().get();
    TreeLogGeometry.Arm down = arms.stream().filter(a -> a.to[1] == 0).findFirst().get();
    assertEquals(1.0, up.r1, 1e-9);
    assertEquals(4.0, down.r1, 1e-9);
  }

  @Test
  void theGroundFlareIsWideAtTheBottom() {
    long mask = 1L << TreeLogConnections.GROUND_BIT;
    TreeLogGeometry.Arm flare = TreeLogGeometry.arms(TreeLogWidth.MEDIUM, mask).get(0);
    assertEquals(0, flare.from[1], 1e-9);
    assertTrue(flare.r0 > flare.r1);
  }

  @Test
  void everyTubeQuadFacesAwayFromItsArm() {
    long mask = faceLog(EnumFacing.DOWN, TreeLogWidth.MEDIUM) | diagonal(new int[]{1, 1, 0})
        | diagonal(new int[]{0, 1, -1}) | (1L << (TreeLogConnections.FACE_LEAVES_SHIFT
        + EnumFacing.UP.getIndex()));
    for (TreeLogGeometry.Quad q : TreeLogGeometry.quads(TreeLogWidth.MEDIUM, mask)) {
      double[] n = q.faceNormal();
      assertEquals(1.0, TreeLogGeometry.len(n), 1e-6, "degenerate quad");
      // Each vertex normal points the same way as the face (the face is not inside out).
      for (double[] vn : q.normal) {
        assertTrue(TreeLogGeometry.dot(n, vn) > 0, "a quad faces inward");
      }
    }
  }

  @Test
  void boxesStayInsideTheBlock() {
    long mask = diagonal(new int[]{-1, 1, 0}) | (1L << TreeLogConnections.GROUND_BIT);
    for (AxisAlignedBB b : TreeLogGeometry.boxes(TreeLogWidth.THICK, mask)) {
      assertTrue(b.minX >= 0 && b.minY >= 0 && b.minZ >= 0);
      assertTrue(b.maxX <= 1 && b.maxY <= 1 && b.maxZ <= 1);
    }
  }

  @Test
  void theCellsBetweenADiagonalAreItsTwoFaceNeighbours() {
    for (int[] d : TreeLogConnections.DIAGONALS) {
      int[][] between = TreeLogConnections.betweenCells(d);
      for (int[] c : between) {
        int nonZero = 0;
        for (int k = 0; k < 3; k++) {
          if (c[k] != 0) {
            nonZero++;
            assertEquals(d[k], c[k]);
          }
        }
        assertEquals(1, nonZero);
      }
      assertFalse(java.util.Arrays.equals(between[0], between[1]));
    }
  }
}
