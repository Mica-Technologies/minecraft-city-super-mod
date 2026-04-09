package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Enum representing the type of display content on a crosswalk signal. Set per block class, not
 * configurable at runtime.
 */
public enum CrosswalkDisplayType implements IStringSerializable
{
    SYMBOL( "symbol", "Symbol (Hand/Man)" ),
    TEXT( "text", "Text (DON'T WALK/WALK)" ),
    SYMBOL_12INCH( "symbol_12inch", "Symbol 12-Inch (Hand/Man)" );

    private final String name;
    private final String friendlyName;

    CrosswalkDisplayType( String name, String friendlyName ) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public static CrosswalkDisplayType fromNBT( int ordinal ) {
        if ( ordinal < 0 || ordinal >= values().length ) {
            return SYMBOL;
        }
        return values()[ ordinal ];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public int toNBT() {
        return ordinal();
    }

    @Override
    public String getName() {
        return name;
    }
}
