package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Enum representing crosswalk signal visor styles. Separate from {@link TrafficSignalVisorType}
 * because crosswalk visors have fundamentally different geometry (flat face covers vs. round bulb
 * hoods).
 */
public enum CrosswalkVisorType implements IStringSerializable
{
    NONE( "none", "None" ),
    CRATE( "crate", "Crate Style" ),
    HOOD( "hood", "Hood Style" );

    private final String name;
    private final String friendlyName;

    CrosswalkVisorType( String name, String friendlyName ) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public static CrosswalkVisorType fromNBT( int ordinal ) {
        if ( ordinal < 0 || ordinal >= values().length ) {
            return NONE;
        }
        return values()[ ordinal ];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public CrosswalkVisorType getNextVisorType() {
        return values()[ ( ordinal() + 1 ) % values().length ];
    }

    public int toNBT() {
        return ordinal();
    }

    @Override
    public String getName() {
        return name;
    }
}
