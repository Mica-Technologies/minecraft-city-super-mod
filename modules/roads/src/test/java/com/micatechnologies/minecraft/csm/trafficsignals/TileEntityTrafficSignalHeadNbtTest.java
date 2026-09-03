package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalFlashPattern;
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
    // The legacy boolean migrates to the flash-pattern ordinal (true -> B)
    assertEquals(1, output.getInteger("flsP"));
    assertFalse(output.getBoolean("hF"));
    assertEquals(2, output.getInteger("mT"));
    assertEquals(3, output.getInteger("mC"));
    assertTrue(output.getBoolean("agE"));
    assertEquals(12345L, output.getLong("agD"));
    assertArrayEquals(new int[]{0, 1, 2}, output.getIntArray("agS"));
    assertEquals(99999L, output.getLong("agSd"));

    // Section infos structure preserved. Legacy 10-slot arrays are rewritten with the
    // appended body-style slot (0 = STANDARD, the pre-body-style look).
    NBTTagCompound infos = output.getCompoundTag("sInfs");
    assertEquals(3, infos.getInteger("count"));
    assertArrayEquals(withStandardBodyStyle(sections[0]), infos.getIntArray("s_0"));
    assertArrayEquals(withStandardBodyStyle(sections[1]), infos.getIntArray("s_1"));
    assertArrayEquals(withStandardBodyStyle(sections[2]), infos.getIntArray("s_2"));

    // Legacy keys absent
    assertFalse(output.hasKey("sectionInfos"));
    assertFalse(output.hasKey("bodyTilt"));
    assertFalse(output.hasKey("alternateFlash"));
    assertFalse(output.hasKey("altF"));
    assertFalse(output.hasKey("horizontalFlip"));
    assertFalse(output.hasKey("mountType"));
    assertFalse(output.hasKey("mountColor"));
    assertFalse(output.hasKey("agingEnabled"));
    assertFalse(output.hasKey("lastAgingDay"));
    assertFalse(output.hasKey("bulbAgingStates"));
    assertFalse(output.hasKey("agingSeed"));
  }

  /** The expected rewrite of a legacy 10-slot section array: body-style slot appended as 0. */
  private static int[] withStandardBodyStyle(int[] legacy) {
    // Legacy 10-slot arrays are rewritten with the appended body-style slot (0 = STANDARD)
    // and the appended bimodal slot (0 = not bimodal).
    int[] out = new int[legacy.length + 2];
    System.arraycopy(legacy, 0, out, 0, legacy.length);
    return out;
  }

  @Test
  void shortKeyRoundTrip() {
    int[][] sections = {{0, 0, 0, 0, 0, 6, 0, 0, 0, 0}};
    NBTTagCompound input = new NBTTagCompound();
    input.setTag("sInfs", buildSectionInfos(1, sections));
    input.setInteger("tlt", 0);
    input.setInteger("flsP", 2);
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

    assertEquals(2, output.getInteger("flsP"));
    assertTrue(output.getBoolean("hF"));
    assertEquals(1, output.getInteger("mT"));
    assertEquals(42L, output.getLong("agSd"));

    NBTTagCompound infos = output.getCompoundTag("sInfs");
    assertEquals(1, infos.getInteger("count"));
  }

  @Test
  void legacyAlternateFlashBooleanMigratesToPattern() {
    int[][] sections = {{0, 0, 0, 0, 0, 6, 0, 0, 0, 0}};

    // The short-form boolean from before the pattern existed: true meant the wig-wag
    // counterpart, which is pattern B.
    NBTTagCompound input = new NBTTagCompound();
    input.setTag("sInfs", buildSectionInfos(1, sections));
    input.setBoolean("altF", true);
    TileEntityTrafficSignalHead te = new TileEntityTrafficSignalHead();
    te.readNBT(input);
    assertEquals(TrafficSignalFlashPattern.B, te.getFlashPattern());
    assertEquals(1, te.writeNBT(new NBTTagCompound()).getInteger("flsP"));

    // false meant the normal flash, which is OFF
    input.setBoolean("altF", false);
    te = new TileEntityTrafficSignalHead();
    te.readNBT(input);
    assertEquals(TrafficSignalFlashPattern.OFF, te.getFlashPattern());

    // Absent entirely -> OFF, the default
    NBTTagCompound bare = new NBTTagCompound();
    bare.setTag("sInfs", buildSectionInfos(1, sections));
    te = new TileEntityTrafficSignalHead();
    te.readNBT(bare);
    assertEquals(TrafficSignalFlashPattern.OFF, te.getFlashPattern());

    // The new key wins over a stale legacy boolean, and an out-of-range ordinal from a
    // future version falls back to OFF rather than throwing.
    NBTTagCompound both = new NBTTagCompound();
    both.setTag("sInfs", buildSectionInfos(1, sections));
    both.setInteger("flsP", 2);
    both.setBoolean("altF", false);
    te = new TileEntityTrafficSignalHead();
    te.readNBT(both);
    assertEquals(TrafficSignalFlashPattern.C, te.getFlashPattern());
    assertEquals(TrafficSignalFlashPattern.OFF, TrafficSignalFlashPattern.fromNBT(99));
  }

  @Test
  void fyaActiveRoundTripsAndDefaultsToFalse() {
    int[][] sections = {{0, 0, 0, 0, 0, 6, 0, 0, 0, 0}};

    // Absent key -> not active, and not written back (keeps NBT small)
    NBTTagCompound input = new NBTTagCompound();
    input.setTag("sInfs", buildSectionInfos(1, sections));
    TileEntityTrafficSignalHead te = new TileEntityTrafficSignalHead();
    te.readNBT(input);
    assertFalse(te.isFyaActive());
    assertFalse(te.writeNBT(new NBTTagCompound()).hasKey("fya"));

    // Present and true -> active, written back
    input.setBoolean("fya", true);
    te = new TileEntityTrafficSignalHead();
    te.readNBT(input);
    assertTrue(te.isFyaActive());
    assertTrue(te.writeNBT(new NBTTagCompound()).getBoolean("fya"));

    // Setter reports change only when the value flips (world is null in tests, no sync)
    assertFalse(te.setFyaActive(true));
    assertTrue(te.setFyaActive(false));
    assertFalse(te.isFyaActive());
  }
}
