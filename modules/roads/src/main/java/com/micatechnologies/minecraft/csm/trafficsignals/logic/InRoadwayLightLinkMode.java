package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Which of a controller's device lists an in-roadway warning light joins, and therefore what
 * makes it flash.
 *
 * <p>The two are genuinely different installations rather than a preference. A fixture wired
 * alongside an RRFB is on the beacon list and is dark until the crossing is called; a fixture at
 * a signalised crosswalk is on the pedestrian list and belongs to the walk interval. They also
 * read the controller's colour differently, which is the real reason this cannot be one
 * behaviour: a beacon is lit on every colour but off, while a crosswalk fixture has to be dark
 * on don't-walk, which is the same colour.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public enum InRoadwayLightLinkMode {

  /**
   * Links as a {@link com.micatechnologies.minecraft.csm.trafficsignals.logic
   * .AbstractBlockControllableSignal.SIGNAL_SIDE#PEDESTRIAN_BEACON}, beside an RRFB or a HAWK.
   * Lit on anything but off, exactly as the RRFB is.
   */
  BEACON("Beacon (RRFB / HAWK)"),

  /**
   * Links as a pedestrian signal at an ordinary signalised crossing. Lit through walk and the
   * pedestrian clearance, dark on don't-walk.
   */
  CROSSWALK("Crosswalk");

  private final String friendlyName;

  InRoadwayLightLinkMode(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public static InRoadwayLightLinkMode fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return BEACON;
    }
    return values()[ordinal];
  }

  public int toNBT() {
    return ordinal();
  }

  public InRoadwayLightLinkMode getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  public String getFriendlyName() {
    return friendlyName;
  }
}
