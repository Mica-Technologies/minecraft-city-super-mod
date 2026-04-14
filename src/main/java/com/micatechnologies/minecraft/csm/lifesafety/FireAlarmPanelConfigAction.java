package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * Enumeration of actions that can be performed on a fire alarm control panel via the
 * configuration GUI or config tool, such as cycling voice evac sounds, silencing, and resetting.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public enum FireAlarmPanelConfigAction {
  CYCLE_VOICE_EVAC_SOUND,
  AUDIBLE_SILENCE,
  RESET_PANEL
}
