package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Enumeration of the different modes of the traffic signal controller. Each has a corresponding
 * ordinal value to be used in the NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #FLASH
 * @see #NORMAL
 * @see #REQUESTABLE
 * @see #MANUAL_OFF
 * @since 2023.2.0
 */
public enum TrafficSignalControllerMode {
  // region: Enumeration Values

  /**
   * Enumeration value for the traffic signal controller mode "flash" with a tick rate of 4.
   *
   * @since 1.0
   */
  FLASH("Flash", 10),

  /**
   * Enumeration value for the traffic signal controller mode "normal" with a tick rate of 20.
   *
   * @since 1.0
   */
  NORMAL("Normal", 20),

  /**
   * Enumeration value for the traffic signal controller mode "ramp meter (full time)" with a tick
   * rate of 20.
   *
   * @since 1.0
   */
  RAMP_METER_FULL_TIME("Ramp Meter (Full Time)", 80),

  /**
   * Enumeration value for the traffic signal controller mode "ramp meter (part time)" with a tick
   * rate of 20.
   *
   * @since 1.0
   */
  RAMP_METER_PART_TIME("Ramp Meter (Part Time)", 80),

  /**
   * Enumeration value for the traffic signal controller mode "requestable" with a tick rate of 20.
   *
   * @since 1.0
   */
  REQUESTABLE("Requestable", 10),

  /**
   * Enumeration value for the traffic signal controller mode "manual off" with a tick rate of 300.
   *
   * @since 1.0
   */
  MANUAL_OFF("Manual Off", 300),

  /**
   * Enumeration value for the traffic signal controller mode "forced fault" with a tick rate of
   * 10.
   *
   * @since 1.0
   */
  FORCED_FAULT("Flash (All Red)", 10);

  // endregion

  // region: Instance Fields

  /**
   * The name of the mode.
   *
   * @since 1.0
   */
  private final String name;

  /**
   * The tick rate value of the mode.
   *
   * @since 1.0
   */
  private final long tickRate;

  // endregion

  // region: Constructors

  /**
   * Constructor of the enumeration with the corresponding {@link String }name and the tick rate of
   * the mode.
   *
   * @param name     the name of the mode to be constructed
   * @param tickRate the tick rate value of the mode to be constructed
   *
   * @since 1.0
   */
  TrafficSignalControllerMode(String name, long tickRate) {
    this.name = name;
    this.tickRate = tickRate;
  }

  // endregion

  // region: Instance Methods

  /**
   * Gets the {@link TrafficSignalControllerMode} enum with the specified ordinal value, or
   * {@link TrafficSignalControllerMode#FLASH} if no {@link TrafficSignalControllerMode} enum with
   * the specified ordinal value exists. This can be used to retrieve a
   * {@link TrafficSignalControllerMode} enum from NBT data which was saved using the
   * {@link #toNBT()} method.
   *
   * @param ordinal the ordinal value of the {@link TrafficSignalControllerMode} enum to get
   *
   * @return the {@link TrafficSignalControllerMode} enum with the specified ordinal value, or
   *     {@link TrafficSignalControllerMode#FLASH} if no {@link TrafficSignalControllerMode} enum
   *     with the specified ordinal value exists
   *
   * @since 1.0
   */
  public static TrafficSignalControllerMode fromNBT(int ordinal) {
    // Check if the specified integer value is within the range of the enumeration
    int finalOrdinal = ordinal;
    if (ordinal < 0 || ordinal >= values().length) {
      finalOrdinal = 0;
    }

    // Return the enumeration value with the specified integer value
    return values()[finalOrdinal];
  }

  /**
   * Gets the name of the mode.
   *
   * @return the name of the mode
   *
   * @since 1.0
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the tick rate value of the mode.
   *
   * @return the tick rate value of the mode
   *
   * @since 1.0
   */
  public long getTickRate() {
    return tickRate;
  }

  /**
   * Gets the next {@link TrafficSignalControllerMode} enum value in the sequence.
   *
   * @return the next {@link TrafficSignalControllerMode} enum value in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalControllerMode getNextMode() {
    // Get the ordinal value of the current mode
    int ordinal = ordinal();

    // Get the next ordinal value
    int nextOrdinal = ordinal + 1;

    // If the next ordinal value is greater than the number of modes, reset to the first mode
    if (nextOrdinal >= values().length) {
      nextOrdinal = 0;
    }

    // Return the next mode
    return values()[nextOrdinal];
  }

  /**
   * Gets the ordinal value of the {@link TrafficSignalControllerMode} enum. This can be used to
   * store a {@link TrafficSignalControllerMode} enum in NBT data to be retrieved later using the
   * {@link #fromNBT(int)} method.
   *
   * @return the ordinal value of the {@link TrafficSignalControllerMode} enum
   *
   * @since 1.0
   */
  public int toNBT() {
    return ordinal();
  }

  // endregion
}
