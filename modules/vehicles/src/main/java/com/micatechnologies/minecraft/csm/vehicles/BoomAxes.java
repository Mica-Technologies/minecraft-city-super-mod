package com.micatechnologies.minecraft.csm.vehicles;

import java.util.Map;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;

/**
 * The axes of a CSM boom truck's boom, which both the held keys ({@code BoomControls}) and the
 * one-touch BOOM driver ({@link BoomAutoDriver}) move.
 *
 * <p>The boom is drawn from these Immersive Vehicles variables alone, each animation reading its
 * value straight onto its axis ({@code gen_vehicle_fleet.py}'s {@code knuckle_animations} and
 * {@code tele_animations}). What a truck has, how far each axis runs and where BOOM takes it are
 * the truck's constants ({@code boom_constants} in the generator), so this reads them from its
 * definition rather than repeating them.</p>
 *
 * @since 2026.11
 */
final class BoomAxes {

  /** The one-touch switch: the panel's BOOM button, or B. */
  static final String BOOM = "BOOM";

  /** Degrees the lower boom is raised. */
  static final String LIFT = "BOOM_LIFT";

  /** Degrees the turret is turned, + to the truck's left. */
  static final String SWING = "BOOM_SWING";

  /** Degrees the knuckle boom's upper boom is opened at the elbow. */
  static final String ELBOW = "BOOM_ELBOW";

  /** Metres the telescopic boom is run out. */
  static final String REACH = "BOOM_REACH";

  /** How far the turret turns either way. */
  static final double SWING_LIMIT = 180.0;

  /** Per tick, held or driven: 20 degrees a second up, 30 round, 30 at the elbow, 1 m out. */
  static final double LIFT_RATE = 1.0;
  static final double SWING_RATE = 1.5;
  static final double ELBOW_RATE = 1.5;
  static final double REACH_RATE = 0.05;

  private BoomAxes() {
  }

  /** The truck's constants, or null when it is not a CSM boom truck. */
  static Map<String, Double> constants(EntityVehicleF_Physics vehicle) {
    if (vehicle == null || !vehicle.isValid || vehicle.definition == null
        || !CsmVehicles.PACK_ID.equals(vehicle.definition.packID)) {
      return null;
    }
    Map<String, Double> constants = vehicle.definition.constantValues;
    return constants != null && constants.containsKey("boom_present") ? constants : null;
  }

  /** Whether the truck's boom has an elbow (knuckle) rather than a reach (telescopic). */
  static boolean hasElbow(Map<String, Double> constants) {
    return constants.containsKey("elbow_present");
  }

  /** The second axis: ELBOW on a knuckle boom, REACH on a telescopic one. */
  static String second(Map<String, Double> constants) {
    return hasElbow(constants) ? ELBOW : REACH;
  }

  static double secondRate(Map<String, Double> constants) {
    return hasElbow(constants) ? ELBOW_RATE : REACH_RATE;
  }

  static double liftMax(Map<String, Double> constants) {
    return constants.getOrDefault("boom_lift_max", 80.0);
  }

  static double secondMax(Map<String, Double> constants) {
    return hasElbow(constants) ? constants.getOrDefault("boom_elbow_max", 170.0)
        : constants.getOrDefault("boom_reach_max", 8.0);
  }

  /** BOOM's working pose: lift, the second axis, swing. */
  static double[] deployPose(Map<String, Double> constants) {
    String second = hasElbow(constants) ? "boom_deploy_elbow" : "boom_deploy_reach";
    return new double[]{constants.getOrDefault("boom_deploy_lift", 0.0),
        constants.getOrDefault(second, 0.0), constants.getOrDefault("boom_deploy_swing", 0.0)};
  }
}
