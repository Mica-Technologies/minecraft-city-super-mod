package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

public enum BlankoutBoxVisorType implements IStringSerializable {
    NONE( "none", "None" ),
    HOOD( "hood", "Hood Style" ),
    DEEP_HOOD( "deep_hood", "Deep Hood Style" );

    private final String name;
    private final String friendlyName;

    BlankoutBoxVisorType( String name, String friendlyName ) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public static BlankoutBoxVisorType fromNBT( int ordinal ) {
        if ( ordinal < 0 || ordinal >= values().length ) {
            return NONE;
        }
        return values()[ ordinal ];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public BlankoutBoxVisorType getNextVisorType() {
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
