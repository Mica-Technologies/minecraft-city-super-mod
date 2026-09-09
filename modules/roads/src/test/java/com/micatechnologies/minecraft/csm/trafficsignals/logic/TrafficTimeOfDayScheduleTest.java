package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TrafficTimeOfDayScheduleTest {

  @Test
  void everyHourOfTheClockResolvesToTheSlotItFallsIn() {
    // The defaults are 6 / 9 / 15 / 19, so the night slot is the one that wraps past midnight
    // and covers the small hours. Walking all 24 is the only way to be sure there is no gap.
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    for (int hour = 6; hour < 9; hour++) {
      assertEquals(TrafficTimeOfDaySchedule.SLOT_AM_PEAK, schedule.getActiveSlot(hour),
          "at " + hour);
    }
    for (int hour = 9; hour < 15; hour++) {
      assertEquals(TrafficTimeOfDaySchedule.SLOT_MIDDAY, schedule.getActiveSlot(hour),
          "at " + hour);
    }
    for (int hour = 15; hour < 19; hour++) {
      assertEquals(TrafficTimeOfDaySchedule.SLOT_PM_PEAK, schedule.getActiveSlot(hour),
          "at " + hour);
    }
    for (int hour = 19; hour < 24; hour++) {
      assertEquals(TrafficTimeOfDaySchedule.SLOT_NIGHT, schedule.getActiveSlot(hour),
          "at " + hour);
    }
    for (int hour = 0; hour < 6; hour++) {
      assertEquals(TrafficTimeOfDaySchedule.SLOT_NIGHT, schedule.getActiveSlot(hour),
          "at " + hour + ", which is the night slot wrapping past midnight");
    }
  }

  @Test
  void theSlotsPartitionTheClockWithNoHourUnclaimed() {
    // The point of start hours rather than windows: a gap cannot be expressed. Whatever the
    // hours are set to, every hour of the day belongs to exactly one slot.
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK, 5);
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_MIDDAY, 10);
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_PM_PEAK, 16);
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_NIGHT, 22);

    int[] hits = new int[TrafficTimeOfDaySchedule.SLOT_COUNT];
    for (int hour = 0; hour < 24; hour++) {
      hits[schedule.getActiveSlot(hour)]++;
    }
    assertEquals(5, hits[TrafficTimeOfDaySchedule.SLOT_AM_PEAK], "05:00-09:59");
    assertEquals(6, hits[TrafficTimeOfDaySchedule.SLOT_MIDDAY], "10:00-15:59");
    assertEquals(6, hits[TrafficTimeOfDaySchedule.SLOT_PM_PEAK], "16:00-21:59");
    assertEquals(7, hits[TrafficTimeOfDaySchedule.SLOT_NIGHT], "22:00-04:59");
  }

  @Test
  void slotsSharingAStartHourResolveDeterministically() {
    // A degenerate schedule must still answer, and answer the same way every tick -- a
    // controller that flickered between two patterns on the same hour would be far worse than
    // one that simply ran the first.
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      schedule.setStartHour(slot, 8);
    }
    for (int hour = 0; hour < 24; hour++) {
      assertEquals(TrafficTimeOfDaySchedule.SLOT_AM_PEAK, schedule.getActiveSlot(hour),
          "at " + hour);
    }
  }

  @Test
  void startHoursWrapIntoRangeBecauseTheGuiStepsThem() {
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_MIDDAY, -1);
    assertEquals(23, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_MIDDAY));
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_MIDDAY, 24);
    assertEquals(0, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_MIDDAY));
  }

  @Test
  void anOutOfRangeSlotIsClampedRatherThanThrowing() {
    // Slot indices reach this from NBT and from packets, so a bad one must not crash a chunk
    // load or hand a crafted packet an exception.
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    schedule.setStartHour(-5, 3);
    assertEquals(3, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
    schedule.setStartHour(99, 21);
    assertEquals(21, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_NIGHT));
  }

  @Test
  void nbtRoundTripsAndAnAbsentTagKeepsTheDefaults() {
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK, 4);
    schedule.setStartHour(TrafficTimeOfDaySchedule.SLOT_NIGHT, 23);

    TrafficTimeOfDaySchedule restored = new TrafficTimeOfDaySchedule();
    restored.readNBT(schedule.writeNBT(new NBTTagCompound()));
    assertEquals(4, restored.getStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
    assertEquals(23, restored.getStartHour(TrafficTimeOfDaySchedule.SLOT_NIGHT));

    // A controller saved before this existed has no tag, and must come back on the defaults
    // rather than on four slots that all start at midnight.
    TrafficTimeOfDaySchedule fresh = new TrafficTimeOfDaySchedule();
    fresh.readNBT(new NBTTagCompound());
    assertEquals(6, fresh.getStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
    assertEquals(19, fresh.getStartHour(TrafficTimeOfDaySchedule.SLOT_NIGHT));
  }

  @Test
  void aShortStoredArrayKeepsDefaultsForTheSlotsItDoesNotCover() {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setIntArray("tods", new int[] {1, 2});
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    schedule.readNBT(nbt);
    assertEquals(1, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
    assertEquals(2, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_MIDDAY));
    assertEquals(15, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_PM_PEAK));
    assertEquals(19, schedule.getStartHour(TrafficTimeOfDaySchedule.SLOT_NIGHT));
  }

  @Test
  void windowArithmeticIsTheSchoolZoneBeaconsBehaviour() {
    // This moved here from the beacon, so its contract moves with it: inclusive start,
    // exclusive end, an end at or before the start wraps, and a zero-length window is off.
    assertFalse(TrafficTimeOfDaySchedule.inWindow(6, 7, 9));
    assertTrue(TrafficTimeOfDaySchedule.inWindow(7, 7, 9));
    assertFalse(TrafficTimeOfDaySchedule.inWindow(9, 7, 9));
    assertTrue(TrafficTimeOfDaySchedule.inWindow(23, 22, 2));
    assertTrue(TrafficTimeOfDaySchedule.inWindow(1, 22, 2));
    assertFalse(TrafficTimeOfDaySchedule.inWindow(2, 22, 2));
    for (int hour = 0; hour < 24; hour++) {
      assertFalse(TrafficTimeOfDaySchedule.inWindow(hour, 8, 8), "at " + hour);
    }
  }

  @Test
  void slotDescriptionReadsAsATimeOfDayTable() {
    TrafficTimeOfDaySchedule schedule = new TrafficTimeOfDaySchedule();
    assertEquals("AM Peak: 6 AM - 9 AM",
        schedule.describeSlot(TrafficTimeOfDaySchedule.SLOT_AM_PEAK));
    // The night slot's end is the AM peak's start, which is what makes the wrap visible.
    assertEquals("Night: 7 PM - 6 AM",
        schedule.describeSlot(TrafficTimeOfDaySchedule.SLOT_NIGHT));
    assertEquals("12 AM", TrafficTimeOfDaySchedule.formatHour(0));
    assertEquals("12 PM", TrafficTimeOfDaySchedule.formatHour(12));
  }
}
