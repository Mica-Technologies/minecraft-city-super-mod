package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalBodyColor implements IStringSerializable {
  FLAT_BLACK("flat_black", "Flat Black", 0.0F, 0.0F, 0.0F),
  DARK_OLIVE_GREEN("dark_olive_green", "Dark Olive Green", 0.0F, 0.141F, 0.0F),
  BATTLESHIP_GRAY("battleship_gray", "Battleship Gray", 0.482F, 0.514F, 0.494F),
  YELLOW("yellow", "Yellow", 0.953F, 0.6F, 0.0F);

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes
  private final float red;            // The red component of the color
  private final float green;          // The green component of the color
  private final float blue;           // The blue component of the color

  // Constructor
  TrafficSignalBodyColor(String name, String friendlyName, float red, float green, float blue) {
    this.name = name;
    this.friendlyName = friendlyName;
    this.red = red;
    this.green = green;
    this.blue = blue;
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

  // Getter for the red component
  public float getRed() {
    return red;
  }

  // Getter for the green component
  public float getGreen() {
    return green;
  }

  // Getter for the blue component
  public float getBlue() {
    return blue;
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