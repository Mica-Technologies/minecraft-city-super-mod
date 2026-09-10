package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * Which warning lights a barricade is carrying.
 *
 * <p>The ordinal is the persisted form, so entries must only ever be appended.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum BarricadeFlashers {

  /** No lights. */
  NONE("None", false, false),

  /** A light on the left-hand end only. */
  LEFT("Left only", true, false),

  /** A light on the right-hand end only. */
  RIGHT("Right only", false, true),

  /** A light on each end. */
  BOTH("Both ends", true, true);

  /** The name shown to the player. */
  private final String friendlyName;

  /** Whether the left-hand end carries a light. */
  private final boolean left;

  /** Whether the right-hand end carries a light. */
  private final boolean right;

  BarricadeFlashers(String friendlyName, boolean left, boolean right) {
    this.friendlyName = friendlyName;
    this.left = left;
    this.right = right;
  }

  /**
   * Gets the name shown to the player when the setting is changed.
   *
   * @return the friendly name
   *
   * @since 1.0
   */
  public String getFriendlyName() {
    return friendlyName;
  }

  /**
   * Gets whether the left-hand end carries a light.
   *
   * @return true if it does
   *
   * @since 1.0
   */
  public boolean hasLeft() {
    return left;
  }

  /**
   * Gets whether the right-hand end carries a light.
   *
   * @return true if it does
   *
   * @since 1.0
   */
  public boolean hasRight() {
    return right;
  }

  /**
   * Gets the next setting, wrapping.
   *
   * @return the next setting
   *
   * @since 1.0
   */
  public BarricadeFlashers next() {
    BarricadeFlashers[] values = values();
    return values[(ordinal() + 1) % values.length];
  }

  /**
   * Gets the setting with the given ordinal, falling back to none.
   *
   * @param ordinal the ordinal, as persisted
   *
   * @return the setting
   *
   * @since 1.0
   */
  public static BarricadeFlashers fromOrdinal(int ordinal) {
    BarricadeFlashers[] values = values();
    return ordinal < 0 || ordinal >= values.length ? NONE : values[ordinal];
  }
}
