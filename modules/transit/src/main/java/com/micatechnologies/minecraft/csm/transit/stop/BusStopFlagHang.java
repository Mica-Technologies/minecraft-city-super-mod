package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import net.minecraft.util.IStringSerializable;

/**
 * Which of a bus stop flag's six models is drawn: the road sign's shift, crossed with the side of
 * the post the flag reaches out to. One property picks the model because two cannot: in a Forge
 * blockstate each property's variant may name a model, and when two of them do the last one
 * wins, so {@code shift} and a separate side could never together pick "setback, reaching left".
 * The flag's {@code shift} variants are therefore empty and this actual-state property, worked
 * out from the shift and the side kept in {@link TileEntityBusStopFlag}, names the model.
 *
 * @since 2026.9
 */
public enum BusStopFlagHang implements IStringSerializable {
  NONE_RIGHT(SignShift.NONE, false),
  SETBACK_RIGHT(SignShift.SETBACK, false),
  BACKTOBACK_RIGHT(SignShift.BACKTOBACK, false),
  NONE_LEFT(SignShift.NONE, true),
  SETBACK_LEFT(SignShift.SETBACK, true),
  BACKTOBACK_LEFT(SignShift.BACKTOBACK, true);

  private final SignShift shift;
  private final boolean left;
  private final String name;

  BusStopFlagHang(SignShift shift, boolean left) {
    this.shift = shift;
    this.left = left;
    this.name = shift.getName() + (left ? "_left" : "_right");
  }

  /**
   * The value for a shift and a side.
   *
   * @param shift the road sign's shift
   * @param left  true for a flag reaching to the reader's left of the post
   *
   * @return the value
   */
  public static BusStopFlagHang of(SignShift shift, boolean left) {
    return values()[shift.ordinal() + (left ? 3 : 0)];
  }

  /**
   * The road sign's shift.
   *
   * @return the shift
   */
  public SignShift getShift() {
    return shift;
  }

  /**
   * Whether the flag reaches to the reader's left of the post.
   *
   * @return true for left
   */
  public boolean isLeft() {
    return left;
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
