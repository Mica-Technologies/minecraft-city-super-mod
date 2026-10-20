package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Actions a player can perform from the sensor configuration GUI, sent to the server in a
 * {@link SensorConfigPacket}. The {@code CLEAR_*} actions wipe every box of a detection zone, the
 * {@code REMOVE_LAST_*} actions remove a zone's most recently drawn box; the {@code ANGLE_*}
 * actions set the cosmetic aim angle on an
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorAngled}.
 *
 * <p>The packet carries the ordinal, so new actions are appended and never reordered.</p>
 */
public enum SensorConfigAction {
  CLEAR_STANDARD,
  CLEAR_LEFT,
  CLEAR_RIGHT,
  CLEAR_PROTECTED,
  CLEAR_ALL,
  ANGLE_NONE,
  ANGLE_LEFT,
  ANGLE_RIGHT,
  REMOVE_LAST_STANDARD,
  REMOVE_LAST_LEFT,
  REMOVE_LAST_RIGHT,
  REMOVE_LAST_PROTECTED
}
