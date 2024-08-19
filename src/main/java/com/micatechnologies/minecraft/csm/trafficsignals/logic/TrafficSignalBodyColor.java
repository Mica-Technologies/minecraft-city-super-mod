package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalBodyColor implements IStringSerializable {
  FLAT_BLACK("flat_black", "Flat Black"),
  DARK_OLIVE_GREEN("dark_olive_green", "Dark Olive Green"),
  BATTLESHIP_GRAY("battleship_gray", "Battleship Gray"),
  YELLOW("yellow", "Yellow"),
  YELLOW_YELLOW_BLACK("yellow_yellow_black", "Yellow Body with Black Visors"),
  YELLOW_BLACK_YELLOW("yellow_black_yellow","Yellow Rear Body, Black Front Body, Yellow Visors");

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes

  // Constructor
  TrafficSignalBodyColor(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  // Method to get the enum from NBT data
  public static TrafficSignalBodyColor fromNBT(int ordinal) {
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
  public TrafficSignalBodyColor getNextColor() {
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