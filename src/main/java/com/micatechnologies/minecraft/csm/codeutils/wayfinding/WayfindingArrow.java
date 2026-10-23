package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

/**
 * Which way a wayfinding panel's arrow points, as the reader of the front sees it. The panel's
 * own arrow ({@code a}) is drawn at the end it points to: the left end for the three leftward
 * ones, the right end otherwise. Saved by {@link #getId() id}, never by ordinal.
 *
 * @since 2026.10
 */
public enum WayfindingArrow {
  NONE("none", 0),
  LEFT("left", 180),
  RIGHT("right", 0),
  UP("up", 90),
  DOWN("down", 270),
  UP_LEFT("up_left", 135),
  UP_RIGHT("up_right", 45),
  DOWN_LEFT("down_left", 225),
  DOWN_RIGHT("down_right", 315);

  private final String id;
  private final int angle;

  WayfindingArrow(String id, int angle) {
    this.id = id;
    this.angle = angle;
  }

  public String getId() {
    return id;
  }

  /** Degrees anticlockwise from pointing right, as the reader sees it. */
  public int getAngle() {
    return angle;
  }

  /** Whether the arrow sits at the reader's left end. */
  public boolean isLeftEnd() {
    return this == LEFT || this == UP_LEFT || this == DOWN_LEFT;
  }

  /**
   * The arrow the back of the sign shows: left and right swapped, so that it points the same way
   * in the world from either side.
   */
  public WayfindingArrow mirrored() {
    switch (this) {
      case LEFT:
        return RIGHT;
      case RIGHT:
        return LEFT;
      case UP_LEFT:
        return UP_RIGHT;
      case UP_RIGHT:
        return UP_LEFT;
      case DOWN_LEFT:
        return DOWN_RIGHT;
      case DOWN_RIGHT:
        return DOWN_LEFT;
      default:
        return this;
    }
  }

  public WayfindingArrow next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /** The arrow with this id, {@link #NONE} for one not known. */
  public static WayfindingArrow byId(String id) {
    for (WayfindingArrow a : values()) {
      if (a.id.equals(id)) {
        return a;
      }
    }
    return NONE;
  }
}
