package com.micatechnologies.minecraft.csm.technology.rack;

/**
 * Where the units go in each kind of rack: how many cells and U it has, where its first U
 * starts, and how far in the units' faces are. A U is two thirds of a model pixel in every
 * frame, so a unit looks the same in each.
 *
 * <p><b>Written by {@code dev-env-utils/scripts/gen_technology_rack.py}; edit it there.</b> The
 * generator writes the pre-filled racks' models from the same numbers, so they and a filled
 * rack look the same. Not saved anywhere: the order may change.</p>
 *
 * @since 2026.10
 */
public enum RackFrame {

  /**
   * The open two-post frame: a unit's face is at most 5.75 px in and its back at most 15.5,
   * so a deep unit hangs centred on the posts as it does on a real relay rack.
   */
  OPEN_42U(2, 42, 2.0f, true, 5.75f, 15.5f),
  /**
   * The enclosed cabinet: faces flush with the rails behind the door, the rear door 13.8 px
   * behind them.
   */
  CABINET_42U(2, 42, 2.0f, false, 1.6f, 13.8f),
  /**
   * The wall cabinet: 400 mm deep, so only the units 6.2 px deep or less fit.
   */
  WALL_12U(1, 12, 4.0f, false, 9.2f, 6.2f);

  /** A U, in model pixels. */
  public static final float PITCH = 2.0f / 3.0f;

  /** The units' faces span x 2.0 to 14.0. */
  public static final float UNIT_X0 = 2.0f;

  /** See {@link #UNIT_X0}. */
  public static final float UNIT_X1 = 14.0f;

  /** In an open frame, the furthest back a unit reaches. */
  public static final float OPEN_BACK = 15.5f;

  private final int cells;
  private final int totalU;
  private final float firstY;
  private final boolean open;
  private final float face;
  private final float deepest;

  RackFrame(int cells, int totalU, float firstY, boolean open, float face, float deepest) {
    this.cells = cells;
    this.totalU = totalU;
    this.firstY = firstY;
    this.open = open;
    this.face = face;
    this.deepest = deepest;
  }

  /** How many block cells tall the rack is (1 or 2). */
  public int getCells() {
    return cells;
  }

  /** How many U it holds. */
  public int getTotalU() {
    return totalU;
  }

  /** The floor of U1, in model pixels from the bottom of the lowest cell. */
  public float getFirstY() {
    return firstY;
  }

  /** Whether it is an open frame, whose units show their backs. */
  public boolean isOpen() {
    return open;
  }

  /** The deepest unit it takes, in model pixels. */
  public float getDeepest() {
    return deepest;
  }

  /**
   * Whether a unit fits this rack's depth.
   *
   * @param type the unit
   * @return whether it fits
   */
  public boolean accepts(RackUnitType type) {
    return type.getDepth() <= deepest;
  }

  /**
   * How far in a unit's face is (z, model pixels from the front).
   *
   * @param depth the unit's depth
   * @return its face's z
   */
  public float frontZ(float depth) {
    return open ? Math.min(face, OPEN_BACK - depth) : face;
  }

  /**
   * The U at a height in the rack, or 0 below U1 and {@code totalU + 1} above the top U.
   *
   * @param y model pixels from the bottom of the lowest cell
   * @return the U
   */
  public int uAt(float y) {
    float k = (y - firstY) / PITCH;
    if (k < 0) {
      return 0;
    }
    return Math.min(totalU + 1, (int) Math.floor(k) + 1);
  }
}
