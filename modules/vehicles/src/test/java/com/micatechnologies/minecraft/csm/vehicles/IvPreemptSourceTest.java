package com.micatechnologies.minecraft.csm.vehicles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which pack custom variables count as emergency lights, by the names the installed packs use.
 */
class IvPreemptSourceTest {

  @Test
  @DisplayName("the names packs give their emergency light and siren switches count")
  void emergencyNamesCount() {
    // Immersive Vehicles' own pack, UNU and DKZ
    assertTrue(IvPreemptSource.isEmergencyVariable("EMERLTS"));
    assertTrue(IvPreemptSource.isEmergencyVariable("siren"));
    // Craftspeed names its switches in words
    assertTrue(IvPreemptSource.isEmergencyVariable("Emergency Lights"));
    assertTrue(IvPreemptSource.isEmergencyVariable("Onboard Emergency Lights"));
    assertTrue(IvPreemptSource.isEmergencyVariable("City Siren"));
  }

  @Test
  @DisplayName("other switches do not")
  void otherNamesDoNot() {
    for (String name : new String[] {"AUXLTS", "Underglow", "Tow_Lights", "Fog Lights", "LOCK",
        "Funky_Mode", "Christmas Lights", ""}) {
      assertFalse(IvPreemptSource.isEmergencyVariable(name), name);
    }
    assertFalse(IvPreemptSource.isEmergencyVariable(null));
  }
}
