package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Utility class for providing a summary of sensor information for a
 * {@link TrafficSignalControllerCircuit}, including the cumulative count of waiting entities at all
 * sensors and each individual facing direction, east, west, north, south, up, and down.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see TrafficSignalControllerCircuit
 * @since 2023.2.0
 */
public class TrafficSignalSensorSummary {
  // region: Fields

  /**
   * The total count of waiting entities at all standard sensors.
   *
   * @since 1.0
   */
  private final int standardTotal;

  /**
   * The count of waiting entities at standard east sensors.
   *
   * @since 1.0
   */
  private final int standardEast;

  /**
   * The count of waiting entities at standard west sensors.
   *
   * @since 1.0
   */
  private final int standardWest;

  /**
   * The count of waiting entities at standard north sensors.
   *
   * @since 1.0
   */
  private final int standardNorth;

  /**
   * The count of waiting entities at standard south sensors.
   *
   * @since 1.0
   */
  private final int standardSouth;

  /**
   * The total count of waiting entities at all left sensors.
   *
   * @since 1.0
   */
  private final int leftTotal;

  /**
   * The count of waiting entities at left east sensors.
   *
   * @since 1.0
   */
  private final int leftEast;

  /**
   * The count of waiting entities at left west sensors.
   *
   * @since 1.0
   */
  private final int leftWest;

  /**
   * The count of waiting entities at left north sensors.
   *
   * @since 1.0
   */
  private final int leftNorth;

  /**
   * The count of waiting entities at left south sensors.
   *
   * @since 1.0
   */
  private final int leftSouth;

  /**
   * The total count of waiting entities at all protected sensors.
   *
   * @since 1.0
   */
  private final int protectedTotal;

  /**
   * The count of waiting entities at protected east sensors.
   *
   * @since 1.0
   */
  private final int protectedEast;

  /**
   * The count of waiting entities at protected west sensors.
   *
   * @since 1.0
   */
  private final int protectedWest;

  /**
   * The count of waiting entities at protected north sensors.
   *
   * @since 1.0
   */
  private final int protectedNorth;

  /**
   * The count of waiting entities at protected south sensors.
   *
   * @since 1.0
   */
  private final int protectedSouth;

  /**
   * The total count of waiting entities at all sensors.
   *
   * @since 1.0
   */
  private final int totalAll;

  /**
   * The total count of waiting entities at all east sensors.
   *
   * @since 1.0
   */
  private final int totalEast;

  /**
   * The total count of waiting entities at all west sensors.
   *
   * @since 1.0
   */
  private final int totalWest;

  /**
   * The total count of waiting entities at all north sensors.
   *
   * @since 1.0
   */
  private final int totalNorth;

  /**
   * The total count of waiting entities at all south sensors.
   *
   * @since 1.0
   */
  private final int totalSouth;

  /**
   * The total count of waiting entities at all (except protected) sensors.
   *
   * @since 1.0
   */
  private final int nonProtectedTotalAll;

  /**
   * The total count of waiting entities at all (except protected) east sensors.
   *
   * @since 1.0
   */
  private final int nonProtectedTotalEast;

  /**
   * The total count of waiting entities at all (except protected) west sensors.
   *
   * @since 1.0
   */
  private final int nonProtectedTotalWest;

  /**
   * The total count of waiting entities at all (except protected) north sensors.
   *
   * @since 1.0
   */
  private final int nonProtectedTotalNorth;

  /**
   * The total count of waiting entities at all (except protected) south sensors.
   *
   * @since 1.0
   */
  private final int nonProtectedTotalSouth;

  // endregion

  // region: Constructors

