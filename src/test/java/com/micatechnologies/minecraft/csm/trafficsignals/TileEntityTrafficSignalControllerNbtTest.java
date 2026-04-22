package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerNBTKeys;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityTrafficSignalControllerNbtTest {

  @Test
  void legacyScalarKeysAreMigrated() {
    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setInteger("tcMode", 1);
    legacy.setInteger("tcOperatingMode", 2);
    legacy.setBoolean("tcPaused", true);
    legacy.setLong("tcLastPhaseChangeTime", 50000L);
    legacy.setLong("tcLastPhaseApplicabilityChangeTime", 40000L);
    legacy.setLong("tcLastPedPhaseTime", 30000L);
    legacy.setString("tcCurrentFaultMessage", "Test fault");
    legacy.setBoolean("tcNightlyFallbackToFlashMode", true);
    legacy.setBoolean("tcPowerLossFallbackToFlashMode", false);
    legacy.setBoolean("tcOverlapPedestrianSignals", true);
    legacy.setLong("tcYellowTime", 60L);
    legacy.setLong("tcFlashDontWalkTime", 100L);
    legacy.setLong("tcAllRedTime", 40L);
    legacy.setLong("tcMinRequestableServiceTime", 200L);
    legacy.setLong("tcMaxRequestableServiceTime", 600L);
    legacy.setLong("tcMinGreenTime", 100L);
    legacy.setLong("tcMaxGreenTime", 500L);
    legacy.setLong("tcMinGreenSecondaryTime", 80L);
    legacy.setLong("tcMaxGreenSecondaryTime", 400L);
    legacy.setLong("tcDedicatedPedSignalTime", 150L);
    legacy.setBoolean("tcUpgradedPreviousNbtFormat", true);
    legacy.setLong("tcLeadPedestrianIntervalTime", 120L);
    legacy.setBoolean("tcAllRedFlash", false);
    legacy.setInteger("tcRampMeterNightMode", 1);

    // Provide empty compound tags for structured data so readNBT doesn't NPE
    legacy.setTag("tcCircuits", new NBTTagCompound());
    legacy.setTag("tcOverlaps", new NBTTagCompound());
    legacy.setTag("tcCachedPhases", new NBTTagCompound());
    legacy.setTag("tcCurrentPhase", new NBTTagCompound());

    TileEntityTrafficSignalController te = new TileEntityTrafficSignalController();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present with correct values
    assertEquals(1, output.getInteger("tcMode"));
    assertEquals(2, output.getInteger("tcOm"));
    assertTrue(output.getBoolean("tcPs"));
    assertEquals(50000L, output.getLong("tcPcT"));
    assertEquals(40000L, output.getLong("tcPaT"));
    assertEquals(30000L, output.getLong("tcPdT"));
    assertEquals("Test fault", output.getString("tcFm"));
    assertTrue(output.getBoolean("tcNfm"));
    assertFalse(output.getBoolean("tcPfm"));
    assertTrue(output.getBoolean("tcOvp"));
    assertEquals(60L, output.getLong("tcYt"));
    assertEquals(100L, output.getLong("tcFdw"));
    assertEquals(40L, output.getLong("tcArt"));
    assertEquals(200L, output.getLong("tcMnR"));
    assertEquals(600L, output.getLong("tcMxR"));
    assertEquals(100L, output.getLong("tcMnG"));
    assertEquals(500L, output.getLong("tcMxG"));
    assertEquals(80L, output.getLong("tcMnGs"));
    assertEquals(400L, output.getLong("tcMxGs"));
    assertEquals(150L, output.getLong("tcDps"));
    assertTrue(output.getBoolean("tcUp"));
    assertEquals(120L, output.getLong("tcLpi"));
    assertFalse(output.getBoolean("tcArF"));
    assertEquals(1, output.getInteger("tcRmN"));

    // All legacy keys absent from output
    for (String legacyKey : TrafficSignalControllerNBTKeys.LEGACY_V2_KEY_LIST) {
      assertFalse(output.hasKey(legacyKey),
          "Legacy key should not be in output: " + legacyKey);
    }
  }

  @Test
  void shortKeyScalarRoundTrip() {
    NBTTagCompound input = new NBTTagCompound();
    input.setInteger("tcMode", 0);
    input.setInteger("tcOm", 1);
    input.setBoolean("tcPs", false);
    input.setLong("tcPcT", 12345L);
    input.setLong("tcPaT", 12000L);
    input.setLong("tcPdT", 11000L);
    input.setTag("tcCp", new NBTTagCompound());
    input.setString("tcFm", "");
    input.setBoolean("tcNfm", false);
    input.setBoolean("tcPfm", true);
    input.setBoolean("tcOvp", false);
    input.setLong("tcYt", 45L);
    input.setLong("tcFdw", 80L);
    input.setLong("tcArt", 30L);
    input.setLong("tcMnR", 150L);
    input.setLong("tcMxR", 450L);
    input.setLong("tcMnG", 75L);
    input.setLong("tcMxG", 350L);
    input.setLong("tcMnGs", 60L);
    input.setLong("tcMxGs", 300L);
    input.setLong("tcDps", 120L);
    input.setBoolean("tcUp", true);
    input.setLong("tcLpi", 90L);
    input.setBoolean("tcArF", true);
    input.setInteger("tcRmN", 0);
    input.setTag("tcCrc", new NBTTagCompound());
    input.setTag("tcOv", new NBTTagCompound());
    input.setTag("tcPh", new NBTTagCompound());

    TileEntityTrafficSignalController te = new TileEntityTrafficSignalController();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(0, output.getInteger("tcMode"));
    assertEquals(1, output.getInteger("tcOm"));
    assertFalse(output.getBoolean("tcPs"));
    assertEquals(12345L, output.getLong("tcPcT"));
    assertTrue(output.getBoolean("tcPfm"));
    assertEquals(45L, output.getLong("tcYt"));
    assertTrue(output.getBoolean("tcArF"));
  }
}
