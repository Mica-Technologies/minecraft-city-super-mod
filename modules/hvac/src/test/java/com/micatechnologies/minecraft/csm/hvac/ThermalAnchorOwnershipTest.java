package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Who may take a device's anchor away (#353). A chunk unloaded and loaded again within one tick
 * has its new tile entity register before the old one's deferred {@code onChunkUnload} runs; the
 * old instance, unregistering by position alone, used to remove the new one's anchor, and the
 * thermostat or unit was never stepped again until something rewrote it.
 */
class ThermalAnchorOwnershipTest {

  private static final BlockPos POS = new BlockPos(309, 227, -653);

  private final HvacThermalWorld w = new HvacThermalWorld(null);

  @Test
  void aStaleInstanceLeavesTheLiveOnesAnchor() {
    Object oldTe = new Object();
    Object newTe = new Object();
    w.registerAnchor(POS, ThermalAnchor.THERMOSTAT, Float.NaN, oldTe);
    w.registerAnchor(POS, ThermalAnchor.THERMOSTAT, Float.NaN, newTe);

    assertFalse(w.ownsAnchor(POS, oldTe));
    w.unregisterAnchor(POS, oldTe);

    assertNotNull(w.anchor(POS));
    assertTrue(w.ownsAnchor(POS, newTe));
  }

  @Test
  void theOwnerRemovesItsAnchor() {
    Object te = new Object();
    w.registerAnchor(POS, ThermalAnchor.UNIT, Float.NaN, te);
    w.unregisterAnchor(POS, te);
    assertNull(w.anchor(POS));
  }

  @Test
  void anAnchorRegisteredByPositionAloneGoesWithAnyInstance() {
    w.registerAnchor(POS, ThermalAnchor.VENT, Float.NaN);
    w.unregisterAnchor(POS, new Object());
    assertNull(w.anchor(POS));
  }

  @Test
  void aDifferentKindAtThePositionReplacesTheAnchor() {
    Object vent = new Object();
    Object tstat = new Object();
    w.registerAnchor(POS, ThermalAnchor.VENT, Float.NaN, vent);
    w.registerAnchor(POS, ThermalAnchor.THERMOSTAT, Float.NaN, tstat);
    w.unregisterAnchor(POS, vent);
    assertNotNull(w.anchor(POS));
    w.unregisterAnchor(POS, tstat);
    assertNull(w.anchor(POS));
  }
}
