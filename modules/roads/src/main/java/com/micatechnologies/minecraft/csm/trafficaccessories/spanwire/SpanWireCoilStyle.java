package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.util.IStringSerializable;

/**
 * How much coiled conductor slack is left hanging at a clamp.
 *
 * <p>Three states rather than a checkbox, because all three occur on real spans — often on the
 * same one. A span where every clamp carries an identical coil looks as artificial as a span
 * where none of them do.
 */
public enum SpanWireCoilStyle implements IStringSerializable {

  /** No slack left at this clamp. */
  NONE("none", "No Coil"),

  /** Coiled off on one side of the mast. The common case. */
  ONE_SIDE("one", "Coil, One Side"),

  /** Coiled off on both sides, as where two circuits are dropped at one hanger. */
  BOTH_SIDES("both", "Coil, Both Sides");

  private final String name;
  private final String friendlyName;

  SpanWireCoilStyle(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public int toNBT() {
    return ordinal();
  }

  public static SpanWireCoilStyle fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return ONE_SIDE;
    }
    return values()[ordinal];
  }

  /** Cycles to the next style, wrapping. */
  public SpanWireCoilStyle getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  @Override
  public String getName() {
    return name;
  }
}
