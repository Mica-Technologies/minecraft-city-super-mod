package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.jupiter.api.Test;

class TileEntityHvacThermostatNbtTest {

  private static NBTTagList buildPosList(int[]... positions) {
    NBTTagList list = new NBTTagList();
    for (int[] p : positions) {
      NBTTagCompound tag = new NBTTagCompound();
      tag.setInteger("x", p[0]);
      tag.setInteger("y", p[1]);
      tag.setInteger("z", p[2]);
      list.appendTag(tag);
    }
    return list;
  }

  @Test
  void legacyKeysAreMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("targetTempLow", 68);
    legacy.setInteger("targetTempHigh", 76);
    legacy.setBoolean("isCalling", true);
    legacy.setInteger("callingMode", 1);
    legacy.setInteger("efficiency", 85);
    legacy.setLong("rampTicks", 500L);
    legacy.setFloat("currentTemp", 72.5f);
    legacy.setTag("linkedUnits", buildPosList(new int[]{10, 20, 30}));
    legacy.setTag("linkedVents", buildPosList(new int[]{40, 50, 60}, new int[]{70, 80, 90}));
    legacy.setTag("linkedZones", buildPosList());

    TileEntityHvacThermostat te = new TileEntityHvacThermostat();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present with correct values
    assertEquals(68, output.getInteger("tLo"));
    assertEquals(76, output.getInteger("tHi"));
    assertTrue(output.getBoolean("cL"));
    assertEquals(1, output.getInteger("cM"));
    assertEquals(85, output.getInteger("eff"));
    assertEquals(500L, output.getLong("rT"));
    assertEquals(72.5f, output.getFloat("cT"), 0.01f);

    // Linked unit list preserved
    NBTTagList units = output.getTagList("lU", Constants.NBT.TAG_COMPOUND);
    assertEquals(1, units.tagCount());
    assertEquals(10, units.getCompoundTagAt(0).getInteger("x"));

    // Linked vent list preserved
    NBTTagList vents = output.getTagList("lV", Constants.NBT.TAG_COMPOUND);
    assertEquals(2, vents.tagCount());

    // Legacy keys absent
    assertFalse(output.hasKey("targetTempLow"));
    assertFalse(output.hasKey("targetTempHigh"));
    assertFalse(output.hasKey("isCalling"));
    assertFalse(output.hasKey("callingMode"));
    assertFalse(output.hasKey("efficiency"));
    assertFalse(output.hasKey("rampTicks"));
    assertFalse(output.hasKey("currentTemp"));
    assertFalse(output.hasKey("linkedUnits"));
    assertFalse(output.hasKey("linkedVents"));
    assertFalse(output.hasKey("linkedZones"));
  }

  @Test
  void shortKeyRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("tLo", 70);
    input.setInteger("tHi", 78);
    input.setBoolean("cL", false);
    input.setInteger("cM", 2);
    input.setInteger("eff", 92);
    input.setLong("rT", 1000L);
    input.setFloat("cT", 75.0f);
    input.setTag("lU", buildPosList(new int[]{1, 2, 3}, new int[]{4, 5, 6}));
    input.setTag("lV", buildPosList(new int[]{7, 8, 9}));
    input.setTag("lZ", buildPosList());

    TileEntityHvacThermostat te = new TileEntityHvacThermostat();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(70, output.getInteger("tLo"));
    assertEquals(78, output.getInteger("tHi"));
    assertFalse(output.getBoolean("cL"));
    assertEquals(2, output.getInteger("cM"));
    assertEquals(92, output.getInteger("eff"));
    assertEquals(1000L, output.getLong("rT"));
    assertEquals(75.0f, output.getFloat("cT"), 0.01f);

    NBTTagList units = output.getTagList("lU", Constants.NBT.TAG_COMPOUND);
    assertEquals(2, units.tagCount());
    assertEquals(4, units.getCompoundTagAt(1).getInteger("x"));
  }

  @Test
  void emptyCompoundDefaultsToStandardRange() {
    TileEntityHvacThermostat te = new TileEntityHvacThermostat();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(65, output.getInteger("tLo"), "Default low temp should be 65");
    assertEquals(80, output.getInteger("tHi"), "Default high temp should be 80");
    assertFalse(output.getBoolean("cL"));
  }
}
