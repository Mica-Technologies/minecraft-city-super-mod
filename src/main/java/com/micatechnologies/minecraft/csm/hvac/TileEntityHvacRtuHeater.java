package com.micatechnologies.minecraft.csm.hvac;

/**
 * Tile entity for the rooftop HVAC heater unit. Large industrial unit with minimal local
 * temperature effect (+2 deg F) but extended vent connection range (100 blocks) and stronger
 * vent relay contribution (+12 deg F).
 */
public class TileEntityHvacRtuHeater extends TileEntityHvacHeater {

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
    return 12.0F;
  }
}
