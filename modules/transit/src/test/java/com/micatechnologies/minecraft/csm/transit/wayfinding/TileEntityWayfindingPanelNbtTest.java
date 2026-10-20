package com.micatechnologies.minecraft.csm.transit.wayfinding;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * The large hanging sign's NBT. A panel with no text scale must read and write exactly what it
 * did before the text scale existed: the same keys, the same values, the same bytes.
 */
class TileEntityWayfindingPanelNbtTest {

  /** A sign as a script writes it, with no text scale. */
  private static NBTTagCompound sample() {
    NBTTagCompound c = new NBTTagCompound();
    c.setString("l1", "BAGGAGE");
    c.setString("l2", "CLAIM");
    c.setString("p", "baggage");
    c.setString("a", "down_left");
    c.setString("s", "metro");
    c.setBoolean("d", false);
    return c;
  }

  private static byte[] bytes(NBTTagCompound c) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CompressedStreamTools.write(c, new DataOutputStream(out));
    return out.toByteArray();
  }

  private static NBTTagCompound roundTrip(NBTTagCompound in) {
    TileEntityWayfindingPanel te = new TileEntityWayfindingPanel();
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
    NBTTagCompound out = new TileEntityWayfindingPanel().writeNBT(new NBTTagCompound());
    NBTTagCompound expected = new NBTTagCompound();
    expected.setString("l1", "GATES");
    expected.setString("p", "depart");
    expected.setString("a", "right");
    expected.setString("s", "airport");
    expected.setBoolean("d", true);
    assertEquals(expected, out);
    assertArrayEquals(bytes(expected), bytes(out));
  }

  @Test
  void oneLineSignLeavesTheEmptyLineOut() {
    NBTTagCompound in = sample();
    in.removeTag("l2");
    NBTTagCompound out = roundTrip(in);
    assertFalse(out.hasKey("l2"));
    assertEquals(in, out);
  }

  @Test
  void compoundWithoutPictogramLeavesTheSignAlone() {
    TileEntityWayfindingPanel te = new TileEntityWayfindingPanel();
    te.readNBT(sample());
    NBTTagCompound partial = new NBTTagCompound();
    partial.setString("l1", "IGNORED");
    te.readNBT(partial);
    assertEquals("BAGGAGE", te.getLine1());
    assertEquals(sample(), te.writeNBT(new NBTTagCompound()));
  }

  // --- the text scale ------------------------------------------------------------------------

  @Test
  void textScaleRoundTrips() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.8f);
    in.setFloat("ts1", 0.9f);
    TileEntityWayfindingPanel te = new TileEntityWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.8f, te.getTextScale());
    assertEquals(0.9f, te.getLine1Scale());
    assertEquals(1f, te.getLine2Scale());
    NBTTagCompound out = te.writeNBT(new NBTTagCompound());
    assertEquals(in, out);
    assertFalse(out.hasKey("ts2"));
    assertEquals(0.8f * 0.9f, te.getLineTextScale(1));
    assertEquals(0.8f, te.getLineTextScale(2));
  }

  @Test
  void textScaleIsClamped() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.2f);
    in.setFloat("ts1", 3f);
    in.setFloat("ts2", Float.NaN);
    TileEntityWayfindingPanel te = new TileEntityWayfindingPanel();
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
    TileEntityWayfindingPanel te = new TileEntityWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.6f, te.getLineTextScale(1));
    assertEquals(0.5f, te.getLineTextScale(2));
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
    TileEntityWayfindingPanel te = new TileEntityWayfindingPanel();
    te.readNBT(in);
    assertEquals(0.75f, te.getTextScale());
    assertEquals(0.5f, te.getLine2Scale());
  }

  @Test
  void scaledSignIsCopiedWhole() {
    NBTTagCompound in = sample();
    in.setFloat("ts", 0.85f);
    in.setFloat("ts2", 0.7f);
    TileEntityWayfindingPanel a = new TileEntityWayfindingPanel();
    a.readNBT(in);
    TileEntityWayfindingPanel b = new TileEntityWayfindingPanel();
    b.copySign(a);
    assertEquals(a.writeNBT(new NBTTagCompound()), b.writeNBT(new NBTTagCompound()));
  }
}
