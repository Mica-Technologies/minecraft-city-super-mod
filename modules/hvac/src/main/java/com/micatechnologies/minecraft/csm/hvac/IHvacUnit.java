package com.micatechnologies.minecraft.csm.hvac;

/**
 * A heating or cooling unit: something that moves heat into or out of the rooms it serves.
 *
 * <p>Most units do one or the other: a heater adds heat, a cooler ({@link #isCoolingUnit()})
 * removes it. A <em>combination</em> unit -- a PTAC, a packaged rooftop unit with its gas heat
 * section, a heat pump -- does both, though never at once: it answers {@link #canHeat()} and
 * {@link #canCool()} with {@code true} and serves whichever mode its system is in, or standalone
 * whichever its room needs. Control code asks those two, never {@code isCoolingUnit()} alone.</p>
 *
 * @author Mica Technologies
 * @see HvacSystemControl
 * @since 2026.4
 */
public interface IHvacUnit {

  /**
   * Heat the unit moves at full output, per second, in the simulation's units (degrees
   * Fahrenheit times air cells). Always positive; {@link #canHeat()} and {@link #canCool()} give
   * the directions it can move it.
   */
  float getHeatCapacity();

  /**
   * Whether the unit only removes heat. A combination unit is not a cooling unit: it answers
   * {@code false} here and {@code true} to both {@link #canHeat()} and {@link #canCool()}.
   */
  boolean isCoolingUnit();

  /** Whether the unit can add heat: a heater, or a combination unit. */
  default boolean canHeat() {
    return !isCoolingUnit();
  }

  /** Whether the unit can remove heat: a cooler, or a combination unit. */
  default boolean canCool() {
    return isCoolingUnit();
  }

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
