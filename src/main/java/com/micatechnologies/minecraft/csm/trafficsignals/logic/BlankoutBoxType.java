package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

public enum BlankoutBoxType implements IStringSerializable {
    DONT_WALK( "dont_walk", "Don't Walk", "DW_BO" ),
    NO_LEFT_TURN( "no_left_turn", "No Left Turn", "NLT_BO" ),
    NO_RIGHT_TURN( "no_right_turn", "No Right Turn", "NRT_BO" ),
    DO_NOT_ENTER( "do_not_enter", "Do Not Enter", "DNE_BO" );

    private final String name;
    private final String friendlyName;
    private final String texturePrefix;

    BlankoutBoxType( String name, String friendlyName, String texturePrefix ) {
        this.name = name;
        this.friendlyName = friendlyName;
        this.texturePrefix = texturePrefix;
    }

    public static BlankoutBoxType fromNBT( int ordinal ) {
        if ( ordinal < 0 || ordinal >= values().length ) {
            return DONT_WALK;
        }
        return values()[ ordinal ];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getTexturePrefix() {
        return texturePrefix;
    }

    public BlankoutBoxType getNextType() {
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
