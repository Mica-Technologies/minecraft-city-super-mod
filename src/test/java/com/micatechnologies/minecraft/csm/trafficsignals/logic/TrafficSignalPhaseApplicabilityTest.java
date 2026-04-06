package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalPhaseApplicabilityTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalPhaseApplicability value : TrafficSignalPhaseApplicability.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalPhaseApplicability.class)
  void fromNbtRoundTrip(TrafficSignalPhaseApplicability applicability) {
    int nbt = applicability.toNBT();
    assertEquals(applicability, TrafficSignalPhaseApplicability.fromNBT(nbt));
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    // Default is NONE (ordinal 1) per source code
    assertEquals(TrafficSignalPhaseApplicability.NONE,
        TrafficSignalPhaseApplicability.fromNBT(-1));
    assertEquals(TrafficSignalPhaseApplicability.NONE,
        TrafficSignalPhaseApplicability.fromNBT(999));
    assertEquals(TrafficSignalPhaseApplicability.NONE,
        TrafficSignalPhaseApplicability.fromNBT(Integer.MIN_VALUE));
  }

  @Test
  void hasExpectedValues() {
    // Verify key values exist
    assertNotNull(TrafficSignalPhaseApplicability.NO_POWER);
    assertNotNull(TrafficSignalPhaseApplicability.NONE);
    assertNotNull(TrafficSignalPhaseApplicability.PEDESTRIAN);
    assertNotNull(TrafficSignalPhaseApplicability.ALL_EAST);
    assertNotNull(TrafficSignalPhaseApplicability.ALL_RED);
    assertNotNull(TrafficSignalPhaseApplicability.WRONG_WAY_IDLE);
    assertNotNull(TrafficSignalPhaseApplicability.OVERHEIGHT_ACTIVE);
  }

  @Test
  void noPowerIsFirstValue() {
    assertEquals(0, TrafficSignalPhaseApplicability.NO_POWER.ordinal());
  }

  @Test
  void noneIsSecondValue() {
    assertEquals(1, TrafficSignalPhaseApplicability.NONE.ordinal());
  }
}
