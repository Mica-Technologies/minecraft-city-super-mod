package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalVisorType implements IStringSerializable {
  CIRCLE("circle", "Circle"),
  TUNNEL("tunnel", "Tunnel"),
  CUTAWAY("cutaway", "Cutaway"),
  VERTICAL_LOUVERED("louvered_vertical", "Vertical Louvered"),
  HORIZONTAL_LOUVERED("louvered_horizontal", "Horizontal Louvered"),
  BOTH_LOUVERED("louvered_both", "Vertical and Horizontal Louvered"),
  NONE("none", "None"),
  BARLO("barlo", "Barlo Safety Beam (Horizontal)"),
  BARLO_VERTICAL("barlo_vertical", "Barlo Safety Beam (Vertical)");

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes

  // Constructor
  TrafficSignalVisorType(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  // Method to get the enum from NBT data
  public static TrafficSignalVisorType fromNBT(int ordinal) {
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
  public TrafficSignalVisorType getNextVisorType() {
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
