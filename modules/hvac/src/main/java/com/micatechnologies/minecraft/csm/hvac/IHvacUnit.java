package com.micatechnologies.minecraft.csm.hvac;

/**
 * A heating or cooling unit: something that moves heat into or out of the rooms it serves.
 *
 * @author Mica Technologies
 * @see HvacSystemControl
 * @since 2026.4
 */
public interface IHvacUnit {

  /**
   * Heat the unit moves at full output, per second, in the simulation's units (degrees
   * Fahrenheit times air cells). Always positive; {@link #isCoolingUnit()} gives the direction.
   */
  float getHeatCapacity();

  /** Whether the unit removes heat rather than adding it. */
  boolean isCoolingUnit();

  /**
   * Whether the unit can only deliver through vents. A rooftop unit sits outdoors: with no vents
   * it heats nothing.
   */
  default boolean isDuctedOnly() {
    return false;
  }

  /** Farthest a vent may be linked from a system containing this unit, blocks. */
  default int getMaxVentLinkDistance() {
    return 30;
  }
}
