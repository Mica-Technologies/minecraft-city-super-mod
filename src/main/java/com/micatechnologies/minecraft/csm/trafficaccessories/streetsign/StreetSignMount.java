package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

/**
 * How a dynamic street sign is hung.
 *
 * <p><b>Append-only: ordinals are serialized</b> in the sign's JSON.
 */
public enum StreetSignMount {
  /**
   * Suspended below a mast arm on two hangers, the way an intersection blade hangs off a
   * traffic signal arm. The panel is centered in the block's depth so it reads from both
   * sides, and the hangers rise from its top edge to the top of the block.
   */
  HANGING("Hanging (mast arm)"),

  /**
   * Flat against whatever is behind it, like the dynamic guide sign. The panel's back sits
   * at the block's rear face, so only the front is ever seen.
   */
  FLAT("Flat (wall mount)");

  private final String friendlyName;

  StreetSignMount(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  /** Whether this mount leaves the panel's reverse exposed, so a back face is worth drawing. */
  public boolean canBeDoubleSided() {
    return this == HANGING;
  }

  public StreetSignMount next() {
    StreetSignMount[] vals = values();
    return vals[(ordinal() + 1) % vals.length];
  }

  public static StreetSignMount fromOrdinal(int value) {
    StreetSignMount[] vals = values();
    if (value < 0 || value >= vals.length) {
      return HANGING;
    }
    return vals[value];
  }
}
