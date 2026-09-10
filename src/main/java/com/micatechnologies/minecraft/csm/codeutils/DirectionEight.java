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

  /**
   * Returns how far a model facing north has to be turned about Y to face this way, in degrees.
   *
   * <p>North is zero and the angle runs anticlockwise, which is the convention the blockstate
   * JSON the generator writes already uses. Anything drawn by a tile entity renderer has to turn
   * by the same amount as the baked model beside it, so both read this rather than each carrying
   * a switch of its own — that is how a renderer ends up drawing every device facing north while
   * its model faces eight ways, with nothing failing anywhere.</p>
   *
   * @return the model rotation in degrees
   *
   * @since 1.0
   */
  public float getRotationDegrees() {
    switch (this) {
      case N:  return 0f;
      case NW: return 45f;
      case W:  return 90f;
      case SW: return 135f;
      case S:  return 180f;
      case SE: return 225f;
      case E:  return 270f;
      case NE: return 315f;
      default: return 0f;
    }
  }

  /**
   * Returns the direction 90° clockwise of this one, seen from above.
   *
   * <p>The counterpart of {@link net.minecraft.util.EnumFacing#rotateY()}, and it cannot be
   * index arithmetic: the four cardinals are numbered 0-3 to match
   * {@code EnumFacing.getHorizontalIndex()} so that a block converted from four facings to eight
   * keeps every facing already saved in a world, which leaves the diagonals appended at 4-7
   * rather than interleaved. The order is a migration decision, not a compass.</p>
   *
   * @return the direction a quarter turn clockwise
   *
   * @since 1.0
   */
  public DirectionEight rotateY() {
    switch (this) {
      case N:  return E;
      case E:  return S;
      case S:  return W;
      case W:  return N;
      case NE: return SE;
      case SE: return SW;
      case SW: return NW;
      case NW: return NE;
      default: return this;
    }
  }

  /**
   * Returns the direction 90° anticlockwise of this one, seen from above.
   *
   * @return the direction a quarter turn anticlockwise
   *
   * @since 1.0
   */
  public DirectionEight rotateYCCW() {
    return rotateY().rotateY().rotateY();
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


