package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Coordination mode for {@code ADVANCED} operation.
 *
 * <ul>
 *   <li>{@link #FREE} — fully actuated, isolated operation: phases are served on demand with no
 *       fixed cycle.</li>
 *   <li>{@link #COORDINATED} — runs on a fixed background cycle (cycle length + offset) with
 *       per-phase splits and force-offs; coordinated phases rest in green and yield only within
 *       their permissive windows.</li>
 * </ul>
 *
 * <p>Flash fallback (nightly / power-loss / fault) is handled by the existing controller fallback
 * machinery, so it is intentionally not a value here.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public enum TrafficSignalCoordinationMode {
  FREE("Free"),
  COORDINATED("Coordinated");

  private final String name;

  TrafficSignalCoordinationMode(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public int toNBT() {
    return ordinal();
  }

  public static TrafficSignalCoordinationMode fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return FREE;
    }
    return values()[ordinal];
  }

  public TrafficSignalCoordinationMode getNext() {
    return values()[(ordinal() + 1) % values().length];
  }
}
