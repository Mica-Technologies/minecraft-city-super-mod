package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalBulbTypeTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalBulbType value : TrafficSignalBulbType.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbType.class)
  void fromNbtRoundTrip(TrafficSignalBulbType type) {
    int nbt = type.toNBT();
    assertEquals(type, TrafficSignalBulbType.fromNBT(nbt),
        "fromNBT(toNBT()) should return same value for " + type);
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertDoesNotThrow(() -> TrafficSignalBulbType.fromNBT(-1));
    assertDoesNotThrow(() -> TrafficSignalBulbType.fromNBT(999));
    assertEquals(TrafficSignalBulbType.values()[0], TrafficSignalBulbType.fromNBT(-1));
    assertEquals(TrafficSignalBulbType.values()[0], TrafficSignalBulbType.fromNBT(999));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbType.class)
  void friendlyNameIsNonNullAndNonEmpty(TrafficSignalBulbType type) {
    assertNotNull(type.getFriendlyName());
    assertFalse(type.getFriendlyName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbType.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalBulbType type) {
    assertNotNull(type.getName());
    assertFalse(type.getName().isEmpty());
  }

  @Test
  void getNextBulbTypeCyclesCorrectly() {
    TrafficSignalBulbType[] values = TrafficSignalBulbType.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalBulbType expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextBulbType(),
          "getNextBulbType from " + values[i] + " should return " + expected);
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalBulbType[] values = TrafficSignalBulbType.values();
    assertEquals(values[0], values[values.length - 1].getNextBulbType());
  }
}
