package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalBulbColorTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalBulbColor value : TrafficSignalBulbColor.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbColor.class)
  void fromNbtRoundTrip(TrafficSignalBulbColor color) {
    int nbt = color.toNBT();
    assertEquals(color, TrafficSignalBulbColor.fromNBT(nbt),
        "fromNBT(toNBT()) should return same value for " + color);
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertDoesNotThrow(() -> TrafficSignalBulbColor.fromNBT(-1));
    assertDoesNotThrow(() -> TrafficSignalBulbColor.fromNBT(999));
    assertNotNull(TrafficSignalBulbColor.fromNBT(-1));
    assertNotNull(TrafficSignalBulbColor.fromNBT(999));
    // Default is first value (ordinal 0)
    assertEquals(TrafficSignalBulbColor.values()[0], TrafficSignalBulbColor.fromNBT(-1));
    assertEquals(TrafficSignalBulbColor.values()[0], TrafficSignalBulbColor.fromNBT(999));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbColor.class)
  void friendlyNameIsNonNullAndNonEmpty(TrafficSignalBulbColor color) {
    assertNotNull(color.getFriendlyName(), "friendlyName should not be null for " + color);
    assertFalse(color.getFriendlyName().isEmpty(), "friendlyName should not be empty for " + color);
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBulbColor.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalBulbColor color) {
    assertNotNull(color.getName(), "getName should not be null for " + color);
    assertFalse(color.getName().isEmpty(), "getName should not be empty for " + color);
  }

  @Test
  void getNextBulbColorCyclesCorrectly() {
    TrafficSignalBulbColor[] values = TrafficSignalBulbColor.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalBulbColor expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextBulbColor(),
          "getNextBulbColor from " + values[i] + " should return " + expected);
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalBulbColor[] values = TrafficSignalBulbColor.values();
    TrafficSignalBulbColor last = values[values.length - 1];
    assertEquals(values[0], last.getNextBulbColor(),
        "Last value should wrap to first");
  }
}
