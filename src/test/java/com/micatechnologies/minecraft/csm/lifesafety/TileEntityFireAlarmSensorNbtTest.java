package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityFireAlarmSensorNbtTest {

  @Test
  void legacyKeysAreMigratedToIntArray() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("lpX", 100);
    legacy.setInteger("lpY", 64);
    legacy.setInteger("lpZ", -200);

    TileEntityFireAlarmSensor te = new TileEntityFireAlarmSensor();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertTrue(output.hasKey("lp"));
    assertFalse(output.hasKey("lpX"));
    assertFalse(output.hasKey("lpY"));
    assertFalse(output.hasKey("lpZ"));

    int[] pos = output.getIntArray("lp");
    assertEquals(3, pos.length);
    assertEquals(100, pos[0]);
    assertEquals(64, pos[1]);
    assertEquals(-200, pos[2]);
  }

  @Test
  void shortKeyRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setIntArray("lp", new int[]{50, 128, 300});

    TileEntityFireAlarmSensor te = new TileEntityFireAlarmSensor();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    int[] pos = output.getIntArray("lp");
    assertEquals(50, pos[0]);
    assertEquals(128, pos[1]);
    assertEquals(300, pos[2]);
  }

  @Test
  void emptyCompoundDefaultsToUnlinked() {
    TileEntityFireAlarmSensor te = new TileEntityFireAlarmSensor();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    int[] pos = output.getIntArray("lp");
    assertEquals(3, pos.length);
    assertEquals(-500, pos[1], "Unlinked sensor should default linkedPanelY to -500");
  }
}
