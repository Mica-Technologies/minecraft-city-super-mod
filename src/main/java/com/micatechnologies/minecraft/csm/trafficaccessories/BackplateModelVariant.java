package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.util.IStringSerializable;

/**
 * Combined blockstate key that encodes both a signal backplate's tilt and its horizontal
 * orientation into a single property value. Backplate blockstate JSONs branch their model
 * selection on this single property so each of the ten (tilt &times; horizontal) combinations
 * can pick a distinct model file — something Forge v1 cannot express cleanly when tilt and
 * horizontal are kept as separate properties, because both end up overriding {@code model} and
 * only the last one wins.
 */
public enum BackplateModelVariant implements IStringSerializable {
  V_NONE("v_none"),
  V_LEFT_TILT("v_left_tilt"),
  V_RIGHT_TILT("v_right_tilt"),
  V_LEFT_ANGLE("v_left_angle"),
  V_RIGHT_ANGLE("v_right_angle"),
  H_NONE("h_none"),
  H_LEFT_TILT("h_left_tilt"),
  H_RIGHT_TILT("h_right_tilt"),
  H_LEFT_ANGLE("h_left_angle"),
  H_RIGHT_ANGLE("h_right_angle");

  private final String name;

  BackplateModelVariant(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Picks the variant that matches the given tilt and orientation. Falls back to
   * {@link #V_NONE} if passed a null tilt.
   */
  public static BackplateModelVariant of(TrafficSignalBodyTilt tilt, boolean horizontal) {
    if (tilt == null) {
      return horizontal ? H_NONE : V_NONE;
    }
    switch (tilt) {
      case LEFT_TILT:
        return horizontal ? H_LEFT_TILT : V_LEFT_TILT;
      case RIGHT_TILT:
        return horizontal ? H_RIGHT_TILT : V_RIGHT_TILT;
      case LEFT_ANGLE:
        return horizontal ? H_LEFT_ANGLE : V_LEFT_ANGLE;
      case RIGHT_ANGLE:
        return horizontal ? H_RIGHT_ANGLE : V_RIGHT_ANGLE;
      case NONE:
      default:
        return horizontal ? H_NONE : V_NONE;
    }
  }
}
