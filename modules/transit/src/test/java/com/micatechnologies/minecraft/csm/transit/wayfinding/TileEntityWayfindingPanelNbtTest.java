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
}
