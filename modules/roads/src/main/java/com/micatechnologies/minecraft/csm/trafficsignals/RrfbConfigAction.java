package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Actions the RRFB configuration GUI can dispatch to a beacon. Appearance only — how the
 * beacon is driven is the controller's business, not the player's.
 */
public enum RrfbConfigAction {
  CYCLE_HOUSING_COLOR,
  TOGGLE_DOUBLE_SIDED
}
