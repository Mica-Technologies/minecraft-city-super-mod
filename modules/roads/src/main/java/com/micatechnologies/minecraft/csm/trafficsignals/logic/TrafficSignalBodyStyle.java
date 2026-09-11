package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * The housing (body) style of a traffic signal section. {@code STANDARD} is the modern
 * flat-back/trapezoidal polycarbonate housing; {@code BUBBLED} is the classic rounded
 * Eagle-style casting whose sections swell past the door frame and pinch at the seams;
 * {@code PV} is the deep housing of an optically programmed (3M / McCain programmable
 * visibility) head, as deep behind the door as it is tall. Cosmetic: doors, visors and bulbs are
 * unaffected, and only the mount hardware notices the PV rear (it bolts to the housing's back).
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public enum TrafficSignalBodyStyle implements IStringSerializable {
  STANDARD("standard", "Standard (Flat Back)"),
  BUBBLED("bubbled", "Bubbled (Eagle)"),
  /** Appended so the ordinals stored in existing worlds keep their meaning. */
  PV("pv", "PV (Deep)");

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
