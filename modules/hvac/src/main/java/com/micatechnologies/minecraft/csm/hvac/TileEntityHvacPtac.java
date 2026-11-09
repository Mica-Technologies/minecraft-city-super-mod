package com.micatechnologies.minecraft.csm.hvac;

/**
 * A PTAC ({@link BlockHvacPtac}): a hotel room's through-the-wall unit, a combination unit of
 * about a ton, a third of a cabinet unit. It sits in the room it serves, so it heats or cools
 * that room with no vents, standalone or linked to a thermostat, and may also feed a system's
 * vents like any unit.
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class TileEntityHvacPtac extends TileEntityHvacComboUnit {

  /** Heat moved per second at full output: about a ton against the cabinet unit's three. */
  public static final float PTAC_CAPACITY = 100.0f;

  @Override
  public float getHeatCapacity() {
    return PTAC_CAPACITY;
  }
}
