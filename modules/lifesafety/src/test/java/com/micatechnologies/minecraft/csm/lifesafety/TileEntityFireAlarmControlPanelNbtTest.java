package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityFireAlarmControlPanelNbtTest {

  @Test
  void legacyKeysAreMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("soundIndex", 5);
    legacy.setBoolean("alarm", true);
    legacy.setBoolean("alarmStorm", false);
    legacy.setBoolean("alarmAnnounced", true);
    legacy.setBoolean("audibleSilence", true);
    legacy.setBoolean("glitchy", false);
    legacy.setString("connectedAppliances", "10 20 30\n40 50 60\n");

    TileEntityFireAlarmControlPanel te = new TileEntityFireAlarmControlPanel();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present
    assertEquals(5, output.getInteger("sIx"));
    assertTrue(output.getBoolean("a"));
    assertFalse(output.getBoolean("aSm"));
    assertTrue(output.getBoolean("aAn"));
    assertTrue(output.getBoolean("aSi"));
    assertFalse(output.getBoolean("gl"));
    assertTrue(output.hasKey("apps"));

    // Legacy keys absent
    assertFalse(output.hasKey("soundIndex"));
    assertFalse(output.hasKey("alarm"));
    assertFalse(output.hasKey("alarmStorm"));
    assertFalse(output.hasKey("alarmAnnounced"));
    assertFalse(output.hasKey("audibleSilence"));
    assertFalse(output.hasKey("glitchy"));
    assertFalse(output.hasKey("connectedAppliances"));
  }

  @Test
  void shortKeyRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("sIx", 11);
    input.setBoolean("a", true);
    input.setBoolean("aSm", true);
    input.setBoolean("aAn", false);
    input.setBoolean("aSi", false);
    input.setBoolean("gl", true);
    input.setString("apps", "1 2 3\n4 5 6\n7 8 9\n");

    TileEntityFireAlarmControlPanel te = new TileEntityFireAlarmControlPanel();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(11, output.getInteger("sIx"));
    assertTrue(output.getBoolean("a"));
    assertTrue(output.getBoolean("aSm"));
    assertFalse(output.getBoolean("aAn"));
    assertFalse(output.getBoolean("aSi"));
    assertTrue(output.getBoolean("gl"));

    String apps = output.getString("apps");
    assertTrue(apps.contains("1 2 3"));
    assertTrue(apps.contains("4 5 6"));
    assertTrue(apps.contains("7 8 9"));
  }

  @Test
  void emptyCompoundProducesDefaults() {
    TileEntityFireAlarmControlPanel te = new TileEntityFireAlarmControlPanel();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(0, output.getInteger("sIx"));
    assertFalse(output.getBoolean("a"));
    assertFalse(output.getBoolean("aSm"));
    assertFalse(output.getBoolean("aAn"));
    assertFalse(output.getBoolean("aSi"));
    assertFalse(output.getBoolean("gl"));
  }
}
