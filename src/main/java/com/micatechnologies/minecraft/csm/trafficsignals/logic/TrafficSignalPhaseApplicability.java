package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Enumeration of the different directions of a traffic signal phase, such as left turns, right turns, all east signals,
 * all west signals, etc.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #NONE
 * @see #PEDESTRIAN
 * @see #ALL_EAST
 * @see #ALL_WEST
 * @see #ALL_NORTH
 * @see #ALL_SOUTH
 * @see #ALL_LEFTS
 * @see #ALL_THROUGHS_PROTECTEDS
 * @see #ALL_THROUGHS_RIGHTS
 * @since 2023.2.0
 */
public enum TrafficSignalPhaseApplicability
{
    //region: Enumeration Values

    /**
     * Enumeration value for no power.
     *
     * @since 1.0
     */
    NO_POWER,

    /**
     * Enumeration value for no applicability.
     *
     * @since 1.0
     */
    NONE,

    /**
     * Enumeration value for pedestrian signals applicability.
     *
     * @since 1.0
     */
    PEDESTRIAN,

    /**
     * Enumeration value for all east facing signals applicability.
     *
     * @since 1.0
     */
    ALL_EAST,

    /**
     * Enumeration value for all west facing signals applicability.
     *
     * @since 1.0
     */
    ALL_WEST,

    /**
     * Enumeration value for all north facing signals applicability.
     *
     * @since 1.0
     */
    ALL_NORTH,

    /**
     * Enumeration value for all south facing signals applicability.
     *
     * @since 1.0
     */
    ALL_SOUTH,

    /**
     * Enumeration value for all left turn signals applicability.
     *
     * @since 1.0
     */
    ALL_LEFTS,

    /**
     * Enumeration value for all through and protected signals applicability.
     *
     * @since 1.0
     */
    ALL_THROUGHS_PROTECTEDS,

    /**
     * Enumeration value for all through and right turn signals applicability.
     *
     * @since 1.0
     */
    ALL_THROUGHS_RIGHTS,

    /**
     * Enumeration value for all through and protected right turn signals applicability.
     *
     * @since 1.0
     */
    ALL_THROUGHS_PROTECTED_RIGHTS,

    /**
     * Enumeration value for ramp meter green signals applicability.
     *
     * @since 1.0
     */
    RAMP_METER_GREEN,

    /**
     * Enumeration value for ramp meter disabled signals applicability.
     *
     * @since 1.0
     */
    RAMP_METER_DISABLED,

    /**
     * Enumeration value for ramp meter starting signals applicability.
     *
     * @since 1.0
     */
    RAMP_METER_STARTING,

    /**
     * Enumeration value for requestable default green signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_DEFAULT_GREEN,

    /**
     * Enumeration value for requestable default green + flashing don't walk signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_DEFAULT_GREEN_FLASH_DW,

    /**
     * Enumeration value for requestable default green + flashing don't walk + flashing yellow HAWK (High-Intensity
     * Activated crossWalK beacon) signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK,

    /**
     * Enumeration value for requestable default yellow signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_DEFAULT_YELLOW,

    /**
     * Enumeration value for requestable default red signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_DEFAULT_RED,

    /**
     * Enumeration value for requestable service green signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_SERVICE_GREEN,

    /**
     * Enumeration value for requestable service green + flashing don't walk signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_SERVICE_GREEN_FLASH_DW,

    /**
     * Enumeration value for requestable service green + flashing don't walk + flashing yellow HAWK (High-Intensity
     * Activated crossWalK beacon) signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK,

    /**
     * Enumeration value for requestable service yellow signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_SERVICE_YELLOW,

    /**
     * Enumeration value for requestable service red signals applicability.
     *
     * @since 1.0
     */
    REQUESTABLE_SERVICE_RED,

    /**
     * Enumeration value for yellow transitioning signals applicability.
     *
     * @since 1.0
     */
    YELLOW_TRANSITIONING,

    /**
     * Enumeration value for flashing don't walk transitioning signals applicability.
     *
     * @since 1.0
     */
    FLASH_DONT_WALK_TRANSITIONING,

    /**
     * Enumeration value for red transitioning signals applicability.
     *
     * @since 1.0
     */
    RED_TRANSITIONING,

    /**
     * Enumeration value for all red signals applicability.
     *
     * @since 1.0
     */
    ALL_RED;

    //endregion

    //region: Instance Fields

    //endregion

    //region: Constructors

    //endregion

    //region: Instance Methods

    /**
     * Gets the ordinal value of the {@link TrafficSignalPhaseApplicability} enum. This can be used to store a
     * {@link TrafficSignalPhaseApplicability} enum in NBT data to be retrieved later using the {@link #fromNBT(int)}
     * method.
     *
     * @return the ordinal value of the {@link TrafficSignalPhaseApplicability} enum
     *
     * @since 1.0
     */
    public int toNBT()
    {
        return ordinal();
    }

    /**
     * Gets the {@link TrafficSignalPhaseApplicability} enum with the specified ordinal value, or
     * {@link TrafficSignalPhaseApplicability#NONE} if no {@link TrafficSignalPhaseApplicability} enum with the
     * specified ordinal value exists. This can be used to retrieve a {@link TrafficSignalPhaseApplicability} enum from
     * NBT data which was saved using the {@link #toNBT()} method.
     *
     * @param ordinal the ordinal value of the {@link TrafficSignalPhaseApplicability} enum to get
     *
     * @return the {@link TrafficSignalPhaseApplicability} enum with the specified ordinal value, or
     *         {@link TrafficSignalPhaseApplicability#NONE} if no {@link TrafficSignalPhaseApplicability} enum with the
     *         specified ordinal value exists
     *
     * @since 1.0
     */
    public static TrafficSignalPhaseApplicability fromNBT( int ordinal )
    {
        // Check if the specified integer value is within the range of the enumeration
        int finalOrdinal = ordinal;
        if ( ordinal < 0 || ordinal >= values().length ) {
            finalOrdinal = NONE.ordinal();
        }

        // Return the enumeration value with the specified integer value
        return values()[ finalOrdinal ];
    }

    //endregion
}
