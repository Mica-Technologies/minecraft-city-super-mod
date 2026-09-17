package com.micatechnologies.minecraft.csm.trafficaccessories;

/**
 * The operating modes of the {@link ItemStreetLightConfigTool}. Sneak + right-click steps
 * through them in declaration order.
 *
 * <p>The tool stores a mode as its ordinal in the stack's NBT, so new modes are <b>appended</b>,
 * never inserted, or every tool already in a player's inventory changes mode under them.
 *
 * @author Mica Technologies
 * @since 2026.9.17
 */
public enum ItemStreetLightConfigToolMode {
  /** Fits or removes the ball finial on top of a pedestal traffic pole. */
  TOGGLE_BALL_FINIAL("Toggle Ball Finial");

  private final String friendlyName;

  ItemStreetLightConfigToolMode(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  /**
   * The name the tool reports in chat and in its tooltip.
   *
   * @return the mode's display name
   */
  public String getFriendlyName() {
    return friendlyName;
  }
}
