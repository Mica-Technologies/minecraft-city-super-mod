package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

/**
 * When a guide sign's lighting is energized. Ordinals are serialized into the sign's JSON,
 * so this enum is APPEND-ONLY: never reorder or remove a constant.
 */
public enum SignLightMode {
  /** Lights are fitted but switched off. */
  OFF("Always Off"),
  /** Lights are on around the clock. */
  ON("Always On"),
  /** Lights follow redstone power at the sign block. */
  REDSTONE("Redstone"),
  /** Photocell: lights come on when the sky light at the sign drops toward dusk. */
  NIGHT("Auto (Night)");

  private final String friendlyName;

  SignLightMode(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public SignLightMode next() {
    SignLightMode[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static SignLightMode fromOrdinal(int value) {
    SignLightMode[] vals = values();
    if (value < 0 || value >= vals.length) {
      return NIGHT;
    }
    return vals[value];
  }
}
