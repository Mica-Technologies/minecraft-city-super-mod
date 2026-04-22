package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalAPSSoundSchemes;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityTrafficSignalAPSNbtTest {

  private static TileEntityTrafficSignalAPS createTestAPS() {
    return new TileEntityTrafficSignalAPS(TrafficSignalAPSSoundSchemes.CAMPBELL);
  }

  @Test
  void legacyKeysAreMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("CrosswalkSoundIndex", 2);
    legacy.setLong("CrosswalkSoundLastPlayTime", 5000L);
    legacy.setLong("CrosswalkLastPressTime", 3000L);
    legacy.setInteger("CrosswalkArrowOrientation", 1);

    TileEntityTrafficSignalAPS te = createTestAPS();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(2, output.getInteger("cwSi"));
    assertEquals(5000L, output.getLong("cwSL"));
    assertEquals(3000L, output.getLong("cwPr"));
    assertEquals(1, output.getInteger("cwAo"));

    assertFalse(output.hasKey("CrosswalkSoundIndex"));
    assertFalse(output.hasKey("CrosswalkSoundLastPlayTime"));
    assertFalse(output.hasKey("CrosswalkLastPressTime"));
    assertFalse(output.hasKey("CrosswalkArrowOrientation"));
  }

  @Test
  void shortKeyRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("cwSi", 1);
    input.setLong("cwSL", 10000L);
    input.setLong("cwPr", 8000L);
    input.setInteger("cwAo", 3);

    TileEntityTrafficSignalAPS te = createTestAPS();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(1, output.getInteger("cwSi"));
    assertEquals(10000L, output.getLong("cwSL"));
    assertEquals(8000L, output.getLong("cwPr"));
    assertEquals(3, output.getInteger("cwAo"));
  }

  @Test
  void outOfRangeSoundIndexClampedToMax() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("cwSi", 99);

    TileEntityTrafficSignalAPS te = createTestAPS();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertTrue(output.getInteger("cwSi") < TrafficSignalAPSSoundSchemes.CAMPBELL.length,
        "Sound index should be clamped to soundSchemes.length");
  }
}
