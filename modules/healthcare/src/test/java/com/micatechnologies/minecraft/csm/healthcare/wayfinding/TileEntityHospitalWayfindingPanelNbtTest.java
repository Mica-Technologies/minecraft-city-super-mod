package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * The hospital wayfinding panel's NBT, Transit's keys plus the server's place keys. A panel with
 * no text scale must read and write exactly what it did before the text scale existed.
 */
class TileEntityHospitalWayfindingPanelNbtTest {

  /** A sign as the hospital builder writes it, with its place, and no text scale. */
  private static NBTTagCompound sample() {
    NBTTagCompound c = new NBTTagCompound();
    c.setString("l1", "IMAGING");
    c.setString("l2", "X-RAY");
    c.setString("p", "xray");
    c.setString("a", "up_left");
    c.setString("s", "neutral");
    c.setBoolean("d", false);
    c.setByte("pc", (byte) 2);
    c.setByte("pr", (byte) 1);
    c.setByte("pw", (byte) 6);
    c.setByte("ph", (byte) 2);
    return c;
  }

  private static byte[] bytes(NBTTagCompound c) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CompressedStreamTools.write(c, new DataOutputStream(out));
    return out.toByteArray();
  }

  private static NBTTagCompound roundTrip(NBTTagCompound in) {
    TileEntityHospitalWayfindingPanel te = new TileEntityHospitalWayfindingPanel();
    te.readNBT(in);
    return te.writeNBT(new NBTTagCompound());
  }

  @Test
  void signWithoutScaleWritesBackByteIdentical() throws IOException {
    NBTTagCompound out = roundTrip(sample());
    assertEquals(sample(), out);
    assertEquals(sample().getKeySet(), out.getKeySet());
    assertArrayEquals(bytes(sample()), bytes(out));
  }

  @Test
  void defaultSignWritesOnlyTheOriginalKeys() throws IOException {
    NBTTagCompound out = new TileEntityHospitalWayfindingPanel().writeNBT(new NBTTagCompound());
    NBTTagCompound expected = new NBTTagCompound();
    expected.setString("l1", "INFORMATION");
    expected.setString("a", "right");
    expected.setString("s", "neutral");
    expected.setBoolean("d", true);
    expected.setByte("pc", (byte) 0);
    expected.setByte("pr", (byte) 0);
    expected.setByte("pw", (byte) 1);
    expected.setByte("ph", (byte) 1);
    assertEquals(expected, out);
    assertArrayEquals(bytes(expected), bytes(out));
  }

  @Test
  void compoundWithoutPictogramLeavesTheSignAlone() {
    TileEntityHospitalWayfindingPanel te = new TileEntityHospitalWayfindingPanel();
    te.readNBT(sample());
    NBTTagCompound partial = new NBTTagCompound();
    partial.setString("l1", "IGNORED");
    te.readNBT(partial);
    assertEquals("IMAGING", te.getLine1());
    assertEquals(sample(), te.writeNBT(new NBTTagCompound()));
  }

  @Test
  void bakedModelKeyIsUnchangedWithoutScale() {
    TileEntityHospitalWayfindingPanel a = new TileEntityHospitalWayfindingPanel();
    a.readNBT(sample());
    TileEntityHospitalWayfindingPanel b = new TileEntityHospitalWayfindingPanel();
    b.readNBT(sample());
    assertEquals(a.getBakedModelKey(), b.getBakedModelKey());
    assertEquals(a.face(null), b.face(null));
  }
}
