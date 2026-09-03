package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalControllerModeTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalControllerMode value : TrafficSignalControllerMode.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalControllerMode.class)
  void fromNbtRoundTrip(TrafficSignalControllerMode mode) {
    int nbt = mode.toNBT();
    assertEquals(mode, TrafficSignalControllerMode.fromNBT(nbt));
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertEquals(TrafficSignalControllerMode.values()[0], TrafficSignalControllerMode.fromNBT(-1));
    assertEquals(TrafficSignalControllerMode.values()[0], TrafficSignalControllerMode.fromNBT(999));
    assertEquals(TrafficSignalControllerMode.FLASH, TrafficSignalControllerMode.fromNBT(-100));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalControllerMode.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalControllerMode mode) {
    assertNotNull(mode.getName());
    assertFalse(mode.getName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalControllerMode.class)
  void tickRatesArePositive(TrafficSignalControllerMode mode) {
    assertTrue(mode.getTickRate() > 0,
        "Tick rate should be positive for " + mode + ", got " + mode.getTickRate());
  }

  @Test
  void getNextModeCyclesCorrectly() {
    TrafficSignalControllerMode[] values = TrafficSignalControllerMode.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalControllerMode expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextMode(),
          "getNextMode from " + values[i] + " should return " + expected);
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalControllerMode[] values = TrafficSignalControllerMode.values();
    assertEquals(values[0], values[values.length - 1].getNextMode());
  }

  @Test
  void knownTickRateValues() {
    assertEquals(10, TrafficSignalControllerMode.FLASH.getTickRate());
    assertEquals(20, TrafficSignalControllerMode.NORMAL.getTickRate());
    assertEquals(80, TrafficSignalControllerMode.RAMP_METER_FULL_TIME.getTickRate());
    assertEquals(300, TrafficSignalControllerMode.MANUAL_OFF.getTickRate());
  }
}
