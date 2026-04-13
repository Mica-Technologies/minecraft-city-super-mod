package com.micatechnologies.minecraft.csm.hvac;

/**
 * Tile entity for the HVAC cooler block. Identical to {@link TileEntityHvacHeater} except that it
 * contributes a negative temperature offset (cooling).
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacCooler extends TileEntityHvacHeater {

  @Override
  public float getTemperatureContribution() {
    return -15.0F;
  }
}
