package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/** Fire alarm zones (#335): which appliances sound for which alarm, and old worlds. */
class FireAlarmZonesTest {

  private static final BlockPos A = new BlockPos(10, 64, 10);
  private static final BlockPos B = new BlockPos(11, 64, 10);
  private static final BlockPos C = new BlockPos(12, 64, 10);

  @Test
  void generalApplianceSoundsForAnyAlarm() {
    assertTrue(FireAlarmZones.sounds(0, FireAlarmZones.ALL));
    assertTrue(FireAlarmZones.sounds(0, FireAlarmZones.alarmMaskFor(3)));
    assertFalse(FireAlarmZones.sounds(0, 0), "nothing sounds with no zone in alarm");
  }

  @Test
  void zonedApplianceSoundsOnlyForItsZone() {
    int zone3 = FireAlarmZones.alarmMaskFor(3);
    assertTrue(FireAlarmZones.sounds(3, zone3));
    assertFalse(FireAlarmZones.sounds(4, zone3));
    assertTrue(FireAlarmZones.sounds(4, zone3 | FireAlarmZones.alarmMaskFor(4)));
    for (int z = 1; z <= FireAlarmZones.MAX_ZONE; z++) {
      assertTrue(FireAlarmZones.sounds(z, FireAlarmZones.ALL), "every zone sounds for ALL");
    }
  }

  @Test
  void generalDeviceAlarmsEveryZone() {
    assertEquals(FireAlarmZones.ALL, FireAlarmZones.alarmMaskFor(0));
    assertEquals(1 << 5, FireAlarmZones.alarmMaskFor(5));
    assertEquals(FireAlarmZones.ALL, FireAlarmZones.alarmMaskFor(42), "out of range is general");
  }

  @Test
  void linkerCycleWraps() {
    int z = 0;
    for (int i = 0; i < FireAlarmZones.MAX_ZONE; i++) {
      z = FireAlarmZones.next(z);
      assertEquals(i + 1, z);
    }
    assertEquals(0, FireAlarmZones.next(z));
  }

  @Test
  void displayText() {
    assertEquals("ALL ZONES", FireAlarmZones.describeMask(FireAlarmZones.ALL));
    assertEquals("ZONE 2", FireAlarmZones.describeMask(1 << 2));
    assertEquals("ZONES 1,3", FireAlarmZones.describeMask((1 << 1) | (1 << 3)));
    assertEquals("", FireAlarmZones.describeMask(0));
  }

  @Test
  void unzonedPanelReadsEveryApplianceAsGeneral() {
    NBTTagCompound old = new NBTTagCompound();
    old.setString("apps", "10 64 10\n11 64 10\n");
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.readNBT(old);
    assertEquals(0, panel.getApplianceZone(A));
    assertEquals(0, panel.getApplianceZone(B));
    assertFalse(panel.hasZonedAppliances());
    NBTTagCompound out = panel.writeNBT(new NBTTagCompound());
    assertFalse(out.hasKey("apZ"), "an unzoned panel writes no zone data");
    assertFalse(out.hasKey("aZn"));
  }

  @Test
  void alarmSavedBeforeZonesSoundsEverything() {
    NBTTagCompound old = new NBTTagCompound();
    old.setBoolean("a", true);
    old.setString("apps", "10 64 10\n");
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.readNBT(old);
    assertTrue(panel.getAlarmState());
    assertEquals(FireAlarmZones.ALL, panel.getAlarmZones());
  }

