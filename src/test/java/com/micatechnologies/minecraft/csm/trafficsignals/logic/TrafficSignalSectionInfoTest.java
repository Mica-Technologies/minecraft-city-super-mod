package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrafficSignalSectionInfoTest {

  @Test
  void defaultConstructorSetsExpectedDefaults() {
    var info = new TrafficSignalSectionInfo();
    assertEquals(TrafficSignalBodyColor.FLAT_BLACK, info.getBodyColor());
    assertEquals(TrafficSignalBodyColor.FLAT_BLACK, info.getDoorColor());
    assertEquals(TrafficSignalBodyColor.FLAT_BLACK, info.getVisorColor());
    assertEquals(TrafficSignalVisorType.TUNNEL, info.getVisorType());
    assertEquals(TrafficSignalBulbStyle.LED, info.getBulbStyle());
    assertEquals(TrafficSignalBulbType.BALL, info.getBulbType());
    assertEquals(TrafficSignalBulbColor.GREEN, info.getBulbColor());
    assertEquals(TrafficSignalBulbColor.GREEN, info.getBulbCustomColor());
    assertFalse(info.isBulbLit());
    assertFalse(info.isBulbFlashing());
  }

  @Test
  void customConstructorWithoutLitStoresValues() {
    var info = new TrafficSignalSectionInfo(
        TrafficSignalBodyColor.YELLOW,
        TrafficSignalBodyColor.BATTLESHIP_GRAY,
        TrafficSignalBodyColor.DARK_OLIVE_GREEN,
        TrafficSignalVisorType.CIRCLE,
        TrafficSignalBulbStyle.INCANDESCENT,
        TrafficSignalBulbType.LEFT,
        TrafficSignalBulbColor.RED,
        true // flashing
    );
    assertEquals(TrafficSignalBodyColor.YELLOW, info.getBodyColor());
    assertEquals(TrafficSignalBodyColor.BATTLESHIP_GRAY, info.getDoorColor());
    assertEquals(TrafficSignalBodyColor.DARK_OLIVE_GREEN, info.getVisorColor());
    assertEquals(TrafficSignalVisorType.CIRCLE, info.getVisorType());
    assertEquals(TrafficSignalBulbStyle.INCANDESCENT, info.getBulbStyle());
    assertEquals(TrafficSignalBulbType.LEFT, info.getBulbType());
    assertEquals(TrafficSignalBulbColor.RED, info.getBulbColor());
    assertEquals(TrafficSignalBulbColor.RED, info.getBulbCustomColor());
    assertFalse(info.isBulbLit()); // defaults to false in this constructor
    assertTrue(info.isBulbFlashing());
  }

  @Test
  void customConstructorWithLitStoresValues() {
    var info = new TrafficSignalSectionInfo(
        TrafficSignalBodyColor.GLOSSY_BLACK,
        TrafficSignalBodyColor.GLOSSY_BLACK,
        TrafficSignalBodyColor.GLOSSY_BLACK,
        TrafficSignalVisorType.NONE,
        TrafficSignalBulbStyle.GTX,
        TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.YELLOW,
        true, // lit
        false // flashing
    );
    assertEquals(TrafficSignalBodyColor.GLOSSY_BLACK, info.getBodyColor());
    assertEquals(TrafficSignalVisorType.NONE, info.getVisorType());
    assertEquals(TrafficSignalBulbStyle.GTX, info.getBulbStyle());
    assertEquals(TrafficSignalBulbColor.YELLOW, info.getBulbColor());
    assertTrue(info.isBulbLit());
    assertFalse(info.isBulbFlashing());
  }

  @Test
  void customConstructorWithCustomColorStoresValues() {
    var info = new TrafficSignalSectionInfo(
        TrafficSignalBodyColor.FLAT_BLACK,
        TrafficSignalBodyColor.FLAT_BLACK,
        TrafficSignalBodyColor.FLAT_BLACK,
        TrafficSignalVisorType.TUNNEL,
        TrafficSignalBulbStyle.LED,
        TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.GREEN,
        TrafficSignalBulbColor.RED, // custom color different from main
        true,
        false
    );
    assertEquals(TrafficSignalBulbColor.GREEN, info.getBulbColor());
    assertEquals(TrafficSignalBulbColor.RED, info.getBulbCustomColor());
  }

  @Test
  void settersUpdateValues() {
    var info = new TrafficSignalSectionInfo();

    info.setBodyColor(TrafficSignalBodyColor.YELLOW);
    assertEquals(TrafficSignalBodyColor.YELLOW, info.getBodyColor());

    info.setDoorColor(TrafficSignalBodyColor.BATTLESHIP_GRAY);
    assertEquals(TrafficSignalBodyColor.BATTLESHIP_GRAY, info.getDoorColor());

    info.setVisorColor(TrafficSignalBodyColor.DARK_OLIVE_GREEN);
    assertEquals(TrafficSignalBodyColor.DARK_OLIVE_GREEN, info.getVisorColor());

    info.setVisorType(TrafficSignalVisorType.BARLO);
    assertEquals(TrafficSignalVisorType.BARLO, info.getVisorType());

    info.setBulbStyle(TrafficSignalBulbStyle.GTX);
    assertEquals(TrafficSignalBulbStyle.GTX, info.getBulbStyle());

    info.setBulbType(TrafficSignalBulbType.UTURN);
    assertEquals(TrafficSignalBulbType.UTURN, info.getBulbType());

    info.setBulbColor(TrafficSignalBulbColor.RED);
    assertEquals(TrafficSignalBulbColor.RED, info.getBulbColor());

    info.setBulbCustomColor(TrafficSignalBulbColor.YELLOW);
    assertEquals(TrafficSignalBulbColor.YELLOW, info.getBulbCustomColor());

    info.setBulbLit(true);
    assertTrue(info.isBulbLit());

    info.setBulbFlashing(true);
    assertTrue(info.isBulbFlashing());
  }

  @Test
  void toNbtArrayRoundTrips() {
    var original = new TrafficSignalSectionInfo(
        TrafficSignalBodyColor.YELLOW,
        TrafficSignalBodyColor.BATTLESHIP_GRAY,
        TrafficSignalBodyColor.DARK_OLIVE_GREEN,
        TrafficSignalVisorType.CUTAWAY,
        TrafficSignalBulbStyle.LED_DOTTED,
        TrafficSignalBulbType.RIGHT,
        TrafficSignalBulbColor.RED,
        TrafficSignalBulbColor.YELLOW,
        true,
        true
    );

    int[] array = original.toNBTArray();
    assertEquals(10, array.length);

    var restored = TrafficSignalSectionInfo.fromNBTArray(array);
    assertEquals(original.getBodyColor(), restored.getBodyColor());
    assertEquals(original.getDoorColor(), restored.getDoorColor());
    assertEquals(original.getVisorColor(), restored.getVisorColor());
    assertEquals(original.getVisorType(), restored.getVisorType());
    assertEquals(original.getBulbStyle(), restored.getBulbStyle());
    assertEquals(original.getBulbType(), restored.getBulbType());
    assertEquals(original.getBulbColor(), restored.getBulbColor());
    assertEquals(original.getBulbCustomColor(), restored.getBulbCustomColor());
    assertEquals(original.isBulbLit(), restored.isBulbLit());
    assertEquals(original.isBulbFlashing(), restored.isBulbFlashing());
  }

  @Test
  void fromNbtArrayWithNullReturnsDefaults() {
    var result = TrafficSignalSectionInfo.fromNBTArray(null);
    assertNotNull(result);
    assertEquals(TrafficSignalBodyColor.FLAT_BLACK, result.getBodyColor());
  }

  @Test
  void fromNbtArrayWithTooShortArrayReturnsDefaults() {
    var result = TrafficSignalSectionInfo.fromNBTArray(new int[]{1, 2, 3});
    assertNotNull(result);
    assertEquals(TrafficSignalBodyColor.FLAT_BLACK, result.getBodyColor());
  }
}
