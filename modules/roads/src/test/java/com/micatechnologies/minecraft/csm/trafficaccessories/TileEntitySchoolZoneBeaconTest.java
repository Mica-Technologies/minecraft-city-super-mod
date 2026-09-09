package com.micatechnologies.minecraft.csm.trafficaccessories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntitySchoolZoneBeaconTest {

  @Test
  void ordinaryWindowCoversStartHourButNotEndHour() {
    // A zone posted 7 to 9 is flashing at 7 and 8 and dark at 9 — the end hour is the moment
    // it stops, not the last hour it runs.
    assertFalse(TileEntitySchoolZoneBeacon.inWindow(6, 7, 9));
    assertTrue(TileEntitySchoolZoneBeacon.inWindow(7, 7, 9));
    assertTrue(TileEntitySchoolZoneBeacon.inWindow(8, 7, 9));
    assertFalse(TileEntitySchoolZoneBeacon.inWindow(9, 7, 9));
    assertFalse(TileEntitySchoolZoneBeacon.inWindow(23, 7, 9));
  }

  @Test
  void windowEndingBeforeItStartsWrapsPastMidnight() {
    // 22 to 02 has to mean "late evening into the small hours", not "empty".
    assertTrue(TileEntitySchoolZoneBeacon.inWindow(22, 22, 2));
    assertTrue(TileEntitySchoolZoneBeacon.inWindow(23, 22, 2));
    assertTrue(TileEntitySchoolZoneBeacon.inWindow(0, 22, 2));
    assertTrue(TileEntitySchoolZoneBeacon.inWindow(1, 22, 2));
    assertFalse(TileEntitySchoolZoneBeacon.inWindow(2, 22, 2));
    assertFalse(TileEntitySchoolZoneBeacon.inWindow(12, 22, 2));
  }

  @Test
  void zeroLengthWindowIsOffRatherThanAllDay() {
    // Both readings are defensible; off is the safe one, because the alternative is a beacon
    // that flashes around the clock the moment someone equalises the two hours by accident.
    for (int hour = 0; hour < 24; hour++) {
      assertFalse(TileEntitySchoolZoneBeacon.inWindow(hour, 8, 8), "at hour " + hour);
    }
  }

  @Test
  void scheduleHoursRoundTripAndWrapIntoRange() {
    TileEntitySchoolZoneBeacon te = new TileEntitySchoolZoneBeacon();
    te.setScheduleHour(0, 7);
    te.setScheduleHour(1, 9);
    te.setScheduleHour(2, 14);
    te.setScheduleHour(3, 16);
    assertEquals(7, te.getScheduleHour(0));
    assertEquals(9, te.getScheduleHour(1));
    assertEquals(14, te.getScheduleHour(2));
    assertEquals(16, te.getScheduleHour(3));

    // Stepping back off zero and forward off 23 both have to land somewhere legal, since the
    // GUI drives these by repeated +1/-1 rather than by entering a number.
    te.setScheduleHour(0, -1);
    assertEquals(23, te.getScheduleHour(0));
    te.setScheduleHour(0, 24);
    assertEquals(0, te.getScheduleHour(0));
  }

  @Test
  void nbtRoundTripsEverySetting() {
    TileEntitySchoolZoneBeacon te = new TileEntitySchoolZoneBeacon();
    te.setSpeedLimit(15);
    te.setScaleIndex(3);
    te.setArrangement(TileEntitySchoolZoneBeacon.BEACONS_TWO_ABOVE);
    te.setBeaconSize(TileEntitySchoolZoneBeacon.BEACON_SIZE_12_INCH);
    te.setBannerColor(MutcdSignFaceColor.YELLOW);
    te.setMode(TileEntitySchoolZoneBeacon.MODE_ON);
    te.setScheduleHour(0, 6);
    te.setScheduleHour(1, 10);
    te.setScheduleHour(2, 15);
    te.setScheduleHour(3, 17);

    NBTTagCompound nbt = te.writeNBT(new NBTTagCompound());
    TileEntitySchoolZoneBeacon restored = new TileEntitySchoolZoneBeacon();
    restored.readNBT(nbt);

    assertEquals(15, restored.getSpeedLimit());
    assertEquals(3, restored.getScaleIndex());
    assertEquals(TileEntitySchoolZoneBeacon.BEACONS_TWO_ABOVE, restored.getArrangement());
    assertEquals(TileEntitySchoolZoneBeacon.BEACON_SIZE_12_INCH, restored.getBeaconSize());
    assertEquals(MutcdSignFaceColor.YELLOW, restored.getBannerColor());
    assertEquals(TileEntitySchoolZoneBeacon.MODE_ON, restored.getMode());
    assertEquals(6, restored.getScheduleHour(0));
    assertEquals(10, restored.getScheduleHour(1));
    assertEquals(15, restored.getScheduleHour(2));
    assertEquals(17, restored.getScheduleHour(3));
  }

  @Test
  void emptyCompoundProducesUsableDefaults() {
    // A beacon placed by hand reads an empty compound before anyone configures it. A posted
    // speed of zero would be the give-away that the absent tag had been taken literally.
    TileEntitySchoolZoneBeacon te = new TileEntitySchoolZoneBeacon();
    te.readNBT(new NBTTagCompound());
    assertEquals(20, te.getSpeedLimit());
    assertEquals(TileEntitySchoolZoneBeacon.MODE_SCHEDULED, te.getMode());
    assertEquals(TileEntitySchoolZoneBeacon.BEACONS_ABOVE, te.getArrangement());
    assertEquals(TileEntitySchoolZoneBeacon.BEACON_SIZE_8_INCH, te.getBeaconSize());
    assertTrue(te.getScale() > 0.0f);
  }

  @Test
  void anUnconfiguredPlaqueStaysFluorescentYellowGreen() {
    // Every beacon built before the colour was a choice was fluorescent yellow-green, which is
    // ordinal 1 -- so an absent tag must not fall through to the enum's zero default, or a
    // world reload would quietly repaint every existing sign yellow.
    TileEntitySchoolZoneBeacon te = new TileEntitySchoolZoneBeacon();
    te.readNBT(new NBTTagCompound());
    assertEquals(MutcdSignFaceColor.FLUORESCENT_YELLOW_GREEN, te.getBannerColor());

    // A written yellow is still yellow, which is the other half of that distinction.
    TileEntitySchoolZoneBeacon yellow = new TileEntitySchoolZoneBeacon();
    yellow.setBannerColor(MutcdSignFaceColor.YELLOW);
    TileEntitySchoolZoneBeacon restored = new TileEntitySchoolZoneBeacon();
    restored.readNBT(yellow.writeNBT(new NBTTagCompound()));
    assertEquals(MutcdSignFaceColor.YELLOW, restored.getBannerColor());
  }

  @Test
  void arrangementZeroIsKeptWhenItWasActuallyWritten() {
    // Index 0 is the two-beacon arrangement and the absent-tag default is the single one, so
    // the two cases have to stay distinguishable — reading a written 0 as "unset" would quietly
    // undo the setting every time the chunk reloaded.
    TileEntitySchoolZoneBeacon te = new TileEntitySchoolZoneBeacon();
    te.setArrangement(TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW);
    TileEntitySchoolZoneBeacon restored = new TileEntitySchoolZoneBeacon();
    restored.readNBT(te.writeNBT(new NBTTagCompound()));
    assertEquals(TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW, restored.getArrangement());
  }

  @Test
  void speedLimitIsHeldInsideItsPostedRange() {
    TileEntitySchoolZoneBeacon te = new TileEntitySchoolZoneBeacon();
    te.setSpeedLimit(1000);
    assertEquals(TileEntitySchoolZoneBeacon.MAX_SPEED, te.getSpeedLimit());
    te.setSpeedLimit(-5);
    assertEquals(TileEntitySchoolZoneBeacon.MIN_SPEED, te.getSpeedLimit());
  }
}
