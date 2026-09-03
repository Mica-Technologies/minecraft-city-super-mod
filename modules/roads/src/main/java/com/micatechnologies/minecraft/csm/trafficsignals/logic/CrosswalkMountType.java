package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Enum representing crosswalk signal mounting styles. Determines the direction of the mounting
 * bracket relative to the signal body.
 */
public enum CrosswalkMountType implements IStringSerializable
{
    BASE( "base", "Base/Front Mount" ),
    REAR( "rear", "Rear Mount" ),
    LEFT( "left", "Left Mount" ),
    RIGHT( "right", "Right Mount" );

    private final String name;
    private final String friendlyName;

    CrosswalkMountType( String name, String friendlyName ) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public static CrosswalkMountType fromNBT( int ordinal ) {
        if ( ordinal < 0 || ordinal >= values().length ) {
            return BASE;
        }
        return values()[ ordinal ];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public CrosswalkMountType getNextMountType() {
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
