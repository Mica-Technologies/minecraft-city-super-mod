package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityTrafficSignalHeadNbtTest {

  private static NBTTagCompound buildSectionInfos(int count, int[][] sectionData) {
    NBTTagCompound infos = new NBTTagCompound();
    infos.setInteger("count", count);
    for (int i = 0; i < count; i++) {
      infos.setIntArray("s_" + i, sectionData[i]);
    }
    return infos;
  }

  @Test
  void legacyKeysAreMigrated() {
    int[][] sections = {
        {1, 0, 0, 0, 0, 6, 0, 0, 0, 0},
        {2, 1, 0, 0, 0, 6, 0, 0, 0, 0},
        {0, 2, 0, 0, 0, 6, 0, 0, 0, 0}
    };
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setTag("sectionInfos", buildSectionInfos(3, sections));
    legacy.setInteger("bodyTilt", 1);
    legacy.setBoolean("alternateFlash", true);
    legacy.setBoolean("horizontalFlip", false);
    legacy.setInteger("mountType", 2);
    legacy.setInteger("mountColor", 3);
    legacy.setBoolean("agingEnabled", true);
    legacy.setLong("lastAgingDay", 12345L);
    legacy.setIntArray("bulbAgingStates", new int[]{0, 1, 2});
    legacy.setLong("agingSeed", 99999L);

    TileEntityTrafficSignalHead te = new TileEntityTrafficSignalHead();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present
    assertTrue(output.hasKey("sInfs"));
    assertEquals(1, output.getInteger("tlt"));
    assertTrue(output.getBoolean("altF"));
    assertFalse(output.getBoolean("hF"));
    assertEquals(2, output.getInteger("mT"));
    assertEquals(3, output.getInteger("mC"));
    assertTrue(output.getBoolean("agE"));
    assertEquals(12345L, output.getLong("agD"));
    assertArrayEquals(new int[]{0, 1, 2}, output.getIntArray("agS"));
    assertEquals(99999L, output.getLong("agSd"));

    // Section infos structure preserved
    NBTTagCompound infos = output.getCompoundTag("sInfs");
    assertEquals(3, infos.getInteger("count"));
    assertArrayEquals(sections[0], infos.getIntArray("s_0"));
    assertArrayEquals(sections[1], infos.getIntArray("s_1"));
    assertArrayEquals(sections[2], infos.getIntArray("s_2"));

    // Legacy keys absent
    assertFalse(output.hasKey("sectionInfos"));
    assertFalse(output.hasKey("bodyTilt"));
    assertFalse(output.hasKey("alternateFlash"));
    assertFalse(output.hasKey("horizontalFlip"));
    assertFalse(output.hasKey("mountType"));
    assertFalse(output.hasKey("mountColor"));
    assertFalse(output.hasKey("agingEnabled"));
    assertFalse(output.hasKey("lastAgingDay"));
    assertFalse(output.hasKey("bulbAgingStates"));
    assertFalse(output.hasKey("agingSeed"));
  }

  @Test
  void shortKeyRoundTrip() {
    int[][] sections = {{0, 0, 0, 0, 0, 6, 0, 0, 0, 0}};
    NBTTagCompound input = new NBTTagCompound();
    input.setTag("sInfs", buildSectionInfos(1, sections));
    input.setInteger("tlt", 0);
    input.setBoolean("altF", false);
    input.setBoolean("hF", true);
    input.setInteger("mT", 1);
    input.setInteger("mC", 0);
    input.setBoolean("agE", false);
    input.setLong("agD", 0L);
    input.setIntArray("agS", new int[]{0});
    input.setLong("agSd", 42L);

    TileEntityTrafficSignalHead te = new TileEntityTrafficSignalHead();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertTrue(output.getBoolean("hF"));
    assertEquals(1, output.getInteger("mT"));
    assertEquals(42L, output.getLong("agSd"));

    NBTTagCompound infos = output.getCompoundTag("sInfs");
    assertEquals(1, infos.getInteger("count"));
  }
}
