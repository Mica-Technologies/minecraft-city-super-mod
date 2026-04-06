package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CrosswalkVisorType} enum: ordinals, NBT round-trip, getNext cycling,
 * getName, and friendly name.
 */
class CrosswalkVisorTypeTest {

  @Test
  void uniqueOrdinals() {
    CrosswalkVisorType[] values = CrosswalkVisorType.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i].ordinal(), values[j].ordinal());
      }
    }
  }

  @Test
  void nbtRoundTrip() {
    for (CrosswalkVisorType type : CrosswalkVisorType.values()) {
      int nbt = type.toNBT();
      CrosswalkVisorType restored = CrosswalkVisorType.fromNBT(nbt);
      assertEquals(type, restored);
    }
  }

  @Test
  void fromNbtOutOfBoundsReturnsDefault() {
    assertEquals(CrosswalkVisorType.NONE, CrosswalkVisorType.fromNBT(-1));
    assertEquals(CrosswalkVisorType.NONE,
        CrosswalkVisorType.fromNBT(CrosswalkVisorType.values().length));
    assertEquals(CrosswalkVisorType.NONE, CrosswalkVisorType.fromNBT(999));
  }

  @Test
  void getNextCyclesThroughAll() {
    CrosswalkVisorType current = CrosswalkVisorType.NONE;
    int count = CrosswalkVisorType.values().length;
    for (int i = 0; i < count; i++) {
      current = current.getNextVisorType();
    }
    assertEquals(CrosswalkVisorType.NONE, current);
  }

  @Test
  void getNextVisorTypeReturnsCorrectSequence() {
    assertEquals(CrosswalkVisorType.CRATE, CrosswalkVisorType.NONE.getNextVisorType());
    assertEquals(CrosswalkVisorType.HOOD, CrosswalkVisorType.CRATE.getNextVisorType());
    assertEquals(CrosswalkVisorType.NONE, CrosswalkVisorType.HOOD.getNextVisorType());
  }

  @Test
  void getNameNotNull() {
    for (CrosswalkVisorType type : CrosswalkVisorType.values()) {
      assertNotNull(type.getName());
      assertFalse(type.getName().isEmpty());
    }
  }

  @Test
  void getFriendlyNameNotNull() {
    for (CrosswalkVisorType type : CrosswalkVisorType.values()) {
      assertNotNull(type.getFriendlyName());
      assertFalse(type.getFriendlyName().isEmpty());
    }
  }

  @Test
  void expectedValues() {
    assertEquals(3, CrosswalkVisorType.values().length);
    assertEquals("none", CrosswalkVisorType.NONE.getName());
    assertEquals("crate", CrosswalkVisorType.CRATE.getName());
    assertEquals("hood", CrosswalkVisorType.HOOD.getName());
  }
}
