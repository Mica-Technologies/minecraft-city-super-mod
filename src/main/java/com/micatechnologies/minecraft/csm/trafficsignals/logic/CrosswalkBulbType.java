package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Enum representing the bulb/display type for crosswalk signals. Controls what is shown on
 * the signal face sections. Different block types support different subsets of bulb types.
 *
 * <p>For the single 16-inch signal, the bulb type is fixed (SYMBOL — hand/man icons) and
 * this enum is not used. For the double 12-inch stacked signal, this enum selects between
 * display modes.
 */
public enum CrosswalkBulbType implements IStringSerializable
{
    /**
     * Text-based display: upper section shows "DON'T WALK" text, lower shows "WALK" text.
     * The classic older-style incandescent crosswalk signal format.
     */
    WORDED( "worded", "Worded (DON'T WALK / WALK)" ),

    /**
     * Bi-modal hand/man in the upper section with a countdown module in the lower section.
     * The upper section shows the hand (don't walk) or walking man (walk) symbol, switching
     * between them based on signal state. The lower section shows a 7-segment countdown
     * during clearance and a dim "88" background at all other times.
     */
    HAND_MAN_COUNTDOWN( "hand_man_countdown", "Hand/Man with Countdown" );

    private final String name;
    private final String friendlyName;

    CrosswalkBulbType( String name, String friendlyName ) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public static CrosswalkBulbType fromNBT( int ordinal ) {
        if ( ordinal < 0 || ordinal >= values().length ) {
            return WORDED;
        }
        return values()[ ordinal ];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public CrosswalkBulbType getNextBulbType() {
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
