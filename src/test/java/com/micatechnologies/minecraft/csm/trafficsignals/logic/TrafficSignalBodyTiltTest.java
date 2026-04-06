package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalBodyTiltTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalBodyTilt value : TrafficSignalBodyTilt.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyTilt.class)
  void fromNbtRoundTrip(TrafficSignalBodyTilt tilt) {
    int nbt = tilt.toNBT();
    assertEquals(tilt, TrafficSignalBodyTilt.fromNBT(nbt));
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertEquals(TrafficSignalBodyTilt.values()[0], TrafficSignalBodyTilt.fromNBT(-1));
    assertEquals(TrafficSignalBodyTilt.values()[0], TrafficSignalBodyTilt.fromNBT(999));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyTilt.class)
  void friendlyNameIsNonNullAndNonEmpty(TrafficSignalBodyTilt tilt) {
    assertNotNull(tilt.getFriendlyName());
    assertFalse(tilt.getFriendlyName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyTilt.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalBodyTilt tilt) {
    assertNotNull(tilt.getName());
    assertFalse(tilt.getName().isEmpty());
  }

  @Test
  void getNextTiltCyclesCorrectly() {
    TrafficSignalBodyTilt[] values = TrafficSignalBodyTilt.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalBodyTilt expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextTilt());
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalBodyTilt[] values = TrafficSignalBodyTilt.values();
    assertEquals(values[0], values[values.length - 1].getNextTilt());
  }
}
