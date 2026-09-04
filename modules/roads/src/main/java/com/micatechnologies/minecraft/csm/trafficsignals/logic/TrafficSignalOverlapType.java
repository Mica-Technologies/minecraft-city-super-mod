package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * The type of a {@link TrafficSignalProgrammedOverlap}, mirroring the Econolite ASC/3 MM-2-2 vehicle
 * overlap types:
 *
 * <ul>
 *   <li>{@link #NORMAL} — green while any included phase is green, yellow during their clearance,
 *       else red.</li>
 *   <li>{@link #MINUS_GREEN_YELLOW} — NORMAL, but forced red while any <i>modifier</i> phase is
 *       green or yellow (the overlap is "minus" that movement). Used to drop a right-turn overlap
 *       during a specific conflicting phase.</li>
 * </ul>
 *
 * <p>FYA (protected/permissive left turns) is handled by the per-phase {@code permissivePhase}
 * mechanism rather than an overlap type — see {@code assets/docs/ADVANCED_MODE_ASC3.md}.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public enum TrafficSignalOverlapType {
  NORMAL("Normal"),
  MINUS_GREEN_YELLOW("-Grn/Yel");

  private final String name;

  TrafficSignalOverlapType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public int toNBT() {
    return ordinal();
  }

  public static TrafficSignalOverlapType fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return NORMAL;
    }
    return values()[ordinal];
  }
}
