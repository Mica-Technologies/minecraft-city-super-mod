package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Type of a preemption sequence in {@code ADVANCED} mode. The {@link #getPriority() priority} value
 * resolves contention when more than one preempt is active at once — higher wins, matching
 * real-world practice where railroad preemption outranks emergency-vehicle preemption, which in
 * turn outranks transit signal priority.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public enum TrafficSignalPreemptType {
  RAILROAD("Railroad", 3),
  EMERGENCY("Emergency Vehicle", 2),
  PRIORITY("Transit Priority", 1);

  private final String name;
  private final int priority;

  TrafficSignalPreemptType(String name, int priority) {
    this.name = name;
    this.priority = priority;
  }

  public String getName() {
    return name;
  }

  /**
   * @return the relative priority of this preempt type; higher overrides lower.
   */
  public int getPriority() {
    return priority;
  }

  public int toNBT() {
    return ordinal();
  }

  public static TrafficSignalPreemptType fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return EMERGENCY;
    }
    return values()[ordinal];
  }

  public TrafficSignalPreemptType getNext() {
    return values()[(ordinal() + 1) % values().length];
  }
}
