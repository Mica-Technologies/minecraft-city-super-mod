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
   * Flash mode. Alternates between two cached flash phases via tick-based toggle.
   * Tick rate 10 (0.5 seconds per phase = 1 second full cycle).
   *
   * @since 1.0
   */
  FLASH("Flash", 10),

  /**
   * Normal mode. Cycles through green/yellow/red phases with configured durations.
   * Tick rate 20 (1 second): phase durations are 2-90+ seconds, so ±1s jitter is fine.
   *
   * @since 1.0
   */
  NORMAL("Normal", 20),

  /**
   * Full-time ramp meter. Checks sensors for waiting vehicles and alternates green/red.
   * Tick rate 80 (4 second): responsive enough for vehicle detection without wasting ticks.
   *
   * @since 1.0
   */
  RAMP_METER_FULL_TIME("Ramp Meter (Full Time)", 80),

  /**
   * Part-time ramp meter. Same as full-time during the day, disabled/flash/off at night.
   * Tick rate 80 (4 second): same reasoning as full-time.
   *
   * @since 1.0
   */
  RAMP_METER_PART_TIME("Ramp Meter (Part Time)", 80),

  /**
   * Requestable mode. Polls sensors and changes phases on request. Tick rate 10 (0.5 seconds):
   * needs faster sensor polling than normal mode for responsive vehicle/pedestrian detection.
   *
   * @since 1.0
   */
  REQUESTABLE("Requestable", 10),

  /**
   * Manual off. Sets all signals to off once, then idles. Tick rate 300 (15 seconds):
   * nothing to do after the initial off phase is set.
   *
   * @since 1.0
   */
  MANUAL_OFF("Manual Off", 300),

  /**
   * Wrong way detection mode. Polls linked sensors rapidly and activates beacons on circuits
   * where an entity is detected approaching the sensor (decreasing distance). Each circuit
   * operates independently. Beacons hold active for 30 seconds after the last detection.
   * Tick rate 10 (0.5 seconds): fast polling for responsive wrong-way approach detection.
   *
   * @since 1.0
   */
  WRONG_WAY_DETECTION("Wrong Way Detection", 10),

  /**
   * Forced fault (all-red flash). Alternates all-red phases via tick-based toggle.
   * Tick rate 10 (0.5 seconds per phase = 1 second full cycle).
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
