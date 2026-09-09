package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * Actions the school zone beacon configuration GUI can dispatch. The four schedule hours share
 * one action and are told apart by the packet's index, so adding a third window later does not
 * mean two more ordinals.
 */
public enum SchoolZoneBeaconConfigAction {
  CYCLE_SPEED_LIMIT,
  CYCLE_SCALE,
  CYCLE_ARRANGEMENT,
  CYCLE_BEACON_SIZE,
  CYCLE_MODE,
  ADJUST_SCHEDULE_HOUR
}
