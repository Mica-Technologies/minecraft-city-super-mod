package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.IStringSerializable;

public enum LaneControlSignalType implements IStringSerializable {
    GREEN_ARROW("green_arrow", "Green Arrow"),
    OFF("off", "Off"),
    RED_X("red_x", "Red X"),
    SHARED_TURN("shared_turn", "Shared Turn"),
    TURN("turn", "Turn"),
    YELLOW_LEFT_ARROW("yellow_left_arrow", "Yellow Left Arrow"),
    YELLOW_RIGHT_ARROW("yellow_right_arrow", "Yellow Right Arrow"),
    YELLOW_X("yellow_x", "Yellow X");

    private final String name;
    private final String friendlyName;

    LaneControlSignalType(String name, String friendlyName) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public static LaneControlSignalType fromNBT(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            return OFF;
        }
        return values()[ordinal];
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public LaneControlSignalType getNextType() {
        return values()[(ordinal() + 1) % values().length];
    }

    public int toNBT() {
        return ordinal();
    }

    @Override
    public String getName() {
        return name;
    }
}
