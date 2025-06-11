package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalBulbType implements IStringSerializable {
  LEFT("left", "Left"),
  UP_LEFT("up_left", "Up Left"),
  UP("up", "Up"),
  UP_RIGHT("up_right", "Up Right"),
  RIGHT("right", "Right"),
  BALL("ball", "Ball"),
  BIKE("bike", "Bike"),
  UTURN("u_turn", "U-Turn"),
  TRANSIT_LEFT("transit_left", "Transit Left"),
  TRANSIT("transit", "Transit"),
  TRANSIT_RIGHT("transit_right", "Transit Right");

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes

  // Constructor
  TrafficSignalBulbType(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  // Method to get the enum from NBT data
  public static TrafficSignalBulbType fromNBT(int ordinal) {
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
  public TrafficSignalBulbType getNextBulbType() {
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