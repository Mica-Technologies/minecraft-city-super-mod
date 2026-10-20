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

  // --- the text scale ------------------------------------------------------------------------

  @Test
  void textScaleRoundTrips() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.8f);
    in.setFloat("ts1", 0.9f);
    TileEntityHospitalWayfindingPanel te = new TileEntityHospitalWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.8f, te.getTextScale());
    assertEquals(0.9f, te.getLine1Scale());
    assertEquals(1f, te.getLine2Scale());
    NBTTagCompound out = te.writeNBT(new NBTTagCompound());
    assertEquals(in, out);
    assertFalse(out.hasKey("ts2"));
    assertEquals(0.8f * 0.9f, te.face(null).scale1);
    assertEquals(0.8f, te.face(null).scale2);
  }

  @Test
  void textScaleIsClamped() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.2f);
    in.setFloat("ts1", 3f);
    in.setFloat("ts2", Float.NaN);
    TileEntityHospitalWayfindingPanel te = new TileEntityHospitalWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.5f, te.getTextScale());
    assertEquals(1f, te.getLine1Scale());
    assertEquals(1f, te.getLine2Scale());
    NBTTagCompound out = te.writeNBT(new NBTTagCompound());
    assertEquals(0.5f, out.getFloat("ts"));
    assertFalse(out.hasKey("ts1"), "a scale clamped to 1 is no scale");
    assertFalse(out.hasKey("ts2"), "a scale that is not a number is no scale");
  }

  @Test
  void lineScaleProductIsClampedToTheMinimum() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.6f);
    in.setFloat("ts2", 0.6f);
    TileEntityHospitalWayfindingPanel te = new TileEntityHospitalWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.6f, te.face(null).scale1);
    assertEquals(0.5f, te.face(null).scale2);
  }

  @Test
  void textScaleOfOneWritesNoKey() throws IOException {
    NBTTagCompound in = sample();
    in.setFloat("ts", 1f);
    in.setFloat("ts1", 1f);
    in.setFloat("ts2", 1f);
    NBTTagCompound out = roundTrip(in);
    assertArrayEquals(bytes(sample()), bytes(out));
  }

  @Test
  void textScaleReadsAnyNumberType() {
    NBTTagCompound in = sample();
    in.setDouble("ts", 0.75);
    in.setInteger("ts2", 0);
    TileEntityHospitalWayfindingPanel te = new TileEntityHospitalWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.75f, te.getTextScale());
    assertEquals(0.5f, te.getLine2Scale());
  }

  @Test
  void scaledSignIsCopiedWhole() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.85f);
    in.setFloat("ts2", 0.7f);
    TileEntityHospitalWayfindingPanel a = new TileEntityHospitalWayfindingPanel();
    a.readNBT(in);
    TileEntityHospitalWayfindingPanel b = new TileEntityHospitalWayfindingPanel();
    b.copySign(a);
    assertEquals(0.85f, b.getTextScale());
    assertEquals(1f, b.getLine1Scale());
    assertEquals(0.7f, b.getLine2Scale());
  }
  @Test
  void textScaleIsPartOfTheBakedModelKey() {
    TileEntityHospitalWayfindingPanel plain = new TileEntityHospitalWayfindingPanel();
    plain.readNBT(sample());
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.8f);
    TileEntityHospitalWayfindingPanel scaled = new TileEntityHospitalWayfindingPanel();
    scaled.readNBT(in);
    assertNotEquals(plain.getBakedModelKey(), scaled.getBakedModelKey());
    assertNotEquals(plain.face(null), scaled.face(null));
    assertEquals(0.8f, scaled.face(null).scale1);
    assertEquals(0.8f, scaled.face(null).scale2);
    in.setFloat("ts1", 0.9f);
    TileEntityHospitalWayfindingPanel line = new TileEntityHospitalWayfindingPanel();
    line.readNBT(in);
    assertNotEquals(scaled.getBakedModelKey(), line.getBakedModelKey());
    assertNotEquals(scaled.face(null), line.face(null));
  }

  @Test
  void copySignReportsAScaleChange() {
    TileEntityHospitalWayfindingPanel a = new TileEntityHospitalWayfindingPanel();
    a.readNBT(sample());
    TileEntityHospitalWayfindingPanel b = new TileEntityHospitalWayfindingPanel();
    b.readNBT(sample());
    assertFalse(b.copySign(a));
    NBTTagCompound in = sample();
    in.setFloat("ts2", 0.75f);
    a.readNBT(in);
    assertTrue(b.copySign(a));
    assertEquals(0.75f, b.getLine2Scale());
  }
}
