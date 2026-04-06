package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalVisorTypeTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalVisorType value : TrafficSignalVisorType.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalVisorType.class)
  void fromNbtRoundTrip(TrafficSignalVisorType visor) {
    int nbt = visor.toNBT();
    assertEquals(visor, TrafficSignalVisorType.fromNBT(nbt));
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertEquals(TrafficSignalVisorType.values()[0], TrafficSignalVisorType.fromNBT(-1));
    assertEquals(TrafficSignalVisorType.values()[0], TrafficSignalVisorType.fromNBT(999));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalVisorType.class)
  void friendlyNameIsNonNullAndNonEmpty(TrafficSignalVisorType visor) {
    assertNotNull(visor.getFriendlyName());
    assertFalse(visor.getFriendlyName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalVisorType.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalVisorType visor) {
    assertNotNull(visor.getName());
    assertFalse(visor.getName().isEmpty());
  }

  @Test
  void getNextVisorTypeCyclesCorrectly() {
    TrafficSignalVisorType[] values = TrafficSignalVisorType.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalVisorType expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextVisorType());
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalVisorType[] values = TrafficSignalVisorType.values();
    assertEquals(values[0], values[values.length - 1].getNextVisorType());
  }
}
