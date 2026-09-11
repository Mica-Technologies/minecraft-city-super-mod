package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignalVisibilityAreaTest {

  private static SignalVisibilityArea rectangle() {
    return SignalVisibilityArea.of(Arrays.asList(
        new int[]{10, 64, 20},
        new int[]{13, 64, 30}));
  }

  @Test
  void twoPointsExpandToFourCornerRectangleInOrder() {
    SignalVisibilityArea area = rectangle();
    assertNotNull(area);
    assertEquals(4, area.pointCount());
    assertArrayEquals(new int[]{
        10, 64, 20,
        13, 64, 20,
        13, 64, 30,
        10, 64, 30}, area.toIntArray());
  }

  @Test
  void twoPointsGivenInAnyOrderProduceTheSameRectangle() {
    SignalVisibilityArea a = SignalVisibilityArea.of(Arrays.asList(
        new int[]{13, 64, 30}, new int[]{10, 64, 20}));
    assertEquals(rectangle(), a);
  }

  @Test
  void tooFewOrTooManyPointsAreRejected() {
    assertNull(SignalVisibilityArea.of(new ArrayList<>()));
    assertNull(SignalVisibilityArea.of(Arrays.asList(new int[]{0, 0, 0})));
    List<int[]> nine = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      nine.add(new int[]{i, 0, i * i});
    }
    assertNull(SignalVisibilityArea.of(nine));
    assertNotNull(SignalVisibilityArea.of(nine.subList(0, 8)));
  }

  @Test
  void intArrayRoundTrips() {
    SignalVisibilityArea area = SignalVisibilityArea.of(Arrays.asList(
        new int[]{0, 60, 0}, new int[]{5, 60, 1}, new int[]{3, 61, 7}));
    assertNotNull(area);
    assertEquals(area, SignalVisibilityArea.fromIntArray(area.toIntArray()));
    assertEquals(area.hashCode(), SignalVisibilityArea.fromIntArray(area.toIntArray()).hashCode());
  }

  @Test
  void malformedIntArraysAreRejected() {
    assertNull(SignalVisibilityArea.fromIntArray(null));
    assertNull(SignalVisibilityArea.fromIntArray(new int[0]));
    assertNull(SignalVisibilityArea.fromIntArray(new int[]{1, 2, 3}));
    assertNull(SignalVisibilityArea.fromIntArray(new int[]{1, 2, 3, 4, 5, 6, 7}));
    assertNull(SignalVisibilityArea.fromIntArray(new int[SignalVisibilityArea.MAX_POINTS * 3 + 3]));
  }

  @Test
  void surfaceIsOneAboveTheMeanClickedBlock() {
    SignalVisibilityArea area = SignalVisibilityArea.of(Arrays.asList(
        new int[]{0, 60, 0}, new int[]{5, 62, 1}, new int[]{3, 61, 7}));
    assertNotNull(area);
    assertEquals(62.0, area.surfaceY(), 1.0e-9);
  }

  @Test
  void verticesSitAtBlockCentres() {
    SignalVisibilityArea area = rectangle();
    assertEquals(10.5, area.vertexX(0), 1.0e-9);
    assertEquals(20.5, area.vertexZ(0), 1.0e-9);
  }

  @Test
  void containsUsesBlockCentreVertices() {
    SignalVisibilityArea area = rectangle();
    assertTrue(area.containsXZ(11.5, 25.0));
    assertTrue(area.containsXZ(10.6, 20.6));
    assertFalse(area.containsXZ(10.4, 25.0));
    assertFalse(area.containsXZ(11.5, 30.6));
    assertFalse(area.containsXZ(50.0, 50.0));
  }

  @Test
  void containsWorksForAConcavePolygon() {
    // An L shape: a 10x10 square with its top-right 5x5 quadrant removed.
    SignalVisibilityArea area = SignalVisibilityArea.of(Arrays.asList(
        new int[]{0, 0, 0},
        new int[]{10, 0, 0},
        new int[]{10, 0, 5},
        new int[]{5, 0, 5},
        new int[]{5, 0, 10},
        new int[]{0, 0, 10}));
    assertNotNull(area);
    assertTrue(area.containsXZ(2.5, 2.5));
    assertTrue(area.containsXZ(8.0, 2.5));
    assertTrue(area.containsXZ(2.5, 8.0));
    assertFalse(area.containsXZ(8.0, 8.0));
  }

  @Test
  void nearestAndFarthestVerticesAreMeasuredHorizontally() {
    SignalVisibilityArea area = rectangle();
    // A head just north-west of the rectangle's first corner.
    assertEquals(0, area.nearestVertex(9.0, 18.0));
    assertEquals(2, area.farthestVertex(9.0, 18.0));
  }

  @Test
  void nearestBoundaryPointLandsOnAnEdgeOrCorner() {
    SignalVisibilityArea area = rectangle();
    // Left of the left edge: projects onto the edge at the same Z.
    double[] q = area.nearestBoundaryPoint(5.0, 25.0);
    assertEquals(10.5, q[0], 1.0e-9);
    assertEquals(25.0, q[1], 1.0e-9);
    // Diagonally past a corner: the corner itself.
    q = area.nearestBoundaryPoint(0.0, 0.0);
    assertEquals(10.5, q[0], 1.0e-9);
    assertEquals(20.5, q[1], 1.0e-9);
  }

  @Test
  void rangeCheckCoversEveryVertex() {
    SignalVisibilityArea area = rectangle();
    assertTrue(area.isWithinRange(11.5, 25.0, 10.0));
    assertFalse(area.isWithinRange(11.5, 25.0, 4.0));
  }

  @Test
  void pointListReturnsTheVerticesInOrder() {
    List<int[]> points = rectangle().toPointList();
    assertEquals(4, points.size());
    assertArrayEquals(new int[]{13, 64, 30}, points.get(2));
  }
}
