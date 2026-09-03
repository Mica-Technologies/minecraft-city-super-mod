package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.util.IStringSerializable;

/**
 * Mounting style for a vehicle traffic signal head. Determines which bracket hardware (if any)
 * the TESR renders around the signal body, and — in add-on arrangements — which edges get
 * suppressed so the mount doesn't double up where two signals join.
 *
 * <ul>
 *   <li>{@link #NONE} — no bracket rendered; the signal is expected to be mounted via a
 *   separate astro-brac mount-kit block. Default value.</li>
 *   <li>{@link #REAR} — paired bracket hardware at the top and bottom of the signal (or
 *   left and right in horizontal mode) extending rearward toward the pole behind the
 *   signal.</li>
 *   <li>{@link #LEFT} — single bracket on the viewer's left side of the signal (bottom end
 *   in horizontal mode), extending leftward toward a pole on the left.</li>
 *   <li>{@link #RIGHT} — single bracket on the viewer's right side (top end in horizontal
 *   mode), extending rightward toward a pole on the right.</li>
 * </ul>
 */
public enum SignalHeadMountType implements IStringSerializable {
  NONE("none", "None"),
  REAR("rear", "Rear Mount"),
  LEFT("left", "Left Mount"),
  RIGHT("right", "Right Mount");

  private final String name;
  private final String friendlyName;

  SignalHeadMountType(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public int toNBT() {
    return ordinal();
  }

  public static SignalHeadMountType fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return NONE;
    }
    return values()[ordinal];
  }

  /** Cycles to the next mount type (NONE → REAR → LEFT → RIGHT → NONE). */
  public SignalHeadMountType getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  @Override
  public String getName() {
    return name;
  }
}
