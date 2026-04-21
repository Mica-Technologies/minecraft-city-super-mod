package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Actions that can be dispatched to a single section of a traffic signal head via the
 * per-section configuration GUI. Distinct from {@link SignalHeadConfigAction} because the
 * whole-head actions (body tilt, alternate flash, aging toggle) do not have per-section
 * equivalents and would be meaningless to route through a section index.
 */
public enum SignalHeadSectionConfigAction {
  CYCLE_BODY_COLOR,
  CYCLE_DOOR_COLOR,
  CYCLE_VISOR_COLOR,
  CYCLE_VISOR_TYPE,
  CYCLE_BULB_STYLE,
  CYCLE_BULB_TYPE,
  CYCLE_BULB_AGING_STATE
}