  /**
   * Creates a new instance of the {@link TrafficSignalSensorSummary} class with the specified
   * cumulative count of waiting entities at all sensors and each individual facing direction, east,
   * west, north, and south.
   *
   * @param standardTotal  The total count of waiting entities at all standard sensors.
   * @param standardEast   The count of waiting entities at standard east sensors.
   * @param standardWest   The count of waiting entities at standard west sensors.
   * @param standardNorth  The count of waiting entities at standard north sensors.
   * @param standardSouth  The count of waiting entities at standard south sensors.
   * @param leftTotal      The total count of waiting entities at all left sensors.
   * @param leftEast       The count of waiting entities at left east sensors.
   * @param leftWest       The count of waiting entities at left west sensors.
   * @param leftNorth      The count of waiting entities at left north sensors.
   * @param leftSouth      The count of waiting entities at left south sensors.
   * @param protectedTotal The total count of waiting entities at all protected sensors.
   * @param protectedEast  The count of waiting entities at protected east sensors.
   * @param protectedWest  The count of waiting entities at protected west sensors.
   * @param protectedNorth The count of waiting entities at protected north sensors.
   * @param protectedSouth The count of waiting entities at protected south sensors.
   *
   * @since 1.0
   */
  public TrafficSignalSensorSummary(int standardTotal,
      int standardEast,
      int standardWest,
      int standardNorth,
      int standardSouth,
      int leftTotal,
      int leftEast,
      int leftWest,
      int leftNorth,
      int leftSouth,
      int protectedTotal,
      int protectedEast,
      int protectedWest,
      int protectedNorth,
      int protectedSouth) {
    this.standardTotal = standardTotal;
    this.standardEast = standardEast;
    this.standardWest = standardWest;
    this.standardNorth = standardNorth;
    this.standardSouth = standardSouth;
    this.leftTotal = leftTotal;
    this.leftEast = leftEast;
    this.leftWest = leftWest;
    this.leftNorth = leftNorth;
    this.leftSouth = leftSouth;
    this.protectedTotal = protectedTotal;
    this.protectedEast = protectedEast;
    this.protectedWest = protectedWest;
    this.protectedNorth = protectedNorth;
    this.protectedSouth = protectedSouth;
    this.totalAll = standardTotal + leftTotal + protectedTotal;
    this.totalEast = standardEast + leftEast + protectedEast;
    this.totalWest = standardWest + leftWest + protectedWest;
    this.totalNorth = standardNorth + leftNorth + protectedNorth;
    this.totalSouth = standardSouth + leftSouth + protectedSouth;
    this.nonProtectedTotalAll = standardTotal + leftTotal;
    this.nonProtectedTotalEast = standardEast + leftEast;
    this.nonProtectedTotalWest = standardWest + leftWest;
    this.nonProtectedTotalNorth = standardNorth + leftNorth;
    this.nonProtectedTotalSouth = standardSouth + leftSouth;
  }

  // endregion

  // region: Properties

  /**
   * Gets the total count of waiting entities at all standard sensors.
   *
   * @return The total count of waiting entities at all standard sensors.
   *
   * @since 1.0
   */
  public int getStandardTotal() {
    return standardTotal;
  }

  /**
   * Gets the count of waiting entities at standard east sensors.
   *
   * @return The count of waiting entities at standard east sensors.
   *
   * @since 1.0
   */
  public int getStandardEast() {
    return standardEast;
  }

  /**
   * Gets the count of waiting entities at standard west sensors.
   *
   * @return The count of waiting entities at standard west sensors.
   *
   * @since 1.0
   */
  public int getStandardWest() {
    return standardWest;
  }

  /**
   * Gets the count of waiting entities at standard north sensors.
   *
   * @return The count of waiting entities at standard north sensors.
   *
   * @since 1.0
   */
  public int getStandardNorth() {
    return standardNorth;
  }

  /**
   * Gets the count of waiting entities at standard south sensors.
   *
   * @return The count of waiting entities at standard south sensors.
   *
   * @since 1.0
   */
  public int getStandardSouth() {
    return standardSouth;
  }

  /**
   * Gets the total count of waiting entities at all left sensors.
   *
   * @return The total count of waiting entities at all left sensors.
   *
   * @since 1.0
   */
  public int getLeftTotal() {
    return leftTotal;
  }

  /**
   * Gets the count of waiting entities at left east sensors.
   *
   * @return The count of waiting entities at left east sensors.
   *
   * @since 1.0
   */
  public int getLeftEast() {
    return leftEast;
  }

  /**
   * Gets the count of waiting entities at left west sensors.
   *
   * @return The count of waiting entities at left west sensors.
   *
   * @since 1.0
   */
  public int getLeftWest() {
    return leftWest;
  }

