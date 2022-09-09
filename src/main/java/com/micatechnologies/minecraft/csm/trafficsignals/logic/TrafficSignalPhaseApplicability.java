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
 * @see #ALL_RIGHTS
 * @see #ALL_PROTECTEDS
 * @see #ALL_THROUGHS
 * @see #ALL_THROUGHS_PROTECTEDS
 * @since 2022.1.0
 */
public enum TrafficSignalPhaseApplicability
{
    //region: Enumeration Values

    /**
     * Enumeration value for no power.
     *
     * @since 1.0
     */
    NO_POWER( -1 ),

    /**
     * Enumeration value for no applicability.
     *
     * @since 1.0
     */
    NONE( 0 ),

    /**
     * Enumeration value for pedestrian signals applicability.
     *
     * @since 1.0
     */
    PEDESTRIAN( 1 ),

    /**
     * Enumeration value for all east facing signals applicability.
     *
     * @since 1.0
     */
    ALL_EAST( 2 ),

    /**
     * Enumeration value for all west facing signals applicability.
     *
     * @since 1.0
     */
    ALL_WEST( 3 ),

    /**
     * Enumeration value for all north facing signals applicability.
     *
     * @since 1.0
     */
    ALL_NORTH( 4 ),

    /**
     * Enumeration value for all south facing signals applicability.
     *
     * @since 1.0
     */
    ALL_SOUTH( 5 ),

    /**
     * Enumeration value for all left turn signals applicability.
     *
     * @since 1.0
     */
    ALL_LEFTS( 6 ),

    /**
     * Enumeration value for all right turn signals applicability.
     *
     * @since 1.0
     */
    ALL_RIGHTS( 7 ),

    /**
     * Enumeration value for all protected signals applicability.
     *
     * @since 1.0
     */
    ALL_PROTECTEDS( 8 ),

    /**
     * Enumeration value for all through signals applicability.
     *
     * @since 1.0
     */
    ALL_THROUGHS( 9 ),

    /**
     * Enumeration value for all through and protected signals applicability.
     *
     * @since 1.0
     */
    ALL_THROUGHS_PROTECTEDS( 10 );

    //endregion

    //region: Instance Fields

    /**
     * The integer ID of the {@link TrafficSignalPhaseApplicability} object.
     *
     * @since 1.0
     */
    private final int id;

    //endregion

    //region: Constructors

    /**
     * Constructor of the enumeration with the corresponding integer ID of the {@link TrafficSignalPhaseApplicability}
     * enum.
     *
     * @param id the integer ID of the {@link TrafficSignalPhaseApplicability} object to be constructed
     *
     * @since 1.0
     */
    TrafficSignalPhaseApplicability( int id )
    {
        this.id = id;
    }

    //endregion

    //region: Instance Methods

    /**
     * Gets the corresponding integer ID of the {@link TrafficSignalPhaseApplicability} object.
     *
     * @return the integer ID of the {@link TrafficSignalPhaseApplicability} object
     *
     * @since 1.0
     */
    public int getId()
    {
        return id;
    }

    /**
     * Gets the {@link TrafficSignalPhaseApplicability} enum with the specified integer value, or
     * {@link TrafficSignalPhaseApplicability#NONE} if no {@link TrafficSignalPhaseApplicability} enum with the
     * specified integer value exists.
     *
     * @param value the integer value of the {@link TrafficSignalPhaseApplicability} object to get
     *
     * @return the {@link TrafficSignalPhaseApplicability} enum with the specified integer value, or
     *         {@link TrafficSignalPhaseApplicability#NONE} if no {@link TrafficSignalPhaseApplicability} enum with the
     *         specified integer value exists
     *
     * @since 1.0
     */
    public static TrafficSignalPhaseApplicability getDirection( int value )
    {
        // Loop through all the values of the enumeration and return the one with the specified value
        for ( TrafficSignalPhaseApplicability direction : TrafficSignalPhaseApplicability.values() ) {
            if ( direction.getId() == value ) {
                return direction;
            }
        }

        // If no direction with the specified value exists, return NONE
        return NONE;
    }

    //endregion
}
