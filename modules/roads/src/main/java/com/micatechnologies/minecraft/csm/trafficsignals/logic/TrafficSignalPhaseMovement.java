package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * The movement a programmed (NEMA) phase serves. In {@code ADVANCED} mode each phase maps onto an
 * existing {@link TrafficSignalControllerCircuit} plus one of these movements, which selects both
 * the circuit signal list the phase drives and the sensor-summary zone that calls it:
 *
 * <ul>
 *   <li>{@link #THROUGH} &rarr; through signals / {@code standardTotal} detection</li>
 *   <li>{@link #LEFT} / {@link #PROTECTED_LEFT} &rarr; left (or protected) signals / {@code leftTotal}</li>
 *   <li>{@link #RIGHT} &rarr; right signals / {@code rightTotal}</li>
 *   <li>{@link #PED} &rarr; pedestrian signals / pedestrian button request count</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public enum TrafficSignalPhaseMovement {
  THROUGH("Through"),
  LEFT("Left"),
  PROTECTED_LEFT("Protected Left"),
  RIGHT("Right"),
  PED("Pedestrian");

  private final String name;

  TrafficSignalPhaseMovement(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * @return the ordinal value used to persist this movement in NBT.
   */
  public int toNBT() {
    return ordinal();
  }

  /**
   * @param ordinal the persisted ordinal value
   *
   * @return the movement with the given ordinal, or {@link #THROUGH} if out of range.
   */
  public static TrafficSignalPhaseMovement fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return THROUGH;
    }
    return values()[ordinal];
  }

  /**
   * @return the next movement in the enumeration, wrapping at the end (for GUI cycling).
   */
  public TrafficSignalPhaseMovement getNext() {
    return values()[(ordinal() + 1) % values().length];
  }
}
