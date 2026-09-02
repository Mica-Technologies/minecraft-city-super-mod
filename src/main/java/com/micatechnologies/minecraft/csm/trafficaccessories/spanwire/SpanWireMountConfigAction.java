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
  /**
   * Cluster mounts only; ignored on a single mount, which has no width to cycle.
   *
   * <p>Must stay last. The configuration screen draws one row per value in order and simply
   * leaves the final row off for a mount that is not a cluster.
   */
  CYCLE_CLUSTER_WIDTH
}
