package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.util.IStringSerializable;

/**
 * Combined blockstate key that encodes both a signal backplate's tilt and its horizontal
 * orientation into a single property value.
 *
 * <p>It no longer chooses a model. {@code TileEntitySignalBackplateRenderer} draws every plate
 * from the untilted model of its orientation and turns it about the head's centre itself, so what
 * this property now carries is <em>which way to turn</em> — the eight tilted constants are read by
 * the renderer and are deliberately not distinct geometry any more. {@link #untilted()} is how the
 * renderer gets from one to the model it actually wants.
 *
 * <h3>What this replaced</h3>
 *
 * Each of the ten combinations used to select its own pre-tilted model file, and the tilted ones
 * never quite lined up. A plate is mounted to the back of a head, so it has to turn about the
 * <em>head's</em> centre — a block away, in the facing direction. A blockstate can bake only one
 * rotation origin into a model and the right origin is a different point for every facing, so the
 * models were authored for whichever facing looked best and the rest drifted; the worst of it was
 * the horizontal add-ons, a quarter to half a block out, because a head's add-on pivots about the
 * <i>main</i> signal rather than itself. Some tilt angles also pushed geometry past Minecraft's
 * {@code [-16, 32]} per-axis element bounds.
 *
 * <p>The fix named here for a long time was "a TESR that replicates the signal head's two-stage
 * rotation". There is one now, so the angle is exact for every facing and the 64 pre-tilted model
 * files are gone.
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
   * The untilted variant of the same orientation.
   *
   * <p>The renderer draws every plate from one of these two and turns it itself, so the eight
   * tilted variants no longer select a model. They are still what {@code getActualState} reports,
   * because that is what tells the renderer which way to turn.
   *
   * @return {@link #H_NONE} for a horizontal variant, {@link #V_NONE} otherwise.
   */
  public BackplateModelVariant untilted() {
    return name.startsWith("h_") ? H_NONE : V_NONE;
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
