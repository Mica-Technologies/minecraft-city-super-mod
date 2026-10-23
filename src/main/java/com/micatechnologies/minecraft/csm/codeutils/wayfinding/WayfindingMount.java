package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

/**
 * How a wayfinding panel is held up ({@code m}). Saved by {@link #getId() id}.
 *
 * <ul>
 *   <li>{@link #AUTO}: flat on the wall when the panel's bottom-left cell has a solid face behind
 *   it, hung otherwise. The default, and what every panel placed before mounts existed reads
 *   as: a panel built against a wall no longer hangs rods off it.</li>
 *   <li>{@link #WALL}: a thin plate flat against the block behind, one-sided, no rods.</li>
 *   <li>{@link #HUNG}: on rods up to the first block above, as the panels always were; the drop
 *   ({@code rd}) lowers it inside its cells so the rods show under a ceiling right above.</li>
 *   <li>{@link #POST}: on posts down to the first block below; the drop raises it.</li>
 *   <li>{@link #CEILING}: its top flush against the ceiling, no rods.</li>
 * </ul>
 *
 * @since 2026.10
 */
public enum WayfindingMount {
  AUTO("auto"),
  WALL("wall"),
  HUNG("hung"),
  POST("post"),
  CEILING("ceiling");

  private final String id;

  WayfindingMount(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  /** Whether the panel stands in the middle of its cells, drawn on both faces if it wants. */
  public boolean isFreeStanding() {
    return this != WALL;
  }

  public WayfindingMount next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /** The mount with this id, {@link #AUTO} for one not known. */
  public static WayfindingMount byId(String id) {
    for (WayfindingMount m : values()) {
      if (m.id.equals(id)) {
        return m;
      }
    }
    return AUTO;
  }
}
