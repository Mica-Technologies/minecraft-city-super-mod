package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #319: a preempt detector's redstone call is the redstone actually reaching it, not a flag
 * kept from the last neighbour update; and a timed test call ({@code tst}) ends on its own.
 */
class TileEntityPreemptDetectorTest {

  private static TileEntityPreemptDetector detectorFrom(String key, boolean flag, int ticks) {
    NBTTagCompound nbt = new NBTTagCompound();
    if (key.equals("pw")) {
      nbt.setBoolean("pw", flag);
    } else {
      nbt.setInteger("tst", ticks);
    }
    TileEntityPreemptDetector te = new TileEntityPreemptDetector();
    te.readNBT(nbt);
    return te;
  }

  @Test
  @DisplayName("#319: a stored redstone call with no redstone behind it ends at the next read")
  void storedPowerWithoutRedstoneEnds() {
    // pw:1 written by /blockdata, or saved with the chunk, with nothing powering the detector
    // and no neighbour update to re-read it: the controller held the preempt dwell for good.
    TileEntityPreemptDetector te = detectorFrom("pw", true, 0);
    assertFalse(te.isCalledAt(100L, () -> false), "nothing powers the detector: no call");
    assertFalse(te.writeNBT(new NBTTagCompound()).getBoolean("pw"), "and it saves as uncalled");
  }

  @Test
  @DisplayName("#319: a redstone call stands while the redstone does")
  void redstoneCallStandsWhilePowered() {
    TileEntityPreemptDetector te = new TileEntityPreemptDetector();
    te.setPowered(true);
    assertTrue(te.isCalledAt(100L, () -> true));
    assertTrue(te.isCalledAt(200L, () -> true));
    assertFalse(te.isCalledAt(300L, () -> false), "the source went away without an update");
  }

  @Test
  @DisplayName("#319: a test call (tst) calls for its ticks and then ends, and survives a save")
  void testCallCountsDown() {
    TileEntityPreemptDetector te = detectorFrom("tst", false, 200);
    assertTrue(te.isCalledAt(1000L, () -> false), "the test call starts at the first read");
    assertTrue(te.isCalledAt(1199L, () -> false));
    NBTTagCompound saved = te.writeNBT(new NBTTagCompound());
    assertEquals(1, saved.getInteger("tst"), "what is left of it is saved");
    assertFalse(te.isCalledAt(1200L, () -> false), "200 ticks after it started it has ended");
    assertFalse(te.writeNBT(new NBTTagCompound()).hasKey("tst"), "an ended test call is not saved");
  }
}
