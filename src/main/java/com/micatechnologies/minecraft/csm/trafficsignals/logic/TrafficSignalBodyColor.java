package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalBodyColor implements IStringSerializable {
  FLAT_BLACK("flat_black", "Flat Black", 0.094F, 0.094F, 0.094F),
  DARK_OLIVE_GREEN("dark_olive_green", "Dark Olive Green", 0.169F, 0.200F, 0.169F),
  BATTLESHIP_GRAY("battleship_gray", "Battleship Gray", 0.482F, 0.514F, 0.494F),
  YELLOW("yellow", "Yellow", 0.957F, 0.667F, 0.0F),
  GLOSSY_BLACK("glossy_black", "Glossy Black", 0.0F, 0.0F, 0.0F),
  SCHOOL_BUS_YELLOW("school_bus_yellow", "School Bus Yellow", 1.0F, 0.820F, 0.129F),
  GOLDENROD("goldenrod", "Goldenrod", 0.855F, 0.647F, 0.125F),
  PORTLAND_BLUE("portland_blue", "Portland Blue", 0.0F, 0.247F, 0.373F),
  NAVY_BLUE("navy_blue", "Navy Blue", 0.098F, 0.137F, 0.294F),
  CHOCOLATE_BROWN("chocolate_brown", "Chocolate Brown", 0.361F, 0.251F, 0.200F),
  DARK_BRONZE("dark_bronze", "Dark Bronze", 0.290F, 0.220F, 0.165F),
  BARE_ALUMINUM("bare_aluminum", "Bare Aluminum", 0.729F, 0.745F, 0.753F),
  CHARCOAL_GRAY("charcoal_gray", "Charcoal Gray", 0.251F, 0.251F, 0.251F),
  OFF_WHITE("off_white", "Off-White", 0.922F, 0.922F, 0.902F),
  MAROON("maroon", "Maroon", 0.392F, 0.110F, 0.149F),
  FOREST_GREEN("forest_green", "Forest Green", 0.133F, 0.267F, 0.180F);

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