package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

/** What the mount configuration screen can ask the server to change. */
public enum SpanWireMountConfigAction {
  CYCLE_MOUNT_STYLE,
  CYCLE_COIL_STYLE,
  /** Applies to the whole span, not just the mount it is set from. */
  CYCLE_SIGNAL_SIDE,
  /** Also span-wide: adds or removes the lower tether. */
  TOGGLE_BOX_SPAN,
  /** Span-wide: how far the messenger droops, from the presets in {@link SpanWireSag}. */
  CYCLE_SAG,
  /** Cluster mounts only; ignored on a single mount, which has no width to cycle. */
  CYCLE_CLUSTER_WIDTH,
  /**
   * Signal cluster hangers only: the hardware's body colour.
   *
   * <p>Sent by ordinal, so values are only ever appended. The configuration screen picks which
   * rows a mount shows; it no longer relies on the order.
   */
  CYCLE_HANGER_COLOR
}
