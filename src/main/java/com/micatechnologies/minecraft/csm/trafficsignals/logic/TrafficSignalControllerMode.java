package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Enumeration of the different modes of the traffic signal controller. Each has a corresponding integer value to be
 * used in the NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see #FLASH
 * @see #NORMAL
 * @see #REQUESTABLE
 * @see #MANUAL_OFF
 * @since 2022.1.0
 */
public enum TrafficSignalControllerMode
{
    //region: Enumeration Values

    /**
     * Enumeration value for the traffic signal controller mode "flash" with an identifier of 0 and a tick rate of 4.
     *
     * @since 1.0
     */
    FLASH( 0, 4, "Flash" ),

    /**
     * Enumeration value for the traffic signal controller mode "normal" with an identifier of 1 and a tick rate of 20.
     *
     * @since 1.0
     */
    NORMAL( 1, 20, "Normal" ),

    /**
     * Enumeration value for the traffic signal controller mode "requestable" with an identifier of 2 and a tick rate of
     * 20.
     *
     * @since 1.0
     */
    REQUESTABLE( 2, 20, "Requestable" ),

    /**
     * Enumeration value for the traffic signal controller mode "manual off" with an identifier of 99 and a tick rate of
     * 300.
     *
     * @since 1.0
     */
    MANUAL_OFF( 99, 300, "Manual Off" );

    //endregion

    //region: Instance Fields

    /**
     * The integer ID of the mode. This must be unique for each mode.
     *
     * @since 1.0
     */
    private final int id;

    /**
     * The tick rate value of the mode.
     *
     * @since 1.0
     */
    private final long tickRate;

    /**
     * The name of the mode.
     *
     * @since 1.0
     */
    private final String name;

    //endregion

    //region: Constructors

    /**
     * Constructor of the enumeration with the corresponding integer ID and the tick rate of the mode.
     *
     * @param id       the integer ID of the mode to be constructed
     * @param tickRate the tick rate value of the mode to be constructed
     * @param name     the name of the mode to be constructed
     *
     * @since 1.0
     */
    TrafficSignalControllerMode( int id, long tickRate, String name )
    {
        this.id = id;
        this.tickRate = tickRate;
        this.name = name;
    }

    //endregion

    //region: Instance Methods

    /**
     * Gets the corresponding integer ID of the mode.
     *
     * @return the integer ID of the mode
     *
     * @since 1.0
     */
    public int getId()
    {
        return id;
    }

    /**
     * Gets the tick rate value of the mode.
     *
     * @return the tick rate value of the mode
     *
     * @since 1.0
     */
    public long getTickRate()
    {
        return tickRate;
    }

    /**
     * Gets the name of the mode.
     *
     * @return the name of the mode
     *
     * @since 1.0
     */
    public String getName()
    {
        return name;
    }

    public TrafficSignalControllerMode getNextMode()
    {
        int nextId = ( id + 1 ) % TrafficSignalControllerMode.values().length;
        return TrafficSignalControllerMode.values()[ nextId ];
    }

    /**
     * Gets the {@link TrafficSignalControllerMode} enum with the specified integer ID, or
     * {@link TrafficSignalControllerMode#FLASH} if no {@link TrafficSignalControllerMode} enum with the specified
     * integer value exists.
     *
     * @param id the integer ID of the {@link TrafficSignalControllerMode} enum to get
     *
     * @return the {@link TrafficSignalControllerMode} enum with the specified integer value, or
     *         {@link TrafficSignalControllerMode#FLASH} if no {@link TrafficSignalControllerMode} enum with the
     *         specified integer value exists
     *
     * @since 1.0
     */
    public static TrafficSignalControllerMode getMode( int id )
    {
        for ( TrafficSignalControllerMode mode : values() ) {
            if ( mode.getId() == id ) {
                return mode;
            }
        }

        return FLASH;
    }

    //endregion
}
