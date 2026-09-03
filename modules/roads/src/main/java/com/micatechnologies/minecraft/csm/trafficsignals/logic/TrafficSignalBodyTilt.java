package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalBodyTilt implements IStringSerializable {
  LEFT_ANGLE("left_angle", "Left Angle"),
  LEFT_TILT("left_tilt", "Left Tilt"),
  NONE("none", "None"),
  RIGHT_TILT("right_tilt", "Right Tilt"),
  RIGHT_ANGLE("right_angle", "Right Angle");

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes

  // Constructor
  TrafficSignalBodyTilt(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  // Method to get the enum from NBT data
  public static TrafficSignalBodyTilt fromNBT(int ordinal) {
    int finalOrdinal = ordinal;
    if (ordinal < 0 || ordinal >= values().length) {
      finalOrdinal = 0;
    }
    return values()[finalOrdinal];
  }

  // Getter for the friendly name
  public String getFriendlyName() {
    return friendlyName;
  }

  // Method to get the next enum value in the sequence
  public TrafficSignalBodyTilt getNextTilt() {
    int ordinal = ordinal();
    int nextOrdinal = ordinal + 1;
    if (nextOrdinal >= values().length) {
      nextOrdinal = 0;
    }
    return values()[nextOrdinal];
  }

  // Method to convert the enum to its ordinal value for NBT storage
  public int toNBT() {
    return ordinal();
  }

  // Overriding the getName method from IStringSerializable
  @Override
  public @NotNull String getName() {
    return this.name;
  }
}