package com.micatechnologies.minecraft.csm.technology;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityRedstoneTTSNbtTest {

  @Test
  void legacyKeysAreMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setString("ttsString", "Hello world");
    legacy.setDouble("ttsRadius", 24.0);

    TileEntityRedstoneTTS te = new TileEntityRedstoneTTS();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals("Hello world", output.getString("tts"));
    assertEquals(24.0, output.getDouble("ttR"), 0.01);

    assertFalse(output.hasKey("ttsString"));
    assertFalse(output.hasKey("ttsRadius"));
  }

  @Test
  void shortKeyRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setString("tts", "Test message");
    input.setDouble("ttR", 16.5);

    TileEntityRedstoneTTS te = new TileEntityRedstoneTTS();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals("Test message", output.getString("tts"));
    assertEquals(16.5, output.getDouble("ttR"), 0.01);
  }

  @Test
  void emptyCompoundProducesDefaults() {
    TileEntityRedstoneTTS te = new TileEntityRedstoneTTS();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertTrue(output.hasKey("tts"));
    assertTrue(output.hasKey("ttR"));
  }
}
