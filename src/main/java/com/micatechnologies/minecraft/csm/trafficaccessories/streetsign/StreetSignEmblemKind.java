package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

/**
 * What the blade's optional emblem slot holds: nothing, a route shield borrowed from the
 * guide sign's marker set (with its route number drawn over it), or a civic logo from the
 * street sign's own atlas rows.
 *
 * <p><b>Append-only: ordinals are serialized</b> in the sign's JSON.
 */
public enum StreetSignEmblemKind {
  NONE("None"),
  SHIELD("Route Shield"),
  LOGO("City Logo");

  private final String friendlyName;

  StreetSignEmblemKind(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public StreetSignEmblemKind next() {
    StreetSignEmblemKind[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static StreetSignEmblemKind fromOrdinal(int value) {
    StreetSignEmblemKind[] vals = values();
    if (value < 0 || value >= vals.length) {
      return NONE;
    }
    return vals[value];
  }
}
