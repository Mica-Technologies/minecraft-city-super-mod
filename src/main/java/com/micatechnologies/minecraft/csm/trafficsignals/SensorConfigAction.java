package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Actions a player can perform from the sensor configuration GUI, sent to the server in a
 * {@link SensorConfigPacket}. The {@code CLEAR_*} actions wipe a detection zone; the {@code ANGLE_*}
 * actions set the cosmetic aim angle on an
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorAngled}.
 */
public enum SensorConfigAction {
  CLEAR_STANDARD,
  CLEAR_LEFT,
  CLEAR_RIGHT,
  CLEAR_PROTECTED,
  CLEAR_ALL,
  ANGLE_NONE,
  ANGLE_LEFT,
  ANGLE_RIGHT
}