  /**
   * Gets the count of waiting entities at left north sensors.
   *
   * @return The count of waiting entities at left north sensors.
   *
   * @since 1.0
   */
  public int getLeftNorth() {
    return leftNorth;
  }

  /**
   * Gets the count of waiting entities at left south sensors.
   *
   * @return The count of waiting entities at left south sensors.
   *
   * @since 1.0
   */
  public int getLeftSouth() {
    return leftSouth;
  }

  /**
   * Gets the total count of waiting entities at all protected sensors.
   *
   * @return The total count of waiting entities at all protected sensors.
   *
   * @since 1.0
   */
  public int getProtectedTotal() {
    return protectedTotal;
  }

  /**
   * Gets the count of waiting entities at protected east sensors.
   *
   * @return The count of waiting entities at protected east sensors.
   *
   * @since 1.0
   */
  public int getProtectedEast() {
    return protectedEast;
  }

  /**
   * Gets the count of waiting entities at protected west sensors.
   *
   * @return The count of waiting entities at protected west sensors.
   *
   * @since 1.0
   */
  public int getProtectedWest() {
    return protectedWest;
  }

  /**
   * Gets the count of waiting entities at protected north sensors.
   *
   * @return The count of waiting entities at protected north sensors.
   *
   * @since 1.0
   */
  public int getProtectedNorth() {
    return protectedNorth;
  }

  /**
   * Gets the count of waiting entities at protected south sensors.
   *
   * @return The count of waiting entities at protected south sensors.
   *
   * @since 1.0
   */
  public int getProtectedSouth() {
    return protectedSouth;
  }

  /**
   * Gets the total count of waiting entities at all sensors.
   *
   * @return The total count of waiting entities at all sensors.
   *
   * @since 1.0
   */
  public int getTotalAll() {
    return totalAll;
  }

  /**
   * Gets the total count of waiting entities at all east sensors.
   *
   * @return The total count of waiting entities at all east sensors.
   *
   * @since 1.0
   */
  public int getTotalEast() {
    return totalEast;
  }

  /**
   * Gets the total count of waiting entities at all west sensors.
   *
   * @return The total count of waiting entities at all west sensors.
   *
   * @since 1.0
   */
  public int getTotalWest() {
    return totalWest;
  }

  /**
   * Gets the total count of waiting entities at all north sensors.
   *
   * @return The total count of waiting entities at all north sensors.
   *
   * @since 1.0
   */
  public int getTotalNorth() {
    return totalNorth;
  }

  /**
   * Gets the total count of waiting entities at all south sensors.
   *
   * @return The total count of waiting entities at all south sensors.
   *
   * @since 1.0
   */
  public int getTotalSouth() {
    return totalSouth;
  }

  /**
   * Gets the total count of waiting entities at all (except protected) sensors.
   *
   * @return The total count of waiting entities at all (except protected) sensors.
   *
   * @since 1.0
   */
  public int getNonProtectedTotalAll() {
    return nonProtectedTotalAll;
  }

  /**
   * Gets the total count of waiting entities at all (except protected) east sensors.
   *
   * @return The total count of waiting entities at all (except protected) east sensors.
   *
   * @since 1.0
   */
  public int getNonProtectedTotalEast() {
    return nonProtectedTotalEast;
  }

  /**
   * Gets the total count of waiting entities at all (except protected) west sensors.
   *
   * @return The total count of waiting entities at all (except protected) west sensors.
   *
   * @since 1.0
   */
  public int getNonProtectedTotalWest() {
    return nonProtectedTotalWest;
  }

  /**
   * Gets the total count of waiting entities at all (except protected) north sensors.
   *
   * @return The total count of waiting entities at all (except protected) north sensors.
   *
   * @since 1.0
   */
  public int getNonProtectedTotalNorth() {
    return nonProtectedTotalNorth;
  }

  /**
   * Gets the total count of waiting entities at all (except protected) south sensors.
   *
   * @return The total count of waiting entities at all (except protected) south sensors.
   *
   * @since 1.0
   */
  public int getNonProtectedTotalSouth() {
    return nonProtectedTotalSouth;
  }

  // endregion
}
