package com.micatechnologies.minecraft.csm.technology.rack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Where units fit, and the saved form of what a rack holds. */
class RackContentsTest {

  @Test
  void theFirstUnitGoesAtTheTop() {
    assertEquals(42, RackContents.EMPTY.findSpanFromTop(1, 42));
    assertEquals(42, RackContents.EMPTY.findSpanFromTop(4, 42));
    assertEquals(12, RackContents.EMPTY.findSpanFromTop(2, 12));
  }

  @Test
  void eachUnitGoesInTheNextFreeSpanDown() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SWITCH_48_PORT, 42);
    assertEquals(41, c.findSpanFromTop(2, 42));
    c = c.with(RackUnitType.SERVER_2U, 41);
    assertEquals(39, c.findSpanFromTop(1, 42));
    assertEquals(3, c.usedU());
  }

  @Test
  void aGapTooSmallIsSkipped() {
    // U42 and U40 taken: a 2U unit cannot use U41 alone and goes below U40
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 42)
        .with(RackUnitType.SERVER_1U, 40);
    assertEquals(41, c.findSpanFromTop(1, 42));
    assertEquals(39, c.findSpanFromTop(2, 42));
  }

  @Test
  void aFullRackHasNoRoom() {
    RackContents c = RackContents.EMPTY;
    for (int top = 42; top >= 4; top -= 4) {
      c = c.with(RackUnitType.STORAGE_4U, top);
    }
    // 40U used: two 1U slots left at U1 and U2
    assertEquals(2, c.findSpanFromTop(2, 42));
    c = c.with(RackUnitType.UPS_2U, 2);
    assertEquals(-1, c.findSpanFromTop(1, 42));
    assertEquals(42, c.usedU());
  }

  @Test
  void aUIsFoundByAnyPartOfItsUnit() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.STORAGE_4U, 30);
    assertEquals(-1, c.indexAt(31));
    for (int u = 27; u <= 30; u++) {
      assertEquals(0, c.indexAt(u));
    }
    assertEquals(-1, c.indexAt(26));
  }

  @Test
  void removingKeepsTheRestInOrder() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 42)
        .with(RackUnitType.PDU, 41).with(RackUnitType.FIREWALL, 40);
    RackContents less = c.without(1);
    assertEquals(2, less.size());
    assertEquals(RackUnitType.SERVER_1U, less.type(0));
    assertEquals(RackUnitType.FIREWALL, less.type(1));
    assertEquals(40, less.top(1));
    assertSame(RackContents.EMPTY, RackContents.EMPTY.with(RackUnitType.PDU, 3).without(0));
  }

  @Test
  void anOverlapIsRefused() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.SERVER_2U, 10);
    assertThrows(IllegalArgumentException.class, () -> c.with(RackUnitType.SERVER_1U, 9));
    assertThrows(IllegalArgumentException.class, () -> c.with(RackUnitType.SERVER_2U, 1));
  }

  @Test
  void bytesRoundTrip() {
    RackContents c = RackContents.EMPTY.with(RackUnitType.MICAFI_GATEWAY, 42)
        .with(RackUnitType.GPU_SERVER_2U, 20).with(RackUnitType.BLANKING_PLATE, 1);
    byte[] bytes = c.toBytes();
    assertArrayEquals(new byte[]{(byte) RackUnitType.MICAFI_GATEWAY.ordinal(), 42,
        (byte) RackUnitType.GPU_SERVER_2U.ordinal(), 20,
        (byte) RackUnitType.BLANKING_PLATE.ordinal(), 1}, bytes);
    RackContents back = RackContents.fromBytes(bytes);
    assertEquals(c, back);
    assertEquals(c.hashCode(), back.hashCode());
  }

  @Test
  void badPairsAreDroppedAndTheRestKept() {
    byte[] bytes = {
        (byte) RackUnitType.SERVER_1U.ordinal(), 42,
        (byte) 200, 30,                                   // a unit this version does not know
        (byte) RackUnitType.SERVER_2U.ordinal(), 42,      // overlaps the first
        (byte) RackUnitType.SERVER_2U.ordinal(), 1,       // reaches below U1
        (byte) RackUnitType.PDU.ordinal(), 43,            // above the tallest rack
        (byte) RackUnitType.PDU.ordinal(), 5,
        7};                                               // an odd byte out
    RackContents c = RackContents.fromBytes(bytes);
    assertEquals(2, c.size());
    assertEquals(RackUnitType.SERVER_1U, c.type(0));
    assertEquals(RackUnitType.PDU, c.type(1));
    assertEquals(5, c.top(1));
    assertTrue(RackContents.fromBytes(null).isEmpty());
    assertTrue(RackContents.fromBytes(new byte[0]).isEmpty());
  }

  @Test
  void equalContentsAreEqualKeys() {
    RackContents a = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 42);
    RackContents b = RackContents.EMPTY.with(RackUnitType.SERVER_1U, 42);
    assertEquals(a, b);
    assertNotEquals(a, RackContents.EMPTY.with(RackUnitType.SERVER_1U, 41));
    assertFalse(a.isEmpty());
  }

  @Test
  void theWallCabinetTakesOnlyShallowUnits() {
    assertTrue(RackFrame.WALL_12U.accepts(RackUnitType.SWITCH_48_PORT));
    assertTrue(RackFrame.WALL_12U.accepts(RackUnitType.PATCH_PANEL));
    assertTrue(RackFrame.WALL_12U.accepts(RackUnitType.MICAFI_GATEWAY));
    assertFalse(RackFrame.WALL_12U.accepts(RackUnitType.SERVER_1U));
    assertFalse(RackFrame.WALL_12U.accepts(RackUnitType.UPS_2U));
    for (RackUnitType t : RackUnitType.values()) {
      assertTrue(RackFrame.CABINET_42U.accepts(t), t.name());
      assertTrue(RackFrame.OPEN_42U.accepts(t), t.name());
    }
  }

  @Test
  void aHeightIsItsU() {
    RackFrame f = RackFrame.CABINET_42U;
    assertEquals(0, f.uAt(1.0f));
    assertEquals(1, f.uAt(2.1f));
    assertEquals(21, f.uAt(15.9f));
    assertEquals(22, f.uAt(16.1f));
    assertEquals(42, f.uAt(29.9f));
    assertEquals(43, f.uAt(31.0f));
  }

  @Test
  void theUnitsFitTheSheet() {
    int row = 0;
    for (RackUnitType t : RackUnitType.values()) {
      assertEquals(row, t.getBandRow(), t.name() + " follows the unit above it on the sheet");
      row += t.getHeightU() * RackUnitType.TEXELS_PER_U;
    }
    assertTrue(row <= RackUnitType.SWATCH_ROW);
  }
}
