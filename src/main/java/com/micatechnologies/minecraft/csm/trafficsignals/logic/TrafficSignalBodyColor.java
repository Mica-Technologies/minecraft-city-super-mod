package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Enumeration of the different body colors of traffic signals. Each has a corresponding ordinal
 * value to be used in the NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2024.1
 */
public enum TrafficSignalBodyColor {
  // region: Enumeration Values

  /**
   * Enumeration value for the traffic signal body color "flat black".
   *
   * @since 1.0
   */
  FLAT_BLACK("Flat Black", "flat_black"),

  /**
   * Enumeration value for the traffic signal body color "dark olive green".
   *
   * @since 1.0
   */
  DARK_OLIVE_GREEN("Dark Olive Green", "dark_olive_green"),

  /**
   * Enumeration value for the traffic signal body color "battleship gray".
   *
   * @since 1.0
   */
  BATTLESHIP_GRAY("Battleship Gray", "battleship_gray"),

  /**
   * Enumeration value for the traffic signal body color "gloss black".
   *
   * @since 1.0
   */
  GLOSS_BLACK("Gloss Black", "gloss_black"),

  /**
   * Enumeration value for the traffic signal body color "tan".
   *
   * @since 1.0
   */
  TAN("Tan", "tan"),

  /**
   * Enumeration value for the traffic signal body color "white".
   *
   * @since 1.0
   */
  WHITE("White", "white"),

  /**
   * Enumeration value for the traffic signal body color "yellow".
   *
   * @since 1.0
   */
  YELLOW("Yellow", "yellow");

  // endregion

  // region: Instance Fields

  /**
   * The name of the signal body color.
   *
   * @since 1.0
   */
  private final String name;

  /**
   * The ID of the signal body color
   *
   * @since 1.0
   */
  private final String id;

  // endregion

  // region: Constructors

  /**
   * Constructor of the enumeration with the corresponding {@link String} name and ID.
   *
   * @param name the name of the signal body color enum to be constructed
   * @param id   the ID of the signal body color enum to be constructed
   *
   * @since 1.0
   */
  TrafficSignalBodyColor(String name, String id) {
    this.name = name;
    this.id = id;
  }

  // endregion

  // region: Instance Methods

  /**
   * Gets the {@link TrafficSignalBodyColor} enum with the specified ordinal value, or
   * {@link TrafficSignalBodyColor#FLAT_BLACK} if no {@link TrafficSignalBodyColor} enum with the
   * specified ordinal value exists. This can be used to retrieve a {@link TrafficSignalBodyColor}
   * enum from NBT data which was saved using the {@link #toNBT()} method.
   *
   * @param ordinal the ordinal value of the {@link TrafficSignalBodyColor} enum to get
   *
   * @return the {@link TrafficSignalBodyColor} enum with the specified ordinal value, or
   *     {@link TrafficSignalBodyColor#FLAT_BLACK} if no {@link TrafficSignalBodyColor} enum with
   *     the specified ordinal value exists
   *
   * @since 1.0
   */
  public static TrafficSignalBodyColor fromNBT(int ordinal) {
    // Check if the specified integer value is within the range of the enumeration
    int finalOrdinal = ordinal;
    if (ordinal < 0 || ordinal >= values().length) {
      finalOrdinal = 0;
    }

    // Return the enumeration value with the specified integer value
    return values()[finalOrdinal];
  }

  /**
   * Gets the name of the signal body color.
   *
   * @return the name of the signal body color
   *
   * @since 1.0
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the ID of the signal body color.
   *
   * @return the ID of the signal body color
   *
   * @since 1.0
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the next {@link TrafficSignalBodyColor} enum value in the sequence.
   *
   * @return the next {@link TrafficSignalBodyColor} enum value in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextColor() {
    // Get the ordinal value of the current color
    int ordinal = ordinal();

    // Get the next ordinal value
    int nextOrdinal = ordinal + 1;

    // If the next ordinal value is greater than the number of colors, reset to the first mode
    if (nextOrdinal >= values().length) {
      nextOrdinal = 0;
    }

    // Return the next color
    return values()[nextOrdinal];
  }

  /**
   * Gets the ordinal value of the {@link TrafficSignalBodyColor} enum. This can be used to store a
   * {@link TrafficSignalBodyColor} enum in NBT data to be retrieved later using the
   * {@link #fromNBT(int)} method.
   *
   * @return the ordinal value of the {@link TrafficSignalBodyColor} enum
   *
   * @since 1.0
   */
  public int toNBT() {
    return ordinal();
  }

  // endregion
}
