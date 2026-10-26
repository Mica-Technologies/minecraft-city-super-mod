package com.micatechnologies.minecraft.csm.buildingmaterials.stairs;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * How a {@link BlockStairRailing} cell's rail runs: level, or along a flight of stairs. Read from
 * the stairs around the cell, never stored.
 *
 * <p>{@code pos} climbs toward the positive end of the run's axis (east or south), {@code neg}
 * toward the other. A cell over a stair with the next stair beyond it is {@code mid}; over the top
 * stair of a flight it is {@code top}, the rail bending level halfway across; in front of the
 * bottom stair it is {@code foot}, the rail bending up halfway across to meet the flight.</p>
 *
 * @since 2026.10
 */
public enum RailSlope implements IStringSerializable {
  NONE("none"),
  MID_POS("mid_pos"),
  MID_NEG("mid_neg"),
  TOP_POS("top_pos"),
  TOP_NEG("top_neg"),
  FOOT_POS("foot_pos"),
  FOOT_NEG("foot_neg");

  private final String name;

  RailSlope(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  /** The slope of a cell over a stair climbing one way or the other. */
  static RailSlope over(boolean pos, boolean top) {
    if (top) {
      return pos ? TOP_POS : TOP_NEG;
    }
    return pos ? MID_POS : MID_NEG;
  }

  /** The slope of a cell in front of a flight climbing away from it one way or the other. */
  static RailSlope foot(boolean pos) {
    return pos ? FOOT_POS : FOOT_NEG;
  }
}
