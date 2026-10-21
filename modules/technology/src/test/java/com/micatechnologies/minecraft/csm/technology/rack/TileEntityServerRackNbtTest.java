package com.micatechnologies.minecraft.csm.technology.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityServerRackNbtTest {

  private static TileEntityServerRack read(NBTTagCompound tag) {
    TileEntityServerRack te = new TileEntityServerRack();
    te.readNBT(tag);
    return te;
  }

  @Test
  void roundTrip() {
    TileEntityServerRack te = new TileEntityServerRack();
    RackContents c = RackContents.EMPTY.with(RackUnitType.SWITCH_24_PORT, 42)
        .with(RackUnitType.STORAGE_4U, 41);
    te.setContents(c);
    NBTTagCompound out = te.writeNBT(new NBTTagCompound());
    assertEquals(1, out.getKeySet().size(), "one short key");
    assertTrue(out.hasKey("u"));
    assertEquals(4, out.getByteArray("u").length);
    assertEquals(c, read(out).getContents());
  }

  @Test
  void anEmptyRackWritesNothing() {
    NBTTagCompound out = new TileEntityServerRack().writeNBT(new NBTTagCompound());
    assertFalse(out.hasKey("u"));
    assertTrue(read(out).getContents().isEmpty());
  }

  @Test
  void aMissingKeyReadsEmpty() {
    TileEntityServerRack te = read(new NBTTagCompound());
    assertTrue(te.getContents().isEmpty());
  }
}
