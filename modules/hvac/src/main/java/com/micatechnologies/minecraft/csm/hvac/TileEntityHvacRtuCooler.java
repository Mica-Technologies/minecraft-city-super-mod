package com.micatechnologies.minecraft.csm.hvac;

/**
 * The rooftop cooling unit: four cabinet coolers' worth of cooling, delivered only through vents,
 * which may be up to 100 blocks away.
 *
 * <p><b>Inheritance note:</b> extends {@link TileEntityHvacCooler} so {@code instanceof} checks for
 * a cooler find it.</p>
 */
public class TileEntityHvacRtuCooler extends TileEntityHvacCooler {

  @Override
  public float getHeatCapacity() {
    return TileEntityHvacRtuHeater.RTU_CAPACITY;
  }

  @Override
  public boolean isDuctedOnly() {
    return true;
  }

  @Override
  public int getMaxVentLinkDistance() {
    return 100;
  }
}
