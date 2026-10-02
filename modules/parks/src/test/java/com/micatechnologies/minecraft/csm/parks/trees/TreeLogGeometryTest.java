package com.micatechnologies.minecraft.csm.parks.trees;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  private static long corner(int[] c) {
    for (int i = 0; i < TreeLogConnections.CORNERS.length; i++) {
      int[] g = TreeLogConnections.CORNERS[i];
      if (g[0] == c[0] && g[1] == c[1] && g[2] == c[2]) {
        return 1L << (TreeLogConnections.CORNER_SHIFT + i);
      }
    }
    throw new IllegalArgumentException();
  }

  private static long cornerBits() {
    return 0xFFL << TreeLogConnections.CORNER_SHIFT;
  }

  /** The links of the log at the origin among logs (all {@code width}) at these cells. */
  private static long links(TreeLogWidth width, int[]... logs) {
    return TreeLogConnections.links((dx, dy, dz) -> {
      for (int[] l : logs) {
        if (l[0] == dx && l[1] == dy && l[2] == dz) {
          return width.getMaskIndex();
        }
      }
      return 0;
    });
  }

  private static final TreeLogWidth[] TUBES = {TreeLogWidth.TWIG, TreeLogWidth.THIN,
      TreeLogWidth.MEDIUM, TreeLogWidth.THICK};

  private static final double[] CENTRE = {8, 8, 8};

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

  @Test
  void aCornerStepBridgesToTheSharedCornerAndCarriesOnStraight() {
    // Issue #250: a log whose next log is a step on all three axes. The bridge runs to the
    // shared corner, the free end straight on to the opposite corner: one leaning line.
    long mask = corner(new int[]{1, 1, 1});
    List<TreeLogGeometry.Arm> arms = TreeLogGeometry.arms(TreeLogWidth.MEDIUM, mask);
    assertEquals(2, arms.size());
    assertArrayEquals(new double[]{16, 16, 16}, arms.get(0).to, 1e-9);
    assertArrayEquals(new double[]{0, 0, 0}, arms.get(1).to, 1e-9);
    assertFalse(TreeLogGeometry.bent(arms));
  }

  @Test
  void aColumnThatStepsOffOnACornerIsJoinedWithAKnuckle() {
    // The reported build: a column, then logs at (+1, +1, +1) and (+2, +2, +2).
    long top = links(TreeLogWidth.MEDIUM, new int[]{0, -1, 0}, new int[]{1, 1, 1});
    assertTrue(TreeLogConnections.faceLog(top, EnumFacing.DOWN.getIndex()));
    assertEquals(corner(new int[]{1, 1, 1}), top & cornerBits());
    assertTrue(TreeLogGeometry.bent(TreeLogGeometry.arms(TreeLogWidth.MEDIUM, top)));
    long middle = links(TreeLogWidth.MEDIUM, new int[]{-1, -1, -1}, new int[]{1, 1, 1});
    assertEquals(corner(new int[]{-1, -1, -1}) | corner(new int[]{1, 1, 1}), middle);
    // Through the middle of a corner-stepped run: one straight tube, corner to corner.
    assertNotNull(TreeLogGeometry.straightThrough(
        TreeLogGeometry.arms(TreeLogWidth.MEDIUM, middle)));
  }

  @Test
  void aCornerIsNotBridgedWhereAFaceOrEdgeAlreadyJoinsTheTwo() {
    for (int[] c : TreeLogConnections.CORNERS) {
      assertEquals(corner(c), links(TreeLogWidth.THIN, c));
      int[][] between = TreeLogConnections.cornerBetweenCells(c);
      assertEquals(6, between.length);
      for (int[] b : between) {
        long via = links(TreeLogWidth.THIN, c, b);
        assertEquals(0, via & cornerBits(), "bridged past a log at " + Arrays.toString(b));
      }
    }
  }

  @Test
  void bothLogsOfACornerStepSeeTheJoin() {
    // The six cells between are the same six from either end, so both halves are drawn or
    // neither is: never half a bridge.
    for (int[] c : TreeLogConnections.CORNERS) {
      int[] back = {-c[0], -c[1], -c[2]};
      Set<String> mine = new HashSet<>();
      for (int[] b : TreeLogConnections.cornerBetweenCells(c)) {
        mine.add(b[0] + "," + b[1] + "," + b[2]);
      }
      Set<String> theirs = new HashSet<>();
      for (int[] b : TreeLogConnections.cornerBetweenCells(back)) {
        theirs.add((b[0] + c[0]) + "," + (b[1] + c[1]) + "," + (b[2] + c[2]));
      }
      assertEquals(mine, theirs);
    }
  }

  @Test
  void aCornerBridgeMeetsItsNeighboursBridgeRingForRing() {
    // Each log draws half the tube; the two ends at the shared corner must be the same ring, or
    // the limb shows a crack or a step there.
    for (TreeLogWidth width : TUBES) {
      for (int[] c : TreeLogConnections.CORNERS) {
        int[] back = {-c[0], -c[1], -c[2]};
        double[] shared = {8 + 8 * c[0], 8 + 8 * c[1], 8 + 8 * c[2]};
        double[] axis = {c[0], c[1], c[2]};
        List<double[]> mine = ringAt(TreeLogGeometry.quads(width, corner(c)), shared, axis,
            new double[]{0, 0, 0});
        List<double[]> theirs = ringAt(TreeLogGeometry.quads(width, corner(back)), shared, axis,
            new double[]{16 * c[0], 16 * c[1], 16 * c[2]});
        assertEquals(TreeLogGeometry.sides(width), mine.size(), width + " ring");
        assertEquals(mine.size(), theirs.size());
        for (double[] p : mine) {
          assertTrue(theirs.stream().anyMatch(q -> TreeLogGeometry.len(TreeLogGeometry.sub(p, q))
              < 1e-6), width + ": no matching vertex for " + Arrays.toString(p));
        }
      }
    }
  }

  /** The distinct vertices, moved by {@code offset}, on the plane through {@code at}. */
  private static List<double[]> ringAt(List<TreeLogGeometry.Quad> quads, double[] at,
      double[] axis, double[] offset) {
    double[] n = TreeLogGeometry.normalize(axis);
    List<double[]> ring = new ArrayList<>();
    for (TreeLogGeometry.Quad q : quads) {
      for (double[] p0 : q.pos) {
        double[] p = TreeLogGeometry.add(p0, offset);
        if (Math.abs(TreeLogGeometry.dot(TreeLogGeometry.sub(p, at), n)) > 1e-6) {
          continue;
        }
        if (ring.stream().noneMatch(r -> TreeLogGeometry.len(TreeLogGeometry.sub(r, p)) < 1e-6)) {
          ring.add(p);
        }
      }
    }
    return ring;
  }

  @Test
  void aCornerBridgeIsATubeOfTheLogsRadiusFacingOutward() {
    for (TreeLogWidth width : TUBES) {
      double r = width.getPixels() / 2.0;
      for (int[] c : TreeLogConnections.CORNERS) {
        double[] d = TreeLogGeometry.normalize(new double[]{c[0], c[1], c[2]});
        List<TreeLogGeometry.Quad> quads = TreeLogGeometry.quads(width, corner(c));
        assertFalse(quads.isEmpty());
        double reach = 0;
        for (TreeLogGeometry.Quad q : quads) {
          double[] n = q.faceNormal();
          assertEquals(1.0, TreeLogGeometry.len(n), 1e-6, "degenerate quad");
          double[] mid = new double[3];
          for (double[] p : q.pos) {
            double[] rel = TreeLogGeometry.sub(p, CENTRE);
            double along = TreeLogGeometry.dot(rel, d);
            double off = TreeLogGeometry.len(
                TreeLogGeometry.sub(rel, TreeLogGeometry.scale(d, along)));
            // Every vertex on the line between the two logs' centres, within the log's radius.
            assertTrue(off <= r + 1e-6, width + ": a vertex off the bridge");
            assertTrue(Math.abs(along) <= 8 * Math.sqrt(3) + 1e-6, width + ": past the corner");
            reach = Math.max(reach, along);
            mid = TreeLogGeometry.add(mid, TreeLogGeometry.scale(p, 0.25));
          }
          double[] rel = TreeLogGeometry.sub(mid, CENTRE);
          double along = TreeLogGeometry.dot(rel, d);
          if (Math.abs(TreeLogGeometry.dot(n, d)) > 0.99) {
            // An end cap: it faces away from the centre, along the line.
            assertTrue(TreeLogGeometry.dot(n, rel) > 0, width + ": a cap faces inward");
          } else {
            // A side: it faces away from the line, so it is seen from outside the limb (a quad
            // facing in is culled from exactly the side the player looks at).
            double[] radial = TreeLogGeometry.sub(rel, TreeLogGeometry.scale(d, along));
            assertTrue(TreeLogGeometry.dot(n, radial) > 0, width + ": a side faces inward");
          }
          for (double[] vn : q.normal) {
            assertTrue(TreeLogGeometry.dot(n, vn) > 0, width + ": shading normal inward");
          }
        }
        // It reaches all the way to the shared corner.
        assertEquals(8 * Math.sqrt(3), reach, 1e-6);
      }
    }
  }

  @Test
  void aCornerBridgeKeepsItsOwnLogsRadius() {
    // As an edge bridge does: a medium log stepping onto a thin one is drawn medium to the
    // corner, and the thin one thin from it.
    long mask = TreeLogConnections.links((dx, dy, dz) -> dx == 1 && dy == 1 && dz == -1
        ? TreeLogWidth.THIN.getMaskIndex() : 0);
    TreeLogGeometry.Arm bridge = TreeLogGeometry.arms(TreeLogWidth.MEDIUM, mask).get(0);
    assertArrayEquals(new double[]{16, 16, 0}, bridge.to, 1e-9);
    assertEquals(TreeLogWidth.MEDIUM.getPixels() / 2.0, bridge.r0, 1e-9);
    assertEquals(TreeLogWidth.MEDIUM.getPixels() / 2.0, bridge.r1, 1e-9);
  }

  @Test
  void cornerBoxesStayInsideTheBlock() {
    for (int[] c : TreeLogConnections.CORNERS) {
      long mask = corner(c) | (1L << TreeLogConnections.GROUND_BIT);
      for (AxisAlignedBB b : TreeLogGeometry.boxes(TreeLogWidth.THICK, mask)) {
        assertTrue(b.minX >= 0 && b.minY >= 0 && b.minZ >= 0);
        assertTrue(b.maxX <= 1 && b.maxY <= 1 && b.maxZ <= 1);
      }
    }
  }

  @Test
  void theCellsBetweenACornerAreItsThreeFaceAndThreeEdgeNeighbours() {
    for (int[] c : TreeLogConnections.CORNERS) {
      int faces = 0;
      int edges = 0;
      for (int[] b : TreeLogConnections.cornerBetweenCells(c)) {
        int nonZero = 0;
        for (int k = 0; k < 3; k++) {
          if (b[k] != 0) {
            nonZero++;
            assertEquals(c[k], b[k]);
          }
        }
        faces += nonZero == 1 ? 1 : 0;
        edges += nonZero == 2 ? 1 : 0;
      }
      assertEquals(3, faces);
      assertEquals(3, edges);
    }
  }
}
