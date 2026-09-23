package com.micatechnologies.minecraft.csm.hvac;

/**
 * The rooftop heating unit: four cabinet heaters' worth of heat, delivered only through vents
 * (it stands outdoors), which may be up to 100 blocks away.
 */
public class TileEntityHvacRtuHeater extends TileEntityHvacHeater {

  /** Heat per second at full output. */
  public static final float RTU_CAPACITY = 1200.0f;

  @Override
  public float getHeatCapacity() {
    return RTU_CAPACITY;
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
