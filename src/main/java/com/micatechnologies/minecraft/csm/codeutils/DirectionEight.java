package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.IStringSerializable;

/**
 * Enum representing eight compass directions (N, NE, E, SE, S, SW, W, NW) for use as a block
 * state property. Used by {@link AbstractBlockRotatableHZEight} to support 8-direction horizontal
 * rotation of blocks.
 *
 * @author Mica Technologies
 */
public enum DirectionEight implements IStringSerializable {
  S(0, "s", 0, 1),
  W(1, "w", -1, 0),
  N(2, "n", 0, -1),
  E(3, "e", 1, 0),
  SE(4, "se", 1, 1),
  SW(5, "sw", -1, 1),
  NW(6, "nw", -1, -1),
  NE(7, "ne", 1, -1);

  private final int index;
  private final String name;
  private final int offsetX;
  private final int offsetZ;

  DirectionEight(int index, String name, int offsetX, int offsetZ) {
    this.index = index;
    this.name = name;
    this.offsetX = offsetX;
    this.offsetZ = offsetZ;
  }

  public int getIndex() {
    return index;
  }

  /** X component of a one-block step in this direction. */
  public int getOffsetX() {
    return offsetX;
  }

  /** Z component of a one-block step in this direction. */
  public int getOffsetZ() {
    return offsetZ;
  }

  public boolean isDiagonal() {
    return this == NE || this == NW || this == SE || this == SW;
  }

  /** Returns the 180° opposite direction (S↔N, E↔W, NE↔SW, SE↔NW). */
  public DirectionEight getOpposite() {
    switch (this) {
      case N:  return S;
      case S:  return N;
      case E:  return W;
      case W:  return E;
      case NE: return SW;
      case SW: return NE;
      case NW: return SE;
      case SE: return NW;
      default: return this;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}


