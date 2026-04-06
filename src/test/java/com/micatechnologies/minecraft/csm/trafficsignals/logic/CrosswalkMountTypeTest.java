package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CrosswalkMountType} enum: ordinals, NBT round-trip, getNext cycling,
 * getName, and friendly name.
 */
class CrosswalkMountTypeTest {

  @Test
  void uniqueOrdinals() {
    CrosswalkMountType[] values = CrosswalkMountType.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i].ordinal(), values[j].ordinal());
      }
    }
  }

  @Test
  void nbtRoundTrip() {
    for (CrosswalkMountType type : CrosswalkMountType.values()) {
      int nbt = type.toNBT();
      CrosswalkMountType restored = CrosswalkMountType.fromNBT(nbt);
      assertEquals(type, restored);
    }
  }

  @Test
  void fromNbtOutOfBoundsReturnsDefault() {
    assertEquals(CrosswalkMountType.BASE, CrosswalkMountType.fromNBT(-1));
    assertEquals(CrosswalkMountType.BASE,
        CrosswalkMountType.fromNBT(CrosswalkMountType.values().length));
    assertEquals(CrosswalkMountType.BASE, CrosswalkMountType.fromNBT(999));
  }

  @Test
  void getNextCyclesThroughAll() {
    CrosswalkMountType current = CrosswalkMountType.BASE;
    int count = CrosswalkMountType.values().length;
    for (int i = 0; i < count; i++) {
      current = current.getNextMountType();
    }
    // After cycling through all values, should be back to BASE
    assertEquals(CrosswalkMountType.BASE, current);
  }

  @Test
  void getNextMountTypeReturnsCorrectSequence() {
    assertEquals(CrosswalkMountType.REAR, CrosswalkMountType.BASE.getNextMountType());
    assertEquals(CrosswalkMountType.LEFT, CrosswalkMountType.REAR.getNextMountType());
    assertEquals(CrosswalkMountType.RIGHT, CrosswalkMountType.LEFT.getNextMountType());
    assertEquals(CrosswalkMountType.BASE, CrosswalkMountType.RIGHT.getNextMountType());
  }

  @Test
  void getNameNotNull() {
    for (CrosswalkMountType type : CrosswalkMountType.values()) {
      assertNotNull(type.getName());
      assertFalse(type.getName().isEmpty());
    }
  }

  @Test
  void getFriendlyNameNotNull() {
    for (CrosswalkMountType type : CrosswalkMountType.values()) {
      assertNotNull(type.getFriendlyName());
      assertFalse(type.getFriendlyName().isEmpty());
    }
  }

  @Test
  void expectedValues() {
    assertEquals(4, CrosswalkMountType.values().length);
    assertEquals("base", CrosswalkMountType.BASE.getName());
    assertEquals("rear", CrosswalkMountType.REAR.getName());
    assertEquals("left", CrosswalkMountType.LEFT.getName());
    assertEquals("right", CrosswalkMountType.RIGHT.getName());
  }
}
