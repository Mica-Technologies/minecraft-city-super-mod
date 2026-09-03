package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * What a programmed (NEMA) phase's movement does while the controller is in flash.
 *
 * <p>Without an override the controller falls back to its legacy flash rule, which is derived from
 * circuit <em>ordinal</em> position: the first circuit flashes yellow and the rest flash red on
 * alternating half-cycles. That rule only lines up when the circuits happen to have been linked in
 * street order, which is rarely true of an {@code ADVANCED} (ASC-3) intersection where the unit of
 * programming is a phase, not a circuit.
 *
 * <p>Setting any phase to something other than {@link #AUTO} switches the controller to a
 * <em>programmed</em> flash: every overridden movement lights on the same half-cycle and is dark on
 * the other (flash-transfer-relay behavior), and any head not claimed by an override flashes red.
 *
 * @author Mica Technologies
 * @see TrafficSignalProgrammedPhase#getFlashOverride()
 * @since 2026.7
 */
public enum TrafficSignalFlashOverride {
  /** No override: this phase's movement follows the legacy circuit-ordinal flash rule. */
  AUTO("Auto"),
  /** This phase's movement flashes yellow. */
  YELLOW("Flash Yellow"),
  /** This phase's movement flashes red. */
  RED("Flash Red"),
  /** This phase's movement stays dark in flash. */
  DARK("Dark");

  private final String name;

  TrafficSignalFlashOverride(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * @return whether this is an actual override (i.e. anything but {@link #AUTO}).
   */
  public boolean isOverride() {
    return this != AUTO;
  }

  /**
   * @return the ordinal value used to persist this override in NBT.
   */
  public int toNBT() {
    return ordinal();
  }

  /**
   * @param ordinal the persisted ordinal value
   *
   * @return the override with the given ordinal, or {@link #AUTO} if out of range.
   */
  public static TrafficSignalFlashOverride fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return AUTO;
    }
    return values()[ordinal];
  }

  /**
   * @return the next override in the enumeration, wrapping at the end (for GUI cycling).
   */
  public TrafficSignalFlashOverride getNext() {
    return values()[(ordinal() + 1) % values().length];
  }
}
