package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * The housing (body) style of a traffic signal section. {@code STANDARD} is the modern
 * flat-back/trapezoidal polycarbonate housing; {@code BUBBLED} is the classic rounded
 * Eagle-style casting whose sections swell past the door frame and pinch at the seams.
 * Purely cosmetic — doors, visors, bulbs, and mounts are unaffected.
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public enum TrafficSignalBodyStyle implements IStringSerializable {
  STANDARD("standard", "Standard (Flat Back)"),
  BUBBLED("bubbled", "Bubbled (Eagle)");

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes

  // Constructor
  TrafficSignalBodyStyle(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  // Method to get the enum from NBT data
  public static TrafficSignalBodyStyle fromNBT(int ordinal) {
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
  public TrafficSignalBodyStyle getNextBodyStyle() {
    int nextOrdinal = ordinal() + 1;
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
