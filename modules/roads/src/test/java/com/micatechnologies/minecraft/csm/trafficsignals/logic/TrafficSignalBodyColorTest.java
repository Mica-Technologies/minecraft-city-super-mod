package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrafficSignalBodyColorTest {

  @Test
  void allValuesHaveUniqueOrdinals() {
    Set<Integer> ordinals = new HashSet<>();
    for (TrafficSignalBodyColor value : TrafficSignalBodyColor.values()) {
      assertTrue(ordinals.add(value.ordinal()),
          "Duplicate ordinal: " + value.ordinal() + " for " + value);
    }
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyColor.class)
  void fromNbtRoundTrip(TrafficSignalBodyColor color) {
    int nbt = color.toNBT();
    assertEquals(color, TrafficSignalBodyColor.fromNBT(nbt));
  }

  @Test
  void fromNbtOutOfRangeReturnsSafeDefault() {
    assertEquals(TrafficSignalBodyColor.values()[0], TrafficSignalBodyColor.fromNBT(-1));
    assertEquals(TrafficSignalBodyColor.values()[0], TrafficSignalBodyColor.fromNBT(999));
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyColor.class)
  void friendlyNameIsNonNullAndNonEmpty(TrafficSignalBodyColor color) {
    assertNotNull(color.getFriendlyName());
    assertFalse(color.getFriendlyName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyColor.class)
  void getNameIsNonNullAndNonEmpty(TrafficSignalBodyColor color) {
    assertNotNull(color.getName());
    assertFalse(color.getName().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrafficSignalBodyColor.class)
  void rgbValuesAreInZeroToOneRange(TrafficSignalBodyColor color) {
    assertTrue(color.getRed() >= 0.0f && color.getRed() <= 1.0f,
        "Red out of range for " + color + ": " + color.getRed());
    assertTrue(color.getGreen() >= 0.0f && color.getGreen() <= 1.0f,
        "Green out of range for " + color + ": " + color.getGreen());
    assertTrue(color.getBlue() >= 0.0f && color.getBlue() <= 1.0f,
        "Blue out of range for " + color + ": " + color.getBlue());
  }

  @Test
  void getNextColorCyclesCorrectly() {
    TrafficSignalBodyColor[] values = TrafficSignalBodyColor.values();
    for (int i = 0; i < values.length; i++) {
      TrafficSignalBodyColor expected = values[(i + 1) % values.length];
      assertEquals(expected, values[i].getNextColor());
    }
  }

  @Test
  void lastValueWrapsToFirst() {
    TrafficSignalBodyColor[] values = TrafficSignalBodyColor.values();
    assertEquals(values[0], values[values.length - 1].getNextColor());
  }

  @Test
  void glossyBlackIsAllZeros() {
    assertEquals(0.0f, TrafficSignalBodyColor.GLOSSY_BLACK.getRed());
    assertEquals(0.0f, TrafficSignalBodyColor.GLOSSY_BLACK.getGreen());
    assertEquals(0.0f, TrafficSignalBodyColor.GLOSSY_BLACK.getBlue());
  }
}
