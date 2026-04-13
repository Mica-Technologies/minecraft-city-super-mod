package com.micatechnologies.minecraft.csm.hvac;

/**
 * Tile entity for the rooftop HVAC cooler unit. Large industrial unit with slight local
 * heating effect (+2 deg F) from the compressor, but extended vent connection range (100
 * blocks) and cooling via vent relays (-12 deg F).
 */
public class TileEntityHvacRtuCooler extends TileEntityHvacHeater {

  @Override
  public float getTemperatureContribution() {
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
