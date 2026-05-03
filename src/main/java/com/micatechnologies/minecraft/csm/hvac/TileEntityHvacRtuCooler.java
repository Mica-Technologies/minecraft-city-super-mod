package com.micatechnologies.minecraft.csm.hvac;

/**
 * Tile entity for the rooftop HVAC cooler unit. Large industrial unit with slight local
 * heating effect (+2 deg F) from the compressor, but extended vent connection range (100
 * blocks) and cooling via vent relays (-12 deg F).
 *
 * <p><b>Inheritance note:</b> extends {@link TileEntityHvacCooler} (not Heater), so
 * {@code instanceof TileEntityHvacCooler} checks correctly identify this as a cooler.
 * Previously this extended Heater directly, which broke the thermostat's mode-lockout
 * logic (system would say "no cooler in primary" even with an RTU cooler linked).</p>
 */
public class TileEntityHvacRtuCooler extends TileEntityHvacCooler {

  @Override
  protected float getActiveContribution() {
    // RTU coolers vent the compressor heat at the unit's location, so the local
    // contribution is positive (+2°F) even though the unit's job is cooling — the
    // cooling effect propagates through linked vents, not through direct contribution.
    return 2.0F;
  }

  @Override
  public int getMaxVentLinkDistance() {
    return 100;
  }

  @Override
  public float getVentRelayContribution() {
    return -12.0F;
  }
}
