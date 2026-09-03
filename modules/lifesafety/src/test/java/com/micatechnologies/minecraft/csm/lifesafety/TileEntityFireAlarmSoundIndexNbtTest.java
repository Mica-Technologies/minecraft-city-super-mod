package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityFireAlarmSoundIndexNbtTest {

  @Test
  void legacyKeyIsMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("soundIndex", 7);

    TileEntityFireAlarmSoundIndex te = new TileEntityFireAlarmSoundIndex();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertTrue(output.hasKey("sIx"));
    assertFalse(output.hasKey("soundIndex"));
    assertEquals(7, output.getInteger("sIx"));
  }

  @Test
  void shortKeyRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("sIx", 3);

    TileEntityFireAlarmSoundIndex te = new TileEntityFireAlarmSoundIndex();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());
    assertEquals(3, output.getInteger("sIx"));
  }

  @Test
  void emptyCompoundDefaultsToZero() {
    TileEntityFireAlarmSoundIndex te = new TileEntityFireAlarmSoundIndex();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());
    assertEquals(0, output.getInteger("sIx"));
  }
}
