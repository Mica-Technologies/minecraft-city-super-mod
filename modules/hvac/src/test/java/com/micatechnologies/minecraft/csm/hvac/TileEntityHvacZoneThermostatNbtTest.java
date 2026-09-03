package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.jupiter.api.Test;

class TileEntityHvacZoneThermostatNbtTest {

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

  private static NBTTagCompound buildPosTag(int x, int y, int z) {
    NBTTagCompound tag = new NBTTagCompound();
    tag.setInteger("x", x);
    tag.setInteger("y", y);
    tag.setInteger("z", z);
    return tag;
  }

  @Test
  void legacyKeysAreMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("targetTempLow", 66);
    legacy.setInteger("targetTempHigh", 74);
    legacy.setBoolean("isCalling", true);
    legacy.setInteger("callingMode", 2);
    legacy.setInteger("efficiency", 50);
    legacy.setLong("rampTicks", 200L);
    legacy.setFloat("currentTemp", 70.0f);
    legacy.setBoolean("hasPrimary", true);
    legacy.setTag("linkedPrimary", buildPosTag(100, 64, -200));
    legacy.setTag("linkedVents", buildPosList(new int[]{10, 20, 30}));

    TileEntityHvacZoneThermostat te = new TileEntityHvacZoneThermostat();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present
    assertEquals(66, output.getInteger("tLo"));
    assertEquals(74, output.getInteger("tHi"));
    assertTrue(output.getBoolean("cL"));
    assertEquals(2, output.getInteger("cM"));
    assertEquals(50, output.getInteger("eff"));
    assertEquals(200L, output.getLong("rT"));
    assertEquals(70.0f, output.getFloat("cT"), 0.01f);
    assertTrue(output.getBoolean("hP"));

    NBTTagCompound primary = output.getCompoundTag("lP");
    assertEquals(100, primary.getInteger("x"));
    assertEquals(64, primary.getInteger("y"));
    assertEquals(-200, primary.getInteger("z"));

    NBTTagList vents = output.getTagList("lV", Constants.NBT.TAG_COMPOUND);
    assertEquals(1, vents.tagCount());

    // Legacy keys absent
    assertFalse(output.hasKey("targetTempLow"));
    assertFalse(output.hasKey("targetTempHigh"));
    assertFalse(output.hasKey("isCalling"));
    assertFalse(output.hasKey("callingMode"));
    assertFalse(output.hasKey("efficiency"));
    assertFalse(output.hasKey("rampTicks"));
    assertFalse(output.hasKey("currentTemp"));
    assertFalse(output.hasKey("hasPrimary"));
    assertFalse(output.hasKey("linkedPrimary"));
    assertFalse(output.hasKey("linkedVents"));
  }

  @Test
  void shortKeyRoundTripWithNoPrimary() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("tLo", 72);
    input.setInteger("tHi", 82);
    input.setBoolean("cL", false);
    input.setInteger("cM", 0);
    input.setInteger("eff", 0);
    input.setLong("rT", 0L);
    input.setFloat("cT", 68.0f);
    input.setBoolean("hP", false);
    input.setTag("lV", buildPosList());

    TileEntityHvacZoneThermostat te = new TileEntityHvacZoneThermostat();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(72, output.getInteger("tLo"));
    assertEquals(82, output.getInteger("tHi"));
    assertFalse(output.getBoolean("hP"));
  }

  @Test
  void emptyCompoundDefaultsToStandardRange() {
    TileEntityHvacZoneThermostat te = new TileEntityHvacZoneThermostat();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(65, output.getInteger("tLo"));
    assertEquals(80, output.getInteger("tHi"));
  }
}
