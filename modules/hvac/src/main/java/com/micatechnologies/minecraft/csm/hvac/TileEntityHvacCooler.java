package com.micatechnologies.minecraft.csm.hvac;

/**
 * The cabinet cooler: a {@link TileEntityHvacHeater} that removes heat instead of adding it.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class TileEntityHvacCooler extends TileEntityHvacHeater {

  @Override
  public boolean isCoolingUnit() {
    return true;
  }
}
