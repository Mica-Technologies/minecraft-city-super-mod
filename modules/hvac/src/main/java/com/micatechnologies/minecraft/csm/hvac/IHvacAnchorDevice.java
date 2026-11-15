package com.micatechnologies.minecraft.csm.hvac;

/**
 * An HVAC tile entity that is a simulation anchor (a thermostat, a unit or a linked vent), so the
 * chunk-load audit in {@link HvacThermalWorld} can make sure it is registered (#353).
 *
 * @version 1.0
 * @since 2026.11
 */
interface IHvacAnchorDevice {

  /**
   * Registers this device's anchor if it should have one, as its own: idempotent, and a device
   * that should not be an anchor (an unlinked vent) does nothing.
   */
  void ensureAnchor();
}