  @Test
  void applianceZonesRoundTrip() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.addLinkedAlarm(A, 3);
    panel.addLinkedAlarm(B, 0);
    panel.addLinkedAlarm(C, 8);
    TileEntityFireAlarmControlPanel copy = new TileEntityFireAlarmControlPanel();
    copy.readNBT(panel.writeNBT(new NBTTagCompound()));
    assertEquals(3, copy.getApplianceZone(A));
    assertEquals(0, copy.getApplianceZone(B));
    assertEquals(8, copy.getApplianceZone(C));
    assertTrue(copy.hasZonedAppliances());
  }

  @Test
  void relinkingMovesAnApplianceBetweenZones() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    assertTrue(panel.addLinkedAlarm(A, 2));
    assertFalse(panel.addLinkedAlarm(A, 2), "same zone again changes nothing");
    assertTrue(panel.addLinkedAlarm(A, 5));
    assertEquals(5, panel.getApplianceZone(A));
    assertTrue(panel.addLinkedAlarm(A, 0));
    assertFalse(panel.hasZonedAppliances());
    assertEquals(1, panel.getConnectedAppliances().size());
  }

  @Test
  void unlinkingDropsTheZone() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.addLinkedAlarm(A, 4);
    panel.removeLinkedAlarm(A);
    assertFalse(panel.hasZonedAppliances());
    panel.addLinkedAlarm(A);
    assertEquals(0, panel.getApplianceZone(A), "a fresh link starts general");
  }

  @Test
  void deviceInAZoneAlarmsOnlyThatZoneAndASecondAddsItsOwn() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.activateAlarmFrom(A, "csm:test", 3, false);
    assertEquals(1 << 3, panel.getAlarmZones());
    assertEquals(3, panel.getAlarmOriginZone());
    panel.activateAlarmFrom(B, "csm:test", 6, false);
    assertEquals((1 << 3) | (1 << 6), panel.getAlarmZones());
    assertEquals(A, panel.getAlarmOriginPos(), "the first device stays the origin");
    assertEquals(3, panel.getAlarmOriginZone());
  }

  @Test
  void unzonedDeviceAlarmsEverything() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.activateAlarmFrom(A, "csm:test");
    assertEquals(FireAlarmZones.ALL, panel.getAlarmZones());
  }

  @Test
  void drillRedstoneAndApiAlarmEveryZone() {
    TileEntityFireAlarmControlPanel drill = new TileEntityFireAlarmControlPanel();
    drill.startDrill();
    assertEquals(FireAlarmZones.ALL, drill.getAlarmZones());

    TileEntityFireAlarmControlPanel external = new TileEntityFireAlarmControlPanel();
    external.setAlarmState(true);
    assertEquals(FireAlarmZones.ALL, external.getAlarmZones());

    TileEntityFireAlarmControlPanel zoned = new TileEntityFireAlarmControlPanel();
    zoned.activateAlarmFrom(A, "csm:test", 2, false);
    zoned.setAlarmState(true);
    assertEquals(FireAlarmZones.ALL, zoned.getAlarmZones(), "an external alarm widens to all");
  }

  @Test
  void resetClearsEveryZone() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.activateAlarmFrom(A, "csm:test", 2, true);
    panel.setAlarmState(false);
    assertEquals(0, panel.getAlarmZones());
    assertEquals(0, panel.getAlarmOriginZone());
    assertFalse(panel.getCarbonMonoxideOnly());
    NBTTagCompound out = panel.writeNBT(new NBTTagCompound());
    assertFalse(out.hasKey("aZn"));
    panel.activateAlarmFrom(B, "csm:test", 7, false);
    assertEquals(1 << 7, panel.getAlarmZones(), "the next alarm starts from its own zone");
  }

  @Test
  void zonedAlarmSurvivesASave() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.activateAlarmFrom(A, "csm:test", 4, false);
    TileEntityFireAlarmControlPanel copy = new TileEntityFireAlarmControlPanel();
    copy.readNBT(panel.writeNBT(new NBTTagCompound()));
    assertEquals(1 << 4, copy.getAlarmZones());
    assertEquals(4, copy.getAlarmOriginZone());
  }

  @Test
  void carbonMonoxideOnlyUntilSmokeReports() {
    TileEntityFireAlarmControlPanel panel = new TileEntityFireAlarmControlPanel();
    panel.activateAlarmFrom(A, "csm:adt_co_detector", 0, true);
    assertTrue(panel.getCarbonMonoxideOnly());
    panel.activateAlarmFrom(B, "csm:adt_co_detector", 0, true);
    assertTrue(panel.getCarbonMonoxideOnly());
    panel.activateAlarmFrom(C, "csm:kiddesmoke", 0, false);
    assertFalse(panel.getCarbonMonoxideOnly(), "smoke outranks CO");

    TileEntityFireAlarmControlPanel drill = new TileEntityFireAlarmControlPanel();
    drill.startDrill();
    drill.activateAlarmFrom(A, "csm:adt_co_detector", 0, true);
    assertFalse(drill.getCarbonMonoxideOnly(), "a drill is a fire alarm");
  }

  @Test
  void deviceZoneAndInterconnectRoundTripAndDefault() {
    TileEntityFireAlarmSensor old = new TileEntityFireAlarmSensor();
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setIntArray("lp", new int[] {1, 2, 3});
    old.readNBT(legacy);
    assertEquals(0, old.getZone(), "a device linked before zones is general");
    assertNull(old.getInterconnectGroup());
    NBTTagCompound out = old.writeNBT(new NBTTagCompound());
    assertFalse(out.hasKey("zn"));
    assertFalse(out.hasKey("ig"));

    TileEntityFireAlarmSensor te = new TileEntityFireAlarmSensor();
    te.setZone(6);
    te.setInterconnectGroup(A);
    TileEntityFireAlarmSensor copy = new TileEntityFireAlarmSensor();
    copy.readNBT(te.writeNBT(new NBTTagCompound()));
    assertEquals(6, copy.getZone());
    assertEquals(A, copy.getInterconnectGroup());
  }
}
