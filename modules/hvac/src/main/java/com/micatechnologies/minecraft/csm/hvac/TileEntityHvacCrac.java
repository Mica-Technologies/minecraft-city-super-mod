package com.micatechnologies.minecraft.csm.hvac;

/**
 * A data hall's computer room air conditioner or air handler (CRAC/CRAH, #305): a cooler three
 * times a cabinet unit's capacity that, like a cabinet unit, cools the room it stands in with no
 * vents, or feeds a system's vents when linked to one.
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class TileEntityHvacCrac extends TileEntityHvacCooler {

  /** Heat removed per second at full output: three cabinet units. */
  public static final float CRAC_CAPACITY = 900.0f;

  @Override
  public float getHeatCapacity() {
    return CRAC_CAPACITY;
  }
}
