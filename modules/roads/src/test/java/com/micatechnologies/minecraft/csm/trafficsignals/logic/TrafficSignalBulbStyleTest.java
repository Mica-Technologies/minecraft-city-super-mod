package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalBulbStyleTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalBulbStyle value : TrafficSignalBulbStyle.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbStyle.class)
  void fromNbtRoundTrip(TrafficSignalBulbStyle style) {
    int nbt = style.toNBT();
    assertEquals(style, TrafficSignalBulbStyle.fromNBT(nbt));
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertEquals(TrafficSignalBulbStyle.values()[0], TrafficSignalBulbStyle.fromNBT(-1));
    assertEquals(TrafficSignalBulbStyle.values()[0], TrafficSignalBulbStyle.fromNBT(999));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbStyle.class)
  void friendlyNameIsNonNullAndNonEmpty(TrafficSignalBulbStyle style) {
    assertNotNull(style.getFriendlyName());
    assertFalse(style.getFriendlyName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbStyle.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalBulbStyle style) {
    assertNotNull(style.getName());
    assertFalse(style.getName().isEmpty());
  }

  @Test
  void getNextBulbStyleCyclesCorrectly() {
    TrafficSignalBulbStyle[] values = TrafficSignalBulbStyle.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalBulbStyle expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextBulbStyle());
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalBulbStyle[] values = TrafficSignalBulbStyle.values();
    assertEquals(values[0], values[values.length - 1].getNextBulbStyle());
  }
}
