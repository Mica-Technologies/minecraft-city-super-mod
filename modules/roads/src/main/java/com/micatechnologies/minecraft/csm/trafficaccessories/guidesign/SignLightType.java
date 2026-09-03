package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

/**
 * How a guide sign is illuminated. Ordinals are serialized into the sign's JSON, so this
 * enum is APPEND-ONLY: never reorder or remove a constant.
 */
public enum SignLightType {
  /** Unlit: retroreflective sheeting only, the way most modern guide signs are built. */
  NONE("None"),
  /**
   * Luminaires on a bracket below the sign, aimed up at the face. The classic overhead
   * guide sign install.
   */
  BOTTOM("Bottom Mount"),
  /** Luminaires on brackets above the sign, aimed down at the face. */
  TOP("Top Mount"),
  /** No external fixtures: the sign face itself is lit from behind (lightbox/button copy). */
  INTERNAL("Internal");

  private final String friendlyName;

  SignLightType(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  /** True when this type draws physical luminaires outside the sign body. */
  public boolean hasFixtures() {
    return this == BOTTOM || this == TOP;
  }

  public SignLightType next() {
    SignLightType[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static SignLightType fromOrdinal(int value) {
    SignLightType[] vals = values();
    if (value < 0 || value >= vals.length) {
      return NONE;
    }
    return vals[value];
  }
}
