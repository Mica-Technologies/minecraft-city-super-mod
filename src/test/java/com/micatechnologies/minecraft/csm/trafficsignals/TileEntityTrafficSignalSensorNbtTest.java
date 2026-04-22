package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class TileEntityTrafficSignalSensorNbtTest {

  @Test
  void legacyKeysAreMigrated() {
    BlockPos corner1 = new BlockPos(10, 64, -20);
    BlockPos corner2 = new BlockPos(20, 70, -10);
    BlockPos left1 = new BlockPos(30, 64, -40);
    BlockPos left2 = new BlockPos(40, 70, -30);

    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setLong("blockPos1", corner1.toLong());
    legacy.setLong("blockPos2", corner2.toLong());
    legacy.setLong("leftBlockPos1", left1.toLong());
    legacy.setLong("leftBlockPos2", left2.toLong());
    legacy.setLong("protectedBlockPos1", corner1.toLong());
    legacy.setLong("protectedBlockPos2", corner2.toLong());
    legacy.setLong("rightBlockPos1", left1.toLong());
    legacy.setLong("rightBlockPos2", left2.toLong());

    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present
    assertEquals(corner1.toLong(), output.getLong("m1"));
    assertEquals(corner2.toLong(), output.getLong("m2"));
    assertEquals(left1.toLong(), output.getLong("l1"));
    assertEquals(left2.toLong(), output.getLong("l2"));
    assertEquals(corner1.toLong(), output.getLong("p1"));
    assertEquals(corner2.toLong(), output.getLong("p2"));
    assertEquals(left1.toLong(), output.getLong("r1"));
    assertEquals(left2.toLong(), output.getLong("r2"));

    // Legacy keys absent
    assertFalse(output.hasKey("blockPos1"));
    assertFalse(output.hasKey("blockPos2"));
    assertFalse(output.hasKey("leftBlockPos1"));
    assertFalse(output.hasKey("leftBlockPos2"));
    assertFalse(output.hasKey("protectedBlockPos1"));
    assertFalse(output.hasKey("protectedBlockPos2"));
    assertFalse(output.hasKey("rightBlockPos1"));
    assertFalse(output.hasKey("rightBlockPos2"));
  }

  @Test
  void shortKeyRoundTrip() {
    BlockPos pos = new BlockPos(100, 64, 200);

    NBTTagCompound input = new NBTTagCompound();
    input.setLong("m1", pos.toLong());
    input.setLong("m2", pos.toLong());

    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(pos.toLong(), output.getLong("m1"));
    assertEquals(pos.toLong(), output.getLong("m2"));
  }

  @Test
  void emptyCompoundProducesNullPositions() {
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertFalse(output.hasKey("m1"), "Null positions should not be written");
  }
}
