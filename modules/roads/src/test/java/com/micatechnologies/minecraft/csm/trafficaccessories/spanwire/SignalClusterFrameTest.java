package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

/**
 * The footprint and frame-centre rules of the signal cluster hanger, on synthetic positions.
 */
class SignalClusterFrameTest {

  private static final BlockPos HANGER = new BlockPos(632, 11, 561);

  /** Where a head's drop lands: its column centre, set back by a facing offset. */
  private static Vec3d point(int x, int z, double dx, double dz) {
    return new Vec3d(x + 0.5 + dx, 0.0, z + 0.5 + dz);
  }

  @Test
  void footprintIsTheThreeByThreeAtTheHangersHeightOwnColumnFirst() {
    final List<BlockPos> columns = new ArrayList<>();
    SignalClusterFrame.footprint(HANGER, columns);
    assertEquals(9, columns.size());
    assertEquals(9, new HashSet<>(columns).size());
    assertEquals(HANGER, columns.get(0));
    for (BlockPos column : columns) {
      assertEquals(HANGER.getY(), column.getY());
      assertTrue(Math.abs(column.getX() - HANGER.getX()) <= 1);
      assertTrue(Math.abs(column.getZ() - HANGER.getZ()) <= 1);
    }
  }

  @Test
  void carriesHeadsOneRowDownInTheFootprintOnly() {
    assertTrue(SignalClusterFrame.carries(HANGER, HANGER.down()));
    assertTrue(SignalClusterFrame.carries(HANGER, HANGER.add(1, -1, -1)));
    assertTrue(SignalClusterFrame.carries(HANGER, HANGER.add(-1, -1, 0)));
    // Outside the footprint.
    assertFalse(SignalClusterFrame.carries(HANGER, HANGER.add(2, -1, 0)));
    // An add-on under a head, or a lone head further down: nothing drops to it.
    assertFalse(SignalClusterFrame.carries(HANGER, HANGER.down(2)));
    // Level with or above the hanger.
    assertFalse(SignalClusterFrame.carries(HANGER, HANGER.add(1, 0, 0)));
  }

  @Test
  void noPointsGivesTheFallbackItself() {
    final Vec3d fallback = new Vec3d(1.5, 11.63, 2.5);
    assertSame(fallback, SignalClusterFrame.centroid(Collections.emptyList(), fallback));
  }

  @Test
  void oneHeadIsAStraightDropOntoIt() {
    final Vec3d head = point(620, 560, 0.0, 0.1875);
    final Vec3d hub = SignalClusterFrame.centroid(Collections.singletonList(head),
        new Vec3d(620.5, 11.63, 560.5));
    assertEquals(head.x, hub.x, 1.0e-9);
    assertEquals(head.z, hub.z, 1.0e-9);
    assertEquals(11.63, hub.y, 1.0e-9);
  }

  @Test
  void aPairAcrossTwoColumnsCentresOnTheirSharedEdge() {
    // Side by side, both facing the same way: the hub sits on the edge between the columns.
    final Vec3d hub = SignalClusterFrame.centroid(
        Arrays.asList(point(640, 560, 0.0, 0.1875), point(641, 560, 0.0, 0.1875)),
        new Vec3d(640.5, 11.63, 560.5));
    assertEquals(641.0, hub.x, 1.0e-9);
    assertEquals(560.6875, hub.z, 1.0e-9);
  }

  @Test
  void aPlusRoundAnEmptyColumnCentresOnThatColumn() {
    final double s = 0.1875;
    final Vec3d hub = SignalClusterFrame.centroid(Arrays.asList(
        point(632, 560, 0.0, -s),
        point(632, 562, 0.0, s),
        point(631, 561, -s, 0.0),
        point(633, 561, s, 0.0)), new Vec3d(632.5, 11.63, 561.5));
    assertEquals(632.5, hub.x, 1.0e-9);
    assertEquals(561.5, hub.z, 1.0e-9);
  }

  @Test
  void backToBackHeadsCentreBetweenTheirHousings() {
    // Adjacent columns, each set back toward the other.
    final Vec3d hub = SignalClusterFrame.centroid(
        Arrays.asList(point(620, 560, 0.0, 0.1875), point(620, 561, 0.0, -0.1875)),
        new Vec3d(620.5, 11.63, 560.5));
    assertEquals(620.5, hub.x, 1.0e-9);
    assertEquals(561.0, hub.z, 1.0e-9);
  }
}
