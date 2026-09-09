package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Actions the in-roadway warning light's configuration GUI can dispatch.
 *
 * <p>Unlike the RRFB's, these are not appearance: the link mode decides which of a controller's
 * device lists the fixture joins, and the pattern decides what it does once it is called.</p>
 */
public enum InRoadwayLightConfigAction {
  CYCLE_LINK_MODE,
  CYCLE_PATTERN
}
