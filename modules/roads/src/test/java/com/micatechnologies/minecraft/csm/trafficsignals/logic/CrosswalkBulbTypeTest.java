package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CrosswalkBulbType} enum: ordinals, NBT round-trip, getNext cycling,
 * getName, and friendly name.
 */
class CrosswalkBulbTypeTest {

  @Test
  void uniqueOrdinals() {
    CrosswalkBulbType[] values = CrosswalkBulbType.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i].ordinal(), values[j].ordinal());
      }
    }
  }

  @Test
  void nbtRoundTrip() {
    for (CrosswalkBulbType type : CrosswalkBulbType.values()) {
      int nbt = type.toNBT();
      CrosswalkBulbType restored = CrosswalkBulbType.fromNBT(nbt);
      assertEquals(type, restored);
    }
  }

  @Test
  void fromNbtOutOfBoundsReturnsDefault() {
    assertEquals(CrosswalkBulbType.WORDED, CrosswalkBulbType.fromNBT(-1));
    assertEquals(CrosswalkBulbType.WORDED,
        CrosswalkBulbType.fromNBT(CrosswalkBulbType.values().length));
    assertEquals(CrosswalkBulbType.WORDED, CrosswalkBulbType.fromNBT(999));
  }

  @Test
  void getNextCyclesThroughAll() {
    CrosswalkBulbType current = CrosswalkBulbType.WORDED;
    int count = CrosswalkBulbType.values().length;
    for (int i = 0; i < count; i++) {
      current = current.getNextBulbType();
    }
    assertEquals(CrosswalkBulbType.WORDED, current);
  }

  @Test
  void getNextBulbTypeReturnsCorrectSequence() {
    assertEquals(CrosswalkBulbType.HAND_MAN_COUNTDOWN, CrosswalkBulbType.WORDED.getNextBulbType());
    assertEquals(CrosswalkBulbType.WORDED, CrosswalkBulbType.HAND_MAN_COUNTDOWN.getNextBulbType());
  }

  @Test
  void getNameNotNull() {
    for (CrosswalkBulbType type : CrosswalkBulbType.values()) {
      assertNotNull(type.getName());
      assertFalse(type.getName().isEmpty());
    }
  }

  @Test
  void getFriendlyNameNotNull() {
    for (CrosswalkBulbType type : CrosswalkBulbType.values()) {
      assertNotNull(type.getFriendlyName());
      assertFalse(type.getFriendlyName().isEmpty());
    }
  }

  @Test
  void expectedValues() {
    assertEquals(2, CrosswalkBulbType.values().length);
    assertEquals("worded", CrosswalkBulbType.WORDED.getName());
    assertEquals("hand_man_countdown", CrosswalkBulbType.HAND_MAN_COUNTDOWN.getName());
  }
}
