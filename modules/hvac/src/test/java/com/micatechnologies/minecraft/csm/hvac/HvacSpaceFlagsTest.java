package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * What a thermostat with no room says about it (#280). An anchor no attach has failed for yet
 * keeps the default status, and used to be reported as open to the outdoors along with the ones
 * that really are: a building whose rooms had simply not been scanned looked as if it leaked.
 */
class HvacSpaceFlagsTest {

  private static ThermalAnchor anchor(ThermalScanner.Status status) {
    ThermalAnchor a = new ThermalAnchor(new BlockPos(0, 64, 0), ThermalAnchor.THERMOSTAT);
    a.status = status;
    return a;
  }

  @Test
  void anAnchorNotYetLookedAtIsFindingItsRoomNotOpenToTheOutdoors() {
    ThermalAnchor a = new ThermalAnchor(new BlockPos(0, 64, 0), ThermalAnchor.THERMOSTAT);
    assertTrue(a.findingRoom());
    assertEquals(HvacStatus.FLAG_FINDING_ROOM, HvacSystemControl.spaceFlags(a));
  }

  @Test
  void eachFailedAttachHasItsOwnFlag() {
    assertEquals(HvacStatus.FLAG_NOT_ENCLOSED,
        HvacSystemControl.spaceFlags(anchor(ThermalScanner.Status.NOT_ENCLOSED)));
    assertEquals(HvacStatus.FLAG_TOO_LARGE,
        HvacSystemControl.spaceFlags(anchor(ThermalScanner.Status.TOO_LARGE)));
    assertEquals(HvacStatus.FLAG_WAITING_FOR_CHUNKS,
        HvacSystemControl.spaceFlags(anchor(ThermalScanner.Status.UNLOADED)));
  }

  @Test
  void noFlagForNoAnchor() {
    assertEquals(0, HvacSystemControl.spaceFlags(null));
  }

  @Test
  void theFlagsAreDistinctBits() {
    int[] flags = {HvacStatus.FLAG_NOT_ENCLOSED, HvacStatus.FLAG_TOO_LARGE,
        HvacStatus.FLAG_NO_VENTS, HvacStatus.FLAG_ROOM_NOT_SERVED, HvacStatus.FLAG_NO_PRIMARY,
        HvacStatus.FLAG_WAITING, HvacStatus.FLAG_NO_POWER, HvacStatus.FLAG_CAPACITY_LIMITED,
        HvacStatus.FLAG_UNITS_UNCONNECTED, HvacStatus.FLAG_WAITING_FOR_CHUNKS,
        HvacStatus.FLAG_SYSTEM_OFF, HvacStatus.FLAG_FINDING_ROOM};
    int seen = 0;
    for (int f : flags) {
      assertEquals(1, Integer.bitCount(f), "one bit each");
      assertEquals(0, seen & f, "no two flags share a bit");
      seen |= f;
    }
  }
}
