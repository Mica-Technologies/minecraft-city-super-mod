package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Per-phase recall mode for {@code ADVANCED} (NEMA) operation, mirroring the recall settings on a
 * real controller:
 *
 * <ul>
 *   <li>{@link #NONE} — phase is served only on a detector/pedestrian call (true actuation).</li>
 *   <li>{@link #MINIMUM} — phase is recalled every cycle and held at least its minimum green even
 *       without a call.</li>
 *   <li>{@link #MAXIMUM} — phase is recalled and held to its maximum green every cycle.</li>
 *   <li>{@link #PEDESTRIAN} — phase is recalled with a pedestrian call (Walk + ped clearance) every
 *       cycle.</li>
 *   <li>{@link #SOFT} — phase is recalled only when no other phase has a call (a place to rest).</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public enum TrafficSignalRecallMode {
  NONE("None"),
  MINIMUM("Minimum"),
  MAXIMUM("Maximum"),
  PEDESTRIAN("Pedestrian"),
  SOFT("Soft");

  private final String name;

  TrafficSignalRecallMode(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public int toNBT() {
    return ordinal();
  }

  public static TrafficSignalRecallMode fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return NONE;
    }
    return values()[ordinal];
  }

  public TrafficSignalRecallMode getNext() {
    return values()[(ordinal() + 1) % values().length];
  }
}
