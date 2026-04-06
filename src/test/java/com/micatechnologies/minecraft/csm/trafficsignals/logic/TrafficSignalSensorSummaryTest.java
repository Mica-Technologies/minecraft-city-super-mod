package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrafficSignalSensorSummary} constructor, getters, and aggregate
 * computations.
 */
class TrafficSignalSensorSummaryTest {

  // region: Constructor and basic getters

  @Test
  void constructorStoresAllFields() {
    TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
        10, 2, 3, 1, 4,   // standard: total, E, W, N, S
        20, 5, 6, 4, 5,   // left: total, E, W, N, S
        30, 7, 8, 9, 6,   // protected: total, E, W, N, S
        40, 10, 11, 12, 7  // right: total, E, W, N, S
    );

    assertEquals(10, summary.getStandardTotal());
    assertEquals(2, summary.getStandardEast());
    assertEquals(3, summary.getStandardWest());
    assertEquals(1, summary.getStandardNorth());
    assertEquals(4, summary.getStandardSouth());

    assertEquals(20, summary.getLeftTotal());
    assertEquals(5, summary.getLeftEast());
    assertEquals(6, summary.getLeftWest());
    assertEquals(4, summary.getLeftNorth());
    assertEquals(5, summary.getLeftSouth());

    assertEquals(30, summary.getProtectedTotal());
    assertEquals(7, summary.getProtectedEast());
    assertEquals(8, summary.getProtectedWest());
    assertEquals(9, summary.getProtectedNorth());
    assertEquals(6, summary.getProtectedSouth());

    assertEquals(40, summary.getRightTotal());
    assertEquals(10, summary.getRightEast());
    assertEquals(11, summary.getRightWest());
    assertEquals(12, summary.getRightNorth());
    assertEquals(7, summary.getRightSouth());
  }

  // endregion

  // region: Aggregate totals

  @Test
  void totalAllSumsAllCategories() {
    TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
        10, 0, 0, 0, 0,
        20, 0, 0, 0, 0,
        30, 0, 0, 0, 0,
        40, 0, 0, 0, 0
    );

    assertEquals(100, summary.getTotalAll());
  }

  @Test
  void totalDirectionSumsAcrossCategories() {
    TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
        0, 1, 2, 3, 4,
        0, 5, 6, 7, 8,
        0, 9, 10, 11, 12,
        0, 13, 14, 15, 16
    );

    assertEquals(1 + 5 + 9 + 13, summary.getTotalEast());    // 28
    assertEquals(2 + 6 + 10 + 14, summary.getTotalWest());    // 32
    assertEquals(3 + 7 + 11 + 15, summary.getTotalNorth());   // 36
    assertEquals(4 + 8 + 12 + 16, summary.getTotalSouth());   // 40
  }

  // endregion

  // region: Non-protected totals

  @Test
  void nonProtectedTotalExcludesProtectedAndRight() {
    // nonProtectedTotal = standard + left (excludes protected AND right)
    TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
        10, 1, 2, 3, 4,
        20, 5, 6, 7, 8,
        30, 9, 10, 11, 12,
        40, 13, 14, 15, 16
    );

    // nonProtectedTotalAll = standardTotal + leftTotal
    assertEquals(30, summary.getNonProtectedTotalAll());
    assertEquals(1 + 5, summary.getNonProtectedTotalEast());
    assertEquals(2 + 6, summary.getNonProtectedTotalWest());
    assertEquals(3 + 7, summary.getNonProtectedTotalNorth());
    assertEquals(4 + 8, summary.getNonProtectedTotalSouth());
  }

  // endregion

  // region: Zero summary

  @Test
  void zeroSummary() {
    TrafficSignalSensorSummary summary = new TrafficSignalSensorSummary(
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0
    );

    assertEquals(0, summary.getTotalAll());
    assertEquals(0, summary.getTotalEast());
    assertEquals(0, summary.getTotalWest());
    assertEquals(0, summary.getTotalNorth());
    assertEquals(0, summary.getTotalSouth());
    assertEquals(0, summary.getNonProtectedTotalAll());
    assertEquals(0, summary.getNonProtectedTotalEast());
    assertEquals(0, summary.getNonProtectedTotalWest());
    assertEquals(0, summary.getNonProtectedTotalNorth());
    assertEquals(0, summary.getNonProtectedTotalSouth());
  }

  // endregion
}
