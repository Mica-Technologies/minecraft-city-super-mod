package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CrosswalkDisplayType} enum: ordinals, NBT round-trip, getName, and
 * friendly name.
 */
class CrosswalkDisplayTypeTest {

  @Test
  void uniqueOrdinals() {
    CrosswalkDisplayType[] values = CrosswalkDisplayType.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i].ordinal(), values[j].ordinal());
      }
    }
  }

  @Test
  void nbtRoundTrip() {
    for (CrosswalkDisplayType type : CrosswalkDisplayType.values()) {
      int nbt = type.toNBT();
      CrosswalkDisplayType restored = CrosswalkDisplayType.fromNBT(nbt);
      assertEquals(type, restored);
    }
  }

  @Test
  void fromNbtOutOfBoundsReturnsDefault() {
    assertEquals(CrosswalkDisplayType.SYMBOL, CrosswalkDisplayType.fromNBT(-1));
    assertEquals(CrosswalkDisplayType.SYMBOL,
        CrosswalkDisplayType.fromNBT(CrosswalkDisplayType.values().length));
    assertEquals(CrosswalkDisplayType.SYMBOL, CrosswalkDisplayType.fromNBT(999));
  }

  @Test
  void getNameNotNull() {
    for (CrosswalkDisplayType type : CrosswalkDisplayType.values()) {
      assertNotNull(type.getName());
      assertFalse(type.getName().isEmpty());
    }
  }

  @Test
  void getFriendlyNameNotNull() {
    for (CrosswalkDisplayType type : CrosswalkDisplayType.values()) {
      assertNotNull(type.getFriendlyName());
      assertFalse(type.getFriendlyName().isEmpty());
    }
  }

  @Test
  void expectedValues() {
    assertEquals(2, CrosswalkDisplayType.values().length);
    assertEquals("symbol", CrosswalkDisplayType.SYMBOL.getName());
    assertEquals("text", CrosswalkDisplayType.TEXT.getName());
  }
}
