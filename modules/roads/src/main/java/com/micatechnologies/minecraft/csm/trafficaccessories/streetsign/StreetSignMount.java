package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

/**
 * How a dynamic street sign is hung.
 *
 * <p><b>Append-only: ordinals are serialized</b> in the sign's JSON. That is why the second
 * hanging style sits after {@link #FLAT} rather than beside {@link #HANGING} where it reads
 * more naturally -- use {@link #isHanging()} rather than comparing against a single constant.
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
  FLAT("Flat (wall mount)"),

  /**
   * Suspended the other common way: the blade hangs from a horizontal support beam carried on
   * a <b>single</b> drop in the middle, instead of from two separate hangers running all the
   * way up. The beam runs past both ends of the blade and takes the two short links the blade
   * actually swings on, so the whole assembly needs only one attachment point on the mast arm.
   * Otherwise identical to {@link #HANGING}: panel centered in the block's depth, readable
   * from both sides.
   */
  HANGING_BRACKET("Hanging (mast arm - alt)");

  private final String friendlyName;

  StreetSignMount(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  /**
   * Whether the blade is suspended rather than bolted to something behind it.
   *
   * <p>Everything that keyed off the mount before there was a second hanging style -- the
   * panel's depth in the block, its hitbox, whether a neighbouring traffic pole should sprout
   * a mount stub -- is asking this, not which hardware is drawn.
   *
   * @return {@code true} for either hanging style
   */
  public boolean isHanging() {
    return this == HANGING || this == HANGING_BRACKET;
  }

  /** Whether this mount leaves the panel's reverse exposed, so a back face is worth drawing. */
  public boolean canBeDoubleSided() {
    return isHanging();
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
